(ns threed.events
  (:require [threed.api :refer [send-position!]]
            [threed.threejs.blocks :as blocks]))

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

(defn- on-mouse-down [event scene intersection]
  (when intersection
    (let [n (:normal intersection)
          v (.. (:object intersection) -meta)
          pos (if (and n v) (mapv + n v))]
      (when pos
        ;; TODO convert to actions which are dispatched to modify the universe
        (send-position! pos)))))

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

(defn call-event-handlers [events scene last-intersect mouse renderer camera]
  (doseq [event events]
    (cond (and (= (:event/type event) :mouse)
               (= (:event/action event) :down))
          (on-mouse-down (:event/object event) scene last-intersect)

          (and (= (:event/type event) :mouse)
               (= (:event/action event) :move))
          (on-mouse-move (:event/object event) mouse)

          (and (= (:event/type event) :window)
               (= (:event/action event) :resize))
          (on-resize (:event/object event) renderer camera))))
