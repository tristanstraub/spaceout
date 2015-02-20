(ns threed.api
  (:require [cljs.core.async :refer [put! chan <!]]
            [om-sync.util :refer [edn-xhr]]
            [threed.comms :as comms]))

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

(defn send-position! [pos]
  (edn-xhr
   {:method :post
    :url "/events"
    :data {:event/name "add" :event/position pos ;; :csrf-token (:csrf-token @comms/chsk-state)
           }
    :on-complete
    (fn [res]
      (println "server response:" res))}))

(defn <get-positions []
  (let [result (chan)]
    (edn-xhr
     {:method :get
      :url "/events"
      :on-complete
      (fn [res]
        (put! result (events->positions (:events res))))
      })
    result))
