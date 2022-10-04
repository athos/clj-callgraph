(ns clj-callgraph.diff
  (:require [editscript.core :as e]))

(defn- reversed-deps [deps]
  (reduce (fn [ret [k {:keys [deps]}]]
            (reduce #(update %1 %2 (fnil conj #{}) k) ret deps))
          {} deps))

(defn- merge-diff [deps1 deps2 diff]
  (let [rev (reversed-deps deps1)]
    (reduce (fn [deps [[k & ks] op]]
              (-> (case op
                    :+ (if ks
                         (assoc-in deps [k :edges (second ks)] {:added true})
                         (assoc-in deps [k :added] true))
                    :- (if ks
                         (assoc-in deps [k :edges (second ks)] {:removed true})
                         (let [attrs (get deps1 k)]
                           (-> (reduce (fn [deps k']
                                         (update-in deps [k' :deps]
                                                    (fnil conj #{})
                                                    k))
                                       deps (rev k))
                               (update k merge (select-keys attrs [:ns :name]))
                               (update-in [k :deps] (fnil into #{})
                                          (:deps attrs))
                               (assoc-in [k :removed] true)))))
                  (assoc-in [k :changed] true)))
            deps2 diff)))

(defn- mark-changes [deps]
  (let [rev (reversed-deps deps)
        queue (into (clojure.lang.PersistentQueue/EMPTY)
                    (comp (keep (fn [[k {:keys [changed]}]]
                                  (when changed (rev k))))
                          cat)
                    deps)]
    (loop [queue queue, deps deps]
      (if (empty? queue)
        deps
        (let [k (peek queue)
              {:keys [changed]} (get deps k)]
          (if changed
            (recur (pop queue) deps)
            (recur (into (pop queue) (rev k))
                   (assoc-in deps [k :changed] true))))))))

(defn- annotate-ns-changes [deps]
  (let [entries-by-ns (group-by (comp :ns val) deps)
        stats (reduce-kv
               (fn [stats ns entries]
                 (reduce
                  (fn [stats [_ {:keys [added removed]}]]
                    (cond-> stats
                      added (update-in [ns :added] (fnil inc 0))
                      removed (update-in [ns :removed] (fnil inc 0))))
                  stats entries))
               {} entries-by-ns)
        ns-changes (reduce-kv (fn [changes ns {:keys [added removed]}]
                                (let [n (count (get entries-by-ns ns))]
                                  (cond-> changes
                                    (= n added) (assoc ns :added)
                                    (= n removed) (assoc ns :removed))))
                              {} stats)]
    (reduce-kv (fn [deps k {:keys [ns]}]
                 (if-let [change (get ns-changes ns)]
                   (case change
                     :added (assoc-in deps [k :ns-added] true)
                     :removed (assoc-in deps [k :ns-removed] true))
                   deps))
               deps deps)))

(defn- shrink-unchanged [deps]
  (into {} (keep (fn [[k attrs]]
                   (when (:changed attrs)
                     [k
                      (update attrs :deps
                              #(into #{} (filter (comp :changed deps)) %))])))
        deps))

(defn build-diff-deps [deps1 deps2]
  (let [diff (e/get-edits (e/diff deps1 deps2))]
    (-> (merge-diff deps1 deps2 diff)
        mark-changes
        annotate-ns-changes
        shrink-unchanged)))
