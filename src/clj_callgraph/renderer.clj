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
    (doseq [ns (keys entries-by-ns)]
      (printf "subgraph cluster_%s {\nshape=\"rect\";\nlabel=\"%s\";\n"
              (munge* ns) ns)
      (doseq [[k {:keys [name]}] (get entries-by-ns ns)]
        (printf "%s[label=\"%s\"];\n" (munge* k) name))
      (println "}"))
    (doseq [[k attrs] deps
            k' (:deps attrs)]
      (printf "%s -> %s;\n" (munge* k) (munge* k')))
    (println "}")))
