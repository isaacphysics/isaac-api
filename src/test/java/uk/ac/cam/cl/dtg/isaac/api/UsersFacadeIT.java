package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.segue.api.UsersFacade;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.stream.Stream;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;

public class UsersFacadeIT extends IsaacIntegrationTest {
    private UsersFacade usersFacade;
    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    @BeforeAll
    public static void beforeAll() {
        misuseMonitor.registerHandler(RegistrationMisuseHandler.class.getSimpleName(),
                new RegistrationMisuseHandler(emailManager, properties));
    }

    @BeforeEach
    public void beforeEach() {
        misuseMonitor.resetMisuseCount("0.0.0.0", RegistrationMisuseHandler.class.getSimpleName());
        this.usersFacade = new UsersFacade(properties, userAccountManager, logManager, userAssociationManager, misuseMonitor, userPreferenceManager, schoolListReader);
        mockRequest = createMockRequestObject();
        mockResponse = niceMock(HttpServletResponse.class);
    }

    private static HttpServletRequest createMockRequestObject() {
        HttpSession mockSession = createNiceMock(HttpSession.class);
        expect(mockSession.getAttribute(ANONYMOUS_USER)).andReturn(null).anyTimes();
        expect(mockSession.getId()).andReturn("sessionId").anyTimes();
        replay(mockSession);
        HttpServletRequest mockRequest = createNiceMock(HttpServletRequest.class);
        expect(mockRequest.getHeader("X-Forwarded-For")).andReturn("0.0.0.0").anyTimes();
        expect(mockRequest.getSession()).andReturn(mockSession).anyTimes();
        replay(mockRequest);
        return mockRequest;
    }

    @ParameterizedTest
    @MethodSource("validEmailProviders")
    public void createUser_validRegistrationParameters(String email) {
        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"%1$s\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"password\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                email);

        Response response = null;
        try {
            response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
        } catch (InvalidKeySpecException e) {
            fail("Unhandled InvalidKeySpecException during test");
        } catch (NoSuchAlgorithmException e) {
            fail("Unhandled NoSuchAlgorithmException during test");
        }
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    }

    private static Stream<Arguments> validEmailProviders() {
        return Stream.of(
                Arguments.of("new-student@test.com"),
                Arguments.of("new-student-google"),
                Arguments.of("new-student-twitter"),
                Arguments.of("new-student-facebook")
        );
    }

    @Test
    public void createUser_existingAccount() {
        String userObjectString = "{\"registeredUser\":{\"loggedIn\":true,\"email\":\"test-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"password\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null}";

        Response response = null;
        try {
            response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
        } catch (InvalidKeySpecException e) {
            fail("Unhandled InvalidKeySpecException during test");
        } catch (NoSuchAlgorithmException e) {
            fail("Unhandled NoSuchAlgorithmException during test");
        }
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    public void createUser_invalidEmail(String email) {
        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"%1$s\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"password\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                email);

        Response response = null;
        try {
            response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
        } catch (InvalidKeySpecException e) {
            fail("Unhandled InvalidKeySpecException during test");
        } catch (NoSuchAlgorithmException e) {
            fail("Unhandled NoSuchAlgorithmException during test");
        }
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    public void createUser_invalidPassword(String password) {
        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"new-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"%1$s\",\"familyName\":\"Test\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                password);

        Response response = null;
        try {
            response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
        } catch (InvalidKeySpecException e) {
            fail("Unhandled InvalidKeySpecException during test");
        } catch (NoSuchAlgorithmException e) {
            fail("Unhandled NoSuchAlgorithmException during test");
        }
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    public void createUser_invalidFamilyName(String familyName) {
        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"new-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"password\",\"familyName\":\"%1$s\",\"givenName\":\"Test\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                familyName);

        Response response = null;
        try {
            response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
        } catch (InvalidKeySpecException e) {
            fail("Unhandled InvalidKeySpecException during test");
        } catch (NoSuchAlgorithmException e) {
            fail("Unhandled NoSuchAlgorithmException during test");
        }
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    public void createUser_invalidGivenName(String givenName) {
        String userObjectString = String.format("{\"registeredUser\":{\"loggedIn\":true,\"email\":\"new-student@test.com\",\"dateOfBirth\":\"2000-01-01T00:00:00.000Z\",\"password\":\"password\",\"familyName\":\"Test\",\"givenName\":\"%1$s\"},\"userPreferences\":{},\"passwordCurrent\":null}",
                givenName);

        Response response = null;
        try {
            response = usersFacade.createOrUpdateUserSettings(mockRequest, mockResponse, userObjectString);
        } catch (InvalidKeySpecException e) {
            fail("Unhandled InvalidKeySpecException during test");
        } catch (NoSuchAlgorithmException e) {
            fail("Unhandled NoSuchAlgorithmException during test");
        }
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    private static Stream<Arguments> invalidEmails() {
        return Stream.of(
                // Isaac addresses are forbidden
                Arguments.of("new-student@isaaccomputerscience.org"),
                // Email field cannot be empty
                Arguments.of(""),
                // Email field cannot have conecutive .s
                Arguments.of("new-student@test..com"),
                // Email field must contain an @
                Arguments.of("new-student.test.com"),
                // Email field must match an @x.y pattern
                Arguments.of("new-student@testcom")
        );
    }

    private static Stream<Arguments> invalidPasswords() {
        return Stream.of(
                // Password cannot be empty
                Arguments.of(""),
                // Password must be at least minimum length
                Arguments.of("pass")
        );
    }

    private static Stream<Arguments> invalidNames() {
        return Stream.of(
                // Names cannot be empty
                Arguments.of(""),
                // Names cannot exceed maximum length
                Arguments.of("a".repeat(256)),
                // Names cannot contain forbidden characters
                Arguments.of("Te*st")
        );
    }
}
