(ns demo
  #_{:clj-kondo/ignore [:unused-namespace]}
  (:require [clojure.test :refer [is]]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.instrument :as i]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.omni-trace.tree :as tree]
            [cyrik.omni-trace.vscode :as vscode]
            [debux.common.util :as ut]
            [portal.api :as p]))
(def expected
  {:inventory {:a1 {:name :taco
                    :price 0.85
                    :qty 10}}
   :coins-inserted [:quarter :dime :nickel :penny]
   :coins-returned []
   :dispensed nil
   :err-msg true})
(o/reset-workspace!)
(o/untrace)
(p/clear)

(is (= expected
       (e/run-machine)))

;; basic tracing, ala clojure.tools.trace
(o/instrument-fn 'e/press-button)
(o/last-call 'cyrik.omni-trace.testing-ns/press-button)
(o/reset-workspace!)
(o/untrace)

;; more tracing
(o/instrument-ns 'cyrik.omni-trace.testing-ns)
(e/run-machine)
(o/last-call 'cyrik.omni-trace.testing-ns/press-button)
(:63297 (:log @i/workspace))


;; what omni-trace tries to do better
(tap> (o/flamegraph))

(o/reset-workspace!)
(o/untrace)

;; standing on the shoulders of giants (clj-kondo, orchard)
(o/run (e/run-machine))
(tap> (o/flamegraph))

;; exploding

(defn exploding []
  (e/retrieve-change-returned 
   (e/press-button 
    (e/insert-coin 
     (e/insert-coin 
      (e/insert-coin 
       (e/insert-coin e/machine-init :quarter) 
       :dime)
      :nickel)
     :penny)
    :a1)))

(o/reset-workspace!)
(o/run (exploding))
(tap> (o/flamegraph))

;; max callsites
(defn test-inc [x]
  (inc x))
(defn soo-many []
  (mapv test-inc (range 1000)))

(o/reset-workspace!)
(o/run (soo-many))
(count (:log @i/workspace))
(:maxed-callsites @i/workspace)
(o/reset-workspace!)

;; vscode enhanced version (hotkeys and tooltips)
;; inline eval all the way down
(e/run-machine-with e/machine-init)

;;;;;; experimental
;; lets trace clojure.core
(reset! i/ns-blacklist [])
(e/run-machine)
(reset! i/ns-blacklist ['cljs.core 'clojure.core])

;; similar but different
;; inner trace
(defn factorial [acc n]
  (if (zero? n)
    acc
    (factorial (* acc n) (dec n))))

(ut/set-use-result-atom! true)
(o/instrument-fn 'demo/factorial {:cyrik.omni-trace/workspace i/workspace :inner-trace true})
(factorial 1 5)
(:log @i/workspace)
(o/flamegraph)
(o/reset-workspace!)
(o/uninstrument-fn 'demo/factorial)
(reset! i/instrumented-vars {})
(ut/set-use-result-atom! false)

;; normal flamegraph
(factorial 1 5)

;;;;; in the works
;; let the data talk back
(p/eval-str
 (str '(ns mine
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

;; the story with cljs
