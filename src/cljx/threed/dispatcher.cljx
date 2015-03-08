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

            [threed.universe :refer [remove-positions add-positions set-positions add-position]]
            [threed.system-bus :refer [subscribe! send-message!]]
            [threed.actions :refer [the-universe]]))

(defprotocol IDispatch
  (dispatch! [this action]))

(defn remote->local [action]
  (case (:name action)
    :send-blocks (assoc action :name :add-blocks)
    :send-remove-blocks (assoc action :name :remove-blocks)
    :request-universe (assoc action :name :send-universe)
    action))

(defn local->remote [action]
  (case (:name action)
    :send-blocks (assoc action :name :add-blocks)
    :send-remove-blocks (assoc action :name :remove-blocks)
    action))

(defn dispatch-actions! [dispatcher system-bus]
  ;; TODO enforce that system-bus should only retrieve remotely sent messages
  (let [messages (subscribe! system-bus {:type :action})]
    (go (loop []
          (let [action (<! messages)]
            ;; remove->local should get translated outside of dispatch-actions!
            (dispatch! dispatcher (remote->local action)))
          (recur)))))

;; TODO does universe belong in dispatcher? as an atom?
(defrecord Dispatcher [clients system-bus state]
  component/Lifecycle
  (start [this]
    (dispatch-actions! this system-bus)
    this)

  ;; TODO outsource action handling
  IDispatch
  (dispatch! [this action]
    (case (:name action)
      ;; Add a new block to the universe
      :add-block (swap! (:universe state) add-position (:position action))

      :send-blocks
      (when (not (empty? (:positions action)))
        (send-message! system-bus (local->remote action)))

      :send-remove-blocks
      (when (not (empty? (:positions action)))
        (send-message! system-bus (local->remote action)))

      ;; TODO control what is allowed on the server
      :add-blocks (swap! (:universe state) add-positions (:positions action))
      :remove-blocks (swap! (:universe state) remove-positions (:positions action))

      ;; server side send the-universe only
      #+clj
      :send-universe
      #+clj
      (send-message! system-bus (the-universe (:positions @(:universe state))))

      ;; client side replace only
      #+cljs
      :the-universe
      #+cljs
      (swap! (:universe state) set-positions (:positions action))

      ;; :else
      (println (str "Unknown action name" (:name action))))))

(defn dispatcher []
  (map->Dispatcher {}))
