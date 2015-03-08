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
            [threed.message :as message]
            [threed.generator :as gen]
            [threed.universe :as uni]
            [threed.math :as math]))

(deftemplate page
  (io/resource "index.html") [] [:body] (if is-dev? inject-devmode-html identity))

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
    (wrap-edn-params
     (if is-dev?
       (reload/wrap-reload (wrap-defaults #'routes ring-defaults-config))
       (wrap-defaults routes ring-defaults-config))

     ;;(logger/wrap-with-logger)
     )))

(defn run-web-server [& [port]]
  (let [port (Integer. (or port (env :port) 3000))]
    (print "Starting web server on port" port ".\n")
    (http-kit/run-server http-handler {:port port})))

(defn run-auto-reload [& [port]]
  (auto-reload *ns*)
  (start-figwheel))

(defn start-system! []
  (defonce sys (atom (component/start (system)))))

(defn stop-system! []
  ;; TODO not working
  (component/stop @sys))

(defn add!
  [positions]
  (swap! (get-in @sys [:state :universe])
         (fn [universe]
           (uni/add-positions universe positions))))

(defn gen-worlds! [n]
  (add! (gen/gen-worlds n)))

(defn gen-sandbox! []
  (add! (gen/hull (gen/cube [0 0 0] 50))))

(defn get-universe-atom []
  (get-in @sys [:state :universe]))

(defn empty-world! []
  (swap! (get-universe-atom)
         (fn [universe]
           (uni/clear universe))))

(defn remove-positions! [positions]
  (swap! (get-universe-atom)
         (fn [universe]
           (uni/remove-positions universe positions))))

(defn run [& [port]]
  (defonce stop! (atom nil))

  (swap! stop! (fn [stop!] (when stop! (stop!)) nil))

  (start-system!)

  (when is-dev?
    (run-auto-reload))

  (let [stop-web-server! (run-web-server port)]
    (reset! stop! (fn []
                    (stop-web-server!)
                    (stop-system!))))

  (gen-worlds! 16))

(defn -main [& [port]]
  (run port))
