/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.dao;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.database.MongoDb;

import com.google.inject.Inject;
import com.mongodb.MongoException;

/**
 * Implementation that specifically works with MongoDB Content objects.
 * 
 * @param <T>
 *            the type that this App Data Manager looks after.
 * @deprecated use postgres instead.
 */
@Deprecated
public class MongoAppDataManager<T> implements IAppDatabaseManager<T> {
    private static final Logger log = LoggerFactory.getLogger(MongoAppDataManager.class);

    private final MongoDb database;
    private final String collectionName;
    private final Class<T> typeParamaterClass;

    /**
     * Create a new ApplicationDataManager that is responsible for managing a particular type of DO.
     * 
     * @param database
     *            - Database that will store the objects.
     * @param typeParameterClass
     *            - Class value for the type that this object manages.
     * @param collectionName
     *            - the string name identifying this database / table.
     */
    @Inject
    public MongoAppDataManager(final MongoDb database, final String collectionName, final Class<T> typeParameterClass) {
        this.database = database;
        this.collectionName = collectionName;
        this.typeParamaterClass = typeParameterClass;
    }

    @Override
    public final String save(final String id, final T objectToSave) throws SegueDatabaseException {
        log.warn("This provider does not allow an id to be set in advance of saving. Generating a random one instead.");
        return this.save(objectToSave);
    }

    @Override
    public final String save(final T objectToSave) throws SegueDatabaseException {
        JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getDB().getCollection(collectionName),
                typeParamaterClass, String.class);

        WriteResult<T, String> r;
        try {
            r = jc.save(objectToSave);
        } catch (MongoException e) {
            throw new SegueDatabaseException("Mongo exception during save of " + objectToSave.getClass(), e);
        }

        if (r.getError() != null) {
            log.error("Error detected during database save operation");
            throw new SegueDatabaseException("Mongo exception during save of " + objectToSave.getClass() + ". Error: "
                    + r.getError());
        }

        return r.getSavedId().toString();
    }

    @Override
    public void delete(final String objectId) throws SegueDatabaseException {
        JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getDB().getCollection(collectionName),
                typeParamaterClass, String.class);

        WriteResult<T, String> r;

        try {
            r = jc.removeById(objectId);
        } catch (MongoException e) {
            throw new SegueDatabaseException("Mongo exception during delete of " + objectId, e);
        }

        if (r.getError() != null) {
            log.error("Error detected during database delete operation");
            throw new SegueDatabaseException("Mongo exception during delete of " + objectId + ". Error: "
                    + r.getError());
        }
    }

    @Override
    public final T getById(final String id) throws SegueDatabaseException {
        Validate.notNull(id);
        JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getDB().getCollection(collectionName),
                typeParamaterClass, String.class);

        T result;
        try {
            result = jc.findOneById(id);
        } catch (MongoException e) {
            throw new SegueDatabaseException("Mongo exception during findById for object id: " + id, e);
        }

        return result;
    }

    @Override
    public final T updateField(final String objectId, final String fieldName, final Object value)
            throws SegueDatabaseException {
        Validate.notNull(objectId);
        JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getDB().getCollection(collectionName),
                typeParamaterClass, String.class);

        T result;
        try {
            jc.updateById(objectId, DBUpdate.set(fieldName, value));
            result = this.getById(objectId);
        } catch (MongoException e) {
            throw new SegueDatabaseException("Mongo exception during updateField by object id:" + objectId
                    + " for fieldName " + fieldName, e);
        }

        return result;
    }

    /**
     * This method must provide a string response equivalent to the table name or collection name for the objects to be
     * persisted.
     * 
     * It is good practice to prefix the result of this method with something unique to the application.
     * 
     * @return the databaseName / collection name / internal reference for objects of this type.
     */
    public final String getDatabaseName() {
        return collectionName;
    }

    @Override
    public final List<T> find(final Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch)
            throws SegueDatabaseException {
        Validate.notNull(fieldsToMatch);

        Query query = DBQuery.empty();

        for (Map.Entry<Map.Entry<BooleanOperator, String>, List<String>> pair : fieldsToMatch.entrySet()) {
            // go through the values for each query
            for (String queryValue : pair.getValue()) {
                if (pair.getKey().getKey().equals(BooleanOperator.AND)) {
                    query = query.and(DBQuery.is(pair.getKey().getValue(), queryValue));
                } else {
                    query = query.or(DBQuery.is(pair.getKey().getValue(), queryValue));
                }
            }
        }

        JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getDB().getCollection(collectionName),
                typeParamaterClass, String.class);

        List<T> result;

        try {
            result = jc.find(query).toArray();
        } catch (MongoException e) {
            throw new SegueDatabaseException("Mongo exception during find using query: " + query.toString(), e);
        }

        return result;
    }

    @Override
    public List<T> findAll() throws SegueDatabaseException {
        JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(database.getDB().getCollection(collectionName),
                typeParamaterClass, String.class);
        return jc.find().toArray();
    }
}
