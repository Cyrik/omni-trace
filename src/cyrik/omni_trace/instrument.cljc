(ns cyrik.omni-trace.instrument
  (:require [net.cgrand.macrovich :as macros]
            [cyrik.omni-trace.instrument.cljs :as cljs]
            #?(:clj [cyrik.omni-trace.instrument.clj :as clj])))


(defonce instrumented-vars (atom {}))

(def empty-workspace {:log {} :max-callsites #{}})
(defonce workspace (atom empty-workspace))


(def ^:dynamic *trace-log-parent* nil)

(def default-callsite-log 100)


(defn now []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

(defn callsite [trace]
  [(:parent trace) (:name trace)])

(defn same-callsite? [trace1 trace2]
  (= (callsite trace1) (callsite trace2)))
(defn log [workspace id trace opts]
  (if (< (count (filter #(same-callsite? trace (second %)) (:log @workspace)))
         (get opts :cyrik.omni-trace/max-callsite-log default-callsite-log))
    (swap! workspace assoc-in [:log id] trace)
    (swap! workspace assoc :max-callsites (callsite trace))))

(defn trace-fn-call [name f args file opts]
  (let [parent (or *trace-log-parent*
                   {:workspace (:cyrik.omni-trace/workspace opts) :parent :root})
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

(defn reset-workspace! [workspace]
  (reset! workspace empty-workspace))


(defn instrumented [sym v file opts]
  (let [to-wrap @v]
    (when (and (fn? to-wrap)
               (not (:macro (meta v)))
               (not (contains? @instrumented-vars v))
               (not= (:name (meta v)) '=)
               (not= (:name (meta v)) 'assoc)
               (not= (:name (meta v)) 'conj)
               (not= (:name (meta v)) 'map)
               (not= (:name (meta v)) 'apply))
      (let [instrumented (fn [& args]
                           (trace-fn-call sym to-wrap args file opts))]
        (swap! instrumented-vars assoc v {:orig to-wrap :instrumented instrumented})
        instrumented))))

(defn uninstrumented [sym v file opts]
  (when-let [wrapped (@instrumented-vars v)]
    (swap! instrumented-vars dissoc v)
    (:orig wrapped)))

(macros/deftime
  (defmacro instrument-fn
    ([sym]
     `(instrument-fn ~sym {:cyrik.omni-trace/workspace workspace}))
    ([sym opts]
     (macros/case :clj `(clj/clj-instrument-fn ~sym ~opts instrumented)
                  :cljs `(cljs/cljs-instrument-fn ~sym ~opts instrumented))))

  (defmacro uninstrument-fn
    ([sym]
     `(uninstrument-fn ~sym {:cyrik.omni-trace/workspace workspace}))
    ([sym opts]
     (macros/case :clj `(clj/clj-instrument-fn ~sym ~opts uninstrumented)
                  :cljs `(cljs/cljs-instrument-fn ~sym ~opts uninstrumented))))

  (defmacro instrument-ns
    ([sym-or-syms]
     `(instrument-ns ~sym-or-syms {:cyrik.omni-trace/workspace workspace}))
    ([sym-or-syms opts]
     (macros/case :clj `(clj/clj-instrument-ns ~sym-or-syms ~opts clj-instrument-fn instrumented)
                  :cljs `(cljs/cljs-instrument-ns ~sym-or-syms ~opts instrumented))))

  (defmacro uninstrument-ns
    "removes instrumentation"
    ([sym-or-syms]
     `(uninstrument-ns ~sym-or-syms {:cyrik.omni-trace/workspace workspace}))
    ([sym-or-syms opts]
     (macros/case :clj `(clj/clj-instrument-ns ~sym-or-syms ~opts clj-instrument-fn uninstrumented)
                  :cljs `(cljs/cljs-uninstrument-ns ~sym-or-syms ~opts uninstrumented)))))