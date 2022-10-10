(ns clj-callgraph.namespaces
  (:require [clj-callgraph.graph :as graph]))

(defn ->ns-graph [deps]
  (reduce-kv (fn [ret _ {:keys [ns] :as attrs}]
               (-> ret
                   (assoc-in [ns :name] ns)
                   (update-in [ns :deps]
                              (fnil into #{})
                              (comp (map (comp :ns deps))
                                    (remove #{ns}))
                              (:deps attrs))))
             {} deps))

(defn topo-sorted-namespaces [deps]
  (graph/topo-sort (->ns-graph deps)))
