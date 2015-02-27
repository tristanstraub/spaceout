(ns threed.threejs.blocks)

;; Create a THREE.Mesh representing a cube at position [x y z]
(defn make-block [pos]
  (println "make-block:pos" pos)
  (let [[x y z] pos

 ;; Each cube needs its own material for picking to work
        geometry (js/THREE.BoxGeometry. 1 1 1)
        material (js/THREE.MeshLambertMaterial. (clj->js {:color 0x48b4fb}))
        cube (js/THREE.Mesh. geometry material)]

    (set! (.. cube -position -x) x)
    (set! (.. cube -position -y) y)
    (set! (.. cube -position -z) z)
    (set! (.. cube -meta) [x y z])

    cube))

(defn add-block!
  [scene pos]
  (when (not pos)

    (println "add-block!" pos))
  (.add scene (make-block pos)))
