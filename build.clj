(ns build
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'org.clojars.cyrik/omni-trace)
;; if you want a version of MAJOR.MINOR.COMMITS:
(def version (format "0.1.%s" (b/git-count-revs nil)))

(defn clean "clean" [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/clean)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn jar "Build jar." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/jar)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/jar)
      (bb/deploy)))