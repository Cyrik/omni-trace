(ns omni-trace.flamegraph)

(defn flamedata [worksapce]
  (into [] (conj (vals worksapce) {:parent nil :name "root" :id :root})))

(defn flamegraph [data]
  {:title "flame"
   :padding 30
   :scales
   [{:name "color"
     :type "ordinal"
     :domain {:data "tree", :field "depth"}
     :range {:scheme "tableau20"}}]
   :marks
   [{:type "rect"
     :from {:data "tree"}
     :encode
     {:enter
      {:stroke {:value "white"}
       :strokeWidth {:value 0.1}

       :tooltip
       {:signal
        "{n: datum.name,
         a:datum.args,
         t:datum.thrown,
         r: datum.return}"}}
      :update
      {:x {:field "a0"}
       :x2 {:field "a1"}
       :y {:field "r0"}
       :y2 {:field "r1"}
       :fill {:scale "color", :field "depth"}
       :stroke {:value "white"}
       :strokeWidth {:value 0.1}}
      :hover
      {:stroke {:value "red"}
       :strokeWidth {:value 2}
       :zindex {:value 1}}}}]
   :$schema
   "https://vega.github.io/schema/vega/v5.json"
   :data
   [{:name "tree"
     :values data
     :transform
     [{:type "stratify"
       :key "id"
       :parentKey "parent"}
      {:type "partition"
       ;:field "size"
       :sort {:field "id"}
       :padding 2
       :size
       [{:signal "width"}
        {:signal "height"}]
       :as
       ["a0"
        "r0"
        "a1"
        "r1"
        "depth"
        "children"]}]}]})