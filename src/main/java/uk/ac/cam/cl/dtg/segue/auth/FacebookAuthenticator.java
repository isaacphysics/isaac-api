package uk.ac.cam.cl.dtg.segue.auth;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserIdException;
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
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class is derived from GoogleAuthenticator and provides 3rd party
 * authentication via the Facebook OAuth API.
 * 
 * @author nr378
 */
public class FacebookAuthenticator implements IOAuth2Authenticator {

	private static final Logger log = LoggerFactory
			.getLogger(FacebookAuthenticator.class);

	private final JsonFactory jsonFactory;
	private final HttpTransport httpTransport;

	private final String clientId;
	private final String clientSecret;
	private final String callbackUri;
	private final Collection<String> requestedScopes;

	private final String AUTH_URL = "https://graph.facebook.com/oauth/authorize";
	private final String TOKEN_EXCHANGE_URL = "https://graph.facebook.com/oauth/access_token";
	private final String USER_INFO_URL = "https://graph.facebook.com/me";
	private final String TOKEN_VERIFICATION_URL = "https://graph.facebook.com/debug_token";

	// weak cache for mapping userInformation to credentials
	private static WeakHashMap<String, Credential> credentialStore;
	private static GoogleIdTokenVerifier tokenVerifier;

	@Inject
	public FacebookAuthenticator(
			@Named(Constants.FACEBOOK_CLIENT_ID) final String clientId,
			@Named(Constants.FACEBOOK_SECRET) final String clientSecret,
			@Named(Constants.FACEBOOK_CALLBACK_URI) final String callbackUri,
			@Named(Constants.FACEBOOK_OAUTH_SCOPES) final String requestedScopes) {
		this.jsonFactory = new JacksonFactory();
		this.httpTransport = new NetHttpTransport();

		this.clientSecret = clientSecret;
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
	public String getAuthorizationUrl(final String antiForgeryStateToken) throws IOException {
		AuthorizationCodeRequestUrl urlBuilder = new AuthorizationCodeRequestUrl(
				AUTH_URL, clientId);

		urlBuilder.set(Constants.STATE_PARAM_NAME, antiForgeryStateToken);
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
			throws CodeExchangeException {
		try {
			AuthorizationCodeTokenRequest request = new AuthorizationCodeTokenRequest(
					httpTransport, jsonFactory, new GenericUrl(TOKEN_EXCHANGE_URL),
					authorizationCode);

			request.setClientAuthentication(new ClientParametersAuthentication(
					clientId, clientSecret));
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
					httpTransport, jsonFactory, new GenericUrl(TOKEN_EXCHANGE_URL),
					new ClientParametersAuthentication(clientId, clientSecret),
					clientId, AUTH_URL);
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
			GenericUrl url = new GenericUrl(USER_INFO_URL);
			url.set("access_token", credentials.getAccessToken());
			
			userInfo = JsonLoader.load(inputStreamToString(url.toURL().openStream()),
					FacebookUser.class, true);

			log.debug("Retrieved User info from Facebook");
		} catch (IOException e) {
			log.error("An IO error occurred while trying to retrieve user information: "
					+ e);
		}

		if (userInfo != null && userInfo.getId() != null) {
			return new User(userInfo.getId(), userInfo.getFirstName(),
					userInfo.getLastName(), userInfo.getEmail(), null, null,
					null, null, null, null, null, null);
		} else {
			throw new NoUserIdException();
		}
	}

	/**
	 * This method will contact the identity provider to verify that the token
	 * is valid for our application.
	 * 
	 * @param credentials
	 * @return true if the token passes our validation false if not.
	 */
	private boolean verifyAccessTokenIsValid(final Credential credentials) {
		Validate.notNull(credentials, "Credentials cannot be null");

		try {
			GenericUrl urlBuilder = new GenericUrl(TOKEN_VERIFICATION_URL);
			urlBuilder.set("access_token", clientId + "|" + clientSecret);
			urlBuilder.set("input_token", credentials.getAccessToken());
			
			FacebookTokenInfo info = JsonLoader.load(
					inputStreamToString(urlBuilder.toURL().openStream()),
					FacebookTokenInfo.class, true);
			return info.getData().getAppId().equals(clientId)
					&& info.getData().isValid();
		} catch (IOException e) {
			log.error("IO error while trying to validate oauth2 security token.");
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Helper method to merge a collection of strings into a single string.
	 * 
	 * @param delim
	 *            - The delimiter to be inserted between the merged strings
	 * @param strings
	 *            - A collection of strings to be merged
	 * @return the merged string
	 */
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

	/**
	 * Helper method to read an InputStream into a String.
	 * 
	 * @param is
	 *            - The InputStream to be read
	 * @return the contents of the InputStream
	 * @throws IOException
	 */
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
