(ns threed.connector
  (:require [quile.component :as component]
            #+clj
            [clojure.core.async
             :as a
             :refer [put! >! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]

            #+cljs
            [cljs.core.async :refer [put! chan <!]]))

(defrecord Connector [system-bus websockets]
  component/Lifecycle
  (start [this]
    (let [connections (ws/<connections websockets)]
      (go (loop []
            (cn/pipe (<! connections) system-bus)
            (recur))))

    ;; (go (loop []
    ;;       (let [conn (<! connections)]
    ;;         (edn/decode coder (<! (cn/<incoming conn)))
    ;;         (recur))))
    ;;   (edn/decode coder (<! (cn/<incoming conn)))
    ;; coder (edn-coder {:readers {'threed.message.Message #'threed.message/read-message
    ;;                             'threed.universe.Block #'threed.universe/read-block}})

    this))

(defn connector []
  (map->Connector {}))
