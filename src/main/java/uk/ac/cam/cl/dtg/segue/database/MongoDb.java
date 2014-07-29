package uk.ac.cam.cl.dtg.segue.database;

import com.google.inject.Inject;
import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * MongoDB class.
 * 
 * This represents an immutable wrapped client for a MongoDb database.
 */
public class MongoDb {
	private final MongoClient client;
	private final DB db;
	
	/**
	 * Create a mongo db wrapper for a given mongodb database.
	 * 
	 * @param client - Mongo client pre-configured with hostname and port details.
	 * @param databaseName - The database name that this MongoDb instance should focus on.
	 */
	@Inject
	public MongoDb(final MongoClient client, final String databaseName) {
		this.client = client;
		db = this.client.getDB(databaseName);
	}

	/**
	 * Provides a handle to the local MongoDB instance.
	 * 
	 * @return DB handle
	 */
	public DB getDB() {
		return db;
	}
}
