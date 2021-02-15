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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manage quiz attempts.
 */
public class QuizAttemptManager {
    private final IQuizAttemptPersistenceManager quizAttemptPersistenceManager;

    /**
     * QuizAttemptManager.
     * @param quizAttemptPersistenceManager
     *            - to save quiz attempts
     */
    @Inject
    public QuizAttemptManager(IQuizAttemptPersistenceManager quizAttemptPersistenceManager) {
        this.quizAttemptPersistenceManager = quizAttemptPersistenceManager;
    }

    public QuizAttemptDTO fetchOrCreate(QuizAssignmentDTO quizAssignment, RegisteredUserDTO user) throws AttemptCompletedException, SegueDatabaseException {
        // Check if an attempt exists
        QuizAttemptDTO existingAttempt = quizAttemptPersistenceManager.getByQuizAssignmentIdAndUserId(quizAssignment.getId(), user.getId());

        if (existingAttempt != null) {
            if (existingAttempt.getCompletedDate() != null) {
                // This is an error as the user cannot get an attempt they have marked completed.
                throw new AttemptCompletedException();
            }
            return existingAttempt;
        }

        // Make a new attempt
        QuizAttemptDTO newQuizAttempt = new QuizAttemptDTO();
        newQuizAttempt.setUserId(user.getId());
        newQuizAttempt.setQuizAssignmentId(quizAssignment.getId());
        newQuizAttempt.setQuizId(quizAssignment.getQuizId());
        newQuizAttempt.setStartDate(new Date());

        newQuizAttempt.setId(quizAttemptPersistenceManager.saveAttempt(newQuizAttempt));

        return newQuizAttempt;
    }

    public QuizAttemptDTO fetchOrCreateFreeQuiz(IsaacQuizDTO quiz, RegisteredUserDTO user) throws SegueDatabaseException {
        // Check if an attempt exists
        List<QuizAttemptDTO> existingAttempts = quizAttemptPersistenceManager.getByQuizIdAndUserId(quiz.getId(), user.getId());

        if (!existingAttempts.isEmpty()) {
            // Find any incomplete free attempts
            Optional<QuizAttemptDTO> incompleteAttempt = existingAttempts.stream()
                .filter(attempt -> attempt.getCompletedDate() == null && attempt.getQuizAssignmentId() == null).findFirst();
            if (incompleteAttempt.isPresent()) {
                // Continue with existing attempt until it is completed or abandoned.
                return incompleteAttempt.get();
            }
        }

        // Make a new attempt
        QuizAttemptDTO newQuizAttempt = new QuizAttemptDTO();
        newQuizAttempt.setUserId(user.getId());
        newQuizAttempt.setQuizAssignmentId(null);
        newQuizAttempt.setQuizId(quiz.getId());
        newQuizAttempt.setStartDate(new Date());

        newQuizAttempt.setId(quizAttemptPersistenceManager.saveAttempt(newQuizAttempt));

        return newQuizAttempt;
    }

    public QuizAttemptDTO getById(Long quizAttemptId) throws SegueDatabaseException {
        return quizAttemptPersistenceManager.getById(quizAttemptId);
    }

    public void deleteAttempt(QuizAttemptDTO quizAttempt) throws SegueDatabaseException {
        quizAttemptPersistenceManager.deleteAttempt(quizAttempt.getId());
    }

    public void updateAttemptCompletionStatus(QuizAttemptDTO quizAttempt, boolean newCompletionStatus) throws SegueDatabaseException {
        quizAttemptPersistenceManager.updateAttemptCompletionStatus(quizAttempt.getId(), newCompletionStatus);
    }

    public Set<Long> getCompletedUserIds(QuizAssignmentDTO assignment) throws SegueDatabaseException {
        return quizAttemptPersistenceManager.getCompletedUserIds(assignment.getId());
    }

    public void augmentAssignmentsFor(RegisteredUserDTO user, List<QuizAssignmentDTO> assignments) throws SegueDatabaseException {
        Map<Long, QuizAttemptDTO> attempts = quizAttemptPersistenceManager.getByQuizAssignmentIdsAndUserId(assignments.stream().map(QuizAssignmentDTO::getId).collect(Collectors.toList()), user.getId());
        assignments.forEach(quizAssignment -> quizAssignment.setAttempt(attempts.get(quizAssignment.getId())));
    }
}
