(ns threed.interop)

(defn threed->vec [v]
  [(.-x v) (.-y v) (.-z v)])

(defn set-threed-vec! [o v]
  (set! (.. o -x) (v 0))
  (set! (.. o -y) (v 1))
  (set! (.. o -z) (v 2)))
