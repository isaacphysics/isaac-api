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

/**
 * An interface for managing application data.
 * 
 * @author Stephen Cummins
 *
 * @param <T> the type of data that this Manager can retrieve and save.
 */
public interface IAppDataManager<T> {
	/**
	 * Find an object by id.
	 * 
	 * @param id
	 *            - id to search for
	 * @return the object associated with the given id or null if not found.
	 * @throws SegueDatabaseException
	 *             - when a database error has occurred.
	 */
	T getById(String id) throws SegueDatabaseException;
	
	/**
	 * Persist an object in the database.
	 * 
	 * @param preferredId
	 *            - Allows you to express a preference of what ID the object
	 *            should be saved under. This can be ignored. The return result is the id that was used.
	 * @param objectToSave
	 *            - the object that should be persisted.
	 * @return the database unique id of the object saved.
	 * @throws SegueDatabaseException
	 *             - when a database error has occurred.
	 */
	String save(String preferredId, T objectToSave) throws SegueDatabaseException;
	
	/**
	 * Delete a given object from the database.
	 * 
	 * The object should contain an _id field which is used for equality checking.
	 * 
	 * @param objectId - the id of the object to remove from the database.
	 * @throws SegueDatabaseException - if there is a problem removing the item from the database.
	 */
	void delete(String objectId) throws SegueDatabaseException;
}
