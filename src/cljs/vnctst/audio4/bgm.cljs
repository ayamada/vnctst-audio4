(ns vnctst.audio4.bgm
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [vnctst.audio4.util :as util]
            [vnctst.audio4.device :as device]
            [vnctst.audio4.cache :as cache]
            [cljs.core.async :as async :refer [>! <!]]
            ))




;;;フェード時の音量変更の単位粒度(msec)
(defonce ^:dynamic fade-granularity-msec 100)



;;; channel-state-table の値は「nilもしくは以下のkeyを持つmap」のatomが入る
;;; - :fade-factor ; 通常は 1.0、フェード中のみ変化する
;;; - :fade-delta ; 通常は0。基本となるフェード量が設定される
;;; - :fade-process ; フェードを進めるgoスレッド、通常はnil
;;; - :current-param ; :path :volume :pan :pitch :oneshot? :as :ac を持つ map
;;; - :next-param ; 「次の曲」。内容は :current-param と同様。「ロード中」を
;;;   示すのもこれで表現される
(defonce channel-state-table (atom {}))


(defn- resolve-state! [channel]
  (or
    (get @channel-state-table channel)
    (let [s (atom nil)]
      (swap! channel-state-table assoc channel s)
      s)))


;;; バックグラウンド復帰の再生ポイント記録用
(defonce resume-pos-table (atom {}))




;;; 更新されたvolume値およびfade状態を、device側に反映させる
(defn- sync-state-volume! [state]
  (when-not (state/get :in-background?)
    (when-let [param (:current-param @state)]
      (when-let [ac (:ac param)]
        (when-let [volume (:volume param)]
          (let [[i-volume _ _] (util/calc-internal-params :bgm volume)
                i-volume (* i-volume (:fade-factor @state))]
            (device/call! :set-volume! ac i-volume)))))))

(defn sync-volume! []
  (doseq [state (vals @channel-state-table)]
    (sync-state-volume! state)))















(defn- fading? [state]
  (when-let [fade-delta (:fade-delta @state)]
    (not (zero? fade-delta))))

(defn- fade-out? [state]
  (and
    (fading? state)
    (neg? (:fade-delta @state))))

(defn- fade-in? [state]
  (and
    (fading? state)
    (pos? (:fade-delta @state))))













(defn- _play-immediately! [bgm-ch state as param]
  (let [ac (device/call! :spawn-audio-channel as)
        volume (:volume param)
        pitch (:pitch param)
        pan (:pan param)
        oneshot? (:oneshot? param)
        [i-volume i-pitch i-pan] (util/calc-internal-params
                                   :bgm volume pitch pan)
        new-param (assoc param
                         :as as
                         :ac ac)]
    (swap! state merge {:fade-factor 1
                        :fade-delta 0
                        :fade-process nil
                        :current-param new-param
                        :next-param nil})
    (util/logging-verbose :bgm/play (:path new-param))
    ;; バックグラウンド時は再生しないようにする。
    ;; また、バックグラウンド時は明示的な再開ポイントのリセットが必要
    ;; (非バックグラウンド時はイベントリスナで設定されるので不要)
    (if (state/get :in-background?)
      (swap! resume-pos-table assoc bgm-ch 0)
      (device/call! :play! ac 0 (not oneshot?) i-volume i-pitch i-pan false))))



;;; NB: ロード中にキャンセルされたり、別の音源の再生要求が入る可能性がある
(defn- _load+play! [bgm-ch state path volume pitch pan oneshot?]
  (let [previous-loading-key (:path (:next-param @state))
        h-done (fn [as]
                 (when-let [next-param (:next-param @state)]
                   (_play-immediately! bgm-ch state as next-param)))
        param {:path path
               :volume volume
               :pan pan
               :pitch pitch
               :oneshot? oneshot?}]
    ;; とりあえず再生対象を :next-param として積んでおく
    (swap! state assoc :next-param param)
    (if (cache/loaded? path)
      ;; プリロード済なら、それを利用する
      (when-let [as (cache/get-as path)]
        (h-done as))
      (cache/load-internal! path h-done bgm-ch))))




(defn- _stop-immediately! [state]
  (when @state
    (let [param (:current-param @state)]
      (when-let [ac (:ac param)]
        (util/logging-verbose :bgm/stop (:path param))
        ;; バックグラウンド中は既に再生停止している。
        ;; 二重に停止させないようにする
        (when-not (state/get :in-background?)
          (device/call! :stop! ac))
        (device/call! :dispose-audio-channel! ac))
      (reset! state nil))))



