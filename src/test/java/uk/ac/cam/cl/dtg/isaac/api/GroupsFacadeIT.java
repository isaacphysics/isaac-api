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
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.BeforeEach;
import uk.ac.cam.cl.dtg.isaac.dos.GroupMembershipStatus;
import uk.ac.cam.cl.dtg.isaac.dos.GroupStatus;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.GroupsFacade;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.*;


public class GroupsFacadeIT extends IsaacIntegrationTest {

    private GroupsFacade groupsFacade;

    @BeforeEach
    public void setUp() throws Exception {
        // get an instance of the facade to test
        this.groupsFacade = new GroupsFacade(properties, userAccountManager, logManager, assignmentManager,
             groupManager, userAssociationManager, misuseMonitor);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // reset group managers in DB, so the same groups can be re-used across test cases
        PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
                "DELETE FROM group_additional_managers WHERE group_id in (?, ?);");
        pst.setInt(1, (int) TEST_TEACHERS_AB_GROUP_ID);
        pst.setInt(2, (int) TEST_TUTORS_AB_GROUP_ID);
        pst.executeUpdate();

        // reset group members in DB, so the same groups can be re-used across test cases
        pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
                "UPDATE group_memberships SET status=?, updated=? WHERE user_id = 7 AND group_id = 1");
        pst.setString(1, GroupMembershipStatus.ACTIVE.name());
        pst.setTimestamp(2, new Timestamp(new Date().getTime()));
        pst.executeUpdate();
    }

    @Test
    public void createGroupEndpoint_createGroupAsTeacher_succeeds() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest createGroupRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(createGroupRequest);

        UserGroup userGroup = new UserGroup(null, "Test Teacher's New Group", TEST_TEACHER_ID,
                GroupStatus.ACTIVE, new Date(), false, false, new Date(), false);

        // Act
        // make request
        Response createGroupResponse = groupsFacade.createGroup(createGroupRequest, userGroup);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), createGroupResponse.getStatus());

        // check the group was created successfully
        UserGroupDTO responseBody = (UserGroupDTO) createGroupResponse.getEntity();
        assertEquals("Test Teacher's New Group", responseBody.getGroupName());
    }

    @Test
    public void createGroupEndpoint_createGroupAsTutor_succeeds() throws Exception {
        // Arrange
        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL,
                TEST_TUTOR_PASSWORD);
        HttpServletRequest createGroupRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
        replay(createGroupRequest);

        UserGroup userGroup = new UserGroup(null, "Test Tutor's New Group", TEST_TUTOR_ID,
                GroupStatus.ACTIVE, new Date(), false, false, new Date(), false);

        // Act
        // make request
        Response createGroupResponse = groupsFacade.createGroup(createGroupRequest, userGroup);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), createGroupResponse.getStatus());

        // check the group was created successfully
        UserGroupDTO responseBody = (UserGroupDTO) createGroupResponse.getEntity();
        assertEquals("Test Tutor's New Group", responseBody.getGroupName());
    }

    @Test
    public void addAdditionalManagerToGroupEndpoint_addAdditionalTeacherManagerAsTeacher_succeeds() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest addManagerRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(addManagerRequest);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("email", DAVE_TEACHER_EMAIL);

        // Act
        // make request
        Response addManagerResponse = groupsFacade.addAdditionalManagerToGroup(addManagerRequest, TEST_TEACHERS_AB_GROUP_ID,
                responseMap);

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), addManagerResponse.getStatus());

        // check the additional teacher was added successfully
        UserGroupDTO responseBody = (UserGroupDTO) addManagerResponse.getEntity();
        assertEquals(DAVE_TEACHER_EMAIL, responseBody.getAdditionalManagers().stream().findFirst().get().getEmail());
    }

    @Test
    public void addAdditionalManagerToGroupEndpoint_addAdditionalTutorManagerAsTeacher_fails() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest addManagerRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(addManagerRequest);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("email", TEST_TUTOR_EMAIL);

        // Act
        // make request
        Response addManagerResponse = groupsFacade.addAdditionalManagerToGroup(addManagerRequest, TEST_TEACHERS_AB_GROUP_ID,
                responseMap);

        // Assert
        // check status code
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), addManagerResponse.getStatus());

        // check an error message was returned
        SegueErrorResponse responseBody = (SegueErrorResponse) addManagerResponse.getEntity();
        assertEquals("There was a problem adding the user specified. Please make sure their email address is "
                + "correct and they have a teacher account.", responseBody.getErrorMessage());
    }

    @Test
    public void addAdditionalManagerToGroupEndpoint_addAdditionalTeacherManagerAsTutor_fails() throws Exception {
        // Arrange
        // log in as Tutor, create request
        LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL,
                TEST_TUTOR_PASSWORD);
        HttpServletRequest addManagerRequest = createRequestWithCookies(new Cookie[]{tutorLogin.cookie});
        replay(addManagerRequest);

        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("email", TEST_TEACHER_EMAIL);

        // Act
        // make request
        Response addManagerResponse = groupsFacade.addAdditionalManagerToGroup(addManagerRequest, TEST_TUTORS_AB_GROUP_ID,
                responseMap);

        // Assert
        // check status code
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), addManagerResponse.getStatus());

        // check an error message was returned
        SegueErrorResponse responseBody = (SegueErrorResponse) addManagerResponse.getEntity();
        assertEquals("You must have a teacher account to add additional group managers to your groups.", responseBody.getErrorMessage());
    }

    @Test
    public void removeUserFromGroup_teacherDeletesStudentFromGroup_studentRemovedFromGroup() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest removeStudentRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(removeStudentRequest);

        // Act
        Response removeStudentResponse = groupsFacade.removeUserFromGroup(
                removeStudentRequest, createMock(Request.class), TEST_TEACHERS_AB_GROUP_ID, ALICE_STUDENT_ID);

        // Assert
        // check status code
        assertEquals(Response.Status.OK.getStatusCode(), removeStudentResponse.getStatus());

        assertFalse(groupManager.getUsersInGroup(groupManager.getGroupById(TEST_TEACHERS_AB_GROUP_ID)).stream()
                .map(RegisteredUserDTO::getId)
                .anyMatch(id -> id == ALICE_STUDENT_ID));
    }

    @Test
    public void removeUserFromGroup_nonManagerDeletesStudentFromGroup_fails() throws Exception {

        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TUTOR_EMAIL,
                ITConstants.TEST_TUTOR_PASSWORD);
        HttpServletRequest removeStudentRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(removeStudentRequest);

        // Act
        Response removeStudentResponse = groupsFacade.removeUserFromGroup(
                removeStudentRequest, createMock(Request.class), TEST_TEACHERS_AB_GROUP_ID, ALICE_STUDENT_ID);

        // Assert
        // check status code
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), removeStudentResponse.getStatus());
    }

    @Test
    public void removeUserFromGroup_teacherDeletesStudentFromGroup_teacherConnectionRemains() throws Exception {
        // Arrange
        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
                ITConstants.TEST_TEACHER_PASSWORD);
        HttpServletRequest removeStudentRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(removeStudentRequest);

        // Act
        Response removeStudentResponse = groupsFacade.removeUserFromGroup(
                removeStudentRequest, createMock(Request.class), TEST_TEACHERS_AB_GROUP_ID, ALICE_STUDENT_ID);

        // Assert
        // check status code
        assertEquals(Response.Status.OK.getStatusCode(), removeStudentResponse.getStatus());

        assertTrue(userAssociationManager.hasTeacherPermission(
                userAccountManager.getUserDTOById(TEST_TEACHER_ID),
                userAccountManager.getUserDTOById(ALICE_STUDENT_ID)
        ));
    }
}
