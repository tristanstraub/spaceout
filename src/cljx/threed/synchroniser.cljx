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
                 (let [new-blocks (clojure.set/difference
                                                  (:blocks new-universe)
                                                  (:blocks old-universe))
                       removed-blocks (clojure.set/difference
                                                      (:blocks old-universe)
                                                      (:blocks new-universe))]

                   (println "new-blocks" (count new-blocks))
                   (println "removed-blocks" (count removed-blocks))

                   ;; NOTE this might be bouncing
                   ;; TODO batch add/remove together
                   ;; TODO reenable for client
                   #+clj
                   (when (not (empty? new-blocks))
                     ;; TODO reconsider how send-blocks gets transformed and dispatched from client to server
                     (dispatch! dispatcher (send-blocks new-blocks)))

                   ;; TODO reenable for client
                   #+clj
                   (when (not (empty? removed-blocks))
                     ;; TODO reconsider how send-blocks gets transformed and dispatched from client to server
                     (dispatch! dispatcher (send-remove-blocks removed-blocks))))))
    this))


(defn synchroniser []
  (map->Synchroniser {}))
