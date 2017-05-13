(ns vnctst.audio4.demo
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [cljs.core.async :as async :refer [>! <!]]
            [clojure.string :as string]
            [vnctst.audio4 :as audio4]
            [vnctst.audio4.js :as audio4-js]
            [vnctst.audio4.prefetch :as audio4-prefetch :include-macros true]
            ))



(defonce display-js-mode? (atom true))


(defn- sync-device-name! []
  (when-let [dom (js/document.getElementById "device-name")]
    (let [msg (pr-str (audio4/current-device-name))]
      (set! (.. dom -textContent) msg))))



(def config-options
  [:debug? true
   :debug-verbose? true
   ])

(def preload-pathes
  (audio4-prefetch/pathlist-from-directory "resources/public/se/" "se/"))

(def button-assign (atom {}))
(defn- defba [k m]
  (swap! button-assign assoc k m))




;;; main

(defba :bgm-va32
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*")
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\")"
   :js "vnctst.audio4.js.bgm(\"bgm/va32.*\")"
   })

(defba :bgm-cntr
  {:fn #(vnctst.audio4/bgm! "bgm/cntr.*")
   :cljs "(vnctst.audio4/bgm! \"bgm/cntr.*\")"
   :js "vnctst.audio4.js.bgm(\"bgm/cntr.*\")"
   })

(defba :bgm-oneshot-ny2017
  {:fn #(vnctst.audio4/bgm-oneshot! "bgm/ny2017.*")
   :cljs "(vnctst.audio4/bgm-oneshot! \"bgm/ny2017.*\")"
   :js "vnctst.audio4.js.bgmOneshot(\"bgm/ny2017.*\")"
   })

(defba :bgm-fadein-cntr
  {:fn #(vnctst.audio4/bgm-fadein! "bgm/cntr.*")
   :cljs "(vnctst.audio4/bgm-fadein! \"bgm/cntr.*\")"
   :js "vnctst.audio4.js.bgmFadein(\"bgm/cntr.*\")"
   })

(defba :stop-bgm
  {:fn #(vnctst.audio4/stop-bgm!)
   :cljs "(vnctst.audio4/stop-bgm!)"
   :js "vnstst.audio4.js.stopBgm()"
   })

(defba :stop-bgm-3
  {:fn #(vnctst.audio4/stop-bgm! 3)
   :cljs "(vnctst.audio4/stop-bgm! 3)"
   :js "vnstst.audio4.js.stopBgm(3)"
   })

(defba :stop-bgm-0
  {:fn #(vnctst.audio4/stop-bgm! 0)
   :cljs "(vnctst.audio4/stop-bgm! 0)"
   :js "vnstst.audio4.js.stopBgm(0)"
   })

(defba :se-launch
  {:fn #(vnctst.audio4/se! "se/launch.*")
   :cljs "(vnctst.audio4/se! \"se/launch.*\")"
   :js "vnstst.audio4.js.se(\"se/launch.*\")"
   })

(defba :se-kick
  {:fn #(vnctst.audio4/se! "se/kick.*")
   :cljs "(vnctst.audio4/se! \"se/kick.*\")"
   :js "vnstst.audio4.js.se(\"se/kick.*\")"
   })

(defba :stop-se
  {:fn #(vnctst.audio4/stop-se!)
   :cljs "(vnctst.audio4/stop-se!)"
   :js "vnstst.audio4.js.stopSe()"
   })

(defba :stop-se-05
  {:fn #(vnctst.audio4/stop-se! 0.5)
   :cljs "(vnctst.audio4/stop-se! 0.5)"
   :js "vnstst.audio4.js.stopSe(0.5)"
   })


;;; configure


(defba :config-volume-master
  {:fn #(js/alert (vnctst.audio4/config :volume-master))
   :cljs "(vnctst.audio4/config :volume-master)"
   :js "vnctst.audio4.js.getConfig(\"volume-master\")"
   })

(defba :set-config-volume-master-100
  {:fn #(vnctst.audio4/set-config! :volume-master 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-master 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 1.0)"
   })

(defba :set-config-volume-master-25
  {:fn #(vnctst.audio4/set-config! :volume-master 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-master 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-master\", 0.25)"
   })

