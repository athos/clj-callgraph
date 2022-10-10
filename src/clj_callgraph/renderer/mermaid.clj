(ns clj-callgraph.renderer.mermaid
  (:require [clj-callgraph.namespaces :as ns]
            [clj-callgraph.output :as output]
            [clj-callgraph.protocols :as proto]
            [clj-callgraph.renderer.utils :as utils]))

(defn- render-namespaces [output deps]
  (let [entries-by-ns (group-by (comp :ns val) deps)]
    (doseq [ns (ns/topo-sorted-namespaces deps)]
      (when ns
        (output/printf output "subgraph \"%s\"\n" ns))
      (doseq [[k attrs] (utils/sort-by-id (get entries-by-ns ns))]
        (output/printf output "%s([\"%s\"])\n" (utils/munge* k) (:name attrs))
        (doseq [k' (:deps attrs)]
          (output/printf output "%s --> %s\n"
                         (utils/munge* k)
                         (utils/munge* k'))))
      (when ns
        (output/println output "end")))))

(defrecord MermaidRenderer [output opts]
  proto/IRenderer
  (render [_ deps]
    (output/println output "flowchart LR")
    (render-namespaces output deps)))

(defn make-mermaid-renderer [output opts]
  (->MermaidRenderer output opts))
