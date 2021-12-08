(ns cyrik.omni-trace.graph-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace.graph :as SUT]))

(def test-nodes  {:a {:name :a :parent :root}
                  :c {:name :c :parent :root}
                  :d {:name :d :parent :c}
                  :e {:name :e :parent :d}
                  :f {:name :f :parent :b}
                  :b {:name :b :parent :a}})

(deftest rooted-dependents
  (testing "basics"
    (is (= #{{:name :a :parent :root} {:name :f :parent :b} {:name :b :parent :a}}
           (SUT/rooted-dependents {:name :a :parent :root} test-nodes)))))


(deftest flamedata
  (testing "basics"
    (is (= (list {:name :a :parent nil} {:name :f :parent :b} {:name :b :parent :a})
           (SUT/flamedata {:log test-nodes} :a)))))