(ns io.allthethings.atoms)

(defn swap-and-return!
  ([atom new-value]
     ;;{:pre [(instance? clojure.lang.Atom atom)]}
     (loop [oldval @atom]
       (if (compare-and-set! atom oldval new-value)
         oldval
         (recur @atom))))
  ([atom f-get f-set]
     ;;{:pre [(instance? clojure.lang.Atom atom)]}
     (loop [oldval @atom]
       (if (compare-and-set! atom oldval (f-set oldval))
         (f-get oldval)
         (recur @atom)))))
