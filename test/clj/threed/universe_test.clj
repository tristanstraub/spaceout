(ns threed.universe-test
  (:use midje.sweet)
  (:require [threed.universe :refer [view box-slice-view create-universe add-blocks block]]
            [threed.generator :refer [gen-worlds cube]]))

(fact "Positions outside of a box slice view are excluded"
      (add-blocks (create-universe) #{(block :position [0 0])})
      => (contains {:blocks #{(block :position [0 0])}}))

(fact "World can be generated"
      (gen-worlds 1
                  :rand (fn [x] x)
                  :dim 1
                  :radius 1) => (contains #{{:color nil :position [1.0 1.0 1.0]}}))

(fact "World can be generated"
      (cube [0 0 0] 1) => (contains #{[0.0 0.0 0.0]}))