(defba :set-config-volume-bgm-100
  {:fn #(vnctst.audio4/set-config! :volume-bgm 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-bgm 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-bgm\", 1.0)"
   })

(defba :set-config-volume-bgm-25
  {:fn #(vnctst.audio4/set-config! :volume-bgm 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-bgm 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-bgm\", 0.25)"
   })

(defba :set-config-volume-se-100
  {:fn #(vnctst.audio4/set-config! :volume-se 1.0)
   :cljs "(vnctst.audio4/set-config! :volume-se 1.0)"
   :js "vnctst.audio4.js.setConfig(\"volume-se\", 1.0)"
   })

(defba :set-config-volume-se-25
  {:fn #(vnctst.audio4/set-config! :volume-se 0.25)
   :cljs "(vnctst.audio4/set-config! :volume-se 0.25)"
   :js "vnctst.audio4.js.setConfig(\"volume-se\", 0.25)"
   })

(defba :set-config-debug?-false
  {:fn #(vnctst.audio4/set-config! :debug? false)
   :cljs "(vnctst.audio4/set-config! :debug? false)"
   :js "vnctst.audio4.js.setConfig(\"debug?\", false)"
   })

(defba :set-config-debug?-true
  {:fn #(vnctst.audio4/set-config! :debug? true)
   :cljs "(vnctst.audio4/set-config! :debug? true)"
   :js "vnctst.audio4.js.setConfig(\"debug?\", true)"
   })

(defba :set-config-debug-verbose?-false
  {:fn #(vnctst.audio4/set-config! :debug-verbose? false)
   :cljs "(vnctst.audio4/set-config! :debug-verbose? false)"
   :js "vnctst.audio4.js.setConfig(\"debug-verbose?\", false)"
   })

(defba :set-config-debug-verbose?-true
  {:fn #(vnctst.audio4/set-config! :debug-verbose? true)
   :cljs "(vnctst.audio4/set-config! :debug-verbose? true)"
   :js "vnctst.audio4.js.setConfig(\"debug-verbose?\", true)"
   })

(defba :set-config-default-bgm-fade-sec-0
  {:fn #(vnctst.audio4/set-config! :default-bgm-fade-sec 0)
   :cljs "(vnctst.audio4/set-config! :default-bgm-fade-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"default-bgm-fade-sec\", 0)"
   })

(defba :set-config-default-bgm-fade-sec-05
  {:fn #(vnctst.audio4/set-config! :default-bgm-fade-sec 0.5)
   :cljs "(vnctst.audio4/set-config! :default-bgm-fade-sec 0.5)"
   :js "vnctst.audio4.js.setConfig(\"default-bgm-fade-sec\", 0.5)"
   })

(defba :set-config-default-se-fade-sec-0
  {:fn #(vnctst.audio4/set-config! :default-se-fade-sec 0)
   :cljs "(vnctst.audio4/set-config! :default-se-fade-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"default-se-fade-sec\", 0)"
   })

(defba :set-config-default-se-fade-sec-05
  {:fn #(vnctst.audio4/set-config! :default-se-fade-sec 0.5)
   :cljs "(vnctst.audio4/set-config! :default-se-fade-sec 0.5)"
   :js "vnctst.audio4.js.setConfig(\"default-se-fade-sec\", 0.5)"
   })

(defba :set-config-dont-stop-on-background?-false
  {:fn #(vnctst.audio4/set-config! :dont-stop-on-background? false)
   :cljs "(vnctst.audio4/set-config! :dont-stop-on-background? false)"
   :js "vnctst.audio4.js.setConfig(\"dont-stop-on-background?\", false)"
   })

(defba :set-config-dont-stop-on-background?-true
  {:fn #(vnctst.audio4/set-config! :dont-stop-on-background? true)
   :cljs "(vnctst.audio4/set-config! :dont-stop-on-background? true)"
   :js "vnctst.audio4.js.setConfig(\"dont-stop-on-background?\", true)"
   })

(defba :set-config-se-chattering-sec-0
  {:fn #(vnctst.audio4/set-config! :se-chattering-sec 0)
   :cljs "(vnctst.audio4/set-config! :se-chattering-sec 0)"
   :js "vnctst.audio4.js.setConfig(\"se-chattering-sec\", 0)"
   })

