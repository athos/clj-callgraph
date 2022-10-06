(ns clj-callgraph.diff
  (:require [editscript.core :as e]))

(defn- reversed-deps [deps]
  (reduce (fn [ret [k {:keys [deps]}]]
            (reduce #(update %1 %2 (fnil conj #{}) k) ret deps))
          {} deps))

(defn- strip-unnecessary-attrs [deps]
  (reduce-kv (fn [deps k _]
               (update deps k select-keys [:ns :name :deps]))
             deps deps))

(defn- add-status [attrs status]
  (update attrs :status (fnil conj #{}) status))

(defn- merge-diff [deps1 deps2 diff]
  (let [rev (reversed-deps deps1)]
    (reduce (fn [deps [[k & ks] op more]]
              (-> (case op
                    :+ (if ks
                         (assoc-in deps [k :edges (second ks)] {:added true})
                         (update deps k add-status :added))
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
                               (update k add-status :removed))))
                    :r (if ks
                         (let [attrs (get deps1 k)]
                           (-> deps
                               (update-in [k :deps] into (:deps attrs))
                               (assoc-in [k :edges]
                                         (into {}
                                               (map (fn [k']
                                                      [k' {:removed true}]))
                                               (:deps attrs)))
                               (update-in [k :edges] into
                                          (map (fn [k'] [k' {:added true}]))
                                          more)))
                         (assert false "Should not be reached here")))
                  (update k add-status :changed)))
            deps2 diff)))

(defn- affected? [{:keys [status]}]
  (or (:changed status) (:affected status)))

(defn- mark-affected [deps]
  (let [rev (reversed-deps deps)
        queue (into (clojure.lang.PersistentQueue/EMPTY)
                    (mapcat (fn [[k {:keys [status]}]]
                              (when (:changed status) (rev k))))
                    deps)]
    (loop [queue queue, deps deps]
      (if (empty? queue)
        deps
        (let [k (peek queue)
              node (get deps k)]
          (if (affected? node)
            (recur (pop queue) deps)
            (recur (into (pop queue)
                         (filter (fn [k']
                                   (let [node' (get deps k')]
                                     (= (:ns node) (:ns node')))))
                         (rev k))
                   (update deps k add-status :affected))))))))

(defn- annotate-with-ns-changes [deps]
  (let [entries-by-ns (group-by (comp :ns val) deps)
        stats (reduce-kv
               (fn [stats ns entries]
                 (reduce
                  (fn [stats [_ {:keys [status]}]]
                    (cond-> stats
                      (:added status) (update-in [ns :added] (fnil inc 0))
                      (:removed status) (update-in [ns :removed] (fnil inc 0))))
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

(defn- prune-unchanged [deps]
  (into {} (keep (fn [[k attrs]]
                   (when (affected? attrs)
                     [k
                      (update attrs :deps
                              #(into #{} (filter (comp affected? deps)) %))])))
        deps))

(defn build-diff-deps [deps1 deps2]
  (let [deps1' (strip-unnecessary-attrs deps1)
        deps2' (strip-unnecessary-attrs deps2)
        diff (e/get-edits (e/diff deps1' deps2'))]
    (-> (merge-diff deps1 deps2 diff)
        mark-affected
        annotate-with-ns-changes
        prune-unchanged)))
