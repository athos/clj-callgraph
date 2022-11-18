(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [org.corfield.build :as bb]))

(def lib 'dev.athos/clj-callgraph)
(def version (format "0.1.%s" (b/git-count-revs nil)))

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
