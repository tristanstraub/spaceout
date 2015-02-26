(ns threed.actions
  (:require [threed.message :refer [message]]))

(defn action [name]
  (assoc (message :type :action)
    :name name))

(defn add-block [pos]
  (assoc (action :add-block)
    :position pos))
