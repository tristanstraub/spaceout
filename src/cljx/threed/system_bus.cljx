(ns threed.system-bus
  #+cljs
  (:require-macros [cljs.core.async.macros :refer [go]])

  (:require [quile.component :as component]
            #+cljs
            [cljs.core.async :refer [put! chan <!]]
            #+clj
            [threed.comms :as comms]))

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
    (let [subscriptions (atom [])
          messages (<get-messages)]
      (go (loop []
            (let [message (<! messages)]
              (println "systembus:message" message)
              (doseq [{:keys [topic subscription-channel]} @subscriptions]
                (when (topic-matches? topic message)
                  (println "putting to subscription" message)
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
(defonce websocket (js/WebSocket. (get-ws-url)))

;; TODO Needs a client end-point argument
(defn -send-message! [message]
  #+cljs
  (do (println "sending" message)
    (.send websocket (pr-str message)))

  #+clj
  (comms/send-message! message))

#+cljs
(defn- <get-messages []
  (let [channel (chan)]

    ;; (set! (.-onopen conn) (fn [e]
    ;;                         (.send conn (.stringify js/JSON (js-obj "command" "getall")))))

    (set! (.-onerror websocket) (fn [] (.error js/console "ws error" js/arguments)))
    (set! (.-onmessage websocket) (fn [e]
                                    (let [message (cljs.reader/read-string (.-data e))]
                                      (println "on-message" message)
                                      (put! channel message))))

    channel))

#+clj
(defn- <get-messages []
  (comms/<get-messages))
