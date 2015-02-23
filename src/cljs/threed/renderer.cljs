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
            [threed.api :refer [<get-positions send-position!]]
            [threed.interop :refer [threed->vec set-threed-vec!]]
            [threed.camera :refer [update-camera]]
            [threed.threejs.blocks :as blocks :refer [make-block]]
            [threed.threejs.intersection :refer [get-intersections]]
            [threed.events :as events :refer [on-mouse-move on-mouse-down on-resize]]
            [threed.world :as world]))

;; Current grid locations that are set
(defonce current-positions (atom []))

;; Newest positions that have arrived from the server
;; are need new Boxes added to threejs
(defonce new-positions (atom []))

(defn update-positions! [positions]
  (reset! new-positions (clojure.set/difference positions @current-positions))
  (reset! current-positions positions))

(defn add-block!
  [scene pos]
  (blocks/add-block! scene pos)
  ;; TODO add-block! should happen after current-positions has been modified (use watcher)
  (swap! current-positions conj pos))

(defn add-blocks! [scene blocks]
  (doseq [block blocks]
    (send-position! block)
    ;;(add-block! scene block)
    ))

(defn add-new-world! [scene pos]
  (add-blocks! scene (world/generate-world pos)))

(defprotocol IInitialise
  (initialise! [this]))

(defprotocol IRender
  (render [this]))

(defprotocol IIntersections
  (intersections [this mouse camera]))

(defrecord Intersector [raycaster]
  IIntersections
  (intersections [this mouse camera]
    (.setFromCamera raycaster mouse camera)))

(defn intersector []
  (map->Intersector {:raycaster (js/THREE.Raycaster.)}))

(defrecord RenderContext [positions
                          events
                          width height
                          scene camera renderer
                          intersector
                          light clock
                          last-intersect
                          mouse keys]
  IInitialise
  (initialise! [this]
    (.setClearColor renderer 0xdbf1ff 1)
    (set! (.. light -position -x) 10)
    (set! (.. light -position -y) 50)
    (set! (.. light -position -z) 130)

    (.setSize renderer width height)

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
    (.lookAt camera (js/THREE.Vector3. 0 0 0)))

  IRender
  (render [this]
    (if-let [events (swap-and-return! events [])]
      (let [keys (swap! keys (fn [keys] (events/events->keys keys events)))]

        ;; add new blocks

        (doseq [pos (swap-and-return! new-positions [])]
          (add-block! scene pos))

        (when-let [last @last-intersect]
          (.. (:object last) -material -color (setHex (:color last))))

        (reset! last-intersect nil)

        (intersections intersector mouse camera)

        (let [intersects (.intersectObjects (:raycaster intersector) (.-children scene))]
          (when-let [intersect (first intersects)]
            (reset! last-intersect {:object (.. intersect -object)
                                    :color (.. intersect -object -material -color getHex)
                                    :normal (threed->vec (.. intersect -face -normal))})

            (.. intersect -object -material -color (setHex 0xff0000))))

        (update-camera keys camera light (.. clock (getElapsedTime)))

        ;; TODO too many parameters
        (events/call-event-handlers events scene @last-intersect mouse renderer camera)

        (.render renderer scene camera)))))

(defn render-context [positions events]
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (map->RenderContext
     {:positions positions
      :events events

      ;; Dimensions
      :width width
      :height height

      ;; Scene/View
      :camera (js/THREE.PerspectiveCamera. 75 (/ width height) 0.1 1000 )
      :scene (js/THREE.Scene.)

      ;; Time
      :clock (js/THREE.Clock.)

      :renderer (js/THREE.WebGLRenderer.)
      :intersector (intersector)
      :light (js/THREE.PointLight. 0xffffff)

      :last-intersect (atom nil)
      :mouse (js/THREE.Vector2.)
      :keys (atom #{})})))

(defn attach-renderer [el positions events]
  (reset! current-positions positions)

  (let [context (render-context positions events)
        do-render (fn cb []
                    (js/requestAnimationFrame cb)
                    (render context))]

    (.appendChild el (.-domElement (:renderer context)))

    (initialise! context)
    (do-render)))
