/**
 * Copyright 2022 Matthew Trew
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

package uk.ac.cam.cl.dtg.isaac.api;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHERS_BC_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.INCORRECT_ROLE_ERROR_MESSAGE;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.NOT_LOGGED_IN_ERROR_MESSAGE;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FEEDBACK_MODE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FREE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_BOB_COMPLETE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_BOB_FOR_SET_COMPLETE_TEST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_CANCELLED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_EXPIRED_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_FIRST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_SECOND_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_NON_EXISTENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_SECOND_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_IT_SECONDARY_TEST_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_IT_TEST_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_QUESTION_FIRST_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_QUIZ_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.QUIZ_TEST_UNKNOWN_QUESTION_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_ADMIN_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_NON_EXISTENT_USER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_STUDENT_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHERS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_PASSWORD;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.UNKNOWN_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.UNKNOWN_QUIZ_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_MILLISECONDS_IN_SECOND;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_FIVE_MINUTES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_DAY;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.QuizFeedbackMode;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuizDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizAttemptFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.QuizUserFeedbackDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuizSummaryDTO;

public class QuizFacadeIT extends IsaacIntegrationTest {

  Date someFutureDate =
      new Date(System.currentTimeMillis() + NUMBER_SECONDS_IN_ONE_DAY * NUMBER_MILLISECONDS_IN_SECOND);
  Date somePastDate = new Date(System.currentTimeMillis() - NUMBER_SECONDS_IN_ONE_DAY * NUMBER_MILLISECONDS_IN_SECOND);
  private QuizFacade quizFacade;

  private static String readSegueErrorMessage(Response errorResponse) {
    return errorResponse.readEntity(SegueErrorResponse.class).getErrorMessage();
  }

  private static HttpServletRequest prepareAnonymousRequest() {
    HttpServletRequest getQuizAssignmentRequest = createNiceMock(HttpServletRequest.class);
    replay(getQuizAssignmentRequest);
    return getQuizAssignmentRequest;
  }

  @BeforeEach
  public void setUp() {
    // get an instance of the facade to test
    this.quizFacade =
        new QuizFacade(properties, logManager, contentManager, quizManager, userAccountManager, userAssociationManager,
            groupManager, quizAssignmentManager, assignmentService, quizAttemptManager, quizQuestionManager);
  }

  /**
   * As the integration tests do not currently support MFA login, we cannot use the normal login process and have to
   * create cookies manually when testing admin accounts.
   *
   * @return a Cookie loaded with session information for the test admin user.
   * @throws JsonProcessingException if the cookie serialisation fails
   */
  private Cookie createManualCookieForAdmin() throws JsonProcessingException {
    SimpleDateFormat sessionDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
    String userId = String.valueOf(TEST_ADMIN_ID);
    String hmacKey = properties.getProperty(HMAC_SALT);
    int sessionExpiryTimeInSeconds = NUMBER_SECONDS_IN_FIVE_MINUTES;

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.SECOND, sessionExpiryTimeInSeconds);
    String sessionExpiryDate = sessionDateFormat.format(calendar.getTime());

    Map<String, String> sessionInformation =
        userAuthenticationManager.prepareSessionInformation(userId, "0", sessionExpiryDate, hmacKey, null);
    return userAuthenticationManager.createAuthCookie(sessionInformation, sessionExpiryTimeInSeconds);
  }

  private HttpServletRequest prepareAdminRequest() {
    try {
      Cookie adminSessionCookie = createManualCookieForAdmin();
      HttpServletRequest adminRequest = createRequestWithCookies(new Cookie[] {adminSessionCookie});
      replay(adminRequest);
      return adminRequest;
    } catch (JsonProcessingException e) {
      fail(e);
      return null;
    }
  }

  protected HttpServletRequest prepareUserRequest(String userEmail, String userPassword) {
    try {
      LoginResult userLogin = loginAs(httpSession, userEmail, userPassword);
      HttpServletRequest userRequest = createRequestWithCookies(new Cookie[] {userLogin.cookie});
      replay(userRequest);
      return userRequest;
    } catch (Exception e) {
      fail(e);
      return null;
    }
  }

  private HttpServletRequest prepareTeacherRequest() {
    return prepareUserRequest(TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
  }

  private HttpServletRequest prepareStudentRequest() {
    return prepareUserRequest(TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
  }

  private HttpServletRequest prepareTeacherWhoIsNotGroupManagerRequest() {
    return prepareUserRequest(DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
  }

  private HttpServletRequest prepareTutorRequest() {
    return prepareUserRequest(TEST_TUTOR_EMAIL, TEST_TUTOR_PASSWORD);
  }

  private HttpServletRequest prepareStudentWithAssignmentsRequest() {
    return prepareUserRequest(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_EMAIL,
        QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_PASSWORD);
  }

  private HttpServletRequest prepareStudentWithFreeAttemptRequest() {
    return prepareUserRequest(QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_EMAIL,
        QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_PASSWORD);
  }

  private HttpServletRequest prepareStudentWithNoExistingAttemptsRequest() {
    return prepareUserRequest(QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_EMAIL,
        QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_PASSWORD);
  }

  @Nested
  class GetAvailableQuizzesEndpoint {
    @Test
    public void asAnonymousUser_isUnauthorised() {
      HttpServletRequest getQuizzesRequest = prepareAnonymousRequest();

      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizzesResponse.getStatus());

      String actualErrorMessage = readSegueErrorMessage(getQuizzesResponse);
      assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
    }

    @Test
    public void asTeacher_returnsAllQuizzes() {
      HttpServletRequest getQuizzesRequest = prepareTeacherRequest();

      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
          (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
      boolean isTestQuizPresent = isIdPresent(responseBody, QUIZ_TEST_QUIZ_ID);
      boolean isHiddenFromStudentsQuizPresent = isIdPresent(responseBody, QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);
      boolean isHiddenFromTutorsQuizPresent = isIdPresent(responseBody, QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID);
      assertTrue(isTestQuizPresent);
      assertTrue(isHiddenFromStudentsQuizPresent);
      assertTrue(isHiddenFromTutorsQuizPresent);
    }

    /**
     * Tests that quizzes with visibleToStudents=false and hiddenFromRoles=[TUTOR] are not considered available to a tutor.
     */
    @Test
    public void asTutor_returnsQuizzesNotHiddenFromStudentOrTutorRole() {
      HttpServletRequest getQuizzesRequest = prepareTutorRequest();

      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
          (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
      boolean isTestQuizPresent = isIdPresent(responseBody, QUIZ_TEST_QUIZ_ID);
      boolean isHiddenFromStudentsQuizPresent = isIdPresent(responseBody, QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);
      boolean isHiddenFromTutorsQuizPresent = isIdPresent(responseBody, QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID);
      assertTrue(isTestQuizPresent);
      assertFalse(isHiddenFromStudentsQuizPresent);
      assertFalse(isHiddenFromTutorsQuizPresent);
    }

    @Test
    public void asStudent_returnsQuizzesNotHiddenFromStudentRole() {
      HttpServletRequest getQuizzesRequest = prepareStudentRequest();

      Response getQuizzesResponse = quizFacade.getAvailableQuizzes(createNiceMock(Request.class), getQuizzesRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") ResultsWrapper<QuizSummaryDTO> responseBody =
          (ResultsWrapper<QuizSummaryDTO>) getQuizzesResponse.getEntity();
      boolean isTestQuizPresent = isIdPresent(responseBody, QUIZ_TEST_QUIZ_ID);
      boolean isHiddenFromStudentsQuizPresent = isIdPresent(responseBody, QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID);
      boolean isHiddenFromTutorsQuizPresent = isIdPresent(responseBody, QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID);
      assertTrue(isTestQuizPresent);
      assertFalse(isHiddenFromStudentsQuizPresent);
      assertFalse(isHiddenFromTutorsQuizPresent);
    }

    private boolean isIdPresent(ResultsWrapper<QuizSummaryDTO> responseBody, String quizTestQuizId) {
      return responseBody.getResults().stream().anyMatch(q -> q.getId().equals(quizTestQuizId));
    }
  }

  @Nested
  class GetAssignedQuizzes {
    @Test
    public void asAnonymousUser_isUnauthorised() {
      HttpServletRequest assignedQuizRequest = prepareAnonymousRequest();

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      String actualErrorMessage = readSegueErrorMessage(getAssignedQuizzesResponse);
      assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
    }

    @Test
    public void asStudent_withNoAssignments_returnsEmptyList() {
      HttpServletRequest assignedQuizRequest = prepareStudentRequest();

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
          (List<QuizAssignmentDTO>) getAssignedQuizzesResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }

    @Test
    public void asStudent_withAssignments_returnsListOfAssignments() {
      HttpServletRequest assignedQuizRequest = prepareStudentWithAssignmentsRequest();

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
          (List<QuizAssignmentDTO>) getAssignedQuizzesResponse.getEntity();
      assertEquals(6, responseBody.size());
      boolean isTestQuizPresent = responseBody.stream().anyMatch(q -> q.getQuizId().equals(QUIZ_TEST_QUIZ_ID));
      assertTrue(isTestQuizPresent);
    }

    @Test
    public void asTeacher_withNoAssignments_returnsEmptyList() {
      HttpServletRequest assignedQuizRequest = prepareTeacherRequest();

      Response getAssignedQuizzesResponse = quizFacade.getAssignedQuizzes(assignedQuizRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getAssignedQuizzesResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
          (List<QuizAssignmentDTO>) getAssignedQuizzesResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }
  }

  @Nested
  class GetFreeAttempts {
    @Test
    public void asAnonymousUser_isUnauthorised() {
      HttpServletRequest getFreeAttemptsRequest = prepareAnonymousRequest();

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getFreeAttemptsResponse.getStatus());

      String actualErrorMessage = readSegueErrorMessage(getFreeAttemptsResponse);
      assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
    }

    @Test
    public void asStudent_withNoAttempts_returnsEmptyList() {
      HttpServletRequest getFreeAttemptsRequest = prepareStudentRequest();

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getFreeAttemptsResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAttemptDTO> responseBody =
          (List<QuizAttemptDTO>) getFreeAttemptsResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }

    @Test
    public void asStudent_withOnlyAssignedAttempts_returnsEmptyList() {
      HttpServletRequest getFreeAttemptsRequest = prepareStudentWithAssignmentsRequest();

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getFreeAttemptsResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAttemptDTO> responseBody =
          (List<QuizAttemptDTO>) getFreeAttemptsResponse.getEntity();
      assertTrue(responseBody.isEmpty());
    }

    @Test
    public void asStudent_withFreeAttempts_returnsListOnlyOfFreeAttempts() {
      HttpServletRequest getFreeAttemptsRequest = prepareStudentWithFreeAttemptRequest();

      Response getFreeAttemptsResponse = quizFacade.getFreeAttempts(getFreeAttemptsRequest);

      assertEquals(Response.Status.OK.getStatusCode(), getFreeAttemptsResponse.getStatus());

      @SuppressWarnings("unchecked") List<QuizAttemptDTO> responseBody =
          (List<QuizAttemptDTO>) getFreeAttemptsResponse.getEntity();
      boolean isTestQuizPresent = responseBody.stream().anyMatch(q -> q.getQuizId().equals(QUIZ_TEST_QUIZ_ID));
      boolean anyReturnedQuizHasAssignment = responseBody.stream().anyMatch(q -> q.getQuizAssignmentId() != null);
      assertTrue(isTestQuizPresent);
      assertFalse(anyReturnedQuizHasAssignment);
    }
  }

  @Nested
  class GetQuizAssignment {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizId_isBadRequest() {
        HttpServletRequest getQuizAssignmentRequest = prepareAnonymousRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentResponse);
        assertEquals("You must provide a valid test assignment id.", actualErrorMessage);
      }

      @Test
      public void cancelledAssignment_isBadRequest() {
        HttpServletRequest getQuizAssignmentRequest = prepareTeacherRequest();

        Response getQuizAssignmentResponse =
            quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_CANCELLED_ID);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentResponse);
        assertEquals("This assignment has been cancelled.", actualErrorMessage);
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest getQuizAssignmentRequest = prepareAnonymousRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentResponse);
        assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest getQuizAssignmentRequest = prepareStudentRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTutor_isForbidden() {
        HttpServletRequest getQuizAssignmentRequest = prepareTutorRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTeacher_whoIsNotGroupManager_isForbidden() {
        HttpServletRequest getQuizAssignmentRequest = prepareTeacherWhoIsNotGroupManagerRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentResponse);
        assertEquals("You can only view assignments to groups you own or manage.", actualErrorMessage);
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void asTeacher_whoIsGroupManager_returnsListOfAssignments_withFeedbackOnlyForStudentsAllowingAccess() {
        HttpServletRequest getQuizAssignmentRequest = prepareTeacherRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        QuizAssignmentDTO responseBody = (QuizAssignmentDTO) getQuizAssignmentResponse.getEntity();
        assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getId());
        boolean isUserFeedbackWithAccessAndFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null);
        boolean isUserFeedbackWithNoAccessAndNoFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null);
        boolean isUserFeedbackWithAccessButNoFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null);
        boolean isUserFeedbackWithNoAccessButFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null);
        assertTrue(isUserFeedbackWithAccessAndFeedbackPresent);
        assertTrue(isUserFeedbackWithNoAccessAndNoFeedbackPresent);
        assertFalse(isUserFeedbackWithAccessButNoFeedbackPresent);
        assertFalse(isUserFeedbackWithNoAccessButFeedbackPresent);
      }

      @Test
      public void asAdmin_returnsListOfAssignments_withFeedbackForAllStudents() {
        HttpServletRequest getQuizAssignmentRequest = prepareAdminRequest();

        Response getQuizAssignmentResponse = quizFacade.getQuizAssignment(getQuizAssignmentRequest, QUIZ_ASSIGNMENT_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        QuizAssignmentDTO responseBody = (QuizAssignmentDTO) getQuizAssignmentResponse.getEntity();
        assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getId());
        boolean isUserFeedbackWithAccessAndFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null);
        boolean isUserFeedbackWithNoAccessAndNoFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null);
        boolean isUserFeedbackWithAccessButNoFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> f.getUser().isAuthorisedFullAccess() && f.getFeedback() == null);
        boolean isUserFeedbackWithNoAccessButFeedbackPresent = responseBody.getUserFeedback().stream()
            .anyMatch(f -> !f.getUser().isAuthorisedFullAccess() && f.getFeedback() != null);
        assertTrue(isUserFeedbackWithAccessAndFeedbackPresent);
        assertFalse(isUserFeedbackWithNoAccessAndNoFeedbackPresent);
        assertFalse(isUserFeedbackWithAccessButNoFeedbackPresent);
        assertFalse(isUserFeedbackWithNoAccessButFeedbackPresent);
      }
    }
  }

  @Nested
  class GetQuizAssignmentAttempt {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId_isBadRequest() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareAnonymousRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, null,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("You must provide a valid test assignment and user id id.", actualErrorMessage);
      }

      @Test
      public void missingUserId_isBadRequest() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareAnonymousRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID, null);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("You must provide a valid test assignment and user id id.", actualErrorMessage);
      }

      @Test
      public void cancelledAssignment_isBadRequest() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTeacherRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_CANCELLED_ID,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("This assignment has been cancelled.", actualErrorMessage);
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareAnonymousRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareStudentRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTutor_isForbidden() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTutorRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTeacher_whoIsNotGroupManager_isForbidden() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTeacherWhoIsNotGroupManagerRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("You can only view assignments to groups you own or manage.", actualErrorMessage);
      }
    }

    @Nested
    class CorrectGroupOwnerButInvalidTarget {
      @Test
      public void asTeacher_withTargetStudent_whoIsNotInGroup_isForbidden() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTeacherRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("That student is not in the group that was assigned this test.", actualErrorMessage);
      }

      @Test
      public void asTeacher_withTargetStudentWhoDoesNotGrantViewingPermissions_isForbidden() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTeacherRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("You do not have access to that student's data.", actualErrorMessage);
      }

      @Test
      public void asTeacher_withTargetStudent_whoHasNotCompletedAssignment_isForbidden() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTeacherRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentAttemptResponse);
        assertEquals("That student has not completed this test assignment.", actualErrorMessage);
      }
    }

    @Nested
    class ValidRequest {
      @Test
      public void asTeacher_withTargetStudent_whoHasCompletedAssignment_returnsQuizAttemptFeedback() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareTeacherRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_ID,
                QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        QuizAttemptFeedbackDTO responseBody = (QuizAttemptFeedbackDTO) getQuizAssignmentAttemptResponse.getEntity();
        assertEquals(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID, responseBody.getUser().getId());
        assertEquals(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID, responseBody.getAttempt().getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getAttempt().getQuizId());
      }


      @Test
      public void asAdmin_overridesNoViewingPermissions_returnsQuizAttemptFeedback() {
        HttpServletRequest getQuizAssignmentAttemptRequest = prepareAdminRequest();

        Response getQuizAssignmentAttemptResponse =
            quizFacade.getQuizAssignmentAttempt(getQuizAssignmentAttemptRequest, QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID,
                QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentAttemptResponse.getStatus());

        QuizAttemptFeedbackDTO responseBody = (QuizAttemptFeedbackDTO) getQuizAssignmentAttemptResponse.getEntity();
        assertEquals(QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID, responseBody.getUser().getId());
        assertEquals(QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID,
            responseBody.getAttempt().getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getAttempt().getQuizId());
      }
    }
  }

  @Nested
  class CreateQuizAssignment {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizId_isBadRequest() {
        HttpServletRequest createQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, null, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("A required field was missing. Must provide group and test ids and a test feedback mode.",
              actualErrorMessage);
        }
      }

      @Test
      public void missingGroupId_isBadRequest() {
        HttpServletRequest createQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, null, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("A required field was missing. Must provide group and test ids and a test feedback mode.",
              actualErrorMessage);
        }
      }

      @Test
      public void missingFeedbackMode_isBadRequest() {
        HttpServletRequest createQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate, null);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("A required field was missing. Must provide group and test ids and a test feedback mode.",
              actualErrorMessage);
        }
      }

      @Test
      public void unknownQuizId_isNotFound() {
        HttpServletRequest createQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, UNKNOWN_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("This test has become unavailable.", actualErrorMessage);
        }
      }

      @Test
      public void dueDateInPast_isBadRequest() {
        HttpServletRequest createQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, somePastDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("You cannot set a quiz with a due date in the past.", actualErrorMessage);
        }
      }

      @Test
      public void duplicateActiveAssignment_isBadRequest() {
        HttpServletRequest createQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, QUIZ_FACADE_IT_TEST_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("You cannot reassign a test until the due date has passed.", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest createQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest createQuizAssignmentRequest = prepareStudentRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTutor_isForbidden() {
        HttpServletRequest createQuizAssignmentRequest = prepareTutorRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_whoIsNotGroupManager_isForbidden() {
        HttpServletRequest createQuizAssignmentRequest = prepareTeacherWhoIsNotGroupManagerRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(createQuizAssignmentResponse);
          assertEquals("You can only set assignments to groups you own or manage.", actualErrorMessage);
        }
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void asTeacher_whoIsGroupManager_returnsQuizAssignment() {
        HttpServletRequest createQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, TEST_TEACHERS_AB_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.OK.getStatusCode(), createQuizAssignmentResponse.getStatus());

          QuizAssignmentDTO responseBody = (QuizAssignmentDTO) createQuizAssignmentResponse.getEntity();
          assertNotNull(responseBody.getId());
          assertEquals(TEST_TEACHER_ID, responseBody.getOwnerUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(TEST_TEACHERS_AB_GROUP_ID, responseBody.getGroupId());
          assertNotNull(responseBody.getCreationDate());
          assertEquals(someFutureDate, responseBody.getDueDate());
        }
      }

      @Test
      public void asAdmin_returnsQuizAssignment() {
        HttpServletRequest createQuizAssignmentRequest = prepareAdminRequest();
        QuizAssignmentDTO assignmentRequest =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, DAVE_TEACHERS_BC_GROUP_ID, null, someFutureDate,
                QuizFeedbackMode.OVERALL_MARK);

        try (Response createQuizAssignmentResponse = quizFacade.createQuizAssignment(createQuizAssignmentRequest,
            assignmentRequest)) {

          assertEquals(Response.Status.OK.getStatusCode(), createQuizAssignmentResponse.getStatus());

          QuizAssignmentDTO responseBody = (QuizAssignmentDTO) createQuizAssignmentResponse.getEntity();
          assertNotNull(responseBody.getId());
          assertEquals(TEST_ADMIN_ID, responseBody.getOwnerUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(DAVE_TEACHERS_BC_GROUP_ID, responseBody.getGroupId());
          assertNotNull(responseBody.getCreationDate());
          assertEquals(someFutureDate, responseBody.getDueDate());
        }
      }
    }
  }

  @Nested
  class CancelQuizAssignment {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId_isBadRequest() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareAnonymousRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAssignmentResponse);
          assertEquals("You must provide a valid test assignment id.", actualErrorMessage);
        }
      }

      @Test
      public void cancelledAssignment_isBadRequest() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareTeacherRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_CANCELLED_ID)) {


          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAssignmentResponse);
          assertEquals("This assignment is already cancelled.", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareAnonymousRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAssignmentResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareStudentRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAssignmentResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTutor_isForbidden() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareTutorRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAssignmentResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_whoIsNotGroupManager_isForbidden() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareTeacherWhoIsNotGroupManagerRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAssignmentResponse);
          assertEquals("You can only cancel assignments to groups you own or manage.", actualErrorMessage);
        }
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void asTeacher_whoIsGroupManager_returnsNoContent() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareTeacherRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_FIRST_ID)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertNull(cancelQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void asAdmin_returnsNoContent() {
        HttpServletRequest cancelQuizAssignmentRequest = prepareTeacherRequest();

        try (Response cancelQuizAssignmentResponse = quizFacade.cancelQuizAssignment(cancelQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_FOR_CANCELLATION_TEST_SECOND_ID)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelQuizAssignmentResponse.getStatus());

          assertNull(cancelQuizAssignmentResponse.getEntity());
        }
      }
    }
  }

  @Nested
  class UpdateQuizAssignment {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest, null,
            quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("You must provide a valid test assignment id.", actualErrorMessage);
        }
      }

      @Test
      public void changingQuizAssignmentId_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(QUIZ_ASSIGNMENT_ID, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("Those fields are not editable.", actualErrorMessage);
        }
      }

      @Test
      public void changingQuizId_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, QUIZ_TEST_QUIZ_ID, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("Those fields are not editable.", actualErrorMessage);
        }
      }

      @Test
      public void changingGroupId_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, 1L, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("Those fields are not editable.", actualErrorMessage);
        }
      }

      @Test
      public void changingOwnerId_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, 1L, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("Those fields are not editable.", actualErrorMessage);
        }
      }

      @Test
      public void changingCreationDateId_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, new Date(), null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("Those fields are not editable.", actualErrorMessage);
        }
      }

      @Test
      public void settingDueDate_withDateInThePast_isForbidden() {
        HttpServletRequest updateQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, somePastDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("You can only extend due dates into the future.", actualErrorMessage);
        }
      }

      @Test
      public void cancelledAssignment_isBadRequest() {
        HttpServletRequest updateQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, someFutureDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_CANCELLED_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("This assignment is already cancelled.", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAnonymousRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest updateQuizAssignmentRequest = prepareStudentRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTutor_isForbidden() {
        HttpServletRequest updateQuizAssignmentRequest = prepareTutorRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_whoisNotGroupManager_isForbidden() {
        HttpServletRequest updateQuizAssignmentRequest = prepareTeacherWhoIsNotGroupManagerRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, null, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(updateQuizAssignmentResponse);
          assertEquals("You can only updates assignments to groups you own or manage.", actualErrorMessage);
        }
      }
    }

    @Nested
    class ValidUser {
      @Test
      public void asTeacher_whoIs_groupManager_settingDueDate_withValidDate_returnsNoContent() {
        HttpServletRequest updateQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, someFutureDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void asTeacher_whoIs_groupManager_settingFeedbackMode_returnsNoContent() {
        HttpServletRequest updateQuizAssignmentRequest = prepareTeacherRequest();
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void asAdmin_settingDueDate_withValidDate_returnsNoContent() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAdminRequest();
        QuizAssignmentDTO quizAssignmentDto = new QuizAssignmentDTO(null, null, null, null, null, someFutureDate, null);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }

      @Test
      public void asAdmin_settingFeedbackMode_returnsNoContent() {
        HttpServletRequest updateQuizAssignmentRequest = prepareAdminRequest();
        QuizAssignmentDTO quizAssignmentDto =
            new QuizAssignmentDTO(null, null, null, null, null, null, QuizFeedbackMode.OVERALL_MARK);

        try (Response updateQuizAssignmentResponse = quizFacade.updateQuizAssignment(updateQuizAssignmentRequest,
            QUIZ_ASSIGNMENT_ID, quizAssignmentDto)) {

          assertEquals(Response.Status.NO_CONTENT.getStatusCode(), updateQuizAssignmentResponse.getStatus());

          assertNull(updateQuizAssignmentResponse.getEntity());
        }
      }
    }
  }

  @Nested
  class PreviewQuiz {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void nullQuizId_isBadRequest() {
        HttpServletRequest previewQuizRequest = prepareTeacherRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals("You must provide a valid test id.", actualErrorMessage);
        }
      }

      @Test
      public void emptyQuizId_isBadRequest() {
        HttpServletRequest previewQuizRequest = prepareTeacherRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            "")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals("You must provide a valid test id.", actualErrorMessage);
        }
      }

      @Test
      public void unknownQuizId_isNotFound() {
        HttpServletRequest previewQuizRequest = prepareTeacherRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            UNKNOWN_QUIZ_ID)) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals("This test has become unavailable.", actualErrorMessage);
        }
      }
    }

    @Nested
    class StandardQuizPermissions {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest previewQuizRequest = prepareAnonymousRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest previewQuizRequest = prepareStudentRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTutor_returnsQuiz() {
        HttpServletRequest previewQuizRequest = prepareTutorRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getId());
        }
      }

      @Test
      public void asTeacher_returnsQuiz() {
        HttpServletRequest previewQuizRequest = prepareTeacherRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getId());
        }
      }
    }

    @Nested
    class RestrictedQuizPermissions {
      @Test
      public void asTutor_withQuizThatIsHiddenFromStudentRole_isForbidden() {
        HttpServletRequest previewQuizRequest = prepareTutorRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTutor_withQuizThatIsHiddenFromTutorRole_isForbidden() {
        HttpServletRequest previewQuizRequest = prepareTutorRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), previewQuizResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(previewQuizResponse);
          assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_withQuizThatIsHiddenFromStudentRole_returnsQuiz() {
        HttpServletRequest previewQuizRequest = prepareTeacherRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID, responseBody.getId());
        }
      }

      @Test
      public void asTeacher_withQuizThatIsHiddenFromTutorRole_returnsQuiz() {
        HttpServletRequest previewQuizRequest = prepareTeacherRequest();

        try (Response previewQuizResponse = quizFacade.previewQuiz(createNiceMock(Request.class), previewQuizRequest,
            QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), previewQuizResponse.getStatus());

          IsaacQuizDTO responseBody = (IsaacQuizDTO) previewQuizResponse.getEntity();
          assertEquals(QUIZ_HIDDEN_FROM_ROLE_TUTORS_QUIZ_ID, responseBody.getId());
        }
      }
    }
  }

  @Nested
  class StartQuizAttempt {
    @Test
    public void asStudent_withValidRequest_returnsQuizAttempt() {
      HttpServletRequest startQuizAttemptRequest = prepareStudentWithFreeAttemptRequest();

      try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
          startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), startQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) startQuizAttemptResponse.getEntity();
        assertEquals(QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getQuizAssignmentId());
        assertNotNull(responseBody.getQuiz());
        assertNotNull(responseBody.getQuizAssignment());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest startQuizAttemptRequest = prepareAnonymousRequest();

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), startQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startQuizAttemptResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotInGroup_isForbidden() {
        HttpServletRequest startQuizAttemptRequest = prepareStudentRequest();

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startQuizAttemptResponse);
          assertEquals("You are not a member of a group to which this test is assigned.", actualErrorMessage);
        }
      }
    }

    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId_isBadRequest() {
        HttpServletRequest startQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), startQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startQuizAttemptResponse);
          assertEquals("You must provide a valid test assignment id.", actualErrorMessage);
        }
      }

      @Test
      public void cancelledAssignment_isGone() {
        HttpServletRequest startQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_CANCELLED_ID)) {

          assertEquals(Response.Status.GONE.getStatusCode(), startQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startQuizAttemptResponse);
          assertEquals("This test assignment has been cancelled.", actualErrorMessage);
        }
      }

      @Test
      public void expiredAssignment_isForbidden() {
        HttpServletRequest startQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_EXPIRED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startQuizAttemptResponse);
          assertEquals("The due date for this test has passed.", actualErrorMessage);
        }
      }

      @Test
      public void alreadyAttemptedAssignment_isForbidden() {
        HttpServletRequest startQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response startQuizAttemptResponse = quizFacade.startQuizAttempt(createNiceMock(Request.class),
            startQuizAttemptRequest, QUIZ_ASSIGNMENT_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startQuizAttemptResponse);
          assertEquals("You have already completed your attempt for this test.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class StartFreeQuizAttempt {
    @Test
    public void asStudent_withValidRequest_returnsQuizAttempt() {
      HttpServletRequest startFreeQuizAttemptRequest = prepareStudentWithNoExistingAttemptsRequest();

      try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
          startFreeQuizAttemptRequest, QUIZ_TEST_QUIZ_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) startFreeQuizAttemptResponse.getEntity();
        assertEquals(QUIZ_FACADE_TEST_STUDENT_5_WITH_NO_EXISTING_ATTEMPTS_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertNull(responseBody.getQuizAssignmentId());
        assertNotNull(responseBody.getQuiz());
        assertNull(responseBody.getQuizAssignment());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest startFreeQuizAttemptRequest = prepareAnonymousRequest();

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startFreeQuizAttemptResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_withQuizThatIsHiddenFromStudentRole() {
        HttpServletRequest startFreeQuizAttemptRequest = prepareStudentWithNoExistingAttemptsRequest();

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, QUIZ_HIDDEN_FROM_ROLE_STUDENTS_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startFreeQuizAttemptResponse);
          assertEquals("Free attempts are not available for test quiz.", actualErrorMessage);
        }
      }

      @Test
      public void asStudent_withQuizThatHasBeenAssigned_isForbidden() {
        HttpServletRequest startFreeQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, QUIZ_TEST_QUIZ_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startFreeQuizAttemptResponse);
          String expectedErrorMessage = "This test has been assigned to you by a teacher. "
              + "You can not attempt this test freely. If you have already done the test for your teacher, "
              + "and want to do it again, ask your teacher to allow you another attempt.";
          assertEquals(expectedErrorMessage, actualErrorMessage);
        }
      }
    }

    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizId_isBadRequest() {
        HttpServletRequest startFreeQuizAttemptRequest = prepareStudentWithNoExistingAttemptsRequest();

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startFreeQuizAttemptResponse);
          assertEquals("You must provide a valid test id.", actualErrorMessage);
        }
      }

      @Test
      public void emptyQuizId_isBadRequest() {
        HttpServletRequest startFreeQuizAttemptRequest = prepareStudentWithNoExistingAttemptsRequest();

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, "")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startFreeQuizAttemptResponse);
          assertEquals("You must provide a valid test id.", actualErrorMessage);
        }
      }

      @Test
      public void unknownQuizId_isNotFound() {
        HttpServletRequest startFreeQuizAttemptRequest = prepareStudentWithNoExistingAttemptsRequest();

        try (Response startFreeQuizAttemptResponse = quizFacade.startFreeQuizAttempt(createNiceMock(Request.class),
            startFreeQuizAttemptRequest, UNKNOWN_QUIZ_ID)) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), startFreeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(startFreeQuizAttemptResponse);
          assertEquals("This test has become unavailable.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class GetQuizAttempt {
    @Test
    public void validRequest_returnsQuizAttempt() {
      HttpServletRequest getQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

      try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptResponse.getEntity();
        assertEquals(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertEquals(QUIZ_ASSIGNMENT_SECOND_ID, responseBody.getQuizAssignmentId());
        assertNotNull(responseBody.getQuiz());
        assertNotNull(responseBody.getQuizAssignment());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest getQuizAttemptRequest = prepareAnonymousRequest();

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotAttemptCreator_isForbidden() {
        HttpServletRequest getQuizAttemptRequest = prepareStudentWithFreeAttemptRequest();

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptResponse);
          assertEquals("This is not your test attempt.", actualErrorMessage);
        }
      }
    }

    @Nested
    class MissingOrInvalidData {
      @Test
      public void cancelledAssignment_isForbidden() {
        HttpServletRequest getQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptResponse);
          assertEquals("This test assignment has been cancelled.", actualErrorMessage);
        }
      }

      @Test
      public void expiredAssignment_isForbidden() {
        HttpServletRequest getQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptResponse);
          assertEquals("The due date for this test has passed.", actualErrorMessage);
        }
      }

      @Test
      public void alreadyCompletedAttempt_isForbidden() {
        HttpServletRequest getQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptResponse = quizFacade.getQuizAttempt(getQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptResponse);
          assertEquals("You have completed this test.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class GetQuizAttemptFeedback {
    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareAnonymousRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptFeedbackResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotAttemptCreator_isForbidden() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithFreeAttemptRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptFeedbackResponse);
          assertEquals("This is not your test attempt.", actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_isForbidden() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareTeacherRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptFeedbackResponse);
          assertEquals("This is not your test attempt.", actualErrorMessage);
        }
      }

      @Test
      public void asAdmin_isForbidden() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareAdminRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptFeedbackResponse);
          assertEquals("This is not your test attempt.", actualErrorMessage);
        }
      }
    }

    @Nested
    class IncompleteAttempt {
      @Test
      public void expiredAssignment_isForbidden() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptFeedbackResponse);
          assertEquals("You have not completed this test.", actualErrorMessage);
        }
      }

      @Test
      public void incompleteAttempt_isForbidden() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(getQuizAttemptFeedbackResponse);
          assertEquals("You have not completed this test.", actualErrorMessage);
        }
      }
    }

    @Nested
    class CompletedAttempt {
      @Test
      public void completedAttempt_withAssignmentWithNoDueDate_returnsQuizAttempt() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(QUIZ_ASSIGNMENT_ID, responseBody.getQuizAssignmentId());
          assertNull(responseBody.getQuiz());
          assertNotNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.NONE, responseBody.getFeedbackMode());
        }
      }

      @Test
      public void completedAttempt_withAssignmentWithDueDate_returnsQuizAttempt() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithFreeAttemptRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_BOB_COMPLETE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(QUIZ_ASSIGNMENT_EXPIRED_ID, responseBody.getQuizAssignmentId());
          assertNull(responseBody.getQuiz());
          assertNotNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.NONE, responseBody.getFeedbackMode());
        }
      }

      @Test
      public void completedAttempt_returnsQuizAttempt_withAppropriateFeedbackMode() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithAssignmentsRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FEEDBACK_MODE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertEquals(QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID, responseBody.getQuizAssignmentId());
          assertNotNull(responseBody.getQuiz());
          assertNotNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.DETAILED_FEEDBACK, responseBody.getFeedbackMode());
        }
      }

      @Test
      public void completedAttempt_withNoAssignment_returnsQuizAttempt() {
        HttpServletRequest getQuizAttemptFeedbackRequest = prepareStudentWithFreeAttemptRequest();

        try (Response getQuizAttemptFeedbackResponse = quizFacade.getQuizAttemptFeedback(getQuizAttemptFeedbackRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), getQuizAttemptFeedbackResponse.getStatus());

          QuizAttemptDTO responseBody = (QuizAttemptDTO) getQuizAttemptFeedbackResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID, responseBody.getUserId());
          assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
          assertNull(responseBody.getQuizAssignmentId());
          assertNotNull(responseBody.getQuiz());
          assertNull(responseBody.getQuizAssignment());
          assertEquals(QuizFeedbackMode.DETAILED_FEEDBACK, responseBody.getFeedbackMode());
        }
      }
    }
  }

  @Nested
  class CompleteQuizAttempt {
    @Test
    public void alreadyCompletedAttempt_isForbidden() {
      HttpServletRequest completeQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

      try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID)) {

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(completeQuizAttemptResponse);
        assertEquals("That test is already complete.", actualErrorMessage);
      }
    }

    @Test
    public void validRequest_returnsQuizAttempt() {
      HttpServletRequest completeQuizAttemptRequest = prepareStudentWithFreeAttemptRequest();

      try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_BOB_FOR_SET_COMPLETE_TEST_ID)) {

        assertEquals(Response.Status.OK.getStatusCode(), completeQuizAttemptResponse.getStatus());

        QuizAttemptDTO responseBody = (QuizAttemptDTO) completeQuizAttemptResponse.getEntity();
        assertEquals(QUIZ_FACADE_TEST_STUDENT_3_WITH_FREE_ATTEMPT_ID, responseBody.getUserId());
        assertEquals(QUIZ_TEST_QUIZ_ID, responseBody.getQuizId());
        assertEquals(QUIZ_ASSIGNMENT_FEEDBACK_MODE_ID, responseBody.getQuizAssignmentId());
        assertNull(responseBody.getQuiz());
        assertNull(responseBody.getQuizAssignment());
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest completeQuizAttemptRequest = prepareAnonymousRequest();

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), completeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(completeQuizAttemptResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotAttemptCreator_isForbidden() {
        HttpServletRequest completeQuizAttemptRequest = prepareStudentWithFreeAttemptRequest();

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(completeQuizAttemptResponse);
          assertEquals("You cannot complete someone else's test.", actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_isForbidden() {
        HttpServletRequest completeQuizAttemptRequest = prepareTeacherRequest();

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(completeQuizAttemptResponse);
          assertEquals("You cannot complete someone else's test.", actualErrorMessage);
        }
      }

      @Test
      public void asAdmin_isForbidden() {
        HttpServletRequest completeQuizAttemptRequest = prepareAdminRequest();

        try (Response completeQuizAttemptResponse = quizFacade.completeQuizAttempt(completeQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), completeQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(completeQuizAttemptResponse);
          assertEquals("You cannot complete someone else's test.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class MarkIncompleteQuizAttempt {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizAssignmentId_isBadRequest() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareAnonymousRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, null, QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("Missing quizAssignmentId.", actualErrorMessage);
        }
      }

      @Test
      public void missingUserId_isBadRequest() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareAnonymousRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("Missing userId.", actualErrorMessage);
        }
      }

      @Test
      public void unknownUserId_isBadRequest() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, TEST_NON_EXISTENT_USER_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("No such user.", actualErrorMessage);
        }
      }

      @Test
      public void cancelledAssignment_isForbidden() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_CANCELLED_ID,
            QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("This test assignment has been cancelled.", actualErrorMessage);
        }
      }

      @Test
      public void expiredAssignment_isForbidden() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_EXPIRED_ID,
            QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("You cannot mark a test attempt as incomplete after the due date.", actualErrorMessage);
        }
      }

      @Test
      public void targetUserIsNotInAssignmentGroup_isBadRequest() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, TEST_STUDENT_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("That user is not in this group.", actualErrorMessage);
        }
      }

      @Test
      public void attemptIsAlreadyIncomplete_isBadRequest() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_SECOND_ID,
            QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("That test is already incomplete.", actualErrorMessage);
        }
      }

      @Test
      public void unknownQuizAssignmentId_isInternalServerError() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_NON_EXISTENT_ID,
            QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
              markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("SegueDatabaseException whilst marking test attempt incomplete", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareAnonymousRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_isForbidden() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareStudentRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("You can only mark assignments incomplete for groups you own or manage.", actualErrorMessage);
        }
      }

      @Test
      public void asTutorIsForbidden() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTutorRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("You can only mark assignments incomplete for groups you own or manage.", actualErrorMessage);
        }
      }

      @Test
      public void asTeacher_whoIsNotGroupManager_IsForbidden() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherWhoIsNotGroupManagerRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID, QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(markIncompleteQuizAttemptResponse);
          assertEquals("You can only mark assignments incomplete for groups you own or manage.", actualErrorMessage);
        }
      }
    }

    @Nested
    class ValidRequest {
      @Test
      public void asTeacher_whoIsGroupManager_returnsQuizUserFeedback() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID,
            QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          QuizUserFeedbackDTO responseBody = (QuizUserFeedbackDTO) markIncompleteQuizAttemptResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_2_WITH_ASSIGNMENTS_ID, responseBody.getUser().getId());
          assertNotNull(responseBody.getFeedback());
        }
      }

      @Test
      public void asTeacher_withTargetStudentWhoDoesNotGrantViewingPermissions_returnsQuizUserFeedback_withNullFeedback() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareTeacherRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_SET_INCOMPLETE_TEST_ID,
            QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          QuizUserFeedbackDTO responseBody = (QuizUserFeedbackDTO) markIncompleteQuizAttemptResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID,
              responseBody.getUser().getId());
          assertNull(responseBody.getFeedback());
        }
      }

      @Test
      public void asAdmin_overridesNoViewingPermissions_returnsQuizUserFeedback() {
        HttpServletRequest markIncompleteQuizAttemptRequest = prepareAdminRequest();

        try (Response markIncompleteQuizAttemptResponse = quizFacade.markIncompleteQuizAttempt(
            markIncompleteQuizAttemptRequest, QUIZ_ASSIGNMENT_ID,
            QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID)) {

          assertEquals(Response.Status.OK.getStatusCode(), markIncompleteQuizAttemptResponse.getStatus());

          QuizUserFeedbackDTO responseBody = (QuizUserFeedbackDTO) markIncompleteQuizAttemptResponse.getEntity();
          assertEquals(QUIZ_FACADE_TEST_STUDENT_4_RESTRICTED_VIEWING_FOR_TEST_TEACHER_ID,
              responseBody.getUser().getId());
          assertNotNull(responseBody.getFeedback());
        }
      }
    }
  }

  @Nested
  class AnswerQuestion {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void nullAnswer_isBadRequest() {
        HttpServletRequest answerQuestionRequest = prepareAnonymousRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("No answer received.", actualErrorMessage);
        }
      }

      @Test
      public void emptyAnswer_isBadRequest() {
        HttpServletRequest answerQuestionRequest = prepareAnonymousRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", "")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("No answer received.", actualErrorMessage);
        }
      }

      @Test
      public void nullQuestionId_isBadRequest() {
        HttpServletRequest answerQuestionRequest = prepareAnonymousRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, null, "answer")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("Missing questionId.", actualErrorMessage);
        }
      }

      @Test
      public void emptyQuestionId_isBadRequest() {
        HttpServletRequest answerQuestionRequest = prepareAnonymousRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "", "answer")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("Missing questionId.", actualErrorMessage);
        }
      }

      @Test
      public void unknownQuestionId_isNotFound() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithAssignmentsRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, QUIZ_TEST_UNKNOWN_QUESTION_ID, "answer")) {

          assertEquals(Response.Status.NOT_FOUND.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          String expectedErrorMessage = "No question object found for given id: " + QUIZ_TEST_UNKNOWN_QUESTION_ID;
          assertEquals(expectedErrorMessage, actualErrorMessage);
        }
      }

      @Test
      public void questionIdIsNotInQuiz_isBadRequest() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithAssignmentsRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, QUIZ_TEST_HIDDEN_FROM_TUTORS_QUESTION_FIRST_ID, "answer")) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("This question is part of another test.", actualErrorMessage);
        }
      }

      @Test
      public void completedAttempt_isForbidden() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithAssignmentsRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("You have completed this test.", actualErrorMessage);
        }
      }

      @Test
      public void cancelledAssignment_isForbidden() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithAssignmentsRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("This test assignment has been cancelled.", actualErrorMessage);
        }
      }

      @Test
      public void expiredAssignment_isForbidden() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithAssignmentsRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("The due date for this test has passed.", actualErrorMessage);
        }
      }

      @Test
      public void unknownAssignmentId_isInternalServerError() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithAssignmentsRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_NON_EXISTENT_ID, QUIZ_TEST_QUESTION_FIRST_ID, "answer")) {

          assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("SegueDatabaseException whilst submitting test answer", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest answerQuestionRequest = prepareAnonymousRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", "answer")) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotAttemptCreator_isForbidden() {
        HttpServletRequest answerQuestionRequest = prepareStudentWithFreeAttemptRequest();

        try (Response answerQuestionResponse = quizFacade.answerQuestion(answerQuestionRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, "questionId", "answer")) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), answerQuestionResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(answerQuestionResponse);
          assertEquals("This is not your test attempt.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class AbandonQuizAttempt {
    @Test
    public void validRequest_returnsNoContent() {
      HttpServletRequest cancelQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

      try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_FREE_ID)) {

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), cancelQuizAttemptResponse.getStatus());

        assertNull(cancelQuizAttemptResponse.getEntity());
      }
    }

    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingQuizAttemptId_isBadRequest() {
        HttpServletRequest cancelQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAttemptResponse);
          assertEquals("You must provide a valid test attempt id.", actualErrorMessage);
        }
      }

      @Test
      public void assignedAttempt_isForbidden() {
        HttpServletRequest cancelQuizAttemptRequest = prepareStudentWithAssignmentsRequest();

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAttemptResponse);
          assertEquals("You can only cancel attempts on tests you chose to take.", actualErrorMessage);
        }
      }

      @Test
      public void completedAttempt_isForbidden() {
        HttpServletRequest cancelQuizAttemptRequest = prepareStudentWithFreeAttemptRequest();

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_BOB_FREE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAttemptResponse);
          assertEquals("You cannot cancel completed test attempts.", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest cancelQuizAttemptRequest = prepareAnonymousRequest();

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAttemptResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotAttemptCreator_isForbidden() {
        HttpServletRequest cancelQuizAttemptRequest = prepareStudentWithFreeAttemptRequest();

        try (Response cancelQuizAttemptResponse = quizFacade.abandonQuizAttempt(cancelQuizAttemptRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), cancelQuizAttemptResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(cancelQuizAttemptResponse);
          assertEquals("You cannot cancel a test attempt for someone else.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class LogQuizSectionView {
    @Test
    public void validRequest_returnsNoContent() {
      HttpServletRequest logQuizSectionViewRequest = prepareStudentWithAssignmentsRequest();

      try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
          QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, 1)) {

        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), logQuizSectionViewResponse.getStatus());

        assertNull(logQuizSectionViewResponse.getEntity());
      }
    }

    @Nested
    class MissingOrInvalidData {
      @Test
      public void missingSectionNumber_isBadRequest() {
        HttpServletRequest logQuizSectionViewRequest = prepareAnonymousRequest();

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, null)) {

          assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), logQuizSectionViewResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(logQuizSectionViewResponse);
          assertEquals("Missing sectionNumber.", actualErrorMessage);
        }
      }

      @Test
      public void cancelledAssignment_isForbidden() {
        HttpServletRequest logQuizSectionViewRequest = prepareStudentWithAssignmentsRequest();

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_CANCELLED_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(logQuizSectionViewResponse);
          assertEquals("This test assignment has been cancelled.", actualErrorMessage);
        }
      }

      @Test
      public void expiredAssignment_isForbidden() {
        HttpServletRequest logQuizSectionViewRequest = prepareStudentWithAssignmentsRequest();

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_EXPIRED_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(logQuizSectionViewResponse);
          assertEquals("The due date for this test has passed.", actualErrorMessage);
        }
      }

      @Test
      public void alreadyCompletedAttempt_isForbidden() {
        HttpServletRequest logQuizSectionViewRequest = prepareStudentWithAssignmentsRequest();

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_COMPLETE_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(logQuizSectionViewResponse);
          assertEquals("You have completed this test.", actualErrorMessage);
        }
      }
    }

    @Nested
    class UnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_isUnauthorised() {
        HttpServletRequest logQuizSectionViewRequest = prepareAnonymousRequest();

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, 1)) {

          assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), logQuizSectionViewResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(logQuizSectionViewResponse);
          assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
        }
      }

      @Test
      public void asStudent_whoIsNotAttemptCreator_isForbidden() {
        HttpServletRequest logQuizSectionViewRequest = prepareStudentWithFreeAttemptRequest();

        try (Response logQuizSectionViewResponse = quizFacade.logQuizSectionView(logQuizSectionViewRequest,
            QUIZ_ASSIGNMENT_ATTEMPT_ALICE_INCOMPLETE_ID, 1)) {

          assertEquals(Response.Status.FORBIDDEN.getStatusCode(), logQuizSectionViewResponse.getStatus());

          String actualErrorMessage = readSegueErrorMessage(logQuizSectionViewResponse);
          assertEquals("This is not your test attempt.", actualErrorMessage);
        }
      }
    }
  }

  @Nested
  class GetQuizAssignments {
    @Nested
    class MissingOrInvalidData {
      @Test
      public void unknownGroupId_isInternalServerError() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTeacherRequest();

        Response getQuizAssignmentsResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, UNKNOWN_GROUP_ID);

        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals("Database error whilst getting assigned tests", actualErrorMessage);
      }
    }

    @Nested
    class UnspecifiedGroupUnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_withNullGroupId_isUnauthorised() {
        HttpServletRequest getQuizAssignmentsRequest = prepareAnonymousRequest();

        Response getQuizAssignmentsResponse = quizFacade.getQuizAssignments(getQuizAssignmentsRequest, null);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asStudent_withNullGroupId_isForbidden() {
        HttpServletRequest getQuizAssignmentsRequest = prepareStudentRequest();

        Response getQuizAssignmentsResponse = quizFacade.getQuizAssignments(getQuizAssignmentsRequest, null);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTutor_withNullGroupId_isForbidden() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTutorRequest();

        Response getQuizAssignmentsResponse = quizFacade.getQuizAssignments(getQuizAssignmentsRequest, null);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }
    }

    @Nested
    class SpecifiedGroupUnauthorisedOrForbiddenUser {
      @Test
      public void asAnonymousUser_withNonNullGroupId_isUnauthorised() {
        HttpServletRequest getQuizAssignmentsRequest = prepareAnonymousRequest();

        Response getQuizAssignmentsResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_TEST_GROUP_ID);

        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals(NOT_LOGGED_IN_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asStudent_withNonNullGroupId_isForbidden() {
        HttpServletRequest getQuizAssignmentsRequest = prepareStudentRequest();

        Response getQuizAssignmentsResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_TEST_GROUP_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTutor_withNonNullGroupId_isForbidden() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTutorRequest();

        Response getQuizAssignmentsResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_TEST_GROUP_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals(INCORRECT_ROLE_ERROR_MESSAGE, actualErrorMessage);
      }

      @Test
      public void asTeacher_whoIsNotGroupManager_withNonNullGroupId_isForbidden() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTeacherWhoIsNotGroupManagerRequest();

        Response getQuizAssignmentsResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_TEST_GROUP_ID);

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        String actualErrorMessage = readSegueErrorMessage(getQuizAssignmentsResponse);
        assertEquals("You are not the owner or manager of that group", actualErrorMessage);
      }
    }

    @Nested
    class UnspecifiedGroupValidRequest {
      @Test
      public void asTeacher_withNullGroupId_withAssignments_returnsListOfQuizAssignmentsForAllManagedGroups() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTeacherRequest();

        Response getQuizAssignmentsResponse = quizFacade.getQuizAssignments(getQuizAssignmentsRequest, null);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
            (List<QuizAssignmentDTO>) getQuizAssignmentsResponse.getEntity();
        assertEquals(7, responseBody.size());
      }

      @Test
      public void asTeacher_withNullGroupId_withoutAssignments_returnsEmptyList() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTeacherWhoIsNotGroupManagerRequest();

        Response getQuizAssignmentsResponse = quizFacade.getQuizAssignments(getQuizAssignmentsRequest, null);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
            (List<QuizAssignmentDTO>) getQuizAssignmentsResponse.getEntity();
        assertTrue(responseBody.isEmpty());
      }
    }

    @Nested
    class SpecifiedGroupValidRequest {
      @Test
      public void asTeacher_whoIsGroupManager_withNonNullGroupId_returnsListOfQuizAssignmentsForGroup() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTeacherRequest();

        Response getQuizAssignmentsResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_TEST_GROUP_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentsResponse.getStatus());

        @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
            (List<QuizAssignmentDTO>) getQuizAssignmentsResponse.getEntity();
        assertEquals(6, responseBody.size());
      }

      @Test
      public void asTeacher_whoIsGroupManager_withOtherNonNullGroupId_returnsListOfQuizAssignmentsForOtherGroup() {
        HttpServletRequest getQuizAssignmentsRequest = prepareTeacherRequest();

        Response getQuizAssignmentResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_SECONDARY_TEST_GROUP_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
            (List<QuizAssignmentDTO>) getQuizAssignmentResponse.getEntity();
        assertEquals(1, responseBody.size());
      }

      @Test
      public void asAdmin_withNonNullGroupId_returnsListOfQuizAssignmentsForGroup() {
        HttpServletRequest getQuizAssignmentsRequest = prepareAdminRequest();

        Response getQuizAssignmentResponse =
            quizFacade.getQuizAssignments(getQuizAssignmentsRequest, QUIZ_FACADE_IT_TEST_GROUP_ID);

        assertEquals(Response.Status.OK.getStatusCode(), getQuizAssignmentResponse.getStatus());

        @SuppressWarnings("unchecked") List<QuizAssignmentDTO> responseBody =
            (List<QuizAssignmentDTO>) getQuizAssignmentResponse.getEntity();
        assertEquals(6, responseBody.size());
      }
    }
  }
}
