/**
 * Copyright 2017 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.dao;

import static java.time.ZoneOffset.UTC;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

/**
 * Common abstract class for postgres DAOs.
 */
public abstract class AbstractPgDataManager {

  /**
   * Helper that picks the correct pst method based on the value provided.
   *
   * @param pst   - prepared statement - already initialised
   * @param index - index of the value to be replaced in the pst
   * @param value - value
   * @throws SQLException - if there is a db problem.
   */
  protected void setValueHelper(final PreparedStatement pst, final int index, final Object value) throws SQLException {
    if (null == value) {
      pst.setNull(index, Types.NULL);
      return;
    }

    if (value.getClass().isEnum()) {
      pst.setString(index, ((Enum<?>) value).name());
    }

    if (value instanceof String stringValue) {
      pst.setString(index, stringValue);
    }

    if (value instanceof Integer integerValue) {
      pst.setInt(index, integerValue);
    }

    if (value instanceof Long longValue) {
      pst.setLong(index, longValue);
    }

    if (value instanceof Instant instantValue) {
      pst.setTimestamp(index, Timestamp.from(instantValue));
    }

    if (value instanceof Boolean booleanValue) {
      pst.setBoolean(index, booleanValue);
    }
  }

  // The ResultSet getter methods can return null if the value in the table is NULL.
  // The Timestamp toInstant method will throw a NullPointerException if called on such a value
  public static Instant getInstantFromTimestamp(ResultSet results, String columnLabel) throws SQLException {
    Timestamp timestamp = results.getTimestamp(columnLabel);
    if (timestamp != null) {
      return timestamp.toInstant();
    } else {
      return null;
    }
  }

  // The Date toInstant method will throw an UnsupportedOperationException, so we must convert it via LocalDate
  public static Instant getInstantFromDate(ResultSet results, String columnLabel) throws SQLException {
    Date date = results.getDate(columnLabel);
    if (date != null) {
      return date.toLocalDate().atStartOfDay(UTC).toInstant();
    } else {
      return null;
    }
  }
}
