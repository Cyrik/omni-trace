(ns cyrik.omni-trace
  (:require
   #?(:clj [cyrik.omni-trace.deep-trace :as deep])
   [cyrik.omni-trace.instrument :as i]
   [cyrik.omni-trace.graph :as flame])

  #?(:cljs (:require-macros [net.cgrand.macrovich :as macros]
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
     (apply #'deep/run-traced (into [s] args))))

(comment
  (require '[portal.api :as p])
  (def portal (p/open))
  (add-tap #'p/submit)


  (name (:ns (meta (resolve 'user/run-machine))))
  (name 'user/run-machine)






  .)