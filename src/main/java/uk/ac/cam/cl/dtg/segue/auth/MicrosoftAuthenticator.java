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

import com.auth0.jwk.InvalidPublicKeyException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwk.SigningKeyNotFoundException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.MissingClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.openidconnect.IdTokenResponse;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

/**
 * Microsoft authenticator adapter for Segue.
 */
public class MicrosoftAuthenticator implements IOAuth2Authenticator {
    private final List<String> scopes = List.of("openid", "profile", "email");
    private final String clientId;
    private final String tenantId;
    private final String clientSecret;
    private final URL redirectUrl;

    private final JwkProvider jwkProvider;
    protected Cache<String, String> credentialStore;


    /**
     * Construct a Microsoft authenticator. Parameters injected by framework, defined in SOPS config.
     * Our app registration hosted at <a href="https://toolkit.uis.cam.ac.uk/endpoints">Toolkit</a>.
     *
     * @param clientId      the id for our app registration with Microsoft.
     * @param tenantId      the id for the tenant whose accounts we support. `common` for all tenants.
     * @param clientSecret  a secret generated on Toolkit. When exchanging an auth code for a token, we need to show
     *                      this secret to Microsoft to prove that we are the intended recipients.
     * @param jwksUrl       the URL for the key store that should be used to validate token signatures
     * @param redirectUrl   Microsoft will pass auth codes to our application using this URL
     * @throws NullPointerException     when some required piece of configuration is missing
     * @throws IllegalArgumentException when some required piece of configuration is invalid
     */
    @Inject
    public MicrosoftAuthenticator(
            @Named(Constants.MICROSOFT_CLIENT_ID) final String clientId,
            @Named(Constants.MICROSOFT_TENANT_ID) final String tenantId,
            @Named(Constants.MICROSOFT_SECRET) final String clientSecret,
            @Named(Constants.MICROSOFT_JWKS_URL) final String jwksUrl,
            @Named(Constants.MICROSOFT_REDIRECT_URL) final String redirectUrl
    )  {
        this.clientId = Validate.notBlank(clientId, "Missing client_id, can't be \"%s\".", clientId);
        this.tenantId = Validate.notBlank(tenantId, "Missing tenant_id, can't be \"%s\".", tenantId);
        this.clientSecret = Validate.notBlank(clientSecret, "Missing client_secret, can't be \"%s\".", clientSecret);
        this.redirectUrl = Validation.url(redirectUrl, "Missing redirect_url, can't be \"%s\".");
        var parsedJwksUrl = Validation.url(jwksUrl, "Missing jwks_url, can't be \"%s\".");
        this.jwkProvider = new JwkProviderBuilder(parsedJwksUrl).cached(10, 1, TimeUnit.HOURS).build();
        this.credentialStore = CacheBuilder.newBuilder().expireAfterAccess(10, TimeUnit.MINUTES).build();
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.MICROSOFT;
    }

    @Override
    public String getFriendlyName() {
        return "Microsoft";
    }

    @Override
    public String getAuthorizationUrl(final String antiForgeryStateToken) {
        return new AuthorizationCodeRequestUrl("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", this.clientId)
                .setScopes(scopes)
                .setRedirectUri(redirectUrl.toString())
                .setState(antiForgeryStateToken)
                .set("prompt", "select_account")
                .set("response_mode", "query")
                .build();
    }

    @Override
    public String getAntiForgeryStateToken() {
        int saltSizeBits = 130;
        int radixForSalt = 32;

        return "microsoft" + new BigInteger(saltSizeBits, new SecureRandom()).toString(radixForSalt);
    }

    @Override
    public String extractAuthCode(final String url) throws AuthenticationCodeException {
        try {
            return new AuthorizationCodeResponseUrl(url).getCode();
        } catch (final Exception e) {
            throw LogException.warn(e, new AuthenticationCodeException("Error extracting authentication code."));
        }
    }

    @Override
    public String exchangeCode(final String authorizationCode) throws CodeExchangeException {
        try {
            var request = new AuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    new GenericUrl(String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenantId)),
                    authorizationCode
            );
            IdTokenResponse response = (IdTokenResponse) request
                    .setResponseClass(IdTokenResponse.class)
                    .setRedirectUri(redirectUrl.toString())
                    .setClientAuthentication(new BasicAuthentication(clientId, clientSecret))
                    .execute();

