package uk.ac.cam.cl.dtg.isaac.api;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.IsaacTest;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.MicrosoftAuthenticator;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.easymock.EasyMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static uk.ac.cam.cl.dtg.isaac.IsaacTest.*;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.*;

public class AuthenticationFacadeIT extends Helpers {
    @Nested
    class MicrosoftAuthenticationCallback {
        @Nested
        class UnsuccessfulAuthentication {
            @Test
            public void csrfTokenMissing_returnsErrorResponse() throws Exception {
                var response = testAuthenticationCallback(null, "");
                response.assertError("CSRF check failed", Response.Status.UNAUTHORIZED);
            }

            @Test
            public void authCodeMissing_throwsException() {
                var query = String.format("?state=%s", csrfToken);
                assertThrows(IllegalArgumentException.class, () -> testAuthenticationCallback(null, query));
            }

            @Test
            public void tokenMissing_returnsErrorResponse() throws Exception {
                var response = testAuthenticationCallback(null, validQuery);
                response.assertError("Token verification: TOKEN_MISSING", Response.Status.UNAUTHORIZED);
            }

            @Test
            public void missingEmail_returnsErrorResponse() throws Exception {
                var token = validToken(t -> t, p -> p.put("email", null));
                var response = testAuthenticationCallback(token, validQuery);
                response.assertError(noUserMessage, Response.Status.UNAUTHORIZED);
            }
        }

        @Nested
        class SuccessfulAuthentication {
            @Nested
            class SignIn {
                @Test
                public void matchedAccountNotConnected_returnsNotUsingMicrosoftResponse() throws Exception {
                    var token = validToken(t -> t, p -> {
                        p.put("sub", "some_non_matching_sub");
                        p.put("email", CHARLIE_STUDENT_EMAIL);
                        return null;
                    });
                    var response = testAuthenticationCallback(token, validQuery);
                    response.assertError(notUsingMicrosoftMessage, Response.Status.FORBIDDEN);
                    response.assertNoUserLoggedIn();
                }

                @Test
                public void matchedAccountConnected_signInAndReturnsUser() throws Exception {
                    var token = validToken(t -> t, p -> p.put("sub", ERIKA_PROVIDER_USER_ID));
                    var response = testAuthenticationCallback(token, validQuery);
                    response.assertUserReturned(ERIKA_STUDENT_EMAIL);
                    response.assertUserLoggedIn(ERIKA_STUDENT_ID);
                }
            }

            @Nested
            class SignUp {
                @Test
                public void completePayload_registersUser() throws Exception {
                    var nextId = nextUserIdFromDb();
                    var token = validToken(t -> t, p -> {
                        p.put("sub", "new_student_provider_user_id");
                        p.put("email", "new_student@outlook.com");
                        p.put("given_name", "New");
                        p.put("family_name", "Student");
                        return null;
                    });
                    var response = testAuthenticationCallback(token, validQuery);
                    response.assertUserLoggedIn(nextId);
                    assertEquals("new_student@outlook.com", response.getUser().getEmail());
                    assertEquals("New", response.getUser().getGivenName());
                    assertEquals("Student", response.getUser().getFamilyName());
                }

                @Test
                public void incompletePayload_returnsError() throws Exception {
                    var token = validToken(t -> t, p -> {
                        p.put("sub", "new_student_2_provider_user_id");
                        p.put("email", "new_student_2@outlook.com");
                        p.remove("given_name");
                        p.remove("family_name");
                        p.remove("name");
                        return null;
                    });
                    var response = testAuthenticationCallback(token, validQuery);
                    response.assertError(noUserMessage, Response.Status.UNAUTHORIZED);
                    response.assertNoUserLoggedIn();
                }
            }

            @Test
            public void cachesJwksCalls() throws Exception {
                var server = startKeySetServer(8888, List.of(validSigningKey));
                try {
                    var subject = subject(validToken(t -> t, p -> p));
                    var url = String.format("http://isaacphysics.org/auth/microsoft/callback%s", validQuery);
                    subject.authenticationCallback(request(url), response().getLeft(), "microsoft");
                    subject.authenticationCallback(request(url), response().getLeft(), "microsoft");
                    assertEquals(1, server.getRight().getRequestCount());
                } finally {
                    server.getLeft().stop();
                };
            }
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
    static class CallbackResponse extends IsaacIntegrationTest {
        private final Response responseReturned;
        private final Pair<HttpServletResponse, Capture<Cookie>> rersponsePassedIn;

        CallbackResponse(Response response, Pair<HttpServletResponse, Capture<Cookie>> passedInResponse) {
            this.responseReturned = response;
            this.rersponsePassedIn = passedInResponse;
        }

        void assertError(String message, Response.Status status) {
            assertEquals(responseReturned.readEntity(SegueErrorResponse.class).getErrorMessage(), message);
            assertEquals(status.getStatusCode(), responseReturned.getStatus());
        }

        void assertUserReturned(String email) throws Exception {
            assertEquals(responseReturned.getStatus(), Response.Status.OK.getStatusCode());
            assertEquals(getUser(), userAccountManager.getUserDTOByEmail(email));
        }

        void assertUserLoggedIn(Number userId) throws Exception {
            assertEquals(responseReturned.getStatus(), Response.Status.OK.getStatusCode());

            var capture = rersponsePassedIn.getRight();
            var session = getSessionInformationFromCookie(capture.getValue());
            assertEquals(userId.toString(), session.get("id"));
        }

        void assertNoUserLoggedIn() {
            assertFalse(rersponsePassedIn.getRight().hasCaptured());
        }

        RegisteredUserDTO getUser() {
            return responseReturned.readEntity(RegisteredUserDTO.class);
        }
    }

    public CallbackResponse testAuthenticationCallback(String token, String query) throws Exception {
        var subject = subject(token);
        var keySetServer = IsaacTest.startKeySetServer(8888, List.of(validSigningKey));
        try {
            var url = String.format("http://isaacphysics.org/auth/microsoft/callback%s", query);
            var passedResponse = response();
            var response = subject.authenticationCallback(request(url), passedResponse.getLeft(), "microsoft");
            return new CallbackResponse(response, passedResponse);
        } finally {
            keySetServer.getLeft().stop();
        }
    }

    static AuthenticationFacade subject(String token) {
        try {
            var microsoftAuthenticator = new MicrosoftAuthenticator(clientId, tenantId, null,
                    "http://localhost:8888/keys", "") {
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
    }

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

    Pair<HttpServletResponse, Capture<Cookie>> response() {
        Capture<Cookie> responseCookieCapture = Capture.newInstance();
        var response = createResponseAndCaptureCookies(responseCookieCapture);
        replay(response);
        return Pair.of(response, responseCookieCapture);
    }

    static HttpSession session() {
        HttpSession session = createNiceMock(HttpSession.class);
        expect(session.getAttribute("state")).andStubReturn(csrfToken);
        expect(session.getId()).andStubReturn("the_session_id");
        replay(session);
        return session;
    }

    static Number nextUserIdFromDb() throws Exception {
        var users = userAccountManager.findUsers(LongStream.range(0, 100).boxed().collect(Collectors.toList()));
        var largestIdUser = users.stream().max(Comparator.comparingLong(RegisteredUserDTO::getId));
        return largestIdUser.map(u -> u.getId() + 1).orElse(null);
    }

    static String csrfToken = "the_csrf_token";
    static String validQuery = String.format("?state=%s&code=123", csrfToken);
    static String notUsingMicrosoftMessage = "You do not use Microsoft to log in. You may have registered using" +
            " a different provider, or your email address and password.";
    static String noUserMessage = "Unable to locate user information.";
}