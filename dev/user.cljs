(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.cljs-macroexpand :as macro]
            [portal.console :as console]
            [cyrik.omni-trace.instrument :as i]
            [test-console :as tc]
            [cljs.analyzer :as ana]
            [cyrik.omni-trace.graph :as flame]
            [portal.web :as p]))

(defn test-inc [x]
  (inc x))

(defn test-throw [x]
  (throw "a")
  (inc x))

(defn test-throw-full [x]
  (throw (js/Error. "Oops"))
  (inc x))

(defn test-log [x]
  (tc/log x)
  (inc x))

(test-log 1)

(defonce portal (p/open))
(add-tap #'p/submit)

(comment
  ;instrument a namespace
  (o/instrument-ns 'cyrik.omni-trace.testing-ns)
  ;or instrument a single function
  (o/instrument-fn 'user/test-inc)

  (test-inc 5)
  ;run functions in that namespace
  (-> e/machine-init
      (e/insert-coin :quarter)
      (e/insert-coin :dime)
      (e/insert-coin :nickel)
      (e/insert-coin :penny)
      (e/press-button :a1)
      (e/retrieve-change-returned)) ;; this is supposed to throw to show exception handling in omni-trace
  ;look at traces for every function that was traced
  @i/workspace
  ;connect to portal

  (add-tap print)
  ;send the trace to portal as a vegajs flamegraph
  (tap> (flame/flamegraph (flame/flamedata @i/workspace)))
  (tap> ^{:portal.viewer/default :portal.viewer/hiccup} [:button {:onclick (str "alert('123')")} (str "alert('123')")])
  [:button {:onclick "alert('123')"} "Test it!"]
  ;remove tracing from a namesapce
  (o/uninstrument-ns 'omni-trace.testing-ns)
  (o/reset-workspace!)
  i/instrumented-vars
  (macroexpand '(o/instrument-ns 'cyrik.omni-trace.testing-ns))
  (macroexpand '(o/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin))
  (macro/cljs-macroexpand-all '(o/instrument-ns 'portal.web))
  (macro/cljs-macroexpand-all '(o/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin))
  (test-log 5)
  (macro/cljs-macroexpand-all '(tc/log 5))
  (alter-meta! (var cyrik.omni-trace.testing-ns/insert-coin) assoc-in [:stuffs] "yeah123")
  (.log js/console (meta (var cyrik.omni-trace.testing-ns/insert-coin)))

  (macro/cljs-macroexpand-all '(o/instrument-fn 'cljs.core/keep))
  (reset! i/ns-blacklist [])
  (o/run-traced-cljs "cyrik.omni-trace.testing-ns" "run-machine")
  (macro/cljs-macroexpand-all '(o/run-traced-cljs "cyrik.omni-trace.testing-ns" "run-machine"))

  (mapv #(first %) #{['cljs.core '+]})
  (Math/round 1.5)
  (when-let [open js/OpenFileInEditor] (open "some/file.cljs" 10 1))
  (.log js/console (throw (js/Error. "Oops")))
  ((fn [] (/ 1 0)))
  (try (test-throw-full 5) (catch :default e (.log js/console e)))
  (try (test-throw-full 5) (catch :default e (doto (js->clj e) (.log js/console e))))



  ;; getting multi arity to work
  (o/instrument-fn 'cljs.core/+)
  (let*
   [temp__5753__auto__
    (cyrik.omni-trace.instrument/instrumented
     'cljs.core/+
     #'cljs.core/+
     "/Users/lukas/Workspace/clojure/omni-trace/src/cyrik/omni_trace/testing_ns.cljc"
     #:cyrik.omni-trace{:workspace cyrik.omni-trace.instrument/workspace})]
   (if
    temp__5753__auto__
     (do
       (let*
        [instrumented__134281__auto__ temp__5753__auto__
         ayyyy (.assign js/Object (js/Object.) cljs.core/keep instrumented__134281__auto__)]
        ;; (.log js/console (.-__proto__ instrumented__134281__auto__))
        ;; (.log js/console instrumented__134281__auto__)
        ;; (.log js/console ayyyy)
        (.log js/console (.assign js/Object instrumented__134281__auto__ cljs.core/+))
        ;; (.log js/console (set! (.-__proto__ (.assign js/Object (js/Object.) instrumented__134281__auto__ cljs.core/keep))
        ;;                        instrumented__134281__auto__))
        ;; (set! cljs.core/keep (.assign js/Object (js/Object.) instrumented__134281__auto__ cljs.core/keep))
        ;; (set! (.-__proto__ ayyyy) (.-__proto__ instrumented__134281__auto__))
        ;; (set! (.-prototype ayyyy) (.-prototype instrumented__134281__auto__))
        (set! cljs.core/+ instrumented__134281__auto__)
        'cljs.core/+))))
  (let*
   [temp__5753__auto__
    (cyrik.omni-trace.instrument/instrumented
     'cljs.core/+
     #'cljs.core/+
     "/Users/lukas/Workspace/clojure/omni-trace/src/cyrik/omni_trace/testing_ns.cljc"
     #:cyrik.omni-trace{:workspace cyrik.omni-trace.instrument/workspace})]
   (if
    temp__5753__auto__
     (do
       (let*
        [instrumented__134281__auto__ temp__5753__auto__
         ayyyy (.assign js/Object (js/Object.) cljs.core/keep instrumented__134281__auto__)]
        ;; (.log js/console (.-__proto__ instrumented__134281__auto__))
        ;; (.log js/console instrumented__134281__auto__)
        ;; (.log js/console ayyyy)
        (.log js/console (.assign js/Object instrumented__134281__auto__ cljs.core/+))
        ;; (.log js/console (set! (.-__proto__ (.assign js/Object (js/Object.) instrumented__134281__auto__ cljs.core/keep))
        ;;                        instrumented__134281__auto__))
        ;; (set! cljs.core/keep (.assign js/Object (js/Object.) instrumented__134281__auto__ cljs.core/keep))
        ;; (set! (.-__proto__ ayyyy) (.-__proto__ instrumented__134281__auto__))
        ;; (set! (.-prototype ayyyy) (.-prototype instrumented__134281__auto__))
        (set! cljs.core/+ instrumented__134281__auto__)
        'cljs.core/+))))
  (defn arity ([] (set! alter-meta! 1)) ([a] (inc 1)))
  (set! cljs.core/keep arity)
  .
  )
  
  