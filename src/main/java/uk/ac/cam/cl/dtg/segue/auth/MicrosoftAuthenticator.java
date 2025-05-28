/*
 * Copyright 2014 Stephen Cummins
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

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.json.gson.GsonFactory;
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
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class MicrosoftAuthenticator implements IOAuth2Authenticator {
    private static final int CREDENTIAL_CACHE_TTL_MINUTES = 10;

    // TODO: why do we cache idTokens? Why don't we just pass them around in code?
    private static final Cache<String, String> credentialStore = CacheBuilder
        .newBuilder()
        .expireAfterAccess(CREDENTIAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
        .build();

    private final String scopes = "email";
    private final String callbackURL = "http://localhost:8004/auth/microsoft/callback";
    private final String clientId;
    private final String tenantId;
    private final String clientSecret;

    @Inject
    public MicrosoftAuthenticator(
        @Named(Constants.MICROSOFT_CLIENT_ID) final String clientId,
        @Named(Constants.MICROSOFT_TENANT_ID) final String tenantId,
        @Named(Constants.MICROSOFT_SECRET) final String clientSecret
    )  {
        this.clientId = clientId;
        this.tenantId = tenantId;
        this.clientSecret = clientSecret;
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
            .builder(callbackURL, Collections.singleton(scopes))
            .responseMode(ResponseMode.QUERY)
            .prompt(Prompt.SELECT_ACCOUNT)
            .state(antiForgeryStateToken)
            .build();
        return client().getAuthorizationRequestUrl(parameters).toString();
    }

    @Override
    public String getAntiForgeryStateToken() {
        int SALT_SIZE_BITS = 130;
        int RADIX_FOR_SALT = 32;

        return "microsoft" + new BigInteger(SALT_SIZE_BITS, new SecureRandom()).toString(RADIX_FOR_SALT);
    }

    @Override
    public String extractAuthCode(String url) {
        return new AuthorizationCodeResponseUrl(url).getCode();
    }

    @Override
    public String exchangeCode(String authorizationCode) throws CodeExchangeException{
        try {
            var authParams = AuthorizationCodeParameters
                .builder(authorizationCode, new URI(callbackURL)).scopes(Collections.singleton(scopes))
                .build();
            var result = client().acquireToken(authParams).get();

            String internalCredentialID = UUID.randomUUID().toString();
            credentialStore.put(internalCredentialID, result.idToken());
            return internalCredentialID;
        } catch (Exception e) {
            throw new CodeExchangeException(e.getMessage());
        }
    }

    @Override
    public UserFromAuthProvider getUserInfo(String internalProviderReference) {
        String idTokenStr = Objects.requireNonNull(credentialStore.getIfPresent(internalProviderReference));
        try {
            var idToken = IdToken.parse(new GsonFactory(), idTokenStr);
            verifyIdToken(idToken);

            // TODO: to support sign-ups, parse more info
            return new UserFromAuthProvider(
                (String) idToken.getPayload().get("sub"), null, null, (String) idToken.getPayload().get("email"),
                null, null, null, null, null, null
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ConfidentialClientApplication client() {
        var secret = ClientCredentialFactory.createFromSecret(clientSecret);
        try {
            return ConfidentialClientApplication.builder(clientId, secret)
                .authority(String.format("https://login.microsoftonline.com/%s", tenantId))
                .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyIdToken(IdToken token) throws AuthenticatorSecurityException {
        if (null == token) {
            throw new AuthenticatorSecurityException("No ID token was found.");
        }

        // TODO: validate token signature against server's public key, as well as issuer, audience and expiration
        // since the token came directly from the server via HTTPS, we're pretty safe even without verification
        // (although, it's actually coming from our own cache by the time it gets here)
    }
}
