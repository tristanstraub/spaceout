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

            #+cljs
            [cljs.reader]))

(declare <get-messages -send-message!)

(defprotocol ISendMessage
  (send-message! [this message]))

(defprotocol ISubscribe
  (subscribe! [this topic]))

(defn topic-matches? [topic message]
  (let [topic-keys (keys topic)]
    (= (select-keys message topic-keys)
       (select-keys topic topic-keys))))

(defrecord SystemBus [subscriptions messages]
  component/Lifecycle
  (start [this]
    (println "starting system bus")
    (let [subscriptions (atom [])
          messages (<get-messages)]
      (go (loop []
            (let [message (<! messages)]
              (doseq [{:keys [topic subscription-channel]} @subscriptions]
                (when (topic-matches? topic message)
                  (put! subscription-channel message)))
              (recur))))
      (assoc this :subscriptions subscriptions :messages messages)))

  ISendMessage
  (send-message! [this message]
    (-send-message! message))

  ISubscribe
  (subscribe! [this topic]
    (let [channel (chan)]
      (swap! subscriptions conj {:topic topic :subscription-channel channel})
      channel)))

(defn system-bus []
  (map->SystemBus {}))

#+cljs
(defn get-ws-url []
  (let [loc (.-location js/window)
        schema (if (= (.-protocol loc) "https:")
                 "wss:"
                 "ws:")]
    (str schema "//" (.-host loc) (.-pathname loc) "ws")))

#+cljs
(defn get-websocket []
  (defonce websocket (atom nil))
  (swap! websocket #(or % (js/WebSocket. (get-ws-url)))))

#+cljs
(defonce websocket-queue (chan))

;; TODO Needs a client end-point argument
(defn -send-message! [message]
  #+cljs
  (put! websocket-queue message)

  #+clj
  (comms/send-message! message))

#+cljs
(defn- <get-messages []
  (let [channel (chan)
        websocket (get-websocket)]

    (set! (.-onopen websocket) (fn [e]
                                 (println "websocket opened")
                                 (go (loop []
                                       (let [message (<! websocket-queue)]
                                         (.send websocket (binding [*print-length* false]
                                                            (pr-str message))))
                                       (recur)))))

    (set! (.-onerror websocket) (fn [] (.error js/console "ws error" js/arguments)))
    (set! (.-onmessage websocket) (fn [e]
                                    (cljs.reader/register-tag-parser! "threed.message.Message" #'threed.message/read-message)

                                    (let [message (cljs.reader/read-string (.-data e))]
                                      (put! channel message))))

    channel))

#+clj
(defn- <get-messages []
  (comms/<get-messages))
