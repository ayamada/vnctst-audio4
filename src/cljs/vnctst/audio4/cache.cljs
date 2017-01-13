(ns vnctst.audio4.cache
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [vnctst.audio4.util :as util]
            [vnctst.audio4.device :as device]
            [cljs.core.async :as async :refer [>! <!]]
            ))





;;; BGMのロード後再生の為に、pathを記憶しておく必要がある
;;; (これは複数のロードによる書き換えの為に必要となる)
(defonce ^:private last-loading-bgm-path (atom {}))


;;; ロード済(エラー含む)のasを保持するテーブル
;;; エラー時は「エントリはあるが値はnil」となる
(defonce ^:private loaded-audiosource-table (atom {}))


;;; ロード中のasの、ロード完了時実行ハンドル等の情報を保持するテーブル
;;; (ロード完了して実行されたら消去される。完全に内部用なので公開禁止)
;;; 内容はmapで保持。ロード中判定にも使われる
(defonce ^:private loading-info-table (atom {}))


;;; プリロードを直列処理にする為のキュー。
;;; これはプリロード(ユーザからのロード要求)のみに使用される。
;;; これは再生要求時の内部ロード時には使用されない(並列処理される)
(defonce preload-request-queue (atom nil))







(defn- loading? [path]
  (or
    (get @loading-info-table path)
    (loop [left @preload-request-queue]
      (when-let [p (first left)]
        (if (= path p)
          true
          (recur (rest left)))))))

;;; NB: エラー発生時も「ロード自体は完了」として真値を返す。
;;;     成功したかどうかを見るには error? を呼ぶ事。
;;;     unload!されると偽値に戻る事にも注意。
(defn loaded? [path]
  (contains? @loaded-audiosource-table path))

;;; ロード済かつロード異常終了であれば真値を返す
(defn error? [path]
  (and
    (loaded? path)
    (not (boolean (get @loaded-audiosource-table path)))))

;;; ロードされたasを取得する
(defn get-as [path]
  (get @loaded-audiosource-table path))