(defn- run-fader! [bgm-ch state]
  (when-not (:fade-process @state)
    (let [interval-msec fade-granularity-msec
          c (async/chan)]
      (go-loop []
        (<! (async/timeout interval-msec))
        (if (state/get :in-background?)
          (recur) ; バックグラウンド時はフェード処理を進めないようにする
          (when @state
            (let [delta (:fade-delta @state)
                  new-factor (max 0 (min 1 (+ delta (:fade-factor @state))))
                  end-value (if (pos? delta) 1 0)]
              (when-not (zero? delta)
                (swap! state assoc :fade-factor new-factor)
                (sync-state-volume! state)
                (if (and
                      (not= end-value new-factor)
                      ;; NB: 以下の判定を行うかどうかは迷う。
                      ;;     この判定は「MEのフェード中にMEの再生が完了した」
                      ;;     場合の為のものなのだが、
                      ;;     このチェックがなくても「フェードの残り秒数分だけ
                      ;;     無駄に待たされる」程度の問題しかない。
                      ;;     なので、この判定がそこそこ重いようであれば、
                      ;;     この判定は行わない方がよい。
                      ;;     現在は行わない事にした。
                      ;(when-let [ac (:ac (:current-param @state))]
                      ;  (device/call! :playing? ac))
                      )
                  (recur)
                  (do
                    (swap! state assoc :fade-process nil)
                    ;; フェードアウト完了時のみ、
                    ;; 次の曲が指定されているなら対応する必要がある
                    (when (zero? end-value)
                      (let [next-param (:next-param @state)]
                        (_stop-immediately! state)
                        (when next-param
                          (_load+play! bgm-ch
                                       state
                                       (:path next-param)
                                       (:volume next-param)
                                       (:pitch next-param)
                                       (:pan next-param)
                                       (:oneshot? next-param))))))))))))
      (swap! state assoc :fade-process c))))





(defn- apply-parameter! [state volume pitch pan]
  (let [[_ i-pitch i-pan] (util/calc-internal-params :bgm volume pitch pan)
        param (assoc (:current-param @state)
                     :volume volume
                     :pan pan
                     :pitch pitch)]
    (swap! state assoc :current-param param)
    (when-let [ac (:ac param)]
      (when-not (state/get :in-background?)
        (device/call! :set-pitch! ac i-pitch)
        (device/call! :set-pan! ac i-pan)
        (sync-state-volume! state)))))



(defn- same-bgm? [state path volume pitch pan]
  (when-let [param (:current-param @state)]
    (and
      (= (:path param) path)
      (= (:pitch param) pitch)
      true)))




;;; device側での再生状態をこちら側にも反映する
;;; (非ループ指定の時に、勝手に終わる為。逆はないので気にしなくてよい)
(defn- sync-playing-state! [state]
  (when-let [param (:current-param @state)]
    (when-let [ac (:ac param)]
      (when-not (device/call! :playing? ac)
        (_stop-immediately! state)))))



;;; 再生への状態遷移については、以下のパターンがある
;;; - 停止 → 再生開始(未ロード時の対応含む)
;;; - プリロード中 → 上記の「未ロード時の対応」と合流
;;; - ロード中 → ロード対象を差し替え
;;; - 再生中orフェードイン中 → 同一音源なので何もしない
;;; - 再生中orフェードイン中 → フェードアウト → 指定音源を再生開始
;;; - フェードアウト中 → 同一音源なのでフェードインに変更
;;; - フェードアウト中 → フェードアウト完了後に指定音源を再生開始

