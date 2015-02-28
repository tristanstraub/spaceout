(ns threed.universe)

(declare universe)

(defprotocol IUniverse
  (add-block [this position]))

;; (defprotocol IDiff
;;   (diff [this another]))

;; (defprotocol IPatch
;;   (patch [this diff]))

(defprotocol IView
  (view [this universe]
    "Returns a subset of a universe"))

(defprotocol IInRegion
  (in-region? [this coords]
    "Returns true when coords are within the region"))

(defn |<= [lower-bounds coords]
  (reduce #(and %1 %2) true (map <= lower-bounds coords)))

(defrecord BoxSlice [from to]
  ;; from <= to
  IInRegion
  (in-region? [this coords]
    (and (|<= from coords)
         (|<= coords to))))

(defrecord SliceView [slice]
  IView
  (view [this universe]
    (update-in universe [:positions]
               (fn [positions]
                 (into #{} (filter (partial in-region? slice) positions))))))

(defn- box-slice [from to] (map->BoxSlice {:from from :to to}))

(defn- slice-view [slice] (map->SliceView {:slice slice}))

(defn box-slice-view [from to]
  (slice-view (box-slice from to)))

(defrecord Universe [positions]
  ;; IDiff
  ;; (diff [this another]
  ;;   ;; TODO not sure about how these positions are created
  ;;   (assoc (universe)
  ;;     :positions
  ;;     (clojure.set/difference (:positions this) (:positions another))))

  ;; IPatch
  ;; (patch [this diff]
  ;;   (update-in this [:positions] clojure.set/union (:positions diff)))
  )

(defn create-universe []
  (map->Universe {:positions #{}}))

;; TODO add colors/material
(defn add-position [universe position]
  (update-in universe [:positions]
             #(conj % position)))

(defn add-positions [universe positions]
  (update-in universe [:positions]
             (fn [upos]
               (if (empty? positions)
                 upos
                 (apply conj upos positions)))))

(defn set-positions [universe positions]
  (assoc universe :positions
         (apply hash-set positions)))

(defn clear [universe]
  (assoc universe :positions #{}))
