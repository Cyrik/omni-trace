(ns cyrik.omni-trace.instrument.cljs
  (:require [net.cgrand.macrovich :as macros]
            #?(:clj [clojure.java.io :as io])
            #?(:clj [cljs.core])
            [clojure.string :as str]
            [cljs.analyzer.api :as ana])
  #?(:cljs (:require-macros
            [cyrik.omni-trace.instrument.cljs :refer [cljs-instrument-fn]])))
(macros/deftime
  (defonce instrumented-var-names (atom #{}))
  (defn vars-in-ns [sym]
    (if (ana/find-ns sym)
      (for [[_ v] (ana/ns-interns sym)
            :when (not (:macro v))]
        (:name v))
      (throw (Exception. (str "ns " sym " does not exist.")))))

  (defn target-language [env]
    (if (contains? env :ns)
      :cljs
      :clj))

  (defn var->sym [v]
    (let [m (meta v)]
      (symbol (name (ns-name (:ns m))) (name (:name m)))))

  (defn ->sym [something]
    (cond
      (and (seq? something) (= (first something) 'quote)) (eval something)
      (and (seq? something) (= (first something) 'var)) (var->sym (eval something))
      (string? something) (apply symbol (str/split something #"/"))
      :else nil))

  (defn ->ns [something]
    (cond
      (and (seq? something) (= (first something) 'quote)) (ana/find-ns (eval something))
      (symbol? something) (ana/find-ns something)
      (string? something) (ana/find-ns (symbol something))
      :else nil))

  #?(:clj
     (defn get-file [env file]
       (if (= :cljs (target-language env))
         (if (= file "repl-input.cljs")
           (get-in env [:ns :meta :file]) ;might always hold the right one @todo check later
           (if-let [classpath-file (io/resource file)]
             (.getPath classpath-file)
             file))
         file)))

  (defmacro cljs-instrument-fn [[_ sym] opts instrumenter]
    (when-let [v (ana/resolve &env sym)]
      (let [var-name (:name v)
            file (get-file &env (or (:file (:meta v))
                                    (:file v)));;incase of jar?
            meta* (assoc (:meta v) :file file)]
        (when-not (:macro v)
          (swap! instrumented-var-names conj var-name)
          `(when-let [instrumented# (~instrumenter '~sym (var ~sym) ~meta* nil ~opts)]
             (set! ~sym instrumented#)
             '~var-name)))))

  (defmacro cljs-uninstrument-fn [[_ sym] opts instrumenter]
    (when-let [v (ana/resolve &env sym)]
      (let [var-name (:name v)
            file (get-file &env (or (:file (:meta v))
                                    (:file v)));;incase of jar?
            meta* (assoc (:meta v) :file file)]
        (when-not (:macro v)
          (swap! instrumented-var-names disj var-name)
          `(when-let [instrumented# (~instrumenter '~sym (var ~sym) ~meta* nil ~opts)]
             (set! ~sym instrumented#)
             '~var-name)))))

  (defmacro cljs-instrument-ns [ns-sym opts instrumented]
    `(doseq [f# ~(->> ns-sym
                      ->sym
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts ~instrumented))))]
       (f#)))

  (defmacro cljs-uninstrument-ns [ns-sym opts uninstrumented]
    `(doseq [f# ~(->> ns-sym
                      ->sym
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-uninstrument-fn '~sym ~opts ~uninstrumented))))]
       (f#)))

  (defmacro instrument [s opts instrumenter]
    (let [xs (if (vector? s) s [s])
          syms (map #(->sym %) xs)
          fn-syms (mapcat (fn [sym] (if (str/includes? (name sym) ".")
                                      (vars-in-ns sym)
                                      [sym]))
                          syms)]
      `(doseq [f# ~(->> fn-syms
                        (filter symbol?)
                        (distinct)
                        (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts ~instrumenter))))]
         (f#))))
  
  (defmacro uninstrument [s opts instrumenter]
    (let [xs (if (vector? s) s [s])
          syms (map #(->sym %) xs)
          fn-syms (mapcat (fn [sym] (if (str/includes? (name sym) ".")
                                      (vars-in-ns sym)
                                      [sym]))
                          syms)
          fn-syms (if (empty? xs) (into [] @instrumented-var-names) fn-syms)]
      `(doseq [f# ~(->> fn-syms
                        (filter symbol?)
                        (distinct)
                        (mapv (fn [sym] `#(cljs-uninstrument-fn '~sym ~opts ~instrumenter))))]
         (f#)))))


(comment
  (require '[cyrik.cljs-macroexpand :as macro])
  (cyrik.omni-trace.instrument.cljs/sym-test "cljs.core/inc")
  (macro/cljs-macroexpand-all '(cyrik.omni-trace.instrument.cljs/instrument ["cyrik.omni-trace.testing-ns" #'cljs.core/inc] {} #()))
  (instrument "cyrik.omni-trace.testing-ns"  {} #())
  (macroexpand '(uninstrument [] {} #()))
  (macroexpand '(cyrik.omni-trace.instrument.cljs/instrument #'clojure.core/inc {} #()))
  (cyrik.omni-trace.instrument.cljs/->sym #'cyrik.omni-trace.testing-ns/insert-coin)
  (macroexpand '(cyrik.omni-trace.instrument.cljs/->sym #'cyrik.omni-trace.testing-ns/insert-coin))
  (meta #'cyrik.omni-trace.instrument.cljs/test123)
  (type (macro/cljs-macroexpand-all '(cyrik.omni-trace.instrument.cljs/->sym "cyrik.omni-trace.testing-ns/insert-coin")))
  (type #'cyrik.omni-trace.testing-ns/insert-coin)
  )