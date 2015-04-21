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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import com.google.inject.Inject;

/**
 * PostgresSqlDb adapter.
 *
 */
public class PostgresSqlDb {
	private final Connection database;
	
	/**
	 * Connect to a given database.
	 * @param databaseUrl - the location of the database
	 * @param username - the username to connect with
	 * @param password - the password to use
	 * @throws SQLException - if we cannot create the connection
	 * @throws ClassNotFoundException - if the driver is not available.
	 */
	@Inject
	public PostgresSqlDb(final String databaseUrl, final String username, final String password)
		throws SQLException, ClassNotFoundException {
		database = this.connect(databaseUrl, username, password);
	}
	
	/**
	 * Get a handle to the database.
	 * @return database connection.
	 */
	public Connection getDatabaseConnection() {
		return database;
	}
	
	/**
	 * Connect to a given database.
	 * @param url - the location of the database
	 * @param username - the username to connect with
	 * @param password - the password to use
	 * @return a connection object
	 * @throws SQLException - if we cannot create the connection
	 * @throws ClassNotFoundException - if the driver is not available.
	 */
	private Connection connect(final String url, final String username, final String password)
		throws SQLException, ClassNotFoundException {
		Properties props = new Properties();
		props.setProperty("user", username);
		props.setProperty("password", password);
		
		Class.forName("org.postgresql.Driver");
		
		return DriverManager.getConnection(url, props);
	}
}
