package uk.ac.cam.cl.dtg.segue.dao;

import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import com.google.inject.Inject;
import com.mongodb.DB;

/**
 * Implementation that specifically works with MongoDB Content objects.
 * 
 * @param <T> the type that this App Data Manager looks after.
 */
public class MongoAppDataManager<T> implements IAppDataManager<T> {

	private final DB database;
	private final String databaseName;
	private final Class<T> typeParamaterClass;
	
	/**
	 * Create a new ApplicationDataManager that is responsible for managing a particular type of DO.
	 * 
	 * @param database - Database that will store the objects.
	 * @param typeParameterClass - Class value for the type that this object manages.
	 * @param databaseName - the string name identifying this database / table.
	 */
	@Inject
	public MongoAppDataManager(final DB database, 
			final String databaseName,
			final Class<T> typeParameterClass) {
		this.database = database;
		this.databaseName = databaseName;
		this.typeParamaterClass = typeParameterClass;
	}

	@Override
	public final String save(final T objectToSave) {
		
		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(
				database.getCollection(databaseName),
				typeParamaterClass, String.class);
		
		WriteResult<T, String> r = jc.save(objectToSave);
		
		return r.getSavedId().toString();
	}

	@Override
	public final T getById(final String id) {
		if (null == id) {
			return null;
		}

		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(
				database.getCollection(databaseName), typeParamaterClass,
				String.class);

		T result = jc.findOneById(id);

		return result;
	}

	/**
	 * This method must provide a string response equivalent to the table name or
	 * collection name for the objects to be persisted.
	 * 
	 * It is good practice to prefix the result of this method with something 
	 * unique to the application.
	 * 
	 * @return the databaseName / collection name / internal reference for 
	 * objects of this type.
	 */
	public final String getDatabaseName() {
		return databaseName;
	}
}
