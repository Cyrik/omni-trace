(ns cyrik.omni-trace.instrument.cljs
  (:require [net.cgrand.macrovich :as macros]
            #?(:clj [clojure.java.io :as io])
            [cljs.analyzer.api :as ana])
  #?(:cljs (:require-macros 
                            [cyrik.omni-trace.instrument.cljs :refer [cljs-instrument-fn]])))
(macros/deftime
  (defn vars-in-ns [sym]
    (if (ana/find-ns sym)
      (for [[_ v] (ana/ns-interns sym)
            :when (not (:macro v))]
        (:name v))
      []))

  (defn target-language [env]
    (if (contains? env :ns)
      :cljs
      :clj))

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
            _ (def local1 sym)
            _ (def local2 v)
            file (get-file &env (or (:file (:meta v))
                                    (:file v)));;incase of jar?
            meta* (assoc (:meta v) :file file)
            _ (def local3 meta*)] 
        (when-not (:macro v)
         `(when-let [instrumented# (~instrumenter '~sym (var ~sym) ~meta* nil ~opts)]
              (set! ~sym instrumented#)
              '~var-name)))))

  (defmacro cljs-instrument-ns [ns-sym opts instrumented]
    `(doseq [f# ~(->> ns-sym
                      eval
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts ~instrumented))))]
       (f#)))
  
  (defmacro cljs-uninstrument-ns [ns-sym opts uninstrumented]
    `(doseq [f# ~(->> ns-sym
                      eval
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts ~uninstrumented))))]
       (f#))))