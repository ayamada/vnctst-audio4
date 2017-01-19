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
      (if (empty? path) ; pathがnilの時はstop-bgm!を呼ぶ
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
    (when-not (empty? path) ; pathがnilの時は何もしない
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
  (let [options (merge {:channel "BGS"}
                       (optional-args->map optional-args))]
    (bgm! path options)))




;;; Preload / Unload

(defn load! [path]
  (init!)
  (when-not (empty? path)
    (when-not (and
                (state/get :disable-mobile?)
                (:mobile util/terminal-type))
      (cache/load! (util/path-key->path path))
      true)))


(defn unload! [path]
  (init!)
  (when-not (empty? path)
    (let [path (util/path-key->path path)]
      ;; NB: まだ鳴っている最中に呼ばないように、このタイミングでBGM/SEで
      ;;     これを鳴らしているチャンネルがないかどうか調べ、
      ;;     もしあれば即座に停止させる必要がある。
      ;;     この処理はcache側には入れられない(モジュール参照の都合で)
      (bgm/stop-for-unload! path)
      (se/unload! path)
      (cache/unload! path)
      true)))


(defn loaded? [path]
  (cache/loaded? (util/path-key->path path)))


(defn error? [path]
  (cache/error? (util/path-key->path path)))


(defn unload-all! []
  (init!)
  (doseq [path (cache/all-pathes)]
    (unload! path)))





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
                   (stop-bgm! nil 0)
                   (stop-se! nil 0)
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
                        (stop-bgm! nil 0)
                        (stop-se! nil 0)
                        (unload-all!)))
   :disable-webaudio? (fn [k v]
                        (stop-bgm! nil 0)
                        (stop-se! nil 0)
                        (unload-all!)
                        (state/set! k v)
                        (init-force!))
   :disable-htmlaudio? (fn [k v]
                         (stop-bgm! nil 0)
                         (stop-se! nil 0)
                         (unload-all!)
                         (state/set! k v)
                         (init-force!))
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






