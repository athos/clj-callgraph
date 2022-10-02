(ns clj-callgraph.graph
  (:require [clj-callgraph.analysis :as ana]
            [clj-callgraph.renderer :as render]))

(defn var-graph [& filenames]
  (render/render (ana/analyze filenames)))

(defn -main [& args]
  (apply var-graph args))
