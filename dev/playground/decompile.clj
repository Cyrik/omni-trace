(ns playground.decompile
  (:require [clj-java-decompiler.core :as decomp]
            [clojure.repl :as repl]
            [cyrik.omni-trace.testing-ns :as testing-ns]
            [cyrik.omni-trace.instrument.clj :as instrument]
            [clojure.string :as str])
  (:import (com.strobel.decompiler DecompilationOptions DecompilerSettings
                                   PlainTextOutput)
           (com.strobel.assembler.metadata DeobfuscationUtilities MetadataSystem
                                           IMetadataResolver MetadataParser
                                           TypeReference)
           (com.strobel.assembler.metadata ArrayTypeLoader)
           com.strobel.decompiler.languages.Languages))

(decomp/decompile (fn [] (map #(println %) (range 10))))

(defn dummy-inc [x]
  (inc x))
(defn dummy-fn [x]
  (map #(dummy-inc %) (filter even? (range 1 10))))

(defn resolve-class-from-bytes
  "Read and process the given classfile with Procyon."
  [file]
  (let [loader (ArrayTypeLoader. file)]
    (println (.getClassNameFromArray loader))
    (doto (.resolve (.lookupType (MetadataSystem. loader) (.getClassNameFromArray loader)))
      (DeobfuscationUtilities/processType))))
(def ^:private java-decompiler (Languages/java))
(def ^:private test-decompiler (Languages/bytecodeAst))
(def ^:private bytecode-decompiler (Languages/bytecode))

(defn decompile-bytes
  "Decompile the given classfile and print the result to stdout."
  [name bytes options]
  (let [type (resolve-class-from-bytes bytes)
        decompiler (if (= (:decompiler options) :bytecode)
                     test-decompiler
                     java-decompiler)
        decomp-options (doto (DecompilationOptions.)
                         (.setSettings (doto (DecompilerSettings.)
                                         (.setSimplifyMemberReferences true)
                                         (.setTypeLoader (ArrayTypeLoader. bytes)))))
        output (PlainTextOutput.)]
    ;; (println "\n// Decompiling class:" (.getInternalName type))
    (.decompileType decompiler type output decomp-options)
    (println (str output))))
(def classbytes (atom {}))

;; https://gist.github.com/hiredman/6214648
(defn- recompile [ns-sym form]
  (push-thread-bindings
   {clojure.lang.Compiler/LOADER
    (proxy [clojure.lang.DynamicClassLoader] [@clojure.lang.Compiler/LOADER]
      (defineClass
        ([name bytes src]
         (swap! classbytes assoc name bytes)
         (proxy-super defineClass name bytes src))))})
  (try
    (let [line @clojure.lang.Compiler/LINE
          column @clojure.lang.Compiler/COLUMN
          line (if-let [line (:line (meta form))]
                 line
                 line)
          column (if-let [column (:column (meta form))]
                   column
                   column)]
      (push-thread-bindings {clojure.lang.Compiler/LINE line
                             clojure.lang.Compiler/COLUMN column})
      (try
        (let [form (macroexpand form)]
          (when (and (coll? form) (= 'clojure.core/fn (first (nth form 2 nil))))
            (binding [*ns* (create-ns ns-sym)]
              (clojure.lang.Compiler/analyze
               clojure.lang.Compiler$C/EVAL
               (nth form 2)))))
        (finally
          (pop-thread-bindings))))
    (finally
      (pop-thread-bindings))))

(defn- as-val
  "Convert `thing` to a function value."
  [thing]
  (cond
    (string? thing) (var-get (find-var (symbol thing)))
    (var? thing) (var-get thing)
    (symbol? thing) (var-get (find-var thing))
    (fn? thing) thing))

(defn fn-name [f]
  (-> f .getName repl/demunge symbol))

(defn fn-deps-class
  [v]
  (let [v (if (class? v) 
            v 
            (eval v))]
    (set (some->>  v .getDeclaredFields
                  (keep (fn [^java.lang.reflect.Field f]
                          (or (and (identical? clojure.lang.Var (.getType f))
                                   (java.lang.reflect.Modifier/isPublic (.getModifiers f))
                                   (java.lang.reflect.Modifier/isStatic (.getModifiers f))
                                   (-> f .getName (.startsWith "const__"))
                                   (.get f (fn-name v)))
                              nil)))))))

(defn f->sym [f]
  (-> f .getClass .getName repl/demunge symbol))

(defn fn-source [f]
  (instrument/hunt-down-source (f->sym f)))

(defn- hunt-down-source
  [fn-sym]
  (let [{:keys [source file line]} (-> fn-sym
                                       resolve
                                       meta)]
    (try (or source
             (and file (read-string {:read-cond :allow}
                                    (or
                                     (clojure.repl/source-fn fn-sym)
                                     (->> file
                                          slurp
                                          clojure.string/split-lines
                                          (drop (dec line))
                                          (clojure.string/join "\n"))
                                     "nil"))))
         (catch Exception _))))

(defn- map-v [f m]
  (reduce-kv (fn [m k v] (assoc m k (f v))) {} m))
(defn fn-deps [f]
  (reset! classbytes {})
  (let [sym (f->sym f)
        source (fn-source f)
        b (do (recompile (-> sym namespace symbol) source) @classbytes)
        class-names (map first b)
        deps (set (mapcat #(-> % symbol fn-deps-class) class-names))]
    deps))

(defn fn-deps-compiling
  "Returns a set with all the functions invoked by `val`.
  `val` can be a function value, a var or a symbol."
  {:added "0.5"}
  [f]
  (reset! classbytes {})
  (let [f (as-val f)]
    (when-let [source (and (fn? f) (fn-source f))]
      (let [sym (f->sym f)
            b (do (recompile (-> sym namespace symbol) source) @classbytes)
            class-names (map first b)
            deps (set (mapcat #(-> % symbol fn-deps-class) class-names))]
        deps))))

(comment
  (fn-deps testing-ns/calc-change-to-return*)
  (namespace 'testing-ns/calc-change-to-return*)
  (recompile 'playground.decompile '(map #(println %) (range 10)))
  (recompile 'playground.decompile '(defn dummy [a] (map #(println a) (range 10))))
  (decompile-bytes (key (first @classbytes)) (val (first @classbytes)) {})
  (map #(decompile-bytes (key %) (val %) {:decompiler :bytecode}) @classbytes)
  (let [class-bytestr (-> @classbytes
                          first
                          val
                          slurp
                          (subs 13))
        klass (subs class-bytestr 0 (str/index-of class-bytestr ""))]
    klass)
  (as-val (Compiler/demunge "playground.decompile$fn__79200$fn__79201"))

  (def field (->> clojure.lang.DynamicClassLoader .getDeclaredFields second))
  (.getName field)
  (.setAccessible field true)
  (type (second (first (fn-deps-compiling cyrik.omni-trace.testing-ns/calc-change-to-return*))))
  (def classes (into {} (.get field clojure.lang.DynamicClassLoader)))
  (->> (keys classes)
       sort
       (filter #(str/includes? % "cyrik.omni_trace.testing"))
       #_(drop 22000)
       ;;(take 2000)
       )
  (require '[clj-kondo.core :as clj-kondo])
  (-> (clj-kondo/run! {:lint ["src"]
                       :config {:output {:analysis true}}})
      :analysis
      :var-usages)
  (System/gc)
  (.getDeclaredFields playground.decompile$fn__79200)
  (fn-deps-class playground.decompile$fn__79200$fn__79201)
  (fn-deps-class (eval (symbol "cyrik.omni_trace.instrument$eval18265")))
  (-> cyrik.omni-trace.testing-ns/calc-change-to-return* .getClass .getName)
  (eval (symbol "cyrik.omni_trace.instrument$eval18265"))
  (.getURLs (ClassLoader/getSystemClassLoader))
  (str/split (System/getProperty "java.class.path") #":")
  )