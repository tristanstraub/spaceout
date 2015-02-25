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
            [threed.system-bus :refer [subscribe!]]))

(defprotocol IDispatch
  (dispatch! [this action]))

(defn dispatch-actions! [dispatcher system-bus]
  (let [messages (subscribe! system-bus {:type :action})]
    (go (loop []
          (let [action (<! messages)]
            (println "action from messages:" dispatcher action)
            (dispatch! dispatcher action))
          (recur)))))

(defrecord Dispatcher [clients system-bus universe]
  component/Lifecycle
  (start [this]
    ;; The universe can evolve
    (let [component (assoc this :universe (atom (create-universe)))]
      (dispatch-actions! component system-bus)

      component))

  IDispatch
  (dispatch! [this action]
    (println "dispatcher" component)
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

      ;; :else
      (println (str "Unknown action name" (:name action))))))

(defn dispatcher []
  (map->Dispatcher {}))
