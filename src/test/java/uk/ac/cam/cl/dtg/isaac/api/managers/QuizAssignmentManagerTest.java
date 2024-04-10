/**
 * Copyright 2021 Raspberry Pi Foundation
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

package uk.ac.cam.cl.dtg.isaac.api.managers;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.resetToNice;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

class QuizAssignmentManagerTest extends AbstractManagerTest {

  private QuizAssignmentManager quizAssignmentManager;

  private IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
  private EmailService emailService;

  private QuizAssignmentDTO newAssignment;

  @BeforeEach
  public void setUp() throws ContentManagerException, SegueDatabaseException {
    initializeAdditionalObjects();

    PropertiesLoader properties = createMock(PropertiesLoader.class);
    emailService = createMock(EmailService.class);
    quizAssignmentPersistenceManager = createMock(IQuizAssignmentPersistenceManager.class);

    quizAssignmentManager =
        new QuizAssignmentManager(quizAssignmentPersistenceManager, emailService, quizManager, groupManager,
            properties);

    expect(properties.getProperty(HOST_NAME)).andStubReturn("example.com.invalid");

    replay(properties, emailService, quizAssignmentPersistenceManager);
  }

  private void initializeAdditionalObjects() {
    newAssignment = new QuizAssignmentDTO(
        null, studentQuiz.getId(),
        teacher.getId(), studentGroup.getId(),
        somePastDate, someFutureDate,
        QuizFeedbackMode.OVERALL_MARK);
  }

  @Test
  void createAssignment() throws SegueDatabaseException, ContentManagerException {
    Long returnedId = 0xF00L;

    withMock(quizAssignmentPersistenceManager, m -> {
      expect(m.getAssignmentsByQuizIdAndGroup(
          studentQuiz.getId(), studentGroup.getId())).andReturn(Collections.emptyList());
      expect(m.saveAssignment(newAssignment)).andReturn(returnedId);
    });
    withMock(emailService, m -> m.sendAssignmentEmailToGroup(eq(newAssignment), eq(studentQuiz), anyObject(),
        eq("email-template-group-quiz-assignment")));

    QuizAssignmentDTO createdAssignment = quizAssignmentManager.createAssignment(newAssignment);

    assertEquals(returnedId, createdAssignment.getId());
    assertTrue(Duration.between(Instant.now(), createdAssignment.getCreationDate()).toMillis() < 1000);
  }

  @Test
  void createAnotherAssignmentAfterFirstIsDueSucceeds() throws SegueDatabaseException, ContentManagerException {

    withMock(quizAssignmentPersistenceManager, m -> {
      expect(m.getAssignmentsByQuizIdAndGroup(
          studentQuiz.getId(), studentGroup.getId())).andReturn(Collections.singletonList(overdueAssignment));
      expect(m.saveAssignment(newAssignment)).andReturn(0L);
    });
    resetToNice(emailService);

    quizAssignmentManager.createAssignment(newAssignment);
  }

  @Test
  void createAssignmentFailsInThePast() {
    newAssignment.setDueDate(somePastDate);
    assertThrows(DueBeforeNowException.class, () -> quizAssignmentManager.createAssignment(newAssignment));
  }

  @Test
  void createDuplicateAssignmentFails() {
    withMock(quizAssignmentPersistenceManager, m -> expect(m.getAssignmentsByQuizIdAndGroup(
        studentQuiz.getId(), studentGroup.getId())).andReturn(Collections.singletonList(studentAssignment)));

    assertThrows(DuplicateAssignmentException.class, () -> quizAssignmentManager.createAssignment(newAssignment));
  }

  @Test
  void getAssignedQuizzes() throws SegueDatabaseException {
    withMock(quizAssignmentPersistenceManager,
        m -> expect(m.getAssignmentsByGroupList(studentGroups)).andReturn(teacherAssignmentsToTheirGroups));
    List<QuizAssignmentDTO> assignedQuizzes = quizAssignmentManager.getAssignedQuizzes(student);

    assertEquals(studentAssignments, assignedQuizzes);
  }

  @Test
  void getActiveQuizAssignments() throws SegueDatabaseException {
    withMock(quizAssignmentPersistenceManager,
        m -> expect(m.getAssignmentsByGroupList(studentGroups)).andReturn(teacherAssignmentsToTheirGroups));

    List<QuizAssignmentDTO> activeQuizAssignments =
        quizAssignmentManager.getActiveQuizAssignments(studentQuiz, student);

    assertEquals(Collections.singletonList(studentAssignment), activeQuizAssignments);
  }
}
