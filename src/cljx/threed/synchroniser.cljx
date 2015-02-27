(ns threed.synchroniser
  (:require [quile.component :as component]

            #+clj
            [clojure.core.async
             :as a
             :refer [put! >! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            #+cljs
            [cljs.core.async :refer [put! chan <!]]

            ;; [threed.universe :refer [add-positions set-positions add-position]]
            ;; [threed.system-bus :refer [subscribe! send-message!]]
            ;; [threed.actions :refer [the-universe]]
            [threed.dispatcher :refer [dispatch!]]
            [threed.actions :refer [send-blocks]]))

(defrecord Synchroniser [dispatcher state]
  component/Lifecycle
  (start [this]
    (add-watch (:universe state) :synchroniser
               (fn [key reference old-universe new-universe]
                 (println "synchroniser[u1:u2]" new-universe old-universe)
                 (let [new-positions (clojure.set/difference
                                                  (:positions new-universe)
                                                  (:positions old-universe))]
                   (when (not (empty? new-positions))
                     ;; TODO reconsider how send-blocks gets transformed and dispatched from client to server
                     (dispatch! dispatcher (send-blocks new-positions))))))
    this))

(defn synchroniser []
  (map->Synchroniser {}))
