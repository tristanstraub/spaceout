(ns threed.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [io.allthethings.atoms :refer [swap-and-return!]]
            ;;[om.dom :as dom :include-macros true]
            [sablono.core :refer-macros [html]]
            [om-sync.util :refer [edn-xhr]]
            [threed.ui.events :refer [get-events]]
            [threed.api :refer [<get-positions events->positions]]
            [threed.renderer :refer [start-renderer update-positions!]]
            ))

;; NOTE: margins on page must be zero for picking to work
;; each mesh must have it's own material for picking to work

(set! *enable-console-print* true)

(defonce app-state (atom {:text "Hello Chestnut!"}))

(defn start-sending! []
  (let [conn (js/WebSocket. "ws://ws")]
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
            (let [events (cljs.reader/read-string (.-data e))]
              (update-positions! (events->positions events)))))))

(defn main []
  (let [ui-events (get-events)]
    (go (let [positions (<! (<get-positions))]
          (om/root
              (fn [app owner]
                (reify
                  om/IDidMount
                  (did-mount [this]
                    (start-sending!)

                    (start-renderer (om/get-node owner) positions ui-events))
                  om/IRender
                  (render [_]
                    (html [:div]))))
              app-state
              {:target (. js/document (getElementById "app"))})))))
