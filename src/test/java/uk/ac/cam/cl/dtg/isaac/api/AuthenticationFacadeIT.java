package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.MicrosoftAuthenticator;

import java.net.URI;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cl.dtg.isaac.IsaacTest.*;

public class AuthenticationFacadeIT extends Helpers {
    @Nested
    class AuthenticationCallback {
        @Test
        public void csrfTokenMissing_returnsErrorResponse() throws Exception {
            var response = testAuthenticationCallback(null, request(endpointURL), response(), "microsoft");
            assertErrorResponse(response, "CSRF check failed", Response.Status.UNAUTHORIZED);
        }

        @Test
        public void authCodeMissing_throwsException() {
            var url = String.format("%s?state=%s", endpointURL, csrfToken);
            assertThrows(IllegalArgumentException.class,
                    () -> testAuthenticationCallback(null, request(url), response(), "microsoft"));
        }

        @Test
        public void tokenMissing_returnsErrorResponse() throws Exception {
            var url = String.format("%s?state=%s&code=123", endpointURL, csrfToken);
            var response = testAuthenticationCallback(null, request(url), response(), "microsoft");
            assertErrorResponse(response, "Token verification: TOKEN_MISSING", Response.Status.UNAUTHORIZED);
        }

        @Test
        public void missingEmail_returnsErrorResponse() throws Exception {
            var url = String.format("%s?state=%s&code=123", endpointURL, csrfToken);
            var token = validToken(t -> t, p -> p.put("email", null));
            var response = testAuthenticationCallback(token, request(url), response(), "microsoft");
            assertErrorResponse(response, "Unable to locate user information.", Response.Status.UNAUTHORIZED);
        }

        @Test
        public void noConnectedAccount_returnsNotUsingMicrosoftResponse() throws Exception {
            var url = String.format("%s?state=%s&code=123", endpointURL, csrfToken);
            var token = validToken(t -> t, p -> p.put("email", "charlie-student@test.com"));
            var response = testAuthenticationCallback(token, request(url), response(), "microsoft");
            assertErrorResponse(response,"You do not use Microsoft to log in.", Response.Status.FORBIDDEN);
        }
    }

    @Nested
    class RegisterWithRaspberryPiAuthenticator {
        @Test
        public void notInitialSignup_omitsForceSignUpParameterFromRedirectURL() {
            var response = subject(null).authenticate(request(""), "RASPBERRYPI", false);

            // Assert
            // check status code is OK
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // check force_signup parameter was not added to redirect URL
            var uri = ((Map<String, URI>) response.getEntity()).get("redirectUrl");
            assertFalse(uri.getQuery().contains("force_signup"));
        }

        @Test
        public void initialSignup_addsForceSignUpParameterToRedirectURL() {
            var response = subject(null).authenticate(request(""), "RASPBERRYPI", true);

            // Assert
            // check status code is OK
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // check force_signup parameter was added to redirect URL
            var uri = ((Map<String, URI>) response.getEntity()).get("redirectUrl");
            assertTrue(uri.getQuery().contains("force_signup"));
        }
    }
}

class Helpers extends IsaacIntegrationTest {
    public Response testAuthenticationCallback(String token, HttpServletRequest req, HttpServletResponse resp, String provider) throws Exception {
        var subject = subject(token);
        var keySetServer = IsaacTest.startKeySetServer(8888, Stream.of(validSigningKey));
        try {
            return subject.authenticationCallback(req, resp, provider);
        } finally {
            keySetServer.stop();
        }
    }

    static AuthenticationFacade subject(String token) {
        try {
            var microsoftAuthenticator = new MicrosoftAuthenticator(clientId, tenantId, null,
                    "http://localhost:8888/keys") {
                public String exchangeCode(String authorizationCode) {
                    var internalCredentialID = UUID.randomUUID().toString();
                    if (null != token) {
                        credentialStore.put(internalCredentialID, token);
                    }
                    return internalCredentialID;
                }
            };
            providersToRegister.put(AuthenticationProvider.MICROSOFT, microsoftAuthenticator);
            return new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    };

    static HttpServletRequest request(String url) {
        HttpServletRequest request = createNiceMock(HttpServletRequest.class);
        Function<Integer, String> splitUrl = i -> {
            try {
                return url.split("\\?")[i];
            } catch (ArrayIndexOutOfBoundsException e) {
                return "";
            }
        };
        expect(request.getRequestURL()).andStubAnswer(() -> new StringBuffer(splitUrl.apply(0)));
        expect(request.getParameter(EasyMock.anyString())).andStubAnswer(() -> {
            var name = (String) EasyMock.getCurrentArguments()[0];
            return new URIBuilder(url).getQueryParams().stream()
                    .filter(p -> p.getName().equals(name))
                    .map(NameValuePair::getValue)
                    .findFirst().orElse(null);
        });
        expect(request.getQueryString()).andStubAnswer(() -> splitUrl.apply(1));
        expect(request.getSession()).andStubReturn(session());
        replay(request);
        return request;
    }

    static HttpServletResponse response() {
        HttpServletResponse response = createNiceMock(HttpServletResponse.class);
        replay(response);
        return response;
    }

    static HttpSession session() {
        HttpSession session = createNiceMock(HttpSession.class);
        expect(session.getAttribute("state")).andStubReturn(csrfToken);
        replay(session);
        return session;
    }

    static void assertErrorResponse(Response response, String message, Response.Status status) {
        assertTrue(response.readEntity(SegueErrorResponse.class).getErrorMessage().startsWith(message));
        assertEquals(status.getStatusCode(), response.getStatus());
    }

    static String csrfToken = "the_csrf_token";
    static String endpointURL = "http://isaacphysics.org/auth/microsoft/callback";
}