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

import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.EmailService;
import uk.ac.cam.cl.dtg.isaac.dao.IQuizAssignmentPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class QuizAssignmentManagerTest extends AbstractManagerTest {

    private QuizAssignmentManager quizAssignmentManager;

    private IQuizAssignmentPersistenceManager quizAssignmentPersistenceManager;
    private EmailService emailService;

    private QuizAssignmentDTO newAssignment;

    @Before
    public void setUp() throws ContentManagerException, SegueDatabaseException {
        AbstractConfigLoader properties = createMock(AbstractConfigLoader.class);
        emailService = createMock(EmailService.class);
        quizAssignmentPersistenceManager = createMock(IQuizAssignmentPersistenceManager.class);

        quizAssignmentManager = new QuizAssignmentManager(quizAssignmentPersistenceManager, emailService, quizManager, groupManager, properties);

        expect(properties.getProperty(HOST_NAME)).andStubReturn("example.com.invalid");

        replay(properties, emailService, quizAssignmentPersistenceManager);
    }

    @Before
    public void initializeAdditionalObjects() {
        newAssignment = new QuizAssignmentDTO(
            null, studentQuiz.getId(),
            teacher.getId(), studentGroup.getId(),
            somePastDate, someFutureDate, null,
            QuizFeedbackMode.OVERALL_MARK);
    }

    @Test
    public void getAssignedQuizzes() throws SegueDatabaseException {
        withMock(quizAssignmentPersistenceManager, m -> {
            expect(m.getAssignmentsByGroupList(studentGroups)).andReturn(teacherAssignmentsToTheirGroups);
        });
        List<QuizAssignmentDTO> assignedQuizzes = quizAssignmentManager.getAssignedQuizzes(student);

        assertEquals(studentAssignments, assignedQuizzes);
    }

    @Test
    public void getActiveQuizAssignments() throws SegueDatabaseException {
        withMock(quizAssignmentPersistenceManager, m -> {
            expect(m.getAssignmentsByGroupList(studentGroups)).andReturn(teacherAssignmentsToTheirGroups);
        });

        List<QuizAssignmentDTO> activeQuizAssignments = quizAssignmentManager.getActiveQuizAssignments(studentQuiz, student);

        assertEquals(Collections.singletonList(studentAssignment), activeQuizAssignments);
    }
}
