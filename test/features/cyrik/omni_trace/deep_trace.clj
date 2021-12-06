(ns cyrik.omni-trace.deep-trace
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace :as o]
            [clojure.tools.namespace.repl :as repl]
            [cyrik.omni-trace.instrument :as i]
            [cyrik.omni-trace.testing-ns :as t]))
(def res {:coins-inserted []
          :coins-returned [:quarter :quarter :nickel]
          :dispensed {:name :taco, :price 0.85, :qty 10}
          :err-msg nil
          :inventory {:a1 {:name :taco, :price 0.85, :qty 9}}})

(deftest run-traced
  (let [result (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)]
    (testing "returns results"
      (is (= res
             result)))
    (testing "tracks traces"
      (is (< 10
             (count (:log @i/workspace)))))
    (testing "removes instrumentation"
      (is (= 0
             (count @i/instrumented-vars))))))