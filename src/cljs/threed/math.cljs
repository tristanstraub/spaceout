(ns threed.math)

(defn vec-dot [a b]
  (reduce + (map * a b)))

(defn vec-add [p1 p2]
  (into [] (map + p1 p2)))

(defn vec-sub [p1 p2]
  (into [] (map - p1 p2)))

(defn vec-scale [a s]
  (into [] (map (fn [v] (* s v)) a)))

(defn matrix-create [m f]
  (mapv (fn [j] (mapv (fn [i] (f i j)) (range m))) (range m)))

(defn zero-matrix [m]
  (mapv (fn [_] (into [] (take m (repeat 0)))) (range m)))

(defn identity-matrix [m]
  (mapv (fn [j] (mapv (fn [i] (if (= i j) 1 0)) (range m))) (range m)))

(def ijk (identity-matrix 3))

(defn translate-matrix [m v]
  (mapv (fn [j] (mapv (fn [i] (cond (= i j) 1
                                    (= i (- m 1)) (nth v j)
                                    :else 0))
                      (range m))) (range m)))

(defn matrix-transpose [m]
  (let [n (count (first m))]
    (mapv (fn [j]
            (mapv (fn [i] (get-in m [i j]))
                  (range n)))
          (range n))))

(defn matrix-scale-create [m s]
  (mapv (fn [j] (mapv (fn [i] (cond (= i j (- m 1)) 1
                                    (= i j) s
                                    :else 0))
                      (range m))) (range m)))

(defn vec-mul [v s]
  (mapv #(* s %) v))

(defn matrix-scale [u s]
  (mapv #(vec-mul % s) u))

(defn matrix-mult [a b]
  (let [n (count (first a))
        tb (matrix-transpose b)]
    (mapv (fn [i]
            (mapv (fn [j]
                    (vec-dot (nth a i)
                             (nth tb j)))
                  (range n)))
          (range n))))

(defn matrix-vec-mult [a v]
  (mapv (fn [vi] (vec-dot vi v)) a))

(defn frustrum [left right bottom top znear zfar]
  (let [x (/ (* 2 znear)
             (- right left))
        y (/ (* 2 znear)
             (- top bottom))
        a (/ (+ right left)
             (- right left))
        b (/ (+ top bottom)
             (- top bottom))
        c (/ (- (+ zfar znear))
             (- zfar znear))
        d (/ (* -2 zfar znear)
             (- zfar znear))]
    [[x 0 a 0]
     [0 y b 0]
     [0 0 c d]
     [0 0 -1 0]]))

(defn perspective [fovy aspect znear zfar]
  (let [ymax (* znear (Math/tan (* fovy (/ Math/PI 360.0))))
        ymin (- ymax)
        xmin (* ymin aspect)
        xmax (* ymax aspect)]
    (frustrum xmin xmax ymin ymax znear zfar)))


(defn flatten-matrix [m]
  (flatten (matrix-transpose m)))

(defn without-column [a i]
  (mapv (fn [row] (concat (take i row)
                          (drop (+ i 1) row)))
        a))

(defn without-row [a i]
  (concat (take i a)
          (drop (+ i 1) a)))

(defn width [a]
  (count (nth a 0)))

(defn vec-cross-product [u v]
  (let [u1 (nth u 0)
        u2 (nth u 1)
        u3 (nth u 2)
        v1 (nth v 0)
        v2 (nth v 1)
        v3 (nth v 2)]
    [(- (* u2 v3)
        (* u3 v2))
     (- (* u3 v1)
        (* u1 v3))
     (- (* u1 v2)
        (* u2 v1))]))

(defn vecxy [x y]
  [x y])

(defn vec-diff [p1 p2]
  (apply vecxy (map - p1 p2)))

(defn sqrt [a]
  (Math/sqrt a))

(defn vec-magnitude [p1]
  (sqrt (vec-dot p1 p1)))

(defn deg->rad [degrees]
  (mod (* Math/PI (/ degrees 180)) (* 2 Math/PI)))

(defn rad->deg [radians]
  (mod (* 180 (/ radians Math/PI)) 360))

(defn vec-angle [p]
  (rad->deg (Math/atan2 (p 1) (p 0))))

(defn abs [value]
  (Math/abs value))

(defn normalize-angle [angle]
  (mod (+ 360 angle) 360))

(defn angle-diff [a b]
  (let [angle (- a b)]
    angle))

(defn angle-to [v1 v2]
  (vec-angle (vec-diff v2 v1)))

(defn asin [a]
  (Math/asin a))

(defn vec-normalize [a]
  (vec-mul a (/ 1 (vec-magnitude a))))

(defn angle-between [v1 v2]
  (rad->deg (asin (vec-dot (vec-normalize v1) (vec-normalize v2)))))

(defn min-angle [angle]
  (let [angle (normalize-angle angle)]
    (if (>= (abs angle) 180)
      (- angle 360)
      angle)))

(defn min-angle-diff [a b]
  (let [angle (normalize-angle (- (normalize-angle a) (normalize-angle b)))]
    (min-angle angle)))

(defn min-angle-to [v1 v2]
  (min-angle (angle-to v1 v2)))

(defn abs-angle-between [a b]
  (abs (min-angle-diff a b)))

(defn facing?
  ([angle p1 p2]
     (facing? angle p1 p2 1))
  ([angle p1 p2 error]
     (<= (abs-angle-between (angle-to p1 p2) angle)
         error)))

(defn sign [a]
  (if (< 0 a)
    1
    -1))

(defn sin [a]
  (Math/sin a))

(defn cos [a]
  (Math/cos a))

(defn intersects-disc? [[center radius] [v1 v-delta]]
  (let [ac (vec-diff center v1)
        theta (deg->rad (angle-between v-delta ac))]
    (<= (abs (* (vec-magnitude ac) (sin theta)))
        radius)))

(defn intersect-plane? [D d1 d2 n]
  [(vec-dot (vec-cross-product D d2) n)
   (vec-dot (vec-cross-product D d1) n)])

(defn cross-product-matrix [u]
  [[0, (- (nth u 2)) (nth u 1)]
   [(nth u 2) 0 (- (nth u 0))]
   [(- (nth u 1)) (nth u 0) 0]])

(defn tensor-product [u]
  (matrix-create 3 (fn [i j] (* (nth u i) (nth u j)))))

(defn matrix-add [u v]
  (map #(into [] %) (map vec-add u v)))

(defn axis-rotation-matrix3 [u theta]
  (let [u (vec-normalize u)
        I (identity-matrix 3)
        costheta (Math/cos theta)
        sintheta (Math/sin theta)
        ux (cross-product-matrix u)
        uxu (tensor-product u)
        R (matrix-scale I costheta)
        R (matrix-add R (matrix-scale ux sintheta))]

    (matrix-add R (matrix-scale uxu (- 1 costheta)))))
