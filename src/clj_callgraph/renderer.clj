(ns clj-callgraph.renderer
  (:refer-clojure :exclude [printf println])
  (:require [clj-callgraph.output :as output]
            [clojure.string :as str]))

(defn- munge* [s]
  (str/replace (str s) #"[^A-Za-z0-9_]" #(format "_%d_" (int (first %)))))

(defn- safe-id-comparator [[_ {x :id}] [_ {y :id}]]
  (if (nil? x)
    (if (nil? y) 0 -1)
    (if (nil? y) 1 (compare x y))))

(def ^:dynamic *output*)

(defn- println [x]
  (output/println *output* x))

(defn- printf [fmt & args]
  (apply output/printf *output* fmt args))

(defn render [output deps]
  (binding [*output* output]
    (println "digraph G {")
    (println "rankdir=\"LR\";")
    (println "fontname=\"monospace\";")
    (println "node[fontname=\"monospace\",ordering=\"in\"];")
    (let [entries-by-ns (group-by (comp :ns val) deps)]
      (doseq [[ns [[_ {:keys [ns-added ns-removed]}]]] entries-by-ns]
        (printf "subgraph cluster_%s {\nshape=\"rect\";\nlabel=\"%s\";\n%s"
                (munge* ns) ns
                (cond ns-added "color=chartreuse2 style=bold;\n"
                      ns-removed "color=crimson style=bold\n"
                      :else ""))
        (doseq [[k attrs] (->> (get entries-by-ns ns)
                               (sort safe-id-comparator))]
          (printf "%s[label=\"%s\" %s];\n" (munge* k) (:name attrs)
                  (cond (:added (:status attrs))
                        "color=chartreuse2 style=bold"

                        (:removed (:status attrs))
                        "color=crimson style=\"bold,dashed\""

                        :else "")))
        (println "}"))
      (doseq [[_ entries] entries-by-ns
              [k attrs] (sort entries)
              k' (:deps attrs)]
        (printf "%s -> %s %s;\n" (munge* k) (munge* k')
                (let [e (get (:edges attrs) k')]
                  (cond (or (:added (:status attrs)) (:added e))
                        "[color=chartreuse2 style=bold]"

                        (or (:removed (:status attrs)) (:removed e))
                        "[color=crimson style=\"bold,dashed\"]"

                        :else ""))))
      (println "}"))))
