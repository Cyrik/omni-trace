(ns cyrik.omni-trace.debux-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace.instrument :as SUT]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as t]))

(deftest ^:no-debux instrument-fn-debux
  (testing "throws if debux is not installed but inner-trace is used"
    (is (thrown-with-msg? Exception #"added debux to dependencies for :inner-trace"
                          (SUT/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin {::o/workspace SUT/workspace :inner-trace true})))))

(deftest ^:debux instrument-fn-with-debux
  (testing "runs normally with inner-trace"
    (SUT/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin {::o/workspace SUT/workspace :inner-trace true})
    (is (= {:inventory {:a1 {:name :taco, :price 0.85, :qty 10}}, :coins-inserted [:quarter], :coins-returned [], :dispensed nil, :err-msg nil}
           (t/insert-coin t/machine-init :quarter)))))
