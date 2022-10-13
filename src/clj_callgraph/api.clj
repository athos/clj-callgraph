(ns clj-callgraph.api
  (:require [clj-callgraph.analyzer :as ana]
            [clj-callgraph.diff :as diff]
            [clj-callgraph.namespaces :as ns]
            [clj-callgraph.output :as output]
            [clj-callgraph.protocols :as proto]
            [clj-callgraph.renderer :as render]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn analyze [files opts]
  (ana/analyze (ana/make-analyzer opts) files))

(defn dump-data
  ([src-files] (dump-data src-files {}))
  ([src-files {:keys [out] :as opts}]
   (let [deps (analyze src-files opts)]
     (if out
       (try
         (proto/write out (pr-str deps))
         (proto/close out)
         (catch Throwable t
           (proto/close out)
           (throw t)))
       deps))))

(defn- ->dump-data [dump]
  (if (map? dump)
    dump
    (with-open [r (PushbackReader. (io/reader dump))]
      (edn/read r))))

(defn- with-renderer [{:keys [out] :as opts} f]
  (let [output (or out (output/to-string))
        renderer (render/make-renderer output opts)]
    (try
      (f renderer)
      (proto/close output)
      (catch Throwable t
        (proto/close output)
        (throw t)))))

(defn render-graph
  ([dump-data] (render-graph dump-data {}))
  ([dump-data opts]
   (with-renderer opts #(render/render % (->dump-data dump-data)))))

(defn render-ns-graph
  ([dump-data] (render-ns-graph dump-data {}))
  ([dump-data opts]
   (with-renderer opts
     #(render/render % (ns/->ns-graph (->dump-data dump-data))))))

(defn render-diff-graph
  ([dump-data1 dump-data2]
   (render-diff-graph dump-data1 dump-data2 {}))
  ([dump-data1 dump-data2 opts]
   (let [deps1 (->dump-data dump-data1)
         deps2 (->dump-data dump-data2)]
     (with-renderer opts
       #(render/render % (diff/build-diff-deps deps1 deps2 opts))))))

(defn render-ns-diff-graph
  ([dump-data1 dump-data2]
   (render-ns-diff-graph dump-data1 dump-data2 {}))
  ([dump-data1 dump-data2 opts]
   (let [deps1 (->dump-data dump-data1)
         deps2 (->dump-data dump-data2)]
     (with-renderer opts
       (fn [renderer]
         (->> (diff/build-diff-deps (ns/->ns-graph deps1)
                                    (ns/->ns-graph deps2)
                                    opts)
              (render/render renderer)))))))

(defn generate-graph
  ([src-files] (generate-graph src-files {}))
  ([src-files opts]
   (with-renderer opts #(render/render % (analyze src-files opts)))))

(defn generate-ns-graph
  ([src-files] (generate-ns-graph src-files {}))
  ([src-files opts]
   (with-renderer opts
     #(render/render % (ns/->ns-graph (analyze src-files opts))))))

(def to-string output/to-string)
(def to-file output/to-file)
(def to-stdout output/to-stdout)
(def to-writer output/to-writer)
