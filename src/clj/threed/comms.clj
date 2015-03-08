(ns threed.comms
  (:require [clojure.core.async
             :as a
             :refer [put! >! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            [ring.util.response :refer [resource-response not-found]]
            [ring.middleware.cors :refer [wrap-cors]]
            [org.httpkit.server :refer [run-server with-channel on-receive on-close send!]]
            [compojure.core :refer (defroutes GET)]
            [compojure.route :as route]
            [compojure.handler :refer [site] :as handler]
            [ring.middleware.reload :as reload]
            [cheshire.core :refer :all]
            [clojure.tools.reader.edn :as edn]
            [threed.message]
            [threed.universe]))

;; TODO rename comms to websockets

(defonce clients (atom {}))

(defn send-message! [message]
  (let [encoded (binding [*print-length* false] (pr-str message))]
    (doseq [client @clients]
      ;; TODO encoding independence
      (send! (key client) encoded))))

(defonce messages (chan))

(defn <get-messages []
  messages)

;; Unify comms/system-bus for clj/cljs
(defn ws [req]
  (with-channel req con
    (swap! clients assoc con true)
    (on-receive con (fn [msg]
                      (put! messages
                            (edn/read-string {:readers {'threed.message.Message #'threed.message/read-message
                                                        'threed.universe.Block #'threed.universe/read-block}} msg))))

    (on-close con (fn [status]
                    (swap! clients dissoc con)))))
