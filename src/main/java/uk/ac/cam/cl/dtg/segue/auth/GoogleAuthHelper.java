package uk.ac.cam.cl.dtg.segue.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

/**
 * A helper class for Google's OAuth2 authentication API.
 */
public final class GoogleAuthHelper{
	// location of json file in resource path that contains the client id / secret
	private static final String AUTH_RESOURCE_LOC = "/client_secret.json";
	private static final String AUTH_USER_ID = "UserEmail";
	private static final String AUTH_USER_EMAIL = "UserEmail";	
	private static final String CALLBACK_URI = "http://localhost:8080/rutherford-server/segue/api/auth/google/callback";

	private static final Collection<String> SCOPE = Arrays.asList("https://www.googleapis.com/auth/userinfo.profile;https://www.googleapis.com/auth/userinfo.email".split(";"));
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	
	private static GoogleClientSecrets clientSecrets = null;

	private String stateToken;

	private final GoogleAuthorizationCodeFlow flow;

	/**
	 * Constructor initializes the Google Authorization Code Flow with CLIENT ID, SECRET, and SCOPE 
	 * @throws IOException 
	 */
	public GoogleAuthHelper() {
		flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
				JSON_FACTORY, clientSecrets.getDetails().getClientId(), clientSecrets.getDetails().getClientSecret(), SCOPE).build();

		generateStateToken();
	}

	/**
	 * Generates a secure state token 
	 */
	private void generateStateToken(){

		SecureRandom sr1 = new SecureRandom();

		stateToken = "google;"+sr1.nextInt();

	}

	/**
	 * Accessor for state token
	 */
	public String getStateToken(){
		return stateToken;
	}

	public String getAuthorizationUrl(String emailAddress, HttpServletRequest request) throws IOException {
		GoogleAuthorizationCodeRequestUrl urlBuilder = null;
		urlBuilder = new GoogleAuthorizationCodeRequestUrl(
				getClientCredential().getDetails().getClientId(),
				CALLBACK_URI,
				SCOPE)
		.setAccessType("offline")
		.setApprovalPrompt("force");

		urlBuilder.set("state", request.getRequestURI());

		if (emailAddress != null) {
			urlBuilder.set("user_id", emailAddress);
		}

		return urlBuilder.build();
	}

	public Credential exchangeCode(String authorizationCode) throws CodeExchangeException{
		try {
			GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
					new NetHttpTransport(), JSON_FACTORY, 
							getClientCredential().getWeb().getClientId(), 
							getClientCredential().getWeb().getClientSecret(),
					authorizationCode, CALLBACK_URI).execute();
			return buildEmptyCredential().setFromTokenResponse(response);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CodeExchangeException();
		}
		
	}

	public Credential buildEmptyCredential() {
		try {
			return new GoogleCredential.Builder()
			.setClientSecrets(getClientCredential())
			.setTransport(new NetHttpTransport())
			.setJsonFactory(JSON_FACTORY).build();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public GoogleClientSecrets getClientCredential() throws IOException {
		if (clientSecrets == null) {
			InputStream inputStream = GoogleAuthHelper.class.getResourceAsStream(AUTH_RESOURCE_LOC);
			Preconditions.checkNotNull(inputStream, "missing resource %s",
					AUTH_RESOURCE_LOC);

			InputStreamReader isr = new InputStreamReader(inputStream);

			clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, isr);
			Preconditions
			.checkArgument(
					!clientSecrets.getDetails().getClientId().startsWith("[[")
					&& !clientSecrets.getDetails().getClientSecret()
					.startsWith("[["),
					"Please enter your client ID and secret from the Google APIs Console in %s from the "
							+ "root samples directory", AUTH_RESOURCE_LOC);
		}
		return clientSecrets;
	}

	public Credential getStoredCredential(String userId, CredentialStore credentialStore) throws IOException {
		Credential credential = buildEmptyCredential();
		if (credentialStore.load(userId, credential)) {
			return credential;
		}
		return null;
	}

	public Credential getActiveCredential(HttpServletRequest request, CredentialStore credentialStore) throws Exception {

		String userId = (String) request.getSession().getAttribute(AUTH_USER_ID);
		Credential credential = null;

		if (userId != null) {
			credential = getStoredCredential(userId, credentialStore);
		}

		if ((credential == null || credential.getRefreshToken() == null) && request.getParameter("code") != null) {
			credential = exchangeCode(request.getParameter("code"));

			if (credential != null) {
				if (credential.getRefreshToken() != null) {
					credentialStore.store(userId, credential);
				}
			}
		}

		if (credential == null || credential.getRefreshToken() == null) {
			String email = (String) request.getSession().getAttribute(AUTH_USER_EMAIL);
			String authorizationUrl = getAuthorizationUrl(email, request);
			throw new Exception(authorizationUrl);
		}

		return credential;
	}
	
}
