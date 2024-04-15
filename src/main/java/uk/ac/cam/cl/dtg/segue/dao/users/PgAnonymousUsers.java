/**
 * Copyright 2019 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao.users;

import static java.util.Objects.requireNonNull;
import static uk.ac.cam.cl.dtg.segue.dao.AbstractPgDataManager.getInstantFromTimestamp;

import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

public class PgAnonymousUsers implements IAnonymousUserDataManager {
  private final PostgresSqlDb database;

  /**
   * PgAnonymousUsers.
   *
   * @param ds - the postgres datasource to use
   */
  @Inject
  public PgAnonymousUsers(final PostgresSqlDb ds) {
    this.database = ds;
  }

  @Override
  public AnonymousUser storeAnonymousUser(final AnonymousUser user) throws SegueDatabaseException {
    String query = "INSERT INTO temporary_user_store (id, temporary_app_data, created, last_updated)"
        + " VALUES (?,?::text::jsonb,?,?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_STORE_ID, user.getSessionId());
      pst.setString(FIELD_STORE_TEMPORARY_APP_DATA, "{\"questionAttempts\":{}}");
      pst.setTimestamp(FIELD_STORE_CREATED, Timestamp.from(user.getDateCreated()));
      pst.setTimestamp(FIELD_STORE_LAST_UPDATED, Timestamp.from(user.getDateCreated()));

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save anonymous user.");
      }
      return user;

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception on creating anonymous user ", e);
    }
  }

  @Override
  public void deleteAnonymousUser(final AnonymousUser userToDelete) throws SegueDatabaseException {
    String query = "DELETE FROM temporary_user_store WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_DELETE_ID, userToDelete.getSessionId());

      int executeUpdate = pst.executeUpdate();
      if (executeUpdate == 0) {
        throw new ResourceNotFoundException("Could not delete the requested anonymous user.");
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to delete anonymous user", e);
    }
  }

  @Override
  public AnonymousUser getById(final String id) throws SegueDatabaseException {
    String query = "SELECT * FROM temporary_user_store WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_ID, id);

      try (ResultSet result = pst.executeQuery()) {
        // are there any results
        if (!result.isBeforeFirst()) {
          return null;
        }
        result.next();

        AnonymousUser userToReturn =
            new AnonymousUser(result.getString("id"), getInstantFromTimestamp(result, "created"),
                getInstantFromTimestamp(result, "last_updated"));
        updateLastUpdatedDate(userToReturn);

        return userToReturn;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while trying to get anonymous user", e);
    }
  }

  @Override
  public Long getCountOfAnonymousUsers() throws SegueDatabaseException {
    String query = "SELECT COUNT(*) AS TOTAL FROM temporary_user_store;";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query);
         ResultSet results = pst.executeQuery()
    ) {
      results.next();
      return results.getLong("TOTAL");
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception: Unable to count log events by type", e);
    }
  }

  /**
   * Mutates input object update date and persists update to db.
   *
   * @param user - to update
   * @return user;
   */
  private AnonymousUser updateLastUpdatedDate(final AnonymousUser user) throws SegueDatabaseException {
    requireNonNull(user);

    String query = "UPDATE temporary_user_store SET last_updated = ? WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      Instant newUpdatedDate = Instant.now();
      pst.setTimestamp(FIELD_UPDATE_UPDATED_DATE, Timestamp.from(newUpdatedDate));
      pst.setString(FIELD_UPDATE_UPDATED_ID, user.getSessionId());
      pst.execute();

      user.setLastUpdated(newUpdatedDate);

      return user;
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  // Field Constants
  // storeAnonymousUser
  private static final int FIELD_STORE_ID = 1;
  private static final int FIELD_STORE_TEMPORARY_APP_DATA = 2;
  private static final int FIELD_STORE_CREATED = 3;
  private static final int FIELD_STORE_LAST_UPDATED = 4;

  // deleteAnonymousUser
  private static final int FIELD_DELETE_ID = 1;

  // getById
  private static final int FIELD_GET_ID = 1;

  // updateLastUpdatedDate
  private static final int FIELD_UPDATE_UPDATED_DATE = 1;
  private static final int FIELD_UPDATE_UPDATED_ID = 2;
}
