package uk.ac.cam.cl.dtg.segue.api;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.NoUserIdException;
import uk.ac.cam.cl.dtg.segue.dao.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.QuestionAttempt;
import uk.ac.cam.cl.dtg.segue.dto.users.User;

/**
 * This class is responsible for all low level user management actions e.g.
 * authentication and registration. TODO: Split authentication functionality
 * into another class and let this one focus on maintaining our segue user
 * state.
 */
public class UserManager {
	private static final Logger log = LoggerFactory
			.getLogger(UserManager.class);

	private static final String HMAC_SHA_ALGORITHM = "HmacSHA1";
	private static final String DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";

	private static final String QUESTION_ATTEMPTS_FIELDNAME = "questionAttempts";

	private final IUserDataManager database;
	private final String hmacSalt;
	private final Map<AuthenticationProvider, IFederatedAuthenticator> 
	registeredAuthProviders;

	/**
	 * Create an instance of the user manager class.
	 * 
	 * @param database - an IUserDataManager that will support persistence.
	 * @param hmacSalt - A random / unique HMAC salt for session authentication. 
	 * @param providersToRegister - A map of known authentication providers.
	 */
	@Inject
	public UserManager(
			final IUserDataManager database,
			@Named(Constants.HMAC_SALT) final String hmacSalt,
			final Map<AuthenticationProvider, 
			IFederatedAuthenticator> providersToRegister) {
		Validate.notNull(database);
		Validate.notNull(hmacSalt);
		Validate.notNull(providersToRegister);
		
		this.database = database;
		this.hmacSalt = hmacSalt;
		this.registeredAuthProviders = providersToRegister;
	}

	/**
	 * This method will attempt to authenticate the user and provide a user
	 * object back to the caller.
	 * 
	 * @param request
	 *            - http request that we can attach the session to.
	 * @param provider
	 *            - the provider the user wishes to authenticate with.
	 * @param redirectUrl - optional redirect Url for when authentication has completed.           
	 * @return A response redirecting the user to their redirect url or a redirect URI to the
	 *         authentication provider if authorization / login is required.
	 */
	public final Response authenticate(final HttpServletRequest request,
			final String provider, @Nullable final String redirectUrl) {
		// set redirect url as a session attribute so we can pick it up when call back happens.
		if (redirectUrl != null) {
			this.storeRedirectUrl(request, redirectUrl);
		} else {
			this.storeRedirectUrl(request, "/");
		}
		
		// get the current user based on their session id information.
		User currentUser = getCurrentUser(request);
		if (null != currentUser) {
			try {
				return Response.temporaryRedirect(this.loadRedirectUrl(request)).build();
			} catch (URISyntaxException e) {
				log.error("Redirect URL is not valid for provider " + provider, e);
				return new SegueErrorResponse(Status.BAD_REQUEST, 
						"Bad authentication redirect url received.", e).toResponse();
			}
		}

		// Ok we don't have a current user so now we have to go ahead and try
		// and authenticate them.
		try {
			IFederatedAuthenticator federatedAuthenticator = 
					mapToProvider(provider);

			// if we are an OAuth2Provider redirect to the provider
			// authorization url.
			if (federatedAuthenticator instanceof IOAuth2Authenticator) {
				IOAuth2Authenticator oauthProvider = 
						(IOAuth2Authenticator) federatedAuthenticator;

				URI redirectLink = URI.create(oauthProvider
						.getAuthorizationUrl());
				List<NameValuePair> urlParams = URLEncodedUtils.parse(
						redirectLink.toString(), Charset.defaultCharset());

				// to deal with cross site request forgery (Provider should
				// embed a state param in the uri returned in the redirectLink)
				String antiForgeryTokenFromProvider = null;

				for (NameValuePair param : urlParams) {
					if (param.getName().equals(Constants.STATE_PARAM_NAME)) {
						antiForgeryTokenFromProvider = param.getValue();
					}
				}

				if (null == antiForgeryTokenFromProvider) {
					SegueErrorResponse error = new SegueErrorResponse(
							Status.INTERNAL_SERVER_ERROR,
							"Anti forgery authenitication error."
									+ " Please contact server admin.");
					log.error("Unable to extract Anti Forgery Token "
							+ "from Authentication provider");
					return error.toResponse();
				}

				// Store antiForgeryToken in the users session.
				request.getSession().setAttribute(Constants.STATE_PARAM_NAME,
						antiForgeryTokenFromProvider);

				return Response.temporaryRedirect(redirectLink)
						.entity(redirectLink).build();
			} else {
				SegueErrorResponse error = new SegueErrorResponse(
						Status.INTERNAL_SERVER_ERROR,
						"Unable to map to a known authenticator. The provider: "
								+ provider + " is unknown");
				log.error(error.getErrorMessage());
				return error.toResponse();
			}
		} catch (IllegalArgumentException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Error mapping to a known authenticator. The provider: "
							+ provider + " is unknown");
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (IOException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"IOException when trying to redirect to OAuth provider", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}
	}