(defn play! [path options]
  (let [channel (or (:channel options) 0)
        volume (or (:volume options) 1)
        pitch (or (:pitch options) 1)
        pan (or (:pan options) 0)
        oneshot? (:oneshot? options)
        state (resolve-state! channel)
        ]
    (sync-playing-state! state)
    ;; NB: これが呼ばれたタイミングで、どのパターンであっても、
    ;;     とりあえず以前の :next-param は無用になるので消しておく
    (when @state
      (swap! state assoc :next-param nil))
    (cond
      ;; 停止中(もしくはプリロード中)なら、即座に再生を開始するだけでよい
      (not @state)
      (_load+play! channel state path volume pitch pan oneshot?)
      ;; ロード中(再生準備中)なら、ロード対象を差し替える
      (and
        (not (fading? state))
        (:next-param @state))
      (do
        (cache/cancel-load-by-stop-bgm! channel)
        (_load+play! channel state path volume pitch pan oneshot?))
      ;; 同一BGMの場合、パラメータの変更だけで済ませる
      ;; (ただしフェード中のみ特殊処理が必要)
      (same-bgm? state path volume pitch pan)
      (do
        (when (fading? state)
          (let [fade-msec (int (* 1000
                                  (or (state/get :default-bgm-fade-sec) 0)))
                fade-delta (/ fade-granularity-msec fade-msec)]
            (swap! state merge {:fade-delta fade-delta
                                :next-param nil})))
        (apply-parameter! state volume pitch pan))
      ;; 上記どれでもない場合は、フェードアウトさせてから再生する
      :else (let [fade-msec (int (* 1000
                                    (or (state/get :default-bgm-fade-sec) 0)))
                  fade-delta (- (/ fade-granularity-msec fade-msec))
                  next-param {:path path
                              :volume volume
                              :pan pan
                              :pitch pitch
                              :oneshot? oneshot?}]
              ;; NB: 既にフェード中の場合の為に、 :fade-factor はいじらない
              (swap! state merge {:fade-delta fade-delta
                                  :next-param next-param})
              (run-fader! channel state)))))





;;; 停止への状態遷移については、以下のパターンがある
;;; - 停止 → 何もしない
;;; - プリロード中 → 何もしない(プリロード自体は、再生イベントとは直交)
;;; - ロード中 → ロードをキャンセル
;;;   (この「ロード中」には「プリロード中」は含まない事に要注意)
;;; - 再生中orフェードイン中 → フェードアウト開始(fade-secが0なら即座に停止)
;;; - フェードアウト中 → 何もしない(fade-secが0なら即座に停止)

(defn stop! [channel fade-sec]
  (if (nil? channel)
    (doseq [bgm-ch (keys @channel-state-table)]
      (stop! bgm-ch fade-sec))
    (let [state (resolve-state! channel)
          fade-msec (int (* 1000 fade-sec))]
      (sync-playing-state! state)
      (cache/cancel-load-by-stop-bgm! channel)
      (when @state
        (if (zero? fade-msec)
          (_stop-immediately! state)
          (let [fade-delta (- (/ fade-granularity-msec fade-msec))]
            ;; NB: 既にフェード中の場合の対応の為に、 :fade-factor はいじらない
            (swap! state merge {:fade-delta fade-delta
                                ;; ロードをキャンセルする
                                :next-param nil})
            (run-fader! channel state)))))))






;;; バックグラウンドに入ったので、stateの再生を停止する
(defn- background-on! [bgm-ch state pos]
  ;; acが存在する場合は基本的には論理再生中。
  ;; ただし、:oneshot?再生終了時はその限りではなく、個別に対応する必要がある
  (when-let [ac (:ac (:current-param @state))]
    ;; NB: :oneshot?のみ、acが存在して再生終了状態になっているケースがある
    (let [playing? (device/call! :playing? ac)]
      (if-not playing?
        (swap! resume-pos-table dissoc bgm-ch)
        (let [pos (device/call! :pos ac)]
          (swap! resume-pos-table assoc bgm-ch pos)
          (device/call! :stop! ac))))))

;;; バックグラウンドが解除されたので、復帰させるべき曲があれば、再生を再開する
(defn- background-off! [bgm-ch state pos]
  (when pos
    (let [param (:current-param @state)]
      (when-let [ac (:ac param)]
        (let [volume (:volume param)
              pitch (:pitch param)
              pan (:pan param)
              oneshot? (:oneshot? param)
              [i-volume i-pitch i-pan] (util/calc-internal-params
                                         :bgm volume pitch pan)
              i-volume (* i-volume (:fade-factor @state))]
          (device/call! :play! ac pos (not oneshot?) i-volume i-pitch i-pan false)))
      (swap! resume-pos-table dissoc bgm-ch))))

(defn sync-background! [now-background? &[force?]]
  (when (or
          force?
          (not (state/get :dont-stop-on-background?)))
    (doseq [bgm-ch (keys @channel-state-table)]
      (let [state (resolve-state! bgm-ch)
            pos (get @resume-pos-table bgm-ch)]
        (if now-background?
          (background-on! bgm-ch state pos)
          (background-off! bgm-ch state pos))))))





(defn stop-for-unload! [path]
  (doseq [bgm-ch (keys @channel-state-table)]
    (let [state (get @channel-state-table bgm-ch)]
      (when (= path (get-in @state [:current-param :path]))
        (stop! bgm-ch 0)))))







