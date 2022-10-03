(ns clj-callgraph.renderer
  (:require [clojure.string :as str]))

(defn- munge* [s]
  (str/replace (str s) #"[^A-Za-z0-9_]" #(format "_%d_" (int (first %)))))

(defn render [deps]
  (println "digraph G {")
  (println "rankdir=\"LR\";")
  (println "fontname=\"monospace\";")
  (println "node[fontname=\"monospace\"];")
  (let [entries-by-ns (group-by (comp :ns val) deps)]
    (doseq [[ns [[_ {:keys [ns-added ns-removed]}]]] entries-by-ns]
      (printf "subgraph cluster_%s {\nshape=\"rect\";\nlabel=\"%s\";\n%s"
              (munge* ns) ns
              (cond ns-added "color=chartreuse2 style=bold;\n"
                    ns-removed "color=crimson style=bold\n"
                    :else ""))
      (doseq [[k attrs] (get entries-by-ns ns)]
        (printf "%s[label=\"%s\" %s];\n" (munge* k) (:name attrs)
                (cond (:added attrs) "color=chartreuse2 style=bold"
                      (:removed attrs) "color=crimson style=\"bold,dashed\""
                      :else "")))
      (println "}"))
    (doseq [[k attrs] deps
            k' (:deps attrs)]
      (printf "%s -> %s %s;\n" (munge* k) (munge* k')
              (let [e (get (:edges attrs) k')]
                (cond (or (:added attrs) (:added e)) "[color=chartreuse2 style=bold]"
                      (or (:removed attrs) (:removed e)) "[color=crimson style=\"bold,dashed\"]"
                      :else ""))))
    (println "}")))
