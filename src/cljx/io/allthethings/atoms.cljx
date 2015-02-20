(ns io.allthethings.atoms)

(defn swap-and-return!
  [atom new-value]
  ;;{:pre [(instance? clojure.lang.Atom atom)]}
  (loop [oldval @atom]
    (if (compare-and-set! atom oldval new-value)
      oldval
      (recur @atom))))
