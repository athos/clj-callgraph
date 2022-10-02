(ns clj-callgraph.analysis
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.tools.analyzer.jvm :as ana]
            [clojure.tools.analyzer.ast :as ast]))

(defn- analyze-form [env form]
  (let [tree (ana/analyze+eval form env)]
    (->> (for [{:keys [op var] :as def} (ast/nodes tree)
               :when (= :def op)
               :let [sym (symbol var)
                     deps (into #{} (keep #(some-> (:var %) symbol))
                                (ast/nodes def))]]
           [sym (disj deps sym)])
         (into {}))))

(defn analyze-file [filename]
  (with-open [r (java.io.PushbackReader. (io/reader filename))]
    (let [[ns-form & forms] (->> (repeatedly #(read r false ::empty))
                                 (take-while #(not= % ::empty)))]
      (cond (nil? ns-form) {}

            (not (and (seq? ns-form)
                      (= 'ns (first ns-form))
                      (symbol? (second ns-form))))
            (throw (ex-info "Unexpected ns form found" {:form ns-form}))

            :else
            (let [ns-sym (second ns-form)
                  env (assoc (ana/empty-env) :ns ns-sym)]
              (eval ns-form)
              (reduce #(merge %1 (analyze-form env %2)) {} forms))))))

(defn- remove-external-syms [deps]
  (let [toplevel-syms (-> deps keys set)]
    (into {} (map (fn [[sym deps]]
                    [sym (set/intersection toplevel-syms deps)]))
          deps)))

(defn analyze [filenames]
  (->> (map analyze-file filenames)
       (apply merge)
       remove-external-syms))
