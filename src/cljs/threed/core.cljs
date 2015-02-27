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
            [threed.actions :as actions]
            [threed.actions :refer [request-universe]]))


;; NOTE: margins on page must be zero for picking to work
;; each mesh must have it's own material for picking to work

(enable-console-print!)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defonce sys (atom nil))

(defn main []
  (let [{:keys [system-bus dispatcher state]} (reset! sys (component/start (system)))]
    ;; (add-watch (:universe dispatcher)
    ;;            :key (fn [key reference old-universe universe]
    ;;                   (swap! app-state #(assoc % :positions (:positions universe)))))

    (send-message! system-bus (request-universe))

    (let [ui-events (get-events)]
      (om/root
          (fn [app owner]
            (reify
              om/IDidMount
              (did-mount [this]
                ;; TODO add queuing for when websockets aren't connected yet
                (attach-renderer (om/get-node owner)
                                 (:universe state)
                                 ui-events
                                 dispatcher)
                ;;(send-message! system-bus (actions/add-block [0 0 1]))
                )
              om/IRender
              (render [_]
                (html [:div #_(pr-str (:positions app))]))))
          app-state
          {:target (. js/document (getElementById "app"))}))))
