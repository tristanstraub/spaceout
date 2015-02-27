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
            [threed.camera :refer [update-camera!]]
            [threed.threejs.blocks :as blocks :refer [make-block]]
            [threed.threejs.intersection :refer [get-intersections]]
            [threed.world :as world]
            [threed.events :as events]
            [threed.dispatcher :refer [dispatch!]]
            [threed.actions :refer [send-blocks]]))

;; Current grid locations that are set
(defonce current-positions (atom []))

;; Newest positions that have arrived from the server
;; are need new Boxes added to threejs
(defonce new-positions (atom []))

(defn update-positions! [positions]
  (let [newest (clojure.set/difference positions @current-positions)]
    (println "update-positions!" newest "from:" positions "and:" @current-positions)
    (reset! new-positions newest)
    (reset! current-positions positions)

    (println "update-positions!" positions)))

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
  (initialise! [this positions]))

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
                          mouse keys]
  IInitialise
  (initialise! [this positions]
    (.setClearColor renderer 0xdbf1ff 1)
    (.setSize renderer width height)

    ;; TODO This can be done outside of initialise as part of listening to server pushes
    (doseq [pos positions]
      (add-block! scene pos))

    ;; TODO Eliminate this through some sort of land generation
    (when (empty? positions)
      (add-block! scene [0 0 0])
      (send-position! [0 0 0]))

    ;; TODO this should be moved into an abstract representation of the world/view and synchronised
    ;; with threejs equivalent components
    (.add scene light)

    ;; TODO factor this out into a vector which tracks camera position state
    (set! (.-x (.-position camera)) 10)
    (set! (.-y (.-position camera)) 10)
    (set! (.-z (.-position camera)) 10)
    (.lookAt camera (js/THREE.Vector3. 0 0 0)))

  IRender
  (render [this]
    (if-let [events (swap-and-return! events [])]
      ;; TODO find a better syntax for this keys/keys/keys
      (let [keys (swap! keys (fn [keys] (events/events->keys keys events)))]

        ;; TODO move this into a side-channel
        (let [newest (swap-and-return! new-positions [])]
          (when (not (empty? newest))
            (println "newest" newest)
            (doseq [pos newest]
              (add-block! scene pos))))

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

(defn render-context [events dispatcher]
  (let [width (.-innerWidth js/window)
        height (.-innerHeight js/window)]
    (map->RenderContext
     {:dispatcher dispatcher
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
  (println "attach-renderer" universe)
  ;; pre: universe is an atom
  (reset! current-positions (:positions universe))

  (let [context (render-context events dispatcher)
        do-render (fn cb []
                    ;; TODO reenable
                    (js/requestAnimationFrame cb)
                    (render context))]

    (add-watch universe :renderer
               (fn [key reference old-universe new-universe]
                 (println "u1:u2" new-universe old-universe "or:" @universe)
                 ;; (dispatch! dispatcher (send-blocks (clojure.set/difference
                 ;;                                                 (:positions new-universe)
                 ;;                                                 (:positions old-universe))))
                 (update-positions! (:positions new-universe))))

    (.appendChild el (.-domElement (:renderer context)))

    (initialise! context (:positions universe))
    (do-render)))
