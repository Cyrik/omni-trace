(ns user
  (:require [advent.day1]
            [clj-async-profiler.core :as prof]
            [clj-memory-meter.core :as mm]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.tools.namespace.repl]
            [clojure.walk :as walk]
            [criterium.core :as crit]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.graph :as flame]
            [cyrik.omni-trace.instrument :as i]
            [cyrik.omni-trace.testing-ns :as e]
            [debux.common.util :as ut]
            [debux.core :as d]
            [portal.api :as p]
            [portal.runtime :as rt]
            portal.runtime.jvm.editor)
  (:import (org.openjdk.jol.info GraphLayout)))

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
{:a :b}
(defn test-inc [x]
  (inc x))
(defn test-log [x]
  (inc x))
(test-log 5)
(type (p/eval-str "(+ 1 2 3)"))
(add-tap #'p/submit)
(defn goto-definition [args]
  (portal.runtime.jvm.editor/goto-definition args))

(portal.runtime.jvm.editor/goto-definition {:ns nil, :name "factorial", :file "/Users/lukas/Workspace/clojure/omni-trace/dev/demo.clj", :end-column 16, :source "factorial", :column 1, :coor [1], :line 52, :end-line 46, :arglists [["acc" "n"]]})
(p/open {:editor :vs-code})
(rt/register! #'goto-definition)
(p/eval-str (str '(ns mine
                    (:require ["vega-embed" :as vegaEmbed]
                              [portal.ui.rpc :as rpc]
                              [portal.ui.api :as api]
                              [portal.ui.inspector :as inspector]))
                 '(defn logger [name value command]
                    (.log js/console (clj->js [name (js->clj value :keywordize-keys true) command])))
                 '(defn add-signal-listner [value signal command]
                    (. (.-view value) addSignalListener signal
                       (fn [name value]
                         (logger name value command)
                         (rpc/call command (js->clj value :keywordize-keys true)))))
                 '(js/console.log vegaEmbed/default)
                 '(api/register-viewer!
                   {:predicate map?
                    :component (fn [val]
                                 (reagent.core/create-class
                                  {:display-name "vega-test3"
                                   :component-did-mount (fn [this]
                                                          (js/console.log (clj->js val))
                                                          (-> (vegaEmbed/default (reagent.dom/dom-node this) (clj->js val)
                                                                                 {:renderer :canvas
                                                                                  :mode "vega-lite"})
                                                              (.then (fn [value]
                                                                                ;; (set! (.-current view) (.-view value))
                                                                       (. (.-view value) addSignalListener "clicked" logger)
                                                                       (. (.-view value) addSignalListener "key" logger)
                                                                       (doseq [{:keys [:signal :command]} (:portal-opts val)]
                                                                         (add-signal-listner value signal command))))))
                                   :component-did-update (fn [this [_ new-doc new-opts]]
                                                           (vegaEmbed/default (reagent.dom/dom-node this) new-doc {:renderer :canvas
                                                                                                                   :mode "vega-lite"}))
                                   :reagent-render (fn [val]
                                                     [:div.viz])}))
                    :name :portal.viewer/vega-test23})))
(p/eval-str (str '(ns mine
                    (:require ["vega-embed" :as vegaEmbed]
                              [portal.ui.rpc :as rpc]
                              [portal.ui.api :as api]
                              [portal.ui.inspector :as inspector]))
                 '(defn logger [name value command]
                    (.log js/console #_(clj->js ["abc"])(clj->js [name (js->clj value :keywordize-keys true) command])))
                 '(logger "a" "b" "c")))
;; (defn goto-definition [_ args]
;;   (portal.runtime.jvm.editor/goto-definition args))
(rt/register! #'user/goto-definition)
(comment
  (def portal (p/open {:editor :vs-code :theme :portal.colors/solarized-light :portal.launcher/window-title (System/getProperty "user.dir")}))
  (p/close)
  (portal.runtime/register! (partial into {}) {:name 'dev/->map})
  "https://github.com/Cyrik/omni-trace"
  ;; tracing + visualization + tool integrations
  ;; debug your own code + understand calls into libs
  (cyrik.omni-trace.vscode/run (cyrik.omni-trace.testing-ns/run-machine))
  ;; run-traced
  (o/reset-workspace!)
  (o/run (cyrik.omni-trace.testing-ns/run-machine))
  (tap> (o/flamegraph))
  (o/reset-workspace!)

  ;; inner trace
  (ut/set-use-result-atom! true)
  (o/instrument-fn 'user/fact {:cyrik.omni-trace/workspace i/workspace :inner-trace true})
  (:log @i/workspace)
  (o/reset-workspace!)
  (o/uninstrument-fn 'user/fact)
  (reset! i/instrumented-vars {})

  ;; deep into core
  (reset! i/ns-blacklist [])
  (o/run (cyrik.omni-trace.testing-ns/run-machine))
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
  (prof/serve-files 8888)
  (reset! i/ns-blacklist [])
  (o/untrace)
  (crit/with-progress-reporting (crit/quick-bench (cyrik.omni-trace.testing-ns/run-machine) :verbose))
  (crit/with-progress-reporting (crit/quick-bench (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine) :verbose))
  (o/reset-workspace!)
  (o/instrument-ns 'cyrik.omni-trace.testing-ns)
  (crit/with-progress-reporting (crit/quick-bench (cyrik.omni-trace.testing-ns/run-machine) :verbose))

  (prof/profile {:width 2400}
                (dotimes [x 100] (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)))
  (reset! i/ns-blacklist ['cljs.core 'clojure.core])
  (prof/profile {:width 2400}
                (dotimes [x 100] (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)))
  (o/instrument-ns 'cyrik.omni-trace.testing-ns)
  (prof/profile {:width 2400}
                (dotimes [x 10000] (cyrik.omni-trace.testing-ns/run-machine)))

  (mm/measure  (->> @i/workspace
                    :log
                    (map #(update-in (second %) [:meta :ns] (constantly nil)))
                    (into [])))
  (mm/measure {:end 1638931833800
               :id :39033
               :inner []
               :parent :39032
               :return true
               :start 1638931833800})
  (println (.toFootprint (GraphLayout/parseInstance  (->> @i/workspace
                                                          :log
                                                          (map #(update-in (second %) [:meta :ns] (constantly nil)))
                                                          into-array))))

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

  (o/run (fact 3)))
