;; TODO sphere of size 30 is too slow. Optimize for visibility

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
            [threed.interop :refer [threed->vec set-threed-vec!]]
            [threed.camera :refer [update-camera!]]
            [threed.threejs.blocks :as blocks]
            [threed.threejs.intersection :refer [get-intersections]]
            [threed.world :as world]
            [threed.events :as events]
            [threed.dispatcher :refer [dispatch!]]
            [threed.actions :refer [send-blocks]]))

(defprotocol IInitialise
  (initialise! [this]))

(defprotocol IRender
  (render [this]))

(defprotocol IIntersections
  (intersections [this mouse camera scene last-intersect]))

(defrecord Intersector [raycaster]
  IIntersections
  (intersections [this mouse camera scene last-intersect]
    (.setFromCamera raycaster mouse camera)

    (let [intersects (.intersectObjects raycaster (.-children scene))]
      (when-let [intersect (first intersects)]
        ;; TODO move these side effects out
        (reset! last-intersect {:object (.. intersect -object)
                                :color (.. intersect -object -material -color getHex)
                                :normal (threed->vec (.. intersect -face -normal))})

        (.. intersect -object -material -color (setHex 0xff0000))))))

(defn intersector []
  (map->Intersector {:raycaster (js/THREE.Raycaster.)}))

(defrecord RenderContext [events dispatcher
                          width height
                          scene camera renderer
                          intersector
                          light clock
                          last-intersect
                          mouse keys
                          universe]
  IInitialise
  (initialise! [this]
    (add-watch universe :renderer
               (fn [key reference old-universe new-universe]
                 (let [new-positions
                       ;; TODO support removals
                       (clojure.set/difference (:positions new-universe) (:positions old-universe))]
                   (doseq [position new-positions]
                     (blocks/add-block! scene position)))))

    (.setClearColor renderer 0xdbf1ff 1)
    (.setSize renderer width height)

    ;; TODO this should be moved into an abstract representation of the world/view and synchronised
    ;; with threejs equivalent components
    (.add scene light)

    ;; TODO factor this out into a vector which tracks camera position state
    (set! (.-x (.-position camera)) 200)
    (set! (.-y (.-position camera)) 200)
    (set! (.-z (.-position camera)) 200)
    (.lookAt camera (js/THREE.Vector3. 0 0 0)))

  IRender
  (render [this]
    (if-let [events (swap-and-return! events [])]
      ;; TODO find a better syntax for this keys/keys/keys
      (let [keys (swap! keys (fn [keys] (events/events->keys keys events)))]
        ;; TODO move into intersector -- deal with side-effecting nature of intersector
        (when-let [last @last-intersect]
          (.. (:object last) -material -color (setHex (:color last))))
        (reset! last-intersect nil)
        (intersections intersector mouse camera scene last-intersect)

        ;; TODO let's remove the side effect, and move the actual side-effecting update into
        ;; a side channel
        (update-camera! keys camera light (.. clock (getElapsedTime)))

        ;; TODO too many parameters -- and who knows what this modifies
        (events/call-event-handlers events dispatcher scene @last-intersect mouse renderer camera)

        (.render renderer scene camera)))))

(defonce webglrenderer (atom nil))

(defn render-context [events dispatcher universe]
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (map->RenderContext
     {:dispatcher dispatcher

      :universe universe
      ;; Communications
      :events events

      ;; Dimensions
      :width width
      :height height

      ;; Canvas
      :renderer (swap! webglrenderer #(or % (js/THREE.WebGLRenderer.)))

      ;; Scene/View
      :camera (js/THREE.PerspectiveCamera. 75 (/ width height) 0.1 1000 )
      :scene (js/THREE.Scene.)
      :light (js/THREE.PointLight. 0xffffff)

      ;; Time
      :clock (js/THREE.Clock.)

      ;; Mouse interactions
      :intersector (intersector)
      :last-intersect (atom nil)
      :mouse (js/THREE.Vector2.)

      ;; Keys interactions
      :keys (atom #{})})))

(defn attach-renderer [el universe events dispatcher]
  ;; pre: universe is an atom

  (let [context (render-context events dispatcher universe)
        do-render (fn cb []
                    (js/requestAnimationFrame cb)
                    (render context))]

    (.appendChild el (.-domElement (:renderer context)))

    (initialise! context)
    (do-render)))
