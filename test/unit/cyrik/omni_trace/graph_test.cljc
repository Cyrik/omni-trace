(ns cyrik.omni-trace.graph-test
  (:require [clojure.test :refer [deftest are is testing use-fixtures]]
            [cyrik.omni-trace.graph :as SUT]))

(def test-nodes  {:a {:id :a :name :named :parent :root}
                  :c {:id :c :parent :root}
                  :d {:id :d :parent :c}
                  :e {:id :e :parent :d}
                  :f {:id :f :parent :b}
                  :b {:id :b :parent :a}})

(deftest rooted-dependents
  (testing "basics"
    (is (= #{{:id :a :name :named :parent :root} {:id :f :parent :b} {:id :b :parent :a}}
           (SUT/rooted-dependents {:id :a :name :named :parent :root} test-nodes)))))


#_(deftest flamedata
  (testing "basics"
    (is (= (into #{} (list {:id :a :name :named :parent nil} {:id :f :parent :b} {:id :b :parent :a}))
           (into #{} (SUT/flamedata {:log test-nodes} :named))))))