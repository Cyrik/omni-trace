(ns omni-trace.omni-trace
  (:require #?(:clj [net.cgrand.macrovich :as macros])
            #?(:clj [cljs.analyzer :as analyzer])
            #?(:clj [clojure.java.io :as io])
            [cljs.analyzer.api :as ana])
            
  #?(:cljs (:require-macros [net.cgrand.macrovich :as macros]
                            [omni-trace.omni-trace :refer [instrument-ns uninstrument-ns]])))

(defonce instrumented-vars (atom {}))
(def ^:dynamic *trace-log-parent* nil)
(def empty-workspace {:log {} :max-callsites #{}})
(defonce workspace (atom empty-workspace))

(def default-callsite-log 100)

(defn now []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

(defn reset-workspace! []
  (reset! workspace empty-workspace))

(defn callsite [trace]
  [(:parent trace)(:name trace)])

(defn same-callsite? [trace1 trace2]
  (= (callsite trace1) (callsite trace2)))
(defn log [workspace id trace opts]
  (if (< (count (filter #(same-callsite? trace (second %)) (:log @workspace))) 
         (get opts ::max-callsite-log default-callsite-log))
    (swap! workspace assoc-in [:log id] trace)
    (swap! workspace assoc :max-callsites (callsite trace))))

(defn trace-fn-call [name f args file opts]
  (let [parent (or *trace-log-parent*
                   {:workspace (::workspace opts) :parent :root})
        call-id (keyword (gensym ""))
        before-time (now)
        this (assoc parent :parent call-id)
        res (binding [*trace-log-parent* this]
              (try
                (apply f args)
                (catch #?(:clj Throwable :cljs :default) t
                  (log (:workspace parent)
                       call-id
                       {:id call-id :file file :name name :args args :start before-time :end (now) :parent (:parent parent) :thrown (#?(:clj Throwable->map :cljs identity) t)}
                       opts)
                  (throw t))))]
    (log (:workspace parent) 
         call-id
         {:id call-id :file file :name name :args args :start before-time :end (now) :parent (:parent parent) :return res}
         opts)
    res))

(defn instrumented [sym v file opts]
  (let [to-wrap @v]
    (when (fn? to-wrap)
      (let [instrumented (fn [& args]
                           (trace-fn-call sym to-wrap args file opts))]
        (swap! instrumented-vars assoc v {:orig to-wrap :instrumented instrumented})
        instrumented))))

(defn uninstrumented [sym v file opts]
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

  #?(:clj
     (defn get-file [env file]
       (if (:ns env) ;; cljs target
         (if (= file "repl-input.cljs")
           (get-in env [:ns :meta :file])
           (if-let [classpath-file (io/resource file)]
             (.getPath classpath-file)
             file))
         *file*)))

  (defmacro cljs-instrument-fn [[_ sym] opts instrumenter]
    (when-let [v (ana/resolve &env sym)]
      (let [var-name (:name v)
            file #?(:clj (get-file &env (:file (:meta v))) :cljs nil)]
        `(when-let [instrumented# (~instrumenter '~sym (var ~sym) ~file ~opts)]


           (set! ~sym instrumented#)
           '~var-name))))

  (defmacro cljs-instrument-ns [ns-sym opts]
    `(doseq [f# ~(->> ns-sym
                      eval
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts instrumented))))]
       (f#)))

  (defmacro cljs-uninstrument-ns [ns-sym opts]
    `(doseq [f# ~(->> ns-sym
                      eval
                      vars-in-ns
                      (filter symbol?)
                      (distinct)
                      (mapv (fn [sym] `#(cljs-instrument-fn '~sym ~opts uninstrumented))))]
       (f#)))

  #?(:clj
     (defn clj-instrument-fn [sym opts instrumenter]
       (when-let [v (resolve sym)]
         (let [var-name (->sym v)]
           (when-let [instrumented-fn (instrumenter var-name v *file* opts)]
             (alter-var-root v (constantly instrumented-fn))
             var-name)))))

  (defn clj-instrument-ns [ns-sym opts mapper instrumenter]
    (->> ns-sym
         vars-in-ns-clj
         (filter symbol?)
         (distinct)
         (mapv (fn [sym] (mapper sym opts instrumenter)))
         (remove nil?)))

  (defmacro instrument-fn
    ([sym]
     `(instrument-fn ~sym {::workspace workspace}))
    ([sym opts]
     (macros/case :clj `(clj-instrument-fn ~sym ~opts instrumented)
                  :cljs `(cljs-instrument-fn ~sym ~opts instrumented))))

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