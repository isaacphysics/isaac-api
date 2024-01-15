/**
 * Copyright 2014 Stephen Cummins
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.database;

import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_EVICTION_RUN_PERIOD_MILLISECONDS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_INITIAL_SIZE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_MAX_TOTAL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_MAX_WAIT_MILLISECONDS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_MIN_EVICTABLE_IDLE_TIMEOUT_MILLISECONDS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_MIN_MIN_IDLE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONNECTION_POOL_REMOVE_ABANDONED_TIMEOUT;

import com.google.inject.Inject;
import java.io.Closeable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PostgresSqlDb adapter.
 */
public class PostgresSqlDb implements Closeable {
  private static final Logger log = LoggerFactory.getLogger(PostgresSqlDb.class);

  private final BasicDataSource dataSource;

  /**
   * Connect to a given database.
   *
   * @param databaseUrl - the location of the database
   * @param username    - the username to connect with
   * @param password    - the password to use
   */
  @Inject
  public PostgresSqlDb(final String databaseUrl, final String username, final String password) {

    dataSource = new BasicDataSource();
    dataSource.setDriverClassName("org.postgresql.Driver");
    dataSource.setUrl(databaseUrl);
    dataSource.setUsername(username);
    dataSource.setPassword(password);
    dataSource.setTestWhileIdle(false);
    dataSource.setTestOnBorrow(true);
    dataSource.setValidationQuery("SELECT 1");
    dataSource.setTestOnReturn(false);
    dataSource.setDurationBetweenEvictionRuns(Duration.ofMillis(CONNECTION_POOL_EVICTION_RUN_PERIOD_MILLISECONDS));
    dataSource.setMaxTotal(CONNECTION_POOL_MAX_TOTAL);
    dataSource.setInitialSize(CONNECTION_POOL_INITIAL_SIZE);
    dataSource.setMaxWait(Duration.ofMillis(CONNECTION_POOL_MAX_WAIT_MILLISECONDS));
    dataSource.setRemoveAbandonedTimeout(Duration.ofSeconds(CONNECTION_POOL_REMOVE_ABANDONED_TIMEOUT));
    dataSource.setMinEvictableIdle(Duration.ofMillis(CONNECTION_POOL_MIN_EVICTABLE_IDLE_TIMEOUT_MILLISECONDS));
    dataSource.setMinIdle(CONNECTION_POOL_MIN_MIN_IDLE);
    dataSource.setLogAbandoned(true);
    dataSource.setRemoveAbandonedOnBorrow(true);
    dataSource.setAutoCommitOnReturn(true);
  }

  /**
   * Get a handle to the database.
   *
   * @return database connection.
   * @throws SQLException if a database access error occurs
   */
  public Connection getDatabaseConnection() throws SQLException {
    return dataSource.getConnection();
  }

  @Override
  public void close() {
    try {
      this.dataSource.close();
    } catch (SQLException e) {
      log.error("Error closing the database connection", e);
    }
  }

  /**
   * Check whether the database is a read only replica or not.
   *
   * @return whether the database is read only.
   */
  public boolean isReadOnlyReplica() throws SQLException {
    try (Connection conn = getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement("SELECT pg_is_in_recovery()");
         ResultSet results = pst.executeQuery()
    ) {
      results.next();
      return results.getBoolean(1);
    }
  }
}
