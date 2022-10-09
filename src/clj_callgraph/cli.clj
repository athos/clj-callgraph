(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]
            [clojure.java.io :as io]))

(defn- coerce-out [{:keys [out] :as opts}]
  (cond-> opts out (update :out str)))

(defn dump-data [opts]
  (with-open [r (io/reader *in*)]
    (api/dump-data (line-seq r) (coerce-out opts))))

(defn render-graph [{:keys [in] :as opts}]
  (api/render-graph (str in) (coerce-out opts)))

(defn render-diff-graph [{:keys [in1 in2] :as opts}]
  (api/render-diff-graph (str in1) (str in2) (coerce-out opts)))

(defn generate-graph [opts]
  (with-open [r (io/reader *in*)]
    (api/generate-graph (line-seq r) (coerce-out opts))))
