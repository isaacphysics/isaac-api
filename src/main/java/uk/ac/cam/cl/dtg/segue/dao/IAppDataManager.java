package uk.ac.cam.cl.dtg.segue.dao;

/**
 * Interface that provides persistence functionality to external apps
 * that sit on top of Segue.
 *
 * @author Stephen Cummins
 *
 * @param <T> - The type of object that this manager should look after.
 */
public interface IAppDataManager<T> {

	/**
	 * Find an object by id.
	 * 
	 * @param id - id to search for
	 * @return the object associated with the given id or null if not found.
	 */
	T getById(String id);
	
	/**
	 * Persist an object in the database.
	 * 
	 * @param objectToSave - the object that should be persisted.
	 * @return the database unique id of the object saved.
	 */
	String save(T objectToSave);
}
