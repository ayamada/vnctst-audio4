(ns vnctst.audio4.util
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [cljs.core.async :as async :refer [>! <!]]
            ))



;;; percentage utilities

(defn float->percent [f]
  (when f
    (js/Math.round (* 100 f))))

(defn percent->float [percent]
  (when percent
    (/ percent 100)))





;;; https://w3g.jp/blog/js_browser_sniffing2015
(defn- detect-terminal-type-by-user-agent []
  (let [ua (js/window.navigator.userAgent.toLowerCase)
        has-not? #(neg? (.indexOf ua %))
        has? (complement has-not?)
        tablet? (or
                  (and (has? "windows") (has? "touch") (has-not? "tablet pc"))
                  (and (has? "ipad"))
                  (and (has? "android") (has-not? "mobile"))
                  (and (has? "firefox") (has? "tablet"))
                  (and (has? "kindle"))
                  (and (has? "silk"))
                  (and (has? "playbook"))
                  )
        mobile? (or
                  (and (has? "windows") (has? "phone"))
                  (and (has? "iphone"))
                  (and (has? "ipod"))
                  (and (has? "android") (has? "mobile"))
                  (and (has? "firefox") (has? "mobile"))
                  (and (has? "blackberry"))
                  )
        android? (or
                   (has? "android")
                   (has? "kindle")
                   (has? "silk"))
        ios? (or
               (has? "iphone")
               (has? "ipod")
               (has? "ipad"))
        chrome? (has? "chrome")
        firefox? (has? "firefox")]
    (into #{} (filter identity
                      [(and tablet? :tablet)
                       (and mobile? :mobile)
                       (and android? :android)
                       (and ios? :ios)
                       (and chrome? :chrome)
                       (and firefox? :firefox)
                       ]))))

(defonce terminal-type (detect-terminal-type-by-user-agent))




;;; :debug? フラグに関わらず、コンソールにログ出力する
(defn logging-force [& msgs]
  (when-let [c (aget js/window "console")]
    (when (aget c "log")
      (.log c (apply pr-str msgs)))))

;;; :debug? フラグがオンの時のみ、コンソールにログ出力する
(defn logging [& msgs]
  (when (state/get :debug?)
    (apply logging-force msgs)))

;;; :debug? debug-verbose? フラグがオンの時のみ、コンソールにログ出力する
(defn logging-verbose [& msgs]
  (when (and
          (state/get :debug?)
          (state/get :debug-verbose?))
    (apply logging-force msgs)))









(def can-play?
  (memoize
    (fn [mime]
      ;; NB: ここで new Audio() 相当を実行しているが、これはiOSにて問題が出る
      ;;     場合があるらしい
      ;;     http://qiita.com/gonshi_com/items/e41dbb80f5eb4c176108
      ;;     適切に回避する方法があれば回避したいところだが、よく分からない
      ;;     (これで回避できている？)
      (let [audio-class (or
                          (aget js/window "Audio")
                          (aget js/window "webkitAudio"))
            audio (when audio-class
                    (try
                      (new audio-class)
                      (catch :default e
                        nil)))]
        (when audio
          (not (empty? (.canPlayType audio mime))))))))

(defn can-play-ogg? [] (can-play? "audio/ogg"))
(defn can-play-mp3? [] (can-play? "audio/mpeg"))
(defn can-play-m4a? [] (can-play? "audio/mp4"))












;;; 実(内部)再生パラメータ(volume, pitch, pan)の算出
(defn calc-internal-params [mode volume & [pitch pan]]
  (let [volume-key ({:bgm :volume-bgm
                     :se :volume-se} mode)
        _ (assert volume-key (str "Invalid mode " mode))
        i-volume (max 0 (min 1 (* (or volume 1)
                                  (state/get :volume-master 0.6)
                                  (state/get volume-key 0.6))))
        i-pitch (max 0.1 (min 10 (or pitch 1)))
        i-pan (max -1 (min 1 (or pan 0)))]
    [i-volume i-pitch i-pan]))




;;; モバイルデバイスおよび最近のブラウザでは、インタラクションイベントを
;;; トリガーにして再生を行う事によるアンロック処理が必要となる。
;;; unlock-fnは、アンロックに成功したら真を返す事。
;;; (偽値を返す事で、次にまたリトライを行う)
;;; アンロックは一回行えば、それ以降は行わなくてもよい(らしい)
(defn register-touch-unlock-fn! [unlock-fn]
  ;; See http://ch.nicovideo.jp/indies-game/blomaga/ar1410968
  (let [event-names ["touchend" "mousedown" "keydown"]
        h (atom nil)]
    (reset! h (fn [e]
                (when (unlock-fn)
                  (doseq [ename event-names]
                    (js/document.removeEventListener ename @h))
                  (reset! h nil))))
    (doseq [ename event-names]
      (js/document.addEventListener ename @h))))



(def autoext-table
  {"ogg" "audio/ogg"
   "mp3" "audio/mpeg"
   "m4a" "audio/mp4"})

(defn- get-resolved-autoext-list []
  (if-let [resolved-autoext-list (state/get :resolved-autoext-list)]
    resolved-autoext-list
    (let [autoext-list (state/get :autoext-list)
          autoext-list (map (fn [entry]
                              (if (string? entry)
                                (if-let [mime (get autoext-table entry)]
                                  [entry mime]
                                  (throw (ex-info (str "Extension "
                                                       (pr-str entry)
                                                       " could not resolve"
                                                       " mime."
                                                       " Must specify"
                                                       " [" (pr-str entry)
                                                       " \"mime/type\"]"
                                                       " form.")
                                                  {})))
                                (if (and
                                      (vector? entry)
                                      (= 2 (count entry))
                                      (string? (first entry))
                                      (string? (second entry)))
                                  entry
                                  (throw (ex-info (str "Invalid entry "
                                                       (pr-str entry))
                                                  {})))))
                            autoext-list)
          autoext-list (filter (fn [[ext mime]]
                                 (can-play? mime))
                               autoext-list)]
      (state/set! :resolved-autoext-list autoext-list)
      autoext-list)))


(defn path-key->path [path-key]
  (if-not (keyword? path-key)
    path-key
    (if-let [dir (namespace path-key)]
      (str dir "/" (name path-key) ".*")
      (str (name path-key) ".*"))))


(defn expand-pathes [path]
  (cond
    (empty? path) nil
    (keyword? path) (expand-pathes (path-key->path path))
    (not (string? path)) (expand-pathes (str path))
    :else (let [[_ basename] (re-find #"^(.*)\.\*$" path)
                path-prefix (state/get :path-prefix)]
            (if-not basename
              [(str path-prefix path)]
              (if-let [resolved-autoext-list (get-resolved-autoext-list)]
                (mapv (fn [[ext mime]]
                        (str path-prefix basename "." ext))
                      resolved-autoext-list)
                nil)))))


;;; 簡易プリロードスケジューラ
(defn run-preload-process! [proc queue resolver]
  (when-not @proc
    (reset! proc true)
    (go-loop []
      (if (empty? @queue)
        (reset! proc nil)
        (let [one (first @queue)]
          (swap! queue rest)
          (<! (resolver one))
          (recur))))))







