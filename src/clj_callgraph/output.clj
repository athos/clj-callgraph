(ns clj-callgraph.output
  (:refer-clojure :exclude [printf println])
  (:require [clj-callgraph.protocols :as proto]
            [clojure.java.io :as io])
  (:import [java.io BufferedWriter Writer]))

(defrecord StringOutput [^StringBuilder sb]
  proto/IOutput
  (write [_ x]
    (.append sb x))
  (close [_]
    (.toString sb)))

(defn to-string []
  (->StringOutput (StringBuilder.)))

(defrecord WriterOutput [^Writer writer]
  proto/IOutput
  (write [_ x]
    (.write writer ^String x))
  (close [_]
    (.flush writer)))

(defn to-writer [w]
  (->WriterOutput w))

(defn to-stdout []
  (to-writer *out*))

(defrecord FileOutput [^BufferedWriter writer]
  proto/IOutput
  (write [_ x]
    (.write writer ^String x))
  (close [_]
    (.close writer)))

(defn to-file [file]
  (->FileOutput (io/writer file)))

(defn println [output s]
  (proto/write output s)
  (proto/write output "\n"))

(defn printf [output fmt & args]
  (proto/write output (apply format fmt args)))
