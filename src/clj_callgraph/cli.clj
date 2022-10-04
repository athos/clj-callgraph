(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]
            [clojure.java.io :as io]))

(defn dump-data [opts]
  (with-open [r (io/reader *in*)]
    (api/dump-data (line-seq r) opts)))

(defn render-graph [{:keys [dump] :as opts}]
  (api/render-graph dump opts))

(defn render-diff-graph [{:keys [dump1 dump2] :as opts}]
  (api/render-diff-graph dump1 dump2 opts))

(defn generate-graph [opts]
  (with-open [r (io/reader *in*)]
    (api/generate-graph (line-seq r) opts)))
