/**
 * Copyright 2014 Stephen Cummins & Nick Rogers.
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
package uk.ac.cam.cl.dtg.segue.api.managers;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IFederatedAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth1Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuth2Authenticator;
import uk.ac.cam.cl.dtg.segue.auth.IOAuthAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IPasswordAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.OAuth1Token;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidSessionException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserQuestionManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * This class is responsible for all low level user management actions e.g. authentication and registration.
 */
public class UserManager {
    private static final Logger log = LoggerFactory.getLogger(UserManager.class);
    private static final String HMAC_SHA_ALGORITHM = "HmacSHA256";

    private final PropertiesLoader properties;

    private final IUserDataManager database;
    
    private final IUserQuestionManager questionAttemptDb;
    
    private final Cache<String, AnonymousUser> temporaryUserCache;

    private final ILogManager logManager;

    private final Map<AuthenticationProvider, IAuthenticator> registeredAuthProviders;
    private final MapperFacade dtoMapper;

    private final EmailManager emailManager;
    private final ObjectMapper serializationMapper;

    /**
     * Create an instance of the user manager class.
     * 
     * @param database
     *            - an IUserDataManager that will support persistence.
     * @param questionDb
     *            - supports persistence of question attempt info.
     * @param properties
     *            - A property loader
     * @param providersToRegister
     *            - A map of known authentication providers.
     * @param dtoMapper
     *            - the preconfigured DO to DTO object mapper for user objects.
     * @param emailQueue
     *            - the preconfigured communicator manager for sending e-mails.
     * @param logManager
     *            - so that we can log events for users..
     */
    @Inject
    public UserManager(final IUserDataManager database, final IUserQuestionManager questionDb,
            final PropertiesLoader properties, final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
            final MapperFacade dtoMapper, final EmailManager emailQueue, final ILogManager logManager) {
        this(database, questionDb, properties, providersToRegister, dtoMapper, emailQueue, CacheBuilder.newBuilder()
                .expireAfterAccess(ANONYMOUS_SESSION_DURATION_IN_MINUTES, TimeUnit.MINUTES)
                .<String, AnonymousUser> build(), logManager);
    }

    /**
     * Fully injectable constructor.
     * 
     * @param database
     *            - an IUserDataManager that will support persistence.
     * @param questionDb
     *            - supports persistence of question attempt info.
     * @param properties
     *            - A property loader
     * @param providersToRegister
     *            - A map of known authentication providers.
     * @param dtoMapper
     *            - the preconfigured DO to DTO object mapper for user objects.
     * @param emailQueue
     *            - the preconfigured communicator manager for sending e-mails.
     * @param temporaryUserCache
     *            - the preconfigured communicator manager for sending e-mails.
     * @param logManager
     *            - so that we can log events for users..
     */
    public UserManager(final IUserDataManager database, final IUserQuestionManager questionDb,
            final PropertiesLoader properties, final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
            final MapperFacade dtoMapper, final EmailManager emailQueue,
            final Cache<String, AnonymousUser> temporaryUserCache, final ILogManager logManager) {
        Validate.notNull(database);
        Validate.notNull(properties);
        Validate.notNull(providersToRegister);
        Validate.notNull(dtoMapper);
        Validate.notNull(emailQueue);
        Validate.notNull(temporaryUserCache);
        Validate.notNull(logManager);
        Validate.notNull(properties.getProperty(HMAC_SALT));
        Validate.notNull(Integer.parseInt(properties.getProperty(SESSION_EXPIRY_SECONDS)));
        Validate.notNull(properties.getProperty(HOST_NAME));

        this.database = database;
        this.questionAttemptDb = questionDb;
        this.temporaryUserCache = temporaryUserCache;
        this.logManager = logManager;
        this.properties = properties;

        this.registeredAuthProviders = providersToRegister;
        this.dtoMapper = dtoMapper;

        this.emailManager = emailQueue;
        this.serializationMapper = new ObjectMapper();
    }

    /**
     * This method will start the authentication process and ultimately provide a url for the client to redirect the
     * user to. This url will be for a 3rd party authenticator who will use the callback method provided after they have
     * authenticated.
     * 
     * Users who are already logged already will be returned their UserDTO without going through the authentication
     * process.
     * 
     * @param request
     *            - http request that we can attach the session to and save redirect url in.
     * @param provider
     *            - the provider the user wishes to authenticate with.
     * @return a URI for redirection
     * @throws IOException - 
     * @throws AuthenticationProviderMappingException - as per exception description.
     */
    public URI authenticate(final HttpServletRequest request, final String provider) 
            throws IOException, AuthenticationProviderMappingException {
        return this.initiateAuthenticationFlow(request, provider);
    }

    /**
     * This method will start the authentication process for linking a user to a 3rd party provider. It will ultimately
     * provide a url for the client to redirect the user to. This url will be for a 3rd party authenticator who will use
     * the callback method provided after they have authenticated.
     * 
     * Users must already be logged in to use this method otherwise a 401 will be returned.
     * 
     * @param request
     *            - http request that we can attach the session to.
     * @param provider
     *            - the provider the user wishes to authenticate with.
     * @return A redirection URI - also this endpoint ensures that the request has a session attribute on so we know
     *         that this is a link request not a new user.
     * @throws IOException - 
     * @throws AuthenticationProviderMappingException - as per exception description.
     */
    public URI initiateLinkAccountToUserFlow(final HttpServletRequest request, final String provider) 
            throws IOException, AuthenticationProviderMappingException {
        // record our intention to link an account.
        request.getSession().setAttribute(LINK_ACCOUNT_PARAM_NAME, Boolean.TRUE);

        return this.initiateAuthenticationFlow(request, provider);
    }

    /**
     * Authenticate Callback will receive the authentication information from the different provider types. (e.g. OAuth
     * 2.0 (IOAuth2Authenticator) or bespoke)
     * 
     * This method will either register a new user and attach the linkedAccount or locate the existing account of the
     * user and create a session for that.
     * @param request
     *            - http request from the user - should contain url encoded token details.
     * @param response
     *            to store the session in our own segue cookie.
     * @param provider
     *            - the provider who has just authenticated the user.
     * @return Response containing the user object. Alternatively a SegueErrorResponse could be returned.
     * @throws AuthenticationProviderMappingException - if we cannot locate an appropriate authenticator.
     * @throws SegueDatabaseException - if there is a local database error.
     * @throws IOException - Problem reading something
     * @throws NoUserException - If the user doesn't exist with the provider.
     * @throws AuthenticatorSecurityException - If there is a security probably with the authenticator.
     * @throws CrossSiteRequestForgeryException - as per exception description.
     * @throws CodeExchangeException - as per exception description.
     * @throws AuthenticationCodeException - as per exception description.
     */
    public RegisteredUserDTO authenticateCallback(final HttpServletRequest request,
            final HttpServletResponse response, final String provider) throws AuthenticationProviderMappingException,
            AuthenticatorSecurityException, NoUserException, IOException, SegueDatabaseException,
            AuthenticationCodeException, CodeExchangeException, CrossSiteRequestForgeryException {
        IAuthenticator authenticator = mapToProvider(provider);

        IOAuthAuthenticator oauthProvider;

        // this is a reference that the provider can use to look up user details.
        String providerSpecificUserLookupReference = null;

        // if we are an OAuth2Provider complete next steps of oauth
        if (authenticator instanceof IOAuthAuthenticator) {
            oauthProvider = (IOAuthAuthenticator) authenticator;

            providerSpecificUserLookupReference = this.getOauthInternalRefCode(oauthProvider, request);
        } else {
            throw new AuthenticationProviderMappingException("Unable to map to a known authenticator. The provider: "
                    + provider + " is unknown");
        }

        // If the user is currently logged in this must be a
        // request to link accounts
        RegisteredUser currentUser = getCurrentRegisteredUserDO(request);

        // if we are already logged in - check if we have already got this
        // provider assigned already? If not this is probably a link request.
        if (null != currentUser) {
            List<AuthenticationProvider> usersProviders = this.database.getAuthenticationProvidersByUser(currentUser);

            if (null != usersProviders && usersProviders.contains(authenticator.getAuthenticationProvider())) {
                // they are already connected to this provider just return the user object
                return this.convertUserDOToUserDTO(currentUser);
            } else {
                // This extra check is to prevent callbacks to this method from merging accounts unexpectedly
                Boolean intentionToLinkRegistered = (Boolean) request.getSession().getAttribute(
                        LINK_ACCOUNT_PARAM_NAME);
                if (intentionToLinkRegistered == null || !intentionToLinkRegistered) {
                    throw new SegueDatabaseException("User is already authenticated - "
                            + "expected request to link accounts but none was found.");
                }

                // clear link accounts intention until next time
                request.removeAttribute(LINK_ACCOUNT_PARAM_NAME);

                // Decide if this is a link operation or an authenticate / register
                // operation.
                log.debug("Linking existing user to another provider account.");
                this.linkProviderToExistingAccount(currentUser, oauthProvider, providerSpecificUserLookupReference);
                return this.convertUserDOToUserDTO(this.getCurrentRegisteredUserDO(request));
            }
        }

        RegisteredUser segueUserDO = this.getUserFromFederatedProvider(oauthProvider,
                providerSpecificUserLookupReference);
        RegisteredUserDTO segueUserDTO = null;
        // decide if this is a registration or an existing user.
        if (null == segueUserDO) {
            // new user
            segueUserDO = this.registerUserWithFederatedProvider(oauthProvider, providerSpecificUserLookupReference);
            segueUserDTO = this.convertUserDOToUserDTO(segueUserDO);
            segueUserDTO.setFirstLogin(true);
        } else {
            // existing user
            segueUserDTO = this.convertUserDOToUserDTO(segueUserDO);
        }

        // create a signed session for this user so that we don't need
        // to do this again for a while.
        this.createSession(request, response, segueUserDO);
        return segueUserDTO;
    }

