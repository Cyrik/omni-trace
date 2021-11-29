(ns cyrik.omni-trace
  (:require
   #?(:clj [cyrik.omni-trace.deep-trace :as deep])
   [cyrik.omni-trace.instrument :as i]
   [cyrik.omni-trace.graph :as flame]
   [net.cgrand.macrovich :as macros])

  #?(:cljs (:require-macros 
                            [cyrik.omni-trace :refer [instrument-fn uninstrument-fn instrument-ns uninstrument-ns]])))

(defmacro instrument-fn
  "Instruments a function.
   Call with fully qualified quoted symbol:
   (instrument-fn 'cyrik.omni-trace.testing-ns/press-button)"
  ([sym]
   `(i/instrument-fn ~sym))
  ([sym opts]
   `(i/instrument-fn ~sym ~opts)))

(defmacro uninstrument-fn
  "Instruments a function.
   Call with fully qualified quoted symbol:
   (uninstrument-fn 'cyrik.omni-trace.testing-ns/press-button)"
  ([sym]
   `(i/uninstrument-fn ~sym ))
  ([sym opts]
   `(i/uninstrument-fn ~sym ~opts)))

(defmacro instrument-ns
  "Instruments all functions in the namespace.
   Call with fully qualified quoted namespace:
   (instrument-ns 'cyrik.omni-trace.testing-ns)"
  ([sym]
   `(i/instrument-ns ~sym))
  ([sym opts]
   `(i/instrument-ns ~sym ~opts)))

(defmacro uninstrument-ns
  "Removes instrumentation.
   Call with fully qualified quoted namespace:
   (uninstrument-ns 'cyrik.omni-trace.testing-ns)"
  ([sym]
   `(i/uninstrument-ns ~sym))
  ([sym opts]
   `(i/uninstrument-ns ~sym ~opts)))

(defn reset-workspace! 
  ([]
   (i/reset-workspace! i/workspace))
  ([workspace]
   (i/reset-workspace! workspace)))

(defn flamegraph
  ([]
   (flamegraph i/workspace))
  ([workspace]
   (flame/flamegraph (flame/flamedata @workspace))))

(defn rooted-flamegraph
  ([root]
   (rooted-flamegraph root i/workspace))
  ([root workspace]
   (flame/flamegraph (flame/flamedata @workspace root))))

#?(:clj
   (defn run-traced [s & args]
     (apply #'deep/run-traced (into [s] args)))
   )


(defmacro run-traced-cljs [ns f & args]
  (let [ns (symbol (name ns))
         f (symbol (name f))
        dep-list (deep/transitive-deps (deep/deps (deep/analysis ["dev" "src"]) :cljs)
                                       ns f)
        sym-list  (mapv #(symbol (name (first %)) (name (second %))) (filter first dep-list)) ;;fix nil namespaces
        instrumenters (mapv (fn [sym] `#(cyrik.omni-trace.instrument.cljs/cljs-instrument-fn '~sym {:cyrik.omni-trace/workspace cyrik.omni-trace.instrument/workspace} cyrik.omni-trace.instrument/instrumented)) sym-list)
        deinstrumenters (mapv (fn [sym] `#(cyrik.omni-trace.instrument.cljs/cljs-instrument-fn '~sym {:cyrik.omni-trace/workspace cyrik.omni-trace.instrument/workspace} cyrik.omni-trace.instrument/uninstrumented)) sym-list)
        runner `#(~(symbol (name ns) (name f)))
        merged (into [](concat instrumenters [runner] deinstrumenters))
        ]
    `(doseq [f# ~merged
             ]
       (f#))
    )
  )

(comment
  (require '[portal.api :as p])
  (def portal (p/open))
  (add-tap #'p/submit)


  

  (filter #(and (= (:lang %) :cljs) (= (:from %) 'cyrik.omni-trace.testing-ns))(:var-usages(deep/analysis ["dev" "src"])))




  .
  )