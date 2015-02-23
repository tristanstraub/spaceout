(ns threed.events)

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
        (send-position! pos)))))