    /**
     * This method will attempt to authenticate the user using the provided credentials and if successful will provide a
     * redirect response to the user based on the redirectUrl provided.
     * 
     * @param request
     *            - http request that we can attach the session to.
     * @param response
     *            to store the session in our own segue cookie.
     * @param provider
     *            - the provider the user wishes to authenticate with.
     * @param credentials
     *            - Credentials email and password credentials should be specified in a map
     * @return A response containing the UserDTO object or a SegueErrorResponse.
     * @throws AuthenticationProviderMappingException
     *             - if we cannot find an authenticator
     * @throws IncorrectCredentialsProvidedException
     *             - if the password is incorrect
     * @throws NoUserException
     *             - if the user does not exist
     * @throws NoCredentialsAvailableException
     *             - If the account exists but does not have a local password
     * @throws SegueDatabaseException
     *             - if there is a problem with the database.
     */
    public final RegisteredUserDTO authenticateWithCredentials(final HttpServletRequest request,
            final HttpServletResponse response, final String provider, @Nullable final Map<String, String> credentials)
            throws AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, NoUserException,
            NoCredentialsAvailableException, SegueDatabaseException {
        Validate.notBlank(credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME));
        Validate.notNull(credentials.get(LOCAL_AUTH_PASSWORD_FIELDNAME));

        // get the current user based on their session id information.
        RegisteredUserDTO currentUser = this.convertUserDOToUserDTO(this.getCurrentRegisteredUserDO(request));
        if (null != currentUser) {
            return currentUser;
        }

        IAuthenticator authenticator = mapToProvider(provider);

