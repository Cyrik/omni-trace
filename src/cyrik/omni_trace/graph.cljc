(ns cyrik.omni-trace.graph)

(defn flamedata 
  ([workspace]
   (into [] (conj (vals (:log workspace)) {:parent nil :name "root" :id :root})))
  ([workspace root]
   (->> (vals (:log workspace))
        (map #(if (= (:name %) root) (assoc % :parent nil) %))
        (filter #(not= (:parent %) :root)))))

(defn flamegraph-with-click [data]
  {:title "flame"
   :autosize {:type "fit" :resize false :contains "padding"}
   :padding 30
   :scales
   [{:name "color"
     :type "ordinal"
     :domain {:data "tree", :field "name"}
     :range {:scheme "tableau20"}}
    {:name "xscale", :zero false, :domain {:signal "xdom"}, :range {:signal "xrange"}}
    {:name "yscale", :zero false, :domain {:signal "ydom"}, :range {:signal "yrange"}}]
   :signals
   [{:name "clicked", :value nil, :on [{:events "click", :update "datum ? {value: datum.id, depth: datum.depth} : null", :force true}]}
    {:name "clear", :value true, :on [{:events "mouseup[!event.item]", :update "true", :force true}]}
    {:name "shift", :value false, :on [{:events "click", :update "event.shiftKey", :force true}]}
    {:name "xdom"
     :update "slice(xext)"
     :on
     [{:events {:signal "delta"}
       :update "[xcur[0] + span(xcur) * delta[0] / width, xcur[1] + span(xcur) * delta[0] / width]"}
      {:events {:signal "zoom"}
       :update "[anchor[0] + (xdom[0] - anchor[0]) * zoom, anchor[0] + (xdom[1] - anchor[0]) * zoom]"}]}
    {:name "ydom"
     :update "slice(yext)"
     :on
     [{:events {:signal "delta"}
       :update "[ycur[0] + span(ycur) * delta[1] / height, ycur[1] + span(ycur) * delta[1] / height]"}
      {:events {:signal "zoom"}
       :update "[anchor[1] + (ydom[0] - anchor[1]) * zoom, anchor[1] + (ydom[1] - anchor[1]) * zoom]"}]}
    {:name "size", :update "clamp(20 / span(xdom), 0.01, 1000)"}
    {:name "xext", :update "[0, width]"} ;; hardcode value dimensions, might use this later
    {:name "yext", :update "[height, 0]"} 
    {:name "xcur", :value nil, :on [{:events "mousedown, touchstart, touchend", :update "slice(xdom)"}]}
    {:name "ycur", :value nil, :on [{:events "mousedown, touchstart, touchend", :update "slice(ydom)"}]}
    {:name "delta"
     :value [0 0]
     :on
     [{:events
       [{:source "window"
         :type "mousemove"
         :consume true
         :between [{:type "mousedown"} {:source "window", :type "mouseup"}]}
        {:type "touchmove", :consume true, :filter "event.touches.length === 1"}]
       :update "down ? [down[0]-x(), y()-down[1]] : [0,0]"}]}
    {:name "anchor"
     :value [0 0]
     :on
     [{:events "wheel", :update "[width / 2, height /2]"}
      {:events {:type "touchstart", :filter "event.touches.length===2"}, :update "[width / 2, height /2]"}]}
    {:name "zoom"
     :value 1
     :on
     [{:events "wheel!", :force true, :update "pow(1.001, event.deltaY * pow(16, event.deltaMode))"}
      {:events {:signal "dist2"}, :force true, :update "dist1 / dist2"}]}
    {:name "down"
     :value nil
     :on [{:events "touchend", :update "null"} {:events "mousedown, touchstart", :update "xy()"}]}
    {:name "dist1"
     :value 0
     :on
     [{:events {:type "touchstart", :filter "event.touches.length===2"}, :update "pinchDistance(event)"}
      {:events {:signal "dist2"}, :update "dist2"}]}
    {:name "dist2"
     :value 0
     :on
     [{:events {:type "touchmove", :consume true, :filter "event.touches.length===2"}, :update "pinchDistance(event)"}]}
    {:name "xrange", :update "[0, width]"}
    {:name "yrange", :update "[height,0]"}]

   :marks
   [{:type "rect"
     :from {:data "filter-tree"}
     :encode
     {:enter
      {:tooltip
       {:signal
        "{name: datum.name,
         args: datum.args,
         throw: datum.thrown,
         return: datum.return}"}}
      :update
            {:x {:scale "xscale" :field "a0"}
       :x2 {:scale "xscale" :field "a1"}
       :y {:scale "yscale" :field "r0"}
       :y2 {:scale "yscale" :field "r1"}
       :fill {:scale "color", :field "name"}
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
      {:fontSize {:signal "size * 3.0 * (datum.a1 - datum.a0)"}
       :x {:scale "xscale" :signal "0.5 * (datum.a0 + datum.a1)"}
       :y {:scale "yscale" :signal "0.5 * (datum.r0 + datum.r1)"}}}}]
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
     [{:key "id", :parentKey "parent", :type "stratify"}
      {:as ["a0" "r0" "a1" "r1" "depth" "children"]
       :type "partition"
       :size [{:signal "width"} {:signal "height"}]
       :padding 2
       :sort {:field "id"}}]}]})

(defn flamegraph [data]
  (flamegraph-with-click data))
