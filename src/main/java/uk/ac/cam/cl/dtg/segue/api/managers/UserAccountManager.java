/*
 * Copyright 2014 Stephen Cummins & Nick Rogers.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.segue.api.managers;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.users.AccountDeletionToken;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.TOTPSharedSecret;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserAuthenticationSettings;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.EmailTemplateDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserAuthenticationSettingsDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.IAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.IPasswordAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.ISecondFactorAuthenticator;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidNameException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidSessionException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.UnknownCountryCodeException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IAnonymousUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.text.WordUtils.capitalizeFully;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * This class is responsible for managing all user data and orchestration of calls to a user Authentication Manager for
 * dealing with sessions and passwords.
 */
public class UserAccountManager implements IUserAccountManager {
    private static final Logger log = LoggerFactory.getLogger(UserAccountManager.class);

    private final IUserDataManager database;
    private final QuestionManager questionAttemptDb;
    private final ILogManager logManager;
    private final MapperFacade dtoMapper;
    private final EmailManager emailManager;

    private final IAnonymousUserDataManager temporaryUserCache;

    private final Map<AuthenticationProvider, IAuthenticator> registeredAuthProviders;
    private final UserAuthenticationManager userAuthenticationManager;
    private final AbstractConfigLoader properties;

    private final ISecondFactorAuthenticator secondFactorManager;

    private final AbstractUserPreferenceManager userPreferenceManager;

    private final Pattern restrictedSignupEmailRegex;
    private static final int USER_NAME_MAX_LENGTH = 255;
    private static final Pattern USER_NAME_FORBIDDEN_CHARS_REGEX = Pattern.compile("[*<>]");

    /**
     * Create an instance of the user manager class.
     *
     * @param database                  - an IUserDataManager that will support persistence.
     * @param questionDb                - for merging an anonymous user with a registered user.
     * @param properties                - A property loader
     * @param providersToRegister       - A map of known authentication providers.
     * @param dtoMapper                 - the preconfigured DO to DTO object mapper for user objects.
     * @param emailQueue                - the preconfigured communicator manager for sending e-mails.
     * @param temporaryUserCache        - temporary user cache for anonymous users
     * @param logManager                - so that we can log events for users.
     * @param userAuthenticationManager
     * @param secondFactorManager
     * @param userPreferenceManager     - Allows user preferences to be managed.
     */
    @Inject
    public UserAccountManager(final IUserDataManager database, final QuestionManager questionDb, final AbstractConfigLoader properties,
                              final Map<AuthenticationProvider, IAuthenticator> providersToRegister, final MapperFacade dtoMapper,
                              final EmailManager emailQueue, final IAnonymousUserDataManager temporaryUserCache,
                              final ILogManager logManager, final UserAuthenticationManager userAuthenticationManager,
                              final ISecondFactorAuthenticator secondFactorManager,
                              final AbstractUserPreferenceManager userPreferenceManager) {

        Objects.requireNonNull(properties.getProperty(HMAC_SALT));
        Objects.requireNonNull(properties.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT));
        Objects.requireNonNull(properties.getProperty(SESSION_EXPIRY_SECONDS_REMEMBERED));
        Objects.requireNonNull(properties.getProperty(HOST_NAME));

        this.properties = properties;

        this.database = database;
        this.questionAttemptDb = questionDb;
        this.temporaryUserCache = temporaryUserCache;

        this.logManager = logManager;

        this.registeredAuthProviders = providersToRegister;
        this.dtoMapper = dtoMapper;

        this.emailManager = emailQueue;
        this.userAuthenticationManager = userAuthenticationManager;
        this.secondFactorManager = secondFactorManager;
        this.userPreferenceManager = userPreferenceManager;

