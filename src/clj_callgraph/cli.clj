(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]))

(defn dump-deps [opts]
  (api/dump-deps (line-seq *in*) opts))

(defn render-graph [{:keys [dump] :as opts}]
  (api/render-graph dump opts))

(defn render-diff-graph [{:keys [dump1 dump2] :as opts}]
  (api/render-diff-graph dump1 dump2 opts))

(defn var-graph [opts]
  (api/var-graph (line-seq *in*) opts))
