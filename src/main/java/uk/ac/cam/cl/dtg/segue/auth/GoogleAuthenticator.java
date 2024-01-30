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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.api.services.oauth2.model.Userinfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Google authenticator adapter for Segue.
 * 
 * @author Stephen Cummins
 *
 */
public class GoogleAuthenticator implements IOAuth2Authenticator {
    private static final Logger log = LoggerFactory.getLogger(GoogleAuthenticator.class);

    private final JsonFactory jsonFactory;
    private final HttpTransport httpTransport;

    private final GoogleClientSecrets clientSecrets;

    private final String callbackUri;
    private final Collection<String> requestedScopes;

    // cache for mapping userInformation to credentials temporarily
    private static Cache<String, Credential> credentialStore;
    private static GoogleIdTokenVerifier tokenVerifier;

    private static final int SALT_SIZE_BITS = 130;
    private static final int RADIX_FOR_SALT = 32;
    
    private static final int CREDENTIAL_CACHE_TTL_MINUTES = 10; 

    /**
     * Construct a google authenticator.
     * 
     * @param clientSecretLocation
     *            - external file containing the secret provided by the google service.
     * @param callbackUri
     *            - The allowed URI for callbacks as registered with google.
     * @param requestedScopes
     *            - The scopes that will be granted to Segue.
     * @throws IOException
     *             - if we cannot load the secret file.
     */
    @Inject
    public GoogleAuthenticator(@Named(Constants.GOOGLE_CLIENT_SECRET_LOCATION) final String clientSecretLocation,
            @Named(Constants.GOOGLE_CALLBACK_URI) final String callbackUri,
            @Named(Constants.GOOGLE_OAUTH_SCOPES) final String requestedScopes) throws IOException {
        this.jsonFactory = new JacksonFactory();
        this.httpTransport = new NetHttpTransport();

        Validate.notBlank(clientSecretLocation, "Missing resource %s", clientSecretLocation);

        // load up the client secrets from the file system.
        try (InputStream inputStream = new FileInputStream(clientSecretLocation);
             InputStreamReader isr = new InputStreamReader(inputStream);
        ) {

            clientSecrets = GoogleClientSecrets.load(new JacksonFactory(), isr);

            this.requestedScopes = Arrays.asList(requestedScopes.split(";"));
            this.callbackUri = callbackUri;

            if (null == credentialStore) {
                credentialStore = CacheBuilder.newBuilder()
                        .expireAfterAccess(CREDENTIAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                        .build();
            }

            if (null == tokenVerifier) {
                tokenVerifier = new GoogleIdTokenVerifier(httpTransport, jsonFactory);
            }
        }
    }

    /**
     * Construct a google authenticator with all of its required dependencies.
     * 
     * @param clientSecret
     *            - external file containing the secret provided by the google service.
     * @param callbackUri
     *            - The allowed URI for callbacks as registered with google.
     * @param requestedScopes
     *            - The scopes that will be granted to Segue.
     */
    public GoogleAuthenticator(final GoogleClientSecrets clientSecret,
            @Named(Constants.GOOGLE_CALLBACK_URI) final String callbackUri,
            @Named(Constants.GOOGLE_OAUTH_SCOPES) final String requestedScopes) {
        this.jsonFactory = new JacksonFactory();
        this.httpTransport = new NetHttpTransport();

        clientSecrets = clientSecret;

        this.requestedScopes = Arrays.asList(requestedScopes.split(";"));
        this.callbackUri = callbackUri;

        if (null == credentialStore) {
            credentialStore = CacheBuilder.newBuilder()
                    .expireAfterAccess(CREDENTIAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
                    .<String, Credential> build();
        }

        if (null == tokenVerifier) {
            tokenVerifier = new GoogleIdTokenVerifier(httpTransport, jsonFactory);
        }
    }
    
    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.GOOGLE;
    }

    @Override
    public String getFriendlyName() {
        return "Google";
    }

    @Override
    public String getAuthorizationUrl(final String antiForgeryStateToken) {
        GoogleAuthorizationCodeRequestUrl urlBuilder;
        urlBuilder = new GoogleAuthorizationCodeRequestUrl(clientSecrets.getDetails().getClientId(), callbackUri,
                requestedScopes);
        // .setAccessType("online") // these can be used to force approval each
        // time the user logs in if we wish.
        // .setApprovalPrompt("force");

        urlBuilder.set(Constants.STATE_PARAM_NAME, antiForgeryStateToken);

        return urlBuilder.build();
    }

    @Override
    public String extractAuthCode(final String url) {
        AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(url);

        if (authResponse.getError() == null) {
            log.debug("User granted access to our app.");
        } else {
            log.debug("User denied access to our app.");
        }

        return authResponse.getCode();
    }

    @Override
    public synchronized String exchangeCode(final String authorizationCode) throws CodeExchangeException {
        try {
            GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(httpTransport, jsonFactory,
                    clientSecrets.getDetails().getClientId(), clientSecrets.getDetails().getClientSecret(),
                    authorizationCode, callbackUri).execute();

            // I don't really want to use the flow storage but it seems to be
            // easier to get credentials this way.
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory,
                    clientSecrets.getDetails().getClientId(), clientSecrets.getDetails().getClientSecret(),
                    requestedScopes).setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance()).build();

            Credential credential = flow.createAndStoreCredential(response, authorizationCode);
            String internalReferenceToken = UUID.randomUUID().toString();
            credentialStore.put(internalReferenceToken, credential);
            flow.getCredentialDataStore().clear();

            return internalReferenceToken;
        } catch (IOException e) {
            log.error("An error occurred during code exchange: " + e);
            throw new CodeExchangeException();
        }
    }

    @Override
    public synchronized UserFromAuthProvider getUserInfo(final String internalProviderReference)
            throws NoUserException, AuthenticatorSecurityException {
        Credential credentials = credentialStore.getIfPresent(internalProviderReference);        
        if (verifyAccessTokenIsValid(credentials)) {
            log.debug("Successful Verification of access token with provider.");
        } else {
            log.error("Unable to verify access token - it could be an indication of fraud.");
            throw new AuthenticatorSecurityException(
                    "Access token is invalid - the client id returned by the identity provider does not match ours.");
        }
        
        Oauth2 userInfoService = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credentials)
                .setApplicationName(Constants.APPLICATION_NAME).build();
        Userinfo userInfo = null;

        try {
            userInfo = userInfoService.userinfo().get().execute();
            log.debug("Retrieved User info from google: " + userInfo.toPrettyString());
        } catch (IOException e) {
            log.error("An IO error occurred while trying to retrieve user information: " + e);
        }
        if (userInfo != null && userInfo.getId() != null) {
            EmailVerificationStatus emailStatus = userInfo.isVerifiedEmail() ? EmailVerificationStatus.VERIFIED : EmailVerificationStatus.NOT_VERIFIED;
            String email = userInfo.getEmail();
            if (null == email) {
                email = userInfo.getId() + "-google";
                emailStatus = EmailVerificationStatus.DELIVERY_FAILED;
                log.warn("No email address provided by Google! Using (" + email + ") instead");
            }

            return new UserFromAuthProvider(userInfo.getId(), userInfo.getGivenName(), userInfo.getFamilyName(),
                    email, emailStatus, null, null, null, null, false);

        } else {
            throw new NoUserException("No user could be created from provider details!");
        }
    }

    @Override
    public String getAntiForgeryStateToken() {
        String antiForgerySalt = new BigInteger(SALT_SIZE_BITS, new SecureRandom()).toString(RADIX_FOR_SALT);

        String antiForgeryStateToken = "google" + antiForgerySalt;

        return antiForgeryStateToken;
    }

    /**
     * This method will contact the identity provider to verify that the token is valid for our application.
     * 
     * This check is intended to mitigate against the confused deputy problem; although I suspect the google client
     * might already do this.
     *
     * Todo from the future: this necessary because OAuth 2 is for authorization not authentication, and doesn't share
     *  information about the original authentication event by default. We should consider replacing this with OpenID,
     *  however the Google library doesn't appear to support that well.
     *
     * @param credentials
     *            - the credential object for the token verification.
     * @return true if the token passes our validation false if not.
     */
    private boolean verifyAccessTokenIsValid(final Credential credentials) {
        Validate.notNull(credentials, "Credentials cannot be null");

        Oauth2 oauth2 = new Oauth2.Builder(httpTransport, jsonFactory, credentials).setApplicationName(
                Constants.APPLICATION_NAME).build();
        try {
            Tokeninfo tokeninfo = oauth2.tokeninfo().setAccessToken(credentials.getAccessToken()).execute();

            if (tokeninfo.getAudience().equals(clientSecrets.getDetails().getClientId())) {
                return true;
            }
        } catch (IOException e) {
            log.error("IO error while trying to validate oauth2 security token.");
            e.printStackTrace();
        }
        return false;
    }
}