            var internalCredentialID = UUID.randomUUID().toString();
            credentialStore.put(internalCredentialID, response.getIdToken());
            return internalCredentialID;
        } catch (final Exception e) {
            throw LogException.error(e, new CodeExchangeException("There was an error exchanging the code."));
        }
    }

    @Override
    public synchronized UserFromAuthProvider getUserInfo(final String internalProviderReference)
            throws AuthenticatorSecurityException, NoUserException {
        String tokenStr = credentialStore.getIfPresent(internalProviderReference);
        var token = parseAndVerifyToken(tokenStr);
        var name = Validation.name(
                token.getClaim("given_name").asString(),
                token.getClaim("family_name").asString(),
                token);

        return new UserFromAuthProvider(
                // use "oid" over "sub" because oid is the same across app registrations, and we might need to
                // move app registrations eg. for publisher verification
                // https://learn.microsoft.com/en-us/entra/identity-platform/id-token-claims-reference
                token.getClaim("oid").asString(),
                name.getLeft(),
                name.getRight(),
                token.getClaim("email").asString(),
                EmailVerificationStatus.NOT_VERIFIED,
                null, null, null, null,
                false
        );
    }

    private DecodedJWT parseAndVerifyToken(final String tokenStr)
            throws AuthenticatorSecurityException, NoUserException {
        // Validating id token based on requirements at
        // https://learn.microsoft.com/en-us/entra/identity-platform/id-tokens, as well as our own requirements
        if (null == tokenStr) {
            throw new AuthenticatorSecurityException("Token verification: TOKEN_MISSING");
        }

        var token = JWT.decode(tokenStr);
        try {
            var keyId = token.getKeyId();
            var jwk = jwkProvider.get(keyId);
            var algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey());
            JWT.require(algorithm)
                    .withAudience(clientId)
                    .withClaim("tid", Validation.uuid())
                    .withIssuer(String.format("https://login.microsoftonline.com/%s/v2.0", token.getClaim("tid").asString()))
                    .withClaim("oid", Validation.uuid())
                    .withClaim("email", Validation.email())
                    .build()
                    .verify(tokenStr);
            return token;
        } catch (final InvalidPublicKeyException e) {
            throw LogException.warn(e, new AuthenticatorSecurityException("Token verification: INVALID_PUBLIC_KEY"));
        } catch (final SigningKeyNotFoundException e) {
            throw LogException.warn(e, new AuthenticatorSecurityException("Token verification: KEY_NOT_FOUND"));
        } catch (final SignatureVerificationException e) {
            throw LogException.warn(e, new AuthenticatorSecurityException("Token verification: BAD_SIGNATURE"));
        } catch (final TokenExpiredException e) {
            throw LogException.warn(e, new AuthenticatorSecurityException("Token verification: TOKEN_EXPIRED"));
        } catch (MissingClaimException | IncorrectClaimException e) {
            String claimName = e instanceof MissingClaimException
                    ? ((MissingClaimException) e).getClaimName() : ((IncorrectClaimException) e).getClaimName();
            if (List.of("oid", "tid", "email").contains(claimName)) {
                throw LogException.warn(e, new NoUserException(
                        String.format("User verification: BAD_CLAIM (%s)", claimName)));
            } else {
                throw LogException.warn(e, new AuthenticatorSecurityException(
                        String.format("Token verification: BAD_CLAIM (%s)", claimName)));
            }
        } catch (final Exception e) {
            throw LogException.error(e, new AuthenticatorSecurityException("Token verification: UNEXPECTED_ERROR"));
        }
    }

    private static class Validation {
        public static URL url(final String urlString, final String message) {
            try {
                return new URL(urlString);
            } catch (final MalformedURLException e) {
                if (e.getCause() instanceof NullPointerException) {
                    throw new NullPointerException(String.format(message, urlString));
                }
                throw new IllegalArgumentException(String.format(message, urlString));
            }
        }

        public static BiPredicate<Claim, DecodedJWT> email() {
            return (c, j) -> UserAccountManager.isUserEmailValid(c.toString());
        }

        public static BiPredicate<Claim, DecodedJWT> uuid() {
            return (c, j) -> {
                try {
                    var uuid = UUID.fromString(c.asString());
                    return uuid.toString().equals(c.asString());
                } catch (final Exception e) {
                    return false;
                }
            };
        }

        public static Pair<String, String> name(final String givenName, final String familyName, final DecodedJWT token)
                throws NoUserException {
            if (UserAccountManager.isUserNameValid(givenName) && UserAccountManager.isUserNameValid((familyName))) {
                return Pair.of(givenName, familyName);
            }
            if (UserAccountManager.isUserNameValid((givenName))) {
                return Pair.of(givenName, null);
            }
            if (UserAccountManager.isUserNameValid((familyName))) {
                return Pair.of(null, familyName);
            }
            if (token != null) {
                try {
                    var name = token.getClaim("name").asString();
                    var names = StringUtils.split(name, " ");
                    var firstName = Arrays.copyOfRange(names, 0, names.length - 1);
                    return Validation.name(String.join(" ", firstName), names[names.length - 1], null);
                } catch (final Exception ignored) {
                    // fall-through to exception thrown at bottom
                }
            }
            throw LogException.warn(null, new NoUserException("Could not determine name"));
        }
    }
}


@SuppressWarnings("checkstyle:OneTopLevelClass")
class LogException {
    private static final Logger log = LoggerFactory.getLogger(MicrosoftAuthenticator.class);

    public static <T extends Exception, U extends Exception> U warn(final T sourceError, final U targetError) {
        log.warn(String.format(targetMessage(targetError), safeExtractMessage(sourceError)), sourceError);
        return targetError;
    }

    public static <T extends Exception, U extends Exception> U error(final T sourceError, final U targetError) {
        log.error(String.format(targetMessage(targetError), safeExtractMessage(sourceError)), sourceError);
        return targetError;
    }

    private static String safeExtractMessage(final Exception e) {
        return Optional.ofNullable(e).map(Exception::getMessage).orElse(null);
    }

    private static String targetMessage(final Exception e) {
        if (e instanceof AuthenticationCodeException) {
            return "Error extracting the authentication code: %s";
        } else if (e instanceof CodeExchangeException) {
            return "Error exchanging the authentication code: %s";
        } else if (e instanceof NoUserException || e instanceof AuthenticatorSecurityException) {
            return "Error validating the authentication token: %s";
        }
        return "Could not construct log message from error";
    }
}