(defba :set-config-se-chattering-sec-05
  {:fn #(vnctst.audio4/set-config! :se-chattering-sec 0.5)
   :cljs "(vnctst.audio4/set-config! :se-chattering-sec 0.5)"
   :js "vnctst.audio4.js.setConfig(\"se-chattering-sec\", 0.5)"
   })

(defba :set-config-autoext-list-a
  {:fn #(vnctst.audio4/set-config! :autoext-list ["ogg"])
   :cljs "(vnctst.audio4/set-config! :autoext-list [\"ogg\"])"
   :js "vnctst.audio4.js.setConfig(\"autoext-list\", [\"ogg\"])"
   })

(defba :set-config-autoext-list-b
  {:fn #(vnctst.audio4/set-config! :autoext-list ["m4a" "mp3" ["wav" "audio/wav"] "ogg"])
   :cljs "(vnctst.audio4/set-config! :autoext-list [\"m4a\" \"mp3\" [\"wav\" \"audio/wav\"] \"ogg\")"
   :js "vnctst.audio4.js.setConfig(\"autoext-list\", [\"m4a\", \"mp3\", [\"wav\", \"audio/wav\"], \"ogg\")"
   })

(defba :set-config-disable-mobile?-false
  {:fn #(vnctst.audio4/set-config! :disable-mobile? false)
   :cljs "(vnctst.audio4/set-config! :disable-mobile? false)"
   :js "vnctst.audio4.js.setConfig(\"disable-mobile?\", false)"
   })

(defba :set-config-disable-mobile?-true
  {:fn #(vnctst.audio4/set-config! :disable-mobile? true)
   :cljs "(vnctst.audio4/set-config! :disable-mobile? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-mobile?\", true)"
   })

(defba :set-config-disable-webaudio?-false
  {:fn #(vnctst.audio4/set-config! :disable-webaudio? false)
   :cljs "(vnctst.audio4/set-config! :disable-webaudio? false)"
   :js "vnctst.audio4.js.setConfig(\"disable-webaudio?\", false)"
   })

(defba :set-config-disable-webaudio?-true
  {:fn #(vnctst.audio4/set-config! :disable-webaudio? true)
   :cljs "(vnctst.audio4/set-config! :disable-webaudio? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-webaudio?\", true)"
   })

(defba :set-config-disable-htmlaudio?-false
  {:fn #(vnctst.audio4/set-config! :disable-htmlaudio? false)
   :cljs "(vnctst.audio4/set-config! :disable-htmlaudio? false)"
   :js "vnctst.audio4.js.setConfig(\"disable-htmlaudio?\", false)"
   })

(defba :set-config-disable-htmlaudio?-true
  {:fn #(vnctst.audio4/set-config! :disable-htmlaudio? true)
   :cljs "(vnctst.audio4/set-config! :disable-htmlaudio? true)"
   :js "vnctst.audio4.js.setConfig(\"disable-htmlaudio?\", true)"
   })

(defba :set-config-additional-query-string
  {:fn #(vnctst.audio4/set-config! :additional-query-string "01234567")
   :cljs "(vnctst.audio4/set-config! :additional-query-string \"01234567\")"
   :js "vnctst.audio4.js.setConfig(\"additional-query-string\", \"01234567\")"
   })


;;; preload / unload


(defba :load-noise
  {:fn #(vnctst.audio4/load! "bgm/noise.*")
   :cljs "(vnctst.audio4/load! \"bgm/noise.*\")"
   :js "vnctst.audio4.js.load(\"bgm/noise.*\")"
   })

(defba :loaded?
  {:fn #(js/alert (vnctst.audio4/loaded? "bgm/noise.*"))
   :cljs "(vnctst.audio4/loaded? \"bgm/noise.*\")"
   :js "vnctst.audio4.js.isLoaded(\"bgm/noise.*\")"
   })

(defba :error?
  {:fn #(js/alert (vnctst.audio4/error? "bgm/noise.*"))
   :cljs "(vnctst.audio4/error? \"bgm/noise.*\")"
   :js "vnctst.audio4.js.isError(\"bgm/noise.*\")"
   })

(defba :unload-noise
  {:fn #(vnctst.audio4/unload! "bgm/noise.*")
   :cljs "(vnctst.audio4/unload! \"bgm/noise.*\")"
   :js "vnctst.audio4.js.unload(\"bgm/noise.*\")"
   })

