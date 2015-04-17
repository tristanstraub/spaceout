(ns threed.system-bus
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
            #+clj
            [threed.comms :as comms]
            [threed.message]
            [threed.universe]

            #+cljs
            [cljs.reader]

            [io.allthethings.net.connection :as cn]))

(defprotocol ISendMessage
  (send-message! [this message]))

(defprotocol ISubscribe
  (subscribe! [this topic]))

(defn topic-matches? [topic message]
  (let [topic-keys (keys topic)]
    (= (select-keys message topic-keys)
       (select-keys topic topic-keys))))

(defrecord SystemBus [subscriptions bus]
  component/Lifecycle
  (start [this]
    (let [subscriptions (atom [])
          incoming (cn/<incoming bus)]
      (go (loop []
            (let [message (<! incoming)]
              (doseq [{:keys [topic subscription-channel]} @subscriptions]
                (when (topic-matches? topic message)
                  (put! subscription-channel message)))
              (recur))))
      (assoc this :subscriptions subscriptions)))

  cn/IIncoming
  (<incoming [this] (cn/<incoming bus))

  cn/IOutgoing
  (>outgoing [this] (cn/>outgoing bus))

  ISendMessage
  (send-message! [this message]
    (println "sedning" message)
    (-send-message! message))

  ISubscribe
  (subscribe! [this topic]
    (let [channel (chan)]
      (swap! subscriptions conj {:topic topic :subscription-channel channel})
      channel)))

(defn system-bus []
  (map->SystemBus {:bus (cn/connection)}))
