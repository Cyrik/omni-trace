(ns cyrik.omni-trace.instrument.clj
  (:require [clojure.tools.reader :as r]
            [clojure.tools.reader.reader-types :as rts]
            clojure.string
            [clojure.java.io :as io]
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

(defn var->sym [v]
  (let [meta (meta v)]
    (symbol (name (ns-name (:ns meta))) (name (:name meta)))))

(defn vars-in-ns-clj [sym]
  (if (find-ns sym)
    (for [[_ v] (ns-interns sym)
          :when (not (:macro (meta v)))]
      (var->sym v))
    []))

(defn clj-instrument-fn [sym opts instrumenter]
  (when-let [v (resolve sym)]
    (let [var-name (var->sym v)
          original @v
          file *file*
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

(defn clj-instrument-ns [ns-sym opts mapper instrumenter]
  (->> ns-sym
       vars-in-ns-clj
       (filter symbol?)
       (distinct)
       (mapv (fn [sym] (mapper sym opts instrumenter)))
       (remove nil?)))

(comment
  (user/test-inc 1)
  (deref ut/result*)
  (ut/trace!)
  )