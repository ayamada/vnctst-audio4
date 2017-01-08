(ns vnctst.audio4.state
  (:refer-clojure :exclude [get get-in set! assoc!]))

;;; 共有すべき内部状態を保持する

(def ^:private initial-state
  {:volume-master 0.5
   :volume-bgm 0.5
   :volume-se 0.5
   :autoext-list ["ogg" "mp3"]
   :debug? false
   :dont-stop-on-background? false
   :disable-mobile? false
   :disable-webaudio? false
   :disable-htmlaudio? false
   :se-chattering-sec 0.05
   :default-bgm-fade-sec 1
   :default-se-fade-sec 0
   })

(defonce ^:private the-state (atom initial-state))

(defn get
  ([] @the-state)
  ([k] (clojure.core/get @the-state k))
  ([k fallback] (clojure.core/get @the-state k fallback)))

(defn get-in [ks & [fallback]]
  (if (empty? ks)
    @the-state
    (let [obj (get (first ks))]
      (clojure.core/get-in obj (rest ks) fallback))))

(defn assoc! [k v & kvs]
  (swap! the-state assoc k v)
  (when-not (empty? kvs)
    (apply assoc! kvs)))

(def set! assoc!)

(defn update! [k f]
  (swap! the-state update k f))



