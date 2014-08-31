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