        if (authenticator instanceof IPasswordAuthenticator) {
            IPasswordAuthenticator passwordAuthenticator = (IPasswordAuthenticator) authenticator;

            RegisteredUser user = passwordAuthenticator.authenticate(credentials.get(LOCAL_AUTH_EMAIL_FIELDNAME),
                    credentials.get(LOCAL_AUTH_PASSWORD_FIELDNAME));
            this.createSession(request, response, user);
            return this.convertUserDOToUserDTO(user);
        } else {
            throw new AuthenticationProviderMappingException("Unable to map to a known authenticator that accepts "
                    + "raw credentials for the given provider: " + provider);
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
     *             - If the change will mean that the user will be unable to login again.
     * @throws AuthenticationProviderMappingException
     *             - if we are unable to locate the authentication provider specified.
     */
    public void unlinkUserFromProvider(final RegisteredUserDTO user, final String providerString)
            throws SegueDatabaseException, MissingRequiredFieldException, AuthenticationProviderMappingException {
        RegisteredUser userDO = this.findUserById(user.getId());
        // check if the provider is there to delete in the first place. If not just return.
        if (!this.database.getAuthenticationProvidersByUser(userDO).contains(
                this.mapToProvider(providerString).getAuthenticationProvider())) {
            return;
        }

        // make sure that the change doesn't prevent the user from logging in again.
        if ((this.database.getAuthenticationProvidersByUser(userDO).size() > 1) || userDO.getPassword() != null) {
            this.database.unlinkAuthProviderFromUser(userDO, this.mapToProvider(providerString)
                    .getAuthenticationProvider());
        } else {
            throw new MissingRequiredFieldException("This modification would mean that the user"
                    + " no longer has a way of authenticating. Failing change.");
        }
    }

    /**
     * CheckUserRole matches a list of valid roles.
     * 
     * @param request
     *            - http request so that we can get current users details.
     * @param validRoles
     *            - a Collection of roles that we would want the user to match.
     * @return true if the user is a member of one of the roles in our valid roles list. False if not.
     * @throws NoUserLoggedInException
     *             - if there is no registered user logged in.
     */
    public final boolean checkUserRole(final HttpServletRequest request, final Collection<Role> validRoles)
            throws NoUserLoggedInException {
        RegisteredUser user = this.getCurrentRegisteredUserDO(request);

        if (null == user) {
            throw new NoUserLoggedInException();
        }

        for (Role roleToMatch : validRoles) {
            if (user.getRole() != null && user.getRole().equals(roleToMatch)) {
                return true;
            }
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
     * @return Returns the current UserDTO if we can get it or null if user is not currently logged in
     * @throws NoUserLoggedInException
     *             - When the session has expired or there is no user currently logged in.
     */
    public final RegisteredUserDTO getCurrentRegisteredUser(final HttpServletRequest request)
            throws NoUserLoggedInException {
        Validate.notNull(request);

        RegisteredUser user = this.getCurrentRegisteredUserDO(request);

        if (null == user) {
            throw new NoUserLoggedInException();
        }

        try {
            updateLastSeen(user);
        } catch (SegueDatabaseException e) {
            log.error(String.format("Unable to update user (%s) last seen date.", user.getId()));
        }

        return this.convertUserDOToUserDTO(user);
    }

    /**
     * Find a list of users based on some user prototype.
     * 
     * @param prototype
     *            - partially completed user object to base search on
     * @return list of registered user dtos.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public final List<RegisteredUserDTO> findUsers(final RegisteredUserDTO prototype) throws SegueDatabaseException {
        List<RegisteredUser> registeredUsersDOs = this.database.findUsers(this.dtoMapper.map(prototype,
                RegisteredUser.class));

        return this.convertUserDOToUserDTOList(registeredUsersDOs);
    }

    /**
     * Find a list of users based on a List of user ids.
     * 
     * @param userIds
     *            - partially completed user object to base search on
     * @return list of registered user dtos.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public final List<RegisteredUserDTO> findUsers(final List<String> userIds) throws SegueDatabaseException {
        Validate.notNull(userIds);
        if (userIds.isEmpty()) {
            return Lists.newArrayList();
        }

        List<RegisteredUser> registeredUsersDOs = this.database.findUsers(userIds);

        return this.convertUserDOToUserDTOList(registeredUsersDOs);
    }

    /**
     * This function can be used to find user information about a user when given an id.
     * 
     * @param id
     *            - the id of the user to search for.
     * @return the userDTO
     * @throws NoUserException
     *             - If we cannot find a valid user with the email address provided.
     * @throws SegueDatabaseException
     *             - If there is another database error       
     */
    public final RegisteredUserDTO getUserDTOById(final Long id) throws NoUserException, SegueDatabaseException {
        return this.convertUserDOToUserDTO(this.findUserById(id));
    }
    
    /**
     * This function can be used to find user information about a user when given an id.
     * 
     * @param id
     *            - the id of the user to search for.
     * @return the userDTO
     * @throws NoUserException
     *             - If we cannot find a valid user with the email address provided.
     * @throws SegueDatabaseException
     *             - If there is another database error
     * @deprecated uses getUserDTOById            
     */
    @Deprecated
    public final RegisteredUserDTO getUserDTOByLegacyId(final String id) throws NoUserException,
            SegueDatabaseException {
        return this.convertUserDOToUserDTO(this.findUserByLegacyId(id));
    }

    /**
     * This function can be used to find user information about a user when given an email.
     * 
     * @param email
     *            - the e-mail address of the user to search for
     * @return the userDTO
     * @throws NoUserException
     *             - If we cannot find a valid user with the email address provided.
     * @throws SegueDatabaseException
     *             - If there is another database error
     */
    public final RegisteredUserDTO getUserDTOByEmail(final String email) throws NoUserException,
            SegueDatabaseException {
        RegisteredUser findUserByEmail = this.findUserByEmail(email);

        if (null == findUserByEmail) {
            throw new NoUserException();
        }

        return this.convertUserDOToUserDTO(findUserByEmail);
    }

    /**
     * This method will return either an AnonymousUserDTO or a RegisteredUserDTO
     * 
     * If the user is currently logged in you will get a RegisteredUserDTO otherwise you will get an AnonymousUserDTO
     * containing a sessionIdentifier and any questionAttempts made by the anonymous user.
     * 
     * @param request
     *            - containing session information.
     * 
     * @return AbstractSegueUserDTO - Either a RegisteredUser or an AnonymousUser
     */
    public AbstractSegueUserDTO getCurrentUser(final HttpServletRequest request) {
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
     *            containing the tomcat session to destroy
     * @param response
     *            to destroy the segue cookie.
     */
    public void logUserOut(final HttpServletRequest request, final HttpServletResponse response) {
        Validate.notNull(request);
        try {
            request.getSession().invalidate();
            Cookie logoutCookie = new Cookie(SEGUE_AUTH_COOKIE, "");
            logoutCookie.setPath("/");
            logoutCookie.setMaxAge(0);
            logoutCookie.setHttpOnly(true);

            response.addCookie(logoutCookie);
        } catch (IllegalStateException e) {
            log.info("The session has already been invalidated. " + "Unable to logout again...", e);
        }
    }

    /**
     * Record that someone has answered a question.
     * 
     * If the user is anonymous the question record will be added as a temporary session variable. This will enable
     * merging later if the user registers.
     * 
     * @param user
     *            - AbstractSegueUserDTO either registered or anonymous. - containing either a registered or anonymous
     *            user.
     * @param questionResponse
     *            - question results.
     */
    public void recordQuestionAttempt(final AbstractSegueUserDTO user,
            final QuestionValidationResponseDTO questionResponse) {
        QuestionValidationResponse questionResponseDO = this.dtoMapper.map(questionResponse,
                QuestionValidationResponse.class);

        // We are operating against the convention that the first component of
        // an id is the question page
        // and that the id separator is |
        String[] questionPageId = questionResponse.getQuestionId().split(ESCAPED_ID_SEPARATOR);

        if (user instanceof RegisteredUserDTO) {
            RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;
            try {
                this.questionAttemptDb.registerQuestionAttempt(registeredUser.getLegacyDbId(), questionPageId[0],
                        questionResponse.getQuestionId(), questionResponseDO);
                log.debug("Question information recorded for user: " + registeredUser.getLegacyDbId());
            } catch (SegueDatabaseException e) {
                log.error("Unable to to record question attempt.", e);
            }

        } else if (user instanceof AnonymousUserDTO) {
            AnonymousUserDTO anonymousUserDTO = (AnonymousUserDTO) user;

            this.recordAnonymousUserQuestionInformation(
                    this.findAnonymousUserDOBySessionId(anonymousUserDTO.getSessionId()), questionPageId[0],
                    questionResponse.getQuestionId(), questionResponse);
        } else {
            log.error("Unexpected user type. Unable to record question response");
        }
    }

    /**
     * getQuestionAttemptsByUser. This method will return all of the question attempts for a given user as a map.
     * 
     * @param user
     *            - with the session information included.
     * @return map of question attempts (QuestionPageId -> QuestionID -> [QuestionValidationResponse] or an empty map.
     * @throws SegueDatabaseException
     *             - if there is a database error.
     */
    public final Map<String, Map<String, List<QuestionValidationResponse>>> getQuestionAttemptsByUser(
            final AbstractSegueUserDTO user) throws SegueDatabaseException {
        Validate.notNull(user);

        if (user instanceof RegisteredUserDTO) {
            RegisteredUser registeredUser = this.findUserByLegacyId(((RegisteredUserDTO) user).getLegacyDbId());

            return this.questionAttemptDb.getQuestionAttempts(registeredUser.getLegacyDbId()).getQuestionAttempts();
        } else {
            AnonymousUser anonymousUser = this.temporaryUserCache.getIfPresent(((AnonymousUserDTO) user)
                    .getSessionId());
            // since no user is logged in assume that we want to use any anonymous attempts
            return anonymousUser.getTemporaryQuestionAttempts();
        }
    }

    /**
     * Method to create a user object in our database.
     * 
     * @param request
     *            to enable access to anonymous user information.
     * @param response
     *            to store the session in our own segue cookie.
     * @param user
     *            - the user DO to use for updates - must not contain a user id.
     * @throws InvalidPasswordException
     *             - the password provided does not meet our requirements.
     * @throws MissingRequiredFieldException
     *             - A required field is missing for the user object so cannot be saved.
     * @return the user object as was saved.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     * @throws AuthenticationProviderMappingException
     *             - if there is a problem locating the authentication provider. This only applies for changing a
     *             password.
     */
    public RegisteredUserDTO createUserObjectAndSession(final HttpServletRequest request,
            final HttpServletResponse response, final RegisteredUser user) throws InvalidPasswordException,
            MissingRequiredFieldException, SegueDatabaseException, AuthenticationProviderMappingException {
        Validate.isTrue(user.getId() == null,
                "When creating a new user the user id must not be set.");

        if (this.findUserByEmail(user.getEmail()) != null) {
            throw new DuplicateAccountException("An account with that e-mail address already exists.");
        }

        RegisteredUser userToSave = null;
        MapperFacade mapper = this.dtoMapper;

        // We want to map to DTO first to make sure that the user cannot
        // change fields that aren't exposed to them
        RegisteredUserDTO userDtoForNewUser = mapper.map(user, RegisteredUserDTO.class);

        // This is a new registration
        userToSave = mapper.map(userDtoForNewUser, RegisteredUser.class);

        // default role is unset
        userToSave.setRole(null);
        userToSave.setRegistrationDate(new Date());
        userToSave.setLastUpdated(new Date());

        this.checkForSeguePasswordChange(user, userToSave);

        // Before save we should validate the user for mandatory fields.
        if (!this.isUserValid(userToSave)) {
            throw new MissingRequiredFieldException("The user provided is missing a mandatory field");
        } else if (!this.database.hasALinkedAccount(userToSave) && userToSave.getPassword() == null) {
            // a user must have a way of logging on.
            throw new MissingRequiredFieldException("This modification would mean that the user"
                    + " no longer has a way of authenticating. Reverting change.");
        }

        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        try {
            authenticator.createEmailVerificationTokenForUser(userToSave, userToSave.getEmail());
        } catch (NoSuchAlgorithmException e1) {
            log.error("Creation of email verification token failed: " + e1.getMessage());
        } catch (InvalidKeySpecException e1) {
            log.error("Creation of email verification token failed: " + e1.getMessage());
        }

        // save the user to get the userId
        RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);

        // send an email confirmation and set up verification
        try {
            emailManager.sendRegistrationConfirmation(userToReturn);
            userToReturn.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        } catch (ContentManagerException e) {
            log.error("Registration email could not be sent due to content issue: " + e.getMessage());
        }

        // save the user again with updated token
        userToReturn = this.database.createOrUpdateUser(userToReturn);

        logManager.logInternalEvent(this.convertUserDOToUserDTO(userToReturn), Constants.USER_REGISTRATION,
                ImmutableMap.builder().put("provider", AuthenticationProvider.SEGUE.name()).build());

        this.createSession(request, response, userToReturn);

        // return it to the caller.
        return this.convertUserDOToUserDTO(userToReturn);
    }

    /**
     * Method to update a user object in our database.
     * 
     * @param user
     *            - the user to update - must contain a user id
     * @throws InvalidPasswordException
     *             - the password provided does not meet our requirements.
     * @throws MissingRequiredFieldException
     *             - A required field is missing for the user object so cannot be saved.
     * @return the user object as was saved.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     * @throws AuthenticationProviderMappingException
     *             - if there is a problem locating the authentication provider. This only applies for changing a
     *             password.
     */
    public RegisteredUserDTO updateUserObject(final RegisteredUser user) throws InvalidPasswordException,
            MissingRequiredFieldException, SegueDatabaseException, AuthenticationProviderMappingException {
        Validate.notNull(user.getId());
        MapperFacade mapper = this.dtoMapper;

        // We want to map to DTO first to make sure that the user cannot
        // change fields that aren't exposed to them
        RegisteredUserDTO userDTOContainingUpdates = mapper.map(user, RegisteredUserDTO.class);
        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "The user object specified has an id. Users cannot be updated without a specific id already set.");
        }

        // This is an update operation.
        final RegisteredUser existingUser = this.findUserById(user.getId());
        // userToSave = existingUser;

        // Check that the user isn't trying to take an existing users e-mail.
        if (this.findUserByEmail(user.getEmail()) != null && !existingUser.getEmail().equals(user.getEmail())) {
            throw new DuplicateAccountException("An account with that e-mail address already exists.");
        }

        // Send a new verification email if the user has changed their email
        if (!existingUser.getEmail().equals(user.getEmail())) {

            IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                    .get(AuthenticationProvider.SEGUE);

            try {
                authenticator.createEmailVerificationTokenForUser(existingUser, user.getEmail());
            } catch (NoSuchAlgorithmException e1) {
                log.error("Creation of email verification token failed: " + e1.getMessage());
            } catch (InvalidKeySpecException e1) {
                log.error("Creation of email verification token failed: " + e1.getMessage());
            }

            log.info(String.format("Sending email for email address change for user (%s)"
                    + " from email (%s) to email (%s)", user.getId(), 
                    existingUser.getEmail(), user.getEmail()));
            try {
                this.emailManager.sendEmailVerificationChange(existingUser, user);
            } catch (ContentManagerException e) {
                log.debug("ContentManagerException during sendEmailVerificationChange " + e.getMessage());
            }

        }

        MapperFacade mergeMapper = new DefaultMapperFactory.Builder().mapNulls(false).build().getMapperFacade();

        RegisteredUser userToSave = new RegisteredUser();
        mergeMapper.map(existingUser, userToSave);
        mergeMapper.map(userDTOContainingUpdates, userToSave);
        userToSave.setRegistrationDate(existingUser.getRegistrationDate());
        userToSave.setLastUpdated(new Date());

        // special case if user used to have a role and admin user has removed it - it needs to be removed.
        // this is safe as it is equivalent to having no permissions.
        if (user.getRole() == null && existingUser.getRole() != null) {
            userToSave.setRole(null);
        }

        if (user.getSchoolId() == null && existingUser.getSchoolId() != null) {
            userToSave.setSchoolId(null);
        }
        
        this.checkForSeguePasswordChange(user, userToSave);

        // Before save we should validate the user for mandatory fields.
        if (!this.isUserValid(userToSave)) {
            throw new MissingRequiredFieldException("The user provided is missing a mandatory field");
        } else if (!this.database.hasALinkedAccount(userToSave) && userToSave.getPassword() == null) {
            // a user must have a way of logging on.
            throw new MissingRequiredFieldException("This modification would mean that the user"
                    + " no longer has a way of authenticating. Failing change.");
        }

        // Make sure the email address is preserved (can't be changed until new email is verified)
        if (!userToSave.getEmail().equals(existingUser.getEmail())) {
            try {
                this.emailManager.sendEmailVerification(userToSave);
            } catch (ContentManagerException e) {
                log.debug("ContentManagerException during sendEmailVerification " + e.getMessage());
            }
            userToSave.setEmail(existingUser.getEmail());
        }

        // save the user
        RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);
        // return it to the caller
        return this.convertUserDOToUserDTO(userToReturn);
    }

