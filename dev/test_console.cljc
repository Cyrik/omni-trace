(ns test-console
  #?(:clj (:require [clojure.java.io :as io]
                    [cljs.analyzer.api :as api]
                    [portal.api :as p]
                    [cljs.env :as env]
                    [shadow.build.data :as data]
                    ;; [clojure.repl :as clj.repl]
                    ;; [cljs.repl :as cljs.repl]
                    [cljs.analyzer :as ana])
     :cljs (:require [cljs.analyzer.api :as api]
                     [portal.web :as p]
                     [cljs.env :as env]
                     [cljs.analyzer :as ana]))
  #?(:cljs (:require-macros [test-console :refer [log]])))

(defn now []
  #?(:clj (java.util.Date.) :cljs (js/Date.)))

(defn run [f]
  (try
    [nil (f)]
    (catch #?(:clj Exception :cljs :default) ex#
      [:throw ex#])))

(defn runtime []
  #?(:bb :bb :clj :clj :cljs :cljs))

#?(:clj
   (defmacro homedir []
     (-> (io/file ".")
         (.getAbsoluteFile)
         (.getAbsolutePath))))
#?(:clj
   (defmacro homedir2 []
     (-> (io/file ana/*cljs-file*)
         (.getAbsoluteFile)
         (.getAbsolutePath))))
#?(:clj
   (defmacro homedir3 
     "works from repl with no src dev"
     []     
     ana/*cljs-file*))

#?(:clj
   (defn homedir4 []
     (-> (io/file ".")
         (.getAbsoluteFile)
         (.getAbsolutePath))))
#?(:clj
   (defn homedir5 []
     (-> (io/file ana/*cljs-file*)
         (.getAbsolutePath))))
#?(:clj
   (defn homedir6 
     "works from repl with no src dev"
     []
     ana/*cljs-file*))

#?(:clj
   (defmacro wtf 
     [ns]
     (ana/locate-src ns)))

#?(:cljs
   (defn wtf2 
     []
     api/current-state))
#?(:cljs
   (defmacro wtf3 
     []
     ana/compiler-options))

#?(:clj
   (defn get-file [env file]
     (if (:ns env) ;; cljs target
       (if (= file "repl-input.cljs")
         (get-in env [:ns :meta :file])
         (if-let [classpath-file (io/resource file)]
           (.getPath (io/file classpath-file))
           file))
       *file*)))

(defn capture [level form expr env]
  (let [{:keys [line column file]} (meta form)]
    `(let [[flow# result#] (run (fn [] ~expr))]
       (tap>
        {:form     (quote ~expr)
         :level    (if (= flow# :throw) :fatal ~level)
         :result   result#
         :ns       (quote ~(symbol (str *ns*)))
         :file     ~#?(:clj (get-file env file) :cljs nil)
        ;;  :file-1   ~(if (:ns env) file #?(:clj *file* :cljs nil))
        ;;  :file1    ~#?(:clj (:shadow/ns-roots @env/*compiler*) :cljs env/*compiler*)
        ;;  :file2    ~#?(:clj (when-let [classpath-file (io/resource file)]
        ;;                       (.toString classpath-file)) :cljs env/*compiler*)
        ;;  :file1    ~#?(:clj (:shadow/ns-roots @env/*compiler*) :cljs env/*compiler*)
        ;;  :file3    ~#?(:clj file :cljs env/*compiler*)
        ;;  :file4    ~#?(:clj (get-in env [:ns :meta :file]) :cljs env/*compiler*)
        ;;  :file3    ~#?(:clj (:options @env/*compiler*) :cljs env/*compiler*)
        ;;  :file1     (wtf ~(:ns env)) ;works if precompiled
        ;;  :file2     (homedir)
        ;;  :file3     (homedir2)
        ;;  :file4     (homedir3)
        ;;  :file5     ~#?(:clj (homedir4) :cljs nil)
        ;;  :file6     ~#?(:clj (homedir5) :cljs nil)
        ;;  :file7     ~#?(:clj (homedir6) :cljs nil)
        ;;  :file8     (wtf)
         :line     ~line
         :column   ~column
         :time     (now)
         :runtime  (runtime)})
       (if (= flow# :throw) (throw result#) result#))))

(defmacro log   [expr] (capture :info &form expr &env))

(defmacro trace [expr] (capture :trace &form expr &env))
(defmacro debug [expr] (capture :debug &form expr &env))
(defmacro info  [expr] (capture :info  &form expr &env))
(defmacro warn  [expr] (capture :warn  &form expr &env))
(defmacro error [expr] (capture :error &form expr &env))
(defmacro fatal [expr] (capture :fatal &form expr &env))


