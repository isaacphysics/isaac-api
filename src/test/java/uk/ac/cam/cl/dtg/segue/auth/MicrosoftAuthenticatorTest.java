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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.String.format;

class MicrosoftAuthenticatorTest extends Helpers {
    @Test
    void getUserInfo_validToken_returnsUserInformation() throws Throwable {
        var token = validToken(t -> t.withPayload("{\"email\": \"test@example.com\"}"));
        var userInfo = testGetUserInfo(token);
        assertEquals("test2@example.com", userInfo.getEmail());
    }

    @Nested
    class TokenValidation {
        @Test
        void getUserInfo_tryingToStoreNull_throwsError() {
            assertThrows(NullPointerException.class, () -> testGetUserInfo(null));
        }

        @Test
        void getUserInfo_noSuchToken_throwsError() throws MalformedURLException {
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

        @Nested
        class SignatureVerification {
            @Test
            void getUserInfo_tokenSignatureNoKeyId_throwsError() {
                var token = validToken(t -> t.withKeyId(null));
                testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: NO_KEY_ID");
            }

            @Test
            void getUserInfo_tokenSignatureKeyNotFound_throwsError() {
                var token = validToken(t -> t.withKeyId("no-such-key"));
                testGetUserInfo(token, AuthenticatorSecurityException.class,
                        "No key found in http://localhost:8888/keys with kid no-such-key");
            }

            @Test
            void getUserInfo_tokenSignatureMismatch_throwsError() {
                var token = signedToken(invalidSigningKey, t -> t.withKeyId(validSigningKey.id()));
                testGetUserInfo(token, SignatureVerificationException.class,
                        "The Token's Signature resulted invalid when verified using the Algorithm: SHA256withRSA");
            }
        }

        @Nested
        class ExpirationClaim {
            @Test
            void getUserInfo_tokenWithoutExp_accepted() {
                assertDoesNotThrow(() -> testGetUserInfo(validToken(t -> t.withExpiresAt((Date) null))));
            }

            @Test
            void getUserInfo_tokenExpired_throwsError() {
                var token = validToken(t -> t.withExpiresAt(oneHourAgo));
                testGetUserInfo(token, TokenExpiredException.class,
                        format("The Token has expired on %s.", oneHourAgo));
            }
        }

        @Nested
        class IssuedAtClaim {
            @Test
            void getUserInfo_tokenWithoutIat_accepted() {
                assertDoesNotThrow(() -> testGetUserInfo(validToken(t -> t.withIssuedAt((Date) null))));
            }

            @Test
            void getUserInfo_tokenIatFuture_throwsError() {
                var token = validToken(t -> t.withIssuedAt(inOneHour));
                testGetUserInfo(token, IncorrectClaimException.class,
                        format("The Token can't be used before %s.", inOneHour));
            }
        }

        @Nested
        class NotBeforeClaim {
            @Test
            void getUserInfo_tokenWithoutNbf_accepted() {
                assertDoesNotThrow(() -> testGetUserInfo(validToken(t -> t.withNotBefore((Date) null))));
            }

            @Test
            void getUserInfo_tokenNbfFuture_throwsError() {
                var token = validToken(t -> t.withNotBefore(inOneHour));
                testGetUserInfo(token, IncorrectClaimException.class,
                        format("The Token can't be used before %s.", inOneHour));
            }
        }

        @Nested
        class AudienceClaim {
            @Test
            void getUserInfo_tokenMissingAud_throwsError() {
                var token = validToken(t -> t.withAudience((String) null));
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'aud' value doesn't contain the required audience.");
            }

            @Test
            void getUserInfo_tokenAudIncorrect_throwsError() {
                var token = validToken(t -> t.withAudience("intended_for_somebody_else"));
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'aud' value doesn't contain the required audience.");
            }
        }

        @Nested
        class IssuerClaim {
            @Test
            void getUserInfo_tokenMissingIssuer_throwsError() {
                var token = validToken(t -> t.withIssuer(null));
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'iss' value doesn't match the required issuer.");
            }

            @Test
            void getUserInfo_tokenIncorrectIssuer_throwsError() {
                var token = validToken(t -> t.withIssuer("some_bad_issuer"));
                testGetUserInfo(token, IncorrectClaimException.class,
                        "The Claim 'iss' value doesn't match the required issuer.");
            }
        }
    }
}

class Helpers {
    static UserFromAuthProvider testGetUserInfo(String token) throws Throwable {
        var store = getStore();
        var subject = new MicrosoftAuthenticator(
                clientId, tenantId, "", "http://localhost:8888/keys") {
            {
                MicrosoftAuthenticator.credentialStore = store;
            }
        };
        store.put("the_internal_id", token);
        return withKeySetServer(() -> subject.getUserInfo("the_internal_id"));
    }

    static <T extends Exception> void testGetUserInfo(String token, Class<T> errorClass, String errorMessage) {
        var error = assertThrows(errorClass, () -> testGetUserInfo(token));
        assertEquals(errorMessage, error.getMessage());
    }

    static <T> T withKeySetServer(ThrowingSupplier<T> supplier) throws Throwable {
        var keySetServer = startKeySetServer(8888, validSigningKey);
        try {
            return supplier.get();
        } finally {
            keySetServer.stop();
        }
    }

    static Server startKeySetServer(int port, TestKeyPair key) throws Exception {
        var server = new Server(port);
        var handler = new ServletHandler();
        server.setHandler(handler);
        handler.addServletWithMapping(KeySetServlet.withKey(key), "/keys");
        server.start();
        return server;
    }

    interface JWTBuildFn extends Function<JWTCreator.Builder, JWTCreator.Builder> {
    }

    ;

    static String validToken(JWTBuildFn fn) {
        return signedToken(validSigningKey, t -> {
            return fn.apply(t.withIssuedAt(oneHourAgo)
                    .withNotBefore(oneHourAgo)
                    .withAudience(clientId)
                    .withIssuer(expectedIssuer));
        });
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
    static TestKeyPair invalidSigningKey = new TestKeyPair();
    static Instant oneHourAgo = Instant.now().minusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    static Instant inOneHour = Instant.now().plusSeconds(60 * 60).truncatedTo(ChronoUnit.SECONDS);
    static String clientId = "the_client_id";
    static String tenantId = "common";
    static String expectedIssuer = "https://login.microsoftonline.com/common/v2.0";
}

class KeySetServlet extends HttpServlet {
    private TestKeyPair key;

    public static ServletHolder withKey(TestKeyPair key) {
        return new ServletHolder(new KeySetServlet(key));
    }

    private KeySetServlet(TestKeyPair key) {
        this.key = key;
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
                "keys", new JSONArray().put(
                        new JSONObject()
                                .put("kty", "RSA")
                                .put("use", "sig")
                                .put("kid", key.id())
                                .put("n", key.modulus())
                                .put("e", key.exponent())
                                .put("cloud_instance_name", "microsoftonline.com")
                        // Microsoft's response also contains an X.509 certificate, which we don't test
                        // here. Sample at: https://login.microsoftonline.com/common/discovery/keys
                        // .put("x5t", key_id)
                        // .put("x5c", new JSONArray("some_string"))
                ));
    }
}

class TestKeyPair {
    private KeyPair keyPair;

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
