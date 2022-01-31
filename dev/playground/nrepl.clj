(ns playground-nrepl
  (:require [nrepl.core :as nrepl]
            [cyrik.omni-trace.testing-ns :as e]
            [orchard.xref :as xref]
            [clojure.reflect :as refl]
            [orchard.query :as query]
            [clojure.repl :as repl]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str])
  (:import (io.github.classgraph ClassGraph ClassInfo)))

(defn fn-source [f]
  (let [fn-name (-> f .getClass .getName)
        fn-name (repl/demunge fn-name)
        fn-sym (symbol fn-name)]
    (println (repl/source-fn fn-sym))))
(load "cyrik/omni_trace/testing_ns")
(fn-source cyrik.omni-trace.testing-ns/calc-coin-value)
(fn-source cyrik.omni-trace.instrument.cljs/->ns)
(fn-source cyrik.omni-trace/flamegraph)

(defn- as-val
  "Convert `thing` to a function value."
  [thing]
  (cond
    (var? thing) (var-get thing)
    (symbol? thing) (var-get (find-var thing))
    (fn? thing) thing))

(defn fn-deps
  "Returns a set with all the functions invoked by `val`.
  `val` can be a function value, a var or a symbol."
  {:added "0.5"}
  [v]
  (let [v (as-val v)]
    (set (some->> v class .getDeclaredFields
                  (keep (fn [^java.lang.reflect.Field f]
                          (or (and (identical? clojure.lang.Var (.getType f))
                                   (java.lang.reflect.Modifier/isPublic (.getModifiers f))
                                   (java.lang.reflect.Modifier/isStatic (.getModifiers f))
                                   (-> f .getName (.startsWith "const__"))
                                   (.get f v))
                              nil)))))))
(defn all-class-infos []
  (let [scan-result (.. (ClassGraph.) enableAllInfo scan)]
    (into {} (.getAllClassesAsMap scan-result))))

(defn try-load-class [class-info]
  (try
    {:err nil :klass (. class-info loadClass)}
    (catch Exception e
      {:err e})))

(defn get-class [class-infos class-name-string]
  (let [class-info (get class-infos class-name-string)
        {:keys [err klass]} (try-load-class class-info)]
    (if err
      nil
      klass)))

(comment
  (def x (all-class-infos))


  (count x)
  (refl/reflect e/calc-change-to-return*)

;; Print some info about a couple of classes, chosen arbitrarily
  (pprint (nth (seq x) 0))
  (pprint (nth (seq x) 100))

  (refl/type-reflect (get-class x "clojure.core$when_first"))
  (refl/type-reflect (get-class x "clojure.lang.PersistentVector"))
  (refl/type-reflect (get-class x "java.lang.Object"))
  (refl/type-reflect (class (as-val e/calc-change-to-return*)))

;; Show all class names as strings, sorted
  (->> (keys x)
       sort
       (filter #(str/includes? % "cyrik"))
       #_(drop 22000)
       ;;(take 2000)
       )
  ((fn [] (throw (Exception. "unnamed"))))
  ((fn myfn [] (throw (Exception. "named"))))
  )


(with-open [conn (nrepl/connect :port 8777)]
  (-> (nrepl/client conn 1000)    ; message receive timeout required
      (nrepl/message {:op "classpath"})
      doall
      tap>))

(with-open [conn (nrepl/connect :port 8777)]
  (-> (nrepl/client conn 1000)    ; message receive timeout required
      (nrepl/message {:op "resources-list"})
      doall
      pprint))

(with-open [conn (nrepl/connect :port 8777)]
  (-> (nrepl/client conn 1000)    ; message receive timeout required
      (nrepl/message {:op "describe" :verbose? "true"})
      doall
      tap>))

(with-open [conn (nrepl/connect :port 8777)]
  (-> (nrepl/client conn 1000)    ; message receive timeout required
      (nrepl/message {:op "fn-deps" 
                      :ns "cyrik.omni-trace.testing-ns" 
                      :sym "run-machine"})
      doall
      pprint))

(with-open [conn (nrepl/connect :port 8777)]
  (-> (nrepl/client conn 1000)    ; message receive timeout required
      (nrepl/message {:op "fn-deps" 
                      :ns "cyrik.omni-trace.testing-ns" 
                      :sym "valid-selection"})
      doall
      pprint))


;; (with-open [conn (nrepl/connect :port 8777)]
;;   (-> (nrepl/client conn 1000)    ; message receive timeout required
;;       (nrepl/message {:op "artifact-list"})
;;       doall
;;       pprint))

;; taken from orchard


(fn-deps e/calc-change-to-return*)
(.get(first (->> (as-val e/calc-change-to-return*) class .getDeclaredFields)) e/calc-change-to-return*)

(defn- fn->sym
  "Convert a function value `f` to symbol."
  [f]
  (symbol (Compiler/demunge (.getName ^Class (type f)))))

(defn- as-var
  "Convert `thing` to a var."
  [thing]
  (cond
    (var? thing) thing
    (symbol? thing) (find-var thing)
    (fn? thing) (find-var (fn->sym thing))))

(defn fn-refs
  "Find all functions that refer `var`.
  `var` can be a function value, a var or a symbol."
  {:added "0.5"}
  [var]
  (let [var (as-var var)
        all-vars (query/vars {:ns-query {:project? true} :private? true})
        deps-map (zipmap all-vars (map fn-deps all-vars))]
    (map first (filter (fn [[_k v]] (contains? v var)) deps-map))))

(find-ns 'cyrik.omni-trace.testing-ns)

