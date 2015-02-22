(ns threed.api
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <! timeout]]
            [om-sync.util :refer [edn-xhr]]
            [io.allthethings.atoms :refer [swap-and-return!]]))

(defn events->positions [events]
  (reduce (fn [positions event]
            (cond (:event/name "add" event)
                  (let [pos (:event/position event)]
                    (if (positions pos)
                      positions
                      (conj positions pos)))
                  :else
                  positions))
          #{}
          events))

(defn send-events! [events]
  (when (< 0 (count events))
    (print "sending" events)
    (edn-xhr
     {:method :post
      :url "events" ;; TODO url builder
      :data {:events events}
      :on-complete
      (fn [res]
        (println "server response:" res))})))

(def event-queue (atom []))

(defn start-api-queue! []
  (go (loop []
        (<! (timeout (/ 1000 60)))
        (send-events! (swap-and-return! event-queue []))
        (recur))))


(defn send-position! [pos]
  (swap! event-queue conj {:event/name "add" :event/position pos}))

(defn <get-positions []
  (let [result (chan)]
    (edn-xhr
     {:method :get
      :url "events"
      :on-complete
      (fn [res]
        (put! result (events->positions (:events res))))
      })
    result))
