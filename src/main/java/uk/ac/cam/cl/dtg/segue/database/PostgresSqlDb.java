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
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import com.google.inject.Inject;

/**
 * PostgresSqlDb adapter.
 *
 */
public class PostgresSqlDb implements Closeable {

	private final PoolProperties p;
	
	private final DataSource dataSource;
	
	/**
	 * Connect to a given database.
	 * @param databaseUrl - the location of the database
	 * @param username - the username to connect with
	 * @param password - the password to use
	 * @throws ClassNotFoundException - if the driver is not available.
	 */
	@Inject
	public PostgresSqlDb(final String databaseUrl, final String username, final String password)
		throws ClassNotFoundException {
	
		p = new PoolProperties();
		// TODO: externalise the config for this.
        p.setUrl(databaseUrl);
        p.setDriverClassName("org.postgresql.Driver");
        p.setUsername(username);
        p.setPassword(password);
        p.setJmxEnabled(true);
        p.setTestWhileIdle(false);
        p.setTestOnBorrow(true);
        p.setValidationQuery("SELECT 1");
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(100);
        p.setInitialSize(10);
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(60);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setMinIdle(10);
        p.setLogAbandoned(true);
        p.setRemoveAbandoned(true);
        p.setJdbcInterceptors(
          "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
          "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
        
        dataSource = new DataSource();
        dataSource.setPoolProperties(p);
	}
	
	/**
	 * Get a handle to the database.
	 * @return database connection.
	 * @throws SQLException 
	 */
	public Connection getDatabaseConnection() throws SQLException {
        return dataSource.getConnection();
	}

	@Override
	public void close() throws IOException {
		this.dataSource.close();
	}
}
