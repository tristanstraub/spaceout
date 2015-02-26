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
            [threed.message]))

;; TODO rename comms to websockets

(def clients (atom {}))

(defn mesg-received [msg]
  (println "received" msg))

(defn send-message! [message]
  (doseq [client @clients]
    (send! (key client) (pr-str message))))

(defonce messages (chan))

(defn <get-messages []
  messages)

(defn ws [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-receive con (fn [msg]
                      (println "server got message:" msg)
                      (put! messages
                            (edn/read-string {:readers {'threed.message.Message #'threed.message/read-message}} msg))))

    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))))
