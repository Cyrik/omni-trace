(ns cyrik.omni-trace.deep-trace-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.instrument :as i]
            [cyrik.omni-trace.graph :as graph]
            [cyrik.omni-trace.testing-ns :as t]))

(defn ns-reset [test-function]
  (i/uninstrument)
  (o/reset-workspace!)
  (test-function)
  (o/reset-workspace!)
  (o/untrace))

(use-fixtures :each ns-reset)

(def res {:coins-inserted []
          :coins-returned [:quarter :quarter :nickel]
          :dispensed {:name :taco, :price 0.85, :qty 10}
          :err-msg nil
          :inventory {:a1 {:name :taco, :price 0.85, :qty 9}}})


(deftest rooted-flamegraph
  (let [_ (o/run (t/run-machine))]
    (testing "sets root"
      (let [root (filter #(not (:parent %)) (graph/flamedata @i/workspace 'cyrik.omni-trace.testing-ns/run-machine))]
        (is (= 1
               (count root)))
        (is (= 'cyrik.omni-trace.testing-ns/run-machine
               (:name (first root))))))))

(deftest run
  (let [result (o/run (t/run-machine-with t/machine-init))]
    (testing "returns results"
      (is (= res
             result)))
    (testing "tracks traces"
      (is (< 10
             (count (:log @i/workspace)))))
    (testing "removes instrumentation"
      (is (= 0
             (count @i/instrumented-vars)) 
          @i/instrumented-vars))))