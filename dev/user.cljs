(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.cljs-macroexpand :as macro]
            [cyrik.omni-trace.testing-ns :as testin-ns]
            [portal.console :as console]
            [test-console :as tc]
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
  (o/instrument-fn 'test-inc)
  (test-inc 5)
  ;run functions in that namespace
  (-> e/machine-init
      (e/insert-coin :quarter)
      (e/insert-coin :dime)
      (e/insert-coin :nickel)
      (e/insert-coin :penny)
      (e/press-button :a1)
      (e/retrieve-change-returned))
  ;look at traces for every function that was traced
  @o/workspace
  ;connect to portal

  (add-tap print)
  ;send the trace to portal as a vegajs flamegraph
  (tap> (flame/flamegraph (flame/flamedata @o/workspace)))
  (tap> ^{:portal.viewer/default :portal.viewer/hiccup} [:button {:onclick (str "alert('123')")} (str "alert('123')")])
  [:button {:onclick "alert('123')"} "Test it!"]
  ;remove tracing from a namesapce
  (o/uninstrument-ns 'omni-trace.testing-ns)
  (o/reset-workspace!)
  o/instrumented-vars
  (macroexpand '(o/instrument-ns 'cyrik.omni-trace.testing-ns))
  (macroexpand '(o/instrument-fn 'omni-trace.testing-ns/insert-coin))
  (macro/cljs-macroexpand-all '(o/instrument-ns 'portal.web))
  (macro/cljs-macroexpand-all '(o/instrument-fn 'omni-trace.testing-ns/insert-coin))
  (test-log 5)
  (macro/cljs-macroexpand-all '(tc/log 5))
  (alter-meta! (var cyrik.omni-trace.testing-ns/insert-coin) assoc-in [:stuffs] "yeah123")
  (.log js/console (meta (var cyrik.omni-trace.testing-ns/insert-coin)))

  (when-let [open js/OpenFileInEditor] (open "some/file.cljs" 10 1))
  (.log js/console (throw (js/Error. "Oops")))
  ((fn [] (/ 1 0)))
  (try (test-throw-full 5) (catch :default e (.log js/console e)))
  (try (test-throw-full 5) (catch :default e (doto (js->clj e) (.log js/console e))))
  .)
  
  