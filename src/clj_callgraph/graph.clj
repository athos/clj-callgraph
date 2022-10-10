(ns clj-callgraph.graph)

(defn transpose-graph [graph]
  (reduce-kv (fn [ret k {:keys [deps]}]
               (reduce #(update-in %1 [%2 :deps] (fnil conj #{}) k) ret deps))
             {} graph))
