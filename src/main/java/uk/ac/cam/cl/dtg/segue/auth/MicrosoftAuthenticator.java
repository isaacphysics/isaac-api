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

import com.auth0.jwk.*;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.IncorrectClaimException;
import com.auth0.jwt.exceptions.MissingClaimException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.microsoft.aad.msal4j.AuthorizationCodeParameters;
import com.microsoft.aad.msal4j.AuthorizationRequestUrlParameters;
import com.microsoft.aad.msal4j.Prompt;
import com.microsoft.aad.msal4j.ResponseMode;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.ClientCredentialFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

public class MicrosoftAuthenticator implements IOAuth2Authenticator {
    private final String scopes = "email";
    private final String clientId;
    private final String tenantId;
    private final String clientSecret;
    private final String redirectUrl;

    private final JwkProvider jwkProvider;
    protected Cache<String, String> credentialStore;

    @Inject
    public MicrosoftAuthenticator(
            @Named(Constants.MICROSOFT_CLIENT_ID) final String clientId,
            @Named(Constants.MICROSOFT_TENANT_ID) final String tenantId,
            @Named(Constants.MICROSOFT_SECRET) final String clientSecret,
            @Named(Constants.MICROSOFT_JWKS_URL) final String jwksUrl,
            @Named(Constants.MICROSOFT_REDIRECT_URL) final String redirectUrL
    ) throws MalformedURLException {
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
        this.redirectUrl = redirectUrL;

        jwkProvider = new JwkProviderBuilder(new URL(jwksUrl)).cached(10, 1, TimeUnit.HOURS).build();
        int CREDENTIAL_CACHE_TTL_MINUTES = 10;
        credentialStore = CacheBuilder
                .newBuilder()
                .expireAfterAccess(CREDENTIAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                .build();
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
    public String getAuthorizationUrl(String antiForgeryStateToken) {
        var parameters = AuthorizationRequestUrlParameters
                .builder(redirectUrl, Collections.singleton(scopes))
                .responseMode(ResponseMode.QUERY)
                .prompt(Prompt.SELECT_ACCOUNT)
                .state(antiForgeryStateToken)
                .build();
        return microsoftClient().getAuthorizationRequestUrl(parameters).toString();
    }

    @Override
    public String getAntiForgeryStateToken() {
        int SALT_SIZE_BITS = 130;
        int RADIX_FOR_SALT = 32;

        return "microsoft" + new BigInteger(SALT_SIZE_BITS, new SecureRandom()).toString(RADIX_FOR_SALT);
    }

    @Override
    public String extractAuthCode(String url) throws AuthenticationCodeException{
        try {
            return new AuthorizationCodeResponseUrl(url).getCode();
        } catch (Exception e) {
            throw new AuthenticationCodeException("Error extracting authentication code.");
        }
    }

    @Override
    public String exchangeCode(String authorizationCode) throws CodeExchangeException {
        try {
            var authParams = AuthorizationCodeParameters
                    .builder(authorizationCode, new URI(redirectUrl)).scopes(Collections.singleton(scopes))
                    .build();
            var result = microsoftClient().acquireToken(authParams).get();

            var internalCredentialID = UUID.randomUUID().toString();
            credentialStore.put(internalCredentialID, result.idToken());
            return internalCredentialID;
        } catch (Exception e) {
            throw new CodeExchangeException(e.getMessage());
        }
    }

    @Override
    public UserFromAuthProvider getUserInfo(String internalProviderReference) throws AuthenticatorSecurityException, NoUserException {
        String tokenStr = credentialStore.getIfPresent(internalProviderReference);
        if (null == tokenStr) {
            throw new AuthenticatorSecurityException("Token verification: TOKEN_MISSING");
        }

        var token = parseAndVerifyToken(tokenStr);
        var name = getName(token.getClaim("given_name").asString(), token.getClaim("family_name").asString(), token);

        return new UserFromAuthProvider(
                token.getSubject(),
                name.getLeft(),
                name.getRight(),
                token.getClaim("email").asString(),
                EmailVerificationStatus.NOT_VERIFIED,
                null, null, null, null,
                false
        );
    }

    private ConfidentialClientApplication microsoftClient() {
        var secret = ClientCredentialFactory.createFromSecret(clientSecret);
        try {
            return ConfidentialClientApplication.builder(clientId, secret)
                    .authority(String.format("https://login.microsoftonline.com/%s", tenantId))
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private DecodedJWT parseAndVerifyToken(String tokenStr) throws AuthenticatorSecurityException, NoUserException{
        // Validating id token based on requirements at
        // https://learn.microsoft.com/en-us/entra/identity-platform/id-tokens, as well as our own requirements
        var token = JWT.decode(tokenStr);
        try {
            var keyId = token.getKeyId();
            var jwk = jwkProvider.get(keyId);
            var algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey());
            JWT.require(algorithm)
                    .withAudience(clientId)
                    .withClaim("tid", validUUID())
                    .withIssuer(String.format("https://login.microsoftonline.com/%s/v2.0", token.getClaim("tid").asString()))
                    .withClaim("sub", notEmpty())
                    .withClaim("email", validEmail())
                    .build()
                    .verify(tokenStr);
            return token;
        } catch (InvalidPublicKeyException e) {
            throw new AuthenticatorSecurityException("Token verification: INVALID_PUBLIC_KEY");
        } catch (SigningKeyNotFoundException e) {
            throw new AuthenticatorSecurityException("Token verification: KEY_NOT_FOUND");
        } catch (SignatureVerificationException e) {
            throw new AuthenticatorSecurityException("Token verification: BAD_SIGNATURE");
        } catch (TokenExpiredException e) {
            throw new AuthenticatorSecurityException("Token verification: TOKEN_EXPIRED");
        }
        catch (MissingClaimException | IncorrectClaimException e) {
            String claimName = e instanceof MissingClaimException ?
                    ((MissingClaimException) e).getClaimName() : ((IncorrectClaimException) e).getClaimName();
            if (List.of("sub", "tid", "email").contains(claimName)) {
                throw new NoUserException(String.format("User verification: BAD_CLAIM (%s)", claimName));
            } else {
                throw new AuthenticatorSecurityException(String.format("Token verification: BAD_CLAIM (%s)", claimName));
            }
        } catch (Exception e) {
            throw new AuthenticatorSecurityException("Token verification: UNEXPECTED_ERROR");
        }
    }

    private BiPredicate<Claim, DecodedJWT> notEmpty() {
        return (c, j) -> !c.isNull() && !c.asString().isBlank();
    }

    private BiPredicate<Claim, DecodedJWT> validEmail() {
        return (c, j) -> UserAccountManager.isUserEmailValid(c.toString());
    }

    private BiPredicate<Claim, DecodedJWT> validUUID() {
        return (c, j) -> {
            try {
                var uuid = UUID.fromString(c.asString());
                return uuid.toString().equals(c.asString());
            } catch (Exception e) {
                return false;
            }
        };
    }

    private Pair<String, String> getName(String givenName, String familyName, DecodedJWT token) throws NoUserException {
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
                return getName(String.join(" ", firstName), names[names.length - 1], null);
            } catch (Exception ignored) {}
        }

        throw new NoUserException("Could not determine name");
    }
}
