package uk.ac.cam.cl.dtg.isaac.api;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.AuthenticationFacade;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.MicrosoftAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.microsoft.KeyPair;
import uk.ac.cam.cl.dtg.segue.auth.microsoft.KeySetServlet;
import uk.ac.cam.cl.dtg.segue.auth.microsoft.Token;

import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.CHARLIE_STUDENT_EMAIL;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ERIKA_PROVIDER_USER_ID;
import static uk.ac.cam.cl.dtg.isaac.api.ITConstants.ERIKA_STUDENT_ID;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class AuthenticationFacadeIT extends IsaacIntegrationTestWithREST {
    @Nested
    class MicrosoftAuthenticationCallback {
        @Nested
        class UnsuccessfulAuthentication {
            @Test
            public void csrfTokenMissing_returnsErrorResponse() throws Exception {
                providersToRegister.put(AuthenticationProvider.MICROSOFT, msAuth());
                var response = subject().client().get("/auth/microsoft/callback");
                response.assertError("CSRF check failed", Response.Status.UNAUTHORIZED);
            }

            @Test
            public void authCodeMissing_returnsErrorResponse() throws Exception {
                providersToRegister.put(AuthenticationProvider.MICROSOFT, msAuth());
                var client = subject().setSessionAttributes(Map.of("state", csrfToken)).client();
                var response = client.get("/auth/microsoft/callback?state=" + csrfToken);
                response.assertError("Error extracting authentication code.", Response.Status.UNAUTHORIZED);
            }

            @Test
            public void authCodeInvalid_returnsErrorResponse() throws Exception {
                providersToRegister.put(AuthenticationProvider.MICROSOFT, msAuth());
                var client = subject().setSessionAttributes(Map.of("state", csrfToken)).client();
                var response = client.get("/auth/microsoft/callback?state=" + csrfToken + "&code=invalid");
                response.assertError("There was an error exchanging the code.", Response.Status.UNAUTHORIZED);
            }

            @Test
            public void tokenMissing_returnsErrorResponse() throws Exception {
                providersToRegister.put(AuthenticationProvider.MICROSOFT, msAuth().mockExchange(null));
                var client = subject().setSessionAttributes(Map.of("state", csrfToken)).client();
                var response = client.get("/auth/microsoft/callback" + validQuery);
                response.assertError("Token verification: TOKEN_MISSING", Response.Status.UNAUTHORIZED);
            }

            @Test
            public void missingEmail_returnsErrorResponse() throws Exception {
                var client = prepareTestCase(token.valid(s -> s, u -> u.put("email", null)));
                var response = client.get("/auth/microsoft/callback" + validQuery);
                response.assertError(noUserMessage, Response.Status.UNAUTHORIZED);
            }
        }

        @Nested
        class SuccessfulAuthentication {
            @Nested
            class SignIn {
                @Test
                public void matchedAccountNotConnected_returnsNotUsingMicrosoftResponse() throws Exception {
                    var client = prepareTestCase(token.valid(s -> s, u -> {
                        u.put("oid", UUID.randomUUID().toString());
                        u.put("email", CHARLIE_STUDENT_EMAIL);
                        return null;
                    }));

                    var response = client.get("/auth/microsoft/callback" + validQuery);

                    response.assertError(notUsingMicrosoftMessage, Response.Status.FORBIDDEN);
                    response.assertNoUserLoggedIn();

                }

                @Test
                public void matchedAccountConnected_signInAndReturnsUser() throws Exception {
                    var client = prepareTestCase(token.valid(s -> s, u -> u.put("oid", ERIKA_PROVIDER_USER_ID)));

                    var response = client.get("/auth/microsoft/callback" + validQuery);

                    response.assertEntityReturned(userAccountManager.getUserDTOById(ERIKA_STUDENT_ID));
                    response.assertUserLoggedIn(ERIKA_STUDENT_ID);
                }
            }

            @Nested
            class SignUp {
                @Test
                public void completePayload_registersUser() throws Exception {
                    var nextId = nextUserIdFromDb();
                    var client = prepareTestCase(token.valid(s -> s, u -> {
                        u.put("oid", UUID.randomUUID().toString());
                        u.put("email", "new_student@outlook.com");
                        u.put("given_name", "New");
                        u.put("family_name", "Student");
                        return null;
                    }));

                    var response = client.get("/auth/microsoft/callback" + validQuery);

                    response.assertUserLoggedIn(nextId);
                    var user = response.readEntity(RegisteredUserDTO.class);
                    assertEquals("new_student@outlook.com", user.getEmail());
                    assertEquals("New", user.getGivenName());
                    assertEquals("Student", user.getFamilyName());
                }

                @Test
                public void incompletePayload_returnsError() throws Exception {
                    var client = prepareTestCase(token.valid(s -> s, u -> {
                        u.put("oid", UUID.randomUUID().toString());
                        u.put("email", "new_student_2@outlook.com");
                        u.remove("given_name");
                        u.remove("family_name");
                        u.remove("name");
                        return null;
                    }));

                    var response = client.get("/auth/microsoft/callback" + validQuery);

                    response.assertError(noUserMessage, Response.Status.UNAUTHORIZED);
                    response.assertNoUserLoggedIn();
                }
            }

            @Test
            public void cachesJwksCalls() throws Exception {
                var keySetServletHolder = new Stack<KeySetServlet>();
                var client = prepareTestCase(token.valid(s -> s, u -> u), keySetServletHolder);

                client.get("/auth/microsoft/callback" + validQuery);
                client.get("/auth/microsoft/callback" + validQuery);

                assertEquals(1, keySetServletHolder.pop().getRequestCount());
            }
        }
    }

    @Nested
    class RegisterWithRaspberryPiAuthenticator {
        @Test
        public void notInitialSignup_omitsForceSignUpParameterFromRedirectURL() throws Exception {
            var response = subject().client().get("/auth/raspberrypi/authenticate?signup=false");
            var redirectUrl = response.readEntity(Map.class).get("redirectUrl");
            assertThat(redirectUrl).isInstanceOf(String.class).asString().doesNotContain("force_signup");
        }

        @Test
        public void initialSignup_addsForceSignUpParameterToRedirectURL() throws Exception {
            var response = subject().client().get("/auth/raspberrypi/authenticate?signup=true");
            var redirectUrl = response.readEntity(Map.class).get("redirectUrl");
            assertThat(redirectUrl).isInstanceOf(String.class).asString().contains("force_signup");
        }
    }

    TestServer subject() throws Exception {
        return startServer(new AuthenticationFacade(properties, userAccountManager, logManager, misuseMonitor));
    }

    TestClient prepareTestCase(final String token) throws Exception {
        return prepareTestCase(token, new Stack<>());
    }

    TestClient prepareTestCase(final String token, final Stack<KeySetServlet> keySetServletHolder) throws Exception {
        providersToRegister.put(AuthenticationProvider.MICROSOFT, msAuth().mockExchange(token));
        var keySetServer = KeySetServlet.startServer(8888, List.of(validSigningKey));
        registerCleanup(() -> keySetServer.getLeft().stop());
        keySetServletHolder.push(keySetServer.getRight());
        return subject().setSessionAttributes(Map.of("state", csrfToken)).client();
    }

    static class MockingMicrosoftAuthenticator extends MicrosoftAuthenticator {
        String clientSecret;
        String jwksUrl;
        String redirectUrl;

        public MockingMicrosoftAuthenticator(
            final String clientId,
            final String tenantId,
            final String clientSecret,
            final String jwksUrl,
            final String redirectUrL
        ) {
            super(clientId, tenantId, clientSecret, jwksUrl, redirectUrL);
            this.clientSecret = clientSecret;
            this.jwksUrl = jwksUrl;
            this.redirectUrl = redirectUrL;
        }

        MicrosoftAuthenticator mockExchange(final String token) {
            return new MicrosoftAuthenticator(clientId, tenantId, clientSecret, jwksUrl, redirectUrl) {
                @Override
                public String exchangeCode(final String authorizationCode) {
                    var internalCredentialID = UUID.randomUUID().toString();
                    if (null != token) {
                        credentialStore.put(internalCredentialID, token);
                    }
                    return internalCredentialID;
                }
            };
        }
    }

    static MockingMicrosoftAuthenticator msAuth() {
        var secret = "some_secret";
        var jwksUrl = "http://localhost:8888/keys";
        var redirectUrl = "http://redirect.me";
        return new MockingMicrosoftAuthenticator(clientId, tenantId, secret, jwksUrl, redirectUrl);
    }

    static Number nextUserIdFromDb() throws Exception {
        var users = userAccountManager.findUsers(LongStream.range(0, 100).boxed().collect(Collectors.toList()));
        var largestIdUser = users.stream().max(Comparator.comparingLong(RegisteredUserDTO::getId));
        return largestIdUser.map(u -> u.getId() + 1).orElse(null);
    }

    static String clientId = "the_client_id";
    static String tenantId = "common";
    static KeyPair validSigningKey = new KeyPair();
    static Token token = new Token(clientId, validSigningKey);
    static String csrfToken = "the_csrf_token";
    static String validQuery = String.format("?state=%s&code=%s", csrfToken, 123);
    static String notUsingMicrosoftMessage = "You do not use Microsoft to log in. You may have registered using"
            + " a different provider, or your email address and password.";
    static String noUserMessage = "Unable to locate user information.";
}