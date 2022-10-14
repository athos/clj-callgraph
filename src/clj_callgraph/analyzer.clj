(ns clj-callgraph.analyzer
  (:require [clj-callgraph.protocols :as proto]))

(defmulti make-analyzer (fn [opts] (:analyzer opts)))

(defmethod make-analyzer :default [opts]
  (if-let [analyzer (:analyzer opts)]
    (throw (ex-info (str "Unknown analyzer specified: " (name analyzer))
                    {:analyzer analyzer}))
    (make-analyzer (assoc opts :analyzer :tools-analyzer))))

(defmethod make-analyzer :tools-analyzer [opts]
  (require 'clj-callgraph.analyzer.tools-analyzer)
  ((resolve 'clj-callgraph.analyzer.tools-analyzer/make-tools-analyzer) opts))

(defmethod make-analyzer :clj-kondo [opts]
  (require 'clj-callgraph.analyzer.clj-kondo)
  ((resolve 'clj-callgraph.analyzer.clj-kondo/make-clj-kondo-analyzer) opts))

(defn analyze [analyzer input]
  (proto/analyze analyzer input))
