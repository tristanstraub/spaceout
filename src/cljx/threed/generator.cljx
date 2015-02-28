(ns threed.generator
  (:require [threed.math :as math]))

(defn mmap
  [m n o]
  ;; TODO convert to a recursive version
  (reduce concat (map (fn [i] (reduce concat (map (fn [j] (map (fn [k] [i j k]) (range o))) (range n)))) (range m))))

(defn cube [position dim]
  (->> (mmap dim dim dim)
       (map #(math/vec-sub
                   %
                   (math/vec-scale [1 1 1] (math/floor (/ dim 2)))
                   ))))

(defn within-distance-from?
  [center radius position]
  (<= (math/vec-magnitude (math/vec-sub center position))
      radius))

(defn sphere [center diameter]
  (->> (cube center diameter)
       (filter #(within-distance-from? center (/ diameter 2) %))))
