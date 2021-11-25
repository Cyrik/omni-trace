(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [clojure.java.io :as io]
            [portal.api :as po]
            [criterium.core :as crit]
            [clojure.walk :as walk]
            [test-console :as tc]
            [clojure.pprint :as pprint]
            [cyrik.omni-trace.graph :as flame]))

(defn spit-pretty!
  "Writes the pretty-printed edn `data` into the `file`."
  [file data]
  (spit (io/file file)
        (with-out-str
          (pprint/pprint data))))



(defn test-inc [x]
  (inc x))
(defn test-log [x]
  (tc/log x)
  (inc x))
(test-log 5)
(defonce portal (po/open))
(add-tap #'po/submit)
(comment

  (o/instrument-fn 'test-inc)
  (mapv test-inc (range 1000))
  (o/instrument-ns 'cyrik.omni-trace.testing-ns
                   {::o/workspace o/workspace})
  (-> e/machine-init
      (e/insert-coin :quarter)
      (e/insert-coin :dime)
      (e/insert-coin :nickel)
      (e/insert-coin :penny)
      (e/press-button :a1))
  (tap> (o/flamegraph o/workspace))
  (meta #'e/insert-coin)


  o/instrumented-vars

  (tap> ^{:portal.viewer/default :portal.viewer/hiccup}
   [:div "custom thing here" [:portal.viewer/inspector {:complex :data-structure}]])

  (o/uninstrument-ns 'omni-trace.testing-ns)
  (walk/macroexpand-all '(o/instrument-ns 'omni-trace.testing-ns))

  (def values (vals (:log @o/workspace)))

  (crit/with-progress-reporting (crit/quick-bench (reduce (fn [acc {:keys [id] :as user}]
                                                            (into acc {id user})) {} values) :verbose))

  (crit/with-progress-reporting (crit/quick-bench (into {} (map (fn [[id user-list]]
                                                                  {id (first user-list)}) (group-by :id values))) :verbose))


  (crit/with-progress-reporting (crit/quick-bench (into {} (map (juxt :id identity)) values) :verbose))
  (crit/with-progress-reporting (crit/quick-bench (zipmap (map :id values) values) :verbose))


  (count (:log @o/workspace))
  (o/reset-workspace!)
  (intern *ns* '~'symname "<<symdefinition>>")
  .
  )
  