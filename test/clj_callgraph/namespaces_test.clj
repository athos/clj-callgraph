(ns clj-callgraph.namespaces-test
  (:require [clj-callgraph.namespaces :as ns]
            [clojure.test :refer [deftest is]]))

(deftest ->ns-graph-test
  (is (= {"foo" {:name "foo" :deps #{}}}
         (ns/->ns-graph '{foo/a {:name "a" :ns "foo" :deps #{foo/b}}
                          foo/b {:name "b" :ns "foo" :deps #{}}})))
  (is (= {"foo" {:name "foo" :deps #{"bar"}}
          "bar" {:name "bar" :deps #{}}}
         (ns/->ns-graph '{foo/a {:name "a" :ns "foo" :deps #{foo/b bar/c}}
                          foo/b {:name "b" :ns "foo" :deps #{bar/c}}
                          bar/c {:name "c" :ns "bar" :deps #{}}})))
  (is (= {"foo" {:name "foo" :deps #{"bar"}}
          "bar" {:name "bar" :deps #{"baz"}}
          "baz" {:name "baz" :deps #{"quux"}}
          "quux" {:name "quux" :deps #{}}}
         (ns/->ns-graph '{foo/a {:name "a" :ns "foo" :deps #{bar/b}}
                          bar/b {:name "b" :ns "bar" :deps #{bar/c baz/d}}
                          bar/c {:name "c" :ns "bar" :deps #{baz/e}}
                          baz/d {:name "d" :ns "baz" :deps #{baz/e}}
                          baz/e {:name "e" :ns "baz" :deps #{quux/f}}
                          quux/f {:name "f" :ns "quux" :deps #{}}}))))

(deftest topo-sorted-namespaces-test
  (let [^java.util.List res
        (ns/topo-sorted-namespaces
         '{foo/a {:name "a" :ns "foo" :deps #{bar/b baz/c}}
           bar/b {:name "b" :ns "bar" :deps #{bar/d}}
           bar/d {:name "d" :ns "bar" :deps #{quux/f}}
           baz/c {:name "c" :ns "baz" :deps #{baz/e}}
           baz/e {:name "e" :ns "baz" :deps #{quux/f}}
           quux/f {:name "f" :ns "quux" :deps #{}}})]
    (is (< (.indexOf res "quux")
           (.indexOf res "bar")
           (.indexOf res "foo")))
    (is (< (.indexOf res "quux")
           (.indexOf res "baz")
           (.indexOf res "foo")))))