	/**
	 * Authenticate Callback will receive the authentication information from
	 * the different provider types. (e.g. OAuth 2.0 (IOAuth2Authenticator) or
	 * bespoke)
	 * 
	 * @param request
	 *            - http request from the user.
	 * @param response
	 *            - http response for the user.
	 * @param provider
	 *            - the provider who has just authenticated the user.
	 * @return Response redirecting the user to their redirect url.
	 */
	public final Response authenticateCallback(
			final HttpServletRequest request,
			final HttpServletResponse response, final String provider) {
		User currentUser = getCurrentUser(request);

		if (null != currentUser) {
			log.info("We already have a cookie set with a valid user. "
					+ "We won't proceed with authentication callback logic.");
			try {
				return Response.temporaryRedirect(this.loadRedirectUrl(request)).build();
			} catch (URISyntaxException e) {
				log.error("Redirect URL is not valid for provider " + provider, e);
				return new SegueErrorResponse(Status.BAD_REQUEST, 
						"Bad authentication redirect url received.", e).toResponse();
			}
		}

		// Ok we don't have a current user so now we have to go ahead and try
		// and authenticate them.
		try {
			IFederatedAuthenticator federatedAuthenticator = 
					mapToProvider(provider);

			String providerSpecificUserLookupReference = null;

			// if we are an OAuth2Provider complete next steps of oauth
			if (federatedAuthenticator instanceof IOAuth2Authenticator) {
				IOAuth2Authenticator oauthProvider = 
						(IOAuth2Authenticator) federatedAuthenticator;

				providerSpecificUserLookupReference = this
						.getOauth2InternalRefCode(oauthProvider, request);
			} else {
				// This should catch any invalid providers
				SegueErrorResponse error = new SegueErrorResponse(
						Status.INTERNAL_SERVER_ERROR,
						"Unable to map to a known authenticator. The provider: "
								+ provider + " is unknown");
				log.error(error.getErrorMessage());
				return error.toResponse();
			}

			// Retrieve the local user object which will be retrieved /
			// generated.
			User localUserInformation = this
					.getUserFromFederatedProvider(federatedAuthenticator,
							providerSpecificUserLookupReference);

			// create a signed session for this user so that we don't need to do
			// this again for a while.
			this.createSession(request, localUserInformation.getDbId());

			return Response.temporaryRedirect(this.loadRedirectUrl(request)).build();

		} catch (IllegalArgumentException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"Unable to map to a known authenticator. The provider: "
							+ provider + " is unknown");
			log.warn(error.getErrorMessage());
			return error.toResponse();
		} catch (IOException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.UNAUTHORIZED,
					"Exception while trying to authenticate a user"
							+ " - during callback step", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (NoUserIdException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.UNAUTHORIZED, "Unable to locate user information.");
			log.error("No userID exception received. Unable to locate user.", e);
			return error.toResponse();
		} catch (CodeExchangeException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.UNAUTHORIZED, "Security code exchange failed.");
			log.error("Unable to verify security code.", e);
			return error.toResponse();
		} catch (AuthenticatorSecurityException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.UNAUTHORIZED, "Error during security checks.");
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (AuthenticationCodeException | CrossSiteRequestForgeryException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.UNAUTHORIZED, e.getMessage());
			log.info(e.getMessage(), e);
			return error.toResponse();
		} catch (URISyntaxException e) {
			log.error("Redirect URL is not valid for provider " + provider, e);
			return new SegueErrorResponse(Status.BAD_REQUEST, 
					"Bad authentication redirect url received.", e).toResponse();
		}
	}

	/**
	 * Get the details of the currently logged in user.
	 * 
	 * @param request
	 *            - to retrieve session information from
	 * @return Returns the current user DTO if we can get it or null if user is
	 *         not currently logged in
	 */
	public final User getCurrentUser(final HttpServletRequest request) {
		Validate.notNull(request);

		// get the current user based on their session id information.
		String currentUserId = (String) request.getSession().getAttribute(
				Constants.SESSION_USER_ID);

		if (null == currentUserId) {
			log.debug("Current userID is null. Assume they are not logged in.");
			return null;
		}

		// check if the users session is validated using our credentials.
		if (!this.validateUsersSession(request)) {
			log.info("User session has failed validation. "
					+ "Assume they are not logged in.");
			return null;
		}

		// retrieve the user from database.
		return database.getById(currentUserId);
	}

	/**
	 * This method destroys the users current session and may do other clean up
	 * activities.
	 * 
	 * @param request
	 *            - from the current user
	 */

	/**
	 * Destroy a session attached to the request.
	 * 
	 * @param request
	 *            containing the session to destroy
	 */
	public final void logUserOut(final HttpServletRequest request) {
		Validate.notNull(request);
		try {
			request.getSession().invalidate();
		} catch (IllegalStateException e) {
			log.info("The session has already been invalidated. "
					+ "Unable to logout again...", e);
		}
	}

	/**
	 * Creates a signed session for the user so we know that it is them when we
	 * check them.
	 * 
	 * @param request
	 * @param userId
	 */

	/**
	 * Create a session and attach it to the request provided.
	 * 
	 * @param request
	 *            to store the session
	 * @param userId
	 *            to associate the session with
	 */
	public final void createSession(final HttpServletRequest request,
			final String userId) {
		Validate.notNull(request);
		Validate.notBlank(userId);

		String currentDate = new SimpleDateFormat(DATE_FORMAT)
				.format(new Date());
		String sessionId = request.getSession().getId();
		String sessionHMAC = this.calculateHMAC(hmacSalt + userId + sessionId
				+ currentDate, userId + sessionId + currentDate);

		request.getSession().setAttribute(Constants.SESSION_USER_ID, userId);
		request.getSession().setAttribute(Constants.SESSION_ID, sessionId);
		request.getSession().setAttribute(Constants.DATE_SIGNED, currentDate);
		request.getSession().setAttribute(Constants.HMAC, sessionHMAC);
	}

	/**
	 * Executes checks on the users sessions to ensure it is valid
	 * 
	 * Checks include verifying the HMAC and the session creation date.
	 * 
	 * @param request
	 *            - request containing session information
	 * @return true if it is still valid, false if not.
	 */
	public final boolean validateUsersSession(final HttpServletRequest request) {
		Validate.notNull(request);

		String userId = (String) request.getSession().getAttribute(
				Constants.SESSION_USER_ID);
		String currentDate = (String) request.getSession().getAttribute(
				Constants.DATE_SIGNED);
		String sessionId = (String) request.getSession().getAttribute(
				Constants.SESSION_ID);
		String sessionHMAC = (String) request.getSession().getAttribute(
				Constants.HMAC);

		String ourHMAC = this.calculateHMAC(hmacSalt + userId + sessionId
				+ currentDate, userId + sessionId + currentDate);

		if (null == userId) {
			log.debug("No session set so not validating user identity.");
			return false;
		}

		if (ourHMAC.equals(sessionHMAC)) {
			log.debug("Valid user session continuing...");
			return true;
		} else {
			log.info("Invalid user session detected");
			return false;
		}
	}
	
	/**
	 * Record that a user has answered a question.
	 * 
	 * @param user - user who answered the question
	 * @param questionResponse - question results.
	 */
	public final void recordUserQuestionInformation(final User user, 
			final QuestionValidationResponse questionResponse) {
		
		QuestionAttempt questionAttempt 
			= new QuestionAttempt(questionResponse.getQuestionId(), new Date(),
					questionResponse.isCorrect(), questionResponse.getAnswer());
		
		this.database.addItemToUserField(
				user, 
				QUESTION_ATTEMPTS_FIELDNAME, 
				Lists.newArrayList(questionAttempt));
		log.info("Question information recorded for user: " + user.getDbId());
	}
	
	/**
	 * Method to update a user object in our database.
	 * 
	 * @param user
	 */
	private void updateUserObject(final User user) {
		this.database.updateUser(user);
	}

	/**
	 * Generate an HMAC using a key and the data to sign.
	 * 
	 * @param key
	 *            - HMAC key for signing
	 * @param dataToSign
	 *            - data to be signed
	 * @return HMAC - Unique HMAC.
	 */
	private String calculateHMAC(final String key, final String dataToSign) {
		Validate.notEmpty(key, "Signing key cannot be blank.");
		Validate.notEmpty(dataToSign, "Data to sign cannot be blank.");

		try {
			SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(),
					HMAC_SHA_ALGORITHM);
			Mac mac = Mac.getInstance(HMAC_SHA_ALGORITHM);
			mac.init(signingKey);

			byte[] rawHmac = mac.doFinal(dataToSign.getBytes());

			String result = new String(Base64.encodeBase64(rawHmac));
			return result;
		} catch (GeneralSecurityException e) {
			log.warn("Unexpected error while creating hash: " + e.getMessage(),
					e);
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Attempts to map a string to a known provider.
	 * 
	 * @param provider
	 *            - String representation of the provider requested
	 * @return the FederatedAuthenticator object which can be used to get a
	 *         user.
	 */
	private IFederatedAuthenticator mapToProvider(final String provider) {
		Validate.notEmpty(provider,
				"Provider name must not be empty or null if we are going "
						+ "to map it to an implementation.");

		AuthenticationProvider enumProvider = null;
		try {
			enumProvider = AuthenticationProvider.valueOf(provider
					.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("The provider requested is "
					+ "invalid and not a known AuthenticationProvider: "
					+ provider);
		}

		if (!registeredAuthProviders.containsKey(enumProvider)) {
			throw new IllegalArgumentException("This authentication provider"
					+ " has not been registered / implemented yet: " + provider);
		}

		log.debug("Mapping provider: " + provider + " to " + enumProvider);

		return this.registeredAuthProviders.get(enumProvider);
	}

	/**
	 * Verify with the request that there is no CSRF violation.
	 * 
	 * @param request
	 *            - http request to verify there is no CSRF
	 * @return true if we are happy , false if we think a violation has
	 *         occurred.
	 */
	private boolean ensureNoCSRF(final HttpServletRequest request) {
		Validate.notNull(request);

		// to deal with cross site request forgery
		String csrfTokenFromUser = (String) request.getSession().getAttribute(
				Constants.STATE_PARAM_NAME);
		String csrfTokenFromProvider = request.getParameter(Constants.STATE_PARAM_NAME);

		if (null == csrfTokenFromUser || null == csrfTokenFromProvider
				|| !csrfTokenFromUser.equals(csrfTokenFromProvider)) {
			log.error("Invalid state parameter - Provider said: "
					+ request.getParameter(Constants.STATE_PARAM_NAME) + " Session said: "
					+ request.getSession().getAttribute(Constants.STATE_PARAM_NAME));
			return false;
		} else {
			log.debug("State parameter matches - Provider said: "
					+ request.getParameter(Constants.STATE_PARAM_NAME) + " Session said: "
					+ request.getSession().getAttribute(Constants.STATE_PARAM_NAME));
			return true;
		}
	}

	/**
	 * This method should handle the situation where we haven't seen a user
	 * before.
	 * 
	 * @param user
	 *            from authentication provider
	 * @param provider
	 *            information
	 * @param providerUserId
	 *            - unique id of provider.
	 * @return The localUser account user id of the user after registration.
	 */
	private String registerUser(final User user,
			final AuthenticationProvider provider, final String providerUserId) {
		String userId = database.register(user, provider, providerUserId);
		return userId;
	}

	/**
	 * This method will attempt to find a segue user using a 3rd party provider
	 * and a unique id that identifies the user to the provider.
	 * 
	 * @param provider
	 *            - the provider that we originally validated with
	 * @param providerId
	 *            - the unique ID of the user as given to us from the provider.
	 * @return A user object or null if we were unable to find the user with the
	 *         information provided.
	 */
	private User getUserFromLinkedAccount(
			final AuthenticationProvider provider, final String providerId) {
		Validate.notNull(provider);
		Validate.notBlank(providerId);

		User user = database.getByLinkedAccount(provider, providerId);
		if (null == user) {
			log.info("Unable to locate user based on provider "
					+ "information provided.");
		}
		return user;
	}

	/**
	 * This method should use the provider specific reference to either register
	 * a new user or retrieve an existing user.
	 * 
	 * @param federatedAuthenticator
	 *            the federatedAuthenticator we are using for authentication
	 * @param providerSpecificUserLookupReference
	 *            - the look up reference provided by the authenticator after
	 *            any authenticator specific actions have been completed.
	 * @return a user object that exists in the segue system.
	 * @throws AuthenticatorSecurityException
	 *             - error with authenticator.
	 * @throws NoUserIdException
	 *             - If we are unable to locate the user id based on the lookup
	 *             reference provided.
	 * @throws IOException
	 *             - if there is an io error.
	 */
	private User getUserFromFederatedProvider(
			final IFederatedAuthenticator federatedAuthenticator,
			final String providerSpecificUserLookupReference)
		throws AuthenticatorSecurityException, NoUserIdException,
			IOException {
		// get user info from federated provider
		// note the userid field in this object will contain the providers user
		// id not ours so we should change this or use auto-mapping between
		// different DOs.
		User userFromProvider = federatedAuthenticator
				.getUserInfo(providerSpecificUserLookupReference);

		if (null == userFromProvider) {
			log.warn("Unable to create user for the provider "
					+ federatedAuthenticator.getAuthenticationProvider().name());
			throw new NoUserIdException();
		}

		log.debug("User with name " + userFromProvider.getEmail()
				+ " retrieved");

		// this is the providers unique id for the user we should store it for
		// now
		String providerId = userFromProvider.getDbId();

		// clear user object id so that it is ready to receive our local one.
		userFromProvider.setDbId(null);

		User localUserInformation = this.getUserFromLinkedAccount(
				federatedAuthenticator.getAuthenticationProvider(), providerId);

		// decide if we need to register a new user or link to an existing
		// account
		if (null == localUserInformation) {
			log.info("New registration - User does not already exist.");
			// register user
			String localUserId = registerUser(userFromProvider,
					federatedAuthenticator.getAuthenticationProvider(),
					providerId);
			localUserInformation = this.database.getById(localUserId);

			if (null == localUserInformation) {
				// we just put it in so something has gone very wrong.
				log.error("Failed to retreive user even though we "
						+ "just put it in the database.");
				throw new NoUserIdException();
			}
		} else {
			log.debug("Returning user detected"
					+ localUserInformation.getDbId());
		}

		return localUserInformation;
	}

	/**
	 * This method is an oauth2 specific method which will ultimately provide an
	 * internal reference number that the oauth2 provider can use to lookup the
	 * information of the user who has just authenticated.
	 * 
	 * @param oauthProvider - The provider to authenticate against.
	 * @param request - The request that will contain session information.
	 * @return an internal reference number that will allow retrieval of the
	 *         users information from the provider.
	 * @throws AuthenticationCodeException - possible authentication code issues.
	 * @throws IOException - error reading from client key?
	 * @throws CodeExchangeException - exception whilst exchanging codes
	 * @throws NoUserIdException - cannot find the user requested
	 * @throws CrossSiteRequestForgeryException - Unable to guarantee no CSRF
	 */
	private String getOauth2InternalRefCode(
			final IOAuth2Authenticator oauthProvider,
			final HttpServletRequest request) 
		throws AuthenticationCodeException,
			IOException, CodeExchangeException, NoUserIdException,
			CrossSiteRequestForgeryException {
		// verify there is no cross site request forgery going on.
		if (request.getQueryString() == null || !ensureNoCSRF(request)) {
			throw new CrossSiteRequestForgeryException("CRSF check failed");
		}

		// this will have our authorization code within it.
		StringBuffer fullUrlBuf = request.getRequestURL();
		fullUrlBuf.append('?').append(request.getQueryString());

		// extract auth code from string buffer
		String authCode = oauthProvider.extractAuthCode(fullUrlBuf.toString());

		if (authCode != null) {
			String internalReference = oauthProvider.exchangeCode(authCode);
			return internalReference;
		} else {
			throw new AuthenticationCodeException(
					"User denied access to our app.");
		}
	}
	
	/**
	 * Helper method to store a redirect url for a user going through external authentication. 
	 * @param request - the request to store the session variable in.
	 * @param url - the url that the user wishes to be redirected to.
	 */
	private void storeRedirectUrl(final HttpServletRequest request, final String url) {
		request.getSession().setAttribute(Constants.REDIRECT_URL_PARAM_NAME,
				url);
	}
	
	/**
	 * Helper method to retrieve the users redirect url from their session.
	 * 
	 * @param request - the request where the redirect url is stored (session variable).
	 * @return the URI containing the users desired uri. If URL is null then returns /
	 * @throws URISyntaxException - if the session retrieved is an invalid URI.
	 */
	private URI loadRedirectUrl(final HttpServletRequest request) throws URISyntaxException {
		String url = (String) request.getSession().getAttribute(Constants.REDIRECT_URL_PARAM_NAME);
		request.getSession().removeAttribute(Constants.REDIRECT_URL_PARAM_NAME);
		if (null == url) {
			return new URI("/");
		}
		
		return new URI(url);
	}
}
