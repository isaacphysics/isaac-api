/**
 * Copyright 2014 Nick Rogers
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow.Builder;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class is derived from GoogleAuthenticator and provides 3rd party authentication via the Facebook OAuth API.
 * 
 * @author nr378
 */
public class TwitterAuthenticator implements IOAuth1Authenticator {

    private static final Logger log = LoggerFactory.getLogger(TwitterAuthenticator.class);

    private final JsonFactory jsonFactory;
    private final HttpTransport httpTransport;
    private final Twitter twitter;

    private final String clientId;
    private final String clientSecret;
    private final String callbackUri;

    private static final String AUTH_URL = "https://api.twitter.com/oauth/authenticate";
    private static final String TOKEN_EXCHANGE_URL = "https://api.twitter.com/oauth/access_token";

    // weak cache for mapping userInformation to credentials
    private static WeakHashMap<String, Credential> credentialStore;
    private static GoogleIdTokenVerifier tokenVerifier;

    /**
     * @param clientId 
     * @param clientSecret 
     * @param callbackUri 
     */
    @Inject
    public TwitterAuthenticator(@Named(Constants.TWITTER_CLIENT_ID) final String clientId,
            @Named(Constants.TWITTER_SECRET) final String clientSecret,
            @Named(Constants.TWITTER_CALLBACK_URI) final String callbackUri) {
        this.jsonFactory = new JacksonFactory();
        this.httpTransport = new NetHttpTransport();

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setIncludeEmailEnabled(true);

        this.twitter = new TwitterFactory(cb.build()).getInstance();
        this.twitter.setOAuthConsumer(clientId, clientSecret);
        this.clientSecret = clientSecret;
        this.clientId = clientId;
        this.callbackUri = callbackUri;

        if (null == credentialStore) {
            credentialStore = new WeakHashMap<String, Credential>();
        }

        if (null == tokenVerifier) {
            tokenVerifier = new GoogleIdTokenVerifier(httpTransport, jsonFactory);
        }
    }

    @Override
    public AuthenticationProvider getAuthenticationProvider() {
        return AuthenticationProvider.TWITTER;
    }

    @Override
    public OAuth1Token getRequestToken() throws IOException {
        RequestToken requestToken;
        try {
            twitter.setOAuthAccessToken(null); // ensure we start from a blank slate
            requestToken = twitter.getOAuthRequestToken(callbackUri);
        } catch (TwitterException e) {
            throw new IOException(e.getMessage());
        }
        return new OAuth1Token(requestToken.getToken(), requestToken.getTokenSecret());
    }

    @Override
    public String getAuthorizationUrl(final OAuth1Token token) {
        return new RequestToken(token.getToken(), token.getTokenSecret()).getAuthenticationURL();
    }

    @Override
    public String extractAuthCode(final String url) {
        GenericUrl urlParser = new GenericUrl(url);
        try {
            String oauthVerifier = getParameterFromUrl(urlParser, "oauth_verifier");
            log.debug("User granted access to our app.");
            return oauthVerifier;
        } catch (IOException e) {
            log.info("User denied access to our app.");
            return null;
        }
    }

    @Override
    public String exchangeCode(final String authorizationCode) throws CodeExchangeException {
        try {
            AccessToken accessToken = twitter.getOAuthAccessToken(authorizationCode);

            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setAccessToken(accessToken.getToken());
            tokenResponse.setRefreshToken(accessToken.getTokenSecret());
            tokenResponse.setExpiresInSeconds(Long.MAX_VALUE);

            Builder builder = new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
                    httpTransport, jsonFactory, new GenericUrl(TOKEN_EXCHANGE_URL),
                    new ClientParametersAuthentication(clientId, clientSecret), clientId, AUTH_URL);

            AuthorizationCodeFlow flow = builder.setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
                    .build();

            Credential credential = flow.createAndStoreCredential(tokenResponse, authorizationCode);

            String internalReferenceToken = UUID.randomUUID().toString();
            credentialStore.put(internalReferenceToken, credential);
            flow.getCredentialDataStore().clear();

            return internalReferenceToken;
        } catch (IOException | TwitterException | IllegalStateException e) {
            log.error("An error occurred during code exchange: " + e);
            throw new CodeExchangeException();
        }
    }

    @Override
    public synchronized UserFromAuthProvider getUserInfo(final String internalProviderReference)
            throws NoUserException, IOException, AuthenticatorSecurityException {
        Credential credentials = credentialStore.get(internalProviderReference);
        twitter.setOAuthAccessToken(new AccessToken(credentials.getAccessToken(), credentials.getRefreshToken()));

        try {
            twitter4j.User userInfo = twitter.verifyCredentials();

            if (userInfo != null) {
                // Using twitter id for email field is a hack to avoid a duplicate
                // exception due to null email field. Alistair and Steve dislike this...
                String givenName = null;
                String familyName = null;
                if (userInfo.getName() != null) {
                    String[] names = userInfo.getName().split(" ");
                    if (names.length > 0) {
                        givenName = names[0];
                    }
                    if (names.length > 1) {
                        familyName = names[1];
                    }
                }

                EmailVerificationStatus emailStatus = EmailVerificationStatus.NOT_VERIFIED;
                String email = userInfo.getEmail();
                if (null == email) {
                    email = userInfo.getId() + "-twitter";
                    emailStatus = EmailVerificationStatus.DELIVERY_FAILED;
                }

                return new UserFromAuthProvider(String.valueOf(userInfo.getId()), givenName, familyName, email,
                        emailStatus, null, null, null);
            } else {
                throw new NoUserException();
            }
        } catch (TwitterException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Helper method to retrieve the value of a parameter from a URL's query string.
     * 
     * @param url
     *            - The URL to parse
     * @param param
     *            - The parameter name
     * @return The value of the parameter
     * @throws IOException 
     */
    private String getParameterFromUrl(final GenericUrl url, final String param) throws IOException {
        try {
            @SuppressWarnings("unchecked")
            ArrayList<Object> arr = (ArrayList<Object>) url.get(param);
            return (String) arr.get(0);
        } catch (ClassCastException | NullPointerException e) {
            throw new IOException("Could not read parameter value.");
        }
    }
}
