(ns clj-callgraph.graph-test
  (:require [clj-callgraph.graph :as graph]
            [clojure.test :refer [deftest is are]]))

(deftest transpose-graph-test
  (are [input expected]
       (= expected (graph/transpose-graph input))
    '{a {:deps #{}}}
    '{a {:deps #{}}}

    '{a {:deps #{a}}}
    '{a {:deps #{a}}}
    ;;      /-> c ->\
    ;; a -> b       e -> f
    ;;      \-> d ->/
    '{a {:deps #{b}}
      b {:deps #{c d}}
      c {:deps #{e}}
      d {:deps #{e}}
      e {:deps #{f}}
      f {:deps #{}}}
    '{f {:deps #{e}}
      e {:deps #{d c}}
      d {:deps #{b}}
      c {:deps #{b}}
      b {:deps #{a}}
      a {:deps #{}}}
    ;; a --> b
    ;; ^     |
    ;; \- c </
    '{a {:deps #{b}}
      b {:deps #{c}}
      c {:deps #{a}}}
    '{c {:deps #{b}}
      b {:deps #{a}}
      a {:deps #{c}}}))

(deftest topo-sort-test
  (is (= '[a] (graph/topo-sort '{a {:deps #{}}})))
  (let [^java.util.List res (graph/topo-sort {:a {:deps #{:b}}
                                              :b {:deps #{:c :d}}
                                              :c {:deps #{:e}}
                                              :d {:deps #{:e}}
                                              :e {:deps #{:f}}
                                              :f {:deps #{}}})]
    (is (< (.indexOf res :f)
           (.indexOf res :e)
           (.indexOf res :d)
           (.indexOf res :b)
           (.indexOf res :a)))
    (is (< (.indexOf res :e)
           (.indexOf res :c)
           (.indexOf res :b)))))
