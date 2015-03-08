(ns threed.dispatcher-test
  (:use midje.sweet)
  (:require [threed.dispatcher :refer [dispatcher dispatch!]]
            [threed.universe :as universe]
            [threed.actions :as actions]))

;; (fact "Dispatcher can be created"
;;       (dispatcher {}) => {:universe {}})

;; (fact "Action :add-block adds a block to the universe"
;;       (dispatch! (dispatcher (universe/universe))
;;                  (actions/add-block [1 2 3])) => {:positions #{[1 2 3]}})


;; (let [slice (box-slice :from [0 0 0] :to [10 10 10])
;;       ;; original two universes
;;       client-universe (universe/universe)
;;       server-universe (universe/universe)
;;       ;; TODO quickcheck and multiple add-block(s)
;;       ;; universes diverge
;;       next-server-universe (universe/add-block server-universe [0 0 0])
;;       ;; total difference
;;       diff (universe/diff next-server-universe server-universe)]

;;   (fact "Applying a universe diff returns the same slice"
;;         ;; only the slice of the diff gets transported
;;         ;; invariant: the client is up to date for the previous slice
;;         (universe/patch client-universe (universe/slice-of diff slice))
;;         => (universe/slice-of server-universe slice)))

;; (fact "Box slice can be mapped")


;; (fact "When the server universe changes, the client slice is synchronised"
;;       (let [client-universe (universe/universe)
;;             server-universe (universe/universe)]
;;         (push server-universe client-universe)
;;         ;; TODO consider filters of block types could be part of a slice
;;         (universe/slice-of server-universe slice)) => (universe/slice-of client-universe slice))
