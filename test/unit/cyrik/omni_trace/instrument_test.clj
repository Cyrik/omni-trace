(ns cyrik.omni-trace.instrument-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace.instrument :as SUT]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as t]))

(defn ns-reset [test-function]
  (test-function))

;; (use-fixtures :each ns-reset)

(deftest instrument-fn
  (testing "runs normally"
    (SUT/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin)
    (is (= {:inventory {:a1 {:name :taco, :price 0.85, :qty 10}}, :coins-inserted [:quarter], :coins-returned [], :dispensed nil, :err-msg nil}
           (t/insert-coin t/machine-init :quarter))))
  (testing "captures call"
    (is (= 1
         (count (:log @SUT/workspace))))))
