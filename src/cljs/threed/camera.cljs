(ns threed.camera
  (:require [threed.interop :refer [threed->vec set-threed-vec!]]
            [threed.math :refer [vec-add vec-scale axis-rotation-matrix3 matrix-vec-mult]]))

(defn update-camera [keys camera light seconds]
  (let [camera-pos (threed->vec (.-position camera))
        rot (threed->vec (.-rotation camera))

        dry (+ (if (keys 81) 1 0)
               (if (keys 69) -1 0))
        drx (+ (if (keys 82) 1 0)  ;; up
              (if (keys 70) -1 0))  ;; down

        dx (+ (if (keys 65) -1 0)  ;; left
              (if (keys 68) 1 0))  ;; right
        dz (+ (if (keys 83) 1 0)   ;; s (backward)
              (if (keys 87) -1 0)) ;; w (forward)


        ] ;;a 65 d 68 w 87 s 83 q 81 e 69 r 82 f 70

    (let [mrx (axis-rotation-matrix3 [1 0 0] (rot 0))
          mry (axis-rotation-matrix3 [0 1 0] (rot 1))
          mrz (axis-rotation-matrix3 [0 0 1] (rot 2))
          horizon (->> [1 0 0]
                       (matrix-vec-mult mrz)
                       (matrix-vec-mult mry)
                       (matrix-vec-mult mrx)
                       )
          dir (->> [0 0 1]
                   (matrix-vec-mult mrz)
                   (matrix-vec-mult mry)
                   (matrix-vec-mult mrx)
                   )


          speed 0.3]

      (set-threed-vec! (.-position camera) (vec-add camera-pos
                                                    (vec-add (vec-scale dir (* speed dz))
                                                             (vec-scale horizon (* speed dx))))))

    (set! (.-z (.-rotation camera)) (+ (rot 2) (* (/ Math/PI 90) dry)))

    (set! (.. light -position -x) (.-x (.-position camera)))
    (set! (.. light -position -y) (.-y (.-position camera)))
    (set! (.. light -position -z) (.-z (.-position camera)))))
