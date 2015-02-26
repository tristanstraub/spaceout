(ns threed.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [quile.component :as component]
            [om.core :as om :include-macros true]
            [io.allthethings.atoms :refer [swap-and-return!]]
            ;;[om.dom :as dom :include-macros true]
            [sablono.core :refer-macros [html]]
            [om-sync.util :refer [edn-xhr]]
            [threed.ui.events :refer [get-events]]
            [threed.api :refer [<get-positions events->positions start-api-queue!]]
            [threed.renderer :refer [attach-renderer update-positions!]]
            ;;[threed.universe :refer [create-universe]]
            [threed.system :refer [system]]
            [threed.system-bus :refer [subscribe! send-message!]]
            [threed.dispatcher :refer [dispatch!]]
            [threed.actions :as actions]))


;; NOTE: margins on page must be zero for picking to work
;; each mesh must have it's own material for picking to work

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defn get-ws-url []
  (let [loc (.-location js/window)
        schema (if (= (.-protocol loc) "https:")
                  "wss:"
                  "ws:")]
    (str schema "//" (.-host loc) (.-pathname loc) "ws")))

(defn start-sending! []
  (println "start-sending!" (get-ws-url))

  (let [conn (js/WebSocket. (get-ws-url))]
    (set! (.-onopen conn)
          (fn [e]
            (.send conn
                   (.stringify js/JSON (js-obj "command" "getall")))))

    (set! (.-onerror conn)
          (fn []
            (.error js/console "ws error")

            (.log js/console js/arguments)))

    (set! (.-onmessage conn)
          (fn [e]
            (println e)
            (let [events (cljs.reader/read-string (.-data e))]
              (update-positions! (events->positions events)))))))

;; (defn main []
;;   (start-api-queue!)

;;   (println "start")
;;   (let [ui-events (get-events)]
;;     (go (let [positions (<! (<get-positions))
;;               universe (create-universe positions)
;;               ]

;;           (om/root
;;               (fn [app owner]
;;                 (reify
;;                   om/IDidMount
;;                   (did-mount [this]
;;                     (start-sending!)
;;                     (attach-renderer (om/get-node owner) universe ui-events))
;;                   om/IRender
;;                   (render [_]
;;                     (html [:div]))))
;;               app-state
;;               {:target (. js/document (getElementById "app"))})))))


(defonce sys (atom nil))

(defn main []
  (let [{:keys [system-bus dispatcher]} (reset! sys (component/start (system)))]
    (add-watch (:universe dispatcher)
               :key (fn [key reference old-universe universe]
                      (swap! app-state #(assoc % :positions (:positions universe)))))

    (let [ui-events (get-events)]
      (om/root
          (fn [app owner]
            (reify
              om/IDidMount
              (did-mount [this]
                (attach-renderer (om/get-node owner)
                                 (:universe dispatcher)
                                 ui-events)
                ;;(send-message! system-bus (actions/add-block [0 0 1]))
                )
              om/IRender
              (render [_]
                (html [:div #_(pr-str (:positions app))]))))
          app-state
          {:target (. js/document (getElementById "app"))}))))
