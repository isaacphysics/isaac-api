(ns rutherford.datomic.logger
  (:require [datomic.api :as d :refer [db q]]
            [rutherford.datomic :as rd]
            [clojure.data.json :as json])
  (:import [uk.ac.cam.cl.dtg.clojure DatomicLogger])
  (:use [clojure.pprint]
        [rutherford.interop]))


(defconstruct DatomicLogger [this]
  (println "Constructing DatomicLogger."))

(defimpl DatomicLogger
  (toString [this] (str "This is a DatomicLogger defined in Clojure"))
  
  (logEvent [this session-id event-json]
     (if @rd/conn
        
	     (let [event (json/read-str event-json :key-fn keyword)]
		     (println "Logging event from session" session-id)
	      
	       ;; Look up which keys from the json have matching attributes in the logging.event ns
	       (def logging-attributes
					 (map #(name (first %)) (q '[:find ?i :where [?e :db/ident ?i]
					                                             [_ :db.install/attribute ?e]
					                                             [(namespace ?i) ?n]
					                                             [(= ?n "logging.event")]] (db @rd/conn)))) 
	       (def matches (filter #(some #{(name %)} logging-attributes) 
	                            (keys event)))
	       
	       (def values (for [m matches] {(keyword "logging.event" (name m)) (m event)}))
	
	       ;; Add the event to the database
		    @(d/transact @rd/conn [{:db/id #db/id[:db.part/user -1]
		                            :logging/sessionId session-id}
	                         
	                             (apply merge {:db/id #db/id[:db.part/user]
	                                           :logging.event/session #db/id[:db.part/user -1]}
	                                          values)])
         true)
      
       ;; We only get here if @conn is nil
       (do 
         (println "DB not available for log message storage")
         false))))

