(ns vnctst.audio4
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [vnctst.audio4.util :as util]
            [vnctst.audio4.device :as device]
            [vnctst.audio4.background :as background]
            [vnctst.audio4.cache :as cache]
            [vnctst.audio4.bgm :as bgm]
            [vnctst.audio4.se :as se]
            [cljs.core.async :as async :refer [>! <!]]
            ))



;;; TODO: stat関数の実装
;;;       (現在のロード数、再生BGMの状態、SEの状態等を一つの文字列として返す)




(defn- empty-path? [path]
  (try
    (empty? path)
    (catch :default e false)))


;;; 最も最近に鳴らしたSEのチャンネルobjを保持する
(defonce a-last-played-se-channel-id (atom nil))
(defn last-played-se-channel-id [] @a-last-played-se-channel-id)





;;; Internal

(defn- init-force! []
  (device/init! (state/get :disable-webaudio?)
                (state/get :disable-htmlaudio?))
  (reset! background/background-handle bgm/sync-background!)
  (background/start-supervise!)
  (se/run-playing-audiochannel-pool-watcher!)
  true)

(defonce initialized? (atom false))

(defn- init! []
  (when-not @initialized?
    (init-force!)
    (reset! initialized? true)))

(defn- optional-args->map [optional-args]
  (when optional-args
    (let [first-opt (first optional-args)]
      (if (map? first-opt)
        first-opt
        (apply hash-map optional-args)))))




;;; Play / Stop

(defn stop-bgm! [& [fade-sec bgm-channel-id]]
  (init!)
  (bgm/stop! bgm-channel-id (or fade-sec (state/get :default-bgm-fade-sec)))
  true)


(defn bgm! [path & optional-args]
  (init!)
  (let [options (optional-args->map optional-args)]
    (when-not (and
                (state/get :disable-mobile?)
                (:mobile util/terminal-type))
      (if (empty-path? path) ; pathがnilの時はstop-bgm!を呼ぶ
        (stop-bgm! (:channel options))
        (bgm/play! (util/path-key->path path) options))))
  true)


(defn bgm-oneshot! [path & optional-args]
  (let [options (merge {:oneshot? true}
                       (optional-args->map optional-args))]
    (bgm! path options)))


(defn bgm-fadein! [path & optional-args]
  (let [options (merge {:fadein (state/get :default-bgm-fade-sec)}
                       (optional-args->map optional-args))]
    (bgm! path options)))


(defn bgm-position [& [bgm-channel-id include-loop-amount?]]
  (bgm/pos bgm-channel-id include-loop-amount?))



(defn stop-se! [& [fade-sec se-channel-id]]
  (init!)
  (if (nil? se-channel-id)
    (doseq [sid (se/playing-se-channel-ids)]
      (stop-se! fade-sec sid))
    (se/stop! se-channel-id (or fade-sec (state/get :default-se-fade-sec))))
  true)


(defn se! [path & optional-args]
  (init!)
  (when-not (and
              (state/get :disable-mobile?)
              (:mobile util/terminal-type))
    (when-not (empty-path? path) ; pathがnilの時は何もしない
      (let [options (optional-args->map optional-args)
            se-channel-id (se/play! (util/path-key->path path) options)]
        (reset! a-last-played-se-channel-id se-channel-id)
        se-channel-id))))


(defn alarm! [& [path & optional-args]]
  (init!)
  (let [options (merge {:alarm? true}
                       (optional-args->map optional-args))]
    (se! path options)))





;;; For backward compatibility

(defn me! [path & optional-args]
  (let [options (merge {:oneshot? true}
                       (optional-args->map optional-args))]
    (bgm! path options)))


(defn bgs! [path & optional-args]
  (let [options (merge {:channel :BGS}
                       (optional-args->map optional-args))]
    (bgm! path options)))




;;; Preload / Unload

(defn load! [path]
  (init!)
  (when-not (empty-path? path)
    (when-not (and
                (state/get :disable-mobile?)
                (:mobile util/terminal-type))
      (cache/load! (util/path-key->path path))
      true)))


(defonce ^:dynamic dont-process-next-bgm? false)

