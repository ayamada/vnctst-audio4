(ns vnctst.audio4.js
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



(defn ^:export getConfig [k] (audio4/config (keyword k)))
(defn ^:export setConfig [k v]
  ;; TODO: 一部のkに応じて、vの値を改変する必要あり！あとで…
  ;; 具体的には、 autoext-list の場合に対応が必要となる
  (audio4/set-config! (keyword k) v))

;(defn ^:export init [& [option]]
;  (if-let [option (when option
;                    (js->clj option :keywordize-keys true))]
;    (audio4/init! option)
;    (audio4/init!)))
;
;
;;;; jsではclojureのキーワードを簡単に指定できないので、
;;;; (play! :se/hoge) のようなものを簡潔に表現できない。そこで、
;;;; vnctst.audio4.js.play({se: "hoge"}) と指定できるようにする
;(defn- conv-kp [key-or-path]
;  (if-not (= js/Object (type key-or-path))
;    key-or-path
;    (let [m (js->clj key-or-path)
;          [k v] (first m)]
;      ;(assert (= 1 (count m)))
;      (assert (not (nil? k)) "k must be not null")
;      (assert (not (nil? v)) "v must be not null")
;      (keyword (str k) (str v)))))
;
;
;
;(defn ^:export bgm [key-or-path & [vol pitch pan]]
;  (audio4/bgm! (conv-kp key-or-path) vol pitch pan))
;
;(defn ^:export bgs [key-or-path & [vol pitch pan]]
;  (audio4/bgs! (conv-kp key-or-path) vol pitch pan))
;
;(defn ^:export me [key-or-path & [vol pitch pan]]
;  (audio4/me! (conv-kp key-or-path) vol pitch pan))
;
;(defn ^:export se [key-or-path & [vol pitch pan]]
;  (audio4/se! (conv-kp key-or-path) vol pitch pan))
;
;(def ^:export playBgm bgm)
;(def ^:export playBgs bgs)
;(def ^:export playMe me)
;(def ^:export playSe se)
;
;(defn ^:export alarm [key-or-path & [vol pitch pan]]
;  (audio4/alarm! (conv-kp key-or-path) vol pitch pan))
;
;(defn ^:export play [k & [vol pitch pan]]
;  (let [k (conv-kp k)]
;    (assert (keyword? k)) ; 文字列指定不可
;    (audio4/play! k vol pitch pan)))
;
;
;
;
;
;(defn ^:export isPlayingBgm [] (audio4/playing-bgm?))
;(defn ^:export isPlayingBgs [] (audio4/playing-bgs?))
;(defn ^:export isPlayingMe [] (audio4/playing-me?))
;
;
;(defn ^:export preloadBgm [key-or-path]
;  (audio4/preload-bgm! (conv-kp key-or-path)))
;(def ^:export preloadBgs preloadBgm)
;(def ^:export preloadMe preloadBgm)
;
;(defn ^:export unloadBgm [key-or-path]
;  (audio4/unload-bgm! (conv-kp key-or-path)))
;(def ^:export unloadBgs unloadBgm)
;(def ^:export unloadMe unloadBgm)
;
;(defn ^:export isPreloadedBgm [key-or-path]
;  (audio4/preloaded-bgm? (conv-kp key-or-path)))
;(defn ^:export isSucceededToPreloadBgm [key-or-path]
;  (audio4/succeeded-to-preload-bgm? (conv-kp key-or-path)))
;
;
;
;
;(defn ^:export isPreloadSe [key-or-path]
;  (audio4/preload-se! (conv-kp key-or-path)))
;(defn ^:export isUnloadSe [key-or-path]
;  (audio4/unload-se! (conv-kp key-or-path)))
;(defn ^:export isLoadedSe [key-or-path]
;  (audio4/loaded-se? (conv-kp key-or-path)))
;(defn ^:export isSucceededToLoadSe [key-or-path]
;  (audio4/succeeded-to-load-se? (conv-kp key-or-path)))







(def ^:export version (project-clj/get :version))

;; jsでsetを表現しづらいので、これは一旦非公開とする
(defn ^:export hasTerminalType [k]
  (get audio4/terminal-type (keyword k)))

(defn ^:export canPlay [mime] (audio4/can-play? mime))
(defn ^:export canPlayOgg [] (audio4/can-play-ogg?))
(defn ^:export canPlayMp3 [] (audio4/can-play-mp3?))
(defn ^:export canPlayM4a [] (audio4/can-play-m4a?))
;
;;;; 0.0～1.0 の値と 0～100 のパーセント値を相互変換する。ボリューム値用。
(def ^:export floatToPercent audio4/float->percent)
(def ^:export percentToFloat audio4/percent->float)





;;; NB: jsではプリセット情報の取得ができないので、プリセット関係は提供しない



