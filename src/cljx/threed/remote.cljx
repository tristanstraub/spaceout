(ns threed.client
  (:require [threed.dispatcher :refer [dispatcher]]
            [threed.universe :refer [universe]]))

(defprotocol IAddClient
  (add-client [this client]))

(defrecord Client [view clients]
  IAddClient
  (add-client [this client]
    (update-in this [:clients] conj client)))

(defn client [view]
  (map->Client {:clients [] :view view}))
