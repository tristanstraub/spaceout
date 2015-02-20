(ns threed.comms
  (:require [ring.util.response :refer [resource-response not-found]]
            [ring.middleware.cors :refer [wrap-cors]]
            [org.httpkit.server :refer [run-server with-channel on-receive on-close send!]]
            [compojure.core :refer (defroutes GET)]
            [compojure.route :as route]
            [compojure.handler :refer [site] :as handler]
            [ring.middleware.reload :as reload]
            [cheshire.core :refer :all]))

(def clients (atom {}))

(defn mesg-received [msg]
  (println "received" msg))

(defn send-events! [events]
  (doseq [client @clients]
    (send! (key client) (pr-str events))))

(defn ws [req]
  (with-channel req con
    (swap! clients assoc con true)
    (println con " connected")
    (on-receive con #'mesg-received)
    (on-close con (fn [status]
                    (swap! clients dissoc con)
                    (println con " disconnected. status: " status)))))
