(ns rutherford.datomic.interest-registration
  (:require [datomic.api :as d :refer [db q]]
            [rutherford.datomic :as rd]
            [clojure.data.json :as json])
  (:import [uk.ac.cam.cl.dtg.clojure InterestRegistration])
  (:use [clojure.pprint]
        [rutherford.interop]))

(defconstruct InterestRegistration [this]
  (println "Constructing InterestRegistration."))

(defimpl InterestRegistration
  (toString [this] (str "This is an InterestRegistration object defined in Clojure"))
  
  (register [this name email role school year feedback]
     (if @rd/conn
        
       (do
         (println "Logging registration from" email)
       
         ;; Add the event to the database
        @(d/transact @rd/conn [{:db/id #db/id[:db.part/user]
                                :registration/name name
                                :registration/email email
                                :registration/role role
                                :registration/school school
                                :registration/year year
                                :registration/feedback feedback}])
        true)
       
       ;; We only get here if @conn is nil
       (do 
         (println "DB not available for registration")
         false))))