(defn unload! [path]
  (init!)
  (when-not (empty-path? path)
    ;; NB: まだ鳴っている最中に呼ばないように、このタイミングでBGM/SEで
    ;;     これを鳴らしているチャンネルがないかどうか調べ、
    ;;     もしあれば即座に停止させる必要がある。
    ;;     この処理はcache側には入れられない(モジュール参照の都合で)。
    ;;     またrace conditionwだが、「対象が現在再生中のBGM」かつ
    ;;     「フェードアウト中」かつ「次のBGMが予約済」かつ
    ;;     「unload-all!からの呼び出しでない」全てを満たすのであれば、
    ;;     BGMの停止後に「次のBGM」を再生する。
    (let [path (util/path-key->path path)
          next-bgm-options (when-not dont-process-next-bgm?
                             (doall
                               (filter identity
                                       (map (fn [[ch m]]
                                              (and
                                                @m
                                                (= path (:path (:current-param @m)))
                                                (neg? (:fade-delta @m))
                                                (when-let [np (:next-param @m)]
                                                  (assoc np :channel ch))))
                                            @bgm/channel-state-table))))]
      (bgm/stop-for-unload! path)
      (se/unload! path)
      (cache/unload! path)
      (doseq [o next-bgm-options]
        ;; TODO: 一瞬再生された後ですぐ止まってしまった。どうすればよい？
        (bgm/play! (:path o) o))
      true)))


(defn loaded? [path]
  (cache/loaded? (util/path-key->path path)))


(defn error? [path]
  (cache/error? (util/path-key->path path)))


(defn unload-all! []
  (init!)
  (binding [dont-process-next-bgm? true]
    (doseq [path (cache/all-pathes)]
      (unload! path))))





;;; Settings

(defn- must-be-sec! [k v]
  (assert (number? v) (str (name k) " must be non-negative number"))
  (max 0 v))

(defn- must-be-volume! [k v]
  (assert (number? v) (str (name k) " must be 0.0-1.0 number"))
  (max 0 (min 1 v)))

