(ns threed.system
  (:require [quile.component :as component]
            [threed.dispatcher :refer [dispatcher]]
            [threed.system-bus :as system-bus]
            [threed.synchroniser :refer [synchroniser]]
            [threed.universe :refer [create-universe add-block]]))
;; TODO clj/cljs tags should only be used for implementation replacement
;; -- not for configuration. Testing is difficult if it is used for
;; configuration
(defn system [& {:keys [system-bus] :or {system-bus system-bus/system-bus}}]
  (component/system-map
             ;; TODO fix clients
             :state {:universe (atom (create-universe))}
             :system-bus (system-bus)
             :dispatcher (component/using (dispatcher) [:system-bus :state])
             ;; universe should be higher than dispatcher
             :synchroniser (component/using (synchroniser) [:dispatcher :state])))
