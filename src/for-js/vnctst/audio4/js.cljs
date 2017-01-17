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





;;; 引数としてjs-objを受ける、古いサンプル
;(defn- conv-kp [key-or-path]
;  (if-not (= js/Object (type key-or-path))
;    key-or-path
;    (let [m (js->clj key-or-path)
;          [k v] (first m)]
;      ;(assert (= 1 (count m)))
;      (assert (not (nil? k)) "k must be not null")
;      (assert (not (nil? v)) "v must be not null")
;      (keyword (str k) (str v)))))







(def ^:export version (project-clj/get :version))

(defn ^:export hasTerminalType [k]
  (boolean (get audio4/terminal-type (keyword k))))

(defn ^:export canPlay [mime] (audio4/can-play? mime))
(defn ^:export canPlayOgg [] (audio4/can-play-ogg?))
(defn ^:export canPlayMp3 [] (audio4/can-play-mp3?))
(defn ^:export canPlayM4a [] (audio4/can-play-m4a?))
;
;;;; 0.0～1.0 の値と 0～100 のパーセント値を相互変換する。ボリューム値用。
(def ^:export floatToPercent audio4/float->percent)
(def ^:export percentToFloat audio4/percent->float)








