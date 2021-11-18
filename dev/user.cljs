(ns user
  (:require [omni-trace.omni-trace :as o]
            [omni-trace.testing-ns :as e]
            [omni-trace.flamegraph :as flame]
            [portal.web :as p]
            [cljs.pprint :as pp]))

(.log js/console "test")

(def ^{:a 5} test-var  1)

(comment
  (def portal (p/open))
  (add-tap #'p/submit)
  (o/instrument-ns 'omni-trace.testing-ns)
  (.log js/console (-> e/machine-init
                       (e/insert-coin :quarter)
                       (e/insert-coin :dime)
                       (e/insert-coin :nickel)
                       (e/insert-coin :penny)
                       (e/press-button :a1)))
  (pp/pprint @o/workspace)
  
  (tap>(flame/flamegraph (flame/flamedata @o/workspace)))
  (o/uninstrument-ns 'omni-trace.testing-ns)

  o/instrumented-vars
  (macroexpand '(o/uninstrument-ns 'omni-trace.testing-ns))
  (alter-meta! (var omni-trace.testing-ns/insert-coin )assoc-in [:stuffs] "yeah123")
  (.log js/console (meta (var omni-trace.testing-ns/insert-coin)))

  ({1 :1 2 :2 3 :3} 1)
  .)
  