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
              (case op
                :+ (if ks
                     (-> deps
                         (assoc-in [k :edges (second ks)] {:added true})
                         (update k add-status :changed))
                     (update deps k add-status :added))
                :- (if ks
                     (-> deps
                         (assoc-in [k :edges (second ks)] {:removed true})
                         (update k add-status :changed))
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
                                      more)
                           (update k add-status :changed)))
                     (assert false "Should not be reached here"))))
            deps2 diff)))

(defn- updated? [{:keys [status]}]
  (or (:added status) (:removed status) (:changed status)))

(defn- traced? [{:keys [status] :as attrs}]
  (or (updated? attrs) (:traced status)))

(defn- trace-affected [deps {:keys [ns-bounded-tracing max-tracing-hops]}]
  (let [rev (reversed-deps deps)
        changes (into []
                      (keep (fn [[k {:keys [status]}]]
                              (when (:changed status) k)))
                      deps)
        queue (into (clojure.lang.PersistentQueue/EMPTY)
                    (mapcat (fn [k] (map (partial vector k) (rev k))))
                    changes)]
    (loop [queue queue
           visited #{}
           deps (reduce #(assoc-in %1 [%2 :hops] 0) deps changes)]
      (if (empty? queue)
        deps
        (let [[k k'] (peek queue)
              from (get deps k)
              to (get deps k')]
          (if (or (visited k') (traced? to)
                  (when ns-bounded-tracing
                    (not= (:ns from) (:ns to)))
                  (when max-tracing-hops
                    (>= (:hops from) max-tracing-hops)))
            (recur (pop queue) visited deps)
            (recur (into (pop queue) (map (partial vector k')) (rev k'))
                   (conj visited k')
                   (-> deps
                       (update k' add-status :traced)
                       (assoc-in [k' :hops] (inc (:hops from)))))))))))

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
                   (when (traced? attrs)
                     [k
                      (update attrs :deps
                              #(into #{} (filter (comp traced? deps)) %))])))
        deps))

(defn build-diff-deps [deps1 deps2 opts]
  (let [deps1' (strip-unnecessary-attrs deps1)
        deps2' (strip-unnecessary-attrs deps2)
        diff (e/get-edits (e/diff deps1' deps2'))]
    (-> (merge-diff deps1 deps2 diff)
        (trace-affected opts)
        annotate-with-ns-changes
        prune-unchanged)))
