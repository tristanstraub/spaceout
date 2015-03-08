(ns threed.system-test
  (:use midje.sweet)
  (:require [quile.component :as component]
            [threed.system :refer [system]]
            [threed.system-bus :as system-bus :refer [system-bus]]
            [threed.dispatcher :refer [dispatcher dispatch!]]
            [threed.system-bus :refer [subscribe! send-message!]]
            [threed.universe :as universe :refer [block]]
            [threed.actions :refer [add-block]]

            [clojure.core.async
             :as a
             :refer [put! >! <! >!! <!! go chan buffer close! thread
                     alts! alts!! timeout]]))

(defn start []
  (let [messages (atom [])]
    {:system (component/start
                        (system
                         :system-bus
                         #(reify
                            component/Lifecycle
                            (start [this] this)
                            system-bus/ISubscribe
                            (subscribe! [this topic]
                              (chan))
                            system-bus/ISendMessage
                            (send-message! [this message]
                              (swap! messages conj message)))))
     :messages messages}))

(fact "Can initialise system"
      @(get-in (:system (start)) [:state :universe]) => {:blocks #{}})

(fact "Can create block"
      (block :position [0 0 0]) => {:color nil :position [0 0 0]})

(fact "Universe change dispatches"
      (let [{:keys [system messages]} (start)]
        (dispatch! (:dispatcher system)
                   (add-block [0 0 0]))
        @messages)
      => (contains [{:blocks #{[0 0 0]} :name :add-blocks :type :action}]))
