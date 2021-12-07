(ns oz.day1
  (:require [cyrik.omni-trace.instrument :as i]
            [cyrik.omni-trace :as o]
            [cyrik.omni-trace.graph :as flame]
            [advent.day1]))

;; Trying to build a solution for: [Advent of Code day 1](https://adventofcode.com/2021/day/1)

;; The problem is how many times n + 1 is higher than n in an array.

;; I'll use [omni-trace](https://github.com/Cyrik/omni-trace) to help me find my mistakes 
;; and [oz](https://github.com/metasoarous/oz) to build this doc.

;; The initial numbers:
(def depths [199 200 208 210 200 207 240 269260 263])

;; A runner function thats going to be used inside reduce to count the number of times n is smaller then n+1
(defn runner1 [acc val] (if (< val (second acc))
                          [(inc (first acc)) val]
                          [(first acc) val]))

;; The actual solution function:
(defn count-increases1 [input]
  (reduce runner1
          (rest input)
          [0 (first input)]))

;; I'll just reduce over the input depths and an accumulator, that seems easy!

;; A run function so its easier to run the solution:
(defn run1 [] (count-increases1 depths))

;; Let's run it and see what we get:

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run1)]

;; That is not what I expected. Let's use omnitrace to see what happened:

[:vega (assoc (cyrik.omni-trace/rooted-flamegraph 'advent.day1/run1) :width 1200 :height 450)]

;; Oh, I see, when hovering over the calls in the flamegraph it's easy to see that 
;; reduce was called with the args switched.

;; Let's redefine it:

(defn count-increases2 [input]
  (reduce runner1
          [0 (first input)
           (rest input)]))
(defn run2 [] (count-increases2 depths))

;; And run it again:

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run2)]

;; Wow, it  blows up? I guess I'll try to see why.

[:vega (assoc (cyrik.omni-trace/rooted-flamegraph 'advent.day1/run2) :width 1200 :height 500)]

;; It seems we accidentally put the collection into the starting argument! Let's fix that.

(defn count-increases3 [input]
  (reduce runner1
          [0 (first input)]
          (rest input)))
(defn run3 [] (count-increases3 depths))

;; And run it again

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run3)]

;; Wrong again? This is starting to be embarrassing...

[:vega (assoc (cyrik.omni-trace/rooted-flamegraph 'advent.day1/run3) :width 1200 :height 500)]

;; It seems runner is only returning increased counts when the first arg is larger than the second.

;; I mixed up our prefix less then again ... let's fix it and run:

(defn runner2 [acc val] (if (< (second acc) val)
                          [(inc (first acc)) val]
                          [(first acc) val]))

(defn count-increases4 [input]
  (reduce runner2
          [0 (first input)]
          (rest input)))

(defn run4 [] (count-increases4 depths))

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run4)]

;; Puh finally correct! 


;; Let's try to do the advanced version as well!

;; This time I need to check a window of 3 numbers against the window moved up by one. That's the same thing right?

;; Let's see if we can use the same solution just interleaving the depths while dropping one every time:

[:pprint (interleave (range 10) (drop 1 (range 10)) (drop 2 (range 10)))]

;; Put it in:
(defn runner3 [acc val] (if (< (apply + (second acc)) (apply + val))
                          [(inc (first acc)) val]
                          [(first acc) val]))

(defn count-increases5 [input]
  (reduce runner3
          (rest (interleave input (drop 1 input) (drop 2 input)))
          [0 (first (interleave input (drop 1 input) (drop 2 input)))]))

(defn run5 [] (count-increases5 depths))

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run5)]

;; exception again?

[:vega (assoc (cyrik.omni-trace/rooted-flamegraph 'advent.day1/run5) :width 1200 :height 500)]

;; It seems we called runner with the wrong inputs.
;; Wouldn't it be great to see what reduce got as inputs to know what happened?

;; Let's take off the security breaks:


(defn run6 [] (count-increases5 depths))

[:pprint (reset! cyrik.omni-trace.instrument/ns-blacklist [])]

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run6)]

(reset! cyrik.omni-trace.instrument/ns-blacklist ['cljs.core 'clojure.core])

[:vega (assoc (cyrik.omni-trace/rooted-flamegraph 'advent.day1/run6) :width 1200 :height 690)]

;; *** interesting interlude: the traces show how lazily some of clojure's seq functions work ***

;; Oh! I called the final partition with the first element instead of calling first on partition!
;; easy fix: 


(defn count-increases6 [input]
  (reduce runner3
          (partition 3 (rest (interleave input (drop 1 input) (drop 2 input))))
          [0 (first (partition 3 (interleave input (drop 1 input) (drop 2 input))))]))

;; Ah switched around again, this time i see it! (plus the indexes are matching again ...)

(defn count-increases7 [input]
  (reduce runner3
          [0 (first (partition 3 (interleave input (drop 1 input) (drop 2 input))))]
          (partition 3 (rest (interleave input (drop 1 input) (drop 2 input))))))

(defn run7 [] (count-increases7 depths))

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run7)]

;; I got that working as well, finally.

;; Although it does look a little ugly, let's clean it up by using the step version of partition and not running it twice:

(defn count-increases-final [input]
    (let [windowed (partition 3 1 input)]
      (reduce runner3
              [0 (first windowed)]
              (rest windowed))))

(defn run-final [] (count-increases-final depths))

[:pprint (cyrik.omni-trace/run-traced 'advent.day1/run-final)]