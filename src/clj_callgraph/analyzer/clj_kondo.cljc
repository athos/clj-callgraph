(ns clj-callgraph.analyzer.clj-kondo
  (:require [clj-callgraph.protocols :as proto]
            #?@(:bb [[pod.borkdude.clj-kondo :as kondo]]
                :default [[clj-kondo.core :as kondo]])))

(defn- run-kondo [files]
  (let [files #?(:bb (map #(.getPath ^java.io.File %) files)
                 :default files)]
    (kondo/run! {:lint files :config {:analysis {:keywords true}}})))

(defn- analyze* [files]
  (let [res (run-kondo files)
        init (into {} (map (fn [{:keys [ns name]}]
                             (let [ns' (str ns), name' (str name)]
                               [(symbol ns' name')
                                {:ns ns', :name name', :deps #{}}])))
                   (:var-definitions (:analysis res)))]
    (reduce (fn [deps {:keys [from from-var to name]}]
              (if (and from from-var to name)
                (let [from (symbol (str from) (str from-var))
                      to (symbol (str to) (str name))]
                  (cond-> deps
                    (and (contains? deps from) (contains? deps to)
                         (not= from to))
                    (update-in [from :deps] conj to)))
                deps))
            init
            (:var-usages (:analysis res)))))

(defrecord CljKondoAnalyzer [opts]
  proto/IAnalyzer
  (analyze [_ files]
    (analyze* files)))

(defn make-clj-kondo-analyzer [opts]
  (->CljKondoAnalyzer opts))
