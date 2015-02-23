(ns threed.universe)

(defrecord Universe [positions])

(defn universe []
  (map->Universe {:positions []}))
