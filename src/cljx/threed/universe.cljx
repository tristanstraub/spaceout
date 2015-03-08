(ns threed.universe)

(declare universe)

;; (defprotocol IUniverse
;;   (add-block [this block]))

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
    (update-in universe [:blocks]
               (fn [blocks]
                 (into #{} (filter (partial in-region? slice) blocks))))))

(defn- box-slice [from to] (map->BoxSlice {:from from :to to}))

(defn- slice-view [slice] (map->SliceView {:slice slice}))

(defn box-slice-view [from to]
  (slice-view (box-slice from to)))

(defrecord Universe [blocks]
  ;; IDiff
  ;; (diff [this another]
  ;;   ;; TODO not sure about how these blocks are created
  ;;   (assoc (universe)
  ;;     :blocks
  ;;     (clojure.set/difference (:blocks this) (:blocks another))))

  ;; IPatch
  ;; (patch [this diff]
  ;;   (update-in this [:blocks] clojure.set/union (:blocks diff)))
  )

;; TODO rename blocks -> blocks

(defn create-universe []
  (map->Universe {:blocks #{}}))

;; TODO add colors/material
(defn add-block [universe block]
  (update-in universe [:blocks]
             #(conj % block)))

(defn add-blocks [universe blocks]
  (update-in universe [:blocks]
             (fn [upos]
               (if (empty? blocks)
                 upos
                 (apply conj upos blocks)))))

(defn remove-blocks [universe blocks]
  (update-in universe [:blocks]
             (fn [upos]
               (if (empty? blocks)
                 upos
                 (apply disj upos blocks)))))

(defn set-blocks [universe blocks]
  (assoc universe :blocks
         (apply hash-set blocks)))

(defn clear [universe]
  (assoc universe :blocks #{}))

(defrecord Block [color position])

(defn block [& {:keys [color position]}]
  (map->Block {:color color :position position}))

(defn read-block [block]
  (map->Block block))
