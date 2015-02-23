(ns threed.world)

(defn generate-world [pos]
  (reduce concat (->> (range 10)
                      (map (fn [i]
                             (reduce concat (->> (range 10)
                                                 (map (fn [j]
                                                        (->> (range 10)
                                                             (map (fn [k] (vec-add pos [i j k])))))))))))))
