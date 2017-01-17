(ns vnctst.audio4.se
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [vnctst.audio4.util :as util]
            [vnctst.audio4.device :as device]
            [vnctst.audio4.cache :as cache]
            [cljs.core.async :as async :refer [>! <!]]
            ))



;;; TODO: BGM同様、バックグラウンド消音に対応してもよい(ただし:alarm?に注意)




(defonce auto-number-source (atom 0))
(defn- auto-number! []
  (let [i (mod (inc @auto-number-source) 16777216)]
    (reset! auto-number-source i)
    i))




;;;フェード時の音量変更の単位粒度(msec)
(defonce ^:dynamic fade-granularity-msec 100)

(defonce fade-process (atom nil))
(defonce fade-targets (atom {}))



;;; 一定msec内には、同一SEを連打する事はできない
(defonce same-se-prevent-table (atom {}))







;;; 再生中のseのacをここに保存する。
;;; 後述のpool-watcherによって適当なタイミングで定期的に検査され、
;;; 再生の終了しているacはdisposeして除外される。
;;; - keyはauto-number!によって発行される数値
;;; - valは以下のような(atom {...})。以降ではこれをstateと呼ぶ。
;;; このstateは以下の情報を保持する。
;;; - :ac ; deviceのaudio-channel。ただしロード待ち中はnilになるので注意
;;; - :path ; asの元path。cacheへのアクセス他に利用される
;;; - :fade-factor ; 通常は 1.0、フェード中のみ変化する
;;; - :fade-delta ; 通常は0。基本となるフェード量が設定される
;;; - :volume : 引数から得られた素の情報
;;; - :pan : 引数から得られた素の情報
;;; - :pitch : 引数から得られた素の情報
;;; - :alarm? : 引数から得られた素の情報
;;; - :dont-play? : ロード中にstop!が実行され、ロード後の再生がキャンセルされた
(defonce ^:private playing-state-pool (atom {}))
;;; TODO: ここへの登録数に制限をかければ、最大同時再生SE数の上限を設定できる。
;;;       ただし、具体的にどれぐらいの上限を設定すべきかは環境によって異なり、
;;;       適切な値を設定するのは困難だと思われる。

(defn playing-se-channel-ids []
  (keys @playing-state-pool))




;;; 更新されたvolume値およびfade状態を、device側に反映させる
(defn- sync-state-volume! [state]
  (when-let [ac (:ac @state)]
    (when-let [volume (:volume @state)]
      (let [[i-volume _ _] (util/calc-internal-params :se volume)
            i-volume (* i-volume (:fade-factor @state))]
        (device/call! :set-volume! ac i-volume)))))

(defn sync-volume! []
  (doseq [state (vals @playing-state-pool)]
    (sync-state-volume! state)))








;;; 後述のwatcherを停止させる為のchan
(defonce playing-audiochannel-pool-watcher (atom nil))

(defn run-playing-audiochannel-pool-watcher! []
  (when-not @playing-audiochannel-pool-watcher
    (let [c (async/chan)]
      (go-loop []
        ;; TODO: cからgoスレッド停止コマンドを受け付けるようにする
        (<! (async/timeout 1111))
        (swap! playing-state-pool
               (fn [old]
                 (reduce (fn [stack [sid state]]
                           (or
                             ;; fade-targetsにエントリがある内は除去しない
                             (when-not (get @fade-targets sid)
                               ;; ロード完了前なら除去しない
                               (when (cache/loaded? (:path @state))
                                 ;; acがまだないなら除去しない。
                                 ;; これは稀なケースだが起こる
                                 ;; (ロードエラー時にはplay!内のgoスレッドにて
                                 ;; playing-state-poolから削除される為、
                                 ;; ここで処理する必要はない。が、
                                 ;; goスレッドで処理されるまでにタイムラグが
                                 ;; あるので、この判定が必要となる)
                                 (when-let [ac (:ac @state)]
                                   ;; 再生中なら除去しない
                                   (when-not (device/call! :playing? ac)
                                     ;; disposeして除去する
                                     (device/call! :dispose-audio-channel! ac)
                                     stack))))
                             (assoc stack sid state)))
                         {}
                         old)))
        (recur))
      (reset! playing-audiochannel-pool-watcher c))))





(defn- same-se-interval []
  (int (* 1000 (state/get :se-chattering-sec))))


(defn- played-same-se? [combined-key]
  (let [prev (or (get @same-se-prevent-table combined-key) 0)
        now (js/Date.now)
        ok? (or
              (< now prev) ; タイマー巻き戻り対策
              (< (+ prev (same-se-interval)) now))]
    (not ok?)))

(defn- update-played-same-se! [combined-key]
  (let [now (js/Date.now)]
    (swap! same-se-prevent-table
           (fn [old-table]
             (let [too-old (- now (same-se-interval))]
               (assoc (into {} (remove (fn [[k v]]
                                         (< v too-old))
                                       old-table))
                      combined-key now))))))











