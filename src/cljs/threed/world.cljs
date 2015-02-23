(ns threed.world
  (:require [threed.math :refer [vec-add vec-scale axis-rotation-matrix3 matrix-vec-mult]]))

(defn generate-world [pos]
  (reduce concat (->> (range 10)
                      (map (fn [i]
                             (reduce concat (->> (range 10)
                                                 (map (fn [j]
                                                        (->> (range 10)
                                                             (map (fn [k] (vec-add pos [i j k])))))))))))))
