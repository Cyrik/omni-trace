(ns omni-trace.omni-trace
  (:require #?(:clj [net.cgrand.macrovich :as macros]
               )
            [cljs.analyzer.api :as ana])
            
  #?(:cljs (:require-macros [net.cgrand.macrovich :as macros]
                            [omni-trace.omni-trace :refer [instrument-ns uninstrument-ns]])))

(defonce instrumented-vars (atom {}))
(def ^:dynamic *trace-log-parent* nil)
(defonce workspace (atom {}))

(defn now []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

(defn trace-fn-call [name f args opts]
  (let [parent (or *trace-log-parent*
                   {:workspace (::workspace opts) :parent :root})
        call-id (keyword (gensym))
        before-time (now)
        this (assoc parent :parent call-id)
        res (binding [*trace-log-parent* this]
              (apply f args))]
    (swap! (:workspace parent) 
           #(assoc % call-id {:id call-id :name name :args args :start before-time :end (now) :parent (:parent parent) :return res}))
    res))

(defn instrumented [sym v opts]
  (let [to-wrap @v]
    (when (fn? to-wrap)
      (let [instrumented (fn [& args]
                           (trace-fn-call sym to-wrap args opts))]
        (swap! instrumented-vars assoc v {:orig to-wrap :instrumented instrumented})
        instrumented))))

(defn uninstrumented [sym v opts]
  (when-let [wrapped (@instrumented-vars v)]
    (swap! instrumented-vars dissoc v)
    (:orig wrapped)))

(defn vars-in-ns [sym]
  (if (ana/find-ns sym)
    (for [[_ v] (ana/ns-interns sym)
          :when (not (:macro v))]
      (:name v))
    []))

(macros/deftime
 (defn ->sym [v]
   (let [meta (meta v)]
     (symbol (name (ns-name (:ns meta))) (name (:name meta)))))
 
 (defn vars-in-ns-clj [sym]
   (if (find-ns sym)
     (for [[_ v] (ns-interns sym)
           :when (not (:macro (meta v)))]
       (->sym v))
     []))
 
  (defmacro cljs-instrument-fn [[_ sym] opts]
    (when-let [v (ana/resolve &env sym)]
      (let [var-name (:name v)]
        `(when-let [instrumented# (instrumented '~sym (var ~sym) ~opts)]
          ;;  (alter-meta! ~v assoc-in [:omni] "yeah")
          ;;  (vary-meta ~v assoc-in [:omni] "yeah")
          ;;  (alter-meta! (var ~sym) assoc-in [:omni] "yeah123")
           (set! ~sym instrumented#)
          ;;  (alter-meta! (var ~sym) assoc-in [:omni2] "yeah2")
          ;;  (alter-meta! ~v assoc-in [:omni2] "yeah123")
           '~var-name))))

  (defmacro cljs-uninstrument-fn [[_ sym] opts]
    (when-let [v (ana/resolve &env sym)]
      (let [var-name (:name v)]
        `(when-let [uninstrumented# (uninstrumented '~sym (var ~sym) ~opts)]
           (set! ~sym uninstrumented#)
           '~var-name))))
  
  (defmacro cljs-instrument-ns [ns-sym opts]
    `(doseq [f# ~(->> ns-sym
                      eval
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts))))]
       (f#)))
  
  (defmacro cljs-uninstrument-ns [ns-sym opts]
    `(doseq [f# ~(->> ns-sym
                      eval
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-uninstrument-fn '~sym ~opts))))]
       (f#)))
  
  

  (defn clj-instrument-fn [sym opts instrumenter]
    (when-let [v (resolve sym)]
      (let [var-name (->sym v)]
        (when-let [instrumented-fn (instrumenter sym v opts)]
          #?(:clj (alter-var-root v (constantly instrumented-fn)))
          var-name))))




  (defn clj-instrument-ns [ns-sym opts mapper instrumenter]
    (->> ns-sym
         vars-in-ns-clj
         (filter symbol?)
         (distinct)
         (mapv (fn [sym] (mapper sym opts instrumenter)))
         (remove nil?)))
  
  (defmacro instrument-ns
    ([sym-or-syms]
     `(instrument-ns ~sym-or-syms {::workspace workspace}))
    ([sym-or-syms opts]
     (macros/case :clj `(clj-instrument-ns ~sym-or-syms ~opts clj-instrument-fn instrumented)
                  :cljs `(cljs-instrument-ns ~sym-or-syms ~opts))))
  
  (defmacro uninstrument-ns
    "removes instrumentation"
    ([sym-or-syms]
     `(uninstrument-ns ~sym-or-syms {::workspace workspace}))
    ([sym-or-syms opts]
     (macros/case :clj `(clj-instrument-ns ~sym-or-syms ~opts clj-instrument-fn uninstrumented)
                  :cljs `(cljs-uninstrument-ns ~sym-or-syms ~opts)))))
  

(defn init [& args]
  (print "yeah"))

(comment

  .)