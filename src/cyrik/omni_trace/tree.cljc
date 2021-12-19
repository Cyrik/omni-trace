(ns cyrik.omni-trace.tree)

(defn- str->int [s]
  #?(:clj (Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn calls* [n tree]
  (filter #(= (:name %) n) (vals tree)))

(defn calls [n data]
  (sort-by #(-> % :id n str->int) (calls* n data)))

(defn last-call [n data]
  (last (calls n data)))

(defn dependants [n traces]
  (let [roots (calls* n traces)]
    (loop [[x & xs :as nodes] roots
           seen []]
      (cond
        (empty? nodes) seen
        (empty? (:children x)) (recur xs (conj seen x))
        (coll? (:children x)) (recur 
                               (into xs (vals (select-keys traces (:children x)))) 
                               (conj seen x))))))


(comment

  (:log @i/workspace)
  (o/reset-workspace!)
  (o/run-traced 'cyrik.omni-trace.testing-ns/run-machine)


  (calls* 'cyrik.omni-trace.testing-ns/insert-coin (:log @i/workspace))

  (last-call 'cyrik.omni-trace.testing-ns/insert-coin (:log @i/workspace))

  (require '[meander.epsilon :as m])
  (defn calls-m-fast [n data]
    (m/search data
              (m/scan {:name ~n :as ?trace})
              [?trace]))
  (require '[com.rpl.specter :as specter])
  (defn calls-s [n data]
    (specter/select [specter/ALL #(= (:name %) n)] data))
  

  (def deps (dependants 'cyrik.omni-trace.testing-ns/press-button (:log @i/workspace)))
  (count deps)
  
  )