(ns threed.server
  (:require [clojure.java.io :as io]
            [quile.component :as component]
            [threed.dev :refer [is-dev? inject-devmode-html browser-repl start-figwheel]]
            [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [resources]]
            [net.cgrand.enlive-html :refer [deftemplate]]
            [net.cgrand.reload :refer [auto-reload]]
            [ring.middleware.reload :as reload]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults site-defaults]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.format-response :refer [wrap-restful-response wrap-json-response]]
            [ring.middleware.json :refer [wrap-json-body]]
            [ring.util.response :refer [response]]
            [ring.middleware.edn :refer [wrap-edn-params]]
            [org.httpkit.server :as http-kit]
            [threed.comms :as comms]
            [compojure.handler :refer [site] :as handler]
            [ring.middleware.logger :as logger]

            [threed.system :refer [system]]
            [threed.system-bus :refer [send-message!]]
            [threed.message :as message]))

(assoc *data-readers*
  :threed.message.Message #'message/message)

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

(defn generate-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/edn"}
   :body (pr-str data)})

(defroutes routes
  (resources "/")

  (GET "/ws" [] comms/ws)

  (GET "/*" req (page)))

(def http-handler
  (let [ring-defaults-config api-defaults
        ;; TODO csrf
        ;; (assoc-in api-defaults [:security :anti-forgery]
        ;;           {:read-token (fn [req] (-> req :params :csrf-token))})
        ]
    (-> (if is-dev?
          (reload/wrap-reload (wrap-defaults #'routes ring-defaults-config))
          (wrap-defaults routes ring-defaults-config))
        (wrap-edn-params)
        ;;(logger/wrap-with-logger)
        )))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 3000))]
    (print "Starting web server on port" port ".\n")
    (http-kit/run-server http-handler {:port port})))

(defn run-auto-reload [& [port]]
  (auto-reload *ns*)
  (start-figwheel))

(defonce sys (atom nil))

(defn run [& [port]]
  (reset! sys (component/start (system)))
  (when is-dev?
    (run-auto-reload))
  (run-web-server port))

(defn -main [& [port]]
  (run port))
