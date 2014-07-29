package uk.ac.cam.cl.dtg.segue.dao;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;

import com.google.inject.Inject;
import com.mongodb.DB;

/**
 * Implementation that specifically works with MongoDB Content objects.
 * 
 * @param <T>
 *            the type that this App Data Manager looks after.
 */
public class MongoAppDataManager<T> implements IAppDataManager<T> {
	private static final Logger log = LoggerFactory
			.getLogger(MongoAppDataManager.class);

	private final DB database;
	private final String collectionName;
	private final Class<T> typeParamaterClass;

	/**
	 * Create a new ApplicationDataManager that is responsible for managing a
	 * particular type of DO.
	 * 
	 * @param database
	 *            - Database that will store the objects.
	 * @param typeParameterClass
	 *            - Class value for the type that this object manages.
	 * @param collectionName
	 *            - the string name identifying this database / table.
	 */
	@Inject
	public MongoAppDataManager(final DB database, final String collectionName,
			final Class<T> typeParameterClass) {
		this.database = database;
		this.collectionName = collectionName;
		this.typeParamaterClass = typeParameterClass;
	}

	@Override
	public final String save(final T objectToSave) {

		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(
				database.getCollection(collectionName), typeParamaterClass,
				String.class);

		WriteResult<T, String> r = jc.save(objectToSave);

		return r.getSavedId().toString();
	}

	@Override
	public final T getById(final String id) {
		Validate.notNull(id);

		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(
				database.getCollection(collectionName), typeParamaterClass,
				String.class);

		T result = jc.findOneById(id);

		return result;
	}

	/**
	 * This method must provide a string response equivalent to the table name
	 * or collection name for the objects to be persisted.
	 * 
	 * It is good practice to prefix the result of this method with something
	 * unique to the application.
	 * 
	 * @return the databaseName / collection name / internal reference for
	 *         objects of this type.
	 */
	public final String getDatabaseName() {
		return collectionName;
	}

	@Override
	public final List<T> find(
			final Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch) {
		Validate.notNull(fieldsToMatch);

		Query query = DBQuery.empty();

		for (Map.Entry<Map.Entry<BooleanOperator, String>, List<String>> pair : fieldsToMatch
				.entrySet()) {
			// go through the values for each query
			for (String queryValue : pair.getValue()) {
				if (pair.getKey().getKey().equals(BooleanOperator.AND)) {
					query = query.and(DBQuery.is(pair.getKey().getValue(),
							queryValue));
				} else {
					query = query.or(DBQuery.is(pair.getKey().getValue(),
							queryValue));
				}
			}
		}

		JacksonDBCollection<T, String> jc = JacksonDBCollection.wrap(
				database.getCollection(collectionName), typeParamaterClass,
				String.class);

		List<T> result = jc.find(query).toArray();

		log.info("Result = " + result.size());
		return result;
	}
}
