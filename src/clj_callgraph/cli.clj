(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]
            [clj-callgraph.output :as output]
            [clojure.java.io :as io])
  (:import [java.io File]))

(defn- prep-out [{:keys [out] :as opts}]
  (assoc opts :out
         (if out
           (output/to-file (str out))
           (output/to-stdout))))

(defn- ->matcher [x]
  (let [regexes (if (coll? x)
                  (mapv (comp re-pattern str) x)
                  [(re-pattern (str x))])]
    (fn [s]
      (boolean (some #(re-find % s) regexes)))))

(defn- collect-files [{:keys [files dir include-regex exclude-regex]}]
  (let [include? (if include-regex
                   (->matcher include-regex)
                   (constantly true))
        exclude? (if exclude-regex
                   (->matcher exclude-regex)
                   (constantly false))]
    (->> (cond files (map io/file files)
               dir (->> (file-seq (io/file (str dir)))
                        (filter (fn [^File file]
                                  (and (.isFile file)
                                       (re-matches #".*\.clj[cs]?$"
                                                   (.getName file))))))
               :else (with-open [r (io/reader *in*)]
                       (doall (map io/file (line-seq r)))))
         (filter (fn [^File file]
                   (let [path (.getPath file)]
                     (and (include? path) (not (exclude? path)))))))))

(defn dump-data [opts]
  (api/dump-data (collect-files opts) (prep-out opts)))

(defn render-graph [{:keys [in] :as opts}]
  (api/render-graph (str in) (prep-out opts)))

(defn render-ns-graph [{:keys [in] :as opts}]
  (api/render-ns-graph (str in) (prep-out opts)))

(defn render-diff-graph [{:keys [in1 in2] :as opts}]
  (api/render-diff-graph (str in1) (str in2) (prep-out opts)))

(defn generate-graph [opts]
  (api/generate-graph (collect-files opts) (prep-out opts)))

(defn generate-ns-graph [opts]
  (api/generate-ns-graph (collect-files opts) (prep-out opts)))
