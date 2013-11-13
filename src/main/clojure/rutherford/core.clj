(ns rutherford.core
  (:require [datomic.api :as d :refer [db q]]
            [clojure.data.json :as json])
  (:import [uk.ac.cam.cl.dtg.teaching DatomicLogger])
  (:use [clojure.pprint]))

(defmulti generate identity)

(defmacro defimpl [base & body]
  `(defmethod generate ~base [_#]
     (reify ~base
       ~@body)))

(defonce uri "datomic:free://localhost:4334/rutherford")
(defonce conn (d/connect uri))

(defimpl DatomicLogger
  (toString [this] (str "This is a DatomicLogger defined in Clojure"))
  
  (logEvent [this session-id event-json]
     (let [event (json/read-str event-json :key-fn keyword)]
	     (println "Logging event from session" session-id)
      
       ;; Look up which keys from the json have matching attributes in the logging.event ns
       (def logging-attributes 
         (map #(name (first %)) (q '[:find ?i :where [?e :db/ident ?i]
                                                     [_ :db.install/attribute ?e]
                                                     [(namespace ?i) ?n]
                                                     [(= ?n "logging.event")]] (db conn))))
       
       
       (def matches (filter #(some #{(name %)} logging-attributes) 
                            (keys event)))
       
       (def values (for [m matches] {(keyword "logging.event" (name m)) (m event)}))

       ;; Add the event to the database
	     @(d/transact conn [{:db/id #db/id[:db.part/user -1]
	                         :logging/sessionId session-id}
                         
                          (apply merge {:db/id #db/id[:db.part/user]
                                        :logging.event/session #db/id[:db.part/user -1]}
                                       values)]))))
