(ns user
  (:require [omni-trace.omni-trace :as o]
            [omni-trace.testing-ns :as e]
            [omni-trace.flamegraph :as flame]
            [portal.web :as p]))


(comment
  ;instrument a namespace
  (o/instrument-ns 'omni-trace.testing-ns)
  ;run functions in that namespace
  (-> e/machine-init
      (e/insert-coin :quarter)
      (e/insert-coin :dime)
      (e/insert-coin :nickel)
      (e/insert-coin :penny)
      (e/press-button :a1)
      (e/retrieve-change-returned))
  ;look at traces for every function that was traced
  @o/workspace
  ;connect to portal
  (def portal (p/open))
  (add-tap #'p/submit)
  ;send the trace to portal as a vegajs flamegraph
  (tap> (flame/flamegraph (flame/flamedata @o/workspace)))
  (tap> ^{:portal.viewer/default :portal.viewer/hiccup} [:button {:onclick (str "alert('123')")} (str "alert('123')")])
  [:button {:onclick "alert('123')"} "Test it!"]
  ;remove tracing from a namesapce
  (o/uninstrument-ns 'omni-trace.testing-ns)
  (o/reset-workspace!)
  o/instrumented-vars
  (macroexpand '(o/uninstrument-ns 'omni-trace.testing-ns))
  (alter-meta! (var omni-trace.testing-ns/insert-coin) assoc-in [:stuffs] "yeah123")
  (.log js/console (meta (var omni-trace.testing-ns/insert-coin)))

  ({1 :1 2 :2 3 :3} 1)
  .
  )
  