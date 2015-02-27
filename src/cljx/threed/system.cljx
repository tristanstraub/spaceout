(ns threed.system
  (:require [quile.component :as component]
            [threed.dispatcher :refer [dispatcher]]
            [threed.system-bus :refer [system-bus]]
            [threed.synchroniser :refer [synchroniser]]
            [threed.universe :refer [create-universe add-position]]))

(defn system []
  (component/system-map
             ;; TODO fix clients
             :state {:universe (atom (add-position (create-universe) [0 0 0]))}
             :clients []
             :system-bus (system-bus)
             :dispatcher (component/using (dispatcher) [:clients :system-bus :state])
             ;; universe should be higher than dispatcher
             :synchroniser (component/using (synchroniser) [:dispatcher :state])))
