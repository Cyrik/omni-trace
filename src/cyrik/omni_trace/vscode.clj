(ns cyrik.omni-trace.vscode
  (:require [clojure.pprint :refer [pprint]]
            [clojure.string :as string]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.deep-trace :as deep]
            [cyrik.omni-trace.instrument :as i]
            [cyrik.omni-trace.tree :refer [last-call]]
            [cyrik.omni-trace.util :as util]))

(defonce workspace (atom i/empty-workspace))

(defn- ->md [v]
  (str "```clojure\n"
       (-> v
           pprint
           with-out-str
           (string/replace "\n"
                           "\n\n"))
       "\n```"))

(defn hover [n v]
  (if-let [call (-> (str n "/" v)
                    symbol
                    (last-call (:log @workspace)))]
    (str "**Input:**\n\n" (-> call :args ->md)
         "\n\n**Output:**\n\n" (-> call :return ->md)
        ;;  "\n\n**print:**\n\n" (str "[some](command:calva.diagnostics.printTextToRichCommentCommand?\"" (:return call) "\")")
         (when-let [parent (->> call :parent (get (:log @workspace)) :name)]
           (str "\n\n**Called by:**\n\n" (-> parent ->md)))
         (when (:thrown call) (str "\n\n**error:**\n\n" (-> call :thrown ->md))))
    ""))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn reset-workspace! []
  (o/reset-workspace! workspace))

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defmacro run [form]
  `(let [result# (deep/run-traced {:cyrik.omni-trace/workspace workspace}
                                  (~util/->sym ~(first form))
                                  ~@(rest form))]
     (tap> (cyrik.omni-trace/flamegraph cyrik.omni-trace.vscode/workspace))
     result#))

(comment
  (hover "cyrik.omni-trace.testing-ns" "insert-coin"))