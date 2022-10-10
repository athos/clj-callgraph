(ns clj-callgraph.renderer
  (:require [clj-callgraph.protocols :as proto]
            [clj-callgraph.renderer.dot :as dot]
            [clj-callgraph.renderer.mermaid :as mermaid]))

(defmulti make-renderer (fn [_output opts] (keyword (:format opts))))

(defmethod make-renderer :default [output opts]
  (if-let [fmt (:format opts)]
    (throw (ex-info (str "Unknown format specified: " (name fmt))
                    {:format fmt}))
    (make-renderer output (assoc opts :format :dot))))

(defmethod make-renderer :dot [output opts]
  (dot/make-dot-renderer output opts))

(defmethod make-renderer :mermaid [output opts]
  (mermaid/make-mermaid-renderer output opts))

(defn render [renderer deps]
  (proto/render renderer deps))
