(ns threed.generator
  (:require [threed.math :as math]))

(defn mmap
  [m n o]
  ;; TODO convert to a recursive version
  (reduce concat (map (fn [i] (reduce concat (map (fn [j] (map (fn [k] [i j k]) (range o))) (range n)))) (range m))))

(defn cube [position dim]
  (->> (mmap dim dim dim)
       (map #(math/vec-add % position))
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

(defn surrounded? [positions position]
  (and (positions (math/vec-add position [-1 0 0]))
       (positions (math/vec-add position [1 0 0]))
       (positions (math/vec-add position [0 -1 0]))
       (positions (math/vec-add position [0 1 0]))
       (positions (math/vec-add position [0 0 -1]))
       (positions (math/vec-add position [0 0 1]))))

(defn hull [positions]
  (let [positions (apply hash-set positions)]
    (remove #(surrounded? positions %) positions)))
