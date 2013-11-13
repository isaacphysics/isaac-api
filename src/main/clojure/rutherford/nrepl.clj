(ns rutherford.nrepl
  (:use [clojure.tools.nrepl.server :only (start-server stop-server)]))

(defonce server (start-server :port 7888))

(println "nREPL server started on port 7888")
