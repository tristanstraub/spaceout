(ns threed.actions
  (:require [threed.message :refer [message]]))

(defn action [name]
  (assoc (message :type :action)
    :name name))

(defn add-block [pos]
  (assoc (action :add-block)
    :position pos))

(defn send-blocks [positions]
  (assoc (action :send-blocks)
    :positions positions))

(defn request-universe []
  (action :request-universe))

(defn the-universe [positions]
  (assoc (action :the-universe)
    :positions positions))
