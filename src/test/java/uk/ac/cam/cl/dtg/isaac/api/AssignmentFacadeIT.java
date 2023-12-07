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

import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ASSIGNMENT_FACADE_TEST_GROUP_ID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentStatusDTO;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

public class AssignmentFacadeIT extends IsaacIntegrationTest {

  private AssignmentFacade assignmentFacade;
  private final String instantExpected = "2049-07-01T12:05:30Z";
  private final Clock clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));

  @BeforeEach
  public void setUp() throws Exception {
    // get an instance of the facade to test
    this.assignmentFacade = new AssignmentFacade(assignmentManager, questionManager, userAccountManager,
        groupManager, properties, gameManager, logManager, userAssociationManager, userBadgeManager,
        new AssignmentService(userAccountManager), clock);
  }

  @AfterEach
  public void tearDown() throws SQLException {
    // reset the mocks
    reset(userBadgeManager);

    // reset assignments in DB, so the same assignment can be re-used across tests
    try (PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
        "DELETE FROM assignments WHERE gameboard_id in (?,?);")) {
      pst.setString(1, ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
      pst.setString(2, ITConstants.ASSIGNMENTS_DATE_TEST_GAMEBOARD_ID);
      pst.executeUpdate();
    }
  }

  @AfterAll
  public static void cleanUp() throws SegueDatabaseException {
    // reset additional manager privileges setting for Daves group
    UserGroupDTO davesGroup = groupManager.getGroupById(ITConstants.DAVE_TEACHERS_BC_GROUP_ID);
    davesGroup.setAdditionalManagerPrivileges(false);
    groupManager.editUserGroup(davesGroup);
  }

  @Test
  public void assignBulkEndpoint_setValidAssignmentAsTeacher_assignsSuccessfully() throws
      NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException,
      NoSuchAlgorithmException, MFARequiredButNotConfiguredException {

    // Arrange
    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(assignGameboardsRequest);

    // build assignment
    AssignmentDTO assignment = new AssignmentDTO();
    assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
    assignment.setGroupId(ITConstants.TEST_TEACHERS_AB_GROUP_ID);

    // Act
    // make request
    Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
        Collections.singletonList(assignment));

    // Assert
    // check status code is OK
    assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

    // check the assignment assigned successfully
    @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
        (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
    assertNull(responseBody.get(0).getErrorMessage());
  }

  // FLAKY
  // @Test
  // public void assignBulkEndpoint_setValidAssignmentAsTutor_assignsSuccessfully() throws
  //         NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
  //         AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
  //         AdditionalAuthenticationRequiredException, InvalidKeySpecException,
  //         NoSuchAlgorithmException, MFARequiredButNotConfiguredException {

  //     // Arrange
  //     // log in as Tutor, create request
  //     LoginResult tutorLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
  //             ITConstants.TEST_TUTOR_PASSWORD);
  //     HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
  //     replay(assignGameboardsRequest);

  //     // build assignment
  //     AssignmentDTO assignment = new AssignmentDTO();
  //     assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
  //     assignment.setGroupId(ITConstants.TEST_TUTORS_AB_GROUP_ID);

  //     // Act
  //     // make request
  //     Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
  //             Collections.singletonList(assignment));

  //     // Assert
  //     // check status code is OK
  //     assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

  //     // check the assignment assigned successfully
  //     @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
  //             (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
  //     assertNull(responseBody.get(0).getErrorMessage());
  // }

  @Test
  public void assignBulkEndpoint_scheduleAssignmentWithValidDueDateAsTeacher_assignsSuccessfully() throws
      NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException,
      NoSuchAlgorithmException, MFARequiredButNotConfiguredException {

    // Arrange
    // build due date
    Calendar dueDateCalendar = Calendar.getInstance();
    dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
    dueDateCalendar.set(Calendar.MONTH, 1);
    dueDateCalendar.set(Calendar.YEAR, 2050);

    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(assignGameboardsRequest);

    // build assignment
    AssignmentDTO assignment = new AssignmentDTO();
    assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
    assignment.setGroupId(ITConstants.TEST_TEACHERS_AB_GROUP_ID);
    assignment.setDueDate(dueDateCalendar.getTime());

    // Act
    // make request
    Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
        Collections.singletonList(assignment));

    // Assert
    // check status code is OK
    assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

    // check the assignment assigned successfully
    @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
        (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
    assertNull(responseBody.get(0).getErrorMessage());
  }

  // FLAKY
  //@Test
  // public void assignBulkEndpoint_scheduleAssignmentWithValidDueDateAsTutor_assignsSuccessfully() throws
  //         NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
  //         AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
  //         AdditionalAuthenticationRequiredException, InvalidKeySpecException,
  //         NoSuchAlgorithmException, MFARequiredButNotConfiguredException {

  //     // Arrange
  //     // build due date
  //     Calendar dueDateCalendar = Calendar.getInstance();
  //     dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
  //     dueDateCalendar.set(Calendar.MONTH, 1);
  //     dueDateCalendar.set(Calendar.YEAR, 2050);

  //     // log in as Tutor, create request
  //     LoginResult tutorLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
  //             ITConstants.TEST_TUTOR_PASSWORD);
  //     HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
  //     replay(assignGameboardsRequest);

  //     // build assignment
  //     AssignmentDTO assignment = new AssignmentDTO();
  //     assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
  //     assignment.setGroupId(ITConstants.TEST_TUTORS_AB_GROUP_ID);
  //     assignment.setDueDate(dueDateCalendar.getTime());

  //     // Act
  //     // make request
  //     Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
  //             Collections.singletonList(assignment));

  //     // Assert
  //     // check status code is OK
  //     assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

  //     // check the assignment assigned successfully
  //     @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
  //             (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
  //     assertNull(responseBody.get(0).getErrorMessage());
  // }

  @Test
  public void assignBulkEndpoint_scheduleAssignmentWithDueDateInPastAsTeacher_failsToAssign() throws
      NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException,
      NoSuchAlgorithmException, MFARequiredButNotConfiguredException {

    // Arrange
    // build due date
    Calendar dueDateCalendar = Calendar.getInstance();
    dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
    dueDateCalendar.set(Calendar.MONTH, 1);
    dueDateCalendar.set(Calendar.YEAR, 2049);

    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(assignGameboardsRequest);

    // build assignment
    AssignmentDTO assignment = new AssignmentDTO();
    assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
    assignment.setGroupId(ITConstants.TEST_TEACHERS_AB_GROUP_ID);
    assignment.setDueDate(dueDateCalendar.getTime());

    // Act
    // make request
    Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
        Collections.singletonList(assignment));

    // Assert
    // check status code is OK (this is expected even if no assignment assigns successfully)
    assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

    // check the assignment failed to assign
    @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
        (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
    assertEquals("The assignment cannot be due in the past.", responseBody.get(0).getErrorMessage());
  }

  @Test
  public void assignBulkEndpoint_scheduleSingleAssignmentWithDistantScheduledDateAsTeacher_failsToAssign() throws
      Exception {

    // Arrange
    // build scheduled date
    Calendar scheduledDateCalendar = Calendar.getInstance();
    scheduledDateCalendar.set(Calendar.DAY_OF_MONTH, 2);
    scheduledDateCalendar.set(Calendar.MONTH, 11);
    scheduledDateCalendar.set(Calendar.YEAR, 2050);

    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(assignGameboardsRequest);

    // build assignment
    AssignmentDTO assignment = new AssignmentDTO();
    assignment.setGameboardId(ITConstants.ASSIGNMENTS_DATE_TEST_GAMEBOARD_ID);
    assignment.setGroupId(ITConstants.TEST_TEACHERS_AB_GROUP_ID);
    assignment.setScheduledStartDate(scheduledDateCalendar.getTime());

    // Act
    // make request
    Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
        Collections.singletonList(assignment));

    // Assert
    // check status code is OK (this is expected even if no assignment assigns successfully)
    assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

    // check the assignment failed to assign
    @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
        (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
    assertEquals("The assignment cannot be scheduled to begin more than one year in the future.",
        responseBody.get(0).getErrorMessage());
  }

  @Test
  public void assignBulkEndpoint_scheduleSingleAssignmentWithScheduledDateAfterDueDateAsTeacher_failsToAssign() throws
      Exception {

    // Arrange
    // build scheduled date
    Calendar scheduledDateCalendar = Calendar.getInstance();
    scheduledDateCalendar.set(Calendar.DAY_OF_MONTH, 5);
    scheduledDateCalendar.set(Calendar.MONTH, 1);
    scheduledDateCalendar.set(Calendar.YEAR, 2050);

    // build due date
    Calendar dueDateCalendar = Calendar.getInstance();
    dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
    dueDateCalendar.set(Calendar.MONTH, 1);
    dueDateCalendar.set(Calendar.YEAR, 2050);

    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(assignGameboardsRequest);

    // build assignment
    AssignmentDTO assignment = new AssignmentDTO();
    assignment.setGameboardId(ITConstants.ASSIGNMENTS_DATE_TEST_GAMEBOARD_ID);
    assignment.setGroupId(ITConstants.TEST_TEACHERS_AB_GROUP_ID);
    assignment.setDueDate(dueDateCalendar.getTime());
    assignment.setScheduledStartDate(scheduledDateCalendar.getTime());

    // Act
    // make request
    Response assignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
        Collections.singletonList(assignment));

    // Assert
    // check status code is OK (this is expected even if no assignment assigns successfully)
    assertEquals(Response.Status.OK.getStatusCode(), assignBulkResponse.getStatus());

    // check the assignment failed to assign
    @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
        (ArrayList<AssignmentStatusDTO>) assignBulkResponse.getEntity();
    assertEquals("The assignment cannot be scheduled to begin after it is due.",
        responseBody.get(0).getErrorMessage());
  }

  // FLAKY
  // @Test public void deleteAssignmentEndpoint_attemptToDeleteOwnersAssignmentAsAdditionalManagerWithAdditionManagerPrivilegesOff_failsToDelete()
  //         throws Exception {

  //     // Arrange
  //     // Ensure that additional manager privileges are set to false
  //     UserGroupDTO davesGroup = groupManager.getGroupById(ITConstants.DAVE_TEACHERS_BC_GROUP_ID);
  //     davesGroup.setAdditionalManagerPrivileges(false);
  //     groupManager.editUserGroup(davesGroup);

  //     // log in as Test teacher, create request
  //     LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
  //             ITConstants.TEST_TEACHER_PASSWORD);
  //     HttpServletRequest deleteAssignmentRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
  //     replay(deleteAssignmentRequest);

  //     // Act
  //     // make request
  //     Response deleteAssignmentResponse = assignmentFacade.deleteAssignment(deleteAssignmentRequest,
  //             ITConstants.ADDITIONAL_MANAGER_TEST_GAMEBOARD_ID, ITConstants.DAVE_TEACHERS_BC_GROUP_ID);

  //     // Assert
  //     // check status code is FORBIDDEN
  //     assertEquals(Response.Status.FORBIDDEN.getStatusCode(), deleteAssignmentResponse.getStatus());

  //     // ensure the deletion was forbidden because additional manager privileges aren't enabled
  //     SegueErrorResponse responseBody = (SegueErrorResponse) deleteAssignmentResponse.getEntity();
  //     assertEquals("You do not have permission to delete this assignment. Unable to delete it.",
  //             responseBody.getErrorMessage());
  // }

  @Test
  public void deleteAssignmentEndpoint_attemptToDeleteOwnersAssignmentAsAdditionalManagerWithAdditionManagerPrivilegesOn_succeeds()
      throws Exception {
    // Test Teacher (5) is additional manager of group 5, which is owned by dave teacher (10)

    // Arrange
    // Ensure that additional manager privileges are set to true
    UserGroupDTO davesGroup = groupManager.getGroupById(ITConstants.DAVE_TEACHERS_BC_GROUP_ID);
    davesGroup.setAdditionalManagerPrivileges(true);
    groupManager.editUserGroup(davesGroup);

    // log in as Test teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest deleteAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(deleteAssignmentRequest);

    // Act
    // make request
    Response deleteAssignmentResponse = assignmentFacade.deleteAssignment(deleteAssignmentRequest,
        ITConstants.ADDITIONAL_MANAGER_TEST_GAMEBOARD_ID, ITConstants.DAVE_TEACHERS_BC_GROUP_ID);

    // Assert
    // check status code is NO_CONTENT (successful)
    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteAssignmentResponse.getStatus());
  }

  @Test
  public void getAssignmentProgressDownloadCSV_succeeds() throws Exception {
    // log in as Test teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest downloadAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(downloadAssignmentRequest);

    Response downloadAssignmentResponse =
        assignmentFacade.getAssignmentProgressDownloadCSV(downloadAssignmentRequest, 4L, "excel");
    String downloadAssignmentContents = downloadAssignmentResponse.getEntity().toString();

    String expectedContents;
    try (FileInputStream expectedFile = new FileInputStream(
        "src/test/resources/expected_assignment_progress_export.csv")) {
      expectedContents = IOUtils.toString(expectedFile, StandardCharsets.UTF_8);
    }
    assertEquals(expectedContents, downloadAssignmentContents);
  }

  @Test
  public void getAssignmentProgressDownloadCSV_permissionDenied() throws Exception {
    // log in as Test teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
        ITConstants.TEST_TUTOR_PASSWORD);
    HttpServletRequest downloadAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(downloadAssignmentRequest);

    Response downloadAssignmentResponse =
        assignmentFacade.getAssignmentProgressDownloadCSV(downloadAssignmentRequest, 4L, "excel");

    assertEquals(403, downloadAssignmentResponse.getStatus());
  }

  @Test
  public void getGroupAssignmentsProgressDownloadCSV_succeeds() throws Exception {
    // log in as Test teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest downloadAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(downloadAssignmentRequest);

    Response downloadAssignmentResponse =
        assignmentFacade.getGroupAssignmentsProgressDownloadCSV(downloadAssignmentRequest,
            ASSIGNMENT_FACADE_TEST_GROUP_ID, "excel");
    String downloadAssignmentContents = downloadAssignmentResponse.getEntity().toString();

    String expectedContents;
    try (FileInputStream expectedFile = new FileInputStream("src/test/resources/expected_group_progress_export.csv")) {
      expectedContents = IOUtils.toString(expectedFile, StandardCharsets.UTF_8);
    }
    assertEquals(expectedContents, downloadAssignmentContents);
  }

  @Test
  public void getGroupAssignmentsProgressDownloadCSV_permissionDenied() throws Exception {
    // log in as Test teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
        ITConstants.TEST_TUTOR_PASSWORD);
    HttpServletRequest downloadAssignmentRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(downloadAssignmentRequest);

    Response downloadAssignmentResponse =
        assignmentFacade.getGroupAssignmentsProgressDownloadCSV(downloadAssignmentRequest,
            ASSIGNMENT_FACADE_TEST_GROUP_ID, "excel");

    assertEquals(403, downloadAssignmentResponse.getStatus());
  }
}
