(ns cyrik.omni-trace.instrument.clj
  (:require [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rts]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [cyrik.omni-trace.util :as u]
            [borkdude.dynaload :as dynaload]
            clojure.repl))

(def dbgn-f (dynaload/dynaload 'debux.core/dbgn-f {:default nil}))

(defmacro dbgn [form & args]
  (if @dbgn-f
    (dbgn-f form)
    nil))

;;;;;;; taken from sayid, needs to be cleaned up
(defn mk-dummy-whitespace
  [lines cols]
  (apply str
         (concat (repeat lines "\n")
                 (repeat cols " "))))

(defn mk-positionalble-src-logging-push-back-rdr
  [s file line col]
  (rts/source-logging-push-back-reader (str (mk-dummy-whitespace (dec line) ;;this seem unfortunate
                                                                 (dec col))
                                            s)
                                       (+ (count s) line col 1)
                                       file))

(defn hunt-down-source
  [fn-sym]
  (let [{:keys [source file line column]} (-> fn-sym
                                              resolve
                                              meta)]
    (or source
        (r/read (mk-positionalble-src-logging-push-back-rdr
                 (or
                  (clojure.repl/source-fn fn-sym)
                  (->> file
                       slurp
                       clojure.string/split-lines
                       (drop (dec line))
                       (clojure.string/join "\n"))
                  "nil")
                 file
                 line
                 column)))))

;;;;;;; taken from sayid, needs to be cleaned up

(defn eval-in-ns
  [ns-sym form]
  (binding [*ns* (create-ns ns-sym)]
    (use 'clojure.core)
    (eval form)))

(defn fully-qualified [s]
  (u/var->sym (resolve s)))

(defn vars-in-ns-clj [sym]
  (if (find-ns sym)
    (for [[_ v] (ns-interns sym)
          :when (not (:macro (meta v)))]
      (u/var->sym v))
    (throw (Exception. (str "ns " sym " does not exist.")))))

(defn clj-instrument-fn [f opts instrumenter]
  (when-let [v (u/->var f)]
    (let [var-name (u/var->sym v)
          original @v
          meta* (update (meta v) :file #(if-let [classpath-file (io/resource %)]
                                          (.getPath classpath-file)
                                          %))]
      (try
        (when (:inner-trace opts)
          (if (nil? @dbgn-f)
            (throw (Exception. "added debux to dependencies for :inner-trace"))
            (eval-in-ns (ns-name (:ns (meta v))) `(dbgn ~(hunt-down-source var-name)))))
        (catch ClassNotFoundException e (throw (if (= (.getMessage e) "debux.core")
                                                 (Exception. "added debux to dependencies for :inner-trace")
                                                 e))))
      (when-let [instrumented-fn (instrumenter var-name v meta* (when (:inner-trace opts) original) opts)]
        (alter-var-root v (constantly instrumented-fn))
        var-name))))

(defn instrument-syms [sym-or-syms opts instrumenter]
  (let [syms (if (coll? sym-or-syms) sym-or-syms [sym-or-syms])]
    (mapv (fn [sym] (clj-instrument-fn sym opts instrumenter)) syms)))

(defn clj-instrument-ns [ns-sym opts instrumenter]
  (->> ns-sym
       u/->sym
       vars-in-ns-clj
       (filter symbol?)
       (distinct)
       ((fn [syms] (instrument-syms syms opts instrumenter)))
       (remove nil?)))

(defn instrument [s opts instrumenter]
  (let [xs (if (coll? s) s [s])
        syms (map #(u/->sym %) xs)]
    (mapcat #(if (string/includes? (name %) ".")
               (clj-instrument-ns % opts instrumenter)
               (instrument-syms [%] opts instrumenter))
            syms)))

(comment
  (-> #'cyrik.omni-trace.instrument.clj/fully-qualified-sym str symbol)
  (instance? clojure.lang.Namespace (u/->ns "cyrik.omni-trace.testing-ns"))
  (u/->var "cyrik.omni-trace.testing-ns/insert-coin")

  (type cyrik.omni-trace.testing-ns/insert-coin)
  )