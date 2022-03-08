(ns cyrik.omni-trace
  (:require
   #?(:clj [cyrik.omni-trace.deep-trace :as deep])
   #?(:clj [cyrik.omni-trace.util :as util])
   [cyrik.omni-trace.instrument :as i]
   [cyrik.omni-trace.graph :as flame]
   [cljs.test]
   #?(:clj [cljs.analyzer.api :as ana-api])
   [net.cgrand.macrovich :as macros])
  #?(:cljs (:require-macros
            [cyrik.omni-trace :refer [instrument-fn uninstrument-fn instrument-ns uninstrument-ns
                                      run]])))

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
   `(i/uninstrument-fn ~sym))
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

(defmacro trace
  "Instruments all functions in passed namespaces or symbols.
   syms can be a fully qualified symbol, a string or a var pointing to a namespace
   or a function. A vector of syms can also be passed.
   (instrument ['cyrik.omni-trace.testing-ns])"
  ([syms]
   `(i/instrument ~syms))
  ([syms opts]
   `(i/instrument ~syms ~opts)))

(defmacro untrace
  "Instruments all functions in passed namespaces or symbols.
   syms can be a fully qualified symbol, a string or a var pointing to a namespace
   or a function. A vector of syms can also be passed.
   (uninstrument 'cyrik.omni-trace.testing-ns)"
  ([]
   `(i/uninstrument))
  ([syms]
   `(i/uninstrument ~syms))
  ([syms opts]
   `(i/uninstrument ~syms ~opts)))

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
     (apply #'deep/run-traced {:cyrik.omni-trace/workspace i/workspace} (into [s] args))))
(macros/deftime
  #?(:clj
     (defmacro run 
       "Runs the form in traced mode. Does not work if the form starts with a macro."
       [form]       
       (macros/case :clj `(deep/run-traced {:cyrik.omni-trace/workspace i/workspace} (~util/->sym ~(first form)) ~@(rest form))
                    :cljs `(do
                             ~(let [fun (ana-api/resolve &env (first form))
                                    args (rest form)
                                    n (:ns fun)
                                    f (-> fun
                                          :name
                                          name
                                          symbol)
                                    dep-list (deep/transitive-deps (deep/dependencies
                                                                    (deep/analysis ["dev" "src"]) :cljs)
                                                                   n f)
                                    sym-list  (mapv #(symbol (name (first %)) (name (second %))) (filter first dep-list)) ;;fix nil namespaces
                                    instrumenters (mapv (fn [sym] `#(cyrik.omni-trace.instrument.cljs/cljs-instrument-fn '~sym {:cyrik.omni-trace/workspace cyrik.omni-trace.instrument/workspace} cyrik.omni-trace.instrument/instrumented)) sym-list)
                                    deinstrumenters (mapv (fn [sym] `#(cyrik.omni-trace.instrument.cljs/cljs-instrument-fn '~sym {:cyrik.omni-trace/workspace cyrik.omni-trace.instrument/workspace} cyrik.omni-trace.instrument/uninstrumented)) sym-list)]
                                `(let [_# (doseq [f# ~instrumenters]
                                            (f#))
                                       result# (apply ~(symbol (name n) (name f)) (list ~@args))
                                       _# (doseq [g# ~deinstrumenters]
                                            (g#))]
                                   result#)))))))


(comment
  (require '[portal.api :as p])
  (def portal (p/open))
  (add-tap #'p/submit)



  (filter #(and (= (:lang %) :cljs) (= (:from %) 'cyrik.omni-trace.testing-ns)) (:var-usages (deep/analysis ["dev" "src"])))



  (require '[cyrik.cljs-macroexpand :as macro])
  (clojure.walk/macroexpand-all '(run (+ 1 2)))
  (macro/cljs-macroexpand-all '(run `(+ 1 2)))
  (macroexpand '(run `(+ 1 2)))
  (defn thing [a]
    (inc a))
  (defn thing2 [a b]
    (+ a b))
  (run `(thing (inc (inc 1))))
  (macroexpand '(run `(thing (inc (inc 1)))))
  (macro/cljs-macroexpand-all '(run `(thing (inc (inc 1)))))
  (cyrik.omni-trace/run (thing2 1 2))
  (macroexpand '(cyrik.omni-trace/run (thing2 1 2)))
  (macro/cljs-macroexpand-all '(cyrik.omni-trace/run (thing2 1 2)))
  .)