(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.omni-trace.instrument :as i]
            [clojure.java.io :as io]
            [portal.api :as p]
            [criterium.core :as crit]
            [debux.core :as d]
            [clojure.walk :as walk]
            [test-console :as tc]
            [zprint.core :as zp]
            [debux.common.util :as ut]
            [clojure.pprint :as pprint]
            [cyrik.omni-trace.graph :as flame]))

(defn spit-pretty!
  "Writes the pretty-printed edn `data` into the `file`."
  [file data]
  (spit (io/file file)
        (with-out-str
          (pprint/pprint data))))

(declare factorial)
(defn factorial [acc n]
  (if (zero? n)
    acc
    (factorial (* acc n) (dec n))))

(defn foo [a b & [c]]
  (if c
    (* a b c)
    (* a b 100)))

(defn fact [num]
  (loop [acc 1 n num]
    (if (zero? n)
      acc
      (recur (* acc n) (dec n)))))

(defn test-inc [x]
  (inc x))
(defn test-log [x]
  (tc/log x)
  (inc x))
(test-log 5)
(defonce portal (p/open))
(add-tap #'p/submit)
(comment

  (o/instrument-fn 'user/test-inc {::o/workspace i/workspace :inner-trace true})
  (o/instrument-fn 'cyrik.omni-trace.testing-ns/run-machine)
  (test-inc 123)

  (mapv test-inc (range 1000))
  (o/instrument-ns 'cyrik.omni-trace.testing-ns
                   {::o/workspace i/workspace})

  (e/run-machine)
  (zp/zprint (:log @i/workspace))
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

  ut/config*

  (o/instrument-fn 'user/foo {::o/workspace i/workspace :inner-trace true})
  (o/instrument-fn 'user/factorial {::o/workspace i/workspace :inner-trace true})
  (o/instrument-fn 'user/fact {::o/workspace i/workspace :inner-trace true})

  (foo 2 3)
  (factorial 1 3)
  (fact 3)
  (o/uninstrument-fn 'user/test-inc)
  (o/uninstrument-fn 'user/factorial)
  (o/uninstrument-fn 'user/fact)
  (reset! ut/result* [])
  (o/reset-workspace!)
  (reset! i/instrumented-vars {})
  
  (walk/macroexpand-all '(d/dbgn (defn factorial [acc n]
                                   (if (zero? n)
                                     acc
                                     (factorial (* acc n) (dec n))))))

  (macroexpand '(debux.dbgn/dbgn (defn fact [num]
                                   (loop [acc 1 n num]
                                     (if (zero? n)
                                       acc
                                       (recur (* acc n) (dec n)))))
                                 (clojure.core/zipmap 'nil [])
                                 {:evals (atom {}), :line 106, :ns "user"}))
  
  (d/dbgn (defn fact [num]
            (loop [acc 1 n num]
              (if (zero? n)
                acc
                (recur (* acc n) (dec n))))))
  
  (def
    factorial
    (fn*
     ([acc n]
      (let*
       []
       (clojure.core/push-thread-bindings
        (clojure.core/hash-map
         #'debux.common.util/*indent-level*
         (clojure.core/inc debux.common.util/*indent-level*)))
       (try
         (clojure.core/reset! (:evals +debux-dbg-opts+) {})
         (debux.common.util/insert-blank-line)
         (let*
          [result__73764__auto__
           (let*
            [opts__70960__auto__
             +debux-dbg-opts+
             n__70961__auto__
             (let*
              [or__5533__auto__ (:n opts__70960__auto__)]
              (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
             form__70962__auto__
             '(if (zero? n) acc (factorial (* acc n) (dec n)))
             result__70963__auto__
             (if
              (let*
               [opts__70960__auto__
                +debux-dbg-opts+
                n__70961__auto__
                (let*
                 [or__5533__auto__ (:n opts__70960__auto__)]
                 (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                form__70962__auto__
                '(zero? n)
                result__70963__auto__
                (zero?
                 (let*
                  [opts__70960__auto__
                   +debux-dbg-opts+
                   n__70961__auto__
                   (let*
                    [or__5533__auto__ (:n opts__70960__auto__)]
                    (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                   form__70962__auto__
                   'n
                   result__70963__auto__
                   n]
                  (if
                   (let*
                    [or__5533__auto__ (:dup opts__70960__auto__)]
                    (if
                     or__5533__auto__
                      or__5533__auto__
                      (debux.common.util/eval-changed?
                       (:evals opts__70960__auto__)
                       form__70962__auto__
                       result__70963__auto__)))
                    (do
                      (debux.common.util/trace!
                       form__70962__auto__
                       (clojure.core/meta form__70962__auto__)
                       result__70963__auto__)
                      (debux.common.util/print-form-with-indent
                       (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                      (let*
                       []
                       (clojure.core/push-thread-bindings
                        (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                       (try
                         (debux.common.util/pprint-result-with-indent result__70963__auto__)
                         (finally (clojure.core/pop-thread-bindings))))))
                  result__70963__auto__))]
               (if
                (let*
                 [or__5533__auto__ (:dup opts__70960__auto__)]
                 (if
                  or__5533__auto__
                   or__5533__auto__
                   (debux.common.util/eval-changed?
                    (:evals opts__70960__auto__)
                    form__70962__auto__
                    result__70963__auto__)))
                 (do
                   (debux.common.util/trace!
                    form__70962__auto__
                    (clojure.core/meta form__70962__auto__)
                    result__70963__auto__)
                   (debux.common.util/print-form-with-indent
                    (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                   (let*
                    []
                    (clojure.core/push-thread-bindings
                     (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                    (try
                      (debux.common.util/pprint-result-with-indent result__70963__auto__)
                      (finally (clojure.core/pop-thread-bindings))))))
               result__70963__auto__)
               (let*
                [opts__70960__auto__
                 +debux-dbg-opts+
                 n__70961__auto__
                 (let*
                  [or__5533__auto__ (:n opts__70960__auto__)]
                  (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                 form__70962__auto__
                 'acc
                 result__70963__auto__
                 acc]
                (if
                 (let*
                  [or__5533__auto__ (:dup opts__70960__auto__)]
                  (if
                   or__5533__auto__
                    or__5533__auto__
                    (debux.common.util/eval-changed?
                     (:evals opts__70960__auto__)
                     form__70962__auto__
                     result__70963__auto__)))
                  (do
                    (debux.common.util/trace!
                     form__70962__auto__
                     (clojure.core/meta form__70962__auto__)
                     result__70963__auto__)
                    (debux.common.util/print-form-with-indent
                     (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                    (let*
                     []
                     (clojure.core/push-thread-bindings
                      (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                     (try
                       (debux.common.util/pprint-result-with-indent result__70963__auto__)
                       (finally (clojure.core/pop-thread-bindings))))))
                result__70963__auto__)
               (let*
                [opts__70960__auto__
                 +debux-dbg-opts+
                 n__70961__auto__
                 (let*
                  [or__5533__auto__ (:n opts__70960__auto__)]
                  (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                 form__70962__auto__
                 '(factorial (* acc n) (dec n))
                 result__70963__auto__
                 (factorial
                  (let*
                   [opts__70960__auto__
                    +debux-dbg-opts+
                    n__70961__auto__
                    (let*
                     [or__5533__auto__ (:n opts__70960__auto__)]
                     (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                    form__70962__auto__
                    '(* acc n)
                    result__70963__auto__
                    (*
                     (let*
                      [opts__70960__auto__
                       +debux-dbg-opts+
                       n__70961__auto__
                       (let*
                        [or__5533__auto__ (:n opts__70960__auto__)]
                        (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                       form__70962__auto__
                       'acc
                       result__70963__auto__
                       acc]
                      (if
                       (let*
                        [or__5533__auto__ (:dup opts__70960__auto__)]
                        (if
                         or__5533__auto__
                          or__5533__auto__
                          (debux.common.util/eval-changed?
                           (:evals opts__70960__auto__)
                           form__70962__auto__
                           result__70963__auto__)))
                        (do
                          (debux.common.util/trace!
                           form__70962__auto__
                           (clojure.core/meta form__70962__auto__)
                           result__70963__auto__)
                          (debux.common.util/print-form-with-indent
                           (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                          (let*
                           []
                           (clojure.core/push-thread-bindings
                            (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                           (try
                             (debux.common.util/pprint-result-with-indent result__70963__auto__)
                             (finally (clojure.core/pop-thread-bindings))))))
                      result__70963__auto__)
                     (let*
                      [opts__70960__auto__
                       +debux-dbg-opts+
                       n__70961__auto__
                       (let*
                        [or__5533__auto__ (:n opts__70960__auto__)]
                        (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                       form__70962__auto__
                       'n
                       result__70963__auto__
                       n]
                      (if
                       (let*
                        [or__5533__auto__ (:dup opts__70960__auto__)]
                        (if
                         or__5533__auto__
                          or__5533__auto__
                          (debux.common.util/eval-changed?
                           (:evals opts__70960__auto__)
                           form__70962__auto__
                           result__70963__auto__)))
                        (do
                          (debux.common.util/trace!
                           form__70962__auto__
                           (clojure.core/meta form__70962__auto__)
                           result__70963__auto__)
                          (debux.common.util/print-form-with-indent
                           (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                          (let*
                           []
                           (clojure.core/push-thread-bindings
                            (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                           (try
                             (debux.common.util/pprint-result-with-indent result__70963__auto__)
                             (finally (clojure.core/pop-thread-bindings))))))
                      result__70963__auto__))]
                   (if
                    (let*
                     [or__5533__auto__ (:dup opts__70960__auto__)]
                     (if
                      or__5533__auto__
                       or__5533__auto__
                       (debux.common.util/eval-changed?
                        (:evals opts__70960__auto__)
                        form__70962__auto__
                        result__70963__auto__)))
                     (do
                       (debux.common.util/trace!
                        form__70962__auto__
                        (clojure.core/meta form__70962__auto__)
                        result__70963__auto__)
                       (debux.common.util/print-form-with-indent
                        (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                       (let*
                        []
                        (clojure.core/push-thread-bindings
                         (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                        (try
                          (debux.common.util/pprint-result-with-indent result__70963__auto__)
                          (finally (clojure.core/pop-thread-bindings))))))
                   result__70963__auto__)
                  (let*
                   [opts__70960__auto__
                    +debux-dbg-opts+
                    n__70961__auto__
                    (let*
                     [or__5533__auto__ (:n opts__70960__auto__)]
                     (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                    form__70962__auto__
                    '(dec n)
                    result__70963__auto__
                    (dec
                     (let*
                      [opts__70960__auto__
                       +debux-dbg-opts+
                       n__70961__auto__
                       (let*
                        [or__5533__auto__ (:n opts__70960__auto__)]
                        (if or__5533__auto__ or__5533__auto__ (:print-length @debux.common.util/config*)))
                       form__70962__auto__
                       'n
                       result__70963__auto__
                       n]
                      (if
                       (let*
                        [or__5533__auto__ (:dup opts__70960__auto__)]
                        (if
                         or__5533__auto__
                          or__5533__auto__
                          (debux.common.util/eval-changed?
                           (:evals opts__70960__auto__)
                           form__70962__auto__
                           result__70963__auto__)))
                        (do
                          (debux.common.util/trace!
                           form__70962__auto__
                           (clojure.core/meta form__70962__auto__)
                           result__70963__auto__)
                          (debux.common.util/print-form-with-indent
                           (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                          (let*
                           []
                           (clojure.core/push-thread-bindings
                            (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                           (try
                             (debux.common.util/pprint-result-with-indent result__70963__auto__)
                             (finally (clojure.core/pop-thread-bindings))))))
                      result__70963__auto__))]
                   (if
                    (let*
                     [or__5533__auto__ (:dup opts__70960__auto__)]
                     (if
                      or__5533__auto__
                       or__5533__auto__
                       (debux.common.util/eval-changed?
                        (:evals opts__70960__auto__)
                        form__70962__auto__
                        result__70963__auto__)))
                     (do
                       (debux.common.util/trace!
                        form__70962__auto__
                        (clojure.core/meta form__70962__auto__)
                        result__70963__auto__)
                       (debux.common.util/print-form-with-indent
                        (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                       (let*
                        []
                        (clojure.core/push-thread-bindings
                         (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                        (try
                          (debux.common.util/pprint-result-with-indent result__70963__auto__)
                          (finally (clojure.core/pop-thread-bindings))))))
                   result__70963__auto__))]
                (if
                 (let*
                  [or__5533__auto__ (:dup opts__70960__auto__)]
                  (if
                   or__5533__auto__
                    or__5533__auto__
                    (debux.common.util/eval-changed?
                     (:evals opts__70960__auto__)
                     form__70962__auto__
                     result__70963__auto__)))
                  (do
                    (debux.common.util/trace!
                     form__70962__auto__
                     (clojure.core/meta form__70962__auto__)
                     result__70963__auto__)
                    (debux.common.util/print-form-with-indent
                     (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                    (let*
                     []
                     (clojure.core/push-thread-bindings
                      (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                     (try
                       (debux.common.util/pprint-result-with-indent result__70963__auto__)
                       (finally (clojure.core/pop-thread-bindings))))))
                result__70963__auto__))]
            (if
             (let*
              [or__5533__auto__ (:dup opts__70960__auto__)]
              (if
               or__5533__auto__
                or__5533__auto__
                (debux.common.util/eval-changed?
                 (:evals opts__70960__auto__)
                 form__70962__auto__
                 result__70963__auto__)))
              (do
                (debux.common.util/trace!
                 form__70962__auto__
                 (clojure.core/meta form__70962__auto__)
                 result__70963__auto__)
                (debux.common.util/print-form-with-indent
                 (debux.common.util/form-header form__70962__auto__ (:msg opts__70960__auto__)))
                (let*
                 []
                 (clojure.core/push-thread-bindings
                  (clojure.core/hash-map #'clojure.core/*print-length* n__70961__auto__))
                 (try
                   (debux.common.util/pprint-result-with-indent result__70963__auto__)
                   (finally (clojure.core/pop-thread-bindings))))))
            result__70963__auto__)
           _
           (debux.common.util/trace-binding!
            'result__73764__auto__
            result__73764__auto__
            (clojure.core/meta 'result__73764__auto__))]
          (debux.common.util/insert-blank-line)
          ((:user-call @debux.common.util/config*) @debux.common.util/result*)
          result__73764__auto__)
         (finally (clojure.core/pop-thread-bindings)))))))
  .
  )
  