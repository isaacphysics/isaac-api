/*
 * Copyright 2021 Raspberry Pi Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dao;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentCancelledException;
import uk.ac.cam.cl.dtg.isaac.dos.QuizAssignmentDO;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.database.PostgresSqlDb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.List;

/**
 * This class is responsible for managing and persisting quiz assignments
 */
public class PgQuizAssignmentPersistenceManager implements IQuizAssignmentPersistenceManager {
    private static final Logger log = LoggerFactory.getLogger(PgQuizAssignmentPersistenceManager.class);

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
    public PgQuizAssignmentPersistenceManager(final PostgresSqlDb database,
                                              final MapperFacade mapper) {
        this.database = database;
        this.mapper = mapper;
    }

    @Override
    public Long saveAssignment(final QuizAssignmentDTO assignment) throws SegueDatabaseException {
        QuizAssignmentDO assignmentToSave = mapper.map(assignment, QuizAssignmentDO.class);

        String query = "INSERT INTO quiz_assignments(quiz_id, group_id, owner_user_id, creation_date, due_date, scheduled_start_date, quiz_feedback_mode)" +
                " VALUES (?, ?, ?, ?, ?, ?, ?);";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        ) {
            pst.setString(1, assignmentToSave.getQuizId());
            pst.setLong(2, assignmentToSave.getGroupId());
            pst.setLong(3, assignmentToSave.getOwnerUserId());

            if (assignmentToSave.getCreationDate() != null) {
                pst.setTimestamp(4, new java.sql.Timestamp(assignmentToSave.getCreationDate().getTime()));
            } else {
                pst.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
            }

            if (assignmentToSave.getDueDate() != null) {
                pst.setTimestamp(5, new java.sql.Timestamp(assignmentToSave.getDueDate().getTime()));
            } else {
                pst.setNull(5, Types.TIMESTAMP);
            }

            if (assignment.getScheduledStartDate() != null) {
                pst.setTimestamp(6, new java.sql.Timestamp(assignmentToSave.getScheduledStartDate().getTime()));
            } else {
                pst.setNull(6, Types.TIMESTAMP);
            }

            pst.setString(7, assignmentToSave.getQuizFeedbackMode().name());

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

                log.debug("Saving Quiz Assignment... Quiz Assignment ID: " + assignment.getId() + " Db id : " + assignmentId);
                return assignmentId;
            }

        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception", e);
        }
    }

    @Override
    public List<QuizAssignmentDTO> getAssignmentsByQuizIdAndGroup(final String quizId, final Long groupId) throws SegueDatabaseException {
        String query = "SELECT * FROM quiz_assignments WHERE quiz_id = ? AND group_id = ? AND NOT deleted";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setString(1, quizId);
            pst.setLong(2, groupId);

            try (ResultSet results = pst.executeQuery()) {

                List<QuizAssignmentDTO> listOfResults = Lists.newArrayList();

                while (results.next()) {
                    listOfResults.add(this.convertToQuizAssignmentDTO(this.convertFromSQLToQuizAssignmentDO(results)));
                }

                return listOfResults;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find quiz assignment by group", e);
        }
    }

    @Override
    public List<QuizAssignmentDTO> getAssignmentsByGroupList(List<Long> groupIds) throws SegueDatabaseException {
        List<QuizAssignmentDTO> listOfResults = Lists.newArrayList();
        if (groupIds.isEmpty()) {
            return listOfResults; // IN condition below doesn't work with empty list.
        }
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM quiz_assignments WHERE group_id IN (");

        for (int i = 0; i < groupIds.size(); i++) {
            sb.append("?").append(i < groupIds.size() - 1 ? ", " : "");
        }
        sb.append(") AND NOT deleted ORDER BY creation_date");
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(sb.toString());
        ) {
            int i = 1;
            for (Long id : groupIds) {
                pst.setLong(i, id);
                i++;
            }

            try (ResultSet results = pst.executeQuery()) {

                while (results.next()) {
                    listOfResults.add(this.convertToQuizAssignmentDTO(this.convertFromSQLToQuizAssignmentDO(results)));
                }

                return listOfResults;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find assignment by group list", e);
        }
    }

    @Override
    public QuizAssignmentDTO getAssignmentById(Long quizAssignmentId) throws SegueDatabaseException, AssignmentCancelledException {
        String query = "SELECT * FROM quiz_assignments WHERE id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            // Deleted quiz assignments are filtered below with a specific error
            pst.setLong(1, quizAssignmentId);

            try (ResultSet results = pst.executeQuery()) {
                if (results.next()) {
                    if (results.getBoolean("deleted")) {
                        throw new AssignmentCancelledException();
                    }
                    return this.convertToQuizAssignmentDTO(this.convertFromSQLToQuizAssignmentDO(results));
                }
            }
            throw new SQLException("QuizAssignment result set empty.");
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find quiz assignment by id", e);
        }
    }

    @Override
    public void cancelAssignment(Long quizAssignmentId) throws SegueDatabaseException {
        String query = "UPDATE quiz_assignments SET deleted = true WHERE id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setLong(1, quizAssignmentId);

            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while deleting quiz assignment", e);
        }
    }

    @Override
    public void updateAssignment(Long quizAssignmentId, QuizAssignmentDTO updates) throws SegueDatabaseException {
        String query = "UPDATE quiz_assignments SET quiz_feedback_mode = COALESCE(?, quiz_feedback_mode)," +
                "due_date = COALESCE(?, due_date) WHERE id = ?";
        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            if (updates.getQuizFeedbackMode() != null) {
                pst.setString(1, updates.getQuizFeedbackMode().name());
            } else {
                pst.setNull(1, Types.VARCHAR);
            }

            if (updates.getDueDate() != null) {
                pst.setTimestamp(2, new java.sql.Timestamp(updates.getDueDate().getTime()));
            } else {
                pst.setNull(2, Types.TIMESTAMP);
            }

            pst.setLong(3, quizAssignmentId);

            pst.execute();
        } catch (SQLException e) {
            throw new SegueDatabaseException("Postgres exception while updating quiz assignment", e);
        }
    }

    @Override
    public List<QuizAssignmentDTO> getAssignmentsScheduledForHour(final Date timestamp) throws SegueDatabaseException {
        if (null == timestamp) {
            throw new SegueDatabaseException("Parameter timestamp is null, cannot search for scheduled assignments!");
        }
        String query = "SELECT * FROM quiz_assignments WHERE scheduled_start_date IS NOT NULL AND scheduled_start_date BETWEEN ((?)::timestamp - INTERVAL '10 minute') AND ((?)::timestamp + INTERVAL '59 minute');";

        try (Connection conn = database.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement(query);
        ) {
            pst.setTimestamp(1, new java.sql.Timestamp(timestamp.getTime()));
            pst.setTimestamp(2, new java.sql.Timestamp(timestamp.getTime()));

            try (ResultSet results = pst.executeQuery()) {
                List<QuizAssignmentDTO> listOfResults = Lists.newArrayList();

                while (results.next()) {
                    listOfResults.add(this.convertToQuizAssignmentDTO(this.convertFromSQLToQuizAssignmentDO(results)));
                }

                return listOfResults;
            }
        } catch (SQLException e) {
            throw new SegueDatabaseException("Unable to find scheduled assignments", e);
        }
    }

    /**
     * Convert from a QuizAssignment DO to a QuizAssignment DTO.
     *
     * This method relies on the api to fully resolve questions.
     *
     * @param quizAssignmentDO
     *            - to convert
     * @return QuizAssignmentDTO
     */
    private QuizAssignmentDTO convertToQuizAssignmentDTO(final QuizAssignmentDO quizAssignmentDO) {
        return mapper.map(quizAssignmentDO, QuizAssignmentDTO.class);
    }

    /**
     * Convert from an SQL result set to an QuizAssignment DO.
     *
     * @param sqlResults set - assumed to be at the correct position.
     * @return QuizAssignmentDO
     * @throws SQLException if we cannot access a required field.
     */
    private QuizAssignmentDO convertFromSQLToQuizAssignmentDO(final ResultSet sqlResults) throws SQLException {
        Date preciseDate = new Date(sqlResults.getTimestamp("creation_date").getTime());

        Date preciseDueDate = null;
        if (sqlResults.getTimestamp("due_date") != null) {
            preciseDueDate = new Date(sqlResults.getTimestamp("due_date").getTime());
        }

        java.util.Date preciseScheduledStartDate = null;
        if (sqlResults.getTimestamp("scheduled_start_date") != null) {
            preciseScheduledStartDate = new java.util.Date(sqlResults.getTimestamp("scheduled_start_date").getTime());
        }

        return new QuizAssignmentDO(sqlResults.getLong("id"), sqlResults.getString("quiz_id"),
                sqlResults.getLong("owner_user_id"), sqlResults.getLong("group_id"), preciseDate,
                preciseDueDate, preciseScheduledStartDate,
                QuizFeedbackMode.valueOf(sqlResults.getString("quiz_feedback_mode")));
    }
}
