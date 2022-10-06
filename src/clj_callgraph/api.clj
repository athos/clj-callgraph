(ns clj-callgraph.api
  (:require [clj-callgraph.analysis :as ana]
            [clj-callgraph.diff :as diff]
            [clj-callgraph.renderer :as render]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(defn- with-out [{:keys [out]} f]
  (if out
    (with-open [w (io/writer out)]
      (binding [*out* w]
        (f)))
    (f)))

(defn dump-data
  ([src-files] (dump-data src-files {}))
  ([src-files opts]
   (with-out opts #(prn (ana/analyze src-files)))))

(defn- read-dump-file [dump-file]
  (with-open [r (PushbackReader. (io/reader dump-file))]
    (edn/read r)))

(defn render-graph
  ([dump-file] (render-graph dump-file {}))
  ([dump-file opts]
   (with-out opts #(render/render (read-dump-file dump-file)))))

(defn render-diff-graph
  ([dump-file1 dump-file2]
   (render-diff-graph dump-file1 dump-file2 {}))
  ([dump-file1 dump-file2 opts]
   (let [deps1 (read-dump-file dump-file1)
         deps2 (read-dump-file dump-file2)]
     (with-out opts #(render/render (diff/build-diff-deps deps1 deps2 opts))))))

(defn generate-graph
  ([src-files] (generate-graph src-files {}))
  ([src-files opts]
   (with-out opts #(render/render (ana/analyze src-files)))))
