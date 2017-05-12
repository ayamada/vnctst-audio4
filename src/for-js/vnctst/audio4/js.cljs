(ns vnctst.audio4.js
  (:refer-clojure :exclude [load])
  (:require-macros [project-clj.core :as project-clj])
  (:require [vnctst.audio4 :as audio4]))

;;; javascript向けのインターフェースを提供する
;;; - 関数名、変数名をjs風の名前に変更
;;; - 必要であれば、js向けに、引数/返り値の変換を行う
;;;   (keywordやmapが扱えない対策として)





(defn ^:export stopBgm [& [fade-sec ch]]
  (audio4/stop-bgm! fade-sec ch))

(defn ^:export bgm [path & [opt]]
  (audio4/bgm! path (js->clj opt :keywordize-keys true)))

(defn ^:export bgmOneshot [path & [opt]]
  (audio4/bgm-oneshot! path (js->clj opt :keywordize-keys true)))

(defn ^:export bgmFadein [path & [opt]]
  (audio4/bgm-fadein! path (js->clj opt :keywordize-keys true)))

(defn ^:export me [path & [opt]]
  (audio4/me! path (js->clj opt :keywordize-keys true)))

(defn ^:export bgs [path & [opt]]
  (audio4/bgs! path (js->clj opt :keywordize-keys true)))




(defn ^:export stopSe [& [fade-sec ch]]
  (audio4/stop-se! fade-sec ch))

(defn ^:export se [path & [opt]]
  (audio4/se! path (js->clj opt :keywordize-keys true)))

(defn ^:export alarm [path & [opt]]
  (audio4/alarm! path (js->clj opt :keywordize-keys true)))




(defn ^:export load [path] (audio4/load! path))
(defn ^:export unload [path] (audio4/unload! path))
(defn ^:export unloadAll [] (audio4/unload-all!))
(defn ^:export isLoaded [path] (audio4/loaded? path))
(defn ^:export isError [path] (audio4/error? path))



(defn ^:export getConfig [k]
  (clj->js (audio4/config (keyword k))))

(defn ^:export setConfig [k v]
  (audio4/set-config! (keyword k) (js->clj v)))









(def ^:export version (project-clj/get :version))

;;; NB: jsではsetを扱えないので関数化しておく
(defn ^:export hasTerminalType [k]
  (boolean (get audio4/terminal-type (keyword k))))

(defn ^:export canPlay [mime] (audio4/can-play? mime))
(defn ^:export canPlayOgg [] (audio4/can-play-ogg?))
(defn ^:export canPlayMp3 [] (audio4/can-play-mp3?))
(defn ^:export canPlayM4a [] (audio4/can-play-m4a?))
(def ^:export floatToPercent audio4/float->percent)
(def ^:export percentToFloat audio4/percent->float)
(def ^:export currentDeviceName audio4/current-device-name)





;;; 以下の二つは上手くラッピングできないので、普通に移植する

(defn ^:export makePlaySePeriodically [interval-sec & path+opt]
  (let [interval-msec (* interval-sec 1000)
        last-play-timestamp (atom 0)
        play-se! (fn [& override-path+opt]
                   (let [now (js/Date.now)]
                     (when (< (+ @last-play-timestamp interval-msec) now)
                       (reset! last-play-timestamp now)
                       (apply se (or override-path+opt path+opt)))))]
    play-se!))

(defn ^:export makePlaySePersonally [& [fade-sec]]
  (let [latest-se-channel-id (atom nil)
        play-se! (fn [& args]
                   (when-let [ch @latest-se-channel-id]
                     (stopSe fade-sec ch))
                   (reset! latest-se-channel-id (apply se args)))]
    play-se!))



