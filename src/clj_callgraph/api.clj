(ns clj-callgraph.api
  (:require [clj-callgraph.analysis :as ana]
            [clj-callgraph.diff :as diff]
            [clj-callgraph.namespaces :as ns]
            [clj-callgraph.output :as output]
            [clj-callgraph.protocols :as proto]
            [clj-callgraph.renderer :as render]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn dump-data
  ([src-files] (dump-data src-files {}))
  ([src-files {:keys [out]}]
   (let [deps (ana/analyze src-files)]
     (if out
       (try
         (proto/write out (pr-str deps))
         (proto/close out)
         (catch Throwable t
           (proto/close out)
           (throw t)))
       deps))))

(defn- read-dump-file [dump-file]
  (with-open [r (PushbackReader. (io/reader dump-file))]
    (edn/read r)))

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
  ([dump-file] (render-graph dump-file {}))
  ([dump-file opts]
   (with-renderer opts #(render/render % (read-dump-file dump-file)))))

(defn render-ns-graph
  ([dump-file] (render-ns-graph dump-file {}))
  ([dump-file opts]
   (with-renderer opts
     #(render/render % (ns/->ns-graph (read-dump-file dump-file))))))

(defn render-diff-graph
  ([dump-file1 dump-file2]
   (render-diff-graph dump-file1 dump-file2 {}))
  ([dump-file1 dump-file2 opts]
   (let [deps1 (read-dump-file dump-file1)
         deps2 (read-dump-file dump-file2)]
     (with-renderer opts
       #(render/render % (diff/build-diff-deps deps1 deps2 opts))))))

(defn generate-graph
  ([src-files] (generate-graph src-files {}))
  ([src-files opts]
   (with-renderer opts #(render/render % (ana/analyze src-files)))))

(defn generate-ns-graph
  ([src-files] (generate-ns-graph src-files {}))
  ([src-files opts]
   (with-renderer opts
     #(render/render % (ns/->ns-graph (ana/analyze src-files))))))

(def to-string output/to-string)
(def to-file output/to-file)
(def to-stdout output/to-stdout)
(def to-writer output/to-writer)
