package uk.ac.cam.cl.dtg.segue.auth;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.WeakHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dto.User;

import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfoplus;

public class GoogleAuthenticator implements IFederatedAuthenticator, IOAuth2Authenticator  {

	private static final Logger log = LoggerFactory.getLogger(GoogleAuthenticator.class);

	// location of json file in resource path that contains the client id / secret
	//private static final String AUTH_RESOURCE_LOC = "/client_secret_local.json";
	private static final String AUTH_RESOURCE_LOC = "/client_secret_dev.json";
	
	//TODO: move these somewhere else
	//private static final String CALLBACK_URI = "http://localhost:8080/rutherford-server/segue/api/auth/google/callback";
	private static final String CALLBACK_URI = "http://www.cl.cam.ac.uk/~ipd21/isaac-staging/segue/api/auth/google/callback";	
	
	private static final Collection<String> SCOPE = Arrays.asList("https://www.googleapis.com/auth/userinfo.profile;https://www.googleapis.com/auth/userinfo.email".split(";"));
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	
	private WeakHashMap<String, Credential> credentialStore;
	private GoogleClientSecrets clientSecrets = null;
	private GoogleAuthorizationCodeFlow flow;

	private String antiForgeryStateToken;

	public GoogleAuthenticator(){
		try {
			getClientCredential();
			flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT,
					JSON_FACTORY, 
					clientSecrets.getDetails().getClientId(), 
					clientSecrets.getDetails().getClientSecret(), SCOPE)
					.setDataStoreFactory(MemoryDataStoreFactory.getDefaultInstance())
					.build();
			if(credentialStore == null)
				credentialStore = new WeakHashMap<String, Credential>();
			
		} catch (IOException e) {
			e.printStackTrace();
			log.error("IOException occurred while trying to initialise the Google Authenticator.");
		}

		generateAntiForgeryStateToken();
	}

	@Override
	public String getAuthorizationUrl() throws IOException {
		return getAuthorizationUrl(null);
	}

	@Override
	public String getAuthorizationUrl(String emailAddress) throws IOException {
		GoogleAuthorizationCodeRequestUrl urlBuilder = null;
		urlBuilder = new GoogleAuthorizationCodeRequestUrl(
				getClientCredential().getDetails().getClientId(),
				CALLBACK_URI,
				SCOPE);
		//.setAccessType("online")
		//.setApprovalPrompt("force");

		urlBuilder.set("state", this.antiForgeryStateToken);

		if (emailAddress != null) {
			urlBuilder.set("user_id", emailAddress);
		}

		// generatedNewAntiForgery Token for the next person.
		generateAntiForgeryStateToken();
		return urlBuilder.build();
	}

	@Override
	public String extractAuthCode(String url) throws IOException {
		AuthorizationCodeResponseUrl authResponse = new AuthorizationCodeResponseUrl(url.toString());

		if (authResponse.getError() != null) {
			log.info("User denied access to our app.");
		} else {
			log.info("User granted access to our app.");
		}

		return authResponse.getCode();
	}

	@Override
	public String exchangeCode(String authorizationCode) throws IOException, CodeExchangeException {
		try {
			GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(
					HTTP_TRANSPORT, JSON_FACTORY, getClientCredential().getDetails().getClientId(), getClientCredential().getDetails().getClientSecret(),
					authorizationCode, CALLBACK_URI).execute();
			
			//Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setFromTokenResponse(response);
			Credential credential = flow.createAndStoreCredential(response, authorizationCode);
			
			String internalReferenceToken = UUID.randomUUID().toString();
			
			credentialStore.put(internalReferenceToken, credential);
			
			// I don't really want to use the flow storage.
			flow.getCredentialDataStore().clear();
			
			return internalReferenceToken;
		} catch (IOException e) {
			System.err.println("An error occurred: " + e);
			throw new CodeExchangeException();
		}
	}

	private GoogleClientSecrets getClientCredential() throws IOException {
		if (clientSecrets == null) {
			// load up the client secrets from the file system.
			InputStream inputStream = GoogleAuthenticator.class.getResourceAsStream(AUTH_RESOURCE_LOC);
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

	@Override
	public User getUserInfo(String internalProviderReference) throws NoUserIdException, IOException {
		Credential credentials = credentialStore.get(internalProviderReference);
		
		Oauth2 userInfoService = new Oauth2.Builder(new NetHttpTransport(), new JacksonFactory(), credentials).build();
		Userinfoplus userInfo = null;

		try {
			userInfo = userInfoService.userinfo().get().execute();
			log.debug("Retrieved User info from google: " + userInfo.toPrettyString());
		} catch (IOException e) {
			log.error("An IO error occurred while trying to retrieve user information: " + e);
		}
		if (userInfo != null && userInfo.getId() != null) {

			return new User(userInfo.getId(), userInfo.getGivenName(), userInfo.getFamilyName(),userInfo.getEmail(),null, null,null,null,null);
		} else {
			throw new NoUserIdException();
		}
	}

	@Override
	public String getAntiForgeryStateToken(){
		return antiForgeryStateToken;
	}
	
	private void generateAntiForgeryStateToken(){
		String antiForgerySalt = new BigInteger(130, new SecureRandom()).toString(32);

		antiForgeryStateToken = "google"+antiForgerySalt;
	}

}
