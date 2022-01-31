(ns cyrik.omni-trace.deep-trace
  (:require [clj-kondo.core :as clj-kondo]
            [clojure.string :as string]
            [cyrik.omni-trace.instrument :as i]
            [clojure.java.shell :as shell]
            [com.stuartsierra.dependency :as dep]))

(defn classpaths []
  (let [sep (re-pattern (System/getProperty "path.separator"))
        {:keys [exit out err]} (shell/sh "clojure" "-A:dev:test" "-Spath" :dir (System/getProperty "user.dir"))]
    (if (= 0 exit)
      (let [paths (-> out
                      string/split-lines
                      last
                      string/trim-newline
                      (string/split sep))]
        paths)
      (throw (Exception. (format "classpaths error in %s. exit: %s. error: %s" (System/getProperty "user.dir") exit err))))))

(defn analysis [files]
  (:analysis (clj-kondo/run! {:lint files #_(string/split (System/getProperty "java.class.path") #":")
                              :cache true
                              :parallel true
                              :config {:output {:analysis true}}})))

(defn dependencies [analysis-data language]
  (reduce (fn [graph {:keys [:from :to :filename :row :col :from-var :name]}]
            (try (dep/depend graph [from from-var] [to name])
                 (catch Exception e
                   (let [ed (ex-data e)]
                     (if (= :com.stuartsierra.dependency/circular-dependency
                            (:reason ed))
                       (do #_(println (str filename ":" row ":" col ":")
                                      "circular single dependency from var "
                                      [from from-var] "to" [to name])
                           graph)
                       (throw e))))))
          (dep/graph)
          (filter #(or (= language (:lang %))
                       (nil? (:lang %))) (:var-usages analysis-data))))

(defn transitive-deps [deps n sym]
  (conj (dep/transitive-dependencies deps [n sym]) [n sym]))

(defn apply-instrumenter [deep-deps instrumenter]
  (doseq [node deep-deps]
    (instrumenter (symbol (name (first node)) (name (second node))))))

(defn deep-trace* [n sym instrumenter]
  (let [deps (dependencies (analysis ["dev" "src"]) :clj)
        deep-deps (transitive-deps deps n sym)]
    (apply-instrumenter deep-deps instrumenter)
    [deps deep-deps]))

(defn deep-trace [n sym]
  (deep-trace* n sym #(i/instrument-fn %)))
(defn deep-untrace [n sym]
  (deep-trace* n sym #(i/uninstrument-fn %)))

(defn run-traced [s & args]
  (let [v (resolve s)
        n (symbol (namespace s))
        s (symbol (name s))
        [_deps-dag deep-deps] (deep-trace n s)
        ;; _ (println v ns s deep-deps)
        result (try (apply v args)
                    (catch Throwable t
                      (Throwable->map t)))
        _ (apply-instrumenter deep-deps #(i/uninstrument-fn %))]
    result))

(comment
  (require '[portal.api :as p])
  (def portal (p/open))
  (add-tap #'p/submit)
  (deep-trace* 'cyrik.omni-trace.testing-ns 'run-machine #())
  (time (def analysis (clj-kondo/run! {:lint (string/split (System/getProperty "java.class.path") #":") #_files
                                       :cache true
                                       :parallel true
                                       :config {:output {:analysis true}}})))

  (filter #(= "dev/advent/day1.clj" (:filename %)) (:var-usages (analysis ["dev"] #_(classpaths))))

  ''[1 2 3]
  (keys analysis)
  (:summary analysis)
  (let [n 'omni-trace.testing-ns
        sym 'run-machine
        deps (dependencies (analysis ["dev" "src"] #_(classpaths)) :clj)
        deep-deps (transitive-deps deps n sym)]
    (tap> deep-deps))
  )