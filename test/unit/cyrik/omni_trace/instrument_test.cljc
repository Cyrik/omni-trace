(ns cyrik.omni-trace.instrument-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace.instrument :as SUT]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as t]))

(defn ns-reset [test-function]
  (o/reset-workspace!)
  (test-function)
  (o/reset-workspace!)
  (SUT/uninstrument))

(use-fixtures :each ns-reset)

(deftest instrument-fn
  (testing "basic usage"
    (SUT/instrument-fn 'cyrik.omni-trace.testing-ns/insert-coin)
    (let [result (t/insert-coin t/machine-init :quarter)]
      (testing "runs normally"
        (is (= {:inventory {:a1 {:name :taco, :price 0.85, :qty 10}}, :coins-inserted [:quarter], :coins-returned [], :dispensed nil, :err-msg nil}
               result)))
      (testing "captures call"
        (is (= 2
               (count (:log @SUT/workspace)))))
      (testing "captures callsite"
        (is (= 1
               (count (:call-sites @SUT/workspace))))
        (is (= 1
               (second (first (:call-sites @SUT/workspace))))))))
  )


(deftest instrument-ns
  (SUT/instrument-ns "cyrik.omni-trace.testing-ns")
  (let [result (t/insert-coin t/machine-init :quarter)]
    (testing "runs normally"
      (is (= {:inventory {:a1 {:name :taco, :price 0.85, :qty 10}}, :coins-inserted [:quarter], :coins-returned [], :dispensed nil, :err-msg nil}
             result)))
    (testing "captures call"
      (is (= 2
             (count (:log @SUT/workspace)))))
    (testing "captures callsite"
      (is (= 1
             (count (:call-sites @SUT/workspace))))
      (is (= 1
             (second (first (:call-sites @SUT/workspace))))))))

(deftest instrument
  (testing "instruments namespace symbols"
    (SUT/instrument 'cyrik.omni-trace.testing-ns)
    (is (< 10
           (count @SUT/instrumented-vars)))
    (SUT/uninstrument))
  (testing "instruments namespace strings"
    (SUT/instrument "cyrik.omni-trace.testing-ns")
    (is (< 10
           (count @SUT/instrumented-vars)))
    (SUT/uninstrument))
  (testing "instruments namespace symbol vectors"
    (SUT/instrument ['cyrik.omni-trace.testing-ns])
    (is (< 10
           (count @SUT/instrumented-vars)))
    (SUT/uninstrument))
  (testing "instruments function symbols"
    (SUT/instrument `t/insert-coin)
    (is (contains? @SUT/instrumented-vars
                   #'cyrik.omni-trace.testing-ns/insert-coin))
    (SUT/uninstrument))
  (testing "instruments function strings"
    (SUT/instrument "cyrik.omni-trace.testing-ns/insert-coin")
    (is (contains? @SUT/instrumented-vars
                   #'cyrik.omni-trace.testing-ns/insert-coin))
    (SUT/uninstrument))
  (testing "instruments function vars"
    (SUT/instrument 'cyrik.omni-trace.testing-ns/insert-coin)
    (is (contains? @SUT/instrumented-vars
                   #'cyrik.omni-trace.testing-ns/insert-coin))
    (SUT/uninstrument))
  (testing "instruments mix of functions and namespaces"
    (SUT/instrument ['cyrik.omni-trace.testing-ns/insert-coin "cyrik.omni-trace.testing-ns"])
    (is (contains? @SUT/instrumented-vars
                   #'cyrik.omni-trace.testing-ns/insert-coin))
    (is (< 10
           (count @SUT/instrumented-vars)))
    (SUT/uninstrument)))
