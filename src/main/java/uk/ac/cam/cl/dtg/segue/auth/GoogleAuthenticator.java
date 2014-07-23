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
import java.util.WeakHashMap;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.dos.users.User;

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
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.inject.Inject;
import com.google.inject.name.Named;

public class GoogleAuthenticator implements IOAuth2Authenticator {

	private static final Logger log = LoggerFactory
			.getLogger(GoogleAuthenticator.class);

	private final JsonFactory jsonFactory;
	private final HttpTransport httpTransport;

	private final GoogleClientSecrets clientSecrets;

	private final String callbackUri;
	private final Collection<String> requestedScopes;

	// weak cache for mapping userInformation to credentials
	private static WeakHashMap<String, Credential> credentialStore;
	private static GoogleIdTokenVerifier tokenVerifier;

	@Inject
	public GoogleAuthenticator(
			GoogleClientSecrets clientSecrets,
			@Named(Constants.GOOGLE_CALLBACK_URI) String callbackUri,
			@Named(Constants.GOOGLE_OAUTH_SCOPES) String requestedScopes) {
		this.jsonFactory = new JacksonFactory();
		this.httpTransport = new NetHttpTransport();

		this.clientSecrets = clientSecrets;
		this.requestedScopes = Arrays.asList(requestedScopes.split(";"));
		this.callbackUri = callbackUri;

		if (null == credentialStore)
			credentialStore = new WeakHashMap<String, Credential>();

		if (null == tokenVerifier)
			tokenVerifier = new GoogleIdTokenVerifier(httpTransport,
					jsonFactory);
	}

	@Override
	public String getAuthorizationUrl() throws IOException {
		return getAuthorizationUrl(null);
	}

	@Override
	public String getAuthorizationUrl(String emailAddress) throws IOException {
		GoogleAuthorizationCodeRequestUrl urlBuilder = null;
		urlBuilder = new GoogleAuthorizationCodeRequestUrl(clientSecrets
				.getDetails().getClientId(), callbackUri, requestedScopes);
		// .setAccessType("online") // these can be used to force approval each
		// time the user logs in if we wish.
		// .setApprovalPrompt("force");

		urlBuilder.set("state", getAntiForgeryStateToken());

		if (emailAddress != null) {
			urlBuilder.set("user_id", emailAddress);
		}

		return urlBuilder.build();
	}

	@Override
	public String extractAuthCode(String url) throws IOException {
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
	public synchronized String exchangeCode(String authorizationCode)
			throws IOException, CodeExchangeException {
		try {
			GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
					httpTransport, jsonFactory, clientSecrets.getDetails()
							.getClientId(), clientSecrets.getDetails()
							.getClientSecret(), authorizationCode, callbackUri)
					.execute();

			// I don't really want to use the flow storage but it seems to be
			// easier to get credentials this way.
			GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
					httpTransport, jsonFactory, clientSecrets.getDetails()
							.getClientId(), clientSecrets.getDetails()
							.getClientSecret(), requestedScopes)
					.setDataStoreFactory(
							MemoryDataStoreFactory.getDefaultInstance())
					.build();

			Credential credential = flow.createAndStoreCredential(response,
					authorizationCode);
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
	public synchronized User getUserInfo(String internalProviderReference)
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
		Oauth2 userInfoService = new Oauth2.Builder(new NetHttpTransport(),
				new JacksonFactory(), credentials).setApplicationName(
				Constants.APPLICATION_NAME).build();
		Userinfoplus userInfo = null;

		try {
			userInfo = userInfoService.userinfo().get().execute();
			log.debug("Retrieved User info from google: "
					+ userInfo.toPrettyString());
		} catch (IOException e) {
			log.error("An IO error occurred while trying to retrieve user information: "
					+ e);
		}
		if (userInfo != null && userInfo.getId() != null) {

			return new User(userInfo.getId(), userInfo.getGivenName(),
					userInfo.getFamilyName(), userInfo.getEmail(), null, null,
					null, null, null, null, null);
		} else {
			throw new NoUserIdException();
		}
	}

	@Override
	public String getAntiForgeryStateToken() {
		String antiForgerySalt = new BigInteger(130, new SecureRandom())
				.toString(32);

		String antiForgeryStateToken = "google" + antiForgerySalt;

		return antiForgeryStateToken;
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
	private boolean verifyAccessTokenIsValid(Credential credentials) {
		Validate.notNull(credentials, "Credentials cannot be null");

		Oauth2 oauth2 = new Oauth2.Builder(httpTransport, jsonFactory,
				credentials).setApplicationName(Constants.APPLICATION_NAME)
				.build();
		try {
			Tokeninfo tokeninfo = oauth2.tokeninfo()
					.setAccessToken(credentials.getAccessToken()).execute();

			if (tokeninfo.getAudience().equals(
					clientSecrets.getDetails().getClientId())) {
				return true;
			}
		} catch (IOException e) {
			log.error("IO error while trying to validate oauth2 security token.");
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public AuthenticationProvider getAuthenticationProvider() {
		return AuthenticationProvider.GOOGLE;
	}
}
