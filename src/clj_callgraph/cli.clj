(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]
            [clj-callgraph.output :as output]
            [clojure.java.io :as io]))

(defn- prep-out [{:keys [out] :as opts}]
  (assoc opts :out
         (if out
           (output/to-file (str out))
           (output/to-stdout))))

(defn dump-data [opts]
  (with-open [r (io/reader *in*)]
    (api/dump-data (line-seq r) (prep-out opts))))

(defn render-graph [{:keys [in] :as opts}]
  (api/render-graph (str in) (prep-out opts)))

(defn render-ns-graph [{:keys [in] :as opts}]
  (api/render-ns-graph (str in) (prep-out opts)))

(defn render-diff-graph [{:keys [in1 in2] :as opts}]
  (api/render-diff-graph (str in1) (str in2) (prep-out opts)))

(defn generate-graph [opts]
  (with-open [r (io/reader *in*)]
    (api/generate-graph (line-seq r) (prep-out opts))))

(defn generate-ns-graph [opts]
  (with-open [r (io/reader *in*)]
    (api/generate-ns-graph (line-seq r) (prep-out opts))))
