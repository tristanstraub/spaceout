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

(def group-size 512)

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

(defprotocol INextMesh
  (next-mesh! [this scene]))

(defprotocol IQueuePushMany
  (push-many! [this objects]))

(defprotocol IQueuePop
  (pop-many! [this n]))

(defrecord SingleBlockQueue [queue]
  IQueuePushMany
  (push-many! [this blocks]
    (push-many! queue blocks))

  INextMesh
  (next-mesh! [this scene]
    (doseq [[geometry material block] (pop-many! queue 5)]
      (.add scene block))))

(defrecord Queue [entries]
  IQueuePushMany
  (push-many! [this new-entries]
    (swap! entries concat new-entries))

  IQueuePop
  (pop-many! [this n]
    (swap-and-return! entries #(take n %) #(drop n %))))

(defn queue []
  (map->Queue {:entries (atom [])}))

(defn rand-color! []
  (Math/floor (* 0xffffff (rand))))

(def nmaterials 6)
(defn make-material [color]
  (js/THREE.MeshLambertMaterial. (clj->js {:color color})))

(def materials
  (clj->js (concat (repeatedly 6 #(make-material 0xff0000))
                   (repeatedly 6 #(make-material 0x00ff00))
                   (repeatedly 6 #(make-material 0x0000ff))
                   (repeatedly 6 #(make-material 0xffb732))
                   )))

(defn color->material [color]
  (if color
    ;; TODO generalize numbers
    (get {:red 0
          :green 6
          :water 12
          :lava 18}
         color)
    0))

(defn add-blocks [blocks]
  {:name :add-blocks :blocks blocks})

(defn remove-blocks [blocks]
  {:name :remove-blocks :blocks blocks})

(defrecord MergedBlockQueue [total-geom total-meshs queue blocks]
  IQueuePushMany
  (push-many! [this blocks]
    (push-many! queue blocks))

  INextMesh
  (next-mesh! [this scene]
    (let [total-geom (js/THREE.Geometry.)
          top (pop-many! queue ;;64
                         1
                         )]
      (when (not-empty top)
        (doseq [[index action] (map-indexed (fn [i j] [i j]) top)]
          (case (:name action)
            :add-blocks
            (do
              (println "next-mesh!:add-blocks" (first (:blocks action)))
              ;;(println "adding-blocks" (first (:blocks action)) (count (:blocks action)))
              ;; TODO include color
              (doseq [[block [geometry material cube]] (map (fn [block] [block (blocks/make-block-parts (:position block))]) (:blocks action))]
                ;; NOTE cannot reuse same geometry and merge in further stuff
                (.updateMatrix cube)

                (.. total-geom (merge
                                geometry
                                (.-matrix cube)
                                (color->material (:color block))
                                )))

              (let [new-mesh (js/THREE.Mesh. total-geom (js/THREE.MeshFaceMaterial. materials))]
                (.add scene new-mesh)
                (swap! total-meshs conj new-mesh)
                (swap! blocks #(apply conj % (:blocks action)))))

            :remove-blocks
            ;; TODO efficiently remove block (partitioning)
            (do
              ;;(println "removing blocks" (count (:blocks action)))
              (doseq [mesh @total-meshs]
                (println "removing mesh")
                (.remove scene mesh))
              (reset! total-meshs [])
              (swap! blocks #(apply disj % (:blocks action)))
              ;;(println "blocks after removal" (count @blocks))
              ;; HACK -- rebuild the whole world

              (when (not-empty @blocks)
                (push-many! this (map #(add-blocks %) (partition-all group-size @blocks)))))))))))

(defn single-block-queue []
  (map->SingleBlockQueue {:queue (queue)}))

(defn merged-block-queue []
  (map->MergedBlockQueue
   {:total-geom (js/THREE.Geometry.)
    :total-meshs (atom [])
    :blocks (atom #{})
    :queue (queue)}))

(defrecord RenderContext [events dispatcher
                          width height
                          scene camera renderer
                          intersector
                          light clock
                          last-intersect
                          mouse keys
                          universe
                          queue]
  IInitialise
  (initialise! [this]
    (add-watch universe :renderer-new-blocks
               (fn [key reference old-universe new-universe]
                 (let [new-blocks (clojure.set/difference
                                                  (:blocks new-universe)
                                                  (:blocks old-universe))
                       removed-blocks (clojure.set/difference
                                                      (:blocks old-universe)
                                                      (:blocks new-universe))]

                   (when (not-empty new-universe)
                     (push-many! queue (map #(add-blocks %) (partition-all group-size (sort-by #(-> % :position first) new-blocks)))))

                   (when (not-empty removed-blocks)
                     (push-many! queue [(remove-blocks (sort-by first removed-blocks))])))))



    (let [color ;;0xdbf1ff
          0x000000
          ]
      (.setClearColor renderer color 1))
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
    (next-mesh! queue scene)
    (if-let [events (swap-and-return! events [])]
      ;; TODO find a better syntax for this keys/keys/keys
      (let [keys (swap! keys (fn [keys] (events/events->keys keys events)))]
        ;; TODO move into intersector -- deal with side-effecting nature of intersector
        (when-let [last @last-intersect]
          (.. (:object last) -material -color (setHex (:color last))))
        (reset! last-intersect nil)
        ;; TODO reenable intersections which is currently broken after switching to a merged mesh
        ;;(intersections intersector mouse camera scene last-intersect)

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
      :queue (merged-block-queue)

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
