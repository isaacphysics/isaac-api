/*
 * Copyright 2023 Matthew Trew
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.auth;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RaspberryPiAuthenticator implements IOAuth2Authenticator {
    private static final Logger log = LoggerFactory.getLogger(RaspberryPiAuthenticator.class);

    // The Google OAuth2 client uses its own HTTP layer for reasons
    private final HttpTransport httpTransport;
    private final JsonFactory jsonFactory;

    private final String clientId;
    private final String clientSecret;
    private final String callbackUri;
    private final List<String> requestedScopes;
    private final String authorizationUri;
    private final String tokenUri;

    // Raspberry Pi login options
    public static final String LOGIN_OPTIONS_PARAM_NAME = "login_options";
    public static final String LOGIN_OPTION_FORCE_SIGNUP = "force_signup";

    // Parameters for anti-CSRF state token
    private static final int SALT_SIZE_BITS = 130;
    private static final int SALT_RADIX = 32;


    @Inject
    public RaspberryPiAuthenticator(
            @Named(Constants.RASPBERRYPI_CLIENT_ID) final String clientId,
            @Named(Constants.RASPBERRYPI_CLIENT_SECRET) final String clientSecret,
            @Named(Constants.RASPBERRYPI_CALLBACK_URI) final String callbackUri,
            @Named(Constants.RASPBERRYPI_OAUTH_SCOPES) final String oauthScopes,
            @Named(Constants.RASPBERRYPI_AUTHORIZATION_URI) final String authorizationUri,
            @Named(Constants.RASPBERRYPI_TOKEN_URI) final String tokenUri
            ) {

        Validate.notBlank(clientId, "Missing resource %s", clientId);
        Validate.notBlank(clientSecret, "Missing resource %s", clientId);

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.callbackUri = callbackUri;
        this.requestedScopes = Arrays.asList(oauthScopes.split(";"));
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;

        this.httpTransport = new NetHttpTransport();
        this.jsonFactory = new JacksonFactory();
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.RASPBERRYPI;
    }

    @Override
    public synchronized UserFromAuthProvider getUserInfo(String internalProviderReference) throws NoUserException, IOException, AuthenticatorSecurityException {
        return null;
    }

    @Override
    public String getAuthorizationUrl(String antiForgeryStateToken) {
        return new AuthorizationCodeRequestUrl(this.authorizationUri, this.clientId)
                .setScopes(requestedScopes)
                .setRedirectUri(callbackUri)
                .setState(antiForgeryStateToken)
                .build();
    }

    @Override
    public String getAntiForgeryStateToken() {
        String antiForgerySalt = new BigInteger(SALT_SIZE_BITS, new SecureRandom()).toString(SALT_RADIX);
        return "raspberrypi" + antiForgerySalt;
    }

    @Override
    public String extractAuthCode(String url) {
        AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(url);

        if (authResponse.getError() == null) {
            log.debug("User granted access to our app.");
        } else {
            log.debug("User denied access to our app.");
        }

        return authResponse.getCode();
    }

    @Override
    public String exchangeCode(String authorizationCode) throws CodeExchangeException {
        try {
            // Exchange the authorization code for the access token from the authorization server's token endpoint
            TokenResponse response = new AuthorizationCodeTokenRequest(
                    httpTransport,
                    jsonFactory,
                    new GenericUrl(tokenUri),
                    authorizationCode
            )
                    .setRedirectUri(callbackUri)
                    .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret)
            ).execute();

            // Get the Credential store for this provider, and store the token there in a Credential
            // (Credentials do a bit more than just hold tokens - see:
            // https://cloud.google.com/java/docs/reference/google-oauth-client/latest/com.google.api.client.auth.oauth2.Credential)
            //
            // todo: We're using an in-memory store, so these will be lost on every deployment. Revisit.
            DataStore<StoredCredential> store =
                    MemoryDataStoreFactory.getDefaultInstance().getDataStore(AuthenticationProvider.RASPBERRYPI.toString());

            // generate a unique identifier we can use to retrieve this Credential later
            String internalCredentialID = UUID.randomUUID().toString();

            // save the Credential to the store
            store.set(internalCredentialID, new StoredCredential(createCredential(response)));

            return internalCredentialID;

        } catch (IOException e) {
            log.error("An error occurred during code exchange: " + e);
            throw new CodeExchangeException();
        }
    }

    // todo: Revisit to see what's required re. managing token lifecycle, refresh tokens etc.
    private Credential createCredential(TokenResponse response) {
        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
                .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setTokenServerEncodedUrl(tokenUri)
                .build()
                .setFromTokenResponse(response);
    }
}
