(ns clj-callgraph.renderer.utils
  (:require [clojure.string :as str]))

(defn munge* [s]
  (str/replace (str s) #"[^A-Za-z0-9_]" #(format "_%d_" (int (first %)))))

(defn- safe-id-comparator [[_ {x :id}] [_ {y :id}]]
  (if (nil? x)
    (if (nil? y) 0 -1)
    (if (nil? y) 1 (compare x y))))

(defn sort-by-id [coll]
  (sort safe-id-comparator coll))
