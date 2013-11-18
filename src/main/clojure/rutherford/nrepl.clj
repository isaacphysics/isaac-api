(ns rutherford.nrepl
  (:use [clojure.tools.nrepl.server :only (start-server stop-server)])
  (:import [java.net BindException]))

(defonce server 
  (let [port 7888]
    (try 
      (start-server :port port)
      (catch BindException e (println "Could not start nREPL server - port" port "already in use."))
      (catch Exception e (println "Could not start nREPL server.")))))

(println "nREPL server started on port 7888")
