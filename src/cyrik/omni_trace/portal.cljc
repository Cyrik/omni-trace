(ns cyrik.omni-trace.portal)

(defonce re-run-trace (atom nil))
(defonce re-run-debug (atom nil))

(defn re-run
  ([] (println "called"))
  ([trace]
   (reset! re-run-trace trace)
   ["1" trace])
  ([name args]
   (reset! re-run-debug args)
   (let [id (keyword (:traceid args))]
     (reset! re-run-trace (:args (id (:log @i/workspace)))))
   (reset! re-run-debug args)
   ["ran" args]))