package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;


public class UsersFacadeIT extends IsaacIntegrationTest {

    private UsersFacade usersFacade;

    @BeforeEach
    public void setUp() {
        this.usersFacade = new UsersFacade(properties, userAccountManager, logManager, userAssociationManager,
                misuseMonitor, userPreferenceManager, schoolListReader);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        // reset created users in DB
        PreparedStatement pst = postgresSqlDb.getDatabaseConnection().prepareStatement(
                "DELETE FROM users WHERE email in (?);");
        pst.setString(1, ITConstants.TEST_SIGNUP_EMAIL);
        pst.executeUpdate();
    }

    /**
     * Tests the 'happy path' of user registration. For a JSON payload with valid properties (name, email etc.), check
     * we receive an 'OK' response with those properties and additional default properties (role).
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUser_succeedsAndMatchesProvidedFieldsAndDefaults() throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", ITConstants.TEST_SIGNUP_EMAIL)
                        .put("password", ITConstants.TEST_SIGNUP_PASSWORD)
                        .put("familyName", "signup")
                        .put("givenName", "test")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        // check status code is 'OK'
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());

        // check the relevant returned fields match what we provided
        assertEquals(ITConstants.TEST_SIGNUP_EMAIL, ((RegisteredUserDTO) createResponse.getEntity()).getEmail());
        assertEquals("test", ((RegisteredUserDTO) createResponse.getEntity()).getGivenName());
        assertEquals("signup", ((RegisteredUserDTO) createResponse.getEntity()).getFamilyName());

        // check the other returned fields match the expected defaults
        assertEquals(Role.STUDENT, ((RegisteredUserDTO) createResponse.getEntity()).getRole());
    }

    /**
     * Check we are given a session cookie after successful signup.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUser_succeedsAndSetsSessionCookie() throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", ITConstants.TEST_SIGNUP_EMAIL)
                        .put("password", ITConstants.TEST_SIGNUP_PASSWORD)
                        .put("familyName", "signup")
                        .put("givenName", "test")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        // create a mock Response object, and keep track of the cookies that will be set.
        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        Capture<Cookie> setCookie = Capture.newInstance();
        response.addCookie(capture(setCookie));
        EasyMock.expectLastCall();
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        // check status code is 'OK'
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());

        // check we were given a session cookie
        assertEquals("SEGUE_AUTH_COOKIE", setCookie.getValue().getName());
    }

    /**
     * Checks that signup attempts using an email address already associated with an account are rejected.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUserWithDuplicateEmail_failsWithBadRequest() throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", integrationTestUsers.TEST_STUDENT.getEmail())
                        .put("password", ITConstants.TEST_STUDENT_PASSWORD)
                        .put("familyName", "student")
                        .put("givenName", "test")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
    }

    /**
     * Checks that signup attempts with a malformed JSON object are rejected.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUserWithMalformedJsonObject_failsWithBadRequest() throws Exception {
        // Arrange
        String payload = "{try{to{parse{this!{}}";

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
    }

    /**
     * Checks that an authenticated (with session cookie) request to update an existing user succeed.
     *  This request uses existing values for all properties except exam board, so should update the exam board only.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_updateUserWhileLoggedIn_succeedsAndUpdatesExpectedPropertiesOnly() throws Exception {
        // Arrange
        // log in as student
        RegisteredUserDTO targetUser = integrationTestUsers.TEST_STUDENT;

        LoginResult studentLogin = loginAs(httpSession, ITConstants.TEST_STUDENT_EMAIL, ITConstants.TEST_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", targetUser.getFamilyName())
                        .put("firstLogin", false)
                        .put("gender", targetUser.getGender())
                        .put("givenName", targetUser.getGivenName())
                        .put("id", targetUser.getId())
                        .put("lastSeen", "1686558490205")
                        .put("lastUpdated", "1581680627635")
                        .put("loggedIn", true)
                        .put("password", JSONObject.NULL)
                        .put("registeredContext", Set.of())
                        .put("registrationDate", "1564660299981")
                        .put("role", targetUser.getRole())
                        .put("schoolId", targetUser.getSchoolId())
                )
                .put("passwordCurrent", JSONObject.NULL)
                .put("registeredUserContexts", Set.of(new JSONObject()
                                .put("stage", "all")
                                .put("examBoard", "aqa") //  this is the update
                    )
                );

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        // check status code was 'OK'
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());

        // check exam board was updated
        assertEquals(ExamBoard.aqa, ((RegisteredUserDTO) createResponse.getEntity()).getRegisteredContexts().get(0).getExamBoard());

        // check nothing else was modified unexpectedly
        assertEquals(targetUser.getEmail(), ((RegisteredUserDTO) createResponse.getEntity()).getEmail());
        assertEquals(targetUser.getEmailVerificationStatus(), ((RegisteredUserDTO) createResponse.getEntity()).getEmailVerificationStatus());
        assertEquals(targetUser.getFamilyName(), ((RegisteredUserDTO) createResponse.getEntity()).getFamilyName());
        assertEquals(targetUser.getGender(), ((RegisteredUserDTO) createResponse.getEntity()).getGender());
        assertEquals(targetUser.getGivenName(), ((RegisteredUserDTO) createResponse.getEntity()).getGivenName());
        assertEquals(Stage.all, ((RegisteredUserDTO) createResponse.getEntity()).getRegisteredContexts().get(0).getStage());
        assertEquals(targetUser.getRole(), ((RegisteredUserDTO) createResponse.getEntity()).getRole());
        assertEquals(targetUser.getSchoolId(), ((RegisteredUserDTO) createResponse.getEntity()).getSchoolId());
    }

    /**
     * Checks that unauthenticated (without session cookie) requests to update existing users of each role are rejected.
     *
     * @throws Exception - not expected.
     */
    @ParameterizedTest
    @MethodSource("allTestUsersProvider")
    public void createOrUpdateEndpoint_updateUserWhileNotLoggedIn_failsWithUnauthorised(RegisteredUserDTO targetUser) throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", ">:)") // this is the update
                        .put("firstLogin", targetUser.isFirstLogin())
                        .put("gender", targetUser.getGender())
                        .put("givenName", targetUser.getGivenName())
                        .put("id", targetUser.getId())
                        .put("lastSeen", "1686558490205")
                        .put("lastUpdated", "1686558490205")
                        .put("loggedIn", true)
                        .put("password", JSONObject.NULL)
                        .put("registeredContext", Set.of())
                        .put("registrationDate", "1564660299981")
                        .put("role", targetUser.getRole())
                        .put("schoolId", targetUser.getSchoolId())
                )
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), createResponse.getStatus());
    }

    /**
     * Checks that a request to update an existing user's password with the correct existing password succeeds.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_updateUserPasswordWithCorrectCurrentPassword_succeedsAndUpdatesExpectedPropertiesOnly() throws Exception {
        // Arrange
        // log in as student
        RegisteredUserDTO targetUser = integrationTestUsers.ERIKA_STUDENT;

        LoginResult studentLogin = loginAs(httpSession, ITConstants.ERIKA_STUDENT_EMAIL, ITConstants.ERIKA_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", targetUser.getFamilyName())
                        .put("firstLogin", false)
                        .put("gender", targetUser.getGender())
                        .put("givenName", targetUser.getGivenName())
                        .put("id", targetUser.getId())
                        .put("lastSeen", "1686558490205")
                        .put("lastUpdated", "1581680627635")
                        .put("loggedIn", true)
                        .put("password", "test12345")
                        .put("registeredContext", Set.of())
                        .put("registrationDate", "1564660299981")
                        .put("role", targetUser.getRole())
                        .put("schoolId", targetUser.getSchoolId())
                )
                .put("passwordCurrent", ITConstants.ERIKA_STUDENT_PASSWORD);


        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        // check status code was 'OK'
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());

        // check nothing else was modified unexpectedly
        assertEquals(targetUser.getEmail(), ((RegisteredUserDTO) createResponse.getEntity()).getEmail());
        assertEquals(targetUser.getEmailVerificationStatus(), ((RegisteredUserDTO) createResponse.getEntity()).getEmailVerificationStatus());
        assertEquals(targetUser.getFamilyName(), ((RegisteredUserDTO) createResponse.getEntity()).getFamilyName());
        assertEquals(targetUser.getGender(), ((RegisteredUserDTO) createResponse.getEntity()).getGender());
        assertEquals(targetUser.getGivenName(), ((RegisteredUserDTO) createResponse.getEntity()).getGivenName());
        assertEquals(targetUser.getRole(), ((RegisteredUserDTO) createResponse.getEntity()).getRole());
        assertEquals(targetUser.getSchoolId(), ((RegisteredUserDTO) createResponse.getEntity()).getSchoolId());
    }

    /**
     * Checks that a request to update an existing user's password with an incorrect existing password fails.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_updateUserPasswordWithIncorrectCurrentPassword_failsWithBadRequest() throws Exception {
        // Arrange
        // log in as student
        RegisteredUserDTO targetUser = integrationTestUsers.ERIKA_STUDENT;

        LoginResult studentLogin = loginAs(httpSession, ITConstants.ERIKA_STUDENT_EMAIL, ITConstants.ERIKA_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", targetUser.getFamilyName())
                        .put("firstLogin", false)
                        .put("gender", targetUser.getGender())
                        .put("givenName", targetUser.getGivenName())
                        .put("id", targetUser.getId())
                        .put("lastSeen", "1686558490205")
                        .put("lastUpdated", "1581680627635")
                        .put("loggedIn", true)
                        .put("password", "test12345")
                        .put("registeredContext", Set.of())
                        .put("registrationDate", "1564660299981")
                        .put("role", targetUser.getRole())
                        .put("schoolId", targetUser.getSchoolId())
                )
                .put("passwordCurrent", "aPasswordThatIsNotCorrect");


        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());
    }

    /**
     * Checks that authenticated requests to update other users of each role by a user with the Student role are rejected.
     *
     * @throws Exception - not expected.
     */
    @ParameterizedTest
    @MethodSource("allTestUsersProvider")
    public void createOrUpdateEndpoint_updateAnotherUserWhileLoggedInAsStudent_failsWithForbidden(RegisteredUserDTO targetUser) throws Exception {
        // Arrange
        // log in as an existing student
        LoginResult studentLogin = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        // build update request
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", ">:)") // this is the update
                        .put("firstLogin", targetUser.isFirstLogin())
                        .put("gender", targetUser.getGender())
                        .put("givenName", targetUser.getGivenName())
                        .put("id", targetUser.getId())
                        .put("lastSeen", "1686558490205")
                        .put("lastUpdated", "1686558490205")
                        .put("loggedIn", true)
                        .put("password", JSONObject.NULL)
                        .put("registeredContext", Set.of())
                        .put("registrationDate", "1564660299981")
                        .put("role", targetUser.getRole())
                        .put("schoolId", targetUser.getSchoolId())
                )
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createResponse.getStatus());
    }

    /**
     * Checks that authenticated requests to update other users of each role by a user with the Teacher role are rejected.
     *
     * @throws Exception - not expected.
     */
    @ParameterizedTest
    @MethodSource("allTestUsersProvider")
    public void createOrUpdateEndpoint_updateAnotherUserWhileLoggedInAsTeacher_failsWithForbidden(RegisteredUserDTO targetUser) throws Exception {
        // Arrange
        // log in as teacher
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.DAVE_TEACHER_EMAIL, ITConstants.DAVE_TEACHER_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(request);

        // create request
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", ">:)") // this is the update
                        .put("firstLogin", targetUser.isFirstLogin())
                        .put("gender", targetUser.getGender())
                        .put("givenName", targetUser.getGivenName())
                        .put("id", targetUser.getId())
                        .put("lastSeen", "1686558490205")
                        .put("lastUpdated", "1686558490205")
                        .put("loggedIn", true)
                        .put("password", JSONObject.NULL)
                        .put("registeredContext", Set.of())
                        .put("registrationDate", "1564660299981")
                        .put("role", targetUser.getRole())
                        .put("schoolId", targetUser.getSchoolId())
                )
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), createResponse.getStatus());
    }
}
