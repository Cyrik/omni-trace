(ns cyrik.omni-trace.util 
  (:require [clojure.repl :as repl]
            ))

(defn var->sym [v]
  (let [m (meta v)]
    (symbol (name (ns-name (:ns m))) (name (:name m)))))

(defn ->var [something]
  (cond
    (var? something) something
    (symbol? something) (resolve something)
    (string? something) (resolve (symbol something))
    :else nil))

(defn ->sym [something]
  (cond
    (fn? something) (-> something .getClass .getName repl/demunge symbol)
    (symbol? something) something
    (var? something) (var->sym something)
    (string? something) (symbol something)
    :else nil))

(defn ->ns [something]
  (cond
    (instance? clojure.lang.Namespace something) something
    (symbol? something) (find-ns something)
    (string? something) (find-ns (symbol something))
    :else nil))

(defn fully-qualified [s]
  (var->sym (resolve s)))