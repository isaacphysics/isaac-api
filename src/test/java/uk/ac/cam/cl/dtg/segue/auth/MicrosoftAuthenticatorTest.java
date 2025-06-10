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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import org.junit.Before;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.function.Executable;
import org.junit.runner.RunWith;
import org.junit.Test;

import org.junit.runners.Parameterized;
import uk.ac.cam.cl.dtg.isaac.IsaacTest.TestKeyPair;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

import java.net.MalformedURLException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.String.format;
import static uk.ac.cam.cl.dtg.isaac.IsaacTest.*;

@RunWith(Enclosed.class)
public class MicrosoftAuthenticatorTest {
    // Contains tests for:
    // 1) getAuthorizationUrl, 2) extractAuthCode, 3) getAntiForgeryStateToken, 4) getUserInfo,
    // 5) getAuthenticationProvider, 6) exchangeCode
    public static class TestShared extends IOAuth2AuthenticatorTest {
        @Before
        public final void setUp() throws Exception {
            this.oauth2Authenticator = Helpers.subject(Helpers.getStore());
            this.authenticator = this.oauth2Authenticator;
        }
    }

    public static class TestExtractAuthCode extends Helpers {
        /** test for happy path: {@link IOAuth2AuthenticatorTest#extractAuthCode_givenValidUrl_returnsCorrectCode} */
        @Test
        public void extractAuthCode_givenInvalidUrl_throwsError() {
            Executable act = () -> subject(getStore()).extractAuthCode("http://example.com");
            assertError(act, AuthenticationCodeException.class, "Error extracting authentication code.");
        }
    }

    @RunWith(Enclosed.class)
    public static class TestGetUserInfo extends GetUserInfoHelpers {
        @RunWith(Enclosed.class)
        public static class TestUserClaims {
            public static class TestValid {
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

            public static class TestSubClaim extends TestNonEmptyClaim {
                String claim() {
                    return "sub";
                }
            }

            @RunWith(Parameterized.class)
            public static class TestValidNameClaims {
                @Parameterized.Parameters(name="test case {index}")
                public static Collection<String[][]> data() {
                    return Arrays.asList(new String[][][] {
                            { {"John", "Doe", "JohnDoe" }, {"John", "Doe"} },
                            { {"John", "Doe", null }, {"John", "Doe"} },
                            { {"John", "Doe", "" }, {"John", "Doe"} },
                            { {"John", null, "John Doe" }, {"John", null} },
                            { {"John", "", "John Doe" }, {"John", null} },
                            { { null, "Doe", "John Doe" }, {null, "Doe"} },
                            { { "", "Doe", "John Doe" }, {null, "Doe"} },
                            { { null, null, "John Doe" }, {"John", "Doe"} },
                            { { "", "", "John Doe" }, {"John", "Doe"} },
                            { { null, null, "Doe" }, {null, "Doe"} },
                            { { null, null, "John" }, {null, "John"} },
                            { { null, null, " John " }, {null, "John"} },
                            { { null, null, "John " }, {null, "John"} },
                            { { null, null, "John  " }, {null, "John"} },
                            { { null, null, "John Joanne Doe" }, {"John Joanne", "Doe"} },
                            { { null, null, "John Joanne Josephine Doe" }, {"John Joanne Josephine", "Doe"} },
                            { { null, null, "John  Joanne   Josephine   Doe " }, {"John Joanne Josephine", "Doe"} },
                    });
                }

                @Parameterized.Parameter(0)
                public String[] input;

                @Parameterized.Parameter(1)
                public String[] expectedOutput;

                @Test
                public void getUserInfo_validNameClaims_accepted() throws Throwable {
                    var token = validToken(t -> t, p -> setName(p, input[0], input[1], input[2]));
                    var userInfo = testGetUserInfo(token);
                    assertEquals(expectedOutput[0], userInfo.getGivenName(), String.format("given name failed for %s", Arrays.toString(input)));
                    assertEquals(expectedOutput[1], userInfo.getFamilyName(), String.format("last name failed for %s", Arrays.toString(input)));
                }
            }

            public static class TestInvalidNameClaims {
                @Test
                public void getUserInfo_MissingNameClaims_rejected() {
                    var token = validToken(t -> t, p -> setName(p, null, null, null));
                    testGetUserInfo(token, NoUserException.class, "Could not determine name");
                }

                @Test
                public void getUserInfo_nullNameClaims_rejected() {
                    var token = validToken(t -> t, p -> {
                        p.put("given_name", null);
                        p.put("family_name", null);
                        p.put("name", null);
                        return null;
                    });
                    testGetUserInfo(token, NoUserException.class, "Could not determine name");
                }

                @Test
                public void getUserInfo_EmptyNameClaims_rejected() {
                    var token = validToken(t -> t, p -> setName(p, "", "", ""));
                    testGetUserInfo(token, NoUserException.class, "Could not determine name");
                }
            }

            public static class TestTidClaim extends TestNonEmptyClaim {
                String claim() {
                    return "tid";
                }

                @Test
                public void getUserInfo_tidInvalid_throwsError() {
                    var token = validToken(t -> t, p -> p.put("tid", "some_bad_tid"));
                    testGetUserInfo(token, NoUserException.class, "User verification: BAD_CLAIM (tid)");
                }

