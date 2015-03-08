
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

(defn send-blocks [positions]
  (assoc (action :send-blocks)
    :blocks positions))

(defn send-remove-blocks [positions]
  (assoc (action :send-remove-blocks)
    :blocks positions))


(defn request-universe []
  (action :request-universe))

(defn the-universe [positions]
  (assoc (action :the-universe)
    :blocks positions))
