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

import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.List;

public interface IQuizAttemptPersistenceManager {
    /**
     * Get the quiz attempt for a particular user in a particular assignment.
     *
     * @param quizAssignmentId The id of the quiz assignment.
     * @param userId The id of the user.
     * @return The attempt, or null if no attempt exists.
     */
    QuizAttemptDTO getByQuizAssignmentIdAndUserId(Long quizAssignmentId, Long userId) throws SegueDatabaseException;

    /**
     * Save an Quiz attempt.
     *
     * @param attempt
     *            - assignment to save
     * @return internal database id for the saved attempt.
     * @throws SegueDatabaseException
     *             - if there is a problem saving the attempt in the database.
     */
    Long saveAttempt(QuizAttemptDTO attempt) throws SegueDatabaseException;

    /**
     * Get all quiz attempts for a particular user at a particular quiz.
     *
     * @param quizId The id of the quiz.
     * @param userId The id of the user.
     * @return The attempts.
     */
    List<QuizAttemptDTO> getByQuizIdAndUserId(String quizId, Long userId) throws SegueDatabaseException;
}
