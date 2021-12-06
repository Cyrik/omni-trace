(ns cyrik.omni-trace.instrument
  (:require [net.cgrand.macrovich :as macros]
            [cyrik.omni-trace.instrument.cljs :as cljs]
            [borkdude.dynaload :as dynaload]
            #?(:clj [cyrik.omni-trace.instrument.clj :as clj])))

(def read-result (dynaload/dynaload 'debux.common.util/result* {:default nil}))
(def reset-inner-result (dynaload/dynaload 'debux.common.util/reset-result {:default nil}))
(def register-inner-callback! (dynaload/dynaload 'debux.common.util/user-callback! {:default nil}))
(def debux-loaded? (if @register-inner-callback! true false))


(defonce instrumented-vars (atom {}))

(def empty-workspace {:log {} :maxed-callsites #{} :call-sites {}})
(defonce workspace (atom empty-workspace))

(defonce ns-blacklist (atom ['cljs.core 'clojure.core]))
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
  (if (< (get (:call-sites @workspace) (callsite trace) 0)
         (get opts :cyrik.omni-trace/max-callsite-log default-callsite-log))
    (do (swap! workspace assoc-in [:log id] trace)
        (swap! workspace update-in [:call-sites (callsite trace)] (fnil inc 0)))
    (swap! workspace assoc :maxed-callsites (callsite trace))))

(defn trace-fn-call [name f args meta* opts]
  (let [parent (or *trace-log-parent*
                   {:workspace (:cyrik.omni-trace/workspace opts) :parent :root})
        call-id (keyword (gensym ""))
        before-time (now)
        inner-result (atom [])
        _ (when debux-loaded?
            (register-inner-callback! (fn [result] (reset! inner-result result)(reset-inner-result))))
        this (assoc parent :parent call-id)
        res (binding [*trace-log-parent* this]
              (try
                (apply f args)
                (catch #?(:clj Throwable :cljs :default) t
                  (log (:workspace parent)
                       call-id
                       {:id call-id :meta meta* :name name :args args :start before-time :end (now) :parent (:parent parent) :inner @inner-result :thrown (#?(:clj Throwable->map :cljs identity) t)}
                       opts)
                  (throw t))))]
    (log (:workspace parent)
         call-id
         {:id call-id :meta meta* :name name :args args :start before-time :end (now) :parent (:parent parent) :return res :inner @inner-result}
         opts)
    res))

(defn reset-workspace! [workspace]
  (reset! workspace empty-workspace))


(defn instrumented [sym v meta* inner opts]
  (let [to-wrap @v]
    (when (and (fn? to-wrap)
               (not (:macro (meta v)))
               (not (contains? @instrumented-vars v))
              ;;  (do (println (:name (meta v))) true)
               (not (some #(= % (ns-name (:ns (meta v)))) @ns-blacklist))
               (not= (:name (meta v)) '=) ;;cljs
               (not= (:name (meta v)) 'assoc) ;;cljs
               (not= (:name (meta v)) 'inc) ;;cljs
               (not= (:name (meta v)) 'conj)
               (not= (:name (meta v)) 'swap!)
               (not= (:name (meta v)) 'update-in)
               (not= (:name (meta v)) 'map)
               (not= (:name (meta v)) 'first) ;;cljs
               (not= (:name (meta v)) 'apply)) ;;clj
      (let [orig (or inner
                     to-wrap)
            instrumented (fn
                           ([]
                            (trace-fn-call sym to-wrap [] meta* opts))
                           ([a]
                            (trace-fn-call sym to-wrap [a] meta* opts))
                           ([a b]
                            (trace-fn-call sym to-wrap [a b] meta* opts))
                           ([a b c]
                            (trace-fn-call sym to-wrap [a b c] meta* opts))
                           ([a b c d]
                            (trace-fn-call sym to-wrap [a b c d] meta* opts))
                           ([a b c d e]
                            (trace-fn-call sym to-wrap [a b c d e] meta* opts))
                           ([a b c d e f]
                            (trace-fn-call sym to-wrap [a b c d e f] meta* opts))
                           ([a b c d e f & args]
                            (trace-fn-call sym to-wrap (into [a b c d e f] args) meta* opts)))]
        (swap! instrumented-vars assoc v {:orig orig :instrumented instrumented})
        instrumented))))

(defn uninstrumented [sym v file inner opts]
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
     (macros/case :clj `(clj/clj-instrument-ns ~sym-or-syms ~opts clj/clj-instrument-fn instrumented)
                  :cljs `(cljs/cljs-instrument-ns ~sym-or-syms ~opts instrumented))))

  (defmacro uninstrument-ns
    "removes instrumentation"
    ([sym-or-syms]
     `(uninstrument-ns ~sym-or-syms {:cyrik.omni-trace/workspace workspace}))
    ([sym-or-syms opts]
     (macros/case :clj `(clj/clj-instrument-ns ~sym-or-syms ~opts clj/clj-instrument-fn uninstrumented)
                  :cljs `(cljs/cljs-uninstrument-ns ~sym-or-syms ~opts uninstrumented)))))

(comment
  (not (some #(= % (ns-name (:ns (meta #'clojure.core//)))) @ns-blacklist))
  )