/*
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.isaac.api;

import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentStatusDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.DetailedQuizSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.*;


public class QuizFacadeIT extends IsaacIntegrationTest {

    private QuizFacade quizFacade;

    @BeforeEach
    public void setUp() throws Exception {
        // get an instance of the facade to test
        this.quizFacade = new QuizFacade(properties, logManager, contentManager, quizManager, userAccountManager,
                userAssociationManager, groupManager, quizAssignmentManager, assignmentService, quizAttemptManager,
                quizQuestionManager);
    }

    @Test
    public void createQuizAssignmentEndpoint_assignQuizAsTeacher_succeeds() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(assignQuizRequest);

        List<QuizAssignmentDTO> quizAssignmentDTOList = new LinkedList<>();
        quizAssignmentDTOList.add(
                new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID,
                        TEST_TEACHER_ID, TEST_TEACHERS_AB_GROUP_ID, new Date(), DateUtils.addDays(new Date(), 5), null,
                        QuizFeedbackMode.DETAILED_FEEDBACK)
        );

        // Act
        // make request
        Response createQuizResponse = quizFacade.createQuizAssignments(assignQuizRequest, quizAssignmentDTOList);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), createQuizResponse.getStatus());

        // check the quiz was assigned successfully
        List<?> responseBody = (List<?>) createQuizResponse.getEntity();
        AssignmentStatusDTO status = (AssignmentStatusDTO) responseBody.get(0);
        assertEquals(TEST_TEACHERS_AB_GROUP_ID, (long) status.getGroupId());
    }

    @Test
    public void createQuizAssignmentEndpoint_assignQuizAsTutor_fails() throws Exception {
        // Arrange
        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
        replay(assignQuizRequest);

        List<QuizAssignmentDTO> quizAssignmentDTOList = new LinkedList<>();
        quizAssignmentDTOList.add(
                new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID,
                TEST_TUTOR_ID, TEST_TUTORS_AB_GROUP_ID, new Date(), DateUtils.addDays(new Date(), 5), null,
                QuizFeedbackMode.DETAILED_FEEDBACK)
        );

        // Act
        // make request
        Response createQuizResponse = quizFacade.createQuizAssignments(assignQuizRequest, quizAssignmentDTOList);

        // Assert
        // check status code is FORBIDDEN
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizResponse.getStatus());

        // check an error message was returned
        SegueErrorResponse responseBody = (SegueErrorResponse) createQuizResponse.getEntity();
        assertEquals("You do not have the permissions to complete this action", responseBody.getErrorMessage());
    }

    @Test
    public void getAvailableQuizzesEndpoint_getQuizzesAsTeacher_returnsAll() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(assignQuizRequest);

        // Act
        // make request
        Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), assignQuizRequest);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

        // check all quizzes are returned as available
        @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
                (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
        assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_QUIZ_ID)));
        assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)));
        assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)));
    }

    /**
     * Tests that quizzes with hiddenFromRoles=[TUTOR] are not considered available to a tutor.
     */
    @Test
    public void getAvailableQuizzesEndpoint_getQuizzesAsTutor_returnsNoHiddenFromRoleQuizzes() throws Exception {
        // Arrange
        // log in as Tutor, create request
        LoginResult teacherLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(assignQuizRequest);

        // Act
        // make request
        Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), assignQuizRequest);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

        // check hidden-from-tutor-role quizzes are not returned as available
        @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
                (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
        assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_QUIZ_ID)));
        assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)));
        assertFalse(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ITConstants.TEST_TUTOR_EMAIL,
            ITConstants.TEST_TEACHER_EMAIL,
    })
    public void getAvailableQuizzesEndpoint_getQuizzesAsNonStaff_omitsNofilterTaggedQuizzes(String email) throws Exception {
        // Arrange
        // log in, create request
        LoginResult login = loginAs(httpSession, email, "test1234");
        HttpServletRequest availableQuizzesRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(availableQuizzesRequest);

        // Act
        // make request
        Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), availableQuizzesRequest);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

        // check nofilter quizzes are not returned as available
        @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
                (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
        assertFalse(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_NOFILTER_QUIZ_ID)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ITConstants.TEST_EVENTMANAGER_EMAIL,
            ITConstants.TEST_EDITOR_EMAIL
    })
    public void getAvailableQuizzesEndpoint_getQuizzesAsStaff_includesNofilterTaggedQuizzes(String email) throws Exception {
        // Arrange
        // log in, create request
        LoginResult login = loginAs(httpSession, email, "test1234");
        HttpServletRequest availableQuizzesRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(availableQuizzesRequest);

        // Act
        // make request
        Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), availableQuizzesRequest);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

        // check nofilter quizzes are returned as available
        @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
                (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
        assertTrue(responseBody.getResults().stream().anyMatch(q -> q.getId().equals(QUIZ_TEST_NOFILTER_QUIZ_ID)));
    }

    @Test
    public void previewQuizEndpoint_previewInvisibleToStudentQuizAsTeacher_succeeds() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(assignQuizRequest);

        // Act
        // make request
        Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), assignQuizRequest,
                QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

        // check the quiz is returned for preview
        IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
        assertEquals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID, responseBody.getId());
    }

    @Test
    public void previewQuizEndpoint_previewHiddenFromRoleTutorQuizAsTutor_fails() throws Exception {
        // Arrange
        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
        HttpServletRequest assignQuizRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
        replay(assignQuizRequest);

        // Act
        // make request
        Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), assignQuizRequest,
                QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID);

        // Assert
        // check status code is FORBIDDEN
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

        // check an error message was returned
        SegueErrorResponse responseBody = (SegueErrorResponse) previewQuizResponse.getEntity();
        assertEquals("You do not have the permissions to complete this action", responseBody.getErrorMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        ITConstants.TEST_TUTOR_EMAIL,
        ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_EVENTMANAGER_EMAIL,
        ITConstants.TEST_EDITOR_EMAIL
    })
    public void previewQuizEndpoint_previewNofilterQuizAsTutorOrAbove_succeeds(String email) throws Exception {
        // Arrange
        // log in as user, create request
        LoginResult login = loginAs(httpSession, email, "test1234");
        HttpServletRequest previewQuizRequest = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(previewQuizRequest);

        // Act
        // make request
        Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
                QUIZ_TEST_NOFILTER_QUIZ_ID);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

        // check the quiz is returned for preview
        IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
        assertEquals(QUIZ_TEST_NOFILTER_QUIZ_ID, responseBody.getId());
    }

    @Test
    public void viewQuizRubricEndpoint_viewRubricAvailableToRoleStudent_succeeds() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ALICE_STUDENT_EMAIL, ALICE_STUDENT_PASSWORD);
        HttpServletRequest viewQuizRubricRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(viewQuizRubricRequest);

        // Act
        // make request
        Response viewQuizRubricResponse = quizFacade.viewQuizRubric(createNiceMock(Request.class), viewQuizRubricRequest,
                QUIZ_TEST_QUIZ_ID);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), viewQuizRubricResponse.getStatus());

        // check the quiz is returned for preview
        DetailedQuizSummaryDTO responseBody = (DetailedQuizSummaryDTO) viewQuizRubricResponse.getEntity();
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getId());
    }

    @Test
    public void viewQuizRubricEndpoint_viewRubricHiddenFromRoleStudent_fails() throws Exception {
        // Arrange
        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, ALICE_STUDENT_EMAIL, ALICE_STUDENT_PASSWORD);
        HttpServletRequest viewQuizRubricRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(viewQuizRubricRequest);

        // Act
        // make request
        Response viewQuizRubricResponse = quizFacade.viewQuizRubric(createNiceMock(Request.class), viewQuizRubricRequest,
                QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);

        // Assert
        // check status code is FORBIDDEN
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), viewQuizRubricResponse.getStatus());

        // check an error message was returned
        SegueErrorResponse responseBody = (SegueErrorResponse) viewQuizRubricResponse.getEntity();
        assertEquals("This test cannot be attempted freely, so no preview is available.", responseBody.getErrorMessage());
    }
}

