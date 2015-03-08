(ns threed.threejs.blocks)

;; (defn to-hex [num]
;;   (let [symbols "0123456789abcdef"]
;;     (str "0x"
;;          (nth symbols (Math/floor (mod k 16)))
;;          (nth symbols (Math/floor (mod (/ k 16) 16)))
;;          (nth symbols (Math/floor (mod (/ k 16 16) 16)))
;;          (nth symbols (Math/floor (mod (/ k 16 16 16) 16)))
;;          (nth symbols (Math/floor (mod (/ k 16 16 16 16) 16)))
;;          (nth symbols (Math/floor (mod (/ k 16 16 16 16 16) 16))))))

(defn rand-color! []
  (Math/floor (* 0xffffff (rand))))

(defn make-block-parts
  ([pos]
     (let [[x y z] pos

           ;; Each cube needs its own material for picking to work
           geometry (js/THREE.BoxGeometry. 1 1 1)
           material (js/THREE.MeshLambertMaterial. (clj->js {:color 0xffcc00}))
           cube (js/THREE.Mesh. geometry material)]

       (set! (.. cube -position -x) x)
       (set! (.. cube -position -y) y)
       (set! (.. cube -position -z) z)
       (set! (.. cube -meta) [x y z])

       [geometry material cube])))

;; Create a THREE.Mesh representing a cube at position [x y z]
(defn make-block [pos]
  (let [[geometry material cube] (make-block-parts pos)]
    cube))



(defn add-block!
  [scene pos]
  (.add scene (make-block pos)))
