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

import uk.ac.cam.cl.dtg.segue.api.Constants;

/**
 * Interface that provides persistence functionality to external apps that sit on top of Segue.
 * 
 * @author Stephen Cummins
 * 
 * @param <T>
 *            - The type of object that this manager should look after.
 */
public interface IAppDatabaseManager<T> extends IAppDataManager<T> {
    /**
     * Persist an object in the database.
     * 
     * @param objectToSave
     *            - the object that should be persisted.
     * @return the database unique id of the object saved.
     */
    String save(T objectToSave);

    /**
     * Find a database record using the map parameter.
     * 
     * @param fieldsToMatch
     *            - a map of boolean operators mapped to lists of field names.
     * @return a list of results or an empty list.
     */
    List<T> find(final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch)
    ;

    /**
     * findAll records.
     * 
     * @return returns a list of all records held by this data manager.
     */
    List<T> findAll();

    /**
     * Update a field in a given object by Id.
     * 
     * @param objectId
     *            - the object id to search for.
     * @param fieldName
     *            - within the object to update.
     * @param value
     *            - to use as the updated field.
     * @return The full object with updates.
     */
    T updateField(String objectId, String fieldName, Object value);
}
