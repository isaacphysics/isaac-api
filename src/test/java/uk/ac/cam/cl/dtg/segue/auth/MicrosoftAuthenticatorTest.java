/*
 * Copyright 2025 Barna Magyarkuti
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
package uk.ac.cam.cl.dtg.segue.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONArray;
import org.json.JSONObject;

import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.Test;

import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.String.format;

@RunWith(Enclosed.class)
public class MicrosoftAuthenticatorTest extends Helpers {
    @RunWith(Enclosed.class)
    public static class TestJwtParsing {
        public static class TestValidPayload {
            @Test
            public void getUserInfo_validToken_returnsUserInformation() throws Throwable {
                var token = validToken(t -> t, p -> {
                    p.put("sub", "some_account_id");
                    p.put("email", "test@example.org");
                    p.put("family_name", "Doe");
                    p.put("given_name", "John");
                    return null;
                });

                var userInfo = testGetUserInfo(token);

                assertEquals("some_account_id", userInfo.getProviderUserId());
                assertEquals("test@example.org", userInfo.getEmail());
                assertEquals(EmailVerificationStatus.NOT_VERIFIED, userInfo.getEmailVerificationStatus());
                assertEquals("Doe", userInfo.getFamilyName());
                assertEquals("John", userInfo.getGivenName());
                assertEquals(false, userInfo.getTeacherAccountPending());
            }
        }

        @RunWith(Enclosed.class)
        public static class TestInvalidPayload {
            public static class TestSubClaim extends TestInvalidPayloadField {
                String claim() {
                    return "sub";
                }
            }

            public static class TestGivenNameClaim extends TestInvalidPayloadField {
                String claim() {
                    return "given_name";
                }
            }

            public static class TestFamilyNameClaim extends TestInvalidPayloadField {
                String claim() {
                    return "family_name";
                }
            }

            public static class TestEmailClaim extends TestInvalidPayloadField {
                String claim() {
                    return "email";
                }

                @Test
                public void getUserInfo_emailInvalid_throwsError() {
                    var token = validToken(t -> t, p -> p.put("email", "some_bad_email"));
                    testGetUserInfo(token, NoUserException.class,
                            format("Required field 'email' missing from identity provider's response."));
                }
            }
        }
    }

    @RunWith(Enclosed.class)
    public static class TestTokenValidation {
        public static class TestTokenMissing {
            @Test
            public void getUserInfo_tryingToStoreNull_throwsError() {
                assertThrows(NullPointerException.class, () -> testGetUserInfo(null));
            }

            @Test
            public void getUserInfo_noSuchToken_throwsError() throws MalformedURLException {
                var subject = new MicrosoftAuthenticator(
                        clientId, "", "", "http://localhost:8888/keys") {
                    {
                        MicrosoftAuthenticator.credentialStore = getStore();
                    }
                };
                var error = assertThrows(AuthenticatorSecurityException.class,
                        () -> subject.getUserInfo("no_token_for_id"));
                assertEquals("Token verification: TOKEN_MISSING", error.getMessage());
            }
        }

        public static class TestSignatureVerification {
            @Test
            public void getUserInfo_tokenSignatureNoKeyId_throwsError() {
                var token = validToken(t -> t.withKeyId(null), p -> p);
                Helpers.testGetUserInfo(token, AuthenticatorSecurityException.class,
                        "No key found in http://localhost:8888/keys with kid null");
            }

            @Test
            public void getUserInfo_tokenSignatureKeyNotFound_throwsError() {
                var token = validToken(t -> t.withKeyId("no-such-key"), p -> p);
                testGetUserInfo(token, AuthenticatorSecurityException.class,
                        "No key found in http://localhost:8888/keys with kid no-such-key");
            }

            @Test
            public void getUserInfo_tokenSignatureMismatch_throwsError() {
                var token = signedToken(invalidSigningKey, t -> t.withKeyId(validSigningKey.id()));
                testGetUserInfo(token, SignatureVerificationException.class,
                        "The Token's Signature resulted invalid when verified using the Algorithm: SHA256withRSA");
            }
        }

        public static class TestExpirationClaim {
            @Test
            public void getUserInfo_tokenWithoutExp_accepted() {
                assertDoesNotThrow(() -> testGetUserInfo(validToken(t -> t.withExpiresAt((Date) null), p -> p)));
            }

            @Test
            public void getUserInfo_tokenExpired_throwsError() {
                var token = validToken(t -> t.withExpiresAt(oneHourAgo), p -> p);
                testGetUserInfo(token, TokenExpiredException.class,
                        format("The Token has expired on %s.", oneHourAgo));
            }
        }

        public static class TestIssuedAtClaim {
            @Test
            public void getUserInfo_tokenWithoutIat_accepted() {
                assertDoesNotThrow(() -> testGetUserInfo(validToken(t -> t.withIssuedAt((Date) null), p -> p)));
            }

            @Test
            public void getUserInfo_tokenIatFuture_throwsError() {
                var token = validToken(t -> t.withIssuedAt(inOneHour), p -> p);
                testGetUserInfo(token, IncorrectClaimException.class, format("The Token can't be used before %s.", inOneHour));
            }
        }

        public static class TestNotBeforeClaim {
            @Test
            public void getUserInfo_tokenWithoutNbf_accepted() {
                assertDoesNotThrow(() -> testGetUserInfo(validToken(t -> t.withNotBefore((Date) null), p -> p)));
            }

            @Test
            public void getUserInfo_tokenNbfFuture_throwsError() {
                var token = validToken(t -> t.withNotBefore(inOneHour), p -> p);
                testGetUserInfo(token, IncorrectClaimException.class, format("The Token can't be used before %s.", inOneHour));
            }
        }

        public static class TestAudienceClaim {
            @Test
            public void getUserInfo_tokenMissingAud_throwsError() {
                var token = validToken(t -> t.withAudience((String) null), p -> p);
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'aud' value doesn't contain the required audience.");
            }

            @Test
            public void getUserInfo_tokenAudIncorrect_throwsError() {
                var token = validToken(t -> t.withAudience("intended_for_somebody_else"), p -> p);
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'aud' value doesn't contain the required audience.");
            }
        }

        public static class TestIssuerClaim {
            @Test
            public void getUserInfo_tokenMissingIssuer_throwsError() {
                var token = validToken(t -> t.withIssuer(null), p -> p);
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'iss' value doesn't match the required issuer.");
            }

            @Test
            public void getUserInfo_tokenIncorrectIssuer_throwsError() {
                var token = validToken(t -> t.withIssuer("some_bad_issuer"), p -> p);
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'iss' value doesn't match the required issuer.");
            }
        }
    }
}

class Helpers {
    static abstract class TestInvalidPayloadField {
        abstract String claim();

        @Test
        public void getUserInfo_missing_throwsError() {
            var token = validToken(t -> t, p -> p.remove(claim()));
            testGetUserInfo(token, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_null_throwsError() {
            var token = validToken(t -> t, p -> p.put(claim(), null));
            testGetUserInfo(token, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_empty_throwsError() {
            var token = validToken(t -> t, p -> p.put(claim(), ""));
            testGetUserInfo(token, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_blank_throwsError() {
            var token = validToken(t -> t, p -> p.put(claim(), " "));
            testGetUserInfo(token, NoUserException.class, expectedMessage());
        }

        private String expectedMessage() {
            return format("Required field '%s' missing from identity provider's response.", claim());
        }
    }

    static UserFromAuthProvider testGetUserInfo(String token) throws Throwable {
        var store = getStore();
        var subject = new MicrosoftAuthenticator(
                clientId, tenantId, "", "http://localhost:8888/keys") {
            {
                MicrosoftAuthenticator.credentialStore = store;
            }
        };
        store.put("the_internal_id", token);
        var keySetServer = startKeySetServer(8888, Stream.of(validSigningKey, anotherValidSigningKey));
        try {
            return subject.getUserInfo("the_internal_id");
        } finally {
            keySetServer.stop();
        }
    }

    static <T extends Exception> void testGetUserInfo(String token, Class<T> errorClass, String errorMessage) {
        var error = assertThrows(errorClass, () -> testGetUserInfo(token));
        assertEquals(errorMessage, error.getMessage());
    }

    static Server startKeySetServer(int port, Stream<TestKeyPair> keys) throws Exception {
        var server = new Server(port);
        var handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(KeySetServlet.withKeys(keys), "/keys");
        server.start();
        return server;
    }

    interface JWTBuildFn extends Function<JWTCreator.Builder, JWTCreator.Builder> {
    }

    static class Payload extends HashMap<String, String> {
    }

    public interface PayloadModifyFn extends Function<Payload, Object> {
    }

    static String validToken(JWTBuildFn customiseToken, PayloadModifyFn customisePayload) {
        JWTBuildFn addDefaults = t -> t.withIssuedAt(oneHourAgo)
                .withNotBefore(oneHourAgo)
                .withAudience(clientId)
                .withIssuer(expectedIssuer)
                .withPayload(validPayload(customisePayload));

        return signedToken(validSigningKey, t -> customiseToken.apply(addDefaults.apply(t)));
    }

    static String signedToken(TestKeyPair key, JWTBuildFn fn) {
        var algorithm = Algorithm.RSA256(key.publicKey(), key.privateKey());
        var token = fn.apply(JWT.create().withKeyId(key.id()));
        return token.sign(algorithm);
    }

    static Cache<String, String> getStore() {
        return CacheBuilder.newBuilder().build();
    }

    static TestKeyPair validSigningKey = new TestKeyPair();
    static TestKeyPair anotherValidSigningKey = new TestKeyPair();
    static TestKeyPair invalidSigningKey = new TestKeyPair();
    static Instant oneHourAgo = Instant.now().minusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    static Instant inOneHour = Instant.now().plusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    static String clientId = "the_client_id";
    static String tenantId = "common";
    static String expectedIssuer = "https://login.microsoftonline.com/common/v2.0";

    static Payload validPayload(PayloadModifyFn customisePayload) {
        var validPayload = new Payload();
        validPayload.put("sub", "the_ms_account_id");
        validPayload.put("email", "test@example.com");
        validPayload.put("family_name", "Family");
        validPayload.put("given_name", "Given");
        customisePayload.apply(validPayload);
        return validPayload;
    }
}

class KeySetServlet extends HttpServlet {
    private final Stream<TestKeyPair> keys;

    public static ServletHolder withKeys(Stream<TestKeyPair> keys) {
        return new ServletHolder(new KeySetServlet(keys));
    }

    private KeySetServlet(Stream<TestKeyPair> keys) {
        this.keys = keys;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().println(keyStore());
    }

    private JSONObject keyStore() {
        return new JSONObject().put(
                "keys", keys.reduce(
                        new JSONArray(),
                        (acc, key) -> acc.put(
                                new JSONObject().put("kty", "RSA")
                                        .put("use", "sig")
                                        .put("kid", key.id())
                                        .put("n", key.modulus())
                                        .put("e", key.exponent())
                                        .put("cloud_instance_name", "microsoftonline.com")
                                // Microsoft's response also contains an X.509 certificate, which we don't test
                                // here. Sample at: https://login.microsoftonline.com/common/discovery/keys
                                // .put("x5t", key_id)
                                // .put("x5c", new JSONArray("some_string"))
                        ),
                        JSONArray::putAll));
    }
}

class TestKeyPair {
    private final KeyPair keyPair;

    public TestKeyPair() {
        try {
            keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String id() {
        return format("key_id_%s", keyPair.getPublic().hashCode());
    }

    public String modulus() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey().getModulus().toByteArray());
    }

    public String exponent() {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey().getPublicExponent().toByteArray());
    }

    public RSAPublicKey publicKey() {
        return (RSAPublicKey) keyPair.getPublic();
    }

    public RSAPrivateKey privateKey() {
        return (RSAPrivateKey) keyPair.getPrivate();
    }
}
