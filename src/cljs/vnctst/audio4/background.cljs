(ns vnctst.audio4.background
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require [vnctst.audio4.state :as state]
            [cljs.core.async :as async :refer [>! <!]]
            ))

(defonce background-handle (atom nil))

(defn start-supervise! []
  (when (nil? (state/get :in-background?))
    (state/set! :in-background?
                (boolean (= js/document.visibilityState "hidden")))
    (let [event-name "visibilitychange"
          h (fn [e]
                  (let [bg? (boolean (= js/document.visibilityState "hidden"))]
                    (state/set! :in-background? bg?)
                    (when @background-handle
                      (@background-handle bg?))))]
      (js/document.addEventListener event-name h)
      (h nil))))






