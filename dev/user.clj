(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.omni-trace.instrument :as i]
            [clojure.java.io :as io]
            [portal.api :as p]
            [criterium.core :as crit]
            [debux.core :as d]
            [clojure.walk :as walk]
            [clojure.tools.namespace.repl]
            [advent.day1]
            [clj-memory-meter.core :as mm]
            [zprint.core :as zp]
            [clojure.tools.deps.alpha.repl :as repl]
            [debux.common.util :as ut]
            [clojure.pprint :as pprint]
            [cyrik.omni-trace.graph :as flame]))

(repl/add-libs '{org.clojure/tools.namespace {:mvn/version "1.1.0"}})
(defn spit-pretty!
  "Writes the pretty-printed edn `data` into the `file`."
  [file data]
  (spit (io/file file)
        (with-out-str
          (pprint/pprint data))))

(defn factorial [acc n]
  (if (zero? n)
    acc
    (factorial (* acc n) (dec n))))

(defn foo [a b & [c]]
  (if c
    (* a b c)
    (* a b 100)))

(defn fact [num]
  (loop [acc 1 n num]
    (if (zero? n)
      acc
      (recur (* acc n) (dec n)))))

(defn test-inc [x]
  (inc x))
(defn test-log [x]
  (inc x))
(test-log 5)
(defonce portal (p/open))
(add-tap #'p/submit)
(comment
  (portal.runtime/register! (partial into {}) {:name 'dev/->map})
  "https://github.com/Cyrik/omni-trace"
  ;; tracing + visualization + tool integrations
  ;; debug your own code + understand calls into libs

  ;; run-traced
  (o/reset-workspace!)
  (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)
  (tap> (o/flamegraph))
  (o/reset-workspace!)

  ;; inner trace
  (ut/set-use-result-atom! true)
  (o/instrument-fn 'user/fact {:cyrik.omni-trace/workspace i/workspace :inner-trace true})
  (fact 3)
  (:log @i/workspace)
  (o/reset-workspace!)
  (o/uninstrument-fn 'user/fact)
  (reset! i/instrumented-vars {})

  ;; deep into core
  (reset! i/ns-blacklist [])
  (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)
  (tap> (o/flamegraph))
  (o/reset-workspace!)
  (reset! i/ns-blacklist ['cljs.core 'clojure.core])

  ;; max callsites
  (o/instrument-fn 'user/test-inc)
  (mapv test-inc (range 1000))
  (count (:log @i/workspace))
  (:maxed-callsites @i/workspace)
  (o/reset-workspace!)
  (o/uninstrument-fn 'user/test-inc)

  ;; catch thrown
  (o/instrument-ns 'cyrik.omni-trace.testing-ns)
  (-> e/machine-init
      (e/insert-coin :quarter)
      (e/insert-coin :dime)
      (e/insert-coin :nickel)
      (e/insert-coin :penny)
      (e/press-button :a1)
      (e/retrieve-change-returned))
  (tap> (o/flamegraph))
  (o/uninstrument-ns 'cyrik.omni-trace.testing-ns)
  (o/reset-workspace!)

  ;; nearly done: - key press to jump to definition
  ;; - key press to "put execution into copy" 
  ;;   -> (apply cyrik.omni-trace.testing-ns/insert-coin @o/call-args)

  ;; todo: - update graph data on the fly
  ;; - push less data into the vis, then push more on zoom
  ;; - integration with reveal / clerk and so on
  ;; - its own viewer?
  ;; - deep-trace into core with fewer problems


  (o/instrument-ns 'cyrik.omni-trace.testing-ns
                   {::o/workspace i/workspace})

  (e/run-machine)

  (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)
  (tap> (flame/flamedata @i/workspace 'cyrik.omni-trace.testing-ns/run-machine))
  (tap> (o/rooted-flamegraph 'cyrik.omni-trace.testing-ns/run-machine))
  (tap> (o/flamegraph))

  i/instrumented-vars

  (tap> ^{:portal.viewer/default :portal.viewer/hiccup}
   [:div "custom thing here" [:portal.viewer/inspector {:complex :data-structure}]])

  (o/uninstrument-ns 'omni-trace.testing-ns)
  (o/uninstrument-fn 'user/test-inc)
  (walk/macroexpand-all '(o/instrument-ns 'omni-trace.testing-ns))

  (def values (vals (:log @i/workspace)))

  (crit/with-progress-reporting (crit/quick-bench (reduce (fn [acc {:keys [id] :as user}]
                                                            (into acc {id user})) {} values) :verbose))

  (crit/with-progress-reporting (crit/quick-bench (into {} (map (fn [[id user-list]]
                                                                  {id (first user-list)}) (group-by :id values))) :verbose))


  (crit/with-progress-reporting (crit/quick-bench (into {} (map (juxt :id identity)) values) :verbose))
  (crit/with-progress-reporting (crit/quick-bench (zipmap (map :id values) values) :verbose))


  (count (:log @i/workspace))
  (o/reset-workspace!)
  (intern *ns* '~'symname "<<symdefinition>>")
  (dotimes [x 100] (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine))
  (tap> (o/flamegraph))
  ut/config*

  (o/instrument-fn 'user/foo {::o/workspace i/workspace :inner-trace true})
  (o/instrument-fn 'user/factorial {::o/workspace i/workspace :inner-trace true})
  (o/instrument-fn 'user/fact {::o/workspace i/workspace :inner-trace true})

  (foo 2 3)
  (factorial 1 3)
  (fact 3)
  (o/uninstrument-fn 'user/test-inc)
  (o/uninstrument-fn 'user/factorial)
  (o/uninstrument-fn 'user/fact)
  (reset! ut/result* [])
  (o/reset-workspace!)
  (reset! i/instrumented-vars {})

  (walk/macroexpand-all '(d/dbgn (defn factorial [acc n]
                                   (if (zero? n)
                                     acc
                                     (factorial (* acc n) (dec n))))))

  (macroexpand '(debux.dbgn/dbgn (defn fact [num]
                                   (loop [acc 1 n num]
                                     (if (zero? n)
                                       acc
                                       (recur (* acc n) (dec n)))))
                                 (clojure.core/zipmap 'nil [])
                                 {:evals (atom {}), :line 106, :ns "user"}))

  (d/dbgn (defn fact [num]
            (loop [acc 1 n num]
              (if (zero? n)
                acc
                (recur (* acc n) (dec n))))))

  .
  )
