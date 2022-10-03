(ns clj-callgraph.graph
  (:require [clj-callgraph.analysis :as ana]
            [clj-callgraph.diff :as diff]
            [clj-callgraph.renderer :as render]
            [clojure.edn :as edn]))

(defn var-graph [& filenames]
  (render/render (ana/analyze filenames)))

(defn diff-var-graph [file1 file2]
  (let [deps1 (edn/read-string (slurp file1))
        deps2 (edn/read-string (slurp file2)) ]
    (render/render (diff/build-diff-deps deps1 deps2))))

(defn -main [& args]
  (apply diff-var-graph args))
