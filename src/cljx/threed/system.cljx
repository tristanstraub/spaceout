(ns threed.system
  (:require [quile.component :as component]
            [threed.dispatcher :refer [dispatcher]]
            [threed.system-bus :refer [system-bus]]))

(defn system []
  (component/system-map
             :clients []
             :system-bus (system-bus)
             :dispatcher (component/using (dispatcher) [:clients :system-bus])
             ))
