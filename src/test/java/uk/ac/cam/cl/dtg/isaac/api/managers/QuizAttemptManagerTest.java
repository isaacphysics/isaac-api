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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAttemptPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QuizAttemptManagerTest extends AbstractManagerTest {
    private static final Long TEST_ID = 0xC0000000000L;
    private QuizAttemptManager quizAttemptManager;

    private IQuizAttemptPersistenceManager quizAttemptPersistenceManager;

    @BeforeEach
    public void setUp() {
        quizAttemptPersistenceManager = mock(IQuizAttemptPersistenceManager.class);

        quizAttemptManager = new QuizAttemptManager(quizAttemptPersistenceManager);
    }

    @Test
    public void fetchOrCreateWithExistingAttempt() throws AttemptCompletedException, SegueDatabaseException {
        withMock(quizAttemptPersistenceManager, forStudentAssignmentReturn(studentAttempt));

        QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreate(studentAssignment, student);
        assertEquals(studentAttempt, attempt);
    }

    @Test
    public void fetchOrCreateWithExistingCompletedAttemptFails() throws AttemptCompletedException, SegueDatabaseException {
        assertThrows(AttemptCompletedException.class, () -> {
            withMock(quizAttemptPersistenceManager, forStudentAssignmentReturn(completedAttempt));
            quizAttemptManager.fetchOrCreate(studentAssignment, student);
        });
    }

    @Test
    public void fetchOrCreateCreatesNewAttempt() throws AttemptCompletedException, SegueDatabaseException {
        withMock(quizAttemptPersistenceManager,
            forStudentAssignmentReturn(null),
            m -> when(m.saveAttempt(attemptMatcher(student.getId(), studentAssignment.getId(), studentAssignment.getQuizId())))
                .thenReturn(TEST_ID));

        QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreate(studentAssignment, student);
        assertEquals(TEST_ID, attempt.getId());
    }

    @Test
    public void fetchOrCreateFreeQuizWithExistingAttempt() throws SegueDatabaseException {
        withMock(quizAttemptPersistenceManager, forStudentQuizReturn(Collections.singletonList(this.ownAttempt)));

        QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreateFreeQuiz(studentQuiz, student);
        assertEquals(ownAttempt, attempt);
    }

    @Test
    public void fetchOrCreateFreeQuizWithExistingCompletedAttemptCreatesNewAttempt() throws SegueDatabaseException {
        withMock(quizAttemptPersistenceManager,
            forStudentQuizReturn(Collections.singletonList(completedAttempt)),
            returnTestIdForSaveAttempt());

        QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreateFreeQuiz(studentQuiz, student);
        assertEquals(TEST_ID, attempt.getId());
    }

    @Test
    public void fetchOrCreateFreeQuizCreatesNewAttempt() throws SegueDatabaseException {
        withMock(quizAttemptPersistenceManager,
            forStudentQuizReturn(Collections.emptyList()),
            returnTestIdForSaveAttempt());

        QuizAttemptDTO attempt = quizAttemptManager.fetchOrCreateFreeQuiz(studentQuiz, student);
        assertEquals(TEST_ID, attempt.getId());
    }

    @Test
    public void augmentAssignmentsFor() throws SegueDatabaseException {
        withMock(quizAttemptPersistenceManager,
            m -> when(m.getByQuizAssignmentIdsAndUserId(Collections.singletonList(studentAssignment.getId()), student.getId()))
                .thenReturn(Collections.singletonMap(studentAssignment.getId(), studentAttempt)));
        quizAttemptManager.augmentAssignmentsFor(student, Collections.singletonList(studentAssignment));

        assertEquals(studentAttempt, studentAssignment.getAttempt());
    }

    private MockConfigurer<IQuizAttemptPersistenceManager> forStudentAssignmentReturn(final QuizAttemptDTO attempt) {
        return m -> when(m.getByQuizAssignmentIdAndUserId(studentAssignment.getId(), student.getId())).thenReturn(attempt);
    }

    private MockConfigurer<IQuizAttemptPersistenceManager> forStudentQuizReturn(final List<QuizAttemptDTO> attempts) {
        return m -> when(m.getByQuizIdAndUserId(studentQuiz.getId(), student.getId())).thenReturn(attempts);
    }

    private MockConfigurer<IQuizAttemptPersistenceManager> returnTestIdForSaveAttempt() {
        return m -> when(m.saveAttempt(attemptMatcher(student.getId(), null, studentQuiz.getId()))).thenReturn(TEST_ID);
    }

    private static QuizAttemptDTO attemptMatcher(final Long userId, final Long assignmentId, final String quizId) {
        argThat(new ArgumentMatcher<QuizAttemptDTO>() {
            @Override
            public boolean matches(final QuizAttemptDTO attempt) {
                if (attempt == null) {
                    return false;
                }

                return Objects.equals(attempt.getUserId(), userId)
                        && Objects.equals(attempt.getQuizAssignmentId(), assignmentId)
                        && Objects.equals(attempt.getQuizId(), quizId)
                        && Math.abs(new Date().getTime() - attempt.getStartDate().getTime()) < 1000;
            }

            @Override
            public String toString() {
                return "attempt(userId=" + userId + ", assignmentId=" + assignmentId + ", quizId=" + quizId + ")";
            }
        });
        return null;
    }
}
