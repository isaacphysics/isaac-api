package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.ac.cam.cl.dtg.isaac.dos.ExamBoard;
import uk.ac.cam.cl.dtg.isaac.dos.Stage;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAuthenticationManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IDeletionTokenPersistenceManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ALICE_STUDENT_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.TEST_TEACHER_PASSWORD;


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
        try (Connection conn = postgresSqlDb.getDatabaseConnection();
             PreparedStatement pst = conn.prepareStatement("DELETE FROM users WHERE email in (?);")) {
            pst.setString(1, ITConstants.TEST_SIGNUP_EMAIL);
            pst.executeUpdate();
        }
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
     * Tests user registration as a teacher where ALLOW_DIRECT_TEACHER_SIGNUP... is enabled. We expect to receive an
     * Accepted response containing 'EMAIL_VERIFICATION_REQUIRED' and a caveat cookie.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerAsTeacherWithSignUpFlagEnabled_acceptedAndSetsCaveatCookie()
            throws Exception {
        // Arrange
        // inject config with feature flag enabled
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml,"
                        + "src/test/resources/segue-integration-test-teacher-signup-override.yaml"
        );

        UserAccountManager userAccountManagerForTest = new UserAccountManager(pgUsers, questionManager,
                propertiesForTest, providersToRegister, mapperFacade, emailManager, pgAnonymousUsers, logManager,
                userAuthenticationManager, secondFactorManager, userPreferenceManager, microsoftAutoLinkingConfig);

        UsersFacade usersFacadeForTest = new UsersFacade(propertiesForTest, userAccountManagerForTest, logManager,
                userAssociationManager, misuseMonitor, userPreferenceManager, schoolListReader);

        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", ITConstants.TEST_SIGNUP_EMAIL)
                        .put("password", ITConstants.TEST_SIGNUP_PASSWORD)
                        .put("familyName", "signup")
                        .put("givenName", "test")
                        .put("role", "TEACHER")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        Capture<Cookie> cookieToCapture = Capture.newInstance();
        HttpServletResponse response = createResponseAndCaptureCookies(cookieToCapture);
        replay(response);

        // Act
        Response createResponse = usersFacadeForTest.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        // check status code is 'Accepted' with the expected body
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), createResponse.getStatus());
        assertEquals(true, ((Map<String, Boolean>) createResponse.getEntity()).get("EMAIL_VERIFICATION_REQUIRED"));

        // check we have an auth cookie with INCOMPLETE_MANDATORY_EMAIL_VERIFICATION caveat only
        assertEquals("SEGUE_AUTH_COOKIE", cookieToCapture.getValue().getName());
        assertEquals(List.of(Constants.AuthenticationCaveat.INCOMPLETE_MANDATORY_EMAIL_VERIFICATION.name()),
                getCaveatsFromCookie(cookieToCapture.getValue()));

        // check the user record has role of teacher and the pending flag set
        assertEquals(Role.TEACHER, pgUsers.getByEmail(ITConstants.TEST_SIGNUP_EMAIL).getRole());
        assertEquals(true, pgUsers.getByEmail(ITConstants.TEST_SIGNUP_EMAIL).getTeacherAccountPending());
    }

    /**
     * Tests user registration as a teacher where ALLOW_DIRECT_TEACHER_SIGNUP... is disabled. This should ignore the
     * requested teacher role and create a student account as is default.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerAsTeacherWithSignUpFlagDisabled_succeedsWithRoleSelectionIgnored()
            throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", ITConstants.TEST_SIGNUP_EMAIL)
                        .put("password", ITConstants.TEST_SIGNUP_PASSWORD)
                        .put("familyName", "signup")
                        .put("givenName", "test")
                        .put("role", "TEACHER")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        Capture<Cookie> cookieToCapture = Capture.newInstance();
        HttpServletResponse response = createResponseAndCaptureCookies(cookieToCapture);
        replay(response);

        // Act
        Response createResponse = usersFacade.createOrUpdateUserSettings(request, response, payload.toString());

        // Assert
        // check status code is 'OK'
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());
        // check we weren't given a teacher account, instead defaulting back to student
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
        RegisteredUser targetUser = integrationTestUsers.TEST_STUDENT;

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
    public void createOrUpdateEndpoint_updateUserWhileNotLoggedIn_failsWithUnauthorised(RegisteredUser targetUser) throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", "Person") // this is the update
                        .put("firstLogin", false)
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
        // check initial password hash
        String initialPasswordHash = passwordDataManager.getLocalUserCredential(ITConstants.ERIKA_STUDENT_ID).getPassword();

        // log in as student
        RegisteredUser targetUser = integrationTestUsers.ERIKA_STUDENT;

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

        // check that password was changed in DB
        assertNotEquals(passwordDataManager.getLocalUserCredential(ITConstants.ERIKA_STUDENT_ID).getPassword(), initialPasswordHash);

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
        // check initial password hash
        String initialPasswordHash = passwordDataManager.getLocalUserCredential(ITConstants.ERIKA_STUDENT_ID).getPassword();

        // log in as Student
        RegisteredUser targetUser = integrationTestUsers.ERIKA_STUDENT;

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

        // check that password was not changed in DB
        assertEquals(passwordDataManager.getLocalUserCredential(ITConstants.ERIKA_STUDENT_ID).getPassword(), initialPasswordHash);
    }

    /**
     * Checks that authenticated requests to update other users of each role by a user with the Student role are rejected.
     *
     * @throws Exception - not expected.
     */
    @ParameterizedTest
    @MethodSource("allTestUsersProvider")
    public void createOrUpdateEndpoint_updateAnotherUserWhileLoggedInAsStudent_failsWithForbidden(RegisteredUser targetUser) throws Exception {
        // Arrange
        // log in as Student
        LoginResult studentLogin = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        // build update request
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", "Person") // this is the update
                        .put("firstLogin", false)
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
    public void createOrUpdateEndpoint_updateAnotherUserWhileLoggedInAsTeacher_failsWithForbidden(RegisteredUser targetUser) throws Exception {
        // Arrange
        // log in as Teacher
        LoginResult teacherLogin = loginAs(httpSession, ITConstants.DAVE_TEACHER_EMAIL, ITConstants.DAVE_TEACHER_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(request);

        // create request
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", "Person") // this is the update
                        .put("firstLogin", false)
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
     * Checks that authenticated requests to update other users of each role by a user with the Content Editor role are rejected.
     *
     * @throws Exception - not expected.
     */
    @ParameterizedTest
    @MethodSource("allTestUsersProvider")
    public void createOrUpdateEndpoint_updateAnotherUserWhileLoggedInAsEditor_failsWithForbidden(RegisteredUser targetUser) throws Exception {
        // Arrange
        // log in as Content Editor
        LoginResult editorLogin = loginAs(httpSession, ITConstants.FREDDIE_EDITOR_EMAIL, ITConstants.FREDDIE_EDITOR_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{editorLogin.cookie});
        replay(request);

        // create request
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", "Person") // this is the update
                        .put("firstLogin", false)
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
     * Checks that authenticated requests to update other users of each role by a user with the Event Manager role are
     * successful.
     *
     * @throws Exception - not expected.
     */
    @ParameterizedTest
    @MethodSource("allTestUsersProvider")
    public void createOrUpdateEndpoint_updateAnotherUserWhileLoggedInAsEventManager_succeeds(RegisteredUser targetUser) throws Exception {
        // Arrange
        // log in as Event Manager
        LoginResult eventManagerLogin = loginAs(httpSession, ITConstants.GARY_EVENTMANAGER_EMAIL, ITConstants.GARY_EVENTMANAGER_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{eventManagerLogin.cookie});
        replay(request);

        // create request
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("email", targetUser.getEmail())
                        .put("emailVerificationStatus", targetUser.getEmailVerificationStatus())
                        .put("familyName", "Person") // this is the update
                        .put("firstLogin", false)
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
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());
    }

    @Test
    public void upgradeToTeacherAccountEndpoint_upgradeWhileLoggedInAsStudentAndAllowUpgradeFlagEnabled_succeeds() throws Exception {
        // Arrange
        // inject config with feature flag enabled
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml," +
                        "src/test/resources/segue-integration-test-teacher-override.yaml"
        );
        UsersFacade usersFacadeForTest = new UsersFacade(propertiesForTest, userAccountManager, logManager, userAssociationManager,
                misuseMonitor, userPreferenceManager, schoolListReader);

        // log in as Student
        LoginResult studentLogin = loginAs(httpSession, ITConstants.ALICE_STUDENT_EMAIL, ITConstants.ALICE_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        // Act
        Response createResponse = usersFacadeForTest.upgradeCurrentAccountRole(request, Role.TEACHER.toString());

        // Assert
        assertEquals(Response.Status.OK.getStatusCode(), createResponse.getStatus());

        // check role was changed in DB
        assertEquals(Role.TEACHER, pgUsers.getById(ITConstants.ALICE_STUDENT_ID).getRole());
    }

    @Test
    public void upgradeToTeacherAccountEndpoint_upgradeWhileLoggedInAsStudentAndAllowUpgradeFlagDisabled_failsWithNotImplemented() throws Exception {
        // Arrange
        // log in as Student
        LoginResult studentLogin = loginAs(httpSession, ITConstants.ERIKA_STUDENT_EMAIL, ITConstants.ERIKA_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        // Act
        Response createResponse = usersFacade.upgradeCurrentAccountRole(request, Role.TEACHER.toString());

        // Assert
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), createResponse.getStatus());

        // check role was not changed in DB
        assertEquals(Role.STUDENT, pgUsers.getById(ITConstants.ERIKA_STUDENT_ID).getRole());
    }

    @Test
    public void upgradeToTeacherAccountEndpoint_upgradeWhileLoggedInAsStudentWithUnverifiedEmail_failsWithBadRequest() throws Exception {
        // Arrange
        // inject config with feature flag enabled
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml," +
                        "src/test/resources/segue-integration-test-teacher-override.yaml"
        );
        UsersFacade usersFacadeForTest = new UsersFacade(propertiesForTest, userAccountManager, logManager, userAssociationManager,
                misuseMonitor, userPreferenceManager, schoolListReader);

        // log in as Student
        LoginResult studentLogin = loginAs(httpSession, ITConstants.CHARLIE_STUDENT_EMAIL, ITConstants.CHARLIE_STUDENT_PASSWORD);
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{studentLogin.cookie});
        replay(request);

        // Act
        Response createResponse = usersFacadeForTest.upgradeCurrentAccountRole(request, Role.TEACHER.toString());

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());

        // check role was not changed in DB
        assertEquals(Role.STUDENT, pgUsers.getById(ITConstants.CHARLIE_STUDENT_ID).getRole());
    }

    @Test
    public void upgradeToTeacherAccountEndpoint_upgradeWhileNotLoggedInAndAllowUpgradeFlagEnabled_failsWithUnauthorized() throws Exception {
        // Arrange
        // inject config with feature flag enabled
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml," +
                        "src/test/resources/segue-integration-test-teacher-override.yaml"
        );
        UsersFacade usersFacadeForTest = new UsersFacade(propertiesForTest, userAccountManager, logManager, userAssociationManager,
                misuseMonitor, userPreferenceManager, schoolListReader);

        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        replay(request);

        // Act
        Response createResponse = usersFacadeForTest.upgradeCurrentAccountRole(request, Role.TEACHER.toString());

        // Assert
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), createResponse.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            ITConstants.TEST_TUTOR_EMAIL,
            ITConstants.TEST_TEACHER_EMAIL,
            ITConstants.TEST_EVENTMANAGER_EMAIL,
            ITConstants.TEST_EDITOR_EMAIL,
        }
    )
    public void upgradeToTeacherAccountEndpoint_upgradeWhileLoggedInAsNonStudentAndAllowUpgradeFlagEnabled_failsWithBadRequest(
            @ConvertWith(ITUsers.EmailToRegisteredUserArgumentConverter.class) RegisteredUser user) throws Exception {
        // Arrange
        // inject config with feature flag enabled
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml," +
                        "src/test/resources/segue-integration-test-teacher-override.yaml"
        );
        UsersFacade usersFacadeForTest = new UsersFacade(propertiesForTest, userAccountManager, logManager, userAssociationManager,
                misuseMonitor, userPreferenceManager, schoolListReader);

        // log in as user
        LoginResult login = loginAs(httpSession, user.getEmail(), "test1234");
        HttpServletRequest request = createRequestWithCookies(new Cookie[]{login.cookie});
        replay(request);

        // Act
        Response createResponse = usersFacadeForTest.upgradeCurrentAccountRole(request, Role.TEACHER.toString());

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), createResponse.getStatus());

        // check role was not changed in DB
        assertEquals(user.getRole(), pgUsers.getById(user.getId()).getRole());
    }


    @Test
    public void generatePasswordResetTokenForOtherUser_teacherResetsStudentPassword_studentReceiveResetEmail() throws Exception {
        // Arrange
        // inject EmailManager mock to verify email sent
        EmailManager dummyEmailManager = createMock(EmailManager.class);
        IDeletionTokenPersistenceManager dummyDeletionTokenManager = createMock(IDeletionTokenPersistenceManager.class);
        UserAuthenticationManager userAuthenticationManager = new UserAuthenticationManager(
                pgUsers, dummyDeletionTokenManager, properties, providersToRegister, dummyEmailManager);
        UserAccountManager userAccountManager = new UserAccountManager(
                pgUsers, questionManager, properties, providersToRegister, mapperFacade, emailManager, pgAnonymousUsers,
                logManager, userAuthenticationManager, secondFactorManager, userPreferenceManager, microsoftAutoLinkingConfig);
        UsersFacade usersFacadeForTest = new UsersFacade(properties, userAccountManager, logManager,
                userAssociationManager, misuseMonitor, userPreferenceManager, schoolListReader);

        // create email template
        EmailTemplateDTO template = new EmailTemplateDTO("password reset test");

        // setup capture
        Capture<Map<String, Object>> capturedEmailValues = newCapture();

        // setup mock
        expect(dummyEmailManager.getEmailTemplateDTO("email-template-password-reset")).andReturn(template);
        dummyEmailManager.sendTemplatedEmailToUser(anyObject(), eq(template), capture(capturedEmailValues), anyObject());
        expectLastCall().once();

        replay(dummyEmailManager);

        // Log in as teacher, create request
        LoginResult teacherLogin = loginAs(httpSession, TEST_TEACHER_EMAIL, TEST_TEACHER_PASSWORD);
        HttpServletRequest resetPasswordRequest = createRequestWithCookies(new Cookie[]{teacherLogin.cookie});
        replay(resetPasswordRequest);

        // Act
        Response resetPasswordResponse = usersFacadeForTest.generatePasswordResetTokenForOtherUser(
                createMock(Request.class), resetPasswordRequest, ALICE_STUDENT_ID);

        String resetLink = (String) capturedEmailValues.getValue().get("resetURL");
        String[] splitResetLink = resetLink.split("/");
        String token = splitResetLink[splitResetLink.length-1];

        Response validatePasswordResponse = usersFacadeForTest.validatePasswordResetRequest(token);

        // Assert
        // check statuses OK
        assertEquals(Response.Status.OK.getStatusCode(), resetPasswordResponse.getStatus());
        assertEquals(Response.Status.OK.getStatusCode(), validatePasswordResponse.getStatus());
    }
}
