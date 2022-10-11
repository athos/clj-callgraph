(ns clj-callgraph.cli
  (:require [clj-callgraph.api :as api]
            [clj-callgraph.output :as output]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]))

(defn- prep-in
  ([opts] (prep-in :in opts))
  ([key opts]
   (let [in (get opts key)]
     (if (or (nil? in) (= (str in) "-"))
       *in*
       (str in)))))

(defn- prep-out [{:keys [out] :as opts}]
  (assoc opts :out
         (if (or (nil? out) (= (str out) "-"))
           (output/to-stdout)
           (output/to-file (str out)))))

(defn- ->matcher [x]
  (let [regexes (if (coll? x)
                  (mapv (comp re-pattern str) x)
                  [(re-pattern (str x))])]
    (fn [s]
      (boolean (some #(re-find % s) regexes)))))

(defn- enumerate-files [{:keys [files namespaces dir] :or {dir "src"}}]
  (cond files
        (if (= (str files) "-")
          (with-open [r (io/reader *in*)]
            (doall (map io/file (line-seq r))))
          (map io/file files))

        namespaces
        (for [ns namespaces
              :let [ns' (-> (str ns)
                            (str/replace \- \_)
                            (str/replace \. \/))]
              ext [".clj" ".cljc" ".cljs"]
              :let [res (io/resource (str ns' ext))]
              :when res]
          (io/file res))

        :else
        (->> (file-seq (io/file (str dir)))
             (filter (fn [^File file]
                       (and (.isFile file)
                            (re-matches #".*\.clj[cs]?$"
                                        (.getName file))))))))

(defn- collect-files [{:keys [include-regex exclude-regex] :as opts}]
  (let [include? (if include-regex
                   (->matcher include-regex)
                   (constantly true))
        exclude? (if exclude-regex
                   (->matcher exclude-regex)
                   (constantly false))]
    (->> (enumerate-files opts)
         (filter (fn [^File file]
                   (let [path (.getPath file)]
                     (and (include? path) (not (exclude? path)))))))))

(defn dump-data [opts]
  (api/dump-data (collect-files opts) (prep-out opts)))

(defn render-graph [opts]
  (api/render-graph (prep-in opts) (prep-out opts)))

(defn render-ns-graph [opts]
  (api/render-ns-graph (prep-in opts) (prep-out opts)))

(defn render-diff-graph [opts]
  (api/render-diff-graph (prep-in :in1 opts)
                         (prep-in :in2 opts)
                         (prep-out opts)))

(defn generate-graph [opts]
  (api/generate-graph (collect-files opts) (prep-out opts)))

(defn generate-ns-graph [opts]
  (api/generate-ns-graph (collect-files opts) (prep-out opts)))
