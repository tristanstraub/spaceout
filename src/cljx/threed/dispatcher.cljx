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

            [threed.universe :refer [add-positions set-positions add-position]]
            [threed.system-bus :refer [subscribe! send-message!]]
            [threed.actions :refer [the-universe]]))

(defprotocol IDispatch
  (dispatch! [this action]))

(defn remote->local [action]
  (case (:name action)
    :send-blocks (assoc action :name :add-blocks)
    #+clj :request-universe #+clj (assoc action :name :send-universe)
    action))

(defn dispatch-actions! [dispatcher system-bus]
  (let [messages (subscribe! system-bus {:type :action})]
    (println "starting dispatcher loop")
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
    (println "dispatch:" (:name action) action)
    (case (:name action)
      ;; Add a new block to the universe
      :add-block
      (swap! (:universe state)
             (fn [universe]
               (add-position universe (:position action))))

      :send-blocks
      (when (not (empty? (:positions action)))
        (send-message! system-bus action))

      :add-blocks
      (swap! (:universe state)
             (fn [universe]
               (add-positions universe (:positions action))))

      ;; server side send the-universe only
      #+clj
      :send-universe
      #+clj
      (send-message! system-bus (the-universe (:positions @(:universe state))))

      ;; client side replace only
      #+cljs
      :the-universe
      #+cljs
      (swap! (:universe state)
             (fn [universe]
               (set-positions universe (:positions action))))

      ;; :else
      (println (str "Unknown action name" (:name action))))))

(defn dispatcher []
  (map->Dispatcher {}))
