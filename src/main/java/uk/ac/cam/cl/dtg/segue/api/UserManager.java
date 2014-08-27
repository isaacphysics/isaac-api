package uk.ac.cam.cl.dtg.segue.api;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IPasswordAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth1Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuthAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.OAuth1Token;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.ICommunicator;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;

/**
 * This class is responsible for all low level user management actions e.g.
 * authentication and registration. 
 */
public class UserManager {
	private static final Logger log = LoggerFactory
			.getLogger(UserManager.class);

	private static final String HMAC_SHA_ALGORITHM = "HmacSHA256";

	// TODO: inject property loader instead
	private final String HOST_NAME;

	private final IUserDataManager database;
	// TODO: inject property loader instead
	private final String hmacKey;
	private final Map<AuthenticationProvider, IAuthenticator> registeredAuthProviders;
	private final ICommunicator communicator;
	
	private final MapperFacade dtoMapper;

	/**
	 * Create an instance of the user manager class.
	 * 
	 * @param database
	 *            - an IUserDataManager that will support persistence.
	 * @param hmacSalt
	 *            - A cryptographically random HMAC value to be used as part of session authentication.
	 * @param providersToRegister
	 *            - A map of known authentication providers.
	 * @param dtoMapper
	 *            - the preconfigured DO to DTO object mapper for user objects.            
	 */
	@Inject
	public UserManager(final IUserDataManager database, @Named(Constants.HMAC_SALT) final String hmacSalt,
			final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
			final MapperFacade dtoMapper, @Named(Constants.HOST_NAME) final String hostName,
			final ICommunicator communicator) {
		Validate.notNull(database);
		Validate.notNull(hmacSalt);
		Validate.notNull(providersToRegister);
		Validate.notNull(dtoMapper);
		Validate.notNull(hostName);
		Validate.notNull(communicator);

		this.database = database;
		this.hmacKey = hmacSalt;
		this.registeredAuthProviders = providersToRegister;
		this.dtoMapper = dtoMapper;
		this.HOST_NAME = hostName;
		this.communicator = communicator;
	}

