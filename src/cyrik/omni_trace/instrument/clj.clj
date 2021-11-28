(ns cyrik.omni-trace.instrument.clj)

(defn var->sym [v]
  (let [meta (meta v)]
    (symbol (name (ns-name (:ns meta))) (name (:name meta)))))

(defn vars-in-ns-clj [sym]
  (if (find-ns sym)
    (for [[_ v] (ns-interns sym)
          :when (not (:macro (meta v)))]
      (var->sym v))
    []))

(defn clj-instrument-fn [sym opts instrumenter]
  (when-let [v (resolve sym)]
    (let [var-name (var->sym v)]
      (when-let [instrumented-fn (instrumenter var-name v *file* opts)]
        (alter-var-root v (constantly instrumented-fn))
        var-name))))

(defn clj-instrument-ns [ns-sym opts mapper instrumenter]
  (->> ns-sym
       vars-in-ns-clj
       (filter symbol?)
       (distinct)
       (mapv (fn [sym] (mapper sym opts instrumenter)))
       (remove nil?)))