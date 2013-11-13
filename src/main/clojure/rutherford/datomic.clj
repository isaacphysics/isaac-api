(ns rutherford.datomic
  (:require [clojure.java.io]
            [datomic.api :as d :refer [db q]])
  (:use [clojure.pprint]))

(defonce uri "datomic:free://localhost:4334/rutherford")
(defonce conn (atom nil))
(defn connect [] (reset! conn (d/connect uri)))

(defn update-schema []
  (let [schema (read-string (slurp (clojure.java.io/resource "datomic_schema.dtm")))]
    @(d/transact @conn schema)))

(defn create-db [] 
  (d/delete-database uri)
  (d/create-database uri)
  (connect)
  (update-schema))

(connect)