(defba :unload-all
  {:fn #(vnctst.audio4/unload-all!)
   :cljs "(vnctst.audio4/unload-all!)"
   :js "vnctst.audio4.js.unloadAll()"
   })


;;; more BGM


(defba :bgm-option-a
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*" {:volume 0.5 :pitch 1.5 :pan -0.5})
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\" {:volume 0.5 :pitch 1.5 :pan -0.5})"
   :js "vnstst.audio4.js.bgm(\"bgm/va32.*\", {volume: 0.5, pitch: 1.5, pan: -0.5})"
   })

(defba :bgm-option-b
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*" :volume 1.0 :pitch 1.0 :pan 0.5)
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\" :volume 1.0 :pitch 1.0 :pan 0.5)"
   :js "vnstst.audio4.js.bgm(\"bgm/va32.*\", {volume: 1.0, pitch: 1.0, pan: 0.5})"
   })

(defba :bgm-option-c
  {:fn #(vnctst.audio4/bgm! "bgm/va32.*" :volume 1.5 :pitch 0.5 :pan 0)
   :cljs "(vnctst.audio4/bgm! \"bgm/va32.*\" :volume 1.5 :pitch 0.5 :pan 0)"
   :js "vnstst.audio4.js.bgm(\"bgm/va32.*\", {volume: 1.5, pitch: 0.5, pan: 0})"
   })

(defba :bgm-noise-ch
  {:fn #(vnctst.audio4/bgm! "bgm/noise.*" :channel :background-sound)
   :cljs "(vnctst.audio4/bgm! \"bgm/noise.*\" :channel :background-sound)"
   :js "vnctst.audio4.js.bgm(\"bgm/noise.*\", {channel: \"background-sound\"})"
   })

(defba :stop-bgm-ch-a
  {:fn #(vnctst.audio4/stop-bgm! nil :background-sound)
   :cljs "(vnctst.audio4/stop-bgm! nil :background-sound)"
   :js "vnstst.audio4.js.stopBgm(null, \"background-sound\")"
   })

(defba :stop-bgm-ch-b
  {:fn #(vnctst.audio4/stop-bgm! 0.25 :BGM)
   :cljs "(vnctst.audio4/stop-bgm! 0.25 :BGM)"
   :js "vnstst.audio4.js.stopBgm(0.25, \"BGM\")"
   })


;;; more SE


(defba :se-option-a
  {:fn #(vnctst.audio4/se! "se/launch.*" {:volume 0.5 :pitch 2.0 :pan -0.5})
   :cljs "(vnctst.audio4/se! \"se/launch.*\" {:volume 0.5 :pitch 2.0 :pan -0.5})"
   :js "vnstst.audio4.js.se(\"se/launch.*\", {volume: 0.5, pitch: 2.0, pan: -0.5})"
   })

(defba :se-option-b
  {:fn #(vnctst.audio4/se! "se/launch.*" :volume 1.0 :pitch 1.0 :pan 0.5)
   :cljs "(vnctst.audio4/se! \"se/launch.*\" :volume 1.0 :pitch 1.0 :pan 0.5)"
   :js "vnstst.audio4.js.se(\"se/launch.*\", {volume: 1.0, pitch: 1.0, pan: 0.5})"
   })

(defba :se-option-c
  {:fn #(vnctst.audio4/se! "se/launch.*" :volume 1.5 :pitch 0.5 :pan 0)
   :cljs "(vnctst.audio4/se! \"se/launch.*\" :volume 1.5 :pitch 0.5 :pan 0)"
   :js "vnstst.audio4.js.se(\"se/launch.*\", {volume: 1.5, pitch: 0.5, pan: 0})"
   })

(defba :stop-se-ch
  {:fn #(when-let [se-ch (vnctst.audio4/last-played-se-channel-id)]
          (vnctst.audio4/stop-se! 0 se-ch))
   :cljs "(vnctst.audio4/stop-se! 0 se-channel-id)"
   :js "vnstst.audio4.js.stopSe(0, seChannelId)"
   })

