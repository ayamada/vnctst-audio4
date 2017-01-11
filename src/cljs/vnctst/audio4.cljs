(ns vnctst.audio4
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [vnctst.audio4.util :as util]
            [vnctst.audio4.device :as device]
            ;[vnctst.audio4.common :as common]
            ;[vnctst.audio4.se :as se]
            ;[vnctst.audio4.bgm :as bgm]
            [cljs.core.async :as async :refer [>! <!]]
            ))

;;; Internal

(defn- init-force! []
  (device/init! (state/get :disable-webaudio?)
                (state/get :disable-htmlaudio?))
  ;; TODO
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


(defn bgm! [path & optional-args]
  (init!)
  (let [options (optional-args->map optional-args)
        ]
    ;; TODO
    true))



(defn bgm-oneshot! [path & optional-args]
  (let [options (merge {:oneshot? true}
                       (optional-args->map optional-args))]
    (bgm! path options)))



(defn stop-bgm! [& [bgm-channel-id fade-sec]]
  (init!)
  ;; TODO
  true)




(defn se! [path & optional-args]
  (init!)
  (let [options (optional-args->map optional-args)
        ]
    ;; TODO
    true))


(defn alarm! [& [path & optional-args]]
  (init!)
  (let [options (optional-args->map optional-args)
        ]
    ;; TODO
    true))


(defn stop-se! [& [se-channel-obj fade-sec]]
  (init!)
  ;; TODO
  true)




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
  ;; TODO
  true)


(defn unload! [path]
  (init!)
  ;; TODO
  true)


(defn loaded? [path]
  (init!)
  ;; TODO
  nil)


(defn error? [path]
  (init!)
  ;; TODO
  nil)


(defn unload-all! []
  (init!)
  ;; TODO
  nil)


;;; Settings

(def ^:private config-fns
  {;; これらはstateの変更のみで対応可能
   :debug? state/set!
   :se-chattering-sec state/set!
   :default-bgm-fade-sec state/set!
   :default-se-fade-sec state/set!
   ;; TODO: volume系はsync操作が必要
   :volume-master (fn [k v]
                    (state/set! k v))
   :volume-bgm (fn [k v]
                 (state/set! k v))
   :volume-se (fn [k v]
                (state/set! k v))
   ;; TODO: 既存プリロードの全破棄が必要(再生中のものはどうする？)
   :autoext-list (fn [k v]
                   (state/set! k v))
   ;; TODO: 既にバックグラウンドの時のみ、特別扱いが必要
   :dont-stop-on-background? (fn [k v]
                               (state/set! k v))
   ;; NB: モバイル環境の場合、既に再生中のものを全て止める必要がある
   :disable-mobile? (fn [k v]
                      (state/set! k v)
                      (when (:mobile util/terminal-type)
                        (stop-bgm! nil 0)
                        (stop-se! nil 0)
                        (unload-all!)))
   ;; NB: 現在がwebaudioモードの場合、全てのやり直しが必要
   :disable-webaudio? (fn [k v]
                        (stop-bgm! nil 0)
                        (stop-se! nil 0)
                        (unload-all!)
                        (state/set! k v)
                        (init-force!))
   ;; NB: 現在がhtmlaudioモードの場合、全てのやり直しが必要
   :disable-htmlaudio? (fn [k v]
                         (stop-bgm! nil 0)
                         (stop-se! nil 0)
                         (unload-all!)
                         (state/set! k v)
                         (init-force!))
   })


(defn set-config! [k v]
  (init!)
  (let [config-fn (config-fns k)]
    (assert config-fn (str "Invalid config key " k))
    (config-fn k v)
    true))


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






