package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UsersFacadeIT extends IsaacIntegrationTest {

    private UsersFacade usersFacade;

    @Before
    public void setUp() {
        this.usersFacade = new UsersFacade(properties, userAccountManager, logManager, userAssociationManager,
                misuseMonitor, userPreferenceManager, schoolListReader);
    }

    @After
    public void tearDown() throws SQLException {
        // reset created/updated users in DB, so all tests start from the same DB state
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
                        .put("loggedIn", "true")
                        .put("email", ITConstants.TEST_SIGNUP_EMAIL)
                        .put("password", ITConstants.TEST_SIGNUP_PASSWORD)
                        .put("familyName", "signup")
                        .put("givenName", "test")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse responseParam = createNiceMock(HttpServletResponse.class);
        replay(responseParam);

        // Act
        Response response = usersFacade.createOrUpdateUserSettings(request, responseParam, payload.toString());

        // Assert
        // check status code is 'OK'
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // check the relevant returned fields match what we provided
        assertEquals("test-signup@test.com", ((RegisteredUserDTO) response.getEntity()).getEmail());
        assertEquals("test", ((RegisteredUserDTO) response.getEntity()).getGivenName());
        assertEquals("signup", ((RegisteredUserDTO) response.getEntity()).getFamilyName());

        // check the other returned fields match the expected defaults
        assertEquals(Role.STUDENT, ((RegisteredUserDTO) response.getEntity()).getRole());
    }

    /**
     * Checks that signup attempts using an email address already associated with an account are rejected.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUserWithDuplicateEmail_failsWithError() throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("loggedIn", "true")
                        .put("email", ITConstants.TEST_STUDENT_EMAIL)
                        .put("password", ITConstants.TEST_STUDENT_PASSWORD)
                        .put("familyName", "student")
                        .put("givenName", "test")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse responseParam = createNiceMock(HttpServletResponse.class);
        replay(responseParam);

        // Act
        Response response = usersFacade.createOrUpdateUserSettings(request, responseParam, payload.toString());

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Checks that signup attempts with a malformed JSON object are rejected.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUserWithMalformedJsonObject_failsWithError() throws Exception {
        // Arrange
        String payload = "{try{to{parse{this!{}}";

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse responseParam = createNiceMock(HttpServletResponse.class);
        replay(responseParam);

        // Act
        Response response = usersFacade.createOrUpdateUserSettings(request, responseParam, payload);

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    /**
     * Checks that signup attempts with a weak password are rejected.
     *
     * @throws Exception - not expected.
     */
    @Test
    public void createOrUpdateEndpoint_registerNewUserWithWeakPassword_failsWithError() throws Exception {
        // Arrange
        JSONObject payload = new JSONObject()
                .put("registeredUser", new JSONObject()
                        .put("loggedIn", "true")
                        .put("email", ITConstants.TEST_SIGNUP_EMAIL)
                        .put("password", "a")
                        .put("familyName", "student")
                        .put("givenName", "test")
                )
                .put("userPreferences", new JSONObject())
                .put("passwordCurrent", JSONObject.NULL);

        HttpServletRequest request = createRequestWithSession();
        replay(request);

        HttpServletResponse responseParam = createNiceMock(HttpServletResponse.class);
        replay(responseParam);

        // Act
        Response response = usersFacade.createOrUpdateUserSettings(request, responseParam, payload.toString());

        // Assert
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }
}
