(ns omni-trace.flamegraph)

(defn flamedata [worksapce]
  (into [] (conj (vals (:log worksapce)) {:parent nil :name "root" :id :root})))

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

(defn flamegraph-with-click [data]
  {:title "flame"
   :padding 30
   :scales
   [{:name "color"
     :type "ordinal"
     :domain {:data "filter-tree", :field "depth"}
     :range {:scheme "tableau20"}}]
   :signals
   [{:name "clicked", :value nil, :on [{:events "click", :update "datum ? {value: datum.id, depth: datum.depth} : null", :force true}]}
    {:name "clear", :value true, :on [{:events "mouseup[!event.item]", :update "true", :force true}]}
    {:name "shift", :value false, :on [{:events "click", :update "event.shiftKey", :force true}]}]

   :marks
   [{:type "rect"
     :from {:data "filter-tree"}
     :encode
     {:enter
      {:tooltip
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
       :zindex {:value 1}}}}
    {:type "text"
     :from {:data "filter-tree"}
     :interactive false
     :encode
     {:enter
      {:font {:value "Helvetica Neue, Arial"}
       :align {:value "center"}
       :baseline {:value "middle"}
       :fill {:value "white"}
       :text {:field "name"}}
      :update
      {:fontSize {:signal "0.05 * (datum.a1 - datum.a0)"}
       :x {:signal "0.5 * (datum.a0 + datum.a1)"}
       :y {:signal "0.5 * (datum.r0 + datum.r1)"}}}}]
   :$schema
   "https://vega.github.io/schema/vega/v5.json"
   :data
   [{:name "selected"
     :on
     [{:trigger "clear", :remove true}
      {:trigger "!shift", :remove true}
      {:trigger "!shift && clicked", :insert "clicked"}
      {:trigger "shift && clicked", :toggle "clicked"}]}
    {:name "tree"
     :values data
     :transform
     [{:type "stratify"
       :key "id"
       :parentKey "parent"}]}
    {:name "tree-ancestors"
     :source "tree"
     :transform [{:type "formula", :expr "treeAncestors('tree', datum.id, 'root')", :as "treeAncestors"}]}
    {:name "tree-ancestors-flatt", :source "tree-ancestors", :transform [{:type "flatten", :fields ["treeAncestors"]}]}
    {:name "selected-ancestors"
     :source "tree-ancestors"
     :transform [{:type "filter", :expr "indata('selected', 'value', datum.id)"}]}
    {:name "selected-ancestors-flat"
     :source "selected-ancestors"
     :transform [{:type "flatten", :fields ["treeAncestors"]}]}
    {:name "filtered"
     :source "tree-ancestors-flatt"
     :transform
     [{:type "filter"
       :expr "!length(data('selected')) || datum.parent == null || indata('selected', 'value', datum.treeAncestors.id)"}]}
    {:name "filtered-aggregate", :source "filtered", :transform [{:type "aggregate", :groupby ["id"]}]}
    {:name "filtered-tree"
     :source "tree"
     :transform
     [{:type "filter"
       :expr
       "!length(data('selected'))|| indata('filtered-aggregate', 'id', datum.id) || indata('selected-ancestors-flat', 'treeAncestors.id', datum.id)"}]}
    {:name "filter-tree"
     :source "filtered-tree"
     :transform
     [
      {:key "id", :parentKey "parent", :type "stratify"}
      {:as ["a0" "r0" "a1" "r1" "depth" "children"]
       :type "partition"
       :size [{:signal "width"} {:signal "height"}]
       :padding 2
       :sort {:field "id"}}]}]})