                @Test
                public void getUserInfo_tidPrefixValid_throwsError() {
                    var token = validToken(t -> t, p -> p.put("tid", format("%s/hello/", msTenantId)));
                    testGetUserInfo(token, NoUserException.class, "User verification: BAD_CLAIM (tid)");
                }
            }

            public static class TestEmailClaim extends TestNonEmptyClaim {
                String claim() {
                    return "email";
                }

                @Test
                public void getUserInfo_emailInvalid_throwsError() {
                    var token = validToken(t -> t, p -> p.put("email", "some_bad_email"));
                    testGetUserInfo(token, NoUserException.class, "User verification: BAD_CLAIM (email)");
                }
            }
        }

        @RunWith(Enclosed.class)
        public static class TestSystemClaims {
            public static class TestTokenMissing {
                @Test
                public void getUserInfo_tryingToStoreNull_throwsError() {
                    assertThrows(NullPointerException.class, () -> testGetUserInfo(null));
                }

                @Test
                public void getUserInfo_noSuchToken_throwsError() {
                    Executable act = () -> subject(getStore()).getUserInfo("no_token_for_id");
                    assertError(act, AuthenticatorSecurityException.class, "Token verification: TOKEN_MISSING");
                }
            }

            public static class TestSignatureVerification {
                @Test
                public void getUserInfo_tokenSignatureNoKeyId_throwsError() {
                    var token = validToken(t -> t.withKeyId(null), p -> p);
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: KEY_NOT_FOUND");
                }

                @Test
                public void getUserInfo_tokenSignatureKeyNotFound_throwsError() {
                    var token = validToken(t -> t.withKeyId("no-such-key"), p -> p);
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: KEY_NOT_FOUND");
                }

                @Test
                public void getUserInfo_tokenSignatureMismatch_throwsError() {
                    var token = signedToken(invalidSigningKey, t -> t.withKeyId(validSigningKey.id()));
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_SIGNATURE");
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
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: TOKEN_EXPIRED");
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
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iat)");
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
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (nbf)");
                }
            }

            public static class TestAudienceClaim {
                @Test
                public void getUserInfo_tokenMissingAud_throwsError() {
                    var token = validToken(t -> t.withAudience((String) null), p -> p);
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (aud)");
                }

                @Test
                public void getUserInfo_tokenAudIncorrect_throwsError() {
                    var token = validToken(t -> t.withAudience("intended_for_somebody_else"), p -> p);
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (aud)");
                }
            }

            public static class TestIssuerClaim {
                @Test
                public void getUserInfo_tokenMissingIssuer_throwsError() {
                    var token = validToken(t -> t.withIssuer(null), p -> p);
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                public void getUserInfo_tokenIncorrectIssuer_throwsError() {
                    var token = validToken(t -> t.withIssuer("some_bad_issuer"), p -> p);
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                public void getUserInfo_tokenTenantIssuerMismatch_throwsError() {
                    var tid = UUID.randomUUID().toString();
                    var token = validToken(t -> t.withIssuer(expectedIssuer(msTenantId)), p -> p.put("tid", tid));
                    testGetUserInfo(token, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                public void getUserInfo_tokenTenantSpecificIssuer_accepted() {
                    var tid = UUID.randomUUID().toString();
                    var token = validToken(t -> t.withIssuer(expectedIssuer(tid)), p -> p.put("tid", tid));
                    assertDoesNotThrow(() -> testGetUserInfo(token));
                }
            }
        }
    }
}

class Helpers {
    static MicrosoftAuthenticator subject(Cache<String, String> store ) throws MalformedURLException{
        return new MicrosoftAuthenticator(
                clientId, tenantId, "the_client_secret", "http://localhost:8888/keys", "http://example.com") {
            {
                credentialStore = store;
            }
        };
    }

    static Cache<String, String> getStore() {
        return CacheBuilder.newBuilder().build();
    }

    static <T extends Exception> void assertError(Executable action, Class<T> errorClass, String errorMessage) {
        var error = assertThrows(errorClass, action);
        assertEquals(errorMessage, error.getMessage());
    }
}

class GetUserInfoHelpers extends Helpers {
    static abstract class TestNonEmptyClaim {
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
            return format("User verification: BAD_CLAIM (%s)", claim());
        }
    }

    static UserFromAuthProvider testGetUserInfo(String token) throws Throwable {
        var store = getStore();
        var subject = subject(store);
        store.put("the_internal_id", token);
        var keySetServer = startKeySetServer(8888, List.of(validSigningKey, anotherValidSigningKey));
        try {
            return subject.getUserInfo("the_internal_id");
        } finally {
            keySetServer.getLeft().stop();
        }
    }

    static <T extends Exception> void testGetUserInfo(String token, Class<T> errorClass, String errorMessage) {
        assertError(() -> testGetUserInfo(token), errorClass, errorMessage);
    }

    static Payload setName(Payload p, String givenName, String familyName, String name) {
        setOrRemove(p, "given_name", givenName);
        setOrRemove(p, "family_name", familyName);
        setOrRemove(p, "name", name);
        return p;
    }

    private static void setOrRemove(Payload p, String key, String value) {
        if (value == null) {
            p.remove(key);
        } else {
            p.put(key, value);
        }
    }

    static TestKeyPair anotherValidSigningKey = new TestKeyPair();
    static TestKeyPair invalidSigningKey = new TestKeyPair();
}
