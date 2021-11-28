(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.omni-trace.instrument :as i]
            [clojure.java.io :as io]
            [portal.api :as p]
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
(defonce portal (p/open))
(add-tap #'p/submit)
(comment

  (o/instrument-fn 'user/test-inc)
  (o/instrument-fn 'cyrik.omni-trace.testing-ns/run-machine)
  (test-inc 123)
  (mapv test-inc (range 1000))
  (o/instrument-ns 'cyrik.omni-trace.testing-ns
                   {::o/workspace i/workspace})

  (e/run-machine)
  (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)
  (tap> (flame/flamedata @i/workspace 'cyrik.omni-trace.testing-ns/run-machine))
  (tap> (o/rooted-flamegraph 'cyrik.omni-trace.testing-ns/run-machine))
  (tap> (o/flamegraph i/workspace))


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
  .
  )
  