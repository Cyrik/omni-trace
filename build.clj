(ns build
  "clojure -T:build deploy
   clojure -T:build jar"
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'org.clojars.cyrik/omni-trace)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "0.3.%s" (b/git-count-revs nil)))

(def resource-dirs ["resources/"])

(defn clean "clean" [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/clean)))


(defn install2
  "Install the JAR to the local Maven repo cache.
  Requires: :lib, :version
  Accepts any options that are accepted by:
  * `tools.build/install`"
  {:arglists '([{:keys [lib version
                        basis class-dir classifier jar-file target]}])}
  [{:keys [lib version basis class-dir classifier jar-file target] :as opts}]
  (assert (and lib version) ":lib and :version are required for install")
  (println (bb/default-class-dir class-dir target))
  (println (or jar-file (bb/default-jar-file target lib version)))
  (let [target (bb/default-target target)]
    (b/install {:basis      (bb/default-basis basis)
                :lib        lib
                :classifier classifier
                :version    version
                :jar-file   (or jar-file (bb/default-jar-file target lib version))
                :class-dir  (bb/default-class-dir class-dir target)})
    opts))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (install2)))

(defn jar "Build jar." [opts]
  (-> opts
      (assoc :lib lib :version version :resource-dirs resource-dirs)
      (bb/jar)))

(defn uber "Build jar." [opts]
  (-> opts
      (assoc :lib lib 
             :version version 
             :ns-compile ['cyrik.omni-trace.testing-ns]
             )
      (bb/uber)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/jar)
      (bb/deploy)))