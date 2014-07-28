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
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

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
 * This class is derived from GoogleAuthenticator and provides 3rd party
 * authentication via the Facebook OAuth API.
 * 
 * @author nr378
 */
public class TwitterAuthenticator implements IOAuth1Authenticator {

	private static final Logger log = LoggerFactory
			.getLogger(TwitterAuthenticator.class);

	private final JsonFactory jsonFactory;
	private final HttpTransport httpTransport;
	private final Twitter twitter;

	private final String clientId;
	private final String clientSecret;
	private final String callbackUri;

	private final String AUTH_URL = "https://api.twitter.com/oauth/authenticate";
	private final String TOKEN_EXCHANGE_URL = "https://api.twitter.com/oauth/access_token";

	// weak cache for mapping userInformation to credentials
	private static WeakHashMap<String, Credential> credentialStore;
	private static GoogleIdTokenVerifier tokenVerifier;

	@Inject
	public TwitterAuthenticator(
			@Named(Constants.TWITTER_CLIENT_ID) final String clientId,
			@Named(Constants.TWITTER_SECRET) final String clientSecret,
			@Named(Constants.TWITTER_CALLBACK_URI) final String callbackUri) {
		this.jsonFactory = new JacksonFactory();
		this.httpTransport = new NetHttpTransport();
		this.twitter = new TwitterFactory().getInstance();
		this.twitter.setOAuthConsumer(clientId, clientSecret);
		this.twitter.setOAuthAccessToken(null); // ensure we start from a blank slate

		this.clientSecret = clientSecret;
		this.clientId = clientId;
		this.callbackUri = callbackUri;

		if (null == credentialStore) {
			credentialStore = new WeakHashMap<String, Credential>();
		}

		if (null == tokenVerifier) {
			tokenVerifier = new GoogleIdTokenVerifier(httpTransport,
					jsonFactory);
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
			requestToken = twitter.getOAuthRequestToken(callbackUri);
		} catch (TwitterException e) {
			throw new IOException(e.getMessage());
		}
		return new OAuth1Token(requestToken.getToken(), requestToken.getTokenSecret());
	}

	@Override
	public String getAuthorizationUrl(OAuth1Token token) {
		return new RequestToken(token.getToken(), token.getTokenSecret()).getAuthenticationURL();
	}

	@Override
	public String extractAuthCode(final String url) {
		GenericUrl urlParser = new GenericUrl(url);
		try {
			String oauthVerifier = getParameterFromUrl(urlParser,
					"oauth_verifier");
			log.info("User granted access to our app.");
			return oauthVerifier;
		} catch (IOException e) {
			log.info("User denied access to our app.");
			return null;
		}
	}

	@Override
	public String exchangeCode(final String authorizationCode)
			throws CodeExchangeException {
		try {
			AccessToken accessToken = twitter
					.getOAuthAccessToken(authorizationCode);

			TokenResponse tokenResponse = new TokenResponse();
			tokenResponse.setAccessToken(accessToken.getToken());
			tokenResponse.setRefreshToken(accessToken.getTokenSecret());
			tokenResponse.setExpiresInSeconds(Long.MAX_VALUE);

			Builder builder = new AuthorizationCodeFlow.Builder(
					BearerToken.authorizationHeaderAccessMethod(),
					httpTransport, jsonFactory, new GenericUrl(
							TOKEN_EXCHANGE_URL),
					new ClientParametersAuthentication(clientId, clientSecret),
					clientId, AUTH_URL);

			AuthorizationCodeFlow flow = builder.setDataStoreFactory(
					MemoryDataStoreFactory.getDefaultInstance()).build();

			Credential credential = flow.createAndStoreCredential(
					tokenResponse, authorizationCode);

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
	public synchronized User getUserInfo(final String internalProviderReference)
			throws NoUserIdException, IOException,
			AuthenticatorSecurityException {
		Credential credentials = credentialStore.get(internalProviderReference);
		twitter.setOAuthAccessToken(new AccessToken(credentials
				.getAccessToken(), credentials.getRefreshToken()));

		try {
			twitter4j.User userInfo = twitter.verifyCredentials();

			if (userInfo != null) {
				return new User(String.valueOf(userInfo.getId()),
						userInfo.getName(), null, null, null, null, null, null,
						null, null, null);
			} else {
				throw new NoUserIdException();
			}
		} catch (TwitterException e) {
			throw new IOException(e.getMessage());
		}
	}
	

	/**
	 * Helper method to retrieve the value of a parameter from a URL's query string.
	 * @param url - The URL to parse
	 * @param param - The parameter name
	 * @return The value of the parameter
	 * @throws IOException
	 */
	private String getParameterFromUrl(GenericUrl url, String param)
			throws IOException {
		try {
			@SuppressWarnings("unchecked")
			ArrayList<Object> arr = (ArrayList<Object>) url.get(param);
			return (String) arr.get(0);
		} catch (ClassCastException | NullPointerException e) {
			throw new IOException("Could not read paramater value.");
		}
	}
}