(defn- autoext-value-msg [k entry]
  (str (pr-str entry) " must be "
       '["ext" "mime/type"]
       " or"
       (apply str (map #(str " " (pr-str %)) (keys util/autoext-table)))
       " in " (name k)))

(def ^:private config-fns
  {:debug? state/set!
   :debug-verbose? state/set!
   :se-chattering-sec (fn [k v]
                        (let [v (must-be-sec! k v)]
                          (state/set! k v)))
   :default-bgm-fade-sec (fn [k v]
                           (let [v (must-be-sec! k v)]
                             (state/set! k v)))
   :default-se-fade-sec (fn [k v]
                          (let [v (must-be-sec! k v)]
                            (state/set! k v)))
   :volume-master (fn [k v]
                    (let [v (must-be-volume! k v)]
                      (state/set! k v)
                      (se/sync-volume!)
                      (bgm/sync-volume!)))
   :volume-bgm (fn [k v]
                 (let [v (must-be-volume! k v)]
                   (state/set! k v)
                   (bgm/sync-volume!)))
   :volume-se (fn [k v]
                (let [v (must-be-volume! k v)]
                  (state/set! k v)
                  (se/sync-volume!)))
   :autoext-list (fn [k v]
                   (assert (vector? v)
                           (str k " must be vector"))
                   (doseq [entry v]
                     (assert (or
                               (vector? entry)
                               (string? entry))
                             (autoext-value-msg k entry))
                     (when (vector? entry)
                       (assert (and
                                 (string? (first entry))
                                 (string? (second entry)))
                               (autoext-value-msg k entry)))
                     (when (string? entry)
                       (assert (util/autoext-table entry)
                               (autoext-value-msg k entry))))
                   (stop-bgm! 0 nil)
                   (stop-se! 0 nil)
                   (unload-all!)
                   (state/set! :resolved-autoext-list nil)
                   (state/set! k v))
   :dont-stop-on-background? (fn [k v]
                               (when (and
                                       (not
                                         (state/get :dont-stop-on-background?))
                                       v
                                       (state/get :in-background?))
                                 (bgm/sync-background! false true))
                               (state/set! k v))
   :disable-mobile? (fn [k v]
                      (state/set! k v)
                      (when (:mobile util/terminal-type)
                        (stop-bgm! 0 nil)
                        (stop-se! 0 nil)
                        (unload-all!)))
   :disable-webaudio? (fn [k v]
                        (stop-bgm! 0 nil)
                        (stop-se! 0 nil)
                        (unload-all!)
                        (state/set! k v)
                        (init-force!))
   :disable-htmlaudio? (fn [k v]
                         (stop-bgm! 0 nil)
                         (stop-se! 0 nil)
                         (unload-all!)
                         (state/set! k v)
                         (init-force!))
   :additional-query-string state/set!
   :path-prefix state/set!
   })


(defn set-config! [k v & kvs]
  (init!)
  (let [config-fn (config-fns k)]
    (assert config-fn (str "Invalid config key " k))
    (config-fn k v)
    (if (empty? kvs)
      true
      (apply set-config! kvs))))


(defn config [k]
  (init!)
  (assert (config-fns k) (str "Invalid config key " k))
  (state/get k))







;;; Utilities

;;; 再生環境種別の入ったset。今のところ、以下の各値が入る
;;; #{:tablet :mobile :android :ios :chrome :firefox}
(def terminal-type util/terminal-type)

;;; 指定したmimeの音源ファイルが再生可能かを返す。
;;; "audio/ogg" のような値を指定する。
(defn can-play? [mime] (util/can-play? mime))

;;; can-play? の、特定拡張子に特化したバージョン
(defn can-play-ogg? [] (util/can-play-ogg?))
(defn can-play-mp3? [] (util/can-play-mp3?))
(defn can-play-m4a? [] (util/can-play-m4a?))

;;; 0.0～1.0 の値と 0～100 のパーセント値を相互変換する。ボリューム値用。
(def float->percent util/float->percent)
(def percent->float util/percent->float)

;;; 現在利用しているデバイス名を文字列で返す
(defn current-device-name []
  (init!)
  (device/call! :name))




;;; エンジン音、プロペラ回転音、機関銃音のような
;;; 「動作中は定期的に再生する」タイプのSEの為に、
;;; 「動作中は定期的に実行するが、実行が集中しないように、指定したsec間隔
;;; 以内での再生を抑制する」関数を生成する高階関数
;;; (なお終了時に明示的に停止させるようなインターフェースが望ましい場合は、
;;; 専用チャンネルでループBGMとして再生させた方がより良い)
;;; 再生SEのパラメータは、高階関数のオプショナル引数として渡してもよいし、
;;; 生成された関数の引数として渡してもよい(こちらに何も渡されなかった場合は、
;;; 高階関数のオプショナル引数の方が適用される)
(defn make-play-se-periodically [interval-sec & se-key+args]
  (let [interval-msec (* interval-sec 1000)
        last-play-timestamp (atom 0)
        play-se! (fn [& override-se-key+args]
                   (let [now (js/Date.now)]
                     (when (< (+ @last-play-timestamp interval-msec) now)
                       (reset! last-play-timestamp now)
                       (apply se! (or override-se-key+args se-key+args)))))]
    play-se!))

;;; 一人のキャラが複数のボイスを再生するような場合は、同時再生を一つだけに抑制
;;; したいケースがほとんど。これを実現する関数を再生元ごとに生成する高階関数
;;; (あるボイスを再生中に別のボイスの再生を行う際に、fade-sec(省略可能)かけて
;;; 先のボイスを自動停止させるようになる)
;;; ※同じボイスを再生させようとした場合でも、先のボイスを停止させてから
;;; 同じボイスを再生開始する処理となる。ここがBGM系と違うのでちょっと注意
(defn make-play-se-personally [& [fade-sec]]
  (let [latest-se-channel-id (atom nil)
        play-se! (fn [& args]
                   (when-let [ch @latest-se-channel-id]
                     (stop-se! fade-sec ch))
                   (reset! latest-se-channel-id (apply se! args)))]
    play-se!))



;;; ロード済音源の総再生秒数を取得する。ロード済でない等の場合はnilが返る。
(defn length [path]
  (cache/length (util/path-key->path path)))


