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
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

class MicrosoftAuthenticatorTest {

    @Nested
    class TestInitialisation extends Helpers {

        static Stream<Object[]> data() {
            return Stream.of(new Object[][] {
                    { "client_id", (C) v -> new MicrosoftAuthenticator(v, tenantId, clientSecret, jwksUrl, redirectUrl) },
                    { "tenant_id", (C) v -> new MicrosoftAuthenticator(clientId, v, clientSecret, jwksUrl, redirectUrl) },
                    { "client_secret", (C) v -> new MicrosoftAuthenticator(clientId, tenantId, v, jwksUrl, redirectUrl) },
                    { "jwks_url", (C) v -> new MicrosoftAuthenticator(clientId, tenantId, clientSecret, v, redirectUrl) },
                    { "redirect_url", (C) v -> new MicrosoftAuthenticator(clientId, tenantId, clientSecret, jwksUrl, v) },
            });
        }

        @ParameterizedTest
        @MethodSource("data")
        void microsoftAuthenticator_nullValue_throwsError(final String field, final C consumer) {
            Executable act = () -> consumer.consume(null);
            assertError(act, NullPointerException.class, format("Missing %s, can't be \"null\".", field));
        }

        @ParameterizedTest
        @MethodSource("data")
        void microsoftAuthenticator_emptyValue_throwsError(final String field, final C consumer) {
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
    @Nested
    class TestShared extends IOAuth2AuthenticatorTest {
        @BeforeEach
        final void setUp() throws Exception {
            this.oauth2Authenticator = Helpers.subject(Helpers.getStore());
            this.authenticator = this.oauth2Authenticator;
        }
    }

    @Nested
    class TestExtractAuthCode extends Helpers {
        /** happy path: {@link IOAuth2AuthenticatorTest#extractAuthCode_givenValidUrl_returnsCorrectCode}. */
        @Test
        void extractAuthCode_givenInvalidUrl_throwsError() {
            Executable act = () -> subject(getStore()).extractAuthCode("https://example.com");
            assertError(act, AuthenticationCodeException.class, "Error extracting authentication code.");
        }
    }

    @Nested
    class TestGetAuthorizationUrl extends Helpers {
        @Test
        void getAuthorizationUrl_validAntiForgeryToken_returnsUrl() {
            var csrfToken = "the_csrf_token";
            var url = subject(getStore()).getAuthorizationUrl(csrfToken);
            var expectedUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
                    + format("?client_id=%s", clientId)
                    + format("&redirect_uri=%s", redirectUrl)
                    + format("&response_type=%s", "code")
                    + format("&scope=%s", "openid%20profile%20email")
                    + format("&state=%s", csrfToken)
                    + format("&prompt=%s", "select_account")
                    + format("&response_mode=%s", "query");
            assertEquals(expectedUrl, url);
        }
    }

    @Nested
    class TestExchangeCode extends Helpers {
        @Test
        void exchangeCode_invalidCode_throwsCodeExchangeExceptionWithoutAnyDetails() {
            Executable act = () -> subject(getStore()).exchangeCode(null);
            assertError(act, CodeExchangeException.class, "There was an error exchanging the code.");
        }
    }

    @Nested
    class TestGetUserInfo extends GetUserInfoHelpers {
        @Nested
        class TestUserClaims {
            @Nested
            class TestValid {
                @Test
                void getUserInfo_validToken_returnsUserInformation() throws Throwable {
                    String oid = UUID.randomUUID().toString();
                    String t = token.valid(s -> s, u -> {
                        u.put("oid", oid);
                        u.put("email", "test@example.org");
                        u.put("family_name", "Doe");
                        u.put("given_name", "John");
                        return null;
                    });

                    UserFromAuthProvider userInfo = testGetUserInfo(t);

                    assertEquals(oid, userInfo.getProviderUserId());
                    assertEquals("test@example.org", userInfo.getEmail());
                    assertEquals(EmailVerificationStatus.NOT_VERIFIED, userInfo.getEmailVerificationStatus());
                    assertEquals("Doe", userInfo.getFamilyName());
                    assertEquals("John", userInfo.getGivenName());
                    assertEquals(false, userInfo.getTeacherAccountPending());
                }
            }

            @Nested
            class TestOidClaim extends TestUUIDClaim {
                @Override
                String claim() {
                    return "oid";
                }
            }

            @Nested
            class TestValidNameClaims {
                static Stream<Arguments> data() {
                    return Stream.of(
                            Arguments.of(new String[]{"John", "Doe", "JohnDoe"}, new String[]{"John", "Doe"}),
                            Arguments.of(new String[]{"John", "Doe", null}, new String[]{"John", "Doe"}),
                            Arguments.of(new String[]{"John", "Doe", ""}, new String[]{"John", "Doe"}),
                            Arguments.of(new String[]{"John", null, "John Doe"}, new String[]{"John", null}),
                            Arguments.of(new String[]{"John", "", "John Doe"}, new String[]{"John", null}),
                            Arguments.of(new String[]{null, "Doe", "John Doe"}, new String[]{null, "Doe"}),
                            Arguments.of(new String[]{"", "Doe", "John Doe"}, new String[]{null, "Doe"}),
                            Arguments.of(new String[]{null, null, "John Doe"}, new String[]{"John", "Doe"}),
                            Arguments.of(new String[]{"", "", "John Doe"}, new String[]{"John", "Doe"}),
                            Arguments.of(new String[]{null, null, "Doe"}, new String[]{null, "Doe"}),
                            Arguments.of(new String[]{null, null, "John"}, new String[]{null, "John"}),
                            Arguments.of(new String[]{null, null, " John "}, new String[]{null, "John"}),
                            Arguments.of(new String[]{null, null, "John "}, new String[]{null, "John"}),
                            Arguments.of(new String[]{null, null, "John  "}, new String[]{null, "John"}),
                            Arguments.of(new String[]{null, null, "John Joanne Doe"},
                                    new String[]{"John Joanne", "Doe"}),
                            Arguments.of(new String[]{null, null, "John Joanne Josephine Doe"},
                                    new String[]{"John Joanne Josephine", "Doe"}),
                            Arguments.of(new String[]{null, null, "John  Joanne   Josephine   Doe "},
                                    new String[]{"John Joanne Josephine", "Doe"})
                    );
                }

                @ParameterizedTest
                @MethodSource("data")
                void getUserInfo_validNameClaims_accepted(final String[] input, final String[] expectedOutput) throws Throwable {
                    String t = token.valid(s -> s, u -> u.setName(input[0], input[1], input[2]));
                    UserFromAuthProvider userInfo = testGetUserInfo(t);
                    assertEquals(expectedOutput[0], userInfo.getGivenName(),
                            String.format("given name failed for %s", Arrays.toString(input)));
                    assertEquals(expectedOutput[1], userInfo.getFamilyName(),
                            String.format("last name failed for %s", Arrays.toString(input)));
                }
            }

            @Nested
            class TestInvalidNameClaims {
                @Test
                void getUserInfo_MissingNameClaims_rejected() {
                    String t = token.valid(s -> s, u -> u.setName(null, null, null));
                    testGetUserInfo(t, NoUserException.class, "Could not determine name");
                }

                @Test
                void getUserInfo_nullNameClaims_rejected() {
                    String t = token.valid(s -> s, u -> {
                        u.put("given_name", null);
                        u.put("family_name", null);
                        u.put("name", null);
                        return null;
                    });
                    testGetUserInfo(t, NoUserException.class, "Could not determine name");
                }

                @Test
                void getUserInfo_EmptyNameClaims_rejected() {
                    String t = token.valid(s -> s, u -> u.setName("", "", ""));
                    testGetUserInfo(t, NoUserException.class, "Could not determine name");
                }
            }

            @Nested
            class TestTidClaim extends TestUUIDClaim {
                @Override
                String claim() {
                    return "tid";
                }
            }

            @Nested
            class TestEmailClaim extends TestNonEmptyClaim {
                @Override
                String claim() {
                    return "email";
                }

                @Test
                void getUserInfo_emailInvalid_throwsError() {
                    String t = token.valid(s -> s, u -> u.put("email", "some_bad_email"));
                    testGetUserInfo(t, NoUserException.class, "User verification: BAD_CLAIM (email)");
                }
            }
        }

        @Nested
        class TestSystemClaims {
            @Nested
            class TestTokenMissing {
                @Test
                void getUserInfo_tryingToStoreNull_throwsError() {
                    assertThrows(NullPointerException.class, () -> testGetUserInfo(null));
                }

                @Test
                void getUserInfo_noSuchToken_throwsError() {
                    Executable act = () -> subject(getStore()).getUserInfo("no_token_for_id");
                    assertError(act, AuthenticatorSecurityException.class, "Token verification: TOKEN_MISSING");
                }
            }

            @Nested
            class TestSignatureVerification {
                @Test
                void getUserInfo_tokenSignatureNoKeyId_throwsError() {
                    String t = token.valid(s -> s.withKeyId(null), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: KEY_NOT_FOUND");
                }

                @Test
                void getUserInfo_tokenSignatureKeyNotFound_throwsError() {
                    String t = token.valid(s -> s.withKeyId("no-such-key"), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: KEY_NOT_FOUND");
                }

                @Test
                void getUserInfo_tokenSignatureMismatch_throwsError() {
                    String t = Token.signed(invalidSigningKey, s -> s.withKeyId(validSigningKey.getPublic().id()));
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_SIGNATURE");
                }
            }

            @Nested
            class TestExpirationClaim {
                @Test
                void getUserInfo_tokenWithoutExp_accepted() {
                    assertDoesNotThrow(() -> testGetUserInfo(token.valid(s -> s.withExpiresAt((Date) null), u -> u)));
                }

                @Test
                void getUserInfo_tokenExpired_throwsError() {
                    String t = token.valid(s -> s.withExpiresAt(Token.oneHourAgo), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: TOKEN_EXPIRED");
                }
            }

            @Nested
            class TestIssuedAtClaim {
                @Test
                void getUserInfo_tokenWithoutIat_accepted() {
                    assertDoesNotThrow(() -> testGetUserInfo(token.valid(s -> s.withIssuedAt((Date) null), u -> u)));
                }

                @Test
                void getUserInfo_tokenIatFuture_throwsError() {
                    var t = token.valid(s -> s.withIssuedAt(Token.inOneHour), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iat)");
                }
            }

            @Nested
            class TestNotBeforeClaim {
                @Test
                void getUserInfo_tokenWithoutNbf_accepted() {
                    assertDoesNotThrow(() -> testGetUserInfo(token.valid(s -> s.withNotBefore((Date) null), u -> u)));
                }

                @Test
                void getUserInfo_tokenNbfFuture_throwsError() {
                    String t = token.valid(s -> s.withNotBefore(Token.inOneHour), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (nbf)");
                }
            }

            @Nested
            class TestAudienceClaim {
                @Test
                void getUserInfo_tokenMissingAud_throwsError() {
                    String t = token.valid(s -> s.withAudience((String) null), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (aud)");
                }

                @Test
                void getUserInfo_tokenAudIncorrect_throwsError() {
                    String t = token.valid(s -> s.withAudience("intended_for_somebody_else"), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (aud)");
                }
            }

            @Nested
            class TestIssuerClaim {
                @Test
                void getUserInfo_tokenMissingIssuer_throwsError() {
                    String t = token.valid(s -> s.withIssuer(null), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                void getUserInfo_tokenIncorrectIssuer_throwsError() {
                    String t = token.valid(s -> s.withIssuer("some_bad_issuer"), u -> u);
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                void getUserInfo_tokenTenantIssuerMismatch_throwsError() {
                    String tid = UUID.randomUUID().toString();
                    String t = token.valid(s -> s.withIssuer(Token.expectedIssuer(Token.msTenantId)),
                            u -> u.put("tid", tid));
                    testGetUserInfo(t, AuthenticatorSecurityException.class, "Token verification: BAD_CLAIM (iss)");
                }

                @Test
                void getUserInfo_tokenTenantSpecificIssuer_accepted() {
                    String tid = UUID.randomUUID().toString();
                    String t = token.valid(s -> s.withIssuer(Token.expectedIssuer(tid)), u -> u.put("tid", tid));
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

    static <T extends Exception> void assertError(final Executable action, final Class<T> errorClass, final String errorMessage) {
        Exception error = assertThrows(errorClass, action);
        assertEquals(errorMessage, error.getMessage());
    }

    static String clientId = "the_client_id";
    static String clientSecret = "the_client_secret";
    static String tenantId = "common";
    static String jwksUrl = "http://localhost:8888/keys";
    static String redirectUrl = "http://the.redirect.url";
}

class GetUserInfoHelpers extends Helpers {
    abstract static class TestNonEmptyClaim {
        abstract String claim();

        @Test
        void getUserInfo_missing_throwsError() {
            String t = token.valid(s -> s, u -> u.remove(claim()));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        void getUserInfo_null_throwsError() {
            String t = token.valid(s -> s, u -> u.put(claim(), null));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        void getUserInfo_empty_throwsError() {
            String t = token.valid(s -> s, u -> u.put(claim(), ""));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        void getUserInfo_blank_throwsError() {
            String t = token.valid(s -> s, u -> u.put(claim(), " "));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        String expectedMessage() {
            return format("User verification: BAD_CLAIM (%s)", claim());
        }
    }

    abstract static class TestUUIDClaim extends TestNonEmptyClaim {
        @Test
        void getUserInfo_invalid_throwsError() {
            String t = token.valid(s -> s, u -> u.put(claim(), "some_bad_uuid"));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }

        @Test
        void getUserInfo_prefixValidButOverallInvalid_throwsError() {
            String t = token.valid(s -> s, u -> u.put(claim(), format("%s/hello/", UUID.randomUUID())));
            testGetUserInfo(t, NoUserException.class, expectedMessage());
        }
    }

    static UserFromAuthProvider testGetUserInfo(final String token) throws Throwable {
        Cache<String, String> store = getStore();
        MicrosoftAuthenticator subject = subject(store);
        store.put("the_internal_id", token);
        Pair<Server, KeySetServlet> keySetServer =
                KeySetServlet.startServer(8888, List.of(validSigningKey, anotherValidSigningKey));
        try {
            return subject.getUserInfo("the_internal_id");
        } finally {
            keySetServer.getLeft().stop();
        }
    }

    static <T extends Exception> void testGetUserInfo(final String token, final Class<T> errorClass,
                                                      final String errorMessage) {
        assertError(() -> testGetUserInfo(token), errorClass, errorMessage);
    }

    static KeyPair validSigningKey = new KeyPair();
    static KeyPair anotherValidSigningKey = new KeyPair();
    static KeyPair invalidSigningKey = new KeyPair();
    static Token token = new Token(clientId, validSigningKey);
}
