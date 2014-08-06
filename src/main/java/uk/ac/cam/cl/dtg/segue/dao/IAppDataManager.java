package uk.ac.cam.cl.dtg.segue.dao;

import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.dtg.segue.api.Constants;

/**
 * Interface that provides persistence functionality to external apps that sit
 * on top of Segue.
 * 
 * @author Stephen Cummins
 * 
 * @param <T>
 *            - The type of object that this manager should look after.
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
	 * Find a database record using the map parameter.
	 * 
	 * @param fieldsToMatch
	 *            - a map of boolean operators mapped to lists of field names.
	 * @return a list of results or an empty list.
	 * @throws SegueDatabaseException
	 *             - when a database error has occurred.
	 */
	List<T> find(final Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatch)
		throws SegueDatabaseException;

	/**
	 * Persist an object in the database.
	 * 
	 * @param objectToSave
	 *            - the object that should be persisted.
	 * @return the database unique id of the object saved.
	 * @throws SegueDatabaseException
	 *             - when a database error has occurred.
	 */
	String save(T objectToSave) throws SegueDatabaseException;
}
