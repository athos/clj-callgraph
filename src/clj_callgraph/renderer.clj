(ns clj-callgraph.renderer
  (:refer-clojure :exclude [munge])
  (:require [clojure.string :as str]))

(defn- munge [s]
  (str/replace s #"[^A-Za-z0-9_]" #(format "_%d_" (int (first %)))))

(defn render [deps]
  (let [id #(munge (str (namespace %) \. (name %)))]
    (println "digraph G {")
    (println "rankdir=\"LR\";")
    (println "fontname=\"monospace\";")
    (println "node[fontname=\"monospace\"];")
    (doseq [ns (into #{} (map namespace) (keys deps))]
      (printf "subgraph cluster_%s {\nshape=\"rect\";\nlabel=\"%s\";\n"
              (munge ns) ns)
      (doseq [sym (filter (comp #{ns} namespace) (keys deps))]
        (printf "%s[label=\"%s\"];\n" (id sym) (name sym)))
      (println "}"))
    (doseq [[sym deps] deps
            sym' deps]
      (printf "%s -> %s;\n" (id sym) (id sym')))
    (println "}")))
