package uk.ac.cam.cl.dtg.segue.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dao.JsonLoader;
import uk.ac.cam.cl.dtg.segue.dos.users.FacebookTokenInfo;
import uk.ac.cam.cl.dtg.segue.dos.users.FacebookUser;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow.Builder;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.AuthorizationCodeTokenRequest;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Tokeninfo;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class FacebookAuthenticator implements IFederatedAuthenticator,
		IOAuth2Authenticator {

	private static final Logger log = LoggerFactory
			.getLogger(FacebookAuthenticator.class);

	private final JsonFactory jsonFactory;
	private final HttpTransport httpTransport;

	private final String secret;
	private final String clientId;
	private final String callbackUri;
	private final Collection<String> requestedScopes;

	private final String AUTH_SERVER = "https://graph.facebook.com/oauth/authorize";
	private final String TOKEN_SERVER = "https://graph.facebook.com/oauth/access_token";

	// weak cache for mapping userInformation to credentials
	private static WeakHashMap<String, Credential> credentialStore;
	private static GoogleIdTokenVerifier tokenVerifier;

	@Inject
	public FacebookAuthenticator(
			@Named(Constants.FACEBOOK_SECRET) final String secret,
			@Named(Constants.FACEBOOK_CLIENT_ID) final String clientId,
			@Named(Constants.FACEBOOK_CALLBACK_URI) final String callbackUri,
			@Named(Constants.FACEBOOK_OAUTH_SCOPES) final String requestedScopes) {
		this.jsonFactory = new JacksonFactory();
		this.httpTransport = new NetHttpTransport();

		this.secret = secret;
		this.clientId = clientId;
		this.callbackUri = callbackUri;
		this.requestedScopes = Arrays.asList(requestedScopes.split(","));

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
		return AuthenticationProvider.FACEBOOK;
	}

	@Override
	public String getAuthorizationUrl() throws IOException {
		return getAuthorizationUrl(null);
	}

	@Override
	public String getAuthorizationUrl(final String emailAddress)
			throws IOException {
		AuthorizationCodeRequestUrl urlBuilder = new AuthorizationCodeRequestUrl(
				AUTH_SERVER, clientId);

		urlBuilder.set(Constants.STATE_PARAM_NAME, getAntiForgeryStateToken());
		urlBuilder.set("redirect_uri", callbackUri);
		urlBuilder.set("scope", mergeStrings(",", requestedScopes));

		return urlBuilder.build();
	}

	@Override
	public String extractAuthCode(final String url) throws IOException {
		// Copied verbatim from GoogleAuthenticator.extractAuthCode
		AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(
				url.toString());

		if (authResponse.getError() == null) {
			log.info("User granted access to our app.");
		} else {
			log.info("User denied access to our app.");
		}

		return authResponse.getCode();
	}

	@Override
	public String exchangeCode(final String authorizationCode)
			throws IOException, CodeExchangeException, NoUserIdException {
		try {
			AuthorizationCodeTokenRequest request = new AuthorizationCodeTokenRequest(
					httpTransport, jsonFactory, new GenericUrl(TOKEN_SERVER),
					authorizationCode);

			request.setClientAuthentication(new ClientParametersAuthentication(
					clientId, secret));
			request.setRedirectUri(callbackUri);

			// TokenResponse response = request.execute();
			// As of 17-07-14 Facebook API does not comply with Section 5.1 of
			// the OAuth2 spec and therefore executeUnparsed() must be used as
			// plain text is returned and not JSON

			HttpResponse response = request.executeUnparsed();
			String data = inputStreamToString(response.getContent());

			// Parse data, data will look something like:
			// access_token=VALUE&expires=VALUE
			String accessToken = null;
			Long expires = null;
			String[] pairs = data.split("&");
			for (String pair : pairs) {
				String[] kv = pair.split("=");
				if (kv.length != 2) {
					throw new RuntimeException("Unexpected auth response");
				} else {
					if (kv[0].equals("access_token")) {
						accessToken = kv[1];
					}
					if (kv[0].equals("expires")) {
						expires = Long.valueOf(kv[1]);
					}
				}
			}

			if (accessToken == null || expires == null) {
				throw new IOException(
						"access_token or expires values were not found");
			}

			TokenResponse tokenResponse = new TokenResponse();
			tokenResponse.setAccessToken(accessToken);
			tokenResponse.setExpiresInSeconds(expires);

			// I don't really want to use the flow storage but it seems to be
			// easier to get credentials this way.
			Builder builder = new AuthorizationCodeFlow.Builder(
					BearerToken.authorizationHeaderAccessMethod(),
					httpTransport, jsonFactory, new GenericUrl(TOKEN_SERVER),
					new ClientParametersAuthentication(clientId, secret),
					clientId, AUTH_SERVER);
			builder.setScopes(requestedScopes);

			AuthorizationCodeFlow flow = builder.setDataStoreFactory(
					MemoryDataStoreFactory.getDefaultInstance()).build();

			Credential credential = flow.createAndStoreCredential(
					tokenResponse, authorizationCode);
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
	public String getAntiForgeryStateToken() {
		String antiForgerySalt = new BigInteger(130, new SecureRandom())
				.toString(32);
		String antiForgeryStateToken = "facebook" + antiForgerySalt;
		return antiForgeryStateToken;
	}

	@Override
	public synchronized User getUserInfo(final String internalProviderReference)
			throws NoUserIdException, IOException,
			AuthenticatorSecurityException {
		Credential credentials = credentialStore.get(internalProviderReference);

		if (verifyAccessTokenIsValid(credentials)) {
			log.debug("Successful Verification of access token with provider.");
		} else {
			log.error("Unable to verify access token - it could be an indication of fraud.");
			throw new AuthenticatorSecurityException(
					"Access token is invalid - the client id returned by the identity provider does not match ours.");
		}

		FacebookUser userInfo = null;

		try {
			URL url = new URL("https://graph.facebook.com/me?access_token="
					+ credentials.getAccessToken());
			userInfo = JsonLoader.load(inputStreamToString(url.openStream()),
					FacebookUser.class, true);

			log.debug("Retrieved User info from Facebook");
		} catch (IOException e) {
			log.error("An IO error occurred while trying to retrieve user information: "
					+ e);
		}

		if (userInfo != null && userInfo.getId() != null) {
			return new User(userInfo.getId(), userInfo.getFirstName(),
					userInfo.getLastName(), userInfo.getEmail(), null, null,
					null, null, null, null, null);
		} else {
			throw new NoUserIdException();
		}
	}

	/**
	 * This method will contact the identity provider to verify that the token
	 * is valid for our application.
	 * 
	 * This check is intended to mitigate against the confused deputy problem;
	 * although I suspect the google client might already do this.
	 * 
	 * TODO: Verify that the google server client library doesn't already do
	 * this check internally - if it does then we can remove this additional
	 * check.
	 * 
	 * @param credentials
	 * @return true if the token passes our validation false if not.
	 */
	private boolean verifyAccessTokenIsValid(final Credential credentials) {
		Validate.notNull(credentials, "Credentials cannot be null");

		try {
			URL url = new URL(
					"https://graph.facebook.com/debug_token?access_token="
							+ clientId + "|" + secret + "&input_token="
							+ credentials.getAccessToken());
			FacebookTokenInfo info = JsonLoader.load(inputStreamToString(url.openStream()),
					FacebookTokenInfo.class, true);
			return info.getData().getAppId().equals(clientId) && info.getData().isValid();
		} catch (IOException e) {
			log.error("IO error while trying to validate oauth2 security token.");
			e.printStackTrace();
		}
		return false;
	}

	private String mergeStrings(final String delim,
			final Collection<String> strings) {
		if (strings == null) {
			return null;
		}

		int numStrings = strings.size();
		if (numStrings == 0) {
			return null;
		}

		StringBuffer sb = new StringBuffer();

		int i = 0;
		for (String s : strings) {
			i++;
			sb.append(s);
			if (i != numStrings) {
				sb.append(delim);
			}
		}

		return sb.toString();
	}

	private String inputStreamToString(final InputStream is) throws IOException {
		String line = "";
		StringBuilder total = new StringBuilder();

		// Wrap a BufferedReader around the InputStream
		BufferedReader rd = new BufferedReader(new InputStreamReader(is));

		// Read response until the end
		while ((line = rd.readLine()) != null) {
			total.append(line);
		}

		// Return full string
		return total.toString();
	}
}