;;; 再生要求の内部でのロード実行。後述のプリロードとは以下が違う
;;; - ロードは即座に開始される(ネットワーク帯域を圧迫する可能性あり)
;;; - ロード完了時に実行されるdone-fnが指定可能
;;;   - pathが既にロード済なら即座にdone-fnが実行される
;;;   - このdone-fnは後から書き換えが行われ、実行されない場合がある
;;;     - 具体的には以下のパターンとなる
;;;       - bgm-channelがnil以外かつ、ロード中に play-bgm! が実行された場合
;;;       - bgm-channelがnil以外かつ、ロード中に stop-bgm! が実行された場合
;;;         - この場合はstop-bgm!サイドからcancel-load-by-stop-bgm!が呼ばれる
;;;       - bgm-channelがnilかつ、ロード中に stop-se! が実行された場合
;;;         - この場合はstop-se!サイドからcancel-load-by-stop-se!が呼ばれる
;;;       - ロード中に unload! が実行された場合
;;;   - このdone-fnは「ロード成功後に再生を開始する為のもの」であるので、
;;;;    ロードがエラーとなった場合は何も行われない。
;;;;    「ロードの成功/失敗に関わらず、完了時に指定関数を実行してほしい」場合は
;;;;    done-fnを使わずに、自前で定期的に loaded? error? を呼んで
;;;;    チェックするgoスレッドを走らせる事。
;;; - ロード完了後にBGMを再生する場合は、bgm-channel引数の指定が必須(nil不可)。
;;;   SEの場合はnilを指定すればよい。
(defn load-internal! [path & [done-fn bgm-channel]]
  (if (or
        (loaded? path)
        (and
          (state/get :disable-mobile?)
          (:mobile util/terminal-type)))
    ;; 既にロード済。done-fnを実行して終了
    (when done-fn
      (done-fn))
    (do
      ;; BGMのロードの場合、last-loading-bgm-pathを更新する必要がある。
      ;; また同時に、既にロード中のBGMがあるなら、先にそれのdone-fnを
      ;; 取り消す必要もある
      (when bgm-channel
        (when-let [loading-bgm-path (get @last-loading-bgm-path bgm-channel)]
          (when-let [info (get @loading-info-table loading-bgm-path)]
            (swap! loading-info-table
                   assoc loading-bgm-path
                   (assoc info :done-fn nil))))
        (swap! last-loading-bgm-path assoc bgm-channel path))
      (if-let [info (get @loading-info-table path)]
        ;; このpathは既にロード中。登録されているdone-fnを上書きするだけで完了
        (swap! loading-info-table
               assoc path
               (assoc info :done-fn done-fn :bgm-channel bgm-channel))
        (do
          ;; このpathは未ロードもしくはキューに入っている。
          ;; キューに入っている場合はこのタイミングでキューから削除しておく
          (swap! preload-request-queue
                 #(remove (fn [p] (= path p)) %))
          (let [real-pathes (util/expand-pathes path)]
            (if (empty? real-pathes)
              (util/logging :error (str "cannot resolve autoext by "
                                        (pr-str path)))
              (let [start-real-path (first real-pathes)
                    info {:path path
                          :left-real-pathes (atom real-pathes)
                          :done-fn done-fn
                          :bgm-channel bgm-channel}
                    h-ok (fn [as]
                           ;; NB: done-fnは後から書き換えられる可能性があるので
                           ;;     ここで改めて最新のものを取得する
                           (let [info (get @loading-info-table path)
                                 real-path (first @(:left-real-pathes info))]
                             (swap! loaded-audiosource-table assoc path as)
                             (swap! loading-info-table dissoc path)
                             (util/logging :loaded path :as real-path)
                             (when-let [bgm-channel (:bgm-channel info)]
                               (swap! last-loading-bgm-path dissoc bgm-channel))
                             (when-let [f (:done-fn info)]
                               (f))))
                    h-err (atom nil)]
                (reset! h-err
                        (fn [msg]
                          (let [left-real-pathes (get-in @loading-info-table
                                                         [path
                                                          :left-real-pathes])
                                real-path (first @left-real-pathes)]
                            (util/logging :error path :as real-path :by msg)
                            (swap! left-real-pathes rest)
                            (if-let [next-real-path (first @left-real-pathes)]
                              (device/call! :load-audio-source!
                                            next-real-path
                                            h-ok
                                            @h-err)
                              (do
                                (when-let [bgm-channel (:bgm-channel info)]
                                  (swap! last-loading-bgm-path
                                         dissoc bgm-channel))
                                ;; NB: エラー時は「エントリはあるが値はnil」
                                ;;     という形式で示す。
                                ;;     うっかりここをdissocに書き換えない事。
                                (swap! loaded-audiosource-table assoc path nil)
                                (swap! loading-info-table dissoc path))))))
                (swap! loading-info-table assoc path info)
                (device/call! :load-audio-source!
                              start-real-path
                              h-ok
                              @h-err)))))))))






(defn cancel-load-by-stop-bgm! [bgm-channel]
  (when-let [path (get @last-loading-bgm-path bgm-channel)]
    (when-let [info (get @loading-info-table path)]
      (swap! loading-info-table
             assoc path
             (assoc info :done-fn nil)))
    (swap! last-loading-bgm-path dissoc bgm-channel)))

(defn cancel-load-by-stop-se! [path]
  (when-let [info (get @loading-info-table path)]
    (swap! loading-info-table
           assoc path
           (assoc info :done-fn nil))))







(defonce preload-process (atom nil))

;;; プリロードを行う。プリロード要求は一旦キューに収められ、
;;; 直列に逐次ロードされる事が保証される。
;;; (並列に同時ロードになってネットワーク帯域を圧迫しないようにしている)
(defn load! [path]
  (when (and
          (not (loaded? path))
          (not (loading? path))
          (not (and
                 (state/get :disable-mobile?)
                 (:mobile (util/terminal-type)))))
    (swap! preload-request-queue #(concat % [path]))
    (util/run-preload-process! preload-process
                               preload-request-queue
                               (fn [k]
                                 (load-internal! k)
                                 (go-loop []
                                   (when-not (loaded? k)
                                     (<! (async/timeout 333))
                                     (recur)))))
    true))


;;; NB: まだ鳴っている最中に呼ばないようにする事
;;;     (必要なら、unload!を呼ぶ前に再生を停止させておく事。
;;;     この停止処理はcacheモジュール内では困難)
(defn unload! [path]
  (if (loaded? path)
    ;; ロード済
    (when-let [as (get @loaded-audiosource-table path)]
      (device/call! :dispose-audio-source! as)
      (swap! loaded-audiosource-table dissoc path))
    ;; 未ロードもしくはロードキュー待ち
    (do
      ;; ロード待ち各種から消しておく
      (when-let [info (get @loading-info-table path)]
        ;; ロード完了ハンドルをunload!で上書きする
        (swap! loading-info-table
               assoc path (assoc info :done-fn #(unload! path))))
      (swap! preload-request-queue
             #(remove (fn [p] (= path p)) %))))
  true)


(defn unload-all! []
  (doseq [path (keys @loaded-audiosource-table)]
    (unload! path))
  (doseq [path (keys @loading-info-table)]
    (unload! path))
  true)






