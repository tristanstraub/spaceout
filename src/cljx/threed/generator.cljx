(ns threed.generator
  (:require [threed.math :as math]
            [threed.universe :refer [block]]))

(defn mmap
  [m n o]
  ;; TODO convert to a recursive version
  (reduce concat (map (fn [i] (reduce concat (map (fn [j] (map (fn [k] [i j k]) (range o))) (range n)))) (range m))))

(defn mmap2
  [m n]
  ;; TODO convert to a recursive version
  (reduce concat (map (fn [i] (reduce concat (map (fn [j] [i j]) (range n)))) (range m))))

(defn cube [position dim]
  (->> (mmap dim dim dim)
       (map #(math/vec-add % position))
       (map #(math/vec-sub
                   %
                   (math/vec-scale [1 1 1] (math/floor (/ dim 2)))
                   ))
       (apply hash-set)))

(defn within-distance-from?
  [center radius position]
  (<= (math/vec-magnitude (math/vec-sub center position))
      radius))

(defn sphere [center diameter]
  (->> (cube center diameter)
       (filter #(within-distance-from? center (/ diameter 2) %))))

(defn surrounded? [positions position]
  ;; TODO rename :position to :coords
  (and (contains? positions (math/vec-add position [-1 0 0]))
       (contains? positions (math/vec-add position [1 0 0]))
       (contains? positions (math/vec-add position [0 -1 0]))
       (contains? positions (math/vec-add position [0 1 0]))
       (contains? positions (math/vec-add position [0 0 -1]))
       (contains? positions (math/vec-add position [0 0 1]))))

(defn hull [positions]
  (let [positions (apply hash-set positions)]
    (remove #(surrounded? positions %) positions)))

(defn gen-worlds
  ([n & {:keys [rand dim radius color] :or {rand rand dim 200 radius 50 color :red}}]
     (let [spheres (map (fn [_] (hull (sphere
                                       (into [] (math/vec-round [(rand dim) (rand dim) (rand dim)]))
                                       (rand radius))))
                        (range n))]
       ;; TODO externalise colour
       (map #(block :position % :color color) (hull (reduce concat spheres))))))

(defn average [heights]
  (/ (reduce + heights) (count heights)))

(defn get-neighbours [positions position]
  (let [[i _ j] position]
    (filter (fn [[x _ z]] (or (and (= i x) (or (= j (inc z)) (= j (dec z))))
                              (and (= j z) (or (= i (inc x)) (= i (dec x))))))
            positions)))

(defn average-neighbours [positions]
  ;; (map (fn [position]
  ;;        (let [neighbour-heights (map second (get-neighbours positions position))
  ;;              height (when (not-empty neighbour-heights) (average neighbour-heights))]
  ;;          (if (not-empty neighbour-heights)
  ;;            (assoc position 1 height)
  ;;            position)))
  ;;      positions)
  (map (fn [[x y z]]
         [x (Math/floor (/ y 2)) z])
       positions))

(defn fill-below [positions]
  (reduce concat (map (fn [[x y z]]
                        (conj (map (fn [i] [x i z])
                                   (range y))
                              [x y z]))
                      positions)))

(defn gen-landscape [position dim]
  (->> (mmap dim 1 dim)
       (map #(math/vec-add % position))
       (map #(math/vec-sub
                   %
                   (math/vec-scale [1 1 1] (math/floor (/ dim 2)))))
       ;; add random heights
       (map #(math/vec-add [0 (* 30 (rand)) 0] %))
       ;; average neighbour heights
       (apply hash-set)
       average-neighbours
       fill-below
       (apply hash-set)
       (map #(block :position % :color :lava))))
