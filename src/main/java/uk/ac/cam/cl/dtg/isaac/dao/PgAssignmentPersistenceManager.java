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

package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.mappers.MiscMapper;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * This class is responsible for managing and persisting user data.
 */
public class PgAssignmentPersistenceManager implements IAssignmentPersistenceManager {
  private static final Logger log = LoggerFactory.getLogger(PgAssignmentPersistenceManager.class);

  private final MiscMapper mapper;
  private final PostgresSqlDb database;

  /**
   * Creates a new user data manager object.
   *
   * @param database
   *            - the database reference used for persistence.
   * @param mapper
   *            - An instance of an automapper that can be used for mapping to and from AssignmentDOs and DTOs.
   */
  @Inject
  public PgAssignmentPersistenceManager(final PostgresSqlDb database,
                                        final MiscMapper mapper) {
    this.database = database;
    this.mapper = mapper;
  }

  @Override
  public Long saveAssignment(final AssignmentDTO assignment) throws SegueDatabaseException {
    AssignmentDO assignmentToSave = mapper.map(assignment);

    String query = "INSERT INTO assignments(gameboard_id, group_id, owner_user_id, creation_date, due_date, notes,"
        + " scheduled_start_date) VALUES (?, ?, ?, ?, ?, ?, ?);";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)
    ) {
      pst.setString(FIELD_SAVE_GAMEBOARD_ID, assignmentToSave.getGameboardId());
      pst.setLong(FIELD_SAVE_GROUP_ID, assignmentToSave.getGroupId());
      pst.setLong(FIELD_SAVE_OWNER_USER_ID, assignmentToSave.getOwnerUserId());

      if (assignment.getCreationDate() != null) {
        pst.setTimestamp(FIELD_SAVE_CREATION_DATE,
            new java.sql.Timestamp(assignmentToSave.getCreationDate().getTime()));
      } else {
        pst.setTimestamp(FIELD_SAVE_CREATION_DATE, new java.sql.Timestamp(new Date().getTime()));
      }

      if (assignment.getDueDate() != null) {
        pst.setTimestamp(FIELD_SAVE_DUE_DATE, new java.sql.Timestamp(assignmentToSave.getDueDate().getTime()));
      } else {
        pst.setNull(FIELD_SAVE_DUE_DATE, Types.TIMESTAMP);
      }

      if (assignment.getNotes() != null) {
        pst.setString(FIELD_SAVE_NOTES, assignmentToSave.getNotes());
      } else {
        pst.setNull(FIELD_SAVE_NOTES, Types.VARCHAR);
      }

      if (assignment.getScheduledStartDate() != null) {
        pst.setTimestamp(FIELD_SAVE_SCHEDULED_START_DATE,
            new java.sql.Timestamp(assignmentToSave.getScheduledStartDate().getTime()));
      } else {
        pst.setNull(FIELD_SAVE_SCHEDULED_START_DATE, Types.TIMESTAMP);
      }

      if (pst.executeUpdate() == 0) {
        throw new SegueDatabaseException("Unable to save assignment.");
      }

      long assignmentId;
      try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          assignmentId = generatedKeys.getLong(1);
        } else {
          throw new SQLException("Creating assignment failed, no ID obtained.");
        }

        log.debug("Saving Assignment... Assignment ID: {} Db id : {}", assignment.getId(), assignmentId);
        return assignmentId;
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception", e);
    }
  }

  @Override
  public AssignmentDTO getAssignmentById(final Long assignmentId) throws SegueDatabaseException {
    if (null == assignmentId) {
      return null;
    }

    String query = "SELECT * FROM assignments WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_ID_ASSIGNMENT_ID, assignmentId);

      try (ResultSet results = pst.executeQuery()) {

        List<AssignmentDO> listOfResults = Lists.newArrayList();
        while (results.next()) {
          listOfResults.add(this.convertFromSQLToAssignmentDO(results));
        }

        if (listOfResults.size() > 1) {
          throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
              + listOfResults);
        }

        if (listOfResults.isEmpty()) {
          return null;
        }

        return this.convertToAssignmentDTO(listOfResults.get(0));
      }

    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find assignment by id", e);
    }
  }

  @Override
  public List<AssignmentDTO> getAssignmentsByGroupId(final Long groupId) throws SegueDatabaseException {
    return this.getAssignmentsByGroupList(Collections.singletonList(groupId));
  }

  @Override
  public List<AssignmentDTO> getAssignmentsByOwnerIdAndGroupId(final Long assignmentOwnerId, final Long groupId)
      throws SegueDatabaseException {

    String query = "SELECT * FROM assignments WHERE owner_user_id = ? AND group_id = ? ORDER BY creation_date";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_OWNER_AND_GROUP_OWNER_USER_ID, assignmentOwnerId);
      pst.setLong(FIELD_GET_BY_OWNER_AND_GROUP_GROUP_ID, groupId);

      try (ResultSet results = pst.executeQuery()) {

        List<AssignmentDTO> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find assignment by group", e);
    }
  }


  @Override
  public List<AssignmentDTO> getAssignmentsByGameboardAndGroup(final String gameboardId, final Long groupId)
      throws SegueDatabaseException {
    String query = "SELECT * FROM assignments WHERE gameboard_id = ? AND group_id = ? ORDER BY creation_date";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setString(FIELD_GET_BY_GAMEBOARD_AND_GROUP_GAMEBOARD_ID, gameboardId);
      pst.setLong(FILED_GET_BY_GAMEBOARD_AND_GROUP_GROUP_ID, groupId);

      try (ResultSet results = pst.executeQuery()) {

        List<AssignmentDTO> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find assignment by gameboard and group", e);
    }
  }

  @Override
  public List<AssignmentDTO> getAssignmentsByOwner(final Long ownerId) throws SegueDatabaseException {
    String query = "SELECT * FROM assignments WHERE owner_user_id = ? ORDER BY creation_date";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_GET_BY_OWNER_OWNER_USER_ID, ownerId);

      try (ResultSet results = pst.executeQuery()) {

        List<AssignmentDTO> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find assignment by owner", e);
    }
  }

  @Override
  public List<AssignmentDTO> getAssignmentsByGroupList(final Collection<Long> groupIds) throws SegueDatabaseException {
    StringBuilder sb = new StringBuilder();
    sb.append("SELECT * FROM assignments WHERE group_id IN (");

    for (int i = 0; i < groupIds.size(); i++) {
      sb.append("?").append(i < groupIds.size() - 1 ? ", " : "");
    }
    sb.append(") ORDER BY creation_date");

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(sb.toString())
    ) {
      int i = 1;
      for (Long id : groupIds) {
        pst.setLong(i, id);
        i++;
      }

      try (ResultSet results = pst.executeQuery()) {
        List<AssignmentDTO> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find assignment by group list", e);
    }
  }

  @Override
  public List<AssignmentDTO> getAssignmentsScheduledForHour(final Date timestamp) throws SegueDatabaseException {
    if (null == timestamp) {
      throw new SegueDatabaseException("Parameter timestamp is null, cannot search for scheduled assignments!");
    }
    String query = "SELECT * FROM assignments WHERE scheduled_start_date IS NOT NULL AND scheduled_start_date "
        + "BETWEEN ((?)::timestamp - INTERVAL '10 minute') AND ((?)::timestamp + INTERVAL '59 minute');";

    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setTimestamp(FIELD_GET_SCHEDULED_FOR_HOUR_FIRST_TIMESTAMP, new java.sql.Timestamp(timestamp.getTime()));
      pst.setTimestamp(FIELD_GET_SCHEDULED_FOR_HOUR_SECOND_TIMESTAMP, new java.sql.Timestamp(timestamp.getTime()));

      try (ResultSet results = pst.executeQuery()) {
        List<AssignmentDTO> listOfResults = Lists.newArrayList();

        while (results.next()) {
          listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
        }

        return listOfResults;
      }
    } catch (SQLException e) {
      throw new SegueDatabaseException("Unable to find scheduled assignments", e);
    }
  }

  @Override
  public void deleteAssignment(final Long id) throws SegueDatabaseException {
    String query = "DELETE FROM assignments WHERE id = ?";
    try (Connection conn = database.getDatabaseConnection();
         PreparedStatement pst = conn.prepareStatement(query)
    ) {
      pst.setLong(FIELD_DELETE_ASSIGNMENT_ID, id);

      pst.execute();
    } catch (SQLException e) {
      throw new SegueDatabaseException("Postgres exception while deleting assignment", e);
    }
  }

  /**
   * Convert form a Assignment DO to a Assignment DTO.
   * <br>
   * This method relies on the api to fully resolve questions.
   *
   * @param assignmentDO
   *            - to convert
   * @return Assignment DTO
   */
  private AssignmentDTO convertToAssignmentDTO(final AssignmentDO assignmentDO) {
    return mapper.map(assignmentDO);
  }

  /**
   * Convert from an SQL result set to an Assignment DO.
   *
   * @param sqlResults set - assumed to be at the correct position.
   * @return AssignmentDO
   * @throws SQLException if we cannot access a required field.
   */
  private AssignmentDO convertFromSQLToAssignmentDO(final ResultSet sqlResults) throws SQLException {
    java.util.Date preciseDate = new java.util.Date(sqlResults.getTimestamp("creation_date").getTime());

    java.util.Date preciseDueDate = null;
    if (sqlResults.getTimestamp("due_date") != null) {
      preciseDueDate = new java.util.Date(sqlResults.getTimestamp("due_date").getTime());
    }
    java.util.Date preciseScheduledStartDate = null;
    if (sqlResults.getTimestamp("scheduled_start_date") != null) {
      preciseScheduledStartDate = new java.util.Date(sqlResults.getTimestamp("scheduled_start_date").getTime());
    }

    return new AssignmentDO(sqlResults.getLong("id"), sqlResults.getString("gameboard_id"),
        sqlResults.getLong("owner_user_id"), sqlResults.getLong("group_id"), sqlResults.getString("notes"), preciseDate,
        preciseDueDate, preciseScheduledStartDate);
  }

  // Field Constants
  // saveAssignment
  private static final int FIELD_SAVE_GAMEBOARD_ID = 1;
  private static final int FIELD_SAVE_GROUP_ID = 2;
  private static final int FIELD_SAVE_OWNER_USER_ID = 3;
  private static final int FIELD_SAVE_CREATION_DATE = 4;
  private static final int FIELD_SAVE_DUE_DATE = 5;
  private static final int FIELD_SAVE_NOTES = 6;
  private static final int FIELD_SAVE_SCHEDULED_START_DATE = 7;

  // getAssignmentById
  private static final int FIELD_GET_BY_ID_ASSIGNMENT_ID = 1;

  // getAssignmentsByOwnerIdAndGroupId
  private static final int FIELD_GET_BY_OWNER_AND_GROUP_OWNER_USER_ID = 1;
  private static final int FIELD_GET_BY_OWNER_AND_GROUP_GROUP_ID = 2;

  // getAssignmentsByGameboardAndGroup
  private static final int FIELD_GET_BY_GAMEBOARD_AND_GROUP_GAMEBOARD_ID = 1;
  private static final int FILED_GET_BY_GAMEBOARD_AND_GROUP_GROUP_ID = 2;

  // getAssignmentsByOwner
  private static final int FIELD_GET_BY_OWNER_OWNER_USER_ID = 1;

  // getAssignmentsScheduledForHour
  private static final int FIELD_GET_SCHEDULED_FOR_HOUR_FIRST_TIMESTAMP = 1;
  private static final int FIELD_GET_SCHEDULED_FOR_HOUR_SECOND_TIMESTAMP = 2;

  // deleteAssignment
  private static final int FIELD_DELETE_ASSIGNMENT_ID = 1;
}
