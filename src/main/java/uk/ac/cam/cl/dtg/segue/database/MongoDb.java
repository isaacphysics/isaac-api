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

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;

import org.elasticsearch.common.lang3.Validate;

import com.google.inject.Inject;
import com.mongodb.DB;
import com.mongodb.MongoClient;

/**
 * MongoDB class.
 * 
 * This represents an immutable wrapped client for a MongoDb database.
 */
public class MongoDb implements Closeable {
	private final MongoClient client;
	private final String databaseName;
	
	/**
	 * Create a mongo db wrapper for a given mongodb database.
	 * 
	 * @param host
	 *            - database host to connect to.
	 * @param port
	 *            - port that the mongodb service is running on.
	 * @param databaseName - The database name that this MongoDb instance should focus on.
	 * @throws UnknownHostException - if we cannot resolve the host
	 * @throws NumberFormatException - if the port number cannot be
	 */
	@Inject
	public MongoDb(final String host, final Integer port, final String databaseName)
		throws NumberFormatException, UnknownHostException {
		Validate.notBlank(databaseName);
		Validate.notBlank(host);
		Validate.notNull(port);
		
		this.client = new MongoClient(host, port);
		this.databaseName = databaseName;
	}


	/**
	 * Provides a handle to the local MongoDB instance from the connection pool.
	 * 
	 * @return DB handle
	 */
	public DB getDB() {
		return this.client.getDB(databaseName);
	}


	@Override
	public void close() throws IOException {
		client.close();
	}
}
