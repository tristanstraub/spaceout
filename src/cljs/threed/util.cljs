(ns threed.util)

(defn make-height-map [n geometry]
  (reduce concat (map (fn [y]
                        (map (fn [x]
                               (let [
                                     ;; Each cube needs its own material for picking to work
                                     material (js/THREE.MeshLambertMaterial. (clj->js {:color 0x48b4fb}))
                                     cube (js/THREE.Mesh. geometry material)
                                     ]
                                 (set! (.. cube -position -x) x)
                                 (set! (.. cube -position -z) y)
                                 (set! (.. cube -position -y) (rand))

                                 (set! (.. cube -meta) [x y])

                                 cube))
                             (range (- (/ n 2)) (/ n 2))))
                      (range (- (/ n 2)) (/ n 2)))))
