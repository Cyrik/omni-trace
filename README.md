# omni-trace
Omnipotent/omniscient tracing core for debugging clojure(script)

very early alpha, api is still unstable but its only for dev time so there shouldn't be any problems.
moved namespaces to cyrik.omni-trace and below. cyrik.omni-trace is the main entry point.


[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.cyrik/omni-trace.svg)](https://clojars.org/org.clojars.cyrik/omni-trace)

or just through github source: 

```clojure

:deps {omni_trace/omni_trace  {:git/url "https://github.com/Cyrik/omni-trace"
                               :sha     "5def7f9ad31d703317e5be5e64a57322e1c89eed"}}
                          
```

## Usage

[contrived example](https://github.com/bpiel/contrived-example) code for demo debugging pupose

```clojure
(ns user
  (:require [cyrik.omni-trace :as o]
            [cyrik.omni-trace.testing-ns :as e]
            [cyrik.omni-trace.flamegraph :as flame]
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
      (e/retrieve-change-returned)) ;throws on purpose for demonstration
  ;look at traces for every function that was traced
  @o/workspace
  ;connect to portal
  (def portal (p/open))
  (add-tap #'p/submit)
  ;send the trace to portal as a vegajs flamegraph
  (tap> (flame/flamegraph (flame/flamedata @o/workspace)))
  ;remove tracing from a namesapce
  (o/uninstrument-ns 'omni-trace.testing-ns))
```

![Screenshot](docs/demo.gif)

# experimental deeptrace(clojure only for now)
```clojure
(require '[cyrik.omni-trace :as o])
(o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)
(tap> (o/rooted-flamegraph 'cyrik.omni-trace.testing-ns/run-machine))
```

This uses [clj-kondo](https://github.com/clj-kondo/clj-kondo) to find all transitive calls from the provided symbol.
It then runs the function with any supplied args and untraces everything. This reaches all the way down into clojure.core.


![Screenshot](docs/deep-trace.png)

Currently there are still a few problems with recursion and cljs needs to be implemented as well.


## Features
- Works in clojure and clojurescript
- Instrument whole namespaces from the repl
- show the trace as a Flamegraph in Portal or anything else that understands Vega.js
- remembers the call that caused the exception and shows the arguments
- stops tracing callsites if they have been called to often, default is 100, can be changed with :omni-trace.omni-trace/max-callsite-log option


## In the works
- better trace output to the REPL
- performance
- callbacks from Portal so you can rerun an updated function with the old params by clicking on it in the Flamegraph
- [Debux](https://github.com/philoskim/debux) integration for inner function traces
- trace a function recursivley so you can see the whole graph without without knowing what to trace
- (maybe) timetravel in trace
- [Calva](https://github.com/BetterThanTomorrow/calva/) integration to display traces inline

## Related works
- [Debux](https://github.com/philoskim/debux): tracing library that show what is going on inside of a function call. Hopefully this can be integrated as the "inner" function trace in omni-trace
- [Sayid](https://github.com/clojure-emacs/sayid/): clojure only version of what omni-trace is trying to do
- [Postmortem](https://github.com/athos/Postmortem): great library for debugging dataflow