(defba :alarm-kick
  {:fn #(vnctst.audio4/alarm! "se/kick.*")
   :cljs "(vnctst.audio4/alarm! \"se/kick.*\")"
   :js "vnstst.audio4.js.alarm(\"se/kick.*\")"
   })


;;; misc 1


(defba :version-js
  {:fn #(js/alert vnctst.audio4.js/version)
   :cljs "----"
   :js "vnctst.audio4.js.version"
   })

(defba :can-play-ogg
  {:fn #(js/alert (vnctst.audio4/can-play-ogg?))
   :cljs "(vnctst.audio4/can-play-ogg?)"
   :js "vnctst.audio4.js.canPlayOgg()"
   })

(defba :can-play-mp3
  {:fn #(js/alert (vnctst.audio4/can-play-mp3?))
   :cljs "(vnctst.audio4/can-play-mp3?)"
   :js "vnctst.audio4.js.canPlayMp3()"
   })

(defba :can-play-m4a
  {:fn #(js/alert (vnctst.audio4/can-play-m4a?))
   :cljs "(vnctst.audio4/can-play-m4a?)"
   :js "vnctst.audio4.js.canPlayM4a()"
   })

(defba :can-play
  {:fn #(js/alert (vnctst.audio4/can-play? "audio/wav"))
   :cljs "(vnctst.audio4/can-play? \"audio/wav\")"
   :js "vnctst.audio4.js.canPlay(\"audio/wav\")"
   })

(defba :terminal-type
  {:fn #(js/alert
          (boolean (vnctst.audio4/terminal-type :firefox)))
   :cljs "(vnctst.audio4/terminal-type :firefox)"
   :js "vnctst.audio4.js.hasTerminalType(\"firefox\")"
   })

(defba :float->percent
  {:fn #(js/alert (vnctst.audio4/float->percent 0.25))
   :cljs "(vnctst.audio4/float->percent 0.25)"
   :js "vnctst.audio4.js.floatToPercent(0.25)"
   })

(defba :percent->float
  {:fn #(js/alert (vnctst.audio4/percent->float 25))
   :cljs "(vnctst.audio4/percent->float 25)"
   :js "vnctst.audio4.js.percentToFloat(25)"
   })

(defba :current-device-name
  {:fn #(js/alert (vnctst.audio4/current-device-name))
   :cljs "(vnctst.audio4/current-device-name)"
   :js "vnctst.audio4.js.currentDeviceName()"
   })

(defonce s1 (atom nil))

(defba :make-play-se-periodically
  {:fn (fn []
         (when-not @s1
           (reset! s1 (vnctst.audio4/make-play-se-periodically 1.0 "se/launch.*")))
         (@s1))
   :cljs "(defonce s1 (vnctst.audio4/make-play-se-periodically 1.0 \"se/launch.*\")) (s1)"
   :js "var s1 = window.s1 || vnctst.audio4.js.makePlaySePeriodically(1.0, \"se/launch.*\"); s1()"
   })

(defonce s2 (atom nil))

(defba :make-play-se-personally
  {:fn (fn []
         (when-not @s2
           (reset! s2 (vnctst.audio4/make-play-se-personally)))
         (@s2 "se/launch.*"))
   :cljs "(defonce s2 (vnctst.audio4/make-play-se-personally)) (s2 \"se/launch.*\")"
   :js "var s2 = window.s2 || vnctst.audio4.js.makePlaySePersonally(); s2(\"se/launch.*\")"
   })


;;; misc 2


(defba :bgm-option-d
  {:fn #(vnctst.audio4/bgm! "bgm/ny2017.*" :oneshot? true :fadein 1.5)
   :cljs "(vnctst.audio4/bgm! \"bgm/ny2017.*\" :oneshot? true :fadein 1.5)"
   :js "vnstst.audio4.js.bgm(\"bgm/ny2017.*\", {\"oneshot?\": true, fadein 1.5})"
   })

(defba :me-launch
  {:fn #(vnctst.audio4/me! "se/launch.*")
   :cljs "(vnctst.audio4/me! \"se/launch.*\")"
   :js "vnctst.audio4.js.me(\"se/launch.*\")"
   })

(defba :bgs-noise
  {:fn #(vnctst.audio4/bgs! "bgm/noise.*")
   :cljs "(vnctst.audio4/bgs! \"bgm/noise.*\")"
   :js "vnctst.audio4.js.bgs(\"bgm/noise.*\")"
   })

(defba :se-kick-keyword
  {:fn #(vnctst.audio4/se! :se/kick)
   :cljs "(vnctst.audio4/se! :se/kick)"
   :js "----"
   })









(defn- sync-button-labels! []
  (when-let [dom (js/document.getElementById "terminal-flags")]
    (let [msg (if @display-js-mode?
                (string/join ", " (map (comp pr-str name)
                                       audio4/terminal-type))
                (string/join " " (map pr-str audio4/terminal-type)))]
      (set! (.. dom -textContent) msg)))
  (sync-device-name!)
  (when-let [dom (js/document.getElementById "config-info")]
    (let [config-map (apply hash-map config-options)
          msg (if @display-js-mode?
                (string/join "; "
                             (map (fn [[k v]]
                                    (str "setConfig(" (pr-str (name k))
                                         ", " (pr-str v) ")"))
                                  config-map))
                (string/join " "
                             (map (fn [[k v]]
                                    (str "(set-config! " k
                                         " " (pr-str v) ")"))
                                  config-map)))]
      (set! (.. dom -textContent) msg)))
  (when-let [dom (js/document.getElementById "preload-info")]
    (let [msg (if @display-js-mode?
                (str "[" (string/join ", " (map pr-str preload-pathes)) "]")
                (str "[" (string/join " " (map pr-str preload-pathes)) "]"))]
      (set! (.. dom -textContent) msg)))
  (doseq [[k m] (seq @button-assign)]
    (when-let [dom (js/document.getElementById (name k))]
      ;(js/addEventListener dom "click" (:fn m))
      (aset dom "onclick" (:fn m))
      (set! (.. dom -textContent) (if @display-js-mode?
                                    (:js m)
                                    (:cljs m))))
    ;(when-let [dom (js/document.getElementById (str (name k) "-desc"))]
    ;  (set! (.. dom -textContent) (:desc m)))
    ))


(defn- display-msg! [msg & more-msgs]
  (when-let [dom (js/document.getElementById "message")]
    (set! (.. dom -textContent) (apply print-str msg more-msgs))))

(defn- display-version! []
  (when-let [dom (js/document.getElementById "version")]
    (set! (.. dom -textContent) (str "Version: "
                                     audio4-js/version))))

(defn- show-floating-header! []
  (when-let [dom (js/document.getElementById "floating-header")]
    (set! (.. dom -style -display) "block")))

(defn- show-buttons! []
  (when-let [dom (js/document.getElementById "main")]
    (set! (.. dom -style -display) "block")))



(def ^:private folding-defines
  [[:h-introduction :introduction]
   [:h-preparation :preparation]])

(defn- add-folding! [label-key content-key]
  (let [visible? (atom false)
        label-dom (js/document.getElementById (name label-key))
        content-dom (js/document.getElementById (name content-key))
        apply-visible! #(when content-dom
                          (set! (.. content-dom -style -display)
                                (if @visible?
                                  "block"
                                  "none")))
        listener (fn [e]
                   (.preventDefault e)
                   (.stopPropagation e)
                   (swap! visible? not)
                   (apply-visible!))]
    (apply-visible!)
    (when label-dom
      (.addEventListener label-dom "click" listener)
      (.addEventListener label-dom "touchend" listener))
    nil))



(defn ^:export jsmode [bool]
  (reset! display-js-mode? bool)
  (sync-button-labels!))

(defn ^:export bootstrap []
  (sync-button-labels!)
  (doseq [[k1 k2] folding-defines]
    (add-folding! k1 k2))
  (apply audio4/set-config! config-options)
  ;; プリセットのプリロードとロード待ちを行う
  (let [target-num (count preload-pathes)
        display-progress! #(display-msg! (str "Loading ... "
                                              %
                                              " / "
                                              target-num))]
    (display-version!)
    (display-progress! 0)
    (doseq [path preload-pathes]
      (audio4/load! path))
    (go-loop []
      (<! (async/timeout 200))
      (let [c (count (filter audio4/loaded? preload-pathes))]
        (display-progress! c)
        (if (< c target-num)
          (recur)
          (do
            (show-floating-header!)
            (show-buttons!)
            (display-msg! "Loaded.")))))))



