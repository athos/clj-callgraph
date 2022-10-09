(ns clj-callgraph.protocols)

(defprotocol IOutput
  (write [this x])
  (close [this]))