    /**
     * This method facilitates the removal of personal user data from Segue.
     * 
     * @param userToDelete
     *            - the user to delete.
     * @throws SegueDatabaseException
     *             - if a general database error has occurred.
     * @throws NoUserException
     *             - if we cannot find the user account specified
     */
    public void deleteUserAccount(final RegisteredUserDTO userToDelete) throws NoUserException, SegueDatabaseException {
        Validate.notNull(userToDelete);

        // check the user exists
        RegisteredUser userDOById = this.findUserById(userToDelete.getId());

        // delete the user.
        this.database.deleteUserAccount(userDOById);
    }

    /**
     * This method will use an email address to check a local user exists and if so, will send an email with a unique
     * token to allow a password reset. This method does not indicate whether or not the email actually existed.
     *
     * @param userObject
     *            - A user object containing the email address of the user to reset the password for.
     * @throws NoSuchAlgorithmException
     *             - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException
     *             - if the preconfigured key spec is invalid.
     * @throws CommunicationException
     *             - if a fault occurred whilst sending the communique
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    public final void resetPasswordRequest(final RegisteredUserDTO userObject) throws InvalidKeySpecException,
            NoSuchAlgorithmException, CommunicationException, SegueDatabaseException {
        RegisteredUser user = this.findUserByEmail(userObject.getEmail());

        if (user == null) {
            // Email address does not exist in the DB
            // Fail silently
            log.error(String.format("Unable to locate user with email (%s) while "
                    + "trying to generate a reset token. Failing silently.", userObject == null ? "null email address"
                    : userObject.getEmail()));

            return;
        }

        if (this.database.hasALinkedAccount(user) && (user.getPassword() == null || user.getPassword().isEmpty())) {
            // User is not authenticated locally
            this.sendFederatedAuthenticatorResetMessage(user);
            return;
        }

        // User is valid and authenticated locally, proceed with reset
        // Generate token
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        user = authenticator.createPasswordResetTokenForUser(user);

        // Save user object
        this.database.createOrUpdateUser(user);

        log.info(String.format("Sending password reset message to %s", user.getEmail()));
        try {
            this.emailManager.sendPasswordReset(user);
        } catch (ContentManagerException e) {
            log.debug("ContentManagerException " + e.getMessage());
        }
    }

    /**
     * This method will use an email address to check a local user exists and if so, will send an email with a unique
     * token to allow a password reset. This method does not indicate whether or not the email actually existed.
     * 
     * @param request
     *            - so we can look up the registered user object.
     * @param email
     *            - The email the user wants to verify.
     * @throws NoSuchAlgorithmException
     *             - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException
     *             - if the preconfigured key spec is invalid.
     * @throws CommunicationException
     *             - if a fault occurred whilst sending the communique
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    public final void emailVerificationRequest(final HttpServletRequest request, final String email)
            throws InvalidKeySpecException, NoSuchAlgorithmException, CommunicationException, SegueDatabaseException {

        RegisteredUser user = this.findUserByEmail(email);
        if (null == user) {
            try {
                RegisteredUserDTO userDTO = getCurrentRegisteredUser(request);
                user = this.findUserById(userDTO.getId());
            } catch (NoUserLoggedInException e) {
                log.error(String.format("Verification requested for email:%s where email does not exist "
                        + "and user not logged in!", email));
            }
        }

        if (user == null) {
            // Email address does not exist in the DB
            // Fail silently
            return;
        }

        // User is valid and authenticated locally, proceed with verification
        // Generate token
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        user = authenticator.createEmailVerificationTokenForUser(user, email);

        // Save user object
        this.database.createOrUpdateUser(user);

        log.info(String.format("Sending password reset message to %s", user.getEmail()));
        try {
            this.emailManager.sendEmailVerification(user);
        } catch (ContentManagerException e) {
            log.debug("ContentManagerException " + e.getMessage());
        }
    }

    /**
     * This method will test if the specified token is a valid password reset token.
     * 
     * 
     * @param token
     *            - The token to test
     * @return true if the reset token is valid
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    public final boolean validatePasswordResetToken(final String token) throws SegueDatabaseException {
        // Set user's password
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        return authenticator.isValidResetToken(this.findUserByResetToken(token));
    }

    /**
     * processEmailVerification.
     * @param userid
     *            - the user id
     *
     * @param email
     *            - the email address - may be new or the same
     * 
     * @param token
     *            - token used to verify email address
     * 
     * @return - whether the token is valid or not
     * @throws SegueDatabaseException
     *             - exception if token cannot be validated
     * @throws InvalidTokenException - if something is wrong with the token provided
     * @throws NoUserException - if the user does not exist.
     */
    public RegisteredUserDTO processEmailVerification(final String userid, final String email, final String token) 
            throws SegueDatabaseException, InvalidTokenException, NoUserException {
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        RegisteredUser user = this.findUserByLegacyId(userid);

        if (null == user) {
            log.warn(String.format("Recieved an invalid email token request for (%s)", email));
            throw new NoUserException();    
        }

        if (!userid.equals(user.getLegacyDbId())) {
            log.warn(String.format("Recieved an invalid email token request for (%s)" + " - provided bad userid",
                    email));
            throw new InvalidTokenException();
        }

        EmailVerificationStatus evStatus = user.getEmailVerificationStatus();
        if (evStatus != null && evStatus == EmailVerificationStatus.VERIFIED && user.getEmail().equals(email)) {
            log.warn(String.format("Recieved an invalid email token request for (%s) - already verified", email));
            throw new InvalidTokenException();
        }

        if (authenticator.isValidEmailVerificationToken(user, email, token)) {
            user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
            user.setEmailVerificationToken(null);
            user.setEmailVerificationTokenExpiry(null);

            // Update the email address if different
            if (!user.getEmail().equals(email)) {
                user.setEmail(email);
            }

            // Save user
            RegisteredUser createOrUpdateUser = this.database.createOrUpdateUser(user);
            log.info(String.format("Email verification for user (%s) has completed successfully.",
                    createOrUpdateUser.getId()));
            return this.convertUserDOToUserDTO(createOrUpdateUser);
        } else {
            log.warn(String.format("Recieved an invalid email verification token for (%s) - invalid token", email));
            throw new InvalidTokenException();
        }
    }

