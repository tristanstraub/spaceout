(ns threed.events
  (:require [threed.threejs.blocks :as blocks]
            [threed.actions :refer [add-block]]
            [threed.dispatcher :refer [dispatch!]]))

;; UI Event Handlers

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

;; TODO convert mouse handlers into a system/component with dispatcher as dependency
(defn- on-mouse-down [event dispatcher scene intersection]
  (when intersection
    (let [n (:normal intersection)
          v (.. (:object intersection) -meta)
          pos (if (and n v) (mapv + n v))]
      (when pos
        (dispatch! dispatcher (add-block pos))))))

(defn events->keys [keys events]
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
          events))

(defn call-event-handlers [events dispatcher scene last-intersect mouse renderer camera]
  (doseq [event events]
    (cond (and (= (:event/type event) :mouse)
               (= (:event/action event) :down))
          (on-mouse-down (:event/object event) dispatcher scene last-intersect)

          (and (= (:event/type event) :mouse)
               (= (:event/action event) :move))
          (on-mouse-move (:event/object event) mouse)

          (and (= (:event/type event) :window)
               (= (:event/action event) :resize))
          (on-resize (:event/object event) renderer camera))))
