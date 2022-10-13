(ns clj-callgraph.protocols)

(defprotocol IOutput
  (write [this x])
  (close [this]))

(defprotocol IAnalyzer
  (analyze [this input]))

(defprotocol IRenderer
 (render [this data]))
