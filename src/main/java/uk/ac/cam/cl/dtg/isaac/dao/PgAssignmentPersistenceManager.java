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
package uk.ac.cam.cl.dtg.isaac.dao;

import java.sql.*;
import java.util.*;
import java.util.Date;

import ma.glasnost.orika.MapperFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.AssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

/**
 * This class is responsible for managing and persisting user data.
 */
public class PgAssignmentPersistenceManager implements IAssignmentPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PgAssignmentPersistenceManager.class);

    private final MapperFacade mapper;
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
            final MapperFacade mapper) {
        this.database = database;
        this.mapper = mapper;
    }

    @Override
    public Long saveAssignment(final AssignmentDTO assignment) throws SegueDatabaseException {
        AssignmentDO assignmentToSave = mapper.map(assignment, AssignmentDO.class);

        PreparedStatement pst;
        try (Connection conn = database.getDatabaseConnection()) {
            pst = conn.prepareStatement(
                    "INSERT INTO assignments(gameboard_id, group_id, owner_user_id, creation_date, due_date)"
                    + " VALUES (?, ?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

            pst.setString(1, assignmentToSave.getGameboardId());
            pst.setLong(2, assignmentToSave.getGroupId());
            pst.setLong(3, assignmentToSave.getOwnerUserId());

            if (assignment.getCreationDate() != null) {
                pst.setTimestamp(4, new java.sql.Timestamp(assignmentToSave.getCreationDate().getTime()));
            } else {
                pst.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
            }

            if (assignment.getDueDate() != null) {
                pst.setTimestamp(5, new java.sql.Timestamp(assignmentToSave.getDueDate().getTime()));
            } else {
                pst.setNull(5, Types.TIMESTAMP);
            }

            if (pst.executeUpdate() == 0) {
                throw new SegueDatabaseException("Unable to save assignment.");
            }

            Long assignmentId;
            try (ResultSet generatedKeys = pst.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    assignmentId = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating assignment failed, no ID obtained.");
                }

                log.debug("Saving Assignment... Assignment ID: " + assignment.getId() + " Db id : " + assignmentId);
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

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM assignments WHERE id = ?");

            pst.setLong(1, assignmentId);
            
            ResultSet results = pst.executeQuery();
 
            List<AssignmentDO> listOfResults = Lists.newArrayList();
            while (results.next()) {
                listOfResults.add(this.convertFromSQLToAssignmentDO(results));
            }

            if (listOfResults.size() > 1) {
                throw new SegueDatabaseException("Ambiguous result, expected single result and found more than one"
                        + listOfResults);
            }
            
            if (listOfResults.size() == 0) {
                return null;
            }

            return this.convertToAssignmentDTO(listOfResults.get(0));

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by id", e);
        }
    }

    @Override
    public List<AssignmentDTO> getAssignmentsByGroupId(final Long groupId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM assignments WHERE group_id = ? SORT BY creation_date");

            pst.setLong(1, groupId);
            
            ResultSet results = pst.executeQuery();
 
            List<AssignmentDTO> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
            }

            return listOfResults;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by group", e);
        }
    }

    @Override
    public List<AssignmentDTO> getAssignmentsByOwnerIdAndGroupId(final Long assignmentOwnerId, final Long groupId)
            throws SegueDatabaseException {

        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement(
                    "SELECT * FROM assignments WHERE owner_user_id = ? AND group_id = ? ORDER BY creation_date");

            pst.setLong(1, assignmentOwnerId);
            pst.setLong(2, groupId);
            
            ResultSet results = pst.executeQuery();
 
            List<AssignmentDTO> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
            }

            return listOfResults;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by group", e);
        }
    }


    @Override
    public List<AssignmentDTO> getAssignmentsByGameboardAndGroup(final String gameboardId, final Long groupId)
            throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement(
                    "SELECT * FROM assignments WHERE gameboard_id = ? AND group_id = ? ORDER BY creation_date");

            pst.setString(1, gameboardId);
            pst.setLong(2, groupId);
            
            ResultSet results = pst.executeQuery();
 
            List<AssignmentDTO> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
            }

            return listOfResults;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by gameboard and group", e);
        }
    }

    @Override
    public List<AssignmentDTO> getAssignmentsByOwner(final Long ownerId) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("SELECT * FROM assignments WHERE owner_user_id = ? ORDER BY creation_date");

            pst.setLong(1, ownerId);
            
            ResultSet results = pst.executeQuery();
 
            List<AssignmentDTO> listOfResults = Lists.newArrayList();
            
            while (results.next()) {
                listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
            }

            return listOfResults;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by owner", e);
        }
    }

    @Override
    public List<AssignmentDTO> getAssignmentsByGroupList(Collection<Long> groupIds) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT * FROM assignments WHERE group_id IN (");

            for (int i = 0; i < groupIds.size(); i++) {
                sb.append("?").append(i < groupIds.size() - 1 ? ", " : "");
            }
            sb.append(") ORDER BY creation_date");

            PreparedStatement pst;
            pst = conn.prepareStatement(sb.toString());
            int i = 1;
            for (Long id : groupIds) {
                pst.setLong(i, id);
                i++;
            }

            ResultSet results = pst.executeQuery();
            List<AssignmentDTO> listOfResults = Lists.newArrayList();

            while (results.next()) {
                listOfResults.add(this.convertToAssignmentDTO(this.convertFromSQLToAssignmentDO(results)));
            }

            return listOfResults;

        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by group list", e);
        }
    }

    @Override
    public void deleteAssignment(final Long id) throws SegueDatabaseException {
        try (Connection conn = database.getDatabaseConnection()) {
            PreparedStatement pst;
            pst = conn.prepareStatement("DELETE FROM assignments WHERE id = ?");

            pst.setLong(1, id);
            
            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while deleting assignment", e);
        }
    }
    
    /**
     * Convert form a Assignment DO to a Assignment DTO.
     * 
     * This method relies on the api to fully resolve questions.
     * 
     * @param assignmentDO
     *            - to convert
     * @return Assignment DTO
     */
    private AssignmentDTO convertToAssignmentDTO(final AssignmentDO assignmentDO) {
        return mapper.map(assignmentDO, AssignmentDTO.class);
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

        return new AssignmentDO(sqlResults.getLong("id"), sqlResults.getString("gameboard_id"),
                sqlResults.getLong("owner_user_id"), sqlResults.getLong("group_id"), preciseDate,
                preciseDueDate);
    }
}