    /**
     * This method will use a unique password reset token to set a new password.
     *
     * @param token
     *            - the password reset token
     * @param userObject
     *            - the supplied user DO
     * @return the user which has had the password reset.
     * @throws InvalidTokenException
     *             - If the token provided is invalid.
     * @throws InvalidPasswordException
     *             - If the password provided is invalid.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    public final RegisteredUserDTO resetPassword(final String token, final RegisteredUser userObject)
            throws InvalidTokenException, InvalidPasswordException, SegueDatabaseException {
        // Ensure new password is valid
        if (userObject.getPassword() == null || userObject.getPassword().isEmpty()) {
            throw new InvalidPasswordException("Empty passwords are not allowed if using local authentication.");
        }

        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

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
        RegisteredUser createOrUpdateUser = this.database.createOrUpdateUser(user);
        log.info(String.format("Password Reset for user (%s) has completed successfully.",
                createOrUpdateUser.getId()));
        return this.convertUserDOToUserDTO(createOrUpdateUser);
    }

    /**
     * Helper method to convert a user object into a cutdown userSummary DTO.
     * 
     * @param userToConvert
     *            - full user object.
     * @return a summarised object with minimal personal information
     */
    public UserSummaryDTO convertToUserSummaryObject(final RegisteredUserDTO userToConvert) {
        return this.dtoMapper.map(userToConvert, UserSummaryDTO.class);
    }

    /**
     * Helper method to convert a user object into a cutdown userSummary DTO.
     * 
     * @param userListToConvert
     *            - full user objects.
     * @return a list of summarised objects with minimal personal information
     */
    public List<UserSummaryDTO> convertToUserSummaryObjectList(final List<RegisteredUserDTO> userListToConvert) {
        Validate.notNull(userListToConvert);
        List<UserSummaryDTO> resultList = Lists.newArrayList();
        for (RegisteredUserDTO user : userListToConvert) {
            resultList.add(this.convertToUserSummaryObject(user));
        }
        return resultList;
    }

    /**
     * Create a session and attach it to the request provided.
     * 
     * @param request
     *            to enable access to anonymous user information.
     * @param response
     *            to store the session in our own segue cookie.
     * @param user
     *            account to associate the session with.
     */
    private void createSession(final HttpServletRequest request, final HttpServletResponse response,
            final RegisteredUser user) {
        Validate.notNull(response);
        Validate.notNull(user);
        Validate.notNull(user.getId());
        SimpleDateFormat sessionDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);
        Integer sessionExpiryTimeInSeconds = Integer.parseInt(properties.getProperty(SESSION_EXPIRY_SECONDS));

        String userId = user.getId().toString();
        String hmacKey = properties.getProperty(HMAC_SALT);

