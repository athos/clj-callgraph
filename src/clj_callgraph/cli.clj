(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]
            [clojure.java.io :as io]))

(defn- coerce-out [{:keys [out] :as opts}]
  (cond-> opts out (update :out str)))

(defn dump-data [opts]
  (with-open [r (io/reader *in*)]
    (api/dump-data (line-seq r) (coerce-out opts))))

(defn render-graph [{:keys [dump] :as opts}]
  (api/render-graph (str dump) (coerce-out opts)))

(defn render-diff-graph [{:keys [dump1 dump2] :as opts}]
  (api/render-diff-graph dump1 dump2 (coerce-out opts)))

(defn generate-graph [opts]
  (with-open [r (io/reader *in*)]
    (api/generate-graph (line-seq r) (coerce-out opts))))
