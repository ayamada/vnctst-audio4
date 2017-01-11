(ns vnctst.audio4.device
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [vnctst.audio4.util :as util]
            [vnctst.audio4.device.entry-table :as entry-table]
            [vnctst.audio4.device.dumb]
            [vnctst.audio4.device.web-audio]
            [vnctst.audio4.device.html-audio-multi]
            [vnctst.audio4.device.html-audio-single]
            [cljs.core.async :as async :refer [>! <!]]
            ))

;;; TODO: init! を実行する前にサードパーティー製のデバイスを設定する事で、
;;;       そのデバイスを優先して利用できる仕組みを組み込みたい





;;; :init!? を一回だけ実行する事を保証する為に、実行結果をキャッシングする
(defonce init-device-table (atom {}))
(defn init-device!? [device-key]
  (if (contains? @init-device-table device-key)
    (get @init-device-table device-key)
    (let [device (entry-table/get device-key)
          ok? ((get device :init!?))]
      (swap! init-device-table device-key (if ok? device false)))))







;;; TODO: 一部の古いモバイル系は :dumb 固定にしたい。しかしどう判定する？

(defn- determine-se-device-keys [never-use-webaudio? never-use-htmlaudio?]
  (let [r (if (or
                (:android util/terminal-type)
                (:ios util/terminal-type))
            [:web-audio :html-audio-single :dumb]
            [:web-audio :html-audio-multi :dumb])
        r (if never-use-webaudio?
            (vec (remove #{:web-audio} r))
            r)
        r (if never-use-htmlaudio?
            (vec (remove #{:html-audio-single :html-audio-multi} r))
            r)]
    r))


(defn- determine-bgm-device-keys
  [never-use-webaudio? never-use-htmlaudio?]
  ;; androidのchromeではWebAudioのデコードに時間がかかるらしいので、
  ;; これのみ特別扱いして、WebAudioを使わせないようにする
  ;; …という事だったが、最近の端末では問題ないようなので、
  ;; 最初からSEと同じ扱いにする事にした
  ;; (以前は (and (:android util/terminal-type) (:chrome util/terminal-type))
  ;; で判定し、 [:html-audio-single :dumb] を固定で返すようにしていた)
  (determine-se-device-keys never-use-webaudio? never-use-htmlaudio?))


(defn- resolve-device [device-keys]
  (loop [device-keys device-keys]
    (when-let [k (first device-keys)]
      (if-let [device (init-device!? k)]
        device
        (recur (rest device-keys))))))


;;; 適切にデバイスの判定と初期化を行い、stateに保存する
(defn init! [& [never-use-webaudio? never-use-htmlaudio?]]
  (let [se-device (resolve-device (determine-se-device-keys never-use-webaudio? never-use-htmlaudio?))
        bgm-device (resolve-device (determine-bgm-device-keys never-use-webaudio? never-use-htmlaudio?))]
    (assert se-device)
    (assert bgm-device)
    (state/set! :se-device se-device)
    (state/set! :bgm-device bgm-device)
    true))





(defn- check-device-fn-keyword! [k]
  (assert (get entry-table/device-fn-keywords k)))

;;; デバイス関数を実行する

(defn se-call! [k & args]
  (check-device-fn-keyword! k)
  (apply (get (state/get :se-device) k) args))

(defn bgm-call! [k & args]
  (check-device-fn-keyword! k)
  (apply (get (state/get :bgm-device) k) args))


