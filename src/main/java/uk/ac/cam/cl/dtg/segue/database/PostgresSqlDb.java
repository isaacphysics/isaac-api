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

import org.apache.commons.dbcp.BasicDataSource;

import com.google.inject.Inject;

/**
 * PostgresSqlDb adapter.
 *
 */
public class PostgresSqlDb implements Closeable {

    private final BasicDataSource dataSource;

    /**
     * Connect to a given database.
     * 
     * @param databaseUrl
     *            - the location of the database
     * @param username
     *            - the username to connect with
     * @param password
     *            - the password to use
     * @throws ClassNotFoundException
     *             - if the driver is not available.
     */
    @Inject
    public PostgresSqlDb(final String databaseUrl, final String username, final String password)
            throws ClassNotFoundException {

        dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(databaseUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setTestWhileIdle(false);
        dataSource.setTestOnBorrow(true);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestOnReturn(false);
        dataSource.setTimeBetweenEvictionRunsMillis(30000);
        dataSource.setMaxActive(100);
        dataSource.setInitialSize(10);
        dataSource.setMaxWait(10000);
        dataSource.setRemoveAbandonedTimeout(60);
        dataSource.setMinEvictableIdleTimeMillis(30000);
        dataSource.setMinIdle(10);
        dataSource.setLogAbandoned(true);
        dataSource.setRemoveAbandoned(true);
    }

    /**
     * Get a handle to the database.
     * 
     * @return database connection.
     * @throws SQLException
     */
    public Connection getDatabaseConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() throws IOException {

        try {
            this.dataSource.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
