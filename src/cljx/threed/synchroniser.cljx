(ns threed.synchroniser
  (:require [quile.component :as component]

            #+clj
            [clojure.core.async
             :as a
             :refer [put! >! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            #+cljs
            [cljs.core.async :refer [put! chan <!]]
            [threed.dispatcher :refer [dispatch!]]
            [threed.actions :refer [send-blocks send-remove-blocks]]))

(defrecord Synchroniser [dispatcher state]
  component/Lifecycle
  (start [this]
    (add-watch (:universe state) :synchroniser
               (fn [key reference old-universe new-universe]
                 (let [new-positions (clojure.set/difference
                                                  (:blocks new-universe)
                                                  (:blocks old-universe))
                       removed-positions (clojure.set/difference
                                                      (:blocks old-universe)
                                                      (:blocks new-universe))]
                   (when (not (empty? new-positions))
                     ;; TODO reconsider how send-blocks gets transformed and dispatched from client to server
                     (dispatch! dispatcher (send-blocks new-positions)))

                   (when (not (empty? removed-positions))
                     ;; TODO reconsider how send-blocks gets transformed and dispatched from client to server
                     (dispatch! dispatcher (send-remove-blocks removed-positions))))))
    this))


(defn synchroniser []
  (map->Synchroniser {}))
