/*
 * Copyright 2021 Raspberry Pi Foundation
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

import uk.ac.cam.cl.dtg.isaac.api.managers.AssignmentCancelledException;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.List;

public interface IQuizAssignmentPersistenceManager {

    /**
     * Save an Quiz assignment.
     *
     * @param assignment
     *            - assignment to save
     * @return internal database id for the saved assignment.
     * @throws SegueDatabaseException
     *             - if there is a problem saving the assignment in the database.
     */
    Long saveAssignment(QuizAssignmentDTO assignment) throws SegueDatabaseException;

    /**
     * Get a list of QuizAssignmentDTO objects for this quiz and group.
     *
     * It is not an error for there to be multiple of these, but the spans of their creationDate and dueDate
     * should be disjoint.
     *
     * @param quizId
     *            - quiz of interest
     * @param groupId
     *            - the group id assigned this quiz.
     * @return the assignments of this quiz to that group.
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database.
     */
    List<QuizAssignmentDTO> getAssignmentsByQuizIdAndGroup(final String quizId, final Long groupId)
        throws SegueDatabaseException;

    /**
     * Get a list of QuizAssignmentDTO objects for these groups.
     *
     * @param groupIds
     *            - the group ids of interest.
     * @return the assignments to those groups.
     * @throws SegueDatabaseException
     *             - if there is an error when accessing the database.
     */
    List<QuizAssignmentDTO> getAssignmentsByGroupList(List<Long> groupIds) throws SegueDatabaseException;

    /**
     * Get a quiz assignment from its ID.
     *
     * @param quizAssignmentId The ID of the quiz assignment.
     * @return The quiz assignment.
     */
    QuizAssignmentDTO getAssignmentById(Long quizAssignmentId) throws SegueDatabaseException, AssignmentCancelledException;

    /**
     * Cancel (soft delete) a quiz assignment.
     *
     * @param quizAssignmentId The ID of the quiz assignment to cancel.
     */
    void cancelAssignment(Long quizAssignmentId) throws SegueDatabaseException;

    /**
     * Update a quiz assignment.
     *
     * @param quizAssignmentId The ID of the quiz assignment to update.
     * @param updates The values to update (ony feedbackMode is considered).
     */
    void updateAssignment(Long quizAssignmentId, QuizAssignmentDTO updates) throws SegueDatabaseException;
}