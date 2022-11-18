(ns build
  (:refer-clojure :exclude [test])
  (:require [org.corfield.build :as bb]))

(def lib 'dev.athos/clj-callgraph)
(def version "0.1.0-SNAPSHOT")

(defn clean [opts]
  (bb/clean opts))

(defn test "Run the tests." [opts]
  (bb/run-tests opts))

(defn jar [opts]
  (-> opts
      (assoc :src-pom "template/pom.xml"
             :lib lib :version version)
      (clean)
      (bb/jar)))

(defn install "Install the JAR locally." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/install)))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (-> opts
      (assoc :lib lib :version version)
      (bb/deploy)))
