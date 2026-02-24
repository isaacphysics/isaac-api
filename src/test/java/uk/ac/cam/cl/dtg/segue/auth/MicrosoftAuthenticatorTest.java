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

import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.runner.RunWith;

import org.junit.runners.Parameterized;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.microsoft.KeyPair;
import uk.ac.cam.cl.dtg.segue.auth.microsoft.KeySetServlet;
import uk.ac.cam.cl.dtg.segue.auth.microsoft.Token;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static java.lang.String.format;

@RunWith(Enclosed.class)
public class MicrosoftAuthenticatorTest {
    @RunWith(Parameterized.class)
    public static class TestInitialisation extends Helpers {
        @Parameterized.Parameters(name="{0}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { "client_id", (C) v -> new MicrosoftAuthenticator(v, tenantId, clientSecret, jwksUrl, redirectUrl) },
                    { "tenant_id", (C) v -> new MicrosoftAuthenticator(clientId, v, clientSecret, jwksUrl, redirectUrl) },
                    { "client_secret", (C) v -> new MicrosoftAuthenticator(clientId, tenantId, v, jwksUrl, redirectUrl) },
                    { "jwks_url", (C) v -> new MicrosoftAuthenticator(clientId, tenantId, clientSecret, v, redirectUrl) },
                    { "redirect_url", (C) v -> new MicrosoftAuthenticator(clientId, tenantId, clientSecret, jwksUrl, v) },
            });
        }

        @Parameterized.Parameter()
        public String field;

        @Parameterized.Parameter(1)
        public C consumer;

        @Test
        public void microsoftAuthenticator_nullValue_throwsError() {
            Executable act = () -> consumer.consume(null);
            assertError(act, NullPointerException.class, format("Missing %s, can't be \"null\".", field));
        }

        @Test
        public void microsoftAuthenticator_emptyValue_throwsError() {
            Executable act = () -> consumer.consume("");
            assertError(act, IllegalArgumentException.class, format("Missing %s, can't be \"\".", field));
        }

        @FunctionalInterface
        public interface C  {
            void consume(String t) throws Exception;
        }
    }

    // Contains tests for:
    // 1) getAuthorizationUrl, 2) extractAuthCode, 3) getAntiForgeryStateToken, 4) getUserInfo,
    // 5) getAuthenticationProvider, 6) exchangeCode
    public static class TestShared extends IOAuth2AuthenticatorTest {
        @BeforeEach
        public final void setUp() throws Exception {
            this.oauth2Authenticator = Helpers.subject(Helpers.getStore());
            this.authenticator = this.oauth2Authenticator;
        }
    }

    public static class TestExtractAuthCode extends Helpers {
        /** happy path: {@link IOAuth2AuthenticatorTest#extractAuthCode_givenValidUrl_returnsCorrectCode} */
        @Test
        public void extractAuthCode_givenInvalidUrl_throwsError() {
            Executable act = () -> subject(getStore()).extractAuthCode("https://example.com");
            assertError(act, AuthenticationCodeException.class, "Error extracting authentication code.");
        }
    }

    public static class TestGetAuthorizationUrl extends Helpers {
        @Test
        public void getAuthorizationUrl_validAntiForgeryToken_returnsUrl() {
            var csrfToken = "the_csrf_token";
            var url = subject(getStore()).getAuthorizationUrl(csrfToken);
            var expectedUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize" +
                    format("?client_id=%s", clientId) +
                    format("&redirect_uri=%s", redirectUrl) +
                    format("&response_type=%s", "code") +
                    format("&scope=%s", "openid%20profile%20email") +
                    format("&state=%s", csrfToken) +
                    format("&prompt=%s", "select_account") +
                    format("&response_mode=%s", "query");
            assertEquals(expectedUrl, url);
        }
    }

    public static class TestExchangeCode extends Helpers {
        @Test
        public void exchangeCode_invalidCode_throwsCodeExchangeExceptionWithoutAnyDetails() {
            Executable act = () -> subject(getStore()).exchangeCode(null);
            assertError(act, CodeExchangeException.class, "There was an error exchanging the code.");
        }
    }

    @RunWith(Enclosed.class)
    public static class TestGetUserInfo extends GetUserInfoHelpers {
        @RunWith(Enclosed.class)
        public static class TestUserClaims {
            public static class TestValid {
                @Test
                public void getUserInfo_validToken_returnsUserInformation() throws Throwable {
                    var oid = UUID.randomUUID().toString();
                    var t = token.valid(s -> s, u -> {
                        u.put("oid", oid);
                        u.put("email", "test@example.org");
                        u.put("family_name", "Doe");
                        u.put("given_name", "John");
                        return null;
                    });

                    var userInfo = testGetUserInfo(t);

                    assertEquals(oid, userInfo.getProviderUserId());
                    assertEquals("test@example.org", userInfo.getEmail());
                    assertEquals(EmailVerificationStatus.NOT_VERIFIED, userInfo.getEmailVerificationStatus());
                    assertEquals("Doe", userInfo.getFamilyName());
                    assertEquals("John", userInfo.getGivenName());
                    assertEquals(false, userInfo.getTeacherAccountPending());
                }
            }

            public static class TestOidClaim extends TestUUIDClaim {
                @Override
                String claim() {
                    return "oid";
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

                @Parameterized.Parameter()
                public String[] input;

                @Parameterized.Parameter(1)
                public String[] expectedOutput;

                @Test
                public void getUserInfo_validNameClaims_accepted() throws Throwable {
                    var t = token.valid(s -> s, u -> u.setName(input[0], input[1], input[2]));
                    var userInfo = testGetUserInfo(t);
                    assertEquals(expectedOutput[0], userInfo.getGivenName(), String.format("given name failed for %s", Arrays.toString(input)));
                    assertEquals(expectedOutput[1], userInfo.getFamilyName(), String.format("last name failed for %s", Arrays.toString(input)));
                }
            }

            public static class TestInvalidNameClaims {
                @Test
                public void getUserInfo_MissingNameClaims_rejected() {
                    var t = token.valid(s -> s, u -> u.setName(null, null, null));
                    testGetUserInfo(t, NoUserException.class, "Could not determine name");
                }

                @Test
                public void getUserInfo_nullNameClaims_rejected() {
                    var t = token.valid(s -> s, u -> {
                        u.put("given_name", null);
                        u.put("family_name", null);
                        u.put("name", null);
                        return null;
                    });
                    testGetUserInfo(t, NoUserException.class, "Could not determine name");
                }

                @Test
                public void getUserInfo_EmptyNameClaims_rejected() {
                    var t = token.valid(s -> s, u -> u.setName("", "", ""));
                    testGetUserInfo(t, NoUserException.class, "Could not determine name");
                }
            }

            public static class TestTidClaim extends TestUUIDClaim {
                @Override
                String claim() {
                    return "tid";
                }
            }

            public static class TestEmailClaim extends TestNonEmptyClaim {
                @Override
                String claim() {
                    return "email";
                }

                @Test
                public void getUserInfo_emailInvalid_throwsError() {
                    var t = token.valid(s -> s, u -> u.put("email", "some_bad_email"));
                    testGetUserInfo(t, NoUserException.class, "User verification: BAD_CLAIM (email)");
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
                    var t = token.valid(s -> s.withKeyId(null), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: KEY_NOT_FOUND");
                }

                @Test
                public void getUserInfo_tokenSignatureKeyNotFound_throwsError() {
                    var t = token.valid(s -> s.withKeyId("no-such-key"), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: KEY_NOT_FOUND");
                }

                @Test
                public void getUserInfo_tokenSignatureMismatch_throwsError() {
                    var t = Token.signed(invalidSigningKey, s -> s.withKeyId(validSigningKey.getPublic().id()));
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_SIGNATURE");
                }
            }

            public static class TestExpirationClaim {
                @Test
                public void getUserInfo_tokenWithoutExp_accepted() {
                    assertDoesNotThrow(() -> testGetUserInfo(token.valid(s -> s.withExpiresAt((Date) null), u -> u)));
                }

                @Test
                public void getUserInfo_tokenExpired_throwsError() {
                    var t = token.valid(s -> s.withExpiresAt(Token.oneHourAgo), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: TOKEN_EXPIRED");
                }
            }

            public static class TestIssuedAtClaim {
                @Test
                public void getUserInfo_tokenWithoutIat_accepted() {
                    assertDoesNotThrow(() -> testGetUserInfo(token.valid(s -> s.withIssuedAt((Date) null), u -> u)));
                }

                @Test
                public void getUserInfo_tokenIatFuture_throwsError() {
                    var t = token.valid(s -> s.withIssuedAt(Token.inOneHour), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iat)");
                }
            }

            public static class TestNotBeforeClaim {
                @Test
                public void getUserInfo_tokenWithoutNbf_accepted() {
                    assertDoesNotThrow(() -> testGetUserInfo(token.valid(s -> s.withNotBefore((Date) null), u -> u)));
                }

                @Test
                public void getUserInfo_tokenNbfFuture_throwsError() {
                    var t = token.valid(s -> s.withNotBefore(Token.inOneHour), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (nbf)");
                }
            }

            public static class TestAudienceClaim {
                @Test
                public void getUserInfo_tokenMissingAud_throwsError() {
                    var t = token.valid(s -> s.withAudience((String) null), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (aud)");
                }

                @Test
                public void getUserInfo_tokenAudIncorrect_throwsError() {
                    var t = token.valid(s -> s.withAudience("intended_for_somebody_else"), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (aud)");
                }
            }

            public static class TestIssuerClaim {
                @Test
                public void getUserInfo_tokenMissingIssuer_throwsError() {
                    var t = token.valid(s -> s.withIssuer(null), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                public void getUserInfo_tokenIncorrectIssuer_throwsError() {
                    var t = token.valid(s -> s.withIssuer("some_bad_issuer"), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                public void getUserInfo_tokenTenantIssuerMismatch_throwsError() {
                    var tid = UUID.randomUUID().toString();
                    var t = token.valid(s -> s.withIssuer(Token.expectedIssuer(Token.msTenantId)),
                            u -> u.put("tid", tid));
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                public void getUserInfo_tokenTenantSpecificIssuer_accepted() {
                    var tid = UUID.randomUUID().toString();
                    var t = token.valid(s -> s.withIssuer(Token.expectedIssuer(tid)), u -> u.put("tid", tid));
                    assertDoesNotThrow(() -> testGetUserInfo(t));
                }
            }
        }
    }
}

class Helpers {
    static MicrosoftAuthenticator subject(Cache<String, String> store ) {
        return new MicrosoftAuthenticator(clientId, tenantId, clientSecret, jwksUrl, redirectUrl) {
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

    static String clientId = "the_client_id";
    static String clientSecret = "the_client_secret";
    static String tenantId = "common";
    static String jwksUrl = "http://localhost:8888/keys";
    static String redirectUrl = "http://the.redirect.url";
}

class GetUserInfoHelpers extends Helpers {
    static abstract class TestNonEmptyClaim {
        abstract String claim();

        @Test
        public void getUserInfo_missing_throwsError() {
            var t = token.valid(s -> s, u -> u.remove(claim()));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_null_throwsError() {
            var t = token.valid(s -> s, u -> u.put(claim(), null));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_empty_throwsError() {
            var t = token.valid(s -> s, u -> u.put(claim(), ""));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_blank_throwsError() {
            var t = token.valid(s -> s, u -> u.put(claim(), " "));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        String expectedMessage() {
            return format("User verification: BAD_CLAIM (%s)", claim());
        }
    }

    static abstract class TestUUIDClaim extends TestNonEmptyClaim {
        @Test
        public void getUserInfo_invalid_throwsError() {
            var t = token.valid(s -> s, u -> u.put(claim(), "some_bad_uuid"));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        public void getUserInfo_prefixValidButOverallInvalid_throwsError() {
            var t = token.valid(s -> s, u -> u.put(claim(), format("%s/hello/", UUID.randomUUID())));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }
    }

    static UserFromAuthProvider testGetUserInfo(String token) throws Throwable {
        var store = getStore();
        var subject = subject(store);
        store.put("the_internal_id", token);
        var keySetServer = KeySetServlet.startServer(8888, List.of(validSigningKey, anotherValidSigningKey));
        try {
            return subject.getUserInfo("the_internal_id");
        } finally {
            keySetServer.getLeft().stop();
        }
    }

    static <T extends Exception> void testGetUserInfo(String token, Class<T> errorClass, String errorMessage) {
        assertError(() -> testGetUserInfo(token), errorClass, errorMessage);
    }

    static KeyPair validSigningKey = new KeyPair();
    static KeyPair anotherValidSigningKey = new KeyPair();
    static KeyPair invalidSigningKey = new KeyPair();
    static Token token = new Token(clientId, validSigningKey);
}
