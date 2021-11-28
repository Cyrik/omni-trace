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
  (:analysis (clj-kondo/run! {:lint files
                              :config {:output {:analysis true}}})))

(defn deps [analysis] (reduce (fn [graph {:keys [:from :to :filename :row :col :from-var :name]}]
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
                              (filter #(= :clj (:lang %)) (:var-usages analysis))))

(defn transitive-deps [deps ns sym]
  (conj (dep/transitive-dependencies deps [ns sym]) [ns sym]))

(defn sss [deep-deps instrumenter]
  (doseq [node deep-deps]
    (instrumenter (symbol (name (first node)) (name (second node))))))

(defn deep-trace* [ns sym instrumenter]
  (let [deps (deps (analysis ["dev" "src"]#_(classpaths)))
        deep-deps (transitive-deps deps ns sym)]
    (sss deep-deps instrumenter)
    [deps deep-deps]))

(defn deep-trace [ns sym]
  (deep-trace* ns sym #(i/instrument-fn %)))
(defn deep-untrace [ns sym]
  (deep-trace* ns sym #(i/uninstrument-fn %)))

(defn run-traced [s & args]
  (let [v (resolve s)
        ns (symbol (namespace s))
        s (symbol (name s))
        [deps-dag deep-deps] (deep-trace ns s)
        ;; _ (println v ns s deep-deps)
        result (try (apply @v args)
                    (catch Throwable t
                      (Throwable->map t)))
        _ (sss deep-deps #(i/uninstrument-fn %))]
    result))

(comment
  (require '[portal.api :as p])
  (def portal (p/open))
  (add-tap #'p/submit)

  (time (def analysis (clj-kondo/run! {:parallel true
                                       :cache true
                                       :lint ["/Users/lukas/Workspace/clojure/omni-trace/src"] #_["dev" "src" "/Users/lukas/.m2/repository/clj-kondo/clj-kondo/2021.10.19/clj-kondo-2021.10.19.jar" "/Users/lukas/.m2/repository/com/stuartsierra/dependency/1.0.0/dependency-1.0.0.jar" "/Users/lukas/.m2/repository/criterium/criterium/0.4.6/criterium-0.4.6.jar" "/Users/lukas/.m2/repository/djblue/portal/0.18.0/portal-0.18.0.jar" "/Users/lukas/.m2/repository/net/cgrand/macrovich/0.2.1/macrovich-0.2.1.jar" "/Users/lukas/.m2/repository/net/clojars/cyrik/cljs-macroexpand/0.1.1/cljs-macroexpand-0.1.1.jar" "/Users/lukas/.m2/repository/org/clojure/clojure/1.10.3/clojure-1.10.3.jar" "/Users/lukas/.m2/repository/org/clojure/clojurescript/1.10.879/clojurescript-1.10.879.jar" "/Users/lukas/.m2/repository/thheller/shadow-cljs/2.15.5/shadow-cljs-2.15.5.jar" "/Users/lukas/.m2/repository/borkdude/sci/0.2.6/sci-0.2.6.jar" "/Users/lukas/.m2/repository/cheshire/cheshire/5.10.0/cheshire-5.10.0.jar" "/Users/lukas/.m2/repository/com/cognitect/transit-clj/1.0.324/transit-clj-1.0.324.jar" "/Users/lukas/.m2/repository/io/lambdaforge/datalog-parser/0.1.8/datalog-parser-0.1.8.jar" "/Users/lukas/.m2/repository/nrepl/bencode/1.1.0/bencode-1.1.0.jar" "/Users/lukas/.m2/repository/com/cognitect/transit-cljs/0.8.269/transit-cljs-0.8.269.jar" "/Users/lukas/.m2/repository/http-kit/http-kit/2.5.3/http-kit-2.5.3.jar" "/Users/lukas/.m2/repository/org/clojure/data.json/2.4.0/data.json-2.4.0.jar" "/Users/lukas/.m2/repository/org/clojure/core.specs.alpha/0.2.56/core.specs.alpha-0.2.56.jar" "/Users/lukas/.m2/repository/org/clojure/spec.alpha/0.2.194/spec.alpha-0.2.194.jar" "/Users/lukas/.m2/repository/com/google/javascript/closure-compiler-unshaded/v20210505/closure-compiler-unshaded-v20210505.jar" "/Users/lukas/.m2/repository/org/clojure/google-closure-library/0.0-20201211-3e6c510d/google-closure-library-0.0-20201211-3e6c510d.jar" "/Users/lukas/.m2/repository/cider/piggieback/0.5.2/piggieback-0.5.2.jar" "/Users/lukas/.m2/repository/com/bhauman/cljs-test-display/0.1.1/cljs-test-display-0.1.1.jar" "/Users/lukas/.m2/repository/com/wsscode/pathom/2.2.31/pathom-2.2.31.jar" "/Users/lukas/.m2/repository/expound/expound/0.8.9/expound-0.8.9.jar" "/Users/lukas/.m2/repository/fipp/fipp/0.6.24/fipp-0.6.24.jar" "/Users/lukas/.m2/repository/hiccup/hiccup/1.0.5/hiccup-1.0.5.jar" "/Users/lukas/.m2/repository/io/methvin/directory-watcher/0.15.0/directory-watcher-0.15.0.jar" "/Users/lukas/.m2/repository/nrepl/nrepl/0.8.3/nrepl-0.8.3.jar" "/Users/lukas/.m2/repository/org/clojure/core.async/1.3.618/core.async-1.3.618.jar" "/Users/lukas/.m2/repository/org/clojure/google-closure-library-third-party/0.0-20201211-3e6c510d/google-closure-library-third-party-0.0-20201211-3e6c510d.jar" "/Users/lukas/.m2/repository/org/clojure/test.check/1.1.0/test.check-1.1.0.jar" "/Users/lukas/.m2/repository/org/clojure/tools.cli/1.0.206/tools.cli-1.0.206.jar" "/Users/lukas/.m2/repository/org/clojure/tools.reader/1.3.6/tools.reader-1.3.6.jar" "/Users/lukas/.m2/repository/org/graalvm/js/js/21.1.0/js-21.1.0.jar" "/Users/lukas/.m2/repository/org/graalvm/js/js-scriptengine/21.1.0/js-scriptengine-21.1.0.jar" "/Users/lukas/.m2/repository/ring/ring-core/1.9.4/ring-core-1.9.4.jar" "/Users/lukas/.m2/repository/thheller/shadow-client/1.3.3/shadow-client-1.3.3.jar" "/Users/lukas/.m2/repository/thheller/shadow-cljsjs/0.0.22/shadow-cljsjs-0.0.22.jar" "/Users/lukas/.m2/repository/thheller/shadow-undertow/0.1.0/shadow-undertow-0.1.0.jar" "/Users/lukas/.m2/repository/thheller/shadow-util/0.7.0/shadow-util-0.7.0.jar" "/Users/lukas/.m2/repository/borkdude/edamame/0.0.11/edamame-0.0.11.jar" "/Users/lukas/.m2/repository/borkdude/sci.impl.reflector/0.0.1/sci.impl.reflector-0.0.1.jar" "/Users/lukas/.m2/repository/com/fasterxml/jackson/core/jackson-core/2.10.2/jackson-core-2.10.2.jar" "/Users/lukas/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.10.2/jackson-dataformat-cbor-2.10.2.jar" "/Users/lukas/.m2/repository/com/fasterxml/jackson/dataformat/jackson-dataformat-smile/2.10.2/jackson-dataformat-smile-2.10.2.jar" "/Users/lukas/.m2/repository/tigris/tigris/0.1.2/tigris-0.1.2.jar" "/Users/lukas/.m2/repository/com/cognitect/transit-java/1.0.343/transit-java-1.0.343.jar" "/Users/lukas/.m2/repository/com/cognitect/transit-js/0.8.874/transit-js-0.8.874.jar" "/Users/lukas/.m2/repository/com/wsscode/spec-inspec/1.0.0-alpha2/spec-inspec-1.0.0-alpha2.jar" "/Users/lukas/.m2/repository/edn-query-language/eql/0.0.9/eql-0.0.9.jar" "/Users/lukas/.m2/repository/spec-coerce/spec-coerce/1.0.0-alpha6/spec-coerce-1.0.0-alpha6.jar" "/Users/lukas/.m2/repository/org/clojure/core.rrb-vector/0.1.1/core.rrb-vector-0.1.1.jar" "/Users/lukas/.m2/repository/net/java/dev/jna/jna/5.7.0/jna-5.7.0.jar" "/Users/lukas/.m2/repository/org/slf4j/slf4j-api/1.7.30/slf4j-api-1.7.30.jar" "/Users/lukas/.m2/repository/org/clojure/tools.analyzer.jvm/1.1.0/tools.analyzer.jvm-1.1.0.jar" "/Users/lukas/.m2/repository/com/ibm/icu/icu4j/68.2/icu4j-68.2.jar" "/Users/lukas/.m2/repository/org/graalvm/regex/regex/21.1.0/regex-21.1.0.jar" "/Users/lukas/.m2/repository/org/graalvm/sdk/graal-sdk/21.1.0/graal-sdk-21.1.0.jar" "/Users/lukas/.m2/repository/org/graalvm/truffle/truffle-api/21.1.0/truffle-api-21.1.0.jar" "/Users/lukas/.m2/repository/commons-fileupload/commons-fileupload/1.4/commons-fileupload-1.4.jar" "/Users/lukas/.m2/repository/commons-io/commons-io/2.10.0/commons-io-2.10.0.jar" "/Users/lukas/.m2/repository/crypto-equality/crypto-equality/1.0.0/crypto-equality-1.0.0.jar" "/Users/lukas/.m2/repository/crypto-random/crypto-random/1.2.1/crypto-random-1.2.1.jar" "/Users/lukas/.m2/repository/ring/ring-codec/1.1.3/ring-codec-1.1.3.jar" "/Users/lukas/.m2/repository/io/undertow/undertow-core/2.2.4.Final/undertow-core-2.2.4.Final.jar" "/Users/lukas/.m2/repository/javax/xml/bind/jaxb-api/2.3.0/jaxb-api-2.3.0.jar" "/Users/lukas/.m2/repository/org/msgpack/msgpack/0.6.12/msgpack-0.6.12.jar" "/Users/lukas/.m2/repository/org/clojure/core.memoize/1.0.236/core.memoize-1.0.236.jar" "/Users/lukas/.m2/repository/org/clojure/tools.analyzer/1.0.0/tools.analyzer-1.0.0.jar" "/Users/lukas/.m2/repository/org/ow2/asm/asm/5.2/asm-5.2.jar" "/Users/lukas/.m2/repository/commons-codec/commons-codec/1.15/commons-codec-1.15.jar" "/Users/lukas/.m2/repository/org/jboss/logging/jboss-logging/3.4.1.Final/jboss-logging-3.4.1.Final.jar" "/Users/lukas/.m2/repository/org/jboss/threads/jboss-threads/3.1.0.Final/jboss-threads-3.1.0.Final.jar" "/Users/lukas/.m2/repository/org/jboss/xnio/xnio-api/3.8.0.Final/xnio-api-3.8.0.Final.jar" "/Users/lukas/.m2/repository/org/jboss/xnio/xnio-nio/3.8.0.Final/xnio-nio-3.8.0.Final.jar" "/Users/lukas/.m2/repository/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar" "/Users/lukas/.m2/repository/org/javassist/javassist/3.18.1-GA/javassist-3.18.1-GA.jar" "/Users/lukas/.m2/repository/org/clojure/core.cache/1.0.207/core.cache-1.0.207.jar" "/Users/lukas/.m2/repository/org/wildfly/client/wildfly-client-config/1.0.1.Final/wildfly-client-config-1.0.1.Final.jar" "/Users/lukas/.m2/repository/org/wildfly/common/wildfly-common/1.5.2.Final/wildfly-common-1.5.2.Final.jar" "/Users/lukas/.m2/repository/org/clojure/data.priority-map/1.0.0/data.priority-map-1.0.0.jar"]
                                       :config {:output {:analysis true}}})))



  (keys analysis)
  (:summary analysis)
  .
  )