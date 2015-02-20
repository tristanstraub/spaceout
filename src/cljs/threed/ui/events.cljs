(ns threed.ui.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]))

(defn get-events []
  (let [events (atom [])]
    (.addEventListener js/window "mousemove" #(swap! events conj {:event/type :mouse
                                                                  :event/action :move
                                                                  :event/object %}) false)

    (.addEventListener js/window "mousedown" #(swap! events conj {:event/type :mouse
                                                                  :event/action :down
                                                                  :event/object %}) false)

    (.addEventListener js/window "keydown" #(swap! events conj {:event/type :keyboard
                                                                :event/action :down
                                                                :event/object %}) false)

    (.addEventListener js/window "keyup" #(swap! events conj {:event/type :keyboard
                                                              :event/action :up
                                                              :event/object %}) false)

    (.addEventListener js/window "resize" #(swap! events conj {:event/type :window
                                                               :event/action :resize
                                                               :event/object %}) false)
    events))