        String forbiddenEmailRegex = properties.getProperty(RESTRICTED_SIGNUP_EMAIL_REGEX);
        if (null == forbiddenEmailRegex || forbiddenEmailRegex.isEmpty()) {
            this.restrictedSignupEmailRegex = null;
        } else {
            this.restrictedSignupEmailRegex = Pattern.compile(forbiddenEmailRegex);
        }
    }

    /**
     * This method will start the authentication process and ultimately provide a url for the client to redirect the
     * user to. This url will be for a 3rd party authenticator who will use the callback method provided after they have
     * authenticated.
     *
     * <p>Users who are already logged in will be returned their UserDTO without going through the authentication
     * process.
     *
     * @param request  - http request that we can attach the session to and save redirect url in.
     * @param provider - the provider the user wishes to authenticate with.
     * @param isSignUp - whether this is an initial sign-up, which may be used to direct the client to a sign-up flow on the IdP.
     * @return a URI for redirection
     * @throws IOException                            -
     * @throws AuthenticationProviderMappingException - as per exception description.
     */
    public URI authenticate(final HttpServletRequest request, final String provider, final boolean isSignUp)
            throws IOException, AuthenticationProviderMappingException {
        return this.userAuthenticationManager.getThirdPartyAuthURI(request, provider, isSignUp);
    }

    /**
     * This method will start the authentication process for linking a user to a 3rd party provider. It will ultimately
     * provide a url for the client to redirect the user to. This url will be for a 3rd party authenticator who will use
     * the callback method provided after they have authenticated.
     *
     * <p>Users must already be logged in to use this method otherwise a 401 will be returned.
     *
     * @param request  - http request that we can attach the session to.
     * @param provider - the provider the user wishes to authenticate with.
     * @return A redirection URI - also this endpoint ensures that the request has a session attribute on so we know
     *      that this is a link request not a new user.
     * @throws IOException                            -
     * @throws AuthenticationProviderMappingException - as per exception description.
     */
    public URI initiateLinkAccountToUserFlow(final HttpServletRequest request, final String provider)
            throws IOException, AuthenticationProviderMappingException {
        // record our intention to link an account.
        request.getSession().setAttribute(LINK_ACCOUNT_PARAM_NAME, Boolean.TRUE);

        return this.userAuthenticationManager.getThirdPartyAuthURI(request, provider, false);
    }

    /**
     * Authenticate Callback will receive the authentication information from the different provider types. (e.g. OAuth
     * 2.0 (IOAuth2Authenticator) or bespoke)
     *
     * <p>This method will either register a new user and attach the linkedAccount or locate the existing account of the
     * user and create a session for that.
     *
     * @param request    - http request from the user - should contain url encoded token details.
     * @param response   to store the session in our own segue cookie.
     * @param provider   - the provider who has just authenticated the user.
     * @param rememberMe - Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @return Response containing the user object. Alternatively a SegueErrorResponse could be returned.
     * @throws AuthenticationProviderMappingException - if we cannot locate an appropriate authenticator.
     * @throws SegueDatabaseException                 - if there is a local database error.
     * @throws IOException                            - Problem reading something
     * @throws NoUserException                        - If the user doesn't exist with the provider.
     * @throws AuthenticatorSecurityException         - If there is a security probably with the authenticator.
     * @throws CrossSiteRequestForgeryException       - as per exception description.
     * @throws CodeExchangeException                  - as per exception description.
     * @throws AuthenticationCodeException            - as per exception description.
     */
    public RegisteredUserDTO authenticateCallback(final HttpServletRequest request,
                                                  final HttpServletResponse response, final String provider, final boolean rememberMe)
            throws AuthenticationProviderMappingException,
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
            return this.logUserIn(request, response, userFromLinkedAccount, rememberMe);
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
            if (providerUserDO.getEmail() != null && !providerUserDO.getEmail().isEmpty() && this.findUserByEmail(providerUserDO.getEmail()) != null) {
                log.warn("A user tried to use unknown provider '" + capitalizeFully(provider)
                        + "' to log in to an account with matching email (" + providerUserDO.getEmail() + ").");
                throw new DuplicateAccountException("You do not use " + authenticator.getFriendlyName() + " to log in."
                        + " You may have registered using a different provider, or your email address and password.");
            }
            // this must be a registration request
            RegisteredUser segueUserDO = this.registerUserWithFederatedProvider(
                    authenticator.getAuthenticationProvider(), providerUserDO);
            RegisteredUserDTO segueUserDTO = this.logUserIn(request, response, segueUserDO, rememberMe);
            segueUserDTO.setFirstLogin(true);

            try {
                ImmutableMap<String, Object> emailTokens = ImmutableMap.of("provider", authenticator.getFriendlyName());

                emailManager.sendTemplatedEmailToUser(segueUserDTO,
                        emailManager.getEmailTemplateDTO("email-template-registration-confirmation-federated"),
                        emailTokens, EmailType.SYSTEM);

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
     * @param request    - http request that we can attach the session to.
     * @param response   to store the session in our own segue cookie.
     * @param provider   - the provider the user wishes to authenticate with.
     * @param email      - the email address of the account holder.
     * @param password   - the plain text password.
     * @param rememberMe - Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @return A response containing the UserDTO object or a SegueErrorResponse.
     * @throws AuthenticationProviderMappingException    - if we cannot find an authenticator
     * @throws IncorrectCredentialsProvidedException     - if the password is incorrect
     * @throws NoUserException                           - if the user does not exist
     * @throws NoCredentialsAvailableException           - If the account exists but does not have a local password
     * @throws AdditionalAuthenticationRequiredException - If the account has 2FA enabled and we need to initiate that flow
     * @throws MFARequiredButNotConfiguredException      - If the account type requires 2FA to be configured but none is enabled for the account
     * @throws SegueDatabaseException                    - if there is a problem with the database.
     */
    public final RegisteredUserDTO authenticateWithCredentials(final HttpServletRequest request,
                                                               final HttpServletResponse response, final String provider, final String email, final String password, final boolean rememberMe)
            throws AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, NoUserException,
            NoCredentialsAvailableException, SegueDatabaseException, AdditionalAuthenticationRequiredException, MFARequiredButNotConfiguredException, InvalidKeySpecException, NoSuchAlgorithmException, EmailMustBeVerifiedException {
        Validate.notBlank(email);
        Validate.notBlank(password);

        // get the current user based on their session id information.
        RegisteredUserDTO currentUser = this.convertUserDOToUserDTO(this.getCurrentRegisteredUserDO(request));
        if (null != currentUser) {
            log.debug(String.format("UserId (%s) already has a valid session - not bothering to reauthenticate",
                    currentUser.getId()));
            return currentUser;
        }

        RegisteredUser user = this.userAuthenticationManager.getSegueUserFromCredentials(provider, email, password);
        log.debug(String.format("UserId (%s) authenticated with credentials", user.getId()));

        // check if user has MFA enabled, if so we can't just log them in - also they won't have the correct cookie
        if (secondFactorManager.has2FAConfigured(convertUserDOToUserDTO(user))) {
            // we can't just log them in we have to set a caveat cookie
            this.logUserInWithCaveats(request, response, user, rememberMe, Set.of(AuthenticationCaveat.INCOMPLETE_MFA_CHALLENGE));
            throw new AdditionalAuthenticationRequiredException();
        } else if (Role.ADMIN.equals(user.getRole())) {
            // Admins MUST have 2FA enabled to use password login, so if we reached this point login cannot proceed.
            String message = "Your account type requires 2FA, but none has been configured! "
                    + "Please ask an admin to demote your account to regain access.";
            throw new MFARequiredButNotConfiguredException(message);
        } else if (Role.TEACHER.equals(user.getRole()) && user.getTeacherAccountPending()) {
            this.logUserInWithCaveats(request, response, user, rememberMe, Set.of(AuthenticationCaveat.INCOMPLETE_MANDATORY_EMAIL_VERIFICATION));
            throw new EmailMustBeVerifiedException();
        } else {
            return this.logUserIn(request, response, user, rememberMe);
        }
    }

    /**
     * Create a user object. This method allows new user objects to be created.
     *
     * @param request              - so that we can identify the user
     * @param response             to tell the browser to store the session in our own segue cookie.
     * @param userObjectFromClient - the new user object from the clients perspective.
     * @param newPassword          - the new password for the user.
     * @param userPreferenceObject - the new preferences for this user
     * @param rememberMe           - Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @return the updated user object.
     */
    public Response createUserObjectAndLogIn(final HttpServletRequest request, final HttpServletResponse response,
                                             final RegisteredUser userObjectFromClient, final String newPassword,
                                             final Map<String, Map<String, Boolean>> userPreferenceObject,
                                             final boolean rememberMe) throws InvalidKeySpecException, NoSuchAlgorithmException {
        return createUserObjectAndLogIn(request, response, userObjectFromClient, newPassword, userPreferenceObject, rememberMe, Set.of());
    }

    /**
     * Create a user object. This method allows new user objects to be created.
     *
     * @param request              - so that we can identify the user
     * @param response             to tell the browser to store the session in our own segue cookie.
     * @param userObjectFromClient - the new user object from the clients perspective.
     * @param newPassword          - the new password for the user.
     * @param userPreferenceObject - the new preferences for this user
     * @param rememberMe           - Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @param sessionCaveats       - any caveats to include in the returned session cookie
     * @return the updated user object.
     */
    public Response createUserObjectAndLogIn(final HttpServletRequest request, final HttpServletResponse response,
                                             final RegisteredUser userObjectFromClient, final String newPassword,
                                             final Map<String, Map<String, Boolean>> userPreferenceObject,
                                             final boolean rememberMe, final Set<AuthenticationCaveat> sessionCaveats)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        try {
            RegisteredUserDTO savedUser = this.createUserObjectAndSession(request, response,
                    userObjectFromClient, newPassword, rememberMe, sessionCaveats);

            if (userPreferenceObject != null) {
                List<UserPreference> userPreferences = userPreferenceObjectToList(userPreferenceObject, savedUser.getId());
                userPreferenceManager.saveUserPreferences(userPreferences);
            }

            if (Role.TEACHER.equals(savedUser.getRole()) && Boolean.parseBoolean(properties.getProperty(ALLOW_DIRECT_TEACHER_SIGNUP_AND_FORCE_VERIFICATION))) {
                return Response.accepted(ImmutableMap.of("EMAIL_VERIFICATION_REQUIRED", true)).build();
            } else {
                return Response.ok(savedUser).build();
            }
        } catch (InvalidPasswordException e) {
            log.warn("Invalid password exception occurred during registration!");
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (FailedToHashPasswordException e) {
            log.error("Failed to hash password during user registration!");
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to set a password.").toResponse();
        } catch (MissingRequiredFieldException e) {
            log.warn(String.format("Missing field during update operation: %s ", e.getMessage()));
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, "You are missing a required field. "
                    + "Please make sure you have specified all mandatory fields in your response.").toResponse();
        } catch (DuplicateAccountException e) {
            log.warn(String.format("Duplicate account registration attempt for (%s)", userObjectFromClient.getEmail()));
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Unable to create account due to a database error.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (EmailMustBeVerifiedException e) {
            log.warn("Someone attempted to register with an Isaac email address: " + userObjectFromClient.getEmail());
            return new SegueErrorResponse(Response.Status.BAD_REQUEST,
                    "You cannot register with an Isaac email address.").toResponse();
        } catch (InvalidNameException e) {
            log.warn("Invalid name provided during registration.");
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (UnknownCountryCodeException e) {
            log.warn("Unknown country code provided during registration.");
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        }
    }


    /**
     * Convert user-provided preference maps to UserPreference lists.
     *
     * @param userPreferenceObject - the user-provided preference object
     * @param userId               - the userId of the user
     * @return whether the preference is valid
     */
    private List<UserPreference> userPreferenceObjectToList(final Map<String, Map<String, Boolean>> userPreferenceObject,
                                                            final long userId) {
        List<UserPreference> userPreferences = com.google.common.collect.Lists.newArrayList();
        if (null == userPreferenceObject) {
            return userPreferences;
        }
        // FIXME: This entire method is horrible, but required to sanitise what is stored in the database . . .
        for (String preferenceType : userPreferenceObject.keySet()) {

            // Check if the given preference type is one we support:
            if (!EnumUtils.isValidEnum(uk.ac.cam.cl.dtg.isaac.api.Constants.IsaacUserPreferences.class, preferenceType)
                    && !EnumUtils.isValidEnum(SegueUserPreferences.class, preferenceType)) {
                log.warn("Unknown user preference type '" + preferenceType + "' provided. Skipping.");
                continue;
            }

            if (EnumUtils.isValidEnum(SegueUserPreferences.class, preferenceType)
                    && SegueUserPreferences.EMAIL_PREFERENCE.equals(SegueUserPreferences.valueOf(preferenceType))) {
                // This is an email preference, which is treated specially:
                for (String preferenceName : userPreferenceObject.get(preferenceType).keySet()) {
                    if (!EnumUtils.isValidEnum(EmailType.class, preferenceName)
                            || !EmailType.valueOf(preferenceName).isValidEmailPreference()) {
                        log.warn("Invalid email preference name '" + preferenceName + "' provided for '"
                                + preferenceType + "'! Skipping.");
                        continue;
                    }
                    boolean preferenceValue = userPreferenceObject.get(preferenceType).get(preferenceName);
                    userPreferences.add(new UserPreference(userId, preferenceType, preferenceName, preferenceValue));
                }
            } else if (EnumUtils.isValidEnum(uk.ac.cam.cl.dtg.isaac.api.Constants.IsaacUserPreferences.class,
                    preferenceType)) {
                // Isaac user preference names are configured in the config files:
                String acceptedPreferenceNamesProperty = properties.getProperty(preferenceType);
                if (null == acceptedPreferenceNamesProperty) {
                    log.error("Failed to find allowed user preferences names for '" + preferenceType
                            + "'! Has it been configured?");
                    acceptedPreferenceNamesProperty = "";
                }
                List<String> acceptedPreferenceNames = Arrays.asList(acceptedPreferenceNamesProperty.split(","));
                for (String preferenceName : userPreferenceObject.get(preferenceType).keySet()) {
                    if (!acceptedPreferenceNames.contains(preferenceName)) {
                        log.warn("Invalid user preference name '" + preferenceName + "' provided for type '"
                                + preferenceType + "'! Skipping.");
                        continue;
                    }
                    boolean preferenceValue = userPreferenceObject.get(preferenceType).get(preferenceName);
                    userPreferences.add(new UserPreference(userId, preferenceType, preferenceName, preferenceValue));
                }
            } else {
                log.warn("Unexpected user preference type '" + preferenceType + "' provided. Skipping.");
            }
        }
        return userPreferences;
    }

    /**
     * Update a user object.
     *
     * <p>This method does all of the necessary security checks to determine who is allowed to edit what.
     *
     * @param request              - so that we can identify the user
     * @param response             - so we can modify the session
     * @param userObjectFromClient - the new user object from the clients perspective.
     * @param passwordCurrent      - the current password, used if the password has changed
     * @param newPassword          - the new password, used if the password has changed
     * @param userPreferenceObject - the preferences for this user
     * @return the updated user object.
     * @throws NoCredentialsAvailableException
     * @throws IncorrectCredentialsProvidedException
     */
    public Response updateUserObject(final HttpServletRequest request, final HttpServletResponse response,
                                     final RegisteredUser userObjectFromClient, final String passwordCurrent,
                                     final String newPassword,
                                     final Map<String, Map<String, Boolean>> userPreferenceObject,
                                     final List<UserContext> registeredUserContexts)
            throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException, InvalidKeySpecException,
            NoSuchAlgorithmException {
        Objects.requireNonNull(userObjectFromClient.getId());

        // this is an update as the user has an id
        // security checks
        try {
            // check that the current user has permissions to change this users details.
            RegisteredUserDTO currentlyLoggedInUser = this.getCurrentRegisteredUser(request);
            if (!currentlyLoggedInUser.getId().equals(userObjectFromClient.getId())
                    && !checkUserRole(currentlyLoggedInUser, Arrays.asList(Role.ADMIN, Role.EVENT_MANAGER))) {
                return new SegueErrorResponse(Response.Status.FORBIDDEN,
                        "You cannot change someone else's user settings.").toResponse();
            }

            // check if they are trying to change a password
            if (newPassword != null && !newPassword.isEmpty()) {
                // only admins and the account owner can change passwords 
                if (!currentlyLoggedInUser.getId().equals(userObjectFromClient.getId())
                        && !checkUserRole(currentlyLoggedInUser, Collections.singletonList(Role.ADMIN))) {
                    return new SegueErrorResponse(Response.Status.FORBIDDEN,
                            "You cannot change someone else's password.").toResponse();
                }

                // Password change requires auth check unless admin is modifying non-admin user account
                if (!(checkUserRole(currentlyLoggedInUser, Collections.singletonList(Role.ADMIN))
                        && userObjectFromClient.getRole() != Role.ADMIN)) {
                    // authenticate the user to check they are allowed to change the password

                    if (null == passwordCurrent) {
                        return new SegueErrorResponse(Response.Status.BAD_REQUEST,
                                "You must provide your current password to change your password!").toResponse();
                    }

                    this.ensureCorrectPassword(AuthenticationProvider.SEGUE.name(),
                            userObjectFromClient.getEmail(), passwordCurrent);
                }
            }

            // check that any changes to protected fields being made are allowed.
            RegisteredUserDTO existingUserFromDb = this.getUserDTOById(userObjectFromClient.getId());

            // You cannot modify role using this endpoint (an admin needs to go through the endpoint specifically for
            // role modification)
            if (null == userObjectFromClient.getRole() || !existingUserFromDb.getRole().equals(userObjectFromClient.getRole())) {
                return new SegueErrorResponse(Response.Status.FORBIDDEN,
                        "You cannot change a users role.").toResponse();
            }

            if (registeredUserContexts != null) {
                // We always set the last confirmed date from code rather than trusting the client
                userObjectFromClient.setRegisteredContexts(registeredUserContexts);
                userObjectFromClient.setRegisteredContextsLastConfirmed(new Date());
            } else {
                // Registered contexts should only ever be set via the registeredUserContexts object, so that it is the
                // server that sets the time that they last confirmed their user context values.
                // To ensure this, we overwrite the fields with the values already set in the db if registeredUserContexts is null
                userObjectFromClient.setRegisteredContexts(existingUserFromDb.getRegisteredContexts());
                userObjectFromClient.setRegisteredContextsLastConfirmed(existingUserFromDb.getRegisteredContextsLastConfirmed());
            }

            RegisteredUserDTO updatedUser = updateUserObject(userObjectFromClient, newPassword);

            // If the user's school has changed, record it. Check this using Objects.equals() to be null safe!
            if (!Objects.equals(updatedUser.getSchoolId(), existingUserFromDb.getSchoolId())
                    || !Objects.equals(updatedUser.getSchoolOther(), existingUserFromDb.getSchoolOther())) {
                LinkedHashMap<String, Object> eventDetails = new LinkedHashMap<>();
                eventDetails.put("oldSchoolId", existingUserFromDb.getSchoolId());
                eventDetails.put("newSchoolId", updatedUser.getSchoolId());
                eventDetails.put("oldSchoolOther", existingUserFromDb.getSchoolOther());
                eventDetails.put("newSchoolOther", updatedUser.getSchoolOther());

                if (!Objects.equals(currentlyLoggedInUser.getId(), updatedUser.getId())) {
                    // This is an ADMIN user changing another user's school:
                    eventDetails.put(USER_ID_FKEY_FIELDNAME, updatedUser.getId());
                    this.logManager.logEvent(currentlyLoggedInUser, request, SegueServerLogType.ADMIN_CHANGE_USER_SCHOOL,
                            eventDetails);
                } else {
                    this.logManager.logEvent(currentlyLoggedInUser, request, SegueServerLogType.USER_SCHOOL_CHANGE,
                            eventDetails);
                }
            }

            if (userPreferenceObject != null) {
                List<UserPreference> userPreferences = userPreferenceObjectToList(userPreferenceObject,
                        updatedUser.getId());
                userPreferenceManager.saveUserPreferences(userPreferences);
            }

            return Response.ok(updatedUser).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Response.Status.NOT_FOUND,
                    "The user specified does not exist.").toResponse();
        } catch (DuplicateAccountException e) {
            log.warn(String.format("Account email change failed due to existing email (%s)", userObjectFromClient.getEmail()));
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Unable to modify user", e);
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Error while modifying the user").toResponse();
        } catch (InvalidPasswordException e) {
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (MissingRequiredFieldException e) {
            log.warn(String.format("Missing field during update operation: %s ", e.getMessage()));
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (AuthenticationProviderMappingException e) {
            return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR,
                    "Unable to map to a known authenticator. The provider: is unknown").toResponse();
        } catch (InvalidNameException e) {
            log.warn("Invalid name provided during user update.");
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (UnknownCountryCodeException e) {
            log.warn("Unknown country code provided during user update.");
            return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
        }
    }

    /**
     * Complete the MFA login process. If the correct TOTPCode is provided we will give the user a full session cookie
     * rather than a partial one.
     *
     * @param request    - containing the partially logged in user.
     * @param response   - response will be updated to include fully logged in cookie if TOTPCode is successfully verified
     * @param TOTPCode   - code to verify
     * @param rememberMe - Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @return RegisteredUserDTO as they are now considered logged in.
     * @throws IncorrectCredentialsProvidedException - if the password is incorrect
     * @throws NoCredentialsAvailableException       - If the account exists but does not have a local password
     * @throws NoUserLoggedInException               - If the user hasn't completed the first step of the authentication process.
     * @throws SegueDatabaseException                - if there is a problem with the database.
     * @throws IOException                           - if there is a problem updating the session caveats.
     */
    public RegisteredUserDTO authenticateMFA(final HttpServletRequest request, final HttpServletResponse response,
                                             final Integer TOTPCode, final boolean rememberMe)
            throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException, SegueDatabaseException, NoUserLoggedInException, IOException, InvalidSessionException {
        RegisteredUser registeredUser = this.retrieveCaveatLogin(request,
                Set.of(AuthenticationCaveat.INCOMPLETE_MFA_CHALLENGE));

        if (registeredUser == null) {
            throw new NoUserLoggedInException();
        }

        RegisteredUserDTO userToReturn = convertUserDOToUserDTO(registeredUser);
        this.secondFactorManager.authenticate2ndFactor(userToReturn, TOTPCode);

        return this.convertUserDOToUserDTO(userAuthenticationManager.removeCaveatFromUserSession(request, response, registeredUser,
                AuthenticationCaveat.INCOMPLETE_MFA_CHALLENGE));
    }

    /**
     * Utility method to ensure that the credentials provided are the current correct ones. If they are invalid an
     * exception will be thrown otherwise nothing will happen.
     *
     * @param provider - the password provider who will validate the credentials.
     * @param email    - the email address of the account holder.
     * @param password - the plain text password.
     * @throws AuthenticationProviderMappingException - if we cannot find an authenticator
     * @throws IncorrectCredentialsProvidedException  - if the password is incorrect
     * @throws NoUserException                        - if the user does not exist
     * @throws NoCredentialsAvailableException        - If the account exists but does not have a local password
     * @throws SegueDatabaseException                 - if there is a problem with the database.
     */
    public void ensureCorrectPassword(final String provider, final String email, final String password)
            throws AuthenticationProviderMappingException, IncorrectCredentialsProvidedException, NoUserException,
            NoCredentialsAvailableException, SegueDatabaseException, InvalidKeySpecException, NoSuchAlgorithmException {

        // this method will throw an error if the credentials are incorrect.
        this.userAuthenticationManager.getSegueUserFromCredentials(provider, email, password);
    }


    /**
     * Unlink User From AuthenticationProvider
     *
     * <p>Removes the link between a user and a provider.
     *
     * @param user           - user to affect.
     * @param providerString - provider to unassociated.
     * @throws SegueDatabaseException                 - if there is an error during the database update.
     * @throws MissingRequiredFieldException          - If the change will mean that the user will be unable to login again.
     * @throws AuthenticationProviderMappingException - if we are unable to locate the authentication provider specified.
     */
    public void unlinkUserFromProvider(final RegisteredUserDTO user, final String providerString)
            throws SegueDatabaseException, MissingRequiredFieldException, AuthenticationProviderMappingException {
        RegisteredUser userDO = this.findUserById(user.getId());
        this.userAuthenticationManager.unlinkUserAndProvider(userDO, providerString);
    }

    /**
     * CheckUserRole matches a list of valid roles.
     *
     * @param request    - http request so that we can get current users details.
     * @param validRoles - a Collection of roles that we would want the user to match.
     * @return true if the user is a member of one of the roles in our valid roles list. False if not.
     * @throws NoUserLoggedInException - if there is no registered user logged in.
     */
    public final boolean checkUserRole(final HttpServletRequest request, final Collection<Role> validRoles)
            throws NoUserLoggedInException {
        RegisteredUserDTO user = this.getCurrentRegisteredUser(request);

        return this.checkUserRole(user, validRoles);
    }

    /**
     * CheckUserRole matches a list of valid roles.
     *
     * @param user       - the users details.
     * @param validRoles - a Collection of roles that we would want the user to match.
     * @return true if the user is a member of one of the roles in our valid roles list. False if not.
     * @throws NoUserLoggedInException - if there is no registered user logged in.
     */
    public final boolean checkUserRole(final RegisteredUserDTO user, final Collection<Role> validRoles)
            throws NoUserLoggedInException {
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
     * @param request - to retrieve session information from
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
     * <p>This method will validate the session and will throw a NoUserLoggedInException if invalid.
     *
     * @param request - to retrieve session information from
     * @return Returns the current UserDTO if we can get it or null if user is not currently logged in
     * @throws NoUserLoggedInException - When the session has expired or there is no user currently logged in.
     */
    public final RegisteredUserDTO getCurrentRegisteredUser(final HttpServletRequest request)
            throws NoUserLoggedInException {
        Objects.requireNonNull(request);

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
     * Extract the session expiry time from a request.
     *
     * <p>Does not check session validity.
     *
     * @param request The request to extract the session information from
     * @return The session expiry as a Date
     */
    public Date getSessionExpiry(final HttpServletRequest request) {
        return userAuthenticationManager.getSessionExpiry(request);
    }

    /**
     * Get the authentication settings of particular user
     *
     * @param user - to retrieve settings from
     * @return Returns the current UserDTO if we can get it or null if user is not currently logged in
     * @throws SegueDatabaseException - If there is an internal database error
     */
    public final UserAuthenticationSettingsDTO getUsersAuthenticationSettings(final RegisteredUserDTO user)
            throws SegueDatabaseException {
        Objects.requireNonNull(user);

        UserAuthenticationSettings userAuthenticationSettings = this.database.getUserAuthenticationSettings(user.getId());
        if (userAuthenticationSettings != null) {
            return this.dtoMapper.map(userAuthenticationSettings, UserAuthenticationSettingsDTO.class);
        } else {
            return new UserAuthenticationSettingsDTO();
        }
    }

    public void upgradeUsersPasswordHashAlgorithm(final Long userId, final String chainedHashingAlgorithmName)
            throws NoCredentialsAvailableException, SegueDatabaseException, NoSuchAlgorithmException, InvalidKeySpecException {
        this.userAuthenticationManager.upgradeUsersPasswordHashAlgorithm(userId, chainedHashingAlgorithmName);
    }

    /**
     * Find a list of users based on some user prototype.
     *
     * @param prototype - partially completed user object to base search on
     * @return list of registered user dtos.
     * @throws SegueDatabaseException - if there is a database error.
     */
    public List<RegisteredUserDTO> findUsers(final RegisteredUserDTO prototype) throws SegueDatabaseException {
        List<RegisteredUser> registeredUsersDOs = this.database.findUsers(this.dtoMapper.map(prototype,
                RegisteredUser.class));

        return this.convertUserDOListToUserDTOList(registeredUsersDOs);
    }

    /**
     * Find a list of users based on a List of user ids.
     *
     * @param userIds - partially completed user object to base search on
     * @return list of registered user dtos.
     * @throws SegueDatabaseException - if there is a database error.
     */
    public List<RegisteredUserDTO> findUsers(final Collection<Long> userIds) throws SegueDatabaseException {
        Objects.requireNonNull(userIds);
        if (userIds.isEmpty()) {
            return Lists.newArrayList();
        }

        List<RegisteredUser> registeredUsersDOs = this.database.findUsers(Lists.newArrayList(userIds));

        return this.convertUserDOListToUserDTOList(registeredUsersDOs);
    }

    /**
     * This function can be used to find user information about a user when given an id.
     *
     * @param id - the id of the user to search for.
     * @return the userDTO
     * @throws NoUserException        - If we cannot find a valid user with the email address provided.
     * @throws SegueDatabaseException - If there is another database error
     */
    @Override
    public final RegisteredUserDTO getUserDTOById(final Long id) throws NoUserException, SegueDatabaseException {
        return this.getUserDTOById(id, false);
    }

    /**
     * This function can be used to find user information about a user when given an id - EVEN if it is a deleted user.
     *
     * <p>WARNING- Do not expect complete RegisteredUser Objects as data may be missing if you include deleted users
     *
     * @param id             - the id of the user to search for.
     * @param includeDeleted - include deleted users in results - true for yes false for no
     * @return the userDTO
     * @throws NoUserException        - If we cannot find a valid user with the email address provided.
     * @throws SegueDatabaseException - If there is another database error
     */
    @Override
    public final RegisteredUserDTO getUserDTOById(final Long id, final boolean includeDeleted) throws NoUserException,
            SegueDatabaseException {
        RegisteredUser user;
        if (includeDeleted) {
            user = this.database.getById(id, true);
        } else {
            user = this.findUserById(id);
        }

        if (null == user) {
            throw new NoUserException("No user found with this ID!");
        }
        return this.convertUserDOToUserDTO(user);
    }

    /**
     * This function can be used to find user information about a user when given an email.
     *
     * @param email - the e-mail address of the user to search for
     * @return the userDTO
     * @throws NoUserException        - If we cannot find a valid user with the email address provided.
     * @throws SegueDatabaseException - If there is another database error
     */
    public final RegisteredUserDTO getUserDTOByEmail(final String email) throws NoUserException,
            SegueDatabaseException {
        RegisteredUser findUserByEmail = this.findUserByEmail(email);

        if (null == findUserByEmail) {
            throw new NoUserException("No user found with this email!");
        }

        return this.convertUserDOToUserDTO(findUserByEmail);
    }

    /**
     * This method will return either an AnonymousUserDTO or a RegisteredUserDTO
     *
     * <p>If the user is currently logged in you will get a RegisteredUserDTO otherwise you will get an AnonymousUserDTO
     * containing a sessionIdentifier and any questionAttempts made by the anonymous user.
     *
     * @param request - containing session information.
     * @return AbstractSegueUserDTO - Either a RegisteredUser or an AnonymousUser
     */
    public AbstractSegueUserDTO getCurrentUser(final HttpServletRequest request) throws SegueDatabaseException {
        try {
            return this.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e) {
            return this.getAnonymousUserDTO(request);
        }
    }

    /**
     * Destroy a session attached to the request.
     *
     * @param request  containing the tomcat session to destroy
     * @param response to destroy the segue cookie.
     */
    public void logUserOut(final HttpServletRequest request, final HttpServletResponse response) {
        Objects.requireNonNull(request);
        this.userAuthenticationManager.destroyUserSession(request, response);
    }

    /**
     * Method to create a user object in our database and log them in.
     *
     * <p>Note: this method is intended for creation of accounts in segue - not for linked account registration.
     *
     * @param request     to enable access to anonymous user information.
     * @param response    to store the session in our own segue cookie.
     * @param user        - the user DO to use for updates - must not contain a user id.
     * @param newPassword - new password for the account being created.
     * @param rememberMe  - Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @param sessionCaveats - caveats for the session cookie indicating authentication isn't fully complete
     * @return the user object as was saved.
     * @throws InvalidPasswordException      - the password provided does not meet our requirements.
     * @throws MissingRequiredFieldException - A required field is missing for the user object so cannot be saved.
     * @throws SegueDatabaseException        - If there is an internal database error.
     * @throws EmailMustBeVerifiedException  - if a user attempts to sign up with an email that must be verified before it can be used
     *                                       (i.e. an @isaacphysics.org or @isaacchemistry.org address).
     */
    public RegisteredUserDTO createUserObjectAndSession(final HttpServletRequest request,
                                                        final HttpServletResponse response, final RegisteredUser user, final String newPassword,
                                                        final boolean rememberMe, final Set<AuthenticationCaveat> sessionCaveats)
            throws InvalidPasswordException, MissingRequiredFieldException, SegueDatabaseException, EmailMustBeVerifiedException,
            InvalidKeySpecException, NoSuchAlgorithmException, InvalidNameException, UnknownCountryCodeException {

        Validate.isTrue(user.getId() == null, "When creating a new user the user id must not be set.");

        MapperFacade mapper = this.dtoMapper;
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders.get(AuthenticationProvider.SEGUE);

        // We want to map via a DTO first to make sure that the user cannot set fields that aren't exposed to them:
        RegisteredUserDTO userDtoForNewUser = mapper.map(user, RegisteredUserDTO.class);
        RegisteredUser userToSave = mapper.map(userDtoForNewUser, RegisteredUser.class);

        // -----  Validate the user object and details:  -----

        // Ensure password is valid.
        authenticator.ensureValidPassword(newPassword);

        // Ensure nobody registers with Isaac email addresses. Users can change emails to restricted ones by verifying them, however.
        if (null != restrictedSignupEmailRegex && restrictedSignupEmailRegex.matcher(userToSave.getEmail()).find()) {
            log.warn("User attempted to register with Isaac email address '{}'!", user.getEmail());
            throw new EmailMustBeVerifiedException("You cannot register with an Isaac email address.");
        }

        // Set defaults
        // keep teacher role if requested and direct teacher signup allowed
        if ((Boolean.parseBoolean(properties.getProperty(ALLOW_DIRECT_TEACHER_SIGNUP_AND_FORCE_VERIFICATION))
                && Role.TEACHER.equals(userToSave.getRole()))) {
            userToSave.setTeacherAccountPending(true);
        } else {
            // otherwise, always set to default role
            userToSave.setRole(Role.STUDENT);
            userToSave.setTeacherAccountPending(false);
        }
        userToSave.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        userToSave.setRegistrationDate(new Date());
        userToSave.setLastUpdated(new Date());

        // Before save we should validate the user for mandatory fields.
        if (!isUserEmailValid(userToSave.getEmail())) {
            throw new MissingRequiredFieldException("The email address provided is invalid.");
        }

        // validate names
        if (!isUserNameValid(userToSave.getGivenName())) {
            throw new InvalidNameException("The given name provided is an invalid length or contains forbidden characters.");
        }
        if (!isUserNameValid(userToSave.getFamilyName())) {
            throw new InvalidNameException("The family name provided is an invalid length or contains forbidden characters.");
        }

        // Validate country code
        if (null != userToSave.getCountryCode() && !CountryLookupManager.isKnownCountryCode(userToSave.getCountryCode())) {
            throw new UnknownCountryCodeException("The country provided is not known.");
        }

        // Critically, and after other validation, check this is not a duplicate registration attempt:
        if (this.findUserByEmail(userToSave.getEmail()) != null) {
            throw new DuplicateAccountException();
        }

        // -----  Save the user object:  -----

        // Create and set a verification token:
        authenticator.createEmailVerificationTokenForUser(userToSave, userToSave.getEmail());

        // Save the user to get the userId
        RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);

        // Save password for the user
        authenticator.setOrChangeUsersPassword(userToReturn, newPassword);

        // send an email confirmation and set up verification
        try {
            RegisteredUserDTO userToReturnDTO = this.getUserDTOById(userToReturn.getId());

            ImmutableMap<String, Object> emailTokens = ImmutableMap.of("verificationURL",
                    generateEmailVerificationURL(userToReturnDTO, userToReturn.getEmailVerificationToken()));

            emailManager.sendTemplatedEmailToUser(userToReturnDTO,
                    emailManager.getEmailTemplateDTO("email-template-registration-confirmation"),
                    emailTokens, EmailType.SYSTEM);

        } catch (ContentManagerException e) {
            log.error("Registration email could not be sent due to content issue: " + e.getMessage());
        } catch (NoUserException e) {
            log.error("Registration email could not be sent due to not being able to locate the user: " + e.getMessage());
        }

        // save the user again with updated token
        //TODO: do we need this?
        userToReturn = this.database.createOrUpdateUser(userToReturn);

        logManager.logEvent(this.convertUserDOToUserDTO(userToReturn), request, SegueServerLogType.USER_REGISTRATION,
                ImmutableMap.builder().put("provider", AuthenticationProvider.SEGUE.name()).build());

        // return it to the caller.
        if (!sessionCaveats.isEmpty()) {
            return this.logUserInWithCaveats(request, response, userToReturn, rememberMe, sessionCaveats);
        } else {
            return this.logUserIn(request, response, userToReturn, rememberMe);
        }
    }

    /**
     * Method to update a user object in our database.
     *
     * @param updatedUser - the user to update - must contain a user id
     * @param newPassword - the new password if being changed.
     * @return the user object as was saved.
     * @throws InvalidPasswordException      - the password provided does not meet our requirements.
     * @throws MissingRequiredFieldException - A required field is missing for the user object so cannot be saved.
     * @throws SegueDatabaseException        - If there is an internal database error.
     */
    public RegisteredUserDTO updateUserObject(final RegisteredUser updatedUser, final String newPassword)
            throws InvalidPasswordException, MissingRequiredFieldException, SegueDatabaseException,
            InvalidKeySpecException, NoSuchAlgorithmException, InvalidNameException, UnknownCountryCodeException {
        Objects.requireNonNull(updatedUser.getId());

        // We want to map to DTO first to make sure that the user cannot
        // change fields that aren't exposed to them
        RegisteredUserDTO userDTOContainingUpdates = this.dtoMapper.map(updatedUser, RegisteredUserDTO.class);
        if (updatedUser.getId() == null) {
            throw new IllegalArgumentException(
                    "The user object specified does not have an id. Users cannot be updated without a specific id set.");
        }

        // This is an update operation.
        final RegisteredUser existingUser = this.findUserById(updatedUser.getId());
        // userToSave = existingUser;

        // Check that the user isn't trying to take an existing users e-mail.
        if (this.findUserByEmail(updatedUser.getEmail()) != null && !existingUser.getEmail().equals(updatedUser.getEmail())) {
            throw new DuplicateAccountException();
        }

        // validate names
        if (!isUserNameValid(updatedUser.getGivenName())) {
            throw new InvalidNameException("The given name provided is an invalid length or contains forbidden characters.");
        }

        if (!isUserNameValid(updatedUser.getFamilyName())) {
            throw new InvalidNameException("The family name provided is an invalid length or contains forbidden characters.");
        }

        // Validate country code
        if (null != updatedUser.getCountryCode() && !CountryLookupManager.isKnownCountryCode(updatedUser.getCountryCode())) {
            throw new UnknownCountryCodeException("The country provided is not known.");
        }

        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        // Check if there is a new password and it is invalid as early as possible:
        if (null != newPassword && !newPassword.isEmpty()) {
            authenticator.ensureValidPassword(newPassword);
        }

        MapperFacade mergeMapper = new DefaultMapperFactory.Builder().mapNulls(false).build().getMapperFacade();

        RegisteredUser userToSave = new RegisteredUser();
        mergeMapper.map(existingUser, userToSave);
        mergeMapper.map(userDTOContainingUpdates, userToSave);
        // Don't modify email verification status, registration date, role, or teacher account pending status
        userToSave.setEmailVerificationStatus(existingUser.getEmailVerificationStatus());
        userToSave.setRegistrationDate(existingUser.getRegistrationDate());
        userToSave.setRole(existingUser.getRole());
        userToSave.setLastUpdated(new Date());
        userToSave.setTeacherAccountPending(existingUser.getTeacherAccountPending());

        if (updatedUser.getSchoolId() == null && existingUser.getSchoolId() != null) {
            userToSave.setSchoolId(null);
        }
        // Correctly remove school_other when it is set to be the empty string:
        if (updatedUser.getSchoolOther() == null || updatedUser.getSchoolOther().isEmpty()) {
            userToSave.setSchoolOther(null);
        }

        // Allow the user to clear their DOB, they have already confirmed they are over 11 at least once.
        // null values are explicitly not mapped by `mergeMapper`.
        if (updatedUser.getDateOfBirth() == null) {
            userToSave.setDateOfBirth(null);
        }

        // Before save we should validate the user for mandatory fields.
        // Doing this before the email change code is necessary to ensure that (a) users cannot try and change to an
        // invalid email, and (b) that users with an invalid email can change their email to a valid one!
        if (!isUserEmailValid(userToSave.getEmail())) {
            throw new MissingRequiredFieldException("The email address provided is invalid.");
        }

        // Make sure the email address is preserved (can't be changed until new email is verified)
        // Send a new verification email if the user has changed their email
        if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
            try {
                String newEmail = updatedUser.getEmail();
                authenticator.createEmailVerificationTokenForUser(userToSave, newEmail);

                RegisteredUserDTO userDTO = this.getUserDTOById(userToSave.getId());
                String emailVerificationToken = userToSave.getEmailVerificationToken();
                this.sendVerificationEmailsForEmailChange(userDTO, newEmail, emailVerificationToken);

            } catch (ContentManagerException | NoUserException e) {
                log.error("ContentManagerException during sendEmailVerificationChange " + e.getMessage());
            }

            userToSave.setEmail(existingUser.getEmail());
        }

        // save the user
        RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);
        if (null != newPassword && !newPassword.isEmpty()) {
            authenticator.setOrChangeUsersPassword(userToReturn, newPassword);
        }

        // return it to the caller
        return this.convertUserDOToUserDTO(userToReturn);
    }

    /**
     * @param id            - the user id
     * @param requestedRole - the new role
     * @throws SegueDatabaseException - an exception when accessing the database
     */
    public void updateUserRole(final Long id, final Role requestedRole) throws SegueDatabaseException {
        Objects.requireNonNull(requestedRole);
        RegisteredUser userToSave = this.findUserById(id);

        // Send welcome email if user has become teacher or tutor, otherwise, role change notification
        try {
            RegisteredUserDTO existingUserDTO = this.getUserDTOById(id);
            if (userToSave.getRole() != requestedRole) {
                String emailTemplate;
                switch (requestedRole) {
                    case TUTOR:
                        emailTemplate = "email-template-tutor-welcome";
                        break;
                    case TEACHER:
                        emailTemplate = "email-template-teacher-welcome";
                        break;
                    default:
                        emailTemplate = "email-template-default-role-change";
                }
                emailManager.sendTemplatedEmailToUser(existingUserDTO,
                        emailManager.getEmailTemplateDTO(emailTemplate),
                        ImmutableMap.of("oldrole", existingUserDTO.getRole().toString(),
                                "newrole", requestedRole.toString()),
                        EmailType.SYSTEM);
            }
        } catch (ContentManagerException | NoUserException e) {
            log.debug("ContentManagerException during sendTeacherWelcome " + e.getMessage());
        }

        userToSave.setRole(requestedRole);
        this.database.createOrUpdateUser(userToSave);
    }

    /**
     * @param email                            - the user email
     * @param requestedEmailVerificationStatus - the new email verification status
     * @throws SegueDatabaseException - an exception when accessing the database
     */
    public void updateUserEmailVerificationStatus(final String email,
                                                  final EmailVerificationStatus requestedEmailVerificationStatus) throws SegueDatabaseException {
        Objects.requireNonNull(requestedEmailVerificationStatus);
        RegisteredUser userToSave = this.findUserByEmail(email);
        if (null == userToSave) {
            log.warn(String.format(
                    "Could not update email verification status of email address (%s) - does not exist",
                    email));
            return;
        }
        userToSave.setEmailVerificationStatus(requestedEmailVerificationStatus);
        userToSave.setLastUpdated(new Date());
        this.database.createOrUpdateUser(userToSave);
    }

    /**
     * This method facilitates the removal of personal user data from Segue.
     *
     * @param userToDelete - the user to delete.
     * @throws SegueDatabaseException - if a general database error has occurred.
     * @throws NoUserException        - if we cannot find the user account specified
     */
    public void deleteUserAccount(final RegisteredUserDTO userToDelete) throws NoUserException, SegueDatabaseException {
        // check the user exists
        if (null == userToDelete) {
            throw new NoUserException("Unable to delete the user as no user was provided.");
        }

        RegisteredUser userDOById = this.findUserById(userToDelete.getId());

        // delete the user.
        this.database.deleteUserAccount(userDOById);
    }

    /**
     * This method facilitates the merging of two user accounts.
     *
     * @param target - the user account to remove.
     * @param source - the user account to merge into.
     * @throws SegueDatabaseException if an error occurs
     */
    public void mergeUserAccounts(final RegisteredUserDTO target, final RegisteredUserDTO source)
            throws SegueDatabaseException {
        // check the users exist
        if (null == target) {
            throw new SegueDatabaseException("Merge users target is null");
        } else if (null == source) {
            throw new SegueDatabaseException("Merge users source is null");
        }

        RegisteredUser targetUser = this.findUserById(target.getId());
        RegisteredUser sourceUser = this.findUserById(source.getId());

        // merge the users.
        this.database.mergeUserAccounts(targetUser, sourceUser);
    }

    /**
     * This method will use an email address to check a local user exists and if so, will send an email with a unique
     * token to allow a password reset. This method does not indicate whether or not the email actually existed.
     *
     * @param userObject - A user object containing the email address of the user to reset the password for.
     * @throws NoSuchAlgorithmException - if the configured algorithm is not valid.
     * @throws InvalidKeySpecException  - if the preconfigured key spec is invalid.
     * @throws CommunicationException   - if a fault occurred whilst sending the communique
     * @throws SegueDatabaseException   - If there is an internal database error.
     * @throws NoUserException          - If no user found with provided email.
     */
    public final void resetPasswordRequest(final RegisteredUserDTO userObject) throws InvalidKeySpecException,
            NoSuchAlgorithmException, CommunicationException, SegueDatabaseException, NoUserException {
        RegisteredUser user = this.findUserByEmail(userObject.getEmail());

        if (null == user) {
            throw new NoUserException("No user found with this email!");
        }

        RegisteredUserDTO userDTO = this.convertUserDOToUserDTO(user);
        this.userAuthenticationManager.resetPasswordRequest(user, userDTO);
    }

    /**
     * This method will use an email address to check a local user exists and if so, will send an email with a unique
     * token to allow a password reset. This method does not indicate whether or not the email actually existed.
     *
     * @param request - so we can look up the registered user object.
     * @param email   - The email the user wants to verify.
     * @throws SegueDatabaseException - If there is an internal database error.
     */
    public final void emailVerificationRequest(final HttpServletRequest request, final String email)
            throws SegueDatabaseException {

        try {
            RegisteredUserDTO userDTO;
            if (Boolean.parseBoolean(properties.getProperty(ALLOW_DIRECT_TEACHER_SIGNUP_AND_FORCE_VERIFICATION))) {
                // allow users who are required to verify but haven't yet done so to use this endpoint
                userDTO = getCurrentPartiallyIdentifiedUser(request, Set.of(AuthenticationCaveat.INCOMPLETE_MANDATORY_EMAIL_VERIFICATION));
            } else {
                userDTO = getCurrentRegisteredUser(request);
            }
            RegisteredUser user = this.findUserById(userDTO.getId());

            // TODO: Email verification stuff does not belong in the password authenticator... It should be moved.
            // Generate token
            IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                    .get(AuthenticationProvider.SEGUE);
            user = authenticator.createEmailVerificationTokenForUser(user, email);

            // Save user object
            this.database.createOrUpdateUser(user);

            String emailVerificationToken = user.getEmailVerificationToken();

            if (email.equals(user.getEmail())) {
                this.sendVerificationEmailForCurrentEmail(userDTO, emailVerificationToken);
            } else {
                this.sendVerificationEmailsForEmailChange(userDTO, email, emailVerificationToken);
            }

        } catch (NoUserLoggedInException e) {
            log.error(String.format("Verification requested for email:%s where email does not exist "
                    + "and user not logged in!", email));
        } catch (ContentManagerException e) {
            log.debug("ContentManagerException " + e.getMessage());
        }
    }

    /**
     * This method will test if the specified token is a valid password reset token.
     *
     * @param token - The token to test
     * @return true if the reset token is valid
     * @throws SegueDatabaseException - If there is an internal database error.
     */
    public final boolean validatePasswordResetToken(final String token) throws SegueDatabaseException {
        // Set user's password
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        return authenticator.isValidResetToken(token);
    }

    /**
     * processEmailVerification.
     *
     * @param userId - the user id
     * @param token  - token used to verify email address
     * @return - whether the token is valid or not
     * @throws SegueDatabaseException - exception if token cannot be validated
     * @throws InvalidTokenException  - if something is wrong with the token provided
     * @throws NoUserException        - if the user does not exist.
     */
    public RegisteredUserDTO processEmailVerification(final HttpServletRequest request, final HttpServletResponse response,
                                                      final Long userId, final String token)
            throws SegueDatabaseException, InvalidTokenException, NoUserException {
        IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
                .get(AuthenticationProvider.SEGUE);

        RegisteredUser user = this.findUserById(userId);

        if (null == user) {
            log.warn(String.format("Received an invalid email token request for (%s)", userId));
            throw new NoUserException("No user found with this userId!");
        }

        EmailVerificationStatus evStatus = user.getEmailVerificationStatus();
        if (evStatus == EmailVerificationStatus.VERIFIED
                && user.getEmail().equals(user.getEmailToVerify())) {
            log.warn(String.format("Received a duplicate email verification request for (%s) - already verified",
                    user.getEmail()));
            return this.convertUserDOToUserDTO(user);
        }

        if (authenticator.isValidEmailVerificationToken(user, token)) {
            // If a direct-sign-up teacher user has just verified themselves:
            if (Boolean.parseBoolean(properties.getProperty(ALLOW_DIRECT_TEACHER_SIGNUP_AND_FORCE_VERIFICATION))
                    && Role.TEACHER.equals(user.getRole()) && user.getTeacherAccountPending()) {
                // Send them a teacher welcome email
                try {
                    emailManager.sendTemplatedEmailToUser(this.convertUserDOToUserDTO(user),
                            emailManager.getEmailTemplateDTO("email-template-teacher-welcome"), ImmutableMap.of(),
                            EmailType.SYSTEM);
                } catch (final ContentManagerException e) {
                    log.debug("Failed to send teacher welcome email after email verification.");
                }

                try {
                    // If this is the current user, update their session to remove caveat
                    RegisteredUserDTO currentUser = this.getCurrentPartiallyIdentifiedUser(request,
                            Set.of(AuthenticationCaveat.INCOMPLETE_MANDATORY_EMAIL_VERIFICATION));

                    if (Objects.equals(currentUser.getId(), userId)) {
                        userAuthenticationManager.removeCaveatFromUserSession(request, response, user,
                                AuthenticationCaveat.INCOMPLETE_MANDATORY_EMAIL_VERIFICATION);
                    } else {
                        log.debug("Logged-in user doesn't match user to verify, session caveats for logged-in user will"
                                + " not be updated.");
                    }
                } catch (NoUserLoggedInException | InvalidSessionException e) {
                    log.debug("No logged-in user for whom to update caveats.");
                } catch (IOException e) {
                    log.debug("Failed to update session caveats due to malformed session cookie.");
                }
            }

            // Update and save user
            user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
            user.setEmail(user.getEmailToVerify());
            user.setEmailVerificationToken(null);
            user.setEmailToVerify(null);
            user.setLastUpdated(new Date());
            user.setTeacherAccountPending(false);

            RegisteredUser createOrUpdateUser = this.database.createOrUpdateUser(user);
            log.info(String.format("Email verification for user (%s) has completed successfully.",
                    createOrUpdateUser.getId()));

            return this.convertUserDOToUserDTO(createOrUpdateUser);
        } else {
            log.warn(String.format("Received an invalid email verification token for (%s) - invalid token", userId));
            throw new InvalidTokenException();
        }
    }

    /**
     * This method will use a unique password reset token to set a new password.
     *
     * @param token       - the password reset token
     * @param newPassword - the supplied password
     * @return the user which has had the password reset.
     * @throws InvalidTokenException    - If the token provided is invalid.
     * @throws InvalidPasswordException - If the password provided is invalid.
     * @throws SegueDatabaseException   - If there is an internal database error.
     */
    public RegisteredUserDTO resetPassword(final String token, final String newPassword)
            throws InvalidTokenException, InvalidPasswordException, SegueDatabaseException, InvalidKeySpecException,
            NoSuchAlgorithmException {
        return this.convertUserDOToUserDTO(this.userAuthenticationManager.resetPassword(token, newPassword));
    }

    /**
     * Check if account has MFA configured.
     *
     * @param user - who requested it
     * @return true if yes false if not.
     * @throws SegueDatabaseException - If there is an internal database error.
     */
    public boolean has2FAConfigured(final RegisteredUserDTO user) throws SegueDatabaseException {
        return this.secondFactorManager.has2FAConfigured(user);
    }

    /**
     * Generate a random shared secret - currently not stored against the users account.
     *
     * @param user - who requested it
     * @return TOTPSharedSecret object
     */
    public TOTPSharedSecret getNewSharedSecret(final RegisteredUserDTO user) {
        return this.secondFactorManager.getNewSharedSecret(user);
    }

    /**
     * Activate MFA for user's account by passing secret and code submitted.
     *
     * @param user          - registered user
     * @param sharedSecret  - shared secret provided by getNewSharedSecret call
     * @param codeSubmitted - latest TOTP code to confirm successful recording of secret.
     * @return true if it is now active on the account, false if secret / TOTP code do not match.
     * @throws SegueDatabaseException - unable to save secret to account.
     */
    public boolean activateMFAForUser(final RegisteredUserDTO user, final String sharedSecret, final Integer codeSubmitted) throws SegueDatabaseException {
        return this.secondFactorManager.activate2FAForUser(user, sharedSecret, codeSubmitted);
    }

    /**
     * Deactivate MFA for user's account.
     *
     * <p>WARNING: should only be used by admins!
     *
     * @throws SegueDatabaseException - unable to save secret to account.
     */
    public void deactivateMFAForUser(final RegisteredUserDTO user) throws SegueDatabaseException {
        this.secondFactorManager.deactivate2FAForUser(user);
    }

    /**
     * Helper method to convert a user object into a userSummary DTO with as little detail as possible about the user.
     *
     * @param userToConvert - full user object.
     * @return a summarised object with minimal personal information
     */
    public UserSummaryDTO convertToUserSummaryObject(final RegisteredUserDTO userToConvert) {
        return this.dtoMapper.map(userToConvert, UserSummaryDTO.class);
    }

    /**
     * Helper method to convert a user object into a more detailed summary object depending on the dto provided.
     *
     * @param userToConvert    - full user object.
     * @param detailedDTOClass - The level of detail required for the conversion
     * @return a summarised object with reduced personal information
     */
    public UserSummaryWithEmailAddressDTO convertToDetailedUserSummaryObject(final RegisteredUserDTO userToConvert, final Class<? extends UserSummaryWithEmailAddressDTO> detailedDTOClass) {
        return this.dtoMapper.map(userToConvert, detailedDTOClass);
    }

    /**
     * Helper method to convert user objects into cutdown userSummary DTOs.
     *
     * @param userListToConvert - full user objects.
     * @return a list of summarised objects with minimal personal information
     */
    public List<UserSummaryDTO> convertToUserSummaryObjectList(final List<RegisteredUserDTO> userListToConvert) {
        Objects.requireNonNull(userListToConvert);
        List<UserSummaryDTO> resultList = Lists.newArrayList();
        for (RegisteredUserDTO user : userListToConvert) {
            resultList.add(this.convertToUserSummaryObject(user));
        }
        return resultList;
    }

    /**
     * Helper method to convert user objects into cutdown DetailedUserSummary DTOs.
     *
     * @param userListToConvert - full user objects.
     * @param detailedDTO       - The level of detail required for the conversion
     * @return a list of summarised objects with reduced personal information
     */
    public List<UserSummaryWithEmailAddressDTO> convertToDetailedUserSummaryObjectList(final List<RegisteredUserDTO> userListToConvert, final Class<? extends UserSummaryWithEmailAddressDTO> detailedDTO) {
        Objects.requireNonNull(userListToConvert);
        List<UserSummaryWithEmailAddressDTO> resultList = Lists.newArrayList();
        for (RegisteredUserDTO user : userListToConvert) {
            resultList.add(this.convertToDetailedUserSummaryObject(user, detailedDTO));
        }
        return resultList;
    }

    /**
     * Get the user object from the session cookie, overlooking the specified caveats if present (but not others).
     *
     * <p>WARNING: Do not use this method to determine if a user has successfully logged in or not in the general case.
     * {@link #getCurrentRegisteredUser} should almost always be preferred.
     * <p>To be used in limited circumstances where the user has authenticated to a degree acceptable for
     * specific purposes, e.g. responding to MFA challenge after a correct email/password login.
     *
     * @param request to pull back the user
     * @return UserSummaryDTO of the partially logged-in user or will throw an exception if not found or the session has unacceptable caveats.
     * @throws NoUserLoggedInException if they haven't started the flow.
     */
    public RegisteredUserDTO getCurrentPartiallyIdentifiedUser(HttpServletRequest request, Set<AuthenticationCaveat> acceptableCaveats) throws NoUserLoggedInException {
        RegisteredUser registeredUser = this.retrieveCaveatLogin(request, acceptableCaveats);
        if (null == registeredUser) {
            throw new NoUserLoggedInException();
        }
        return this.convertUserDOToUserDTO(registeredUser);
    }

    /**
     * Sends verification email for the user's current email address. The destination will match the userDTO's email.
     *
     * @param userDTO                - user to which the email is to be sent.
     * @param emailVerificationToken - the generated email verification token.
     * @throws ContentManagerException - if the email template does not exist.
     * @throws SegueDatabaseException  - if there is a database exception during the processing of the email.
     */
    private void sendVerificationEmailForCurrentEmail(final RegisteredUserDTO userDTO,
                                                      final String emailVerificationToken)
            throws ContentManagerException, SegueDatabaseException {

        EmailTemplateDTO emailVerificationTemplate =
                emailManager.getEmailTemplateDTO("email-template-email-verification");
        Map<String, Object> emailTokens =
                ImmutableMap.of("verificationURL", this.generateEmailVerificationURL(userDTO, emailVerificationToken));

        log.info(String.format("Sending email verification message to %s", userDTO.getEmail()));

        emailManager.sendTemplatedEmailToUser(userDTO, emailVerificationTemplate, emailTokens, EmailType.SYSTEM);
    }

    /**
     * Send an account deletion token email to a user's registered email address.
     *
     * N.B. This method does not check that a user's account email is valid!
     *
     * @param userDTO - the user to email.
     * @throws ContentManagerException - if the email template cannot be found.
     * @throws SegueDatabaseException  - on database error.
     */
    public void sendAccountDeletionEmail(RegisteredUserDTO userDTO) throws ContentManagerException, SegueDatabaseException {

        AccountDeletionToken token = userAuthenticationManager.createAccountDeletionTokenForUser(userDTO);

        EmailTemplateDTO emailVerificationTemplate = emailManager.getEmailTemplateDTO("email-template-account-deletion");
        Map<String, Object> emailTokens = ImmutableMap.of(
                "accountDeletionURL", this.generateAccountDeletionURL(token.getToken())
        );

        log.info("Sending account deletion message to {}", userDTO.getEmail());

        emailManager.sendTemplatedEmailToUser(userDTO, emailVerificationTemplate, emailTokens, EmailType.SYSTEM);
    }

    /**
     * Use a deletion token to delete a user's account.
     *
     * @param userDTO - the user requesting deletion.
     * @param token   - the deletion token emailed to the user.
     * @throws SegueDatabaseException - on database error.
     * @throws InvalidTokenException  - if the token is incorrect or expired.
     * @throws NoUserException        - if the user cannot be found.
     */
    public void confirmDeletionTokenAndDeleteAccount(final RegisteredUserDTO userDTO, final String token)
            throws SegueDatabaseException, InvalidTokenException, NoUserException {

        if (!userAuthenticationManager.isValidAccountDeletionToken(userDTO, token)) {
            throw new InvalidTokenException();
        }

        deleteUserAccount(userDTO);
    }

    /**
     * Sends a notice email for email change to the user's current email address and then creates a copy of the user
     * with the new email to send to the sendVerificationEmailForCurrentEmail method.
     *
     * @param userDTO       - initial user where the notice of change is to be sent.
     * @param newEmail      - the new email which has been requested to change to.
     * @param newEmailToken - the generated HMAC token for the new email.
     * @throws ContentManagerException - if the email template does not exist.
     * @throws SegueDatabaseException  - if there is a database exception during the processing of the email.
     */
    private void sendVerificationEmailsForEmailChange(final RegisteredUserDTO userDTO,
                                                      final String newEmail,
                                                      final String newEmailToken)
            throws ContentManagerException, SegueDatabaseException {

        EmailTemplateDTO emailChangeTemplate = emailManager.getEmailTemplateDTO("email-verification-change");
        Map<String, Object> emailTokens = ImmutableMap.of("requestedemail", newEmail);

        log.info(String.format("Sending email for email address change for user (%s)"
                + " from email (%s) to email (%s)", userDTO.getId(), userDTO.getEmail(), newEmail));
        emailManager.sendTemplatedEmailToUser(userDTO, emailChangeTemplate, emailTokens, EmailType.SYSTEM);

        // Defensive copy to ensure old email address is preserved (shouldn't change until new email is verified)
        RegisteredUserDTO temporaryUser = this.dtoMapper.map(userDTO, RegisteredUserDTO.class);
        temporaryUser.setEmail(newEmail);
        temporaryUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
        this.sendVerificationEmailForCurrentEmail(temporaryUser, newEmailToken);
    }

    /**
     * Logs the user in and creates the signed sessions.
     *
     * @param request    - for the session to be attached
     * @param response   - for the session to be attached.
     * @param user       - the user who is being logged in.
     * @param rememberMe Boolean to indicate whether or not this cookie expiry duration should be long or short
     * @return the DTO version of the user.
     * @throws SegueDatabaseException - if there is a problem with the database.
     */
    private RegisteredUserDTO logUserIn(final HttpServletRequest request, final HttpServletResponse response,
                                        final RegisteredUser user, final boolean rememberMe) throws SegueDatabaseException {
        AnonymousUser anonymousUser = this.getAnonymousUserDO(request);
        if (anonymousUser != null) {
            log.debug(String.format("Anonymous User (%s) located during login - need to merge question information", anonymousUser.getSessionId()));
        }

        // now we want to clean up any data generated by the user while they weren't logged in.
        mergeAnonymousUserWithRegisteredUser(anonymousUser, user);

        return this.convertUserDOToUserDTO(this.userAuthenticationManager.createUserSession(request, response, user, rememberMe));
    }

    /**
     * Generate a partially logged-in session for the user.
     *
     * <p>The session will contain caveats which will only be accepted by some endpoints (e.g. for MFA).
     *
     * @param request    - http request containing the cookie
     * @param response   - response to update cookie information
     * @param user       - user of interest
     * @param rememberMe - Boolean to indicate whether or not this cookie expiry duration should be long or short
     */
    private RegisteredUserDTO logUserInWithCaveats(final HttpServletRequest request, final HttpServletResponse response,
                                                   final RegisteredUser user, final boolean rememberMe, final Set<AuthenticationCaveat> caveats) {
        return this.convertUserDOToUserDTO(this.userAuthenticationManager.createUserSessionWithCaveats(request, response, user, caveats, rememberMe));
    }

    /**
     * Retrieve a partially logged-in session for the user based on successful password authentication.
     *
     * <p>NOTE: You should not treat users has having logged in using this method as they haven't completed login.
     *
     * @param request - http request containing the cookie
     */
    private RegisteredUser retrieveCaveatLogin(final HttpServletRequest request, Set<AuthenticationCaveat> acceptableCaveats) {
        return this.userAuthenticationManager.getUserFromSession(request, acceptableCaveats);
    }

    /**
     * Method to migrate anonymously generated data to a persisted account.
     *
     * @param anonymousUser to look up.
     * @param user          to migrate to.
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
                Thread logMigrationJob = new Thread(() -> {
                    // run this asynchronously as there is no need to block and it is quite slow.
                    logManager.transferLogEventsToRegisteredUser(anonymousUser.getSessionId(), user.getId()
                            .toString());

                    logManager.logInternalEvent(userDTO, SegueServerLogType.MERGE_USER,
                            ImmutableMap.of("oldAnonymousUserId", anonymousUser.getSessionId()));

                    // delete the session attribute as merge has completed.
                    try {
                        temporaryUserCache.deleteAnonymousUser(anonymousUser);
                    } catch (SegueDatabaseException e) {
                        log.error("Unable to delete anonymous user during merge operation.", e);
                    }
                });

                logMigrationJob.start();

            } catch (SegueDatabaseException e) {
                log.error("Unable to merge anonymously collected data with stored user object.", e);
            }
        }
    }

    /**
     * Library method that allows the api to locate a user object from the database based on a given unique id.
     *
     * @param userId - to search for.
     * @return user or null if we cannot find it.
     * @throws SegueDatabaseException - If there is an internal database error.
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
     * @param email - to search for.
     * @return user or null if we cannot find it.
     * @throws SegueDatabaseException - If there is an internal database error.
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
     * @param federatedAuthenticator the federatedAuthenticator we are using for authentication
     * @param userFromProvider       - the user object returned by the auth provider.
     * @return a Segue UserDO that exists in the segue database.
     * @throws NoUserException        - If we are unable to locate the user id based on the lookup reference provided.
     * @throws SegueDatabaseException - If there is an internal database error.
     */
    private RegisteredUser registerUserWithFederatedProvider(final AuthenticationProvider federatedAuthenticator,
                                                             final UserFromAuthProvider userFromProvider) throws NoUserException, SegueDatabaseException {

        log.debug(String.format("New registration (%s) as user does not already exist.", federatedAuthenticator));

        if (null == userFromProvider) {
            log.warn("Unable to create user for the provider "
                    + federatedAuthenticator);
            throw new NoUserException("No user returned by the provider!");
        }

        RegisteredUser newLocalUser = this.dtoMapper.map(userFromProvider, RegisteredUser.class);
        newLocalUser.setRegistrationDate(new Date());

        // register user
        RegisteredUser newlyRegisteredUser = database.registerNewUserWithProvider(newLocalUser,
                federatedAuthenticator, userFromProvider.getProviderUserId());

        RegisteredUser localUserInformation = this.database.getById(newlyRegisteredUser.getId());

        if (null == localUserInformation) {
            // we just put it in so something has gone very wrong.
            log.error("Failed to retrieve user even though we just put it in the database!");
            throw new NoUserException("Failed to retrieve user immediately after saving to database!");
        }

        // since the federated providers didn't always provide email addresses - we have to check and update accordingly.
        if (!localUserInformation.getEmail().contains("@")
                && !EmailVerificationStatus.DELIVERY_FAILED.equals(localUserInformation.getEmailVerificationStatus())) {
            this.updateUserEmailVerificationStatus(localUserInformation.getEmail(),
                    EmailVerificationStatus.DELIVERY_FAILED);
        }

        logManager.logInternalEvent(this.convertUserDOToUserDTO(localUserInformation), SegueServerLogType.USER_REGISTRATION,
                ImmutableMap.builder().put("provider", federatedAuthenticator.name())
                        .build());

        return localUserInformation;
    }

    /**
     * This function will check that the user's email address is valid.
     *
     * @param email - the user email to validate.
     * @return true if it meets the internal storage requirements, false if not.
     */
    public static boolean isUserEmailValid(final String email) {
        return EmailValidator.getInstance().isValid(email);
    }

    /**
     * This function checks that the name provided is valid.
     *
     * @param name - the name to validate.
     * @return true if the name is valid, false otherwise.
     */
    public static boolean isUserNameValid(final String name) {
        return null != name && name.length() <= USER_NAME_MAX_LENGTH && !USER_NAME_FORBIDDEN_CHARS_REGEX.matcher(name).find()
                && !name.isEmpty();
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
        return this.convertUserDOListToUserDTOList(Collections.singletonList(user)).get(0);
    }

    /**
     * Converts a list of userDOs into a List of userDTOs.
     *
     * @param users - list of DOs to convert
     * @return the list of user dtos.
     */
    private List<RegisteredUserDTO> convertUserDOListToUserDTOList(final List<RegisteredUser> users) {
        List<RegisteredUser> userDOs = users.parallelStream().filter(Objects::nonNull).collect(Collectors.toList());
        if (userDOs.isEmpty()) {
            return new ArrayList<>();
        }

        return users.parallelStream().map(user -> this.dtoMapper.map(user, RegisteredUserDTO.class)).collect(Collectors.toList());
    }

    /**
     * Get the RegisteredUserDO of the currently logged in user. This is for internal use only.
     *
     * <p>This method will validate the session as well returning null if it is invalid.
     *
     * @param request - to retrieve session information from
     * @return Returns the current UserDTO if we can get it or null if user is not currently logged in / there is an
     *       invalid session.
     *
     *       Rejects attempts to retrieve a user if the session has any caveats.
     */
    private RegisteredUser getCurrentRegisteredUserDO(final HttpServletRequest request) {
        return this.userAuthenticationManager.getUserFromSession(request);
    }

    /**
     * Retrieves anonymous user information if it is available.
     *
     * @param request - request containing session information.
     * @return An anonymous user containing any anonymous question attempts (which could be none)
     */
    private AnonymousUserDTO getAnonymousUserDTO(final HttpServletRequest request) throws SegueDatabaseException {
        return this.dtoMapper.map(this.getAnonymousUserDO(request), AnonymousUserDTO.class);
    }

    /**
     * Retrieves anonymous user information if it is available.
     *
     * @param request - request containing session information.
     * @return An anonymous user containing any anonymous question attempts (which could be none)
     */
    private AnonymousUser getAnonymousUserDO(final HttpServletRequest request) throws SegueDatabaseException {
        AnonymousUser user;

        // no session exists so create one.
        if (request.getSession().getAttribute(ANONYMOUS_USER) == null) {
            String anonymousUserId = getAnonymousUserIdFromRequest(request);
            user = new AnonymousUser(anonymousUserId);
            user.setDateCreated(new Date());
            // add the user reference to the session
            request.getSession().setAttribute(ANONYMOUS_USER, anonymousUserId);
            this.temporaryUserCache.storeAnonymousUser(user);

        } else {
            // reuse existing one
            if (request.getSession().getAttribute(ANONYMOUS_USER) instanceof String) {
                String userId = (String) request.getSession().getAttribute(ANONYMOUS_USER);
                user = this.temporaryUserCache.getById(userId);

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
     * Hide the Jetty internals of session IDs and return an anonymous user ID.
     *
     * @param request - to extract the Jetty session ID
     * @return - a String suitable for use as an anonymous identifier
     */
    private String getAnonymousUserIdFromRequest(final HttpServletRequest request) {
        // TODO - could prepend with API segue version?
        return request.getSession().getId().replace("node0", "");
    }

    /**
     * Update the users' last seen field.
     *
     * @param user of interest
     * @throws SegueDatabaseException - if an error occurs with the update.
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

    /**
     * Logout user from all sessions.
     * Increment the users' session token field to invalidate all other sessions.
     *
     * @param request  - request containing session information.
     * @param response to destroy the segue cookie.
     * @throws NoUserLoggedInException - when the request doesn't have an auth cookie.
     * @throws SegueDatabaseException  - if an error occurs with the update.
     */
    public void logoutEverywhere(final HttpServletRequest request, final HttpServletResponse response)
            throws SegueDatabaseException, NoUserLoggedInException {
        RegisteredUser user = this.getCurrentRegisteredUserDO(request);
        if (null == user) {
            throw new NoUserLoggedInException();
        }
        this.database.incrementSessionToken(user);
        logUserOut(request, response);
    }

    /**
     * @param userDTO                the userDTO of interest
     * @param emailVerificationToken the verifcation token
     * @return verification URL
     */
    private String generateEmailVerificationURL(final RegisteredUserDTO userDTO, final String emailVerificationToken) {
        List<NameValuePair> urlParamPairs = Lists.newArrayList();
        urlParamPairs.add(new BasicNameValuePair("userid", userDTO.getId().toString()));
        urlParamPairs.add(
                new BasicNameValuePair("token", emailVerificationToken.substring(0, Constants.TRUNCATED_TOKEN_LENGTH)));
        String urlParams = URLEncodedUtils.format(urlParamPairs, "UTF-8");

        return String.format("https://%s/verifyemail?%s", properties.getProperty(HOST_NAME), urlParams);
    }

    private String generateAccountDeletionURL(final String deletionToken) {
        List<NameValuePair> urlParamPairs = Lists.newArrayList();
        urlParamPairs.add(
                new BasicNameValuePair("token", deletionToken));
        String urlParams = URLEncodedUtils.format(urlParamPairs, "UTF-8");

        return String.format("https://%s/deleteaccount?%s", properties.getProperty(HOST_NAME), urlParams);
    }

    /**
     * Method to retrieve the number of users by role from the Database.
     *
     * @return a map of role to counter
     */
    public Map<Role, Long> getRoleCount() throws SegueDatabaseException {
        return this.database.getRoleCount();
    }

    /**
     * Count the users by role seen over the previous time interval.
     *
     * @param timeInterval time interval over which to count
     * @return map of counts for each role
     * @throws SegueDatabaseException - if there is a problem with the database.
     */
    public Map<Role, Long> getActiveRolesOverPrevious(TimeInterval timeInterval) throws SegueDatabaseException {
        return this.database.getRolesLastSeenOver(timeInterval);
    }

    /**
     * Count users' reported genders.
     *
     * @return map of counts for each gender.
     * @throws SegueDatabaseException - if there is a problem with the database.
     */
    public Map<Gender, Long> getGenderCount() throws SegueDatabaseException {
        return this.database.getGenderCount();
    }

    /**
     * Count users' reported school information.
     *
     * @return map of counts for students who have provided or not provided school information
     * @throws SegueDatabaseException - if there is a problem with the database.
     */
    public Map<SchoolInfoStatus, Long> getSchoolInfoStats() throws SegueDatabaseException {
        return this.database.getSchoolInfoStats();
    }

    /**
     * Count the number of anonymous users currently in our temporary user cache.
     *
     * @return the number of anonymous users
     * @throws SegueDatabaseException - if there is a problem with the database.
     */
    public Long getNumberOfAnonymousUsers() throws SegueDatabaseException {
        return temporaryUserCache.getCountOfAnonymousUsers();
    }
}