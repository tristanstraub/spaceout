(ns threed.actions)

(defrecord Action [name])

(defn action [name]
  (Action. name))

(defn add-block [pos]
  (assoc (action :add-block)
    :position pos))
