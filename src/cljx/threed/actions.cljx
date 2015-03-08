
(ns threed.actions
  (:require [threed.message :refer [message]]))

(defn action [name]
  (assoc (message :type :action)
    :name name))

(defn add-block
  ([pos]
     (add-block pos 0xffffff))
  ([pos color]
     (assoc (action :add-block)
       :position pos
       :color color)))

(defn send-blocks [blocks]
  (assoc (action :send-blocks)
    :blocks blocks))

(defn send-remove-blocks [blocks]
  (assoc (action :send-remove-blocks)
    :blocks blocks))


(defn request-universe []
  (action :request-universe))

(defn the-universe [blocks]
  (assoc (action :the-universe)
    :blocks blocks))
