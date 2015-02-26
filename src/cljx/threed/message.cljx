(ns threed.message)

(defrecord Message [type])

(defn message [& {:keys [type]}]
  (map->Message {:type type}))

(defn read-message [map]
  (map->Message map))
