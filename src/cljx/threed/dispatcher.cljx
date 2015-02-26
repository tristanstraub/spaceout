(ns threed.dispatcher
  #+cljs
  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require [quile.component :as component]

            #+clj
            [clojure.core.async
             :as a
             :refer [put! >! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]
            #+cljs
            [cljs.core.async :refer [put! chan <!]]

            [threed.universe :refer (create-universe)]
            [threed.system-bus :refer [subscribe! send-message!]]
            [threed.actions :refer [the-universe]]))

(defprotocol IDispatch
  (dispatch! [this action]))

(defn remote->local [action]
  (case (:name action)
    :send-blocks (assoc action :name :add-blocks)
    action))

(defn dispatch-actions! [dispatcher system-bus]
  (let [messages (subscribe! system-bus {:type :action})]
    (println "starting dispatcher loop")
    (go (loop []
          (let [action (<! messages)]

            (println "action from messages:" dispatcher action)
            (dispatch! dispatcher (remote->local action)))
          (recur)))))

;; TODO does universe belong in dispatcher? as an atom?
(defrecord Dispatcher [clients system-bus universe]
  component/Lifecycle
  (start [this]
    (println "start dispatcher")
    ;; The universe can evolve
    (let [component (assoc this :universe (atom (create-universe)))]
      (println "dispatch actions!")
      (dispatch-actions! component system-bus)

      component))

  ;; TODO outsource action handling
  IDispatch
  (dispatch! [this action]
    (println "dispatch:" (:name action) action)
    (case (:name action)
      ;; Add a new block to the universe
      :add-block
      (do
        (println "add-block?" (:position action))

        (swap! universe
               (fn [universe]
                 (update-in universe [:positions] #(conj % (:position action)))))

        (println "new universe" @universe))

      :send-blocks
      (do
        (println "send-blocks?")
        (send-message! system-bus action))

      :add-blocks
      (do
        (println "adding blocks")
        (swap! universe
               (fn [universe]
                 (update-in universe [:positions] #(apply conj % (:positions action)))))
        (println :positions universe))

      ;; server side send the-universe only
      #+clj
      :request-universe
      #+clj
      (send-message! system-bus (the-universe (:positions @universe)))

      ;; client side replace only
      #+cljs
      :the-universe
      #+cljs
      (do
        (println "it's the universe!" action)
        (swap! universe
               (fn [universe]
                 (assoc universe :positions (:positions action)))))

      ;; :else
      (println (str "Unknown action name" (:name action))))))

(defn dispatcher []
  (map->Dispatcher {}))