	/**
	 * This method will start the authentication process and ultimately provide
	 * a url for the client to redirect the user to. This url will be for a 3rd party
	 * authenticator who will use the callback method provided after they have
	 * authenticated.
	 * 
	 * Users who are already logged already will be returned their UserDTO without 
	 * going through the authentication process.
	 * 
	 * @param request
	 *            - http request that we can attach the session to and save
	 *            redirect url in.
	 * @param provider
	 *            - the provider the user wishes to authenticate with.
	 * @return A response containing either an object containing a
	 *         redirect URI to the authentication provider if authorization /
	 *         login is required or the user object if the user is already logged in.
	 */
	public final Response authenticate(final HttpServletRequest request, final String provider) {
		if (!this.isRegisteredUserLoggedIn(request)) {
			// this is the expected case so we can
			// start the authenticationFlow.
			return this.initiateAuthenticationFlow(request, provider);
		} else {
			try {
				// if they are already logged in then we do not want to proceed with
				// this authentication flow. We can just return the user object.
				return Response.ok(this.getCurrentRegisteredUser(request)).build();
			} catch (NoUserLoggedInException e1) {
				// this should never happen due to the check above.
				log.error("Unable to verify session information after initial check - this should not happen.");
				return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
						"Unable to validate session.")
						.toResponse();
			}			
		}
	}
	
	/**
	 * This method will start the authentication process for linking a user to a
	 * 3rd party provider. It will ultimately provide a url for the client to
	 * redirect the user to. This url will be for a 3rd party authenticator who
	 * will use the callback method provided after they have authenticated.
	 * 
	 * Users must already be logged in to use this method otherwise a 401 will
	 * be returned.
	 * 
	 * @param request
	 *            - http request that we can attach the session to.
	 * @param provider
	 *            - the provider the user wishes to authenticate with.
	 * @return A response redirecting the user to their redirect url or a
	 *         redirect URI to the authentication provider if authorization /
	 *         login is required. Alternatively a SegueErrorResponse could be
	 *         returned.
	 */
	public final Response initiateLinkAccountToUserFlow(final HttpServletRequest request,
			final String provider) {
		// The user must be logged in to be able to link accounts.
		if (!this.isRegisteredUserLoggedIn(request)) {
			return new SegueErrorResponse(Status.UNAUTHORIZED,
					"You need to be logged in to link accounts.")
					.toResponse();
		}
		
		return this.initiateAuthenticationFlow(request, provider);
	}
	
	/**
	 * Authenticate Callback will receive the authentication information from
	 * the different provider types. (e.g. OAuth 2.0 (IOAuth2Authenticator) or
	 * bespoke)
	 * 
	 * This method will either register a new user and attach the linkedAccount
	 * or locate the existing account of the user and create a session for that.
	 * 
	 * @param request
	 *            - http request from the user - should contain url encoded token details.
	 * @param provider
	 *            - the provider who has just authenticated the user.
	 * @return Response containing the user object.
	 *         Alternatively a SegueErrorResponse could be returned.
	 */
	public final Response authenticateCallback(
			final HttpServletRequest request, final String provider) {
		
		try {
			IAuthenticator authenticator = mapToProvider(provider);
			
			IFederatedAuthenticator federatedAuthenticator;
			if (authenticator instanceof IFederatedAuthenticator) {
				federatedAuthenticator = (IFederatedAuthenticator) authenticator;
			} else {
				return new SegueErrorResponse(Status.BAD_REQUEST,
						"The authenticator requested does not have a callback function.")
						.toResponse();
			}
			
			// this is a reference that the segue provider can use to look up user details.
			String providerSpecificUserLookupReference = null;

			// if we are an OAuth2Provider complete next steps of oauth
			if (federatedAuthenticator instanceof IOAuthAuthenticator) {
				IOAuthAuthenticator oauthProvider = (IOAuthAuthenticator) federatedAuthenticator;

				providerSpecificUserLookupReference = this
						.getOauthInternalRefCode(oauthProvider, request);
			} else {
				// This should catch any invalid providers
				SegueErrorResponse error = new SegueErrorResponse(
						Status.INTERNAL_SERVER_ERROR,
						"Unable to map to a known authenticator. The provider: "
								+ provider + " is unknown");
				
				log.error(error.getErrorMessage());
				return error.toResponse();
			}
			
			// If the user is currently logged in this must be a
			// request to link accounts
			RegisteredUser currentUser = getCurrentUserDO(request);
			
			// if we are already logged in - check if we have already got this
			// provider assigned already? If not this is probably a link request.
			if (null != currentUser) {
				List<AuthenticationProvider> usersProviders = this.database
						.getAuthenticationProvidersByUser(currentUser);

				if (null != usersProviders
						&& usersProviders.contains(authenticator.getAuthenticationProvider())) {
					// they are already connected to this provider just return the user object
					return Response.ok(currentUser).build();
				} else {
					// Decide if this is a link operation or an authenticate / register
					// operation.
					log.info("Linking existing user to another provider account.");
					this.linkProviderToExistingAccount(currentUser, federatedAuthenticator,
							providerSpecificUserLookupReference);
					return Response.ok(this.convertUserDOToUserDTO(this.getCurrentUserDO(request))).build();
				}
			}
			
			RegisteredUser segueUserDO = this.getUserFromFederatedProvider(federatedAuthenticator,
					providerSpecificUserLookupReference);
			RegisteredUserDTO segueUserDTO = null;
			// decide if this is a registration or an existing user.
			if (null == segueUserDO) {
				// new user
				segueUserDO = this.registerUserWithFederatedProvider(federatedAuthenticator,
						providerSpecificUserLookupReference);
				segueUserDTO = this.convertUserDOToUserDTO(segueUserDO);
				segueUserDTO.setFirstLogin(true);
			} else {
				// existing user
				segueUserDTO = this.convertUserDOToUserDTO(segueUserDO);
			}

			// create a signed session for this user so that we don't need
			// to do this again for a while.
			this.createSession(request, segueUserDO.getDbId());
			return Response.ok(segueUserDTO).build();

		} catch (IOException e) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Exception while trying to authenticate a user"
							+ " - during callback step. IO problem.", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (NoUserException e) {
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
			log.info("Error detected during authentication: " + e.getClass().toString(), e);
			return error.toResponse();
		} catch (DuplicateAccountException e) { 
			log.info("Duplicate user already exists in the database.", e);
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"A user already exists with the e-mail address specified.")
					.toResponse();
		} catch (SegueDatabaseException e) { 
			log.error("Internal Database error during authentication", e);
			return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"Internal database error during authentication.")
					.toResponse();
		} catch (AuthenticationProviderMappingException e) {
			return new SegueErrorResponse(Status.BAD_REQUEST,
					"Unable to map to a known authenticator. The provider: " + provider + " is unknown")
					.toResponse();
		}
	}
	
	/**
	 * This method will attempt to authenticate the user using the provided
	 * credentials and if successful will provide a redirect response to the
	 * user based on the redirectUrl provided.
	 * 
	 * @param request
	 *            - http request that we can attach the session to.
	 * @param provider
	 *            - the provider the user wishes to authenticate with.
	 * @param credentials
	 *            - Credentials email and password credentials should be
	 *            specified in a map
	 * @return A response containing the UserDTO object or a SegueErrorResponse.
	 */
	public final Response authenticateWithCredentials(final HttpServletRequest request,
			final String provider,
			@Nullable final Map<String, String> credentials) {

		// in this case we expect a username and password to have been
		// sent in the json response.
		if (null == credentials
				|| credentials.get(Constants.LOCAL_AUTH_EMAIL_FIELDNAME) == null
				|| credentials.get(Constants.LOCAL_AUTH_PASSWORD_FIELDNAME) == null) {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.BAD_REQUEST,
					"You must specify credentials email and password to use this authentication provider.");
			return error.toResponse();
		}

		// get the current user based on their session id information.
		RegisteredUserDTO currentUser = this.convertUserDOToUserDTO(this.getCurrentUserDO(request));
		if (null != currentUser) {
			return Response.ok(currentUser).build();
		}

		IAuthenticator authenticator;
		try {
			authenticator = mapToProvider(provider);
			
		} catch (AuthenticationProviderMappingException e) {
			String errorMsg = "Unable to locate the provider specified";
			log.error(errorMsg, e);
			return new SegueErrorResponse(Status.BAD_REQUEST,
					errorMsg).toResponse();
		}
		
		if (authenticator instanceof IPasswordAuthenticator) {
			IPasswordAuthenticator passwordAuthenticator = (IPasswordAuthenticator) authenticator;

			try {
				RegisteredUser user = passwordAuthenticator.authenticate(credentials
						.get(Constants.LOCAL_AUTH_EMAIL_FIELDNAME), credentials
						.get(Constants.LOCAL_AUTH_PASSWORD_FIELDNAME));

				this.createSession(request, user.getDbId());
				
				return Response.ok(this.convertUserDOToUserDTO(user)).build();

			} catch (IncorrectCredentialsProvidedException | NoUserException
					| NoCredentialsAvailableException e) {
				log.debug("Incorrect Credentials Received", e);
				return new SegueErrorResponse(Status.UNAUTHORIZED,
						"Incorrect credentials provided.").toResponse();
			} catch (SegueDatabaseException e) {
				String errorMsg = "Internal Database error has occurred during authentication.";
				log.error(errorMsg, e);
				return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
						errorMsg).toResponse();
			}
		} else {
			SegueErrorResponse error = new SegueErrorResponse(
					Status.INTERNAL_SERVER_ERROR,
					"Unable to map to a known authenticator that accepts "
							+ "raw credentials for the given provider: "
							+ provider);
			log.error(error.getErrorMessage());
			return error.toResponse();
		}
	}	
	
	/**
	 * Unlink User From AuthenticationProvider
	 * 
	 * Removes the link between a user and a provider.
	 * 
	 * @param user
	 *            - user to affect.
	 * @param providerString
	 *            - provider to unassociated.
	 * @throws SegueDatabaseException
	 *             - if there is an error during the database update.
	 * @throws MissingRequiredFieldException
	 *             - If the change will mean that the user will be unable to
	 *             login again.
	 * @throws AuthenticationProviderMappingException
	 *             - if we are unable to locate the authentication provider
	 *             specified.
	 */
	public void unlinkUserFromProvider(final RegisteredUserDTO user, final String providerString)
		throws SegueDatabaseException, MissingRequiredFieldException, AuthenticationProviderMappingException {
		RegisteredUser userDO = this.findUserById(user.getDbId());
		
		// make sure that the change doesn't prevent the user from logging in again.
		if ((this.database.getAuthenticationProvidersByUser(userDO).size() > 1)
				|| userDO.getPassword() != null) {
			this.database.unlinkAuthProviderFromUser(userDO, this.mapToProvider(providerString)
					.getAuthenticationProvider());			
		} else {
			throw new MissingRequiredFieldException(
					"This modification would mean that the user"
							+ " no longer has a way of authenticating. Failing change.");
		}
	}
	
	/**
	 * Is the current user an admin.
	 * 
	 * @param request - with session information
	 * @return true if user is logged in as an admin, false otherwise.
	 * @throws NoUserLoggedInException - if we are unable to tell because they are not logged in.
	 */
	public final boolean isUserAnAdmin(final HttpServletRequest request) throws NoUserLoggedInException {
		RegisteredUser user = this.getCurrentUserDO(request);
		
		if (null == user) {
			throw new NoUserLoggedInException();
		}
		
		if (user.getRole() != null && user.getRole().equals(Role.ADMIN)) {
			return true;
		} 
		
		return false;
	}
	
	/**
	 * Determine if there is a user logged in with a valid session.
	 * 
	 * @param request
	 *            - to retrieve session information from
	 * @return True if the user is logged in and the session is valid, false if not.
	 */
	public final boolean isRegisteredUserLoggedIn(final HttpServletRequest request) {
		try {
			return this.getCurrentRegisteredUser(request) != null;
		} catch (NoUserLoggedInException e) {
			return false;
		}
	}
	
	/**
	 * Get the details of the currently logged in registered user.
	 * 
	 * This method will validate the session and will throw a NoUserLoggedInException if invalid.
	 * 
	 * @param request
	 *            - to retrieve session information from
	 * @return Returns the current UserDTO if we can get it or null if user is
	 *         not currently logged in
	 * @throws NoUserLoggedInException - When the session has expired or there is no user currently logged in.
	 */
	public final RegisteredUserDTO getCurrentRegisteredUser(final HttpServletRequest request)
		throws NoUserLoggedInException {
		Validate.notNull(request);

		RegisteredUser user = this.getCurrentUserDO(request);
		
		if (null == user) {
			throw new NoUserLoggedInException();
		}
		
		return this.convertUserDOToUserDTO(user);
	}
	
	/**
	 * This method will return either an AnonymousUserDTO or a RegisteredUserDTO
	 * 
	 * If the user is currently logged in you will get a RegisteredUserDTO otherwise you
	 * will get an AnonymousUserDTO containing a sessionIdentifier and any questionAttempts made
	 * by the anonymous user.
	 * 
	 * @param request
	 *            - containing session information.
	 * 
	 * @return AbstractSegueUserDTO - Either a RegisteredUser or an AnonymousUser
	 */
	public final AbstractSegueUserDTO getCurrentUser(final HttpServletRequest request) {
		try {
			return this.getCurrentRegisteredUser(request);
		} catch (NoUserLoggedInException e) {
			return this.getAnonymousUser(request);
		}
	}
	
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

		String currentDate = new Date().toString();
		String sessionId = request.getSession().getId();
		String sessionHMAC = this.calculateSessionHMAC(hmacKey, userId, sessionId, currentDate);

		request.getSession().setAttribute(Constants.SESSION_USER_ID, userId);
		request.getSession().setAttribute(Constants.SESSION_ID, sessionId);
		request.getSession().setAttribute(Constants.DATE_SIGNED, currentDate);
		request.getSession().setAttribute(Constants.HMAC, sessionHMAC);
		
		// merge any anonymous information collected with this user.
		try {
			this.mergeAnonymousQuestionInformationWithUserRecord(request);
		} catch (NoUserLoggedInException | SegueDatabaseException e) {
			log.error("Unable to merge anonymously collected data with stored user object.", e);
		}
	}

	/**
	 * Record that someone has answered a question.
	 * 
	 * If the user is anonymous the question record will be added as a temporary session variable. 
	 * This will enable merging later if the user registers.
	 * 
	 * @param user
	 *            - containing either a registered or anonymous user.
	 * @param questionResponse
	 *            - question results.
	 */
	public final void recordQuestionAttempt(final AbstractSegueUserDTO user,
			final QuestionValidationResponseDTO questionResponse) {
		
		// We are operating against the convention that the first component of
		// an id is the question page
		// and that the id separator is |
		String[] questionPageId = questionResponse.getQuestionId().split(
				Constants.ESCAPED_ID_SEPARATOR);

		if (user instanceof RegisteredUserDTO) {
			RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;
			try {
				this.database.registerQuestionAttempt(registeredUser.getDbId(), questionPageId[0],
						questionResponse.getQuestionId(), questionResponse);
				log.info("Question information recorded for user: " + registeredUser.getDbId());
			} catch (SegueDatabaseException e) {
				log.error("Unable to to record question attempt.", e);
			}
		} else if (user instanceof AnonymousUserDTO) {
			AnonymousUserDTO anonymousUser = (AnonymousUserDTO) user;
			
			// record this as an anonymous question
			this.recordAnonymousUserQuestionInformation(anonymousUser, questionPageId[0],
					questionResponse.getQuestionId(), questionResponse);
		} else {
			log.error("Unexpected user type. Unable to record question response");
		}
	}
	
	/**
	 * getQuestionAttemptsByUser. This method will return all of the question
	 * attempts for a given user as a map.
	 * 
	 * @param user
	 *            - with the session information included.
	 * @return map of question attempts (QuestionPageId -> QuestionID ->
	 *         [QuestionValidationResponse] or an empty map.
	 * @throws SegueDatabaseException
	 *             - if there is a database error.
	 */
	public final Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsByUser(
			final AbstractSegueUserDTO user) throws SegueDatabaseException {
		Validate.notNull(user);
		
		if (user instanceof RegisteredUserDTO) {
			RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;
			return this.database.getQuestionAttempts(registeredUser.getDbId()).getQuestionAttempts();	
		} else {
			AnonymousUserDTO anonymousUser = (AnonymousUserDTO) user;
			// since no user is logged in assume that we want to use any anonymous attempts			
			return anonymousUser.getTemporaryQuestionAttempts();
		}
	}

	/**
	 * Method to update a user object in our database.
	 * 
	 * @param user
	 *            - the user to update.
	 * @throws InvalidPasswordException
	 *             - the password provided does not meet our requirements.
	 * @throws MissingRequiredFieldException
	 *             - A required field is missing for the user object so cannot
	 *             be saved.
	 * @return the user object as was saved.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 * @throws AuthenticationProviderMappingException
	 *             - if there is a problem locating the authentication provider.
	 *             This only applies for changing a password.
	 */
	public RegisteredUserDTO createOrUpdateUserObject(final RegisteredUser user)
		throws InvalidPasswordException,
			MissingRequiredFieldException, SegueDatabaseException, AuthenticationProviderMappingException {
		RegisteredUser userToSave = null;

		MapperFacade mapper = this.dtoMapper;

		// We want to map to DTO first to make sure that the user cannot
		// change fields that aren't exposed to them
		RegisteredUserDTO userDTOContainingUpdates = mapper.map(user, RegisteredUserDTO.class);

		if (user.getDbId() != null && !user.getDbId().isEmpty()) {
			// This is an update operation.
			RegisteredUser existingUser = this.findUserById(user.getDbId());

			userToSave = existingUser;

			MapperFacade mergeMapper = new DefaultMapperFactory.Builder()
            	.mapNulls(false).build().getMapperFacade();

			mergeMapper.map(userDTOContainingUpdates, userToSave);
		} else {
			// This is a new registration
			userToSave = mapper.map(userDTOContainingUpdates, RegisteredUser.class);
		}

		// do we need to do local password storage using the segue
		// authenticator? I.e. is the password changing?
		if (null != user.getPassword() && !user.getPassword().isEmpty()) {
			IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this
					.mapToProvider(AuthenticationProvider.SEGUE.name());
			String plainTextPassword = user.getPassword();
			
			// clear reference to plainTextPassword
			user.setPassword(null);

			// set the new password on the object to be saved.
			authenticator.setOrChangeUsersPassword(userToSave, plainTextPassword);
		}

		// Before save we should validate the user for mandatory fields.
		if (!this.isUserValid(userToSave)) {
			throw new MissingRequiredFieldException(
					"The user provided is missing a mandatory field");
		} else if (!this.database.hasALinkedAccount(userToSave)
				&& userToSave.getPassword() == null) {
			// a user must have a way of logging on.
			throw new MissingRequiredFieldException(
					"This modification would mean that the user"
							+ " no longer has a way of authenticating. Failing change.");
		} else {
			
			RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);
			return this.convertUserDOToUserDTO(userToReturn);
		}
	}

	/**
	 * This method will use an email address to check a local user exists and if so, will send
	 * an email with a unique token to allow a password reset. This method does not indicate
	 * whether or not the email actually existed.
	 *
	 * @param userObject - A user object containing the email address of the user to reset the password for.
	 * @throws NoSuchAlgorithmException - if the configured algorithm is not valid.
	 * @throws InvalidKeySpecException  - if the preconfigured key spec is invalid.
	 * @throws CommunicationException - if a fault occurred whilst sending the communique
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	public final void resetPasswordRequest(final RegisteredUserDTO userObject) throws InvalidKeySpecException,
			NoSuchAlgorithmException, CommunicationException, SegueDatabaseException {
		RegisteredUser user = this.findUserByEmail(userObject.getEmail());

		if (user == null) {
			// Email address does not exist in the DB
			// Fail silently
			return;
		}

		if (this.database.hasALinkedAccount(user) && (user.getPassword() == null || user.getPassword().isEmpty())) {
			// User is not authenticated locally
			this.sendFederatedAuthenticatorResetMessage(user);
			return;
		}

		// User is valid and authenticated locally, proceed with reset
		// Generate token
		IPasswordAuthenticator authenticator =
				(IPasswordAuthenticator) this.registeredAuthProviders.get(AuthenticationProvider.SEGUE);
		
		user = authenticator.createPasswordResetTokenForUser(user);

		// Save user object
		this.database.createOrUpdateUser(user);

		log.info(String.format("Sending password reset message to %s", user.getEmail()));
		this.sendPasswordResetMessage(user);
	}

	/**
	 * This method will test if the specified token is a valid password reset token.
	 * 
	 * 
	 * @param token - The token to test
	 * @return true if the reset token is valid
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	public final boolean validatePasswordResetToken(final String token) throws SegueDatabaseException {
		// Set user's password
		IPasswordAuthenticator authenticator =
				(IPasswordAuthenticator) this.registeredAuthProviders.get(AuthenticationProvider.SEGUE);
		
		return authenticator.isValidResetToken(this.findUserByResetToken(token));
	}
	
	/**
	 * This method will use a unique password reset token to set a new password.
	 *
	 * @param token - the password reset token
	 * @param userObject - the supplied user DO
	 * @throws InvalidTokenException - If the token provided is invalid.
	 * @throws InvalidPasswordException - If the password provided is invalid.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	public final void resetPassword(final String token, final RegisteredUser userObject) throws InvalidTokenException,
			InvalidPasswordException, SegueDatabaseException {
		// Ensure new password is valid
		if (userObject.getPassword() == null || userObject.getPassword().isEmpty()) {
			throw new InvalidPasswordException("Empty passwords are not allowed if using local authentication.");
		}

		IPasswordAuthenticator authenticator =
				(IPasswordAuthenticator) this.registeredAuthProviders.get(AuthenticationProvider.SEGUE);
		
		// Ensure reset token is valid
		RegisteredUser user = this.findUserByResetToken(token);
		if (!authenticator.isValidResetToken(user)) {
			throw new InvalidTokenException();
		}

		// Set user's password
		authenticator.setOrChangeUsersPassword(user, userObject.getPassword());

		// clear plainTextPassword
		userObject.setPassword(null);
		
		// Nullify reset token
		user.setResetToken(null);
		user.setResetExpiry(null);

		// Save user
		this.database.createOrUpdateUser(user);
	}

	/**
	 * Library method that allows the api to locate a user object from the
	 * database based on a given unique id.
	 *
	 * @param userId
	 *            - to search for.
	 * @return user or null if we cannot find it.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private RegisteredUser findUserById(final String userId) throws SegueDatabaseException {
		if (null == userId) {
			return null;
		}
		return this.database.getById(userId);
	}
	
	/**
	 * Library method that allows the api to locate a user object from the
	 * database based on a given unique email address.
	 *
	 * @param email
	 *            - to search for.
	 * @return user or null if we cannot find it.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private RegisteredUser findUserByEmail(final String email) throws SegueDatabaseException {
		if (null == email) {
			return null;
		}
		return this.database.getByEmail(email);
	}
	
	/**
	 * Library method that allows the api to locate a user object from the
	 * database based on a given unique password reset token.
	 *
	 * @param token
	 *            - to search for.
	 * @return user or null if we cannot find it.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private RegisteredUser findUserByResetToken(final String token) throws SegueDatabaseException {
		if (null == token) {
			return null;
		}
		return this.database.getByResetToken(token);
	}	
	
	/**
	 * This method will trigger the authentication flow for a 3rd party
	 * authenticator.
	 * 
	 * This method can be used for regular logins, new registrations or for
	 * linking 3rd party authenticators to an existing Segue user account.
	 * 
	 * @param request
	 *            - http request that we can attach the session to and that
	 *            already has a redirect url attached.
	 * @param provider
	 *            - the provider the user wishes to authenticate with.
	 * @return A json response containing a URI to the authentication provider if authorization /
	 *         login is required. Alternatively a SegueErrorResponse could be returned.
	 */
	private Response initiateAuthenticationFlow(final HttpServletRequest request,
			final String provider) {
		try {
			IAuthenticator federatedAuthenticator = mapToProvider(provider);

			// if we are an OAuthProvider redirect to the provider
			// authorization url.
			URI redirectLink = null;
			if (federatedAuthenticator instanceof IOAuth2Authenticator) {
				IOAuth2Authenticator oauth2Provider = (IOAuth2Authenticator) federatedAuthenticator;
				String antiForgeryTokenFromProvider = oauth2Provider.getAntiForgeryStateToken();

				// Store antiForgeryToken in the users session.
				request.getSession().setAttribute(Constants.STATE_PARAM_NAME, antiForgeryTokenFromProvider);

				redirectLink = URI.create(oauth2Provider.getAuthorizationUrl(antiForgeryTokenFromProvider));
			} else if (federatedAuthenticator instanceof IOAuth1Authenticator) {
				IOAuth1Authenticator oauth1Provider = (IOAuth1Authenticator) federatedAuthenticator;
				OAuth1Token token = oauth1Provider.getRequestToken();

				// Store token and secret in the users session.
				request.getSession().setAttribute(Constants.OAUTH_TOKEN_PARAM_NAME, token.getToken());

				redirectLink = URI.create(oauth1Provider.getAuthorizationUrl(token));
			} else {
				SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
						"Unable to map to a known authenticator. The provider: " + provider + " is unknown");
				log.error(error.getErrorMessage());
				return error.toResponse();
			}
			
			Map<String, URI> redirectResponse = new ImmutableMap.Builder<String, URI>().put(Constants.REDIRECT_URL,
					redirectLink).build();
			
			return Response.ok(redirectResponse).build();

		} catch (IOException e) {
			SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
					"IOException when trying to redirect to OAuth provider", e);
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		} catch (AuthenticationProviderMappingException e) {
			SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
					"Error mapping to a known authenticator. The provider: " + provider + " is unknown");
			log.error(error.getErrorMessage(), e);
			return error.toResponse();
		}
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
	private boolean isValidUsersSession(final HttpServletRequest request) {
		Validate.notNull(request);

		String userId = (String) request.getSession().getAttribute(
				Constants.SESSION_USER_ID);
		String currentDate = (String) request.getSession().getAttribute(
				Constants.DATE_SIGNED);
		String sessionId = (String) request.getSession().getAttribute(
				Constants.SESSION_ID);
		String sessionHMAC = (String) request.getSession().getAttribute(
				Constants.HMAC);

		String ourHMAC = this.calculateSessionHMAC(hmacKey, userId, sessionId, currentDate);

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
	 * Calculate the session HMAC value based on the properties of interest.
	 * @param key - secret key.
	 * @param userId - User Id
	 * @param sessionId - Session Id
	 * @param currentDate - Current date
	 * @return HMAC signature.
	 */
	private String calculateSessionHMAC(final String key, final String userId, final String sessionId,
			final String currentDate) {
		return this.calculateHMAC(key, userId + "|" + sessionId + "|" + currentDate);
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
	 * @throws AuthenticationProviderMappingException 
	 */
	private IAuthenticator mapToProvider(final String provider) throws AuthenticationProviderMappingException {
		Validate.notEmpty(provider,
				"Provider name must not be empty or null if we are going "
						+ "to map it to an implementation.");

		AuthenticationProvider enumProvider = null;
		try {
			enumProvider = AuthenticationProvider.valueOf(provider
					.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new AuthenticationProviderMappingException("The provider requested is "
					+ "invalid and not a known AuthenticationProvider: "
					+ provider);
		}

		if (!registeredAuthProviders.containsKey(enumProvider)) {
			throw new AuthenticationProviderMappingException("This authentication provider"
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
	 * @param oauthProvider
	 *            -
	 * @return true if we are happy , false if we think a violation has
	 *         occurred.
	 * @throws CrossSiteRequestForgeryException
	 *             - if we suspect cross site request forgery.
	 */
	private boolean ensureNoCSRF(final HttpServletRequest request,
			final IOAuthAuthenticator oauthProvider)
		throws CrossSiteRequestForgeryException {
		Validate.notNull(request);

		String key;
		if (oauthProvider instanceof IOAuth2Authenticator) {
			key = Constants.STATE_PARAM_NAME;
		} else if (oauthProvider instanceof IOAuth1Authenticator) {
			key = Constants.OAUTH_TOKEN_PARAM_NAME;
		} else {
			throw new CrossSiteRequestForgeryException(
					"Provider not recognized.");
		}

		// to deal with cross site request forgery
		String csrfTokenFromUser = (String) request.getSession().getAttribute(
				key);
		String csrfTokenFromProvider = request.getParameter(key);

		if (null == csrfTokenFromUser || null == csrfTokenFromProvider
				|| !csrfTokenFromUser.equals(csrfTokenFromProvider)) {
			log.error("Invalid state parameter - Provider said: "
					+ request.getParameter(Constants.STATE_PARAM_NAME)
					+ " Session said: "
					+ request.getSession().getAttribute(
							Constants.STATE_PARAM_NAME));
			return false;
		} else {
			log.debug("State parameter matches - Provider said: "
					+ request.getParameter(Constants.STATE_PARAM_NAME)
					+ " Session said: "
					+ request.getSession().getAttribute(
							Constants.STATE_PARAM_NAME));
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
	 * @throws DuplicateAccountException
	 *             - If there is an account that already exists in the system
	 *             with matching indexed fields.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private String registerUser(final RegisteredUser user,
			final AuthenticationProvider provider,
			final String providerUserId) throws DuplicateAccountException, SegueDatabaseException {
		String userId = database.registerNewUserWithProvider(user, provider, providerUserId);
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
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private RegisteredUser getUserFromLinkedAccount(
			final AuthenticationProvider provider, final String providerId) throws SegueDatabaseException {
		Validate.notNull(provider);
		Validate.notBlank(providerId);

		RegisteredUser user = database.getByLinkedAccount(provider, providerId);
		if (null == user) {
			log.info("Unable to locate user based on provider "
					+ "information provided.");
		}
		return user;
	}
	
	/**
	 * Gets an existing Segue user from a 3rd party authenticator.
	 * 
	 * @param federatedAuthenticator
	 *            the federatedAuthenticator we are using for authentication
	 * @param providerSpecificUserLookupReference
	 *            - the look up reference provided by the authenticator after
	 *            any authenticator specific actions have been completed.
	 * @return a Segue UserDO that exists in the segue database.
	 * @throws AuthenticatorSecurityException
	 *             - error with authenticator.
	 * @throws NoUserException
	 *             - If we are unable to locate the user id based on the lookup
	 *             reference provided.
	 * @throws IOException
	 *             - if there is an io error.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private RegisteredUser getUserFromFederatedProvider(final IFederatedAuthenticator federatedAuthenticator,
			final String providerSpecificUserLookupReference) throws SegueDatabaseException, NoUserException,
			IOException, AuthenticatorSecurityException {
		UserFromAuthProvider userFromProvider = federatedAuthenticator
				.getUserInfo(providerSpecificUserLookupReference);

		if (null == userFromProvider) {
			log.warn("Unable to create user for the provider "
					+ federatedAuthenticator.getAuthenticationProvider().name());
			throw new NoUserException();
		}

		log.debug("User with name " + userFromProvider.getEmail() + " retrieved");

		return this.getUserFromLinkedAccount(federatedAuthenticator.getAuthenticationProvider(),
				userFromProvider.getProviderUserId());
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
	 * @return a Segue UserDO that exists in the segue database.
	 * @throws AuthenticatorSecurityException
	 *             - error with authenticator.
	 * @throws NoUserException
	 *             - If we are unable to locate the user id based on the lookup
	 *             reference provided.
	 * @throws IOException
	 *             - if there is an io error.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private RegisteredUser registerUserWithFederatedProvider(
			final IFederatedAuthenticator federatedAuthenticator,
			final String providerSpecificUserLookupReference)
		throws AuthenticatorSecurityException, NoUserException,
			IOException, SegueDatabaseException {		
		// get user info from federated provider
		RegisteredUser localUserInformation = this.getUserFromFederatedProvider(federatedAuthenticator,
				providerSpecificUserLookupReference);

		// decide if we need to register a new user or link to an existing
		// account
		if (null == localUserInformation) {
			log.info("New registration - User does not already exist.");

			UserFromAuthProvider userFromProvider = federatedAuthenticator
					.getUserInfo(providerSpecificUserLookupReference);

			if (null == userFromProvider) {
				log.warn("Unable to create user for the provider "
						+ federatedAuthenticator.getAuthenticationProvider().name());
				throw new NoUserException();
			}
			
			RegisteredUser newLocalUser = this.dtoMapper.map(userFromProvider, RegisteredUser.class);

			// register user
			String localUserId = registerUser(newLocalUser,
					federatedAuthenticator.getAuthenticationProvider(),
					userFromProvider.getProviderUserId());
			localUserInformation = this.database.getById(localUserId);

			if (null == localUserInformation) {
				// we just put it in so something has gone very wrong.
				log.error("Failed to retreive user even though we "
						+ "just put it in the database.");
				throw new NoUserException();
			}
		} else {
			log.error("Returning user detected"
					+ localUserInformation.getDbId() + " unable to create a new segue user.");
		}

		return localUserInformation;
	}

	/**
	 * Link Provider To Existing Account.
	 * 
	 * @param currentUser
	 *            - the current user to link provider to.
	 * @param federatedAuthenticator
	 *            the federatedAuthenticator we are using for authentication
	 * @param providerSpecificUserLookupReference
	 *            - the look up reference provided by the authenticator after
	 *            any authenticator specific actions have been completed.
	 * 
	 * @throws AuthenticatorSecurityException
	 *             - If a third party authenticator fails a security check.
	 * @throws NoUserException
	 *             - If we are unable to find a user that matches
	 * @throws IOException
	 *             - If there is a problem reading from the data source.
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private void linkProviderToExistingAccount(final RegisteredUser currentUser,
			final IFederatedAuthenticator federatedAuthenticator,
			final String providerSpecificUserLookupReference) throws AuthenticatorSecurityException,
			NoUserException, IOException, SegueDatabaseException {
		Validate.notNull(currentUser);
		Validate.notNull(federatedAuthenticator);
		Validate.notEmpty(providerSpecificUserLookupReference);

		// get user info from federated provider
		UserFromAuthProvider userFromProvider = federatedAuthenticator
				.getUserInfo(providerSpecificUserLookupReference);

		this.database.linkAuthProviderToAccount(currentUser,
				federatedAuthenticator.getAuthenticationProvider(), userFromProvider.getProviderUserId());
		
	}
	
	/**
	 * This method is an oauth2 specific method which will ultimately provide an
	 * internal reference number that the oauth2 provider can use to lookup the
	 * information of the user who has just authenticated.
	 * 
	 * @param oauthProvider
	 *            - The provider to authenticate against.
	 * @param request
	 *            - The request that will contain session information.
	 * @return an internal reference number that will allow retrieval of the
	 *         users information from the provider.
	 * @throws AuthenticationCodeException
	 *             - possible authentication code issues.
	 * @throws IOException
	 *             - error reading from client key?
	 * @throws CodeExchangeException
	 *             - exception whilst exchanging codes
	 * @throws NoUserException
	 *             - cannot find the user requested
	 * @throws CrossSiteRequestForgeryException
	 *             - Unable to guarantee no CSRF
	 */
	private String getOauthInternalRefCode(final IOAuthAuthenticator oauthProvider,
			final HttpServletRequest request) throws AuthenticationCodeException, IOException,
			CodeExchangeException, NoUserException, CrossSiteRequestForgeryException {
		// verify there is no cross site request forgery going on.
		if (request.getQueryString() == null || !ensureNoCSRF(request, oauthProvider)) {
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
			throw new AuthenticationCodeException("User denied access to our app.");
		}
	}

	/**
	 * IsUserValid This function will check that the user object is valid.
	 * 
	 * @param userToValidate
	 *            - the user to validate.
	 * @return true if it meets the internal storage requirements, false if not.
	 */
	private boolean isUserValid(final RegisteredUser userToValidate) {
		boolean isValid = true;

		if (userToValidate.getEmail() == null
				|| userToValidate.getEmail().isEmpty()
				|| !userToValidate.getEmail().contains("@")) {
			isValid = false;
		}

		return isValid;
	}
	
	/**
	 * Converts the sensitive UserDO into a limited DTO.
	 * 
	 * @param user - DO
	 * @return user - DTO
	 */
	private RegisteredUserDTO convertUserDOToUserDTO(final RegisteredUser user) {
		if (null == user) {
			return null;
		}
		
		RegisteredUserDTO userDTO = this.dtoMapper.map(user, RegisteredUserDTO.class);

		// Augment with linked account information
		try {
			userDTO.setLinkedAccounts(this.database.getAuthenticationProvidersByUser(user));
		} catch (SegueDatabaseException e) {
			log.error("Unable to set linked accounts for user due to a database error.");
		}
		
		if (user.getPassword() != null && !user.getPassword().isEmpty()) {
			userDTO.setHasSegueAccount(true);
		} else {
			userDTO.setHasSegueAccount(false);
		}
		
		return userDTO;
	}
	
	/**
	 * Get the RegisteredUserDO of the currently logged in user. This is for internal use only.
	 * 
	 * This method will validate the session as well returning null if it is invalid.
	 * 
	 * @param request
	 *            - to retrieve session information from
	 * @return Returns the current UserDTO if we can get it or null if user is
	 *         not currently logged in / there is an invalid session
	 */
	private RegisteredUser getCurrentUserDO(final HttpServletRequest request) {
		Validate.notNull(request);

		// get the current user based on their session id information.
		String currentUserId = (String) request.getSession().getAttribute(
				Constants.SESSION_USER_ID);

		if (null == currentUserId) {
			log.debug("Current userID is null. Assume they are not logged in.");
			return null;
		}

		// check if the users session is validated using our credentials.
		if (!this.isValidUsersSession(request)) {
			log.info("User session has failed validation. "
					+ "Assume they are not logged in.");
			return null;
		}
		
		// retrieve the user from database.
		try {
			return database.getById(currentUserId);
		} catch (SegueDatabaseException e) {
			log.error("Internal Database error. Failed to resolve current user.", e);
			return null;
		}
	}

	/**
	 * This method will send a message to a user explaining that they only use a
	 * federated authenticator.
	 *
	 * @param user - a user with the givenName, email and token fields set
	 * @throws CommunicationException - if a fault occurred whilst sending the communique
	 * @throws SegueDatabaseException
	 *             - If there is an internal database error.
	 */
	private void sendFederatedAuthenticatorResetMessage(final RegisteredUser user) throws CommunicationException,
			SegueDatabaseException {
		// Get the user's federated authenticators
		List<AuthenticationProvider> providers = this.database.getAuthenticationProvidersByUser(user);
		List<String> providerNames = new ArrayList<>();
		for (AuthenticationProvider provider : providers) {
			IAuthenticator authenticator = this.registeredAuthProviders.get(provider);
			if (!(authenticator instanceof IFederatedAuthenticator)) {
				continue;
			}

			String providerName = provider.name().toLowerCase();
			providerName = providerName.substring(0, 1).toUpperCase() + providerName.substring(1);
			providerNames.add(providerName);
		}

		String providersString;
		if (providerNames.size() == 1) {
			providersString = providerNames.get(0);
		} else {
			StringBuilder providersBuilder = new StringBuilder();
			for (int i = 0; i < providerNames.size(); i++) {
				if (i == providerNames.size() - 1) {
					providersBuilder.append(" and ");
				} else if (i > 1) {
					providersBuilder.append(", ");
				}
				providersBuilder.append(providerNames.get(i));
			}
			providersString = providersBuilder.toString();
		}

		String subject = "Password Reset";
		String providerWord = "provider";
		if (providerNames.size() > 1) {
			providerWord += "s";
		}

		// Construct message
		String message = String.format("You requested a password reset however you use %s to log in to our site. You"
				+ " need go to your authentication %s to reset your password.", providersString, providerWord);

		// Send message
		communicator.sendMessage(user.getEmail(), user.getGivenName(), subject, message);
	}

	/**
	 * This method will send a password reset message to a user.
	 *
	 * @param user - a user with the givenName, email and token fields set
	 * @throws CommunicationException - if a fault occurred whilst sending the communique
	 */
	private void sendPasswordResetMessage(final RegisteredUser user) throws CommunicationException {
		String subject = "Password Reset";

		// Construct message
		String message = String.format("Please follow this link to reset your password: https://%s/resetpassword/%s",
				this.HOST_NAME, user.getResetToken());

		// Send message
		communicator.sendMessage(user.getEmail(), user.getGivenName(), subject, message);
	}
	
	/**
	 * Temporarily Record Anonymous User Question Information in the anonymous
	 * user object provided.
	 * 
	 * @param anonymousUser
	 *            - anonymous user object stored in a session.
	 * @param questionPageId
	 *            - page id to record
	 * @param questionId
	 *            - question id to record
	 * @param questionResponse
	 *            - response to temporarily record.
	 */
	private void recordAnonymousUserQuestionInformation(final AnonymousUserDTO anonymousUser,
			final String questionPageId, final String questionId,
			final QuestionValidationResponseDTO questionResponse) {

		
		Map<String, Map<String, List<QuestionValidationResponse>>> anonymousResponses = 
				anonymousUser.getTemporaryQuestionAttempts();

		if (!anonymousResponses.containsKey(questionPageId)) {
			anonymousResponses.put(questionPageId, new HashMap<String, List<QuestionValidationResponse>>());
		}

		if (!anonymousResponses.get(questionPageId).containsKey(questionId)) {
			anonymousResponses.get(questionPageId).put(questionId,
					new ArrayList<QuestionValidationResponse>());
		}

		QuestionValidationResponse questionResponseDO = this.dtoMapper.map(questionResponse,
				QuestionValidationResponse.class);
		
		// add the response to the session object
		anonymousResponses.get(questionPageId).get(questionId).add(questionResponseDO);

		log.debug("Recording anonymous question attempt in session as user is not logged in.");
	}
	
	/**
	 * Retrieves anonymous user information if it is available. 
	 * @param request - request containing session information.
	 * @return An anonymous user containing any anonymous question attempts (which could be none)
	 */
	private AnonymousUserDTO getAnonymousUser(final HttpServletRequest request) {
		// no session exists so create one.
		if (request.getSession().getAttribute(Constants.ANONYMOUS_USER) == null) {
			AnonymousUserDTO user = new AnonymousUserDTO(request.getSession().getId());

			// add the user reference to the session
			request.getSession().setAttribute(Constants.ANONYMOUS_USER, user);
			
			return user;
		}

		// reuse existing one
		if (request.getSession().getAttribute(Constants.ANONYMOUS_USER) instanceof AnonymousUserDTO) {
			return (AnonymousUserDTO) request.getSession().getAttribute(
					Constants.ANONYMOUS_USER);
		} else {
			// this means that someone has put the wrong type in to the session variable.
			throw new ClassCastException("Unable to get AnonymousUser from session.");			
		}
	}
	
	/**
	 * Merges any question data stored in the session (this will only happen for
	 * anonymous users).
	 * 
	 * @param request
	 *            - the request containing the session information.
	 * @throws NoUserLoggedInException
	 *             - Unable to merge as the user is still anonymous.
	 * @throws SegueDatabaseException
	 *             - if we are unable to locate the questions attempted by this
	 *             user already.
	 */
	private void mergeAnonymousQuestionInformationWithUserRecord(final HttpServletRequest request)
		throws NoUserLoggedInException, SegueDatabaseException {
		
		Map<String, Map<String, List<QuestionValidationResponse>>> anonymouslyAnsweredQuestions = this
				.getAnonymousUser(request).getTemporaryQuestionAttempts();
		
		if (anonymouslyAnsweredQuestions.isEmpty()) {
			return;
		}

		// ensure the user is logged in. This will throw an exception if not.
		RegisteredUserDTO user = this.getCurrentRegisteredUser(request);
		
		log.info("Merging anonymous questions with known user account");
		
		for (String questionPageId : anonymouslyAnsweredQuestions.keySet()) {
			for (String questionId : anonymouslyAnsweredQuestions.get(questionPageId).keySet()) {
				for (QuestionValidationResponse questionResponse : anonymouslyAnsweredQuestions.get(
						questionPageId).get(questionId)) {
					QuestionValidationResponseDTO questionRespnseDTO = this.dtoMapper.map(questionResponse,
							QuestionValidationResponseDTO.class);
					this.recordQuestionAttempt(user, questionRespnseDTO);
				}
			}
		}
		
		// delete the session attribute as merge has completed.
		request.getSession().removeAttribute(Constants.ANONYMOUS_USER);
	}
}