(defn play! [path options]
  (let [volume (or (:volume options) 1)
        pitch (or (:pitch options) 1)
        pan (or (:pan options) 0)
        alarm? (:alarm? options)]
    (let [sid (auto-number!)
          r (util/calc-internal-params :se volume pitch pan)
          [i-volume i-pitch i-pan] r
          combined-key [path volume pitch pan]
          doit! (fn [state]
                  (util/logging-verbose :se/play (:path @state))
                  (update-played-same-se! combined-key)
                  (device/call!
                    :play! (:ac @state) 0 false i-volume i-pitch i-pan alarm?)
                  (swap! playing-state-pool assoc sid state))]
      (when (and
              (or
                alarm?
                (not (state/get :in-background?)))
              (pos? i-volume)
              (not (played-same-se? combined-key)))
        (let [as (cache/get-as path)
              ac (when as
                   (device/call! :spawn-audio-channel as))
              state (atom {:ac ac
                           :path path
                           :fade-factor 1.0
                           :fade-delta 0
                           :volume volume
                           :pan pan
                           :pitch pitch
                           :alarm? alarm?
                           :dont-play? false})]
          (if ac
            (doit! state)
            (let [h-err (fn []
                          (swap! playing-state-pool dissoc sid))
                  h-done (fn [as]
                           (if (:dont-play? @state)
                             (h-err)
                             (let [ac (device/call! :spawn-audio-channel as)]
                               (swap! state assoc :ac ac)
                               (when-not (played-same-se? combined-key)
                                 (doit! state)))))]
              (cache/load-internal! path h-done)
              ;; NB: ロード失敗時は不要エントリの削除を行わなくてはならない
              (go-loop []
                (<! (async/timeout 1234))
                (if-not (cache/loaded? path)
                  (recur)
                  (when (cache/error? path)
                    (h-err))))))
          sid)))))







(defn- _stop-immediately! [sid state]
  ;; NB: playing-state-poolからのdissocと、
  ;;     :dispose-audio-channel!の呼び出しはpool-watcherにやらせる事が必須！
  ;;     うっかり実行してしまわない事！
  (swap! fade-targets dissoc sid)
  (when-let [ac (:ac @state)]
    (util/logging-verbose :se/stop (:path @state))
    (device/call! :stop! ac)))


(defn- run-fader! []
  (when-not @fade-process
    (let [interval-msec fade-granularity-msec
          c (async/chan)]
      (go-loop []
        (<! (async/timeout interval-msec))
        (if (empty? @fade-targets)
          (reset! fade-process nil)
          (do
            (doseq [[sid state] @fade-targets]
              (let [delta (:fade-delta @state)
                    new-factor (max 0 (min 1 (+ delta (:fade-factor @state))))
                    end-value (if (pos? delta) 1 0)]
                (if (zero? delta)
                  (swap! fade-targets dissoc sid)
                  (do
                    (swap! state assoc :fade-factor new-factor)
                    (sync-state-volume! state)
                    (when (= end-value new-factor)
                      (swap! fade-targets dissoc sid)
                      (when (zero? end-value)
                        (_stop-immediately! sid state)))))))
            (recur))))
      (reset! fade-process c))))



(defn- fade-out! [sid state fade-sec]
  (if (zero? fade-sec)
    (_stop-immediately! sid state)
    (let [fade-msec (int (* 1000 fade-sec))
          fade-delta (- (/ fade-granularity-msec fade-msec))]
      ;; NB: 既にフェード中の場合の対応の為に、 :fade-factor はいじらない
      (swap! state assoc :fade-delta fade-delta)
      (swap! fade-targets assoc sid state)
      (run-fader!))))



;;; 再生中のSEをフェードアウト停止(もしくは即時停止)させる
(defn stop! [se-channel-id fade-sec]
  (when se-channel-id
    (when-let [state (get @playing-state-pool se-channel-id)]
      ;; NB: acが既にdispose済の場合は何も行わないようにする必要がある。
      ;;     ただし、disposeかどうかの判定はpool-watcherのみが行う必要がある
      ;;     (stateの除去と同時でなくてはならない為)。
      ;;     ※うっかり忘れて、修正時にこの前提を壊さないようにする事！
      (if-let [ac (:ac @state)]
        (fade-out! se-channel-id state (or fade-sec 0))
        ;; asがまだロード中の場合、acもまたnilとなる。
        ;; この場合は「ロード後の再生」をキャンセルする必要がある。
        ;; この場合はフェード秒を無視する(再生前でありフェードの意味がない)
        (let [new-state (assoc state :dont-play? true)]
          (util/logging-verbose :se/stop (:path @state))
          (swap! playing-state-pool assoc se-channel-id new-state)))
      true)))




(defn unload! [path]
  (doseq [[sid state] @playing-state-pool]
    (when (= path (:path @state))
      (_stop-immediately! sid state)
      ;; NB: 本来ならpool-watcherにやらせるべきfinalize処理だが、
      ;;     ここでは即座の完全破棄が必要なので、自前で行う必要がある
      (swap! playing-state-pool dissoc sid)
      (swap! fade-targets dissoc sid)
      (when-let [ac (:ac @state)]
        (device/call! :dispose-audio-channel! ac)))))




