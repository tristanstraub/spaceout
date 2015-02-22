(ns threed.renderer
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [io.allthethings.atoms :refer [swap-and-return!]]
            ;;[om.dom :as dom :include-macros true]
            [sablono.core :refer-macros [html]]
            [om-sync.util :refer [edn-xhr]]
            [threed.ui.events :refer [get-events]]
            [threed.math :refer [vec-add vec-scale axis-rotation-matrix3 matrix-vec-mult]]
            [threed.api :refer [<get-positions send-position!]]))

(defonce scene (atom nil))

(defonce current-positions (atom []))
(defonce new-positions (atom []))

(defn threed->vec [v]
  [(.-x v) (.-y v) (.-z v)])

(defn set-threed-vec! [o v]
  (set! (.. o -x) (v 0))
  (set! (.. o -y) (v 1))
  (set! (.. o -z) (v 2)))

(defn make-block [[x y z]]
  (let [ ;; Each cube needs its own material for picking to work
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
  (.add scene (make-block pos))
  (swap! current-positions conj pos))

(defn- on-mouse-move [event mouse]
  ;; TODO clientX,clientY depends on location of canvas. Fix this.
  (set! (.. mouse -x) (- (* 2 (/ (.-clientX event) (.-innerWidth js/window))) 1))
  (set! (.. mouse -y) (+ (* -2 (/ (.-clientY event) (.-innerHeight js/window))) 1)))

(defn- on-resize [event renderer camera]
  (let [w (.-innerWidth js/window)
        h (.-innerHeight js/window)]
    (.. renderer (setSize w h))
    (set! (.. camera -aspect) (/ w h))
    (.. camera (updateProjectionMatrix))))

(defn- on-mouse-down [event scene intersection]
  (when intersection
    (let [n (:normal intersection)
          v (.. (:object intersection) -meta)
          pos (if (and n v) (mapv + n v))]
      (when pos
        (add-block! scene pos)
        (send-position! pos)))))

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
          ;; sky (->> [0 1 0]
          ;;          (matrix-vec-mult mrz)
          ;;          (matrix-vec-mult mry)
          ;;          (matrix-vec-mult mrx)
          ;;          )
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
    ;; (set! (.-x (.-rotation camera)) (+ (rot 0) (* (/ Math/PI 90) drx)))
    ;; (set! (.-y (.-rotation camera)) (+ (rot 1) (* (/ Math/PI 90) drx)))

    (set! (.. light -position -x) (.-x (.-position camera)))
    (set! (.. light -position -y) (.-y (.-position camera)))
    (set! (.. light -position -z) (.-z (.-position camera)))))

(defn start-renderer [el positions events]
  (reset! current-positions positions)

  (let [scene (reset! scene (js/THREE.Scene.))
        width (.-innerWidth js/window)
        height (.-innerHeight js/window)
        camera (js/THREE.PerspectiveCamera. 75 (/ width height) 0.1 1000 )
        renderer (js/THREE.WebGLRenderer.)
        geometry (js/THREE.BoxGeometry. 1 1 1)
        texture (js/THREE.ImageUtils.loadTexture "textures/grid.png")
        raycaster (js/THREE.Raycaster.)
        light (js/THREE.PointLight. 0xffffff)

        ;; floor-material (js/THREE.MeshLambertMaterial. (clj->js {:map texture}))
        ;; floor-material2 (js/THREE.MeshLambertMaterial. (clj->js {:map texture}))
        ;; floor-geometry (js/THREE.PlaneGeometry. 1 1 1)
        ;; floor (js/THREE.Mesh. floor-geometry floor-material)
        ;; floor2 (js/THREE.Mesh. floor-geometry floor-material2)

        clock (js/THREE.Clock.)

        last-intersect (atom nil)

        mouse (js/THREE.Vector2.)

        keys (atom #{})

        render (fn cb []
                 (js/requestAnimationFrame cb)
                 (if-let [events (swap-and-return! events [])]
                   (let [keys (swap! keys (fn [keys]
                                            (reduce (fn [keys event]
                                                      (cond (and (= (:event/type event) :keyboard)
                                                                 (= (:event/action event) :down))
                                                            (conj keys (.-which (:event/object event)))

                                                            (and (= (:event/type event) :keyboard)
                                                                 (= (:event/action event) :up))
                                                            (disj keys (.-which (:event/object event)))

                                                            :else
                                                            keys))
                                                    keys
                                                    events)))]

                     ;; add new blocks

                     (doseq [pos (swap-and-return! new-positions [])]
                       (add-block! scene pos))

                     (.setFromCamera raycaster mouse camera)

                     (when-let [last @last-intersect]
                       (.. (:object last) -material -color (setHex (:color last))))

                     (reset! last-intersect nil)

                     (let [intersects (.intersectObjects raycaster (.-children scene))]
                       (when-let [intersect (first intersects)]
                         (reset! last-intersect {:object (.. intersect -object)
                                                 :color (.. intersect -object -material -color getHex)
                                                 :normal (threed->vec (.. intersect -face -normal))})

                         (.. intersect -object -material -color (setHex 0xff0000))))

                     (update-camera keys camera light (.. clock (getElapsedTime)))


                     (doseq [event events]
                       (cond (and (= (:event/type event) :mouse)
                                  (= (:event/action event) :down))
                             (on-mouse-down (:event/object event) scene @last-intersect)

                             (and (= (:event/type event) :mouse)
                                  (= (:event/action event) :move))
                             (on-mouse-move (:event/object event) mouse)

                             (and (= (:event/type event) :window)
                                  (= (:event/action event) :resize))
                             (on-resize (:event/object event) renderer camera)))

                     (.render renderer scene camera))))]

    (set! (.. texture -wrapS) js/THREE.RepeatWrapping)
    (set! (.. texture -wrapT) js/THREE.RepeatWrapping)
    (.. texture -repeat (set 2 2))

    (.setClearColor renderer 0xdbf1ff 1)
    (set! (.. light -position -x) 10)
    (set! (.. light -position -y) 50)
    (set! (.. light -position -z) 130)

    (.setSize renderer width height)
    (.appendChild el (.-domElement renderer) )

    (doseq [pos positions]
      (add-block! scene pos))

    (when (empty? positions)
      (add-block! scene [0 0 0])
      (send-position! [0 0 0]))

    ;; (.add scene floor)
    ;; (.add scene floor2)
    (.add scene light)

    (set! (.-y (.-position camera)) 10)
    (set! (.-z (.-position camera)) 10)
    (.lookAt camera (js/THREE.Vector3. 0 0 0))

    (render)))



(defn update-positions! [positions]
  (reset! new-positions (clojure.set/difference positions @current-positions))
  (reset! current-positions positions))

(defn generate-world [pos]
  (reduce concat (->> (range 10)
                      (map (fn [i]
                             (reduce concat (->> (range 10)
                                                 (map (fn [j]
                                                        (->> (range 10)
                                                             (map (fn [k] (vec-add pos [i j k])))))))))))))

(defn add-blocks! [scene blocks]
  (doseq [block blocks]
    (send-position! block)
    ;;(add-block! scene block)
    ))

(defn add-new-world! [scene pos]
  (add-blocks! scene (generate-world pos)))
