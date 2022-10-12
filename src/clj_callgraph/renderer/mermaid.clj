(ns clj-callgraph.renderer.mermaid
  (:require [clj-callgraph.namespaces :as ns]
            [clj-callgraph.output :as output]
            [clj-callgraph.protocols :as proto]
            [clj-callgraph.renderer.utils :as utils]))

(defn- munge* [s]
  ;; current mermaid doesn't allow node ids ending with a keyword (e.g. a_graph)
  ;; so modify ids here to always end with `_`
  (str (utils/munge* s) \_))

(defn- make-id-generator []
  (let [id (volatile! -1)]
    (fn [] (vswap! id inc))))

(defn- render-namespaces [output deps]
  (let [entries-by-ns (group-by (comp :ns val) deps)
        gen (make-id-generator)]
    (doseq [ns (ns/topo-sorted-namespaces deps)]
      (when ns
        (output/printf output "subgraph \"%s\"\n" ns))
      (doseq [[k attrs] (utils/sort-by-id (get entries-by-ns ns))
              :let [munged (munge* k)]]
        (output/printf output "%s([\"%s\"])\n" munged (:name attrs))
        (cond (:added (:status attrs))
              (output/printf output "class %s node_added\n" munged)

              (:removed (:status attrs))
              (output/printf output "class %s node_removed\n" munged))
        (doseq [k' (:deps attrs)
                :let [id (gen)
                      e (get (:edges attrs) k')]]
          (output/printf output "%s --> %s\n" munged (munge* k'))
          (cond (or (:added (:status attrs)) (:added e))
                (output/printf output "linkStyle %d stroke:lawngreen,stroke-width:2px\n" id)

                (or (:removed (:status attrs)) (:removed e))
                (output/printf output "linkStyle %d stroke:red,stroke-width:3px,stroke-dasharray:8 8\n" id))))
      (when ns
        (output/println output "end")))))

(defrecord MermaidRenderer [output opts]
  proto/IRenderer
  (render [_ deps]
    (doto output
      (output/println "flowchart LR")
      (output/println "classDef node_added stroke:lawngreen,stroke-width:2px")
      (output/println "classDef node_removed stroke:red,stroke-width:3px,stroke-dasharray:8 8"))
    (render-namespaces output deps)))

(defn make-mermaid-renderer [output opts]
  (->MermaidRenderer output opts))
