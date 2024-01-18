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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.DAVE_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHERS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTORS_AB_GROUP_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TUTOR_PASSWORD;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.GroupStatus;
import uk.ac.cam.cl.dtg.isaac.dos.UserGroup;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.segue.api.GroupsFacade;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;


class GroupsFacadeIT extends IsaacIntegrationTest {

  private GroupsFacade groupsFacade;

  @BeforeEach
  public void setUp() throws Exception {
    // get an instance of the facade to test
    this.groupsFacade = new GroupsFacade(properties, userAccountManager, logManager, assignmentManager, gameManager,
        groupManager, userAssociationManager, userBadgeManager, misuseMonitor);
  }

  @AfterEach
  public void tearDown() throws SQLException {
    // reset group managers in DB, so the same groups can be re-used across test cases
    try (PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
        "DELETE FROM group_additional_managers WHERE group_id in (?, ?);");) {
      pst.setInt(1, (int) TEST_TEACHERS_AB_GROUP_ID);
      pst.setInt(2, (int) TEST_TUTORS_AB_GROUP_ID);
      pst.executeUpdate();
    }
  }

  @Test
  void createGroupEndpoint_createGroupAsTeacher_succeeds() throws NoCredentialsAvailableException,
      NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
      IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
      NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
    // Arrange
    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest createGroupRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
    replay(createGroupRequest);

    UserGroup userGroup = new UserGroup(null, "Test Teacher's New Group", TEST_TEACHER_ID,
        GroupStatus.ACTIVE, new Date(), false, false, new Date());

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
  void createGroupEndpoint_createGroupAsTutor_succeeds() throws NoCredentialsAvailableException,
      NoUserException, SegueDatabaseException, AuthenticationProviderMappingException,
      IncorrectCredentialsProvidedException, AdditionalAuthenticationRequiredException, InvalidKeySpecException,
      NoSuchAlgorithmException, MFARequiredButNotConfiguredException {
    // Arrange
    // log in as Tutor, create request
    LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL,
        TEST_TUTOR_PASSWORD);
    HttpServletRequest createGroupRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
    replay(createGroupRequest);

    UserGroup userGroup = new UserGroup(null, "Test Tutor's New Group", TEST_TUTOR_ID,
        GroupStatus.ACTIVE, new Date(), false, false, new Date());

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
  void addAdditionalManagerToGroupEndpoint_addAdditionalTeacherManagerAsTeacher_succeeds() throws
      NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    // Arrange
    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest addManagerRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
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
  void addAdditionalManagerToGroupEndpoint_addAdditionalTutorManagerAsTeacher_fails() throws
      NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    // Arrange
    // log in as Teacher, create request
    LoginResult teacherLogin = loginAs(httpSession, ITConstants.TEST_TEACHER_EMAIL,
        ITConstants.TEST_TEACHER_PASSWORD);
    HttpServletRequest addManagerRequest = createRequestWithCookies(new Cookie[] {teacherLogin.cookie});
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
  void addAdditionalManagerToGroupEndpoint_addAdditionalTeacherManagerAsTutor_fails() throws
      NoCredentialsAvailableException, NoUserException, SegueDatabaseException,
      AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      AdditionalAuthenticationRequiredException, InvalidKeySpecException, NoSuchAlgorithmException,
      MFARequiredButNotConfiguredException {
    // Arrange
    // log in as Tutor, create request
    LoginResult tutorLogin = loginAs(httpSession, TEST_TUTOR_EMAIL,
        TEST_TUTOR_PASSWORD);
    HttpServletRequest addManagerRequest = createRequestWithCookies(new Cookie[] {tutorLogin.cookie});
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
    assertEquals("You must have a teacher account to add additional group managers to your groups.",
        responseBody.getErrorMessage());
  }
}
