(ns cyrik.omni-trace.instrument-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace.instrument :as SUT]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as t]))

(defn ns-reset [test-function]
  (o/reset-workspace!)
  (test-function)
  (o/reset-workspace!)
  (o/uninstrument-ns 'cyrik.omni-trace.testing-ns))

(use-fixtures :each ns-reset)

(deftest instrument-fn
   (SUT/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin)
  (let [result (t/insert-coin t/machine-init :quarter)]
    (testing "runs normally"
      (is (= {:inventory {:a1 {:name :taco, :price 0.85, :qty 10}}, :coins-inserted [:quarter], :coins-returned [], :dispensed nil, :err-msg nil}
             result)))
    (testing "captures call"
      (is (= 1
             (count (:log @SUT/workspace)))))
    (testing "captures callsite"
      (is (= 1
             (count (:call-sites @SUT/workspace))))
      (is (= 1
             (second (first (:call-sites @SUT/workspace))))))))
