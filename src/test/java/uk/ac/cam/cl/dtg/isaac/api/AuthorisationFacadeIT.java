package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.AssociationToken;
import uk.ac.cam.cl.dtg.isaac.dto.AssignmentProgressDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.UserGroupDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.AuthorisationFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.dao.associations.IAssociationDataManager;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Response;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.*;

public class AuthorisationFacadeIT extends IsaacIntegrationTest {
    private AuthorisationFacade authorisationFacade;

    @BeforeEach
    public void setUp() throws Exception {
        this.authorisationFacade = new AuthorisationFacade(
                properties, userAccountManager, logManager, userAssociationManager, groupManager, misuseMonitor);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // reset group managers in DB, so the same groups can be re-used across test cases
        PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
                "DELETE FROM group_additional_managers WHERE group_id in (?, ?);");
        pst.setInt(1, (int) TEST_TEACHERS_AB_GROUP_ID);
        pst.setInt(2, (int) TEST_TUTORS_AB_GROUP_ID);
        pst.executeUpdate();
    }

    @Test
    public void useToken_addStudentToGroup_studentInGroupMembers() throws Exception {
        // Arrange
        IAssociationDataManager dummyAssociationDataManager = createMock(IAssociationDataManager.class);
        UserAssociationManager dummyUserAssociationManager = new UserAssociationManager(
                dummyAssociationDataManager, userAccountManager, groupManager);

        AuthorisationFacade authorisationFacadeForTest = new AuthorisationFacade(
                properties, userAccountManager, logManager, dummyUserAssociationManager, groupManager, misuseMonitor
        );

        // create group token
        AssociationToken token = new AssociationToken("testToken", TEST_TEACHER_ID, TEST_TEACHERS_AB_GROUP_ID);
        expect(dummyAssociationDataManager.lookupAssociationToken(token.getToken())).andReturn(token);
        expect(dummyAssociationDataManager.hasValidAssociation(TEST_TEACHER_ID, TEST_STUDENT_ID)).andReturn(false);
        dummyAssociationDataManager.createAssociation(TEST_TEACHER_ID, TEST_STUDENT_ID);
        expectLastCall().once();
        replay(dummyAssociationDataManager);

        // Get group DTO
        UserGroupDTO group = groupManager.getGroupById(TEST_TEACHERS_AB_GROUP_ID);

        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest addStudentRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(addStudentRequest);

        // Act
        // make request
        Response addStudentResponse = authorisationFacadeForTest.useToken(addStudentRequest, token.getToken());

        // Assert
        // check status code is OK
        assertEquals(Response.Status.OK.getStatusCode(), addStudentResponse.getStatus());

        assertTrue(groupManager.getUsersInGroup(group).stream().anyMatch(s -> s.getId() == TEST_STUDENT_ID));
    }

    @Test
    public void getAssociationToken_teacherOwnerUsesInviteLink_generatesTokenForURL() throws Exception {
        // Arrange
        IAssociationDataManager dummyAssociationDataManager = createMock(IAssociationDataManager.class);
        UserAssociationManager dummyUserAssociationManager = new UserAssociationManager(
                dummyAssociationDataManager, userAccountManager, groupManager);

        AuthorisationFacade authorisationFacadeForTest = new AuthorisationFacade(
                properties, userAccountManager, logManager, dummyUserAssociationManager, groupManager, misuseMonitor
        );

        // create group token
        AssociationToken token = new AssociationToken("testToken", TEST_TEACHER_ID, TEST_TEACHERS_AB_GROUP_ID);
        expect(dummyAssociationDataManager.getAssociationTokenByGroupId(TEST_TEACHERS_AB_GROUP_ID)).andReturn(token);
        replay(dummyAssociationDataManager);

        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest inviteLinkRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(inviteLinkRequest);

        // Assert
        Response inviteLinkResponse = authorisationFacadeForTest.getAssociationToken(
                inviteLinkRequest, TEST_TEACHERS_AB_GROUP_ID);

        // Act
        // check status code
        assertEquals(Response.Status.OK.getStatusCode(), inviteLinkResponse.getStatus());

        AssociationToken responseToken = (AssociationToken) inviteLinkResponse.getEntity();
        assertEquals(token, responseToken);
    }

    @Test
    public void getAssociationToken_additionalManagerUsesInviteLink_succeeds() throws Exception {
        // Arrange
        IAssociationDataManager dummyAssociationDataManager = createMock(IAssociationDataManager.class);
        UserAssociationManager dummyUserAssociationManager = new UserAssociationManager(
                dummyAssociationDataManager, userAccountManager, groupManager);

        AuthorisationFacade authorisationFacadeForTest = new AuthorisationFacade(
                properties, userAccountManager, logManager, dummyUserAssociationManager, groupManager, misuseMonitor);

        // create group token
        AssociationToken token = new AssociationToken("testToken", TEST_TEACHER_ID, TEST_TEACHERS_AB_GROUP_ID);
        expect(dummyAssociationDataManager.getAssociationTokenByGroupId(TEST_TEACHERS_AB_GROUP_ID)).andReturn(token);
        replay(dummyAssociationDataManager);

        // Add additional manager
        UserGroupDTO group = groupManager.getGroupById(TEST_TEACHERS_AB_GROUP_ID);
        groupManager.addUserToManagerList(group, userAccountManager.getUserDTOById(DAVE_TEACHER_ID));

        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest inviteLinkRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(inviteLinkRequest);

        // Assert
        Response inviteLinkResponse = authorisationFacadeForTest.getAssociationToken(
                inviteLinkRequest, TEST_TEACHERS_AB_GROUP_ID);

        // Act
        // check status code
        assertEquals(Response.Status.OK.getStatusCode(), inviteLinkResponse.getStatus());

        AssociationToken responseToken = (AssociationToken) inviteLinkResponse.getEntity();
        assertEquals(token, responseToken);
    }

    @Test
    public void getAssociationToken_otherTeacherUsesInviteLink_fails() throws Exception {
        // Arrange
        IAssociationDataManager dummyAssociationDataManager = createMock(IAssociationDataManager.class);
        UserAssociationManager dummyUserAssociationManager = new UserAssociationManager(
                dummyAssociationDataManager, userAccountManager, groupManager);

        AuthorisationFacade authorisationFacadeForTest = new AuthorisationFacade(
                properties, userAccountManager, logManager, dummyUserAssociationManager, groupManager, misuseMonitor);

        // log in as Teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, DAVE_TEACHER_EMAIL, DAVE_TEACHER_PASSWORD);
        HttpServletRequest inviteLinkRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(inviteLinkRequest);

        // Assert
        Response inviteLinkResponse = authorisationFacadeForTest.getAssociationToken(
                inviteLinkRequest, TEST_TEACHERS_AB_GROUP_ID);

        // Act
        // check status code
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), inviteLinkResponse.getStatus());

        SegueErrorResponse responseBody = (SegueErrorResponse) inviteLinkResponse.getEntity();
        assertEquals("You do not have permission to create or request a group token for this group. "
                + "Only owners or additional managers can.", responseBody.getErrorMessage());
    }

    @Test
    public void getTokenOwnerUserSummary_studentUsesInviteLink_groupManagersReturned() throws Exception {
        // Arrange
        IAssociationDataManager dummyAssociationDataManager = createMock(IAssociationDataManager.class);
        UserAssociationManager dummyUserAssociationManager = new UserAssociationManager(
                dummyAssociationDataManager, userAccountManager, groupManager);

        AuthorisationFacade authorisationFacadeForTest = new AuthorisationFacade(
                properties, userAccountManager, logManager, dummyUserAssociationManager, groupManager, misuseMonitor);

        // create group token
        AssociationToken token = new AssociationToken("testToken", TEST_TEACHER_ID, TEST_TEACHERS_AB_GROUP_ID);
        expect(dummyAssociationDataManager.lookupAssociationToken(token.getToken())).andReturn(token);
        expect(dummyAssociationDataManager.hasValidAssociation(TEST_TEACHER_ID, TEST_STUDENT_ID)).andReturn(false);
        dummyAssociationDataManager.createAssociation(TEST_TEACHER_ID, TEST_STUDENT_ID);
        expectLastCall().once();

        replay(dummyAssociationDataManager);

        // Add additional manager
        UserGroupDTO group = groupManager.getGroupById(TEST_TEACHERS_AB_GROUP_ID);
        group = groupManager.addUserToManagerList(group, userAccountManager.getUserDTOById(DAVE_TEACHER_ID));

        // Get group managers
        List<Long> groupManagerIDs = new ArrayList<>();
        groupManagerIDs.add(group.getOwnerSummary().getId());
        groupManagerIDs.addAll(group.getAdditionalManagers().stream()
                .map(UserSummaryDTO::getId)
                .collect(Collectors.toList()));

        // log in as Student, create request
        LoginResult studentLogin = loginAs(httpSession, TEST_STUDENT_EMAIL, TEST_STUDENT_PASSWORD);
        HttpServletRequest inviteLinkRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(inviteLinkRequest);

        // Act
        Response inviteLinkResponse = authorisationFacadeForTest.getTokenOwnerUserSummary(inviteLinkRequest, token.getToken());

        // Assert
        // check status code
        assertEquals(Response.Status.OK.getStatusCode(), inviteLinkResponse.getStatus());

        @SuppressWarnings("unchecked")
        List<UserSummaryWithEmailAddressDTO> usersLinkedToToken =
                (List<UserSummaryWithEmailAddressDTO>) inviteLinkResponse.getEntity();
        assertEquals(groupManagerIDs, usersLinkedToToken.stream()
                .map(UserSummaryDTO::getId)
                .collect(Collectors.toList()));
    }

    @Test
    public void revokeOwnerAssociation_studentRevokesTeacherConnection_studentNotInMarkbook() throws Exception {
        try {
            // Arrange
            // create assignment facade to query
            AssignmentFacade assignmentFacade = new AssignmentFacade(
                    assignmentManager, questionManager, userAccountManager, groupManager, properties, gameManager,
                    logManager, userAssociationManager, assignmentService,
                    Clock.fixed(Instant.now(), ZoneId.of("UTC")));

            // log in as Student, create request
            LoginResult studentLogin = loginAs(httpSession, ALICE_STUDENT_EMAIL, ALICE_STUDENT_PASSWORD);
            HttpServletRequest revokeConnectionRequest = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
            replay(revokeConnectionRequest);

            // Act
            Response revokeConnectionResponse = authorisationFacade.revokeOwnerAssociation(
                    revokeConnectionRequest, TEST_TEACHER_ID);

            // Assert
            // check status code
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), revokeConnectionResponse.getStatus());

            // log in as Teacher, create request
            LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
            HttpServletRequest markbookRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
            replay(markbookRequest);

            // get assignment progress
            Response markbookResponse = assignmentFacade.getAssignmentProgress(markbookRequest, ASSIGNMENTS_TEST_EXISTING_TEACHER_AB_ASSIGNMENT_ID);
            assertEquals(Response.Status.OK.getStatusCode(), markbookResponse.getStatus());

            @SuppressWarnings("unchecked")
            List<AssignmentProgressDTO> markbook = (List<AssignmentProgressDTO>) markbookResponse.getEntity();

            for (AssignmentProgressDTO studentResults : markbook) {
                UserSummaryDTO userSummary = studentResults.getUser();
                assert userSummary != null;
                if (userSummary.getId() == ALICE_STUDENT_ID) {
                    List<Constants.CompletionState> results = studentResults.getQuestionResults();
                    assertTrue(results != null && results.isEmpty());
                    break;
                }
            }
        } finally {
            // reset associations in DB, so the same groups can be re-used across test cases
            PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
                    "INSERT INTO user_associations(user_id_granting_permission, user_id_receiving_permission," +
                            " created) VALUES (?, ?, ?);");
            pst.setInt(1, (int) ALICE_STUDENT_ID);
            pst.setInt(2, (int) TEST_TEACHER_ID);
            pst.setTimestamp(3, new Timestamp(new Date().getTime()));
            pst.executeUpdate();
        }
    }
}
