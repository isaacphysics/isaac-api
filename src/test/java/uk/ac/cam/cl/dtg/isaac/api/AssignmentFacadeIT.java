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

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.api.easymock.PowerMock;
import uk.ac.cam.cl.dtg.isaac.api.services.AssignmentService;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentStatusDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AssignmentFacadeIT extends IsaacIntegrationTest {

    private AssignmentFacade assignmentFacade;
    private final String instantExpected = "2049-07-01T12:05:30Z";
    private final Clock clock = Clock.fixed(Instant.parse(instantExpected), ZoneId.of("UTC"));


    @BeforeEach
    public void setUp() throws Exception {
        // get an instance of the facade to test
        this.assignmentFacade = new AssignmentFacade(assignmentManager, questionManager, userAccountManager,
                groupManager, properties, gameManager, logManager, userAssociationManager,
                new AssignmentService(userAccountManager), clock);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // reset the mocks
        PowerMock.reset(Date.class);

        // reset assignments in DB, so the same assignment can be re-used across tests
        try (Connection conn = postgresSqlDb.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("DELETE FROM assignments WHERE gameboard_id in (?,?);")) {
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
    public void assignBulkEndpoint_setValidAssignmentAsTeacher_assignsSuccessfully() throws Exception {

        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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

    @Test
    public void assignBulkEndpoint_setValidAssignmentAsTutor_assignsSuccessfully() throws Exception {

        // Arrange
        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
                ITConstants.TEST_TUTOR_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
        replay(assignGameboardsRequest);

        // build assignment
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
        assignment.setGroupId(ITConstants.TEST_TUTORS_AB_GROUP_ID);

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

    @Test
    public void assignBulkEndpoint_scheduleAssignmentWithValidDueDateAsTeacher_assignsSuccessfully() throws Exception {

        // Arrange
        // build due date
        Calendar dueDateCalendar = Calendar.getInstance();
        dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
        dueDateCalendar.set(Calendar.MONTH, 1);
        dueDateCalendar.set(Calendar.YEAR, 2050);

        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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

    @Test
    public void assignBulkEndpoint_scheduleAssignmentWithValidDueDateAsTutor_assignsSuccessfully() throws Exception {

        // Arrange
        // build due date
        Calendar dueDateCalendar = Calendar.getInstance();
        dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
        dueDateCalendar.set(Calendar.MONTH, 1);
        dueDateCalendar.set(Calendar.YEAR, 2050);

        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
                ITConstants.TEST_TUTOR_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
        replay(assignGameboardsRequest);

        // build assignment
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
        assignment.setGroupId(ITConstants.TEST_TUTORS_AB_GROUP_ID);
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

    @Test
    public void assignBulkEndpoint_scheduleAssignmentWithDueDateInPastAsTeacher_failsToAssign() throws Exception {

        // Arrange
        // build due date
        Calendar dueDateCalendar = Calendar.getInstance();
        dueDateCalendar.set(Calendar.DAY_OF_MONTH, 1);
        dueDateCalendar.set(Calendar.MONTH, 1);
        dueDateCalendar.set(Calendar.YEAR, 2049);

        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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
        scheduledDateCalendar.set(Calendar.MONTH, 6);
        scheduledDateCalendar.set(Calendar.YEAR, 2050);

        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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


    @Test
    public void deleteAssignmentEndpoint_attemptToDeleteOwnersAssignmentAsAdditionalManagerWithAdditionManagerPrivilegesOff_failsToDelete()
            throws Exception {

        // Arrange
        // Ensure that additional manager privileges are set to false
        UserGroupDTO davesGroup = groupManager.getGroupById(ITConstants.DAVE_TEACHERS_BC_GROUP_ID);
        davesGroup.setAdditionalManagerPrivileges(false);
        groupManager.editUserGroup(davesGroup);

        // log in as Test teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest deleteAssignmentRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(deleteAssignmentRequest);

        // Act
        // make request
        Response deleteAssignmentResponse = assignmentFacade.deleteAssignment(deleteAssignmentRequest,
                ITConstants.ADDITIONAL_MANAGER_TEST_GAMEBOARD_ID, ITConstants.DAVE_TEACHERS_BC_GROUP_ID);

        // Assert
        // check status code is FORBIDDEN
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), deleteAssignmentResponse.getStatus());

        // ensure the deletion was forbidden because additional manager privileges aren't enabled
        SegueErrorResponse responseBody = (SegueErrorResponse) deleteAssignmentResponse.getEntity();
        assertEquals("You do not have permission to delete this assignment. Unable to delete it.",
                responseBody.getErrorMessage());
    }

    @Test public void deleteAssignmentEndpoint_attemptToDeleteOwnersAssignmentAsAdditionalManagerWithAdditionManagerPrivilegesOn_succeeds()
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
        HttpServletRequest deleteAssignmentRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
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
    public void assignBulkEndpoint_setDuplicateAssignmentAsTeacher_failsToAssign() throws Exception {

        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest assignGameboardsRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(assignGameboardsRequest);

        // build assignment
        AssignmentDTO assignment = new AssignmentDTO();
        assignment.setGameboardId(ITConstants.ASSIGNMENTS_TEST_GAMEBOARD_ID);
        assignment.setGroupId(ITConstants.TEST_TEACHERS_AB_GROUP_ID);

        // Act
        // make initial request, which should succeed
        assignmentFacade.assignGameBoards(assignGameboardsRequest,
                Collections.singletonList(assignment));

        // make duplicate request, which should fail
        Response duplicateAssignBulkResponse = assignmentFacade.assignGameBoards(assignGameboardsRequest,
                Collections.singletonList(assignment));

        // Assert
        // check status code is OK (this is expected even if no assignment assigns successfully)
        assertEquals(Response.Status.OK.getStatusCode(), duplicateAssignBulkResponse.getStatus());

        // check the assignment failed to assign
        @SuppressWarnings("unchecked") ArrayList<AssignmentStatusDTO> responseBody =
                (ArrayList<AssignmentStatusDTO>) duplicateAssignBulkResponse.getEntity();
        assertEquals("You cannot assign the same work to a group more than once.",
                responseBody.get(0).getErrorMessage());
    }
}
