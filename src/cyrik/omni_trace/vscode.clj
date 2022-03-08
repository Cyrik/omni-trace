(ns cyrik.omni-trace.vscode
  (:require [cyrik.omni-trace.tree :refer [last-call]]
            [cyrik.omni-trace.instrument :as i]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [cyrik.omni-trace.deep-trace :as deep]
            [cyrik.omni-trace.util :as util]))

(defonce workspace (atom i/empty-workspace))

(defn- ->md [v]
  (-> v
      pprint
      with-out-str
      (string/replace "\n"
                      "\n\n")))

(defn hover [n v]
  (if-let [call (-> (str n "/" v)
                    symbol
                    (last-call (:log @workspace)))]
    (str "**Input:**\n\n" (-> call
                           :args
                           ->md)
         "\n\n**Output:**\n\n" (-> call :return ->md))
    ""))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro run [form]
  `(deep/run-traced {:cyrik.omni-trace/workspace workspace}
                    (~util/->sym ~(first form))
                    ~@(rest form)))

(comment
  (hover "cyrik.omni-trace.testing-ns" "insert-coin"))