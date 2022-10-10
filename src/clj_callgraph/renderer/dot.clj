(ns clj-callgraph.renderer.dot
  (:refer-clojure :exclude [printf println])
  (:require [clj-callgraph.output :as output]
            [clj-callgraph.protocols :as proto]
            [clj-callgraph.renderer.utils :as utils]))

(defn- render-node [output k attrs]
  (output/printf output
                 "%s[label=\"%s\" %s];\n" (utils/munge* k) (:name attrs)
                 (cond (:added (:status attrs))
                       "color=chartreuse2 style=bold"

                       (:removed (:status attrs))
                       "color=crimson style=\"bold,dashed\""

                       :else "")))

(defn- render-namespace
  [output ns-name [[_ {:keys [ns-added ns-removed]}] :as entries]]
  (when ns-name
    (output/printf output
                   "subgraph cluster_%s {\nshape=\"rect\";\nlabel=\"%s\";\n%s"
                   (utils/munge* ns-name) ns-name
                   (cond ns-added "color=chartreuse2 style=bold;\n"
                         ns-removed "color=crimson style=bold\n"
                         :else "")))
  (doseq [[k attrs] (utils/sort-by-id entries)]
    (render-node output k attrs))
  (when ns-name
    (output/println output "}")))

(defn- render-namespaces [output entries-by-ns]
  (doseq [[ns entries] entries-by-ns]
    (render-namespace output ns entries)))

(defn- render-arrows [output entries-by-ns]
  (doseq [[_ entries] entries-by-ns
          [k attrs] (sort entries)
          k' (:deps attrs)]
    (output/printf output
                   "%s -> %s %s;\n" (utils/munge* k) (utils/munge* k')
                   (let [e (get (:edges attrs) k')]
                     (cond (or (:added (:status attrs)) (:added e))
                           "[color=chartreuse2 style=bold]"

                           (or (:removed (:status attrs)) (:removed e))
                           "[color=crimson style=\"bold,dashed\"]"

                           :else "")))))

(defrecord DotRenderer [output opts]
  proto/IRenderer
  (render [_ deps]
    (doto output
      (output/println "digraph G {")
      (output/println "rankdir=\"LR\";")
      (output/println "fontname=\"monospace\";")
      (output/println "node[fontname=\"monospace\",ordering=\"in\"];"))
    (let [entries-by-ns (group-by (comp :ns val) deps)]
      (render-namespaces output entries-by-ns)
      (render-arrows output entries-by-ns))
    (output/println output "}")))

(defn make-dot-renderer [output opts]
  (->DotRenderer output opts))
