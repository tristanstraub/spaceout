(ns threed.universe-test
  (:use midje.sweet)
  (:require [threed.universe :refer [view box-slice-view universe]]))

(fact "Positions outside of a box slice view are excluded"
      (view (box-slice-view [0 0] [5 5])
            (assoc (universe) :positions #{[0 0] [6 0] [5 5]}))
      => (contains {:positions #{[0 0] [5 5]}}))
