(ns user
  (:require [omni-trace.omni-trace :as o]
            [omni-trace.testing-ns :as e]
            [clojure.java.io :as io]
            [portal.api :as po]
            [clojure.pprint :as pprint]
            [omni-trace.flamegraph :as flame]))

(defn spit-pretty!
  "Writes the pretty-printed edn `data` into the `file`."
  [file data]
  (spit (io/file file)
        (with-out-str
          (pprint/pprint data))))



(comment
  (def portal (po/open))
  (add-tap #'po/submit)
  (o/instrument-ns 'omni-trace.testing-ns
                       {::o/workspace omni-trace.omni-trace/workspace})
  (-> e/machine-init
      (e/insert-coin :quarter)
      (e/insert-coin :dime)
      (e/insert-coin :nickel)
      (e/insert-coin :penny)
      (e/press-button :a1))
  (flame/flamegraph (flame/flamedata @o/workspace))
  (meta #'e/insert-coin)

  o/instrumented-vars

  (tap> ^{:portal.viewer/default :portal.viewer/hiccup}
   [:div "custom thing here" [:portal.viewer/inspector {:complex :data-structure}]])
  
  (o/uninstrument-ns 'omni-trace.testing-ns)
  (macroexpand '(o/instrument-ns 'omni-trace.testing-ns))


  (intern *ns* '~'symname "<<symdefinition>>")
  .)
  