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
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IPasswordAuthenticator;
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
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.DetailedUserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Lists;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * This class is responsible for managing all user data and orchestration of calls to a user Authentication Manager for
 * dealing with sessions and passwords.
 */
public class UserAccountManager {
    private static final Logger log = LoggerFactory.getLogger(UserAccountManager.class);

    private final IUserDataManager database;
    private final QuestionManager questionAttemptDb;
    private final ILogManager logManager;
    private final MapperFacade dtoMapper;
    private final EmailManager emailManager;
    
    private final Cache<String, AnonymousUser> temporaryUserCache;
    private final Map<AuthenticationProvider, IAuthenticator> registeredAuthProviders;
    private final UserAuthenticationManager userAuthenticationManager;

    /**
     * Create an instance of the user manager class.
     * 
     * @param database
     *            - an IUserDataManager that will support persistence.
     * @param questionDb
     *            - allows this class to instruct the questionDB to merge an anonymous user with a registered user.  
     * @param properties
     *            - A property loader
     * @param providersToRegister
     *            - A map of known authentication providers.
     * @param dtoMapper
     *            - the preconfigured DO to DTO object mapper for user objects.
     * @param emailQueue
     *            - the preconfigured communicator manager for sending e-mails.
     * @param logManager
     *            - so that we can log events for users.
     * @param userAuthenticationManager
     *            - Class responsible for handling sessions, passwords and linked accounts.
     */
    @Inject
    public UserAccountManager(final IUserDataManager database, final QuestionManager questionDb,
            final PropertiesLoader properties, final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
            final MapperFacade dtoMapper, final EmailManager emailQueue, final ILogManager logManager,
            final UserAuthenticationManager userAuthenticationManager) {
        this(database, questionDb, properties, providersToRegister, dtoMapper, emailQueue, CacheBuilder.newBuilder()
                .expireAfterAccess(ANONYMOUS_SESSION_DURATION_IN_MINUTES, TimeUnit.MINUTES)
                .<String, AnonymousUser> build(), logManager, userAuthenticationManager);
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
     * @param userAuthenticationManager
     *            - Class responsible for handling sessions, passwords and linked accounts.
     */
    public UserAccountManager(final IUserDataManager database, final QuestionManager questionDb,
            final PropertiesLoader properties, final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
            final MapperFacade dtoMapper, final EmailManager emailQueue,
            final Cache<String, AnonymousUser> temporaryUserCache, final ILogManager logManager,
            final UserAuthenticationManager userAuthenticationManager) {
        Validate.notNull(properties.getProperty(HMAC_SALT));
        Validate.notNull(Integer.parseInt(properties.getProperty(SESSION_EXPIRY_SECONDS)));
        Validate.notNull(properties.getProperty(HOST_NAME));

        this.database = database;
        this.questionAttemptDb = questionDb;
        this.temporaryUserCache = temporaryUserCache;
        this.logManager = logManager;

        this.registeredAuthProviders = providersToRegister;
        this.dtoMapper = dtoMapper;

        this.emailManager = emailQueue;

        this.userAuthenticationManager = userAuthenticationManager;
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
        return this.userAuthenticationManager.getThirdPartyAuthURI(request, provider);
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

        return this.userAuthenticationManager.getThirdPartyAuthURI(request, provider);
    }

    /**
     * Authenticate Callback will receive the authentication information from the different provider types. (e.g. OAuth
     * 2.0 (IOAuth2Authenticator) or bespoke)
     * 
     * This method will either register a new user and attach the linkedAccount or locate the existing account of the
     * user and create a session for that.
     * 
     * @param request
     *            - http request from the user - should contain url encoded token details.
     * @param response
     *            to store the session in our own segue cookie.
     * @param provider
     *            - the provider who has just authenticated the user.
     * @return Response containing the user object. Alternatively a SegueErrorResponse could be returned.
     * @throws AuthenticationProviderMappingException
     *             - if we cannot locate an appropriate authenticator.
     * @throws SegueDatabaseException
     *             - if there is a local database error.
     * @throws IOException
     *             - Problem reading something
     * @throws NoUserException
     *             - If the user doesn't exist with the provider.
     * @throws AuthenticatorSecurityException
     *             - If there is a security probably with the authenticator.
     * @throws CrossSiteRequestForgeryException
     *             - as per exception description.
     * @throws CodeExchangeException
     *             - as per exception description.
     * @throws AuthenticationCodeException
     *             - as per exception description.
     */
    public RegisteredUserDTO authenticateCallback(final HttpServletRequest request,
            final HttpServletResponse response, final String provider) throws AuthenticationProviderMappingException,
            AuthenticatorSecurityException, NoUserException, IOException, SegueDatabaseException,
            AuthenticationCodeException, CodeExchangeException, CrossSiteRequestForgeryException {
        IAuthenticator authenticator = this.userAuthenticationManager.mapToProvider(provider);
        // get the auth provider user data.
        UserFromAuthProvider providerUserDO = this.userAuthenticationManager.getThirdPartyUserInformation(request,
                provider);

        // if the UserFromAuthProvider exists then this is a login request so process it.
        RegisteredUser userFromLinkedAccount = this.userAuthenticationManager.getSegueUserFromLinkedAccount(
                authenticator.getAuthenticationProvider(), providerUserDO.getProviderUserId());
        if (userFromLinkedAccount != null) {
            return this.logUserIn(request, response, userFromLinkedAccount);
        }

        RegisteredUser currentUser = getCurrentRegisteredUserDO(request);
        // if the user is currently logged in and this is a request for a linked account, then create the new link.
        if (null != currentUser) {
            Boolean intentionToLinkRegistered = (Boolean) request.getSession().getAttribute(LINK_ACCOUNT_PARAM_NAME);
            if (intentionToLinkRegistered == null || !intentionToLinkRegistered) {
                throw new SegueDatabaseException("User is already authenticated - "
                        + "expected request to link accounts but none was found.");
            }
            
            List<AuthenticationProvider> usersProviders = this.database.getAuthenticationProvidersByUser(currentUser);
            if (!usersProviders.contains(authenticator.getAuthenticationProvider())) {
                // create linked account
                this.userAuthenticationManager.linkProviderToExistingAccount(currentUser,
                        authenticator.getAuthenticationProvider(), providerUserDO);
                // clear link accounts intention until next time
                request.removeAttribute(LINK_ACCOUNT_PARAM_NAME);
            }

            return this.convertUserDOToUserDTO(getCurrentRegisteredUserDO(request));
        } else {
            // this must be a registration request
            RegisteredUser segueUserDO = this.registerUserWithFederatedProvider(
                    authenticator.getAuthenticationProvider(), providerUserDO);
            RegisteredUserDTO segueUserDTO = this.logUserIn(request, response, segueUserDO);
            segueUserDTO.setFirstLogin(true);
            
            try {
                emailManager.sendFederatedRegistrationConfirmation(segueUserDTO, provider);
            } catch (ContentManagerException e) {
                log.error("Registration email could not be sent due to content issue: " + e.getMessage());
            }
            
            return segueUserDTO;
        }
    }

    /**
     * This method will attempt to authenticate the user using the provided credentials and if successful will log the
     * user in and create a session.
     * 
     * @param request
     *            - http request that we can attach the session to.
     * @param response
     *            to store the session in our own segue cookie.
     * @param provider
     *            - the provider the user wishes to authenticate with.
     * @param email
     *            - the email address of the account holder.
     * @param password
     *            - the plain text password.
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
            final HttpServletResponse response, final String provider, final String email, final String password)
            throws AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, NoUserException,
            NoCredentialsAvailableException, SegueDatabaseException {
        Validate.notBlank(email);
        Validate.notBlank(password);

        // get the current user based on their session id information.
        RegisteredUserDTO currentUser = this.convertUserDOToUserDTO(this.getCurrentRegisteredUserDO(request));
        if (null != currentUser) {
            return currentUser;
        }

        RegisteredUser user = this.userAuthenticationManager.getSegueUserFromCredentials(provider, email, password);

        return this.logUserIn(request, response, user);
    }
    
    /**
     * Utility method to ensure that the credentials provided are the current correct ones. If they are invalid an
     * exception will be thrown otherwise nothing will happen.
     * 
     * @param provider
     *            - the password provider who will validate the credentials.
     * @param email
     *            - the email address of the account holder.
     * @param password
     *            - the plain text password.
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
    public void ensureCorrectPassword(final String provider, final String email, final String password)
            throws AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, NoUserException,
            NoCredentialsAvailableException, SegueDatabaseException {

        // this method will throw an error if the credentials are incorrect.
        this.userAuthenticationManager.getSegueUserFromCredentials(provider, email, password);
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
        this.userAuthenticationManager.unlinkUserAndProvider(userDO, providerString);
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
    public final List<RegisteredUserDTO> findUsers(final List<Long> userIds) throws SegueDatabaseException {
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
            return this.getAnonymousUserDTO(request);
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
        this.userAuthenticationManager.destroyUserSession(request, response);
    }

    /**
     * Method to create a user object in our database and log them in.
     *
     * Note: this method is intended for creation of accounts in segue - not for linked account registration.
     * 
     * @param request
     *            to enable access to anonymous user information.
     * @param response
     *            to store the session in our own segue cookie.
     * @param user
     *            - the user DO to use for updates - must not contain a user id.
     * @param newPassword
     *            - new password for the account being created.
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
     * @throws EmailMustBeVerifiedException
     *             - if a user attempts to sign up with an email that must be verified before it can be used
     *             (i.e. an @isaacphysics.org or @isaacchemistry.org address).
     */
    public RegisteredUserDTO createUserObjectAndSession(final HttpServletRequest request,
            final HttpServletResponse response, final RegisteredUser user, final String newPassword) throws InvalidPasswordException,
            MissingRequiredFieldException, SegueDatabaseException, AuthenticationProviderMappingException,
            EmailMustBeVerifiedException {
        Validate.isTrue(user.getId() == null,
                "When creating a new user the user id must not be set.");

        if (this.findUserByEmail(user.getEmail()) != null) {
            throw new DuplicateAccountException("An account with that e-mail address already exists.");
        }

        // Ensure nobody registers with Isaac email addresses. Users can change emails by verifying them however.
        if (user.getEmail().matches(".*@isaac(physics|chemistry|biology|science)\\.org")) {
            log.warn("User attempted to register with Isaac email address '" + user.getEmail() + "'!");
            throw new EmailMustBeVerifiedException("You cannot register with an Isaac email address.");
        }

        RegisteredUser userToSave = null;
        MapperFacade mapper = this.dtoMapper;

        // We want to map to DTO first to make sure that the user cannot
        // change fields that aren't exposed to them
        RegisteredUserDTO userDtoForNewUser = mapper.map(user, RegisteredUserDTO.class);

        // This is a new registration
        userToSave = mapper.map(userDtoForNewUser, RegisteredUser.class);

        // Set defaults
        userToSave.setRole(Role.STUDENT);
        userToSave.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        userToSave.setRegistrationDate(new Date());
        userToSave.setLastUpdated(new Date());

        // Before save we should validate the user for mandatory fields.
        if (!this.isUserValid(userToSave)) {
            throw new MissingRequiredFieldException("The user provided is missing a mandatory field");
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

        // create password for the user
        authenticator.setOrChangeUsersPassword(userToReturn, newPassword);

        // send an email confirmation and set up verification
        try {
        	RegisteredUserDTO userToReturnDTO = this.getUserDTOById(userToReturn.getId());
            emailManager.sendRegistrationConfirmation(userToReturnDTO, userToReturn.getEmailVerificationToken());
        } catch (ContentManagerException e) {
            log.error("Registration email could not be sent due to content issue: " + e.getMessage());
        } catch (NoUserException e) {
            log.error("Registration email could not be sent due to not being able to locate the user: " + e.getMessage());
		}

        // save the user again with updated token
        //TODO: do we need this?
        userToReturn = this.database.createOrUpdateUser(userToReturn);

        logManager.logInternalEvent(this.convertUserDOToUserDTO(userToReturn), Constants.USER_REGISTRATION,
                ImmutableMap.builder().put("provider", AuthenticationProvider.SEGUE.name()).build());

        // return it to the caller.
        return this.logUserIn(request, response, userToReturn);
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
    public RegisteredUserDTO updateUserObject(final RegisteredUser user, final String newPassword) throws InvalidPasswordException,
            MissingRequiredFieldException, SegueDatabaseException, AuthenticationProviderMappingException {
        Validate.notNull(user.getId());

        // We want to map to DTO first to make sure that the user cannot
        // change fields that aren't exposed to them
        RegisteredUserDTO userDTOContainingUpdates = this.dtoMapper.map(user, RegisteredUserDTO.class);
        if (user.getId() == null) {
            throw new IllegalArgumentException(
                    "The user object specified does not have an id. Users cannot be updated without a specific id set.");
        }

        // This is an update operation.
        final RegisteredUser existingUser = this.findUserById(user.getId());

        // Check that the user isn't trying to take an existing users e-mail.
        if (this.findUserByEmail(user.getEmail()) != null && !existingUser.getEmail().equals(user.getEmail())) {
            throw new DuplicateAccountException("An account with that e-mail address already exists.");
        }

        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        // Send a new verification email if the user has changed their email
        if (!existingUser.getEmail().equals(user.getEmail())) {
            try {
                authenticator.createEmailVerificationTokenForUser(existingUser, user.getEmail());

                RegisteredUserDTO existingUserDTO = this.getUserDTOById(existingUser.getId());
                this.emailManager.sendEmailVerificationChange(existingUserDTO, user);

                log.info(String.format("Sending email for email address change for user (%s)"
                                + " from email (%s) to email (%s)", user.getId(),
                        existingUser.getEmail(), user.getEmail()));

            } catch (ContentManagerException | NoUserException e) {
                log.error("ContentManagerException during sendEmailVerificationChange " + e.getMessage());
			} catch (NoSuchAlgorithmException | InvalidKeySpecException e1) {
                log.error("Creation of email verification token failed: " + e1.getMessage());
            }
        }

        // Send a welcome email if the user has become a teacher
        try {
            RegisteredUserDTO existingUserDTO = this.getUserDTOById(existingUser.getId());
            if (user.getRole() != existingUser.getRole()) {
                switch (user.getRole()) {
                    case TEACHER:
                        this.emailManager.sendTeacherWelcome(existingUserDTO);
                        break;
                    default:
                        this.emailManager.sendRoleChange(existingUserDTO, user.getRole());
                        break;
                }
            }
        } catch (ContentManagerException | NoUserException e) {
            log.error("ContentManagerException during sendTeacherWelcome " + e.getMessage());
        }

        MapperFacade mergeMapper = new DefaultMapperFactory.Builder().mapNulls(false).build().getMapperFacade();

        RegisteredUser userToSave = new RegisteredUser();
        mergeMapper.map(existingUser, userToSave);
        mergeMapper.map(userDTOContainingUpdates, userToSave);
        userToSave.setRegistrationDate(existingUser.getRegistrationDate());
        userToSave.setLastUpdated(new Date());

        if (user.getSchoolId() == null && existingUser.getSchoolId() != null) {
            userToSave.setSchoolId(null);
        }
        // Correctly remove school_other when it is set to be the empty string:
        if (user.getSchoolOther() == null || user.getSchoolOther().isEmpty()) {
            userToSave.setSchoolOther(null);
        }

        // Before save we should validate the user for mandatory fields.
        if (!this.isUserValid(userToSave)) {
            throw new MissingRequiredFieldException("The user provided is missing a mandatory field");
        }

        // Make sure the email address is preserved (can't be changed until new email is verified)
        if (!userToSave.getEmail().equals(existingUser.getEmail())) {
            try {
                RegisteredUserDTO userToSaveDTO = this.dtoMapper.map(userToSave, RegisteredUserDTO.class);
                this.emailManager.sendEmailVerification(userToSaveDTO, userToSave.getEmailVerificationToken());
            } catch (ContentManagerException e) {
                log.error("ContentManagerException during sendEmailVerification " + e.getMessage());
            } catch (NoUserException e) {
                log.debug("No user found exception " + e.getMessage());
			}
            userToSave.setEmail(existingUser.getEmail());
        }

        // If the school has changed, update it. Check this using Objects.equals() to be null safe!
        if (!Objects.equals(userToSave.getSchoolId(), existingUser.getSchoolId())
                || !Objects.equals(userToSave.getSchoolOther(), existingUser.getSchoolOther())) {
            LinkedHashMap<String, String> eventDetails = new LinkedHashMap<>();
            eventDetails.put("oldSchoolId", existingUser.getSchoolId());
            eventDetails.put("newSchoolId", userToSave.getSchoolId());
            eventDetails.put("oldSchoolOther", existingUser.getSchoolOther());
            eventDetails.put("newSchoolOther", userToSave.getSchoolOther());

            logManager.logInternalEvent(this.convertUserDOToUserDTO(userToSave), Constants.USER_SCHOOL_CHANGE,
                    eventDetails);
        }

        // save the user and password
        RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);
        if (null != newPassword && !newPassword.isEmpty()) {
            authenticator.setOrChangeUsersPassword(userToReturn, newPassword);
        }

        // return it to the caller
        return this.convertUserDOToUserDTO(userToReturn);
    }

    /**
     * @param id
     *            - the user id
     * @param requestedRole
     *            - the new role
     * @throws SegueDatabaseException
     *             - an exception when accessing the database
     */
    public void updateUserRole(final Long id, final Role requestedRole) throws SegueDatabaseException {
        Validate.notNull(requestedRole);
        RegisteredUser userToSave = this.findUserById(id);

        // Send welcome email if user has become teacher, otherwise, role change notification
        try {
            RegisteredUserDTO existingUserDTO = this.getUserDTOById(id);
            if (userToSave.getRole() != requestedRole) {
                switch (requestedRole) {
                    case TEACHER:
                        this.emailManager.sendTeacherWelcome(existingUserDTO);
                        break;
                    default:
                        this.emailManager.sendRoleChange(existingUserDTO, requestedRole);
                        break;
                }
            }
        } catch (ContentManagerException | NoUserException e) {
            log.debug("ContentManagerException during sendTeacherWelcome " + e.getMessage());
        }

        userToSave.setRole(requestedRole);
        this.database.createOrUpdateUser(userToSave);
    }

    /**
     * @param email
     *            - the user email
     * @param requestedEmailVerificationStatus
     *            - the new email verification status
     * @throws SegueDatabaseException
     *             - an exception when accessing the database
     */
    public void updateUserEmailVerificationStatus(final String email, 
            final EmailVerificationStatus requestedEmailVerificationStatus) throws SegueDatabaseException {
        Validate.notNull(requestedEmailVerificationStatus);
        RegisteredUser userToSave = this.findUserByEmail(email);
        if (null == userToSave) {
            log.warn(String.format(
                    "Could not update email verification status of email address (%s) - does not exist",
                    email));
            return;
        }
        userToSave.setEmailVerificationStatus(requestedEmailVerificationStatus);
        this.database.createOrUpdateUser(userToSave);
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
            NoSuchAlgorithmException, CommunicationException, SegueDatabaseException, NoUserException {
        RegisteredUser user = this.findUserByEmail(userObject.getEmail());

        if (null == user) {
            throw new NoUserException();
        }

        RegisteredUserDTO userDTO = this.convertUserDOToUserDTO(user);
        this.userAuthenticationManager.resetPasswordRequest(user, userDTO);
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

        // TODO: Email verification stuff does not belong in the password authenticator... It should be moved.
        // Generate token
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        user = authenticator.createEmailVerificationTokenForUser(user, email);

        // Save user object
        this.database.createOrUpdateUser(user);

        log.info(String.format("Sending password reset message to %s", user.getEmail()));
        try {
        	RegisteredUserDTO userDTO = this.getUserDTOById(user.getId());
            this.emailManager.sendEmailVerification(userDTO, user.getEmailVerificationToken());
        } catch (ContentManagerException e) {
            log.debug("ContentManagerException " + e.getMessage());
        } catch (NoUserException e) {
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

        return authenticator.isValidResetToken(token);
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
    public RegisteredUserDTO processEmailVerification(final Long userid, final String email, final String token) 
            throws SegueDatabaseException, InvalidTokenException, NoUserException {
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        RegisteredUser user = this.findUserById(userid);

        if (null == user) {
            log.warn(String.format("Recieved an invalid email token request for (%s)", email));
            throw new NoUserException();    
        }

        if (!userid.equals(user.getId())) {
            log.warn(String.format("Recieved an invalid email token request for (%s)" + " - provided bad userid",
                    email));
            throw new InvalidTokenException();
        }

        EmailVerificationStatus evStatus = user.getEmailVerificationStatus();
        if (evStatus != null && evStatus == EmailVerificationStatus.VERIFIED && user.getEmail().equals(email)) {
            log.warn(String
                    .format("Recieved a duplicate email verification request for (%s) - already verified", email));
            return this.convertUserDOToUserDTO(user);
        }

        if (authenticator.isValidEmailVerificationToken(user, email, token)) {
            user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
            user.setEmailVerificationToken(null);

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
     * @param newPassword
     *            - the supplied password
     * @return the user which has had the password reset.
     * @throws InvalidTokenException
     *             - If the token provided is invalid.
     * @throws InvalidPasswordException
     *             - If the password provided is invalid.
     * @throws SegueDatabaseException
     *             - If there is an internal database error.
     */
    public RegisteredUserDTO resetPassword(final String token, final String newPassword)
            throws InvalidTokenException, InvalidPasswordException, SegueDatabaseException {
        return this.convertUserDOToUserDTO(this.userAuthenticationManager.resetPassword(token, newPassword));
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
     * Helper method to convert a user object into a cutdown detailedUserSummary DTO.
     *
     * @param userToConvert
     *            - full user object.
     * @return a summarised object with reduced personal information
     */
    public DetailedUserSummaryDTO convertToDetailedUserSummaryObject(final RegisteredUserDTO userToConvert) {
        return this.dtoMapper.map(userToConvert, DetailedUserSummaryDTO.class);
    }

    /**
     * Helper method to convert user objects into cutdown userSummary DTOs.
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
     * Helper method to convert user objects into cutdown DetailedUserSummary DTOs.
     *
     * @param userListToConvert
     *            - full user objects.
     * @return a list of summarised objects with reduced personal information
     */
    public List<DetailedUserSummaryDTO> convertToDetailedUserSummaryObjectList(final List<RegisteredUserDTO> userListToConvert) {
        Validate.notNull(userListToConvert);
        List<DetailedUserSummaryDTO> resultList = Lists.newArrayList();
        for (RegisteredUserDTO user : userListToConvert) {
            resultList.add(this.convertToDetailedUserSummaryObject(user));
        }
        return resultList;
    }

    /**
     * Logs the user in and creates the signed sessions.
     * 
     * @param request
     *            - for the session to be attached
     * @param response
     *            - for the session to be attached.
     * @param user
     *            - the user who is being logged in.
     * @return the DTO version of the user.
     */
    private RegisteredUserDTO logUserIn(final HttpServletRequest request, final HttpServletResponse response,
            final RegisteredUser user) {
        AnonymousUser anonymousUser = this.getAnonymousUserDO(request);

        // now we want to clean up any data generated by the user while they weren't logged in.
        mergeAnonymousUserWithRegisteredUser(anonymousUser, user);
        
        return this.convertUserDOToUserDTO(this.userAuthenticationManager.createUserSession(request, response, user));
    }

    /**
     * Method to migrate anonymously generated data to a persisted account.
     * 
     * @param anonymousUser
     *            to look up.
     * @param user
     *            to migrate to.
     */
    private void mergeAnonymousUserWithRegisteredUser(final AnonymousUser anonymousUser, final RegisteredUser user) {
        if (anonymousUser != null) {
            // merge any anonymous information collected with this user.
            try {
                final RegisteredUserDTO userDTO = this.convertUserDOToUserDTO(user);

                this.questionAttemptDb.mergeAnonymousQuestionAttemptsIntoRegisteredUser(
                        this.dtoMapper.map(anonymousUser, AnonymousUserDTO.class), userDTO);

                // may as well spawn a new thread to do the log migration stuff asynchronously
                // work now.
                Thread logMigrationJob = new Thread() {
                    @Override
                    public void run() {
                        // run this asynchronously as there is no need to block and it is quite slow.
                        logManager.transferLogEventsToRegisteredUser(anonymousUser.getSessionId(), user.getId()
                                .toString());

                        logManager.logInternalEvent(userDTO, MERGE_USER,
                                ImmutableMap.of("oldAnonymousUserId", anonymousUser.getSessionId()));

                        // delete the session attribute as merge has completed.
                        temporaryUserCache.invalidate(anonymousUser.getSessionId());
                    }
                };

                logMigrationJob.start();

            } catch (SegueDatabaseException e) {
                log.error("Unable to merge anonymously collected data with stored user object.", e);
            }
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
     * This method should use the provider specific reference to either register a new user or retrieve an existing
     * user.
     * 
     * @param federatedAuthenticator
     *            the federatedAuthenticator we are using for authentication
     * @param userFromProvider
     *            - the user object returned by the auth provider.
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
    private RegisteredUser registerUserWithFederatedProvider(final AuthenticationProvider federatedAuthenticator,
            final UserFromAuthProvider userFromProvider) throws AuthenticatorSecurityException, NoUserException,
            IOException, SegueDatabaseException {

        log.debug(String.format("New registration (%s) as user does not already exist.", federatedAuthenticator));

        if (null == userFromProvider) {
            log.warn("Unable to create user for the provider "
                    + federatedAuthenticator);
            throw new NoUserException();
        }

        RegisteredUser newLocalUser = this.dtoMapper.map(userFromProvider, RegisteredUser.class);
        newLocalUser.setRegistrationDate(new Date());

        // register user
        RegisteredUser newlyRegisteredUser = database.registerNewUserWithProvider(newLocalUser,
                federatedAuthenticator, userFromProvider.getProviderUserId());

        RegisteredUser localUserInformation = this.database.getById(newlyRegisteredUser.getId());

        if (null == localUserInformation) {
            // we just put it in so something has gone very wrong.
            log.error("Failed to retreive user even though we " + "just put it in the database.");
            throw new NoUserException();
        }

        logManager.logInternalEvent(this.convertUserDOToUserDTO(localUserInformation), Constants.USER_REGISTRATION,
                ImmutableMap.builder().put("provider", federatedAuthenticator.name())
                        .build());

        return localUserInformation;
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
            userDTO.setHasSegueAccount(this.userAuthenticationManager.hasLocalCredentials(user));

        } catch (SegueDatabaseException e) {
            log.error("Unable to set linked accounts or local account property for user due to a database error.");
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
        return this.userAuthenticationManager.getUserFromSession(request);
    }

    /**
     * Retrieves anonymous user information if it is available.
     * 
     * @param request
     *            - request containing session information.
     * @return An anonymous user containing any anonymous question attempts (which could be none)
     */
    private AnonymousUserDTO getAnonymousUserDTO(final HttpServletRequest request) {
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