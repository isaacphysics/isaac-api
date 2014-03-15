package uk.ac.cam.cl.dtg.segue.api;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.segue.auth.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.GoogleAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.NoUserIdException;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.database.PersistenceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.User;

/**
 *  This class is responsible for all low level user management actions e.g. authentication and registration.
 *
 */
public class UserManager{

	public static final String SESSION_USER_ID = "currentUserId";
	
	public enum AuthenticationProvider {GOOGLE, FACEBOOK, RAVEN;};
	
	private IUserDataManager database;
	private static final Logger log = LoggerFactory.getLogger(UserManager.class);

	//TODO: Move the below to somewhere else!
	private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
	private static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
	private static final String DATE_SIGNED = "DATE_SIGNED";
	private static final String SESSION_ID = "SESSION_ID";
	private static final String HMAC = "HMAC";
	private static final String HMAC_KEY = "fbf4c8996fb92427ae41e4649SUPER-SECRET-KEY896354df48w7q5s231a";
	
	@Inject
	public UserManager(IUserDataManager database){
		this.database = database;
	}
	
	/**
	 * This method will attempt to authenticate the user and provide a user object back to the caller.
	 * 
	 * @param request
	 * @param provider
	 * @return A response containing a user object or a redirect URI to the authentication provider if authorization / login is required.
	 */
	public Response authenticate(HttpServletRequest request, String provider){
		// get the current user based on their session id information.
		User currentUser = getCurrentUser(request);

		if(null != currentUser){
			return Response.ok().entity(currentUser).build();
		}

		// Ok we don't have a current user so now we have to go ahead and try and authenticate them.
		IFederatedAuthenticator federatedAuthenticator = mapToProvider(provider);

		if(null == federatedAuthenticator){
			log.error("Unable to map to an authenticator. The provider: " + provider + " is unknown");
			return Response.serverError().entity("Failed to identify authenticator.").build();
		}

		// if we are an OAuthProvider redirect to the provider authorization url.	
		if(federatedAuthenticator instanceof IOAuth2Authenticator){
			IOAuth2Authenticator oauthProvider = (IOAuth2Authenticator) federatedAuthenticator;

			try {
				// to deal with cross site request forgery
				request.getSession().setAttribute("state", oauthProvider.getAntiForgeryStateToken());
				URI redirectLink = URI.create(oauthProvider.getAuthorizationUrl());
				return Response.temporaryRedirect(redirectLink).entity(redirectLink).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.error("IOException when trying to redirect to OAuth provider");
			}
		}
	
		return Response.serverError().entity("We should never see this if a correct provider has been given").build();
	}

	/**
	 * Authenticate Callback will receive the authentication information from the different provider types. 
	 * (e.g. OAuth 2.0 (IOAuth2Authenticator) or bespoke)
	 * 
	 * @param request
	 * @param response
	 * @param provider
	 * @return Response containing the populated user DTO.
	 */
	public Response authenticateCallback(HttpServletRequest request, HttpServletResponse response, String provider){
		User currentUser = getCurrentUser(request);

		if(null != currentUser){
			log.warn("We already have a cookie set with a valid user. We won't proceed with authentication callback logic.");
			return Response.ok().entity(currentUser).build();
		}

		IFederatedAuthenticator federatedAuthenticator = mapToProvider(provider);

		if(null == federatedAuthenticator){
			log.error("Unable to identify authenticator");
			return Response.serverError().entity("Unable to identify authenticator").build();
		}

		// if we are an OAuthProvider complete next steps of oauth
		if(federatedAuthenticator instanceof IOAuth2Authenticator){
			IOAuth2Authenticator oauthProvider = (IOAuth2Authenticator) federatedAuthenticator;

			// verify there is no cross site request forgery going on.
			if(!ensureNoCSRF(request) || request.getQueryString() == null)
				return Response.status(401).entity("CSRF check failed.").build();

			// this will have our authorization code within it.
			StringBuffer fullUrlBuf = request.getRequestURL();
			fullUrlBuf.append('?').append(request.getQueryString());

			try{
				// extract auth code from string buffer
				String authCode = oauthProvider.extractAuthCode(fullUrlBuf.toString());

				if (authCode == null) {
					log.info("User denied access to our app.");
					return Response.status(401).entity("Provider failed to give us an authorization code.").build();
				} else {	      
					log.debug("User granted access to our app");

					String internalReference = oauthProvider.exchangeCode(authCode);
					log.info(request.getSession().getId());

					// get user info from provider
					// note the userid field in this object will contain the providers user id.
					User userFromProvider = federatedAuthenticator.getUserInfo(internalReference);
					
					if(null == userFromProvider)
						return Response.noContent().entity("Can't create user").build();

					log.info("User with name " + userFromProvider.getEmail() + " retrieved");

					// this is the providers unique id for the user
					String providerId = userFromProvider.getDbId();
					
					// clear user object id so that it is ready to receive our local one.
					userFromProvider.setDbId(null);

					AuthenticationProvider providerReference = AuthenticationProvider.valueOf(provider.toUpperCase());					
					User localUserInformation = this.getUserFromLinkedAccount(providerReference, providerId);

					//decide if we need to register a new user or link to an existing account
					if(null == localUserInformation){
						log.info("New registration - User does not already exist.");
						//register user
						String localUserId = registerUser(userFromProvider, providerReference, providerId);
						localUserInformation = this.database.getById(localUserId);
						
						if(null == localUserInformation){
							// we just put it in so something has gone very wrong.
							log.error("Failed to retreive user even though we just put it in the database.");
							throw new NoUserIdException();
						}
					}
					
					// create a signed session for this user so that we don't need to do this again for a while.
					this.createSession(request, localUserInformation.getDbId());
					
					log.info("Cookie with userid = " + request.getSession().getAttribute(SESSION_USER_ID));
					return Response.ok(localUserInformation).build();
				}				
			}
			catch(IOException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoUserIdException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CodeExchangeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return Response.ok().build();
	}
	
	public User getUserFromLinkedAccount(AuthenticationProvider provider, String providerId){
		User user = database.getByLinkedAccount(provider, providerId);
		
		log.info("Unable to locate user based on provider information provided.");
		return user;
	}

	/**
	 * Get the details of the currently logged in user
	 * TODO: test me
	 * 
	 * @return Returns the current user DTO if we can get it or null if user is not currently logged in
	 */
	public User getCurrentUser(HttpServletRequest request){
		//Injector injector = Guice.createInjector(new PersistenceConfigurationModule());

		// get the current user based on their session id information.
		String currentUserId = (String) request.getSession().getAttribute(UserManager.SESSION_USER_ID);

		// check if the users session is validated using our credentials.
		
		if(!this.validateUsersSession(request)){
			log.error("User session failed validation. Assume they are not logged in.");
			return null;
		}
		
		if(null == currentUserId){			
			log.info("Current userID is null");
			return null;
		}

		// retrieve the user from database.
		
		return database.getById(currentUserId);
	}

	/**
	 * This method destroys the users current session and may do other clean up activities.
	 * 
	 * @param request - from the current user
	 */
	public void logUserOut(HttpServletRequest request){
		request.getSession().invalidate();
	}
	
	/**
	 * Creates a signed session for the user so we know that it is them when we check them.
	 * 
	 * @param request
	 * @param userId
	 */
	public void createSession(HttpServletRequest request, String userId){
		String currentDate = new SimpleDateFormat(DATE_FORMAT).format(new Date());
		String sessionId =  request.getSession().getId();
		String sessionHMAC = this.calculateHMAC(HMAC_KEY+userId + sessionId + currentDate, userId + sessionId + currentDate);
		
		request.getSession().setAttribute(SESSION_USER_ID, userId);
		request.getSession().setAttribute(SESSION_ID, sessionId);
		request.getSession().setAttribute(DATE_SIGNED, currentDate);
		request.getSession().setAttribute(HMAC,sessionHMAC);
	}

	/**
	 * Verifies that the signed session is valid 
	 * Currently only confirms the signature.
	 * 
	 * @param request
	 * @return True if we are happy, false if we are not.
	 */
	public boolean validateUsersSession(HttpServletRequest request){
		String userId = (String) request.getSession().getAttribute(SESSION_USER_ID);
		String currentDate = (String) request.getSession().getAttribute(DATE_SIGNED);
		String sessionId = (String) request.getSession().getAttribute(SESSION_ID);
		String sessionHMAC = (String) request.getSession().getAttribute(HMAC);
		
		String ourHMAC = this.calculateHMAC(HMAC_KEY+userId + sessionId + currentDate, userId + sessionId + currentDate);
		
		if(ourHMAC.equals(sessionHMAC)){
			log.info("Valid user session continuing...");
			return true;	
		}
		else
		{
			log.info("Invalid user session detected");
			return false;
		}
			
	}
	
	private String calculateHMAC(String key, String dataToSign){
		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA1_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
			mac.init(signingKey);
			
			byte[] rawHmac = mac.doFinal(dataToSign.getBytes());
			
			String result = new String(Base64.encodeBase64(rawHmac));
			return result;
		} catch (GeneralSecurityException e) {
			log.warn("Unexpected error while creating hash: " + e.getMessage(),	e);
			throw new IllegalArgumentException();
		}		
	}

	private IFederatedAuthenticator mapToProvider(String provider){
		Injector injector = Guice.createInjector(new PersistenceConfigurationModule());
		IFederatedAuthenticator federatedAuthenticator = null;
		
		log.error(provider + " compared to " + AuthenticationProvider.GOOGLE.name());
		
		if(AuthenticationProvider.GOOGLE.name().equals(provider.toUpperCase())){
			federatedAuthenticator = injector.getInstance(GoogleAuthenticator.class);
		}

		return federatedAuthenticator;
	}
	
	/**
	 * This method will compare the state in the users cookie with the response from the provider.
	 * 
	 * @param request
	 * @return True if we are satisfied that they match and false if we think there is a problem.
	 */
	private boolean ensureNoCSRF(HttpServletRequest request){
		// to deal with cross site request forgery
		String csrfTokenFromUser = (String) request.getSession().getAttribute("state");
		String csrfTokenFromProvider = request.getParameter("state");

		if(null == csrfTokenFromUser || null == csrfTokenFromProvider ||!csrfTokenFromUser.equals(csrfTokenFromProvider)){
			log.error("Invalid state parameter - Provider said: " + request.getParameter("state") + " Session said: " + request.getSession().getAttribute("state"));
			return false;
		}
		else
		{
			log.debug("State parameter matches - Provider said: " + request.getParameter("state") + " Session said: " + request.getSession().getAttribute("state"));
			return true;
		}
	}

	/**
	 * This method should handle the situation where we haven't seen a user before.
	 * 
	 * @param user from authentication provider
	 * @param provider information
	 * @param unique reference for this user held by the authentication provider.
	 * @return The localUser account user id of the user after registration.
	 */
	private String registerUser(User user, AuthenticationProvider provider, String providerId){
		String userId = database.register(user, provider, providerId);
		return userId;
	}	
}
