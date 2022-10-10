(ns clj-callgraph.graph)

(defn transpose-graph [graph]
  (reduce-kv (fn [ret k {:keys [deps]}]
               (reduce #(update-in %1 [%2 :deps] (fnil conj #{}) k) ret deps))
             {} graph))

(defn topo-sort [graph]
  (loop [ret ()
         g graph
         r (transpose-graph g)
         queue (into #{} (remove (comp seq :deps r)) (keys g))]
    (if (seq queue)
      (let [x (first queue)
            [add g' r'] (loop [deps (get-in g [x :deps]), g g, r r, add #{}]
                          (if (seq deps)
                            (let [d (first deps)
                                  g' (update-in g [x :deps] disj d)
                                  r' (update-in r [d :deps] disj x)]
                              (recur (rest deps) g' r'
                                     (cond-> add
                                       (empty? (get-in r' [d :deps]))
                                       (conj d))))
                            [add g r]))]
        (recur (cons x ret)
               (dissoc g' x)
               (dissoc r' x)
               (into (disj queue x) add)))
      (vec ret))))