        try {
            String currentDate = sessionDateFormat.format(new Date());
            String sessionHMAC = this.calculateSessionHMAC(hmacKey, userId, currentDate);

            Map<String, String> sessionInformation = ImmutableMap.of(SESSION_USER_ID, userId, DATE_SIGNED,
                    currentDate, HMAC, sessionHMAC);

            Cookie authCookie = new Cookie(SEGUE_AUTH_COOKIE,
                    serializationMapper.writeValueAsString(sessionInformation));
            authCookie.setMaxAge(sessionExpiryTimeInSeconds);
            authCookie.setPath("/");
            authCookie.setHttpOnly(true);

            response.addCookie(authCookie);

            AnonymousUser anonymousUser = this.findAnonymousUserDOBySessionId(this.getAnonymousUser(request)
                    .getSessionId());
            if (anonymousUser != null) {
                // merge any anonymous information collected with this user.
                try {
                    this.mergeAnonymousQuestionInformationWithUserRecord(anonymousUser, user);
                } catch (NoUserLoggedInException | SegueDatabaseException e) {
                    log.error("Unable to merge anonymously collected data with stored user object.", e);
                }
            }
        } catch (JsonProcessingException e1) {
            log.error("Unable to save cookie.", e1);
        }
    }

    /**
     * Helper method to handle the setting of segue passwords when user objects are updated.
     * 
     * This method will mutate the password fields in both parameters.
     * 
     * @param userContainingPlainTextPassword
     *            - the object to extract the plain text password from (and then nullify it)
     * @param userToSave
     *            - the object to store the hashed credentials prior to saving.
     * 
     * @throws AuthenticationProviderMappingException
     *             - if we can't map to a valid authenticator.
     * @throws InvalidPasswordException
     *             - if the password is not valid.
     */
    private void checkForSeguePasswordChange(final RegisteredUser userContainingPlainTextPassword,
            final RegisteredUser userToSave) throws AuthenticationProviderMappingException, InvalidPasswordException {
        // do we need to do local password storage using the segue
        // authenticator? I.e. is the password changing?
        if (null != userContainingPlainTextPassword.getPassword()
                && !userContainingPlainTextPassword.getPassword().isEmpty()) {
            IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this
                    .mapToProvider(AuthenticationProvider.SEGUE.name());
            String plainTextPassword = userContainingPlainTextPassword.getPassword();

            // clear reference to plainTextPassword
            userContainingPlainTextPassword.setPassword(null);

            // set the new password on the object to be saved.
            authenticator.setOrChangeUsersPassword(userToSave, plainTextPassword);
        }
    }

    /**
     * Library method that allows the api to locate a user object from the database based on a given unique id.
     *
     * @param userId
     *            - to search for.
     * @return user or null if we cannot find it.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    @Deprecated
    private RegisteredUser findUserByLegacyId(final String userId) throws SegueDatabaseException {
        if (null == userId) {
            return null;
        }
        return this.database.getByLegacyId(userId);
    }
    
    /**
     * Library method that allows the api to locate a user object from the database based on a given unique id.
     *
     * @param userId
     *            - to search for.
     * @return user or null if we cannot find it.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    private RegisteredUser findUserById(final Long userId) throws SegueDatabaseException {
        if (null == userId) {
            return null;
        }
        return this.database.getById(userId);
    }

    /**
     * Library method that allows the api to locate a user object from the database based on a given unique email
     * address.
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
     * Library method that allows the api to locate a user object from the database based on a given unique password
     * reset token.
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
     * This method will trigger the authentication flow for a 3rd party authenticator.
     * 
     * This method can be used for regular logins, new registrations or for linking 3rd party authenticators to an
     * existing Segue user account.
     * 
     * @param request
     *            - http request that we can attach the session to and that already has a redirect url attached.
     * @param provider
     *            - the provider the user wishes to authenticate with.
     * @return A json response containing a URI to the authentication provider if authorization / login is required.
     *         Alternatively a SegueErrorResponse could be returned.
     * @throws IOException - 
     * @throws AuthenticationProviderMappingException - as per exception description.
     */
    private URI initiateAuthenticationFlow(final HttpServletRequest request, final String provider) 
            throws IOException, AuthenticationProviderMappingException {
        IAuthenticator federatedAuthenticator = mapToProvider(provider);

        // if we are an OAuthProvider redirect to the provider
        // authorisation URL.
        URI redirectLink = null;
        if (federatedAuthenticator instanceof IOAuth2Authenticator) {
            IOAuth2Authenticator oauth2Provider = (IOAuth2Authenticator) federatedAuthenticator;
            String antiForgeryTokenFromProvider = oauth2Provider.getAntiForgeryStateToken();

            // Store antiForgeryToken in the users session.
            request.getSession().setAttribute(STATE_PARAM_NAME, antiForgeryTokenFromProvider);

            redirectLink = URI.create(oauth2Provider.getAuthorizationUrl(antiForgeryTokenFromProvider));
        } else if (federatedAuthenticator instanceof IOAuth1Authenticator) {
            IOAuth1Authenticator oauth1Provider = (IOAuth1Authenticator) federatedAuthenticator;
            OAuth1Token token = oauth1Provider.getRequestToken();

            // Store token and secret in the users session.
            request.getSession().setAttribute(OAUTH_TOKEN_PARAM_NAME, token.getToken());

            redirectLink = URI.create(oauth1Provider.getAuthorizationUrl(token));
        } else {
            throw new AuthenticationProviderMappingException("Unable to map to a known authenticator. "
                    + "The provider: " + provider + " is unknown");
        }

        return redirectLink;
    }

    /**
     * Executes checks on the users sessions to ensure it is valid
     * 
     * Checks include verifying the HMAC and the session creation date.
     * 
     * @param sessionInformation
     *            - map containing session information retrieved from the cookie.
     * @return true if it is still valid, false if not.
     */
    private boolean isValidUsersSession(final Map<String, String> sessionInformation) {
        Validate.notNull(sessionInformation);

        Integer sessionExpiryTimeInSeconds = Integer.parseInt(properties.getProperty(SESSION_EXPIRY_SECONDS));

        SimpleDateFormat sessionDateFormat = new SimpleDateFormat(DEFAULT_DATE_FORMAT);

        String hmacKey = properties.getProperty(HMAC_SALT);

        String userId = sessionInformation.get(SESSION_USER_ID);
        String sessionCreationDate = sessionInformation.get(DATE_SIGNED);
        String sessionHMAC = sessionInformation.get(HMAC);

        String ourHMAC = this.calculateSessionHMAC(hmacKey, userId, sessionCreationDate);

        if (null == userId) {
            log.debug("No session set so not validating user identity.");
            return false;
        }

        // check it hasn't expired
        Calendar sessionExpiryDate = Calendar.getInstance();
        try {
            sessionExpiryDate.setTime(sessionDateFormat.parse(sessionCreationDate));
            sessionExpiryDate.add(Calendar.SECOND, sessionExpiryTimeInSeconds);

            if (new Date().after(sessionExpiryDate.getTime())) {
                log.debug("Session expired");
                return false;
            }
        } catch (ParseException e) {
            return false;
        }

        // check no one has tampered with the session.
        if (ourHMAC.equals(sessionHMAC)) {
            log.debug("Valid user session continuing...");
            return true;
        } else {
            log.debug("Invalid HMAC detected for user id " + userId);
            return false;
        }
    }

    /**
     * Calculate the session HMAC value based on the properties of interest.
     * 
     * @param key
     *            - secret key.
     * @param userId
     *            - User Id
     * @param currentDate
     *            - Current date
     * @return HMAC signature.
     */
    private String calculateSessionHMAC(final String key, final String userId, final String currentDate) {
        return UserManager.calculateHMAC(key, userId + "|" + currentDate);
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
    public static String calculateHMAC(final String key, final String dataToSign) {
        Validate.notEmpty(key, "Signing key cannot be blank.");
        Validate.notEmpty(dataToSign, "Data to sign cannot be blank.");

        try {
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), HMAC_SHA_ALGORITHM);
            Mac mac = Mac.getInstance(HMAC_SHA_ALGORITHM);
            mac.init(signingKey);

            byte[] rawHmac = mac.doFinal(dataToSign.getBytes());

            String result = new String(Base64.encodeBase64(rawHmac));
            return result;
        } catch (GeneralSecurityException e) {
            log.warn("Unexpected error while creating hash: " + e.getMessage(), e);
            throw new IllegalArgumentException();
        }
    }

    /**
     * Attempts to map a string to a known provider.
     * 
     * @param provider
     *            - String representation of the provider requested
     * @return the FederatedAuthenticator object which can be used to get a user.
     * @throws AuthenticationProviderMappingException
     *             if we are unable to locate the provider requested.
     */
    private IAuthenticator mapToProvider(final String provider) throws AuthenticationProviderMappingException {
        Validate.notEmpty(provider, "Provider name must not be empty or null if we are going "
                + "to map it to an implementation.");

        AuthenticationProvider enumProvider = null;
        try {
            enumProvider = AuthenticationProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthenticationProviderMappingException("The provider requested is "
                    + "invalid and not a known AuthenticationProvider: " + provider);
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
     * @return true if we are happy , false if we think a violation has occurred.
     * @throws CrossSiteRequestForgeryException
     *             - if we suspect cross site request forgery.
     */
    private boolean ensureNoCSRF(final HttpServletRequest request, final IOAuthAuthenticator oauthProvider)
            throws CrossSiteRequestForgeryException {
        Validate.notNull(request);

        String key;
        if (oauthProvider instanceof IOAuth2Authenticator) {
            key = STATE_PARAM_NAME;
        } else if (oauthProvider instanceof IOAuth1Authenticator) {
            key = OAUTH_TOKEN_PARAM_NAME;
        } else {
            throw new CrossSiteRequestForgeryException("Provider not recognized.");
        }

        // to deal with cross site request forgery
        String csrfTokenFromUser = (String) request.getSession().getAttribute(key);
        String csrfTokenFromProvider = request.getParameter(key);

        if (null == csrfTokenFromUser || null == csrfTokenFromProvider
                || !csrfTokenFromUser.equals(csrfTokenFromProvider)) {
            log.error("Invalid state parameter - Provider said: " + request.getParameter(STATE_PARAM_NAME)
                    + " Session said: " + request.getSession().getAttribute(STATE_PARAM_NAME));
            return false;
        } else {
            log.debug("State parameter matches - Provider said: " + request.getParameter(STATE_PARAM_NAME)
                    + " Session said: " + request.getSession().getAttribute(STATE_PARAM_NAME));
            return true;
        }
    }

    /**
     * This method should handle the situation where we haven't seen a user before.
     * 
     * @param user
     *            from authentication provider
     * @param provider
     *            information
     * @param providerUserId
     *            - unique id of provider.
     * @return The localUser account user id of the user after registration.
     * @throws DuplicateAccountException
     *             - If there is an account that already exists in the system with matching indexed fields.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    private Long registerUser(final RegisteredUser user, final AuthenticationProvider provider,
            final String providerUserId) throws DuplicateAccountException, SegueDatabaseException {
        RegisteredUser newlyRegisteredUser = database.registerNewUserWithProvider(user, provider, providerUserId);
        return newlyRegisteredUser.getId();
    }

    /**
     * This method will attempt to find a segue user using a 3rd party provider and a unique id that identifies the user
     * to the provider.
     * 
     * @param provider
     *            - the provider that we originally validated with
     * @param providerId
     *            - the unique ID of the user as given to us from the provider.
     * @return A user object or null if we were unable to find the user with the information provided.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    private RegisteredUser getUserFromLinkedAccount(final AuthenticationProvider provider, final String providerId)
            throws SegueDatabaseException {
        Validate.notNull(provider);
        Validate.notBlank(providerId);

        RegisteredUser user = database.getByLinkedAccount(provider, providerId);
        if (null == user) {
            log.debug("Unable to locate user based on provider " + "information provided.");
        }
        return user;
    }

    /**
     * Gets an existing Segue user from a 3rd party authenticator.
     * 
     * @param federatedAuthenticator
     *            the federatedAuthenticator we are using for authentication
     * @param providerSpecificUserLookupReference
     *            - the look up reference provided by the authenticator after any authenticator specific actions have
     *            been completed.
     * @return a Segue UserDO that exists in the segue database.
     * @throws AuthenticatorSecurityException
     *             - error with authenticator.
     * @throws NoUserException
     *             - If we are unable to locate the user id based on the lookup reference provided.
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
     * This method should use the provider specific reference to either register a new user or retrieve an existing
     * user.
     * 
     * @param federatedAuthenticator
     *            the federatedAuthenticator we are using for authentication
     * @param providerSpecificUserLookupReference
     *            - the look up reference provided by the authenticator after any authenticator specific actions have
     *            been completed.
     * @return a Segue UserDO that exists in the segue database.
     * @throws AuthenticatorSecurityException
     *             - error with authenticator.
     * @throws NoUserException
     *             - If we are unable to locate the user id based on the lookup reference provided.
     * @throws IOException
     *             - if there is an io error.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    private RegisteredUser registerUserWithFederatedProvider(final IFederatedAuthenticator federatedAuthenticator,
            final String providerSpecificUserLookupReference) throws AuthenticatorSecurityException, NoUserException,
            IOException, SegueDatabaseException {
        // get user info from federated provider
        RegisteredUser localUserInformation = this.getUserFromFederatedProvider(federatedAuthenticator,
                providerSpecificUserLookupReference);

        // decide if we need to register a new user or link to an existing
        // account
        if (null == localUserInformation) {
            log.debug(String.format("New registration (%s) as user does not already exist.", federatedAuthenticator
                    .getAuthenticationProvider().name()));

            UserFromAuthProvider userFromProvider = federatedAuthenticator
                    .getUserInfo(providerSpecificUserLookupReference);

            if (null == userFromProvider) {
                log.warn("Unable to create user for the provider "
                        + federatedAuthenticator.getAuthenticationProvider().name());
                throw new NoUserException();
            }

            RegisteredUser newLocalUser = this.dtoMapper.map(userFromProvider, RegisteredUser.class);
            newLocalUser.setRegistrationDate(new Date());

            // register user
            Long localUserId = registerUser(newLocalUser, federatedAuthenticator.getAuthenticationProvider(),
                    userFromProvider.getProviderUserId());
            localUserInformation = this.database.getById(localUserId);

            if (null == localUserInformation) {
                // we just put it in so something has gone very wrong.
                log.error("Failed to retreive user even though we " + "just put it in the database.");
                throw new NoUserException();
            }

            logManager.logInternalEvent(this.convertUserDOToUserDTO(localUserInformation),
                    Constants.USER_REGISTRATION,
                    ImmutableMap.builder().put("provider", federatedAuthenticator.getAuthenticationProvider().name())
                            .build());

        } else {
            log.error("Returning user detected" + localUserInformation.getId()
                    + " unable to create a new segue user.");
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
     *            - the look up reference provided by the authenticator after any authenticator specific actions have
     *            been completed.
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
            final IFederatedAuthenticator federatedAuthenticator, final String providerSpecificUserLookupReference)
            throws AuthenticatorSecurityException, NoUserException, IOException, SegueDatabaseException {
        Validate.notNull(currentUser);
        Validate.notNull(federatedAuthenticator);
        Validate.notEmpty(providerSpecificUserLookupReference);

        // get user info from federated provider
        UserFromAuthProvider userFromProvider = federatedAuthenticator
                .getUserInfo(providerSpecificUserLookupReference);

        this.database.linkAuthProviderToAccount(currentUser, federatedAuthenticator.getAuthenticationProvider(),
                userFromProvider.getProviderUserId());

    }

    /**
     * This method is an oauth2 specific method which will ultimately provide an internal reference number that the
     * oauth2 provider can use to lookup the information of the user who has just authenticated.
     * 
     * @param oauthProvider
     *            - The provider to authenticate against.
     * @param request
     *            - The request that will contain session information.
     * @return an internal reference number that will allow retrieval of the users information from the provider.
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
    private String getOauthInternalRefCode(final IOAuthAuthenticator oauthProvider, final HttpServletRequest request)
            throws AuthenticationCodeException, IOException, CodeExchangeException, NoUserException,
            CrossSiteRequestForgeryException {
        // verify there is no cross site request forgery going on.
        if (request.getQueryString() == null || !ensureNoCSRF(request, oauthProvider)) {
            throw new CrossSiteRequestForgeryException("CSRF check failed");
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

        if (userToValidate.getEmail() == null || userToValidate.getEmail().isEmpty()
                || !userToValidate.getEmail().contains("@")) {
            isValid = false;
        }

        return isValid;
    }

    /**
     * Converts the sensitive UserDO into a limited DTO.
     * 
     * @param user
     *            - DO
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
     * Converts a list of userDOs into a List of userDTOs.
     * 
     * @param listToConvert
     *            - list of DOs to convert
     * @return the list of user dtos.
     */
    private List<RegisteredUserDTO> convertUserDOToUserDTOList(final List<RegisteredUser> listToConvert) {
        List<RegisteredUserDTO> result = Lists.newArrayList();
        for (RegisteredUser user : listToConvert) {
            result.add(this.convertUserDOToUserDTO(user));
        }
        return result;
    }

    /**
     * Get the RegisteredUserDO of the currently logged in user. This is for internal use only.
     * 
     * This method will validate the session as well returning null if it is invalid.
     * 
     * @param request
     *            - to retrieve session information from
     * @return Returns the current UserDTO if we can get it or null if user is not currently logged in / there is an
     *         invalid session
     */
    private RegisteredUser getCurrentRegisteredUserDO(final HttpServletRequest request) {
        Validate.notNull(request);

        Map<String, String> currentSessionInformation;
        try {
            currentSessionInformation = this.getSegueSessionFromRequest(request);
        } catch (IOException e1) {
            log.error("Error parsing session information ");
            return null;
        } catch (InvalidSessionException e) {
            log.debug("We cannot read the session information. It probably doesn't exist");
            // assuming that no user is logged in.
            return null;
        }

        // check if the users session is valid.
        if (!this.isValidUsersSession(currentSessionInformation)) {
            log.debug("User session has failed validation. Assume they are not logged in. Session: "
                    + currentSessionInformation);
            return null;
        }

        // retrieve the user from database.
        try {
            // get the current user based on their session id information
            Long currentUserId = Long.parseLong(currentSessionInformation.get(SESSION_USER_ID));
            
            // should be ok as isValidUser checks this.
            Validate.notNull(currentUserId);
            
            return database.getById(currentUserId);
        } catch (SegueDatabaseException e) {
            log.error("Internal Database error. Failed to resolve current user.", e);
            return null;
        } catch (NumberFormatException e) {
            log.error("Invalid user id detected in session.", e);
            return null;            
        }
    }

    /**
     * This method will send a message to a user explaining that they only use a federated authenticator.
     *
     * @param user
     *            - a user with the givenName, email and token fields set
     * @throws CommunicationException
     *             - if a fault occurred whilst sending the communique
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

        String providerWord = "provider";
        if (providerNames.size() > 1) {
            providerWord += "s";
        }

        try {
            emailManager.sendFederatedPasswordReset(user, providersString, providerWord);
        } catch (ContentManagerException e1) {
            log.error(String.format("Error sending federated email verification message"));
        }
    }

    /**
     * Temporarily Record Anonymous User Question Information in the anonymous user object provided.
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
    private void recordAnonymousUserQuestionInformation(final AnonymousUser anonymousUser,
            final String questionPageId, final String questionId, 
            final QuestionValidationResponseDTO questionResponse) {

        QuestionValidationResponse questionResponseDO = this.dtoMapper.map(questionResponse,
                QuestionValidationResponse.class);

        Map<String, Map<String, List<QuestionValidationResponse>>> anonymousResponses = anonymousUser
                .getTemporaryQuestionAttempts();

        if (!anonymousResponses.containsKey(questionPageId)) {
            anonymousResponses.put(questionPageId, new HashMap<String, List<QuestionValidationResponse>>());
        }

        if (!anonymousResponses.get(questionPageId).containsKey(questionId)) {
            anonymousResponses.get(questionPageId).put(questionId, new ArrayList<QuestionValidationResponse>());
        }

        // we could really create a specialised orika deserializer for this.

        // add the response to the session object
        anonymousResponses.get(questionPageId).get(questionId).add(questionResponseDO);

        log.debug("Recording anonymous question attempt in session as user is not logged in.");
    }

    /**
     * Retrieves anonymous user information if it is available.
     * 
     * @param request
     *            - request containing session information.
     * @return An anonymous user containing any anonymous question attempts (which could be none)
     */
    private AnonymousUserDTO getAnonymousUser(final HttpServletRequest request) {
        return this.dtoMapper.map(this.getAnonymousUserDO(request), AnonymousUserDTO.class);
    }

    /**
     * Retrieves anonymous user information if it is available.
     * 
     * @param request
     *            - request containing session information.
     * @return An anonymous user containing any anonymous question attempts (which could be none)
     */
    private AnonymousUser getAnonymousUserDO(final HttpServletRequest request) {
        AnonymousUser user;
        // no session exists so create one.
        if (request.getSession().getAttribute(ANONYMOUS_USER) == null) {
            user = new AnonymousUser(request.getSession().getId());
            user.setDateCreated(new Date());
            // add the user reference to the session
            request.getSession().setAttribute(ANONYMOUS_USER, user.getSessionId());
            this.temporaryUserCache.put(user.getSessionId(), user);
        } else {
            // reuse existing one
            if (request.getSession().getAttribute(ANONYMOUS_USER) instanceof String) {
                String userId = (String) request.getSession().getAttribute(ANONYMOUS_USER);
                user = this.temporaryUserCache.getIfPresent(userId);

                if (null == user) {
                    // the session must have expired. Create a new user and run this method again.
                    // this probably won't happen often as the session expiry and the cache should be timed correctly.
                    request.getSession().removeAttribute(ANONYMOUS_USER);
                    log.warn("Anonymous user session expired so creating a"
                            + " new one - this should not happen often if cache settings are correct.");
                    return this.getAnonymousUserDO(request);
                }
            } else {
                // this means that someone has put the wrong type in to the session variable.
                throw new ClassCastException("Unable to get AnonymousUser from session.");
            }
        }
        return user;
    }

    /**
     * Returns the anonymousUser if present in our cache.
     * 
     * @param id
     *            - users id
     * @return AnonymousUser or null.
     */
    private AnonymousUser findAnonymousUserDOBySessionId(final String id) {
        return this.temporaryUserCache.getIfPresent(id);
    }

    /**
     * Merges any question data stored in the session (this will only happen for anonymous users).
     * 
     * @param anonymousUser
     *            - containing the question attempts.
     * @param registeredUser
     *            - the account to merge with.
     * @throws NoUserLoggedInException
     *             - Unable to merge as the user is still anonymous.
     * @throws SegueDatabaseException
     *             - if we are unable to locate the questions attempted by this user already.
     */
    private void mergeAnonymousQuestionInformationWithUserRecord(final AnonymousUser anonymousUser,
            final RegisteredUser registeredUser) throws NoUserLoggedInException, SegueDatabaseException {
        Validate.notNull(anonymousUser, "Anonymous user must not be null when merging anonymousQuestion info");
        Validate.notNull(registeredUser, "Registered user must not be null when merging anonymousQuestion info");

        Map<String, Map<String, List<QuestionValidationResponse>>> anonymouslyAnsweredQuestions = anonymousUser
                .getTemporaryQuestionAttempts();

        this.logManager.transferLogEventsToRegisteredUser(anonymousUser.getSessionId(), registeredUser.getId()
                .toString());

        if (anonymouslyAnsweredQuestions.isEmpty()) {
            return;
        }

        log.info(String.format("Merging anonymously answered questions with known user account (%s)",
                registeredUser.getId()));

        for (String questionPageId : anonymouslyAnsweredQuestions.keySet()) {
            for (String questionId : anonymouslyAnsweredQuestions.get(questionPageId).keySet()) {
                for (QuestionValidationResponse questionResponse : anonymouslyAnsweredQuestions.get(questionPageId)
                        .get(questionId)) {
                    QuestionValidationResponseDTO questionRespnseDTO = this.dtoMapper.map(questionResponse,
                            QuestionValidationResponseDTO.class);
                    this.recordQuestionAttempt(this.dtoMapper.map(registeredUser, RegisteredUserDTO.class),
                            questionRespnseDTO);
                }
            }
        }

        this.logManager.logInternalEvent(this.convertUserDOToUserDTO(registeredUser), MERGE_USER,
                ImmutableMap.of("oldAnonymousUserId", anonymousUser.getSessionId()));

        // delete the session attribute as merge has completed.
        this.temporaryUserCache.invalidate(anonymousUser.getSessionId());
    }

    /**
     * This method will extract the segue session information from a given request.
     * 
     * @param request
     *            - possibly containing a segue cookie.
     * @return The segue session information (unchecked or validated)
     * @throws IOException
     *             - problem parsing session information.
     * @throws InvalidSessionException
     *             - if there is no session set or if it is not valid.
     */
    private Map<String, String> getSegueSessionFromRequest(final HttpServletRequest request) throws IOException,
            InvalidSessionException {
        Cookie segueAuthCookie = null;
        if (request.getCookies() == null) {
            throw new InvalidSessionException("There are no cookies set.");
        }

        for (Cookie c : request.getCookies()) {
            if (c.getName().equals(SEGUE_AUTH_COOKIE)) {
                segueAuthCookie = c;
            }
        }

        if (null == segueAuthCookie) {
            throw new InvalidSessionException("There are no cookies set.");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> sessionInformation = this.serializationMapper.readValue(segueAuthCookie.getValue(),
                HashMap.class);

        return sessionInformation;
    }

    /**
     * Update the users' last seen field.
     * 
     * @param user
     *            of interest
     * @throws SegueDatabaseException
     *             - if an error occurs with the update.
     */
    private void updateLastSeen(final RegisteredUser user) throws SegueDatabaseException {
        if (user.getLastSeen() == null) {
            this.database.updateUserLastSeen(user);
        } else {
            // work out if we should update the user record again...
            long timeDiff = Math.abs(new Date().getTime() - user.getLastSeen().getTime());
            long minutesElapsed = TimeUnit.MILLISECONDS.toMinutes(timeDiff);
            if (minutesElapsed > LAST_SEEN_UPDATE_FREQUENCY_MINUTES) {
                this.database.updateUserLastSeen(user);
            }
        }
    }

}