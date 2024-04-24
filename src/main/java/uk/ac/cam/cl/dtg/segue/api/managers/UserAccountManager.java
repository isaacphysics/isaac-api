/**
 * Copyright 2014 Stephen Cummins & Nick Rogers.
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.segue.api.managers;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.text.WordUtils.capitalizeFully;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EMAIL_TEMPLATE_TOKEN_PROVIDER;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.EXCEPTION_MESSAGE_INVALID_EMAIL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANONYMOUS_USER;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EMAIL_SIGNATURE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LAST_SEEN_UPDATE_FREQUENCY_MINUTES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LINK_ACCOUNT_PARAM_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_2FA_REQUIRED_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.RESTRICTED_SIGNUP_EMAIL_REGEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_EXPIRY_SECONDS_DEFAULT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SchoolInfoStatus;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueUserPreferences;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TimeInterval;
import static uk.ac.cam.cl.dtg.segue.api.Constants.USER_ID_FKEY_FIELDNAME;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.users.AnonymousUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Gender;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
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
import uk.ac.cam.cl.dtg.isaac.mappers.UserMapper;
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
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidEmailException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidNameException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.EmailManager;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dao.users.IAnonymousUserDataManager;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * This class is responsible for managing all user data and orchestration of calls to a user Authentication Manager for
 * dealing with sessions and passwords.
 */
public class UserAccountManager implements IUserAccountManager {
  private static final Logger log = LoggerFactory.getLogger(UserAccountManager.class);

  private final IUserDataManager database;
  private final QuestionManager questionAttemptDb;
  private final ILogManager logManager;
  private final UserMapper dtoMapper;
  private final EmailManager emailManager;

  private final IAnonymousUserDataManager temporaryUserCache;

  private final Map<AuthenticationProvider, IAuthenticator> registeredAuthProviders;
  private final UserAuthenticationManager userAuthenticationManager;
  private final PropertiesLoader properties;

  private final ISecondFactorAuthenticator secondFactorManager;

  private final AbstractUserPreferenceManager userPreferenceManager;
  private final SchoolListReader schoolListReader;

  private final Pattern restrictedSignupEmailRegex;
  private static final int USER_NAME_MAX_LENGTH = 255;
  private static final Pattern USER_NAME_PERMITTED_CHARS_REGEX =
      Pattern.compile("^[\\p{L}\\d_\\-' ]+$", Pattern.CANON_EQ);
  private static final Pattern EMAIL_PERMITTED_CHARS_REGEX = Pattern.compile("^[a-zA-Z0-9!#$%&'+\\-=?^_`.{|}~@]+$");
  private static final Pattern EMAIL_CONSECUTIVE_FULL_STOP_REGEX = Pattern.compile("\\.\\.");

  private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

  /**
   * Create an instance of the user manager class.
   *
   * @param database                  - an IUserDataManager that will support persistence.
   * @param questionDb                - allows this class to instruct the questionDB to merge an anonymous user with a
   *                                        registered user.
   * @param properties                - A property loader
   * @param providersToRegister       - A map of known authentication providers.
   * @param dtoMapper                 - the preconfigured DO to DTO object mapper for user objects.
   * @param emailQueue                - the preconfigured communicator manager for sending e-mails.
   * @param temporaryUserCache        - temporary user cache for anonymous users
   * @param logManager                - so that we can log events for users.
   * @param userAuthenticationManager - for managing sessions, passwords and third-party provider links
   * @param secondFactorManager       - for configuring 2FA
   * @param userPreferenceManager     - Allows user preferences to be managed.
   * @param schoolListReader          - to look up a users school.
   */
  @Inject
  public UserAccountManager(final IUserDataManager database, final QuestionManager questionDb,
                            final PropertiesLoader properties,
                            final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
                            final UserMapper dtoMapper,
                            final EmailManager emailQueue, final IAnonymousUserDataManager temporaryUserCache,
                            final ILogManager logManager, final UserAuthenticationManager userAuthenticationManager,
                            final ISecondFactorAuthenticator secondFactorManager,
                            final AbstractUserPreferenceManager userPreferenceManager,
                            final SchoolListReader schoolListReader) {

    requireNonNull(properties.getProperty(HMAC_SALT));
    requireNonNull(properties.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT));
    requireNonNull(properties.getProperty(HOST_NAME));

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
    this.schoolListReader = schoolListReader;

    String forbiddenEmailRegex = properties.getProperty(RESTRICTED_SIGNUP_EMAIL_REGEX);
    if (null == forbiddenEmailRegex || forbiddenEmailRegex.isEmpty()) {
      this.restrictedSignupEmailRegex = null;
    } else {
      this.restrictedSignupEmailRegex = Pattern.compile(forbiddenEmailRegex);
    }
  }

  /**
   * This method will start the authentication process and ultimately provide an url for the client to redirect the
   * user to. This url will be for a 3rd party authenticator who will use the callback method provided after they have
   * authenticated.
   * <br>
   * Users who are already logged will be returned their UserDTO without going through the authentication
   * process.
   *
   * @param request  - http request that we can attach the session to and save redirect url in.
   * @param provider - the provider the user wishes to authenticate with.
   * @return a URI for redirection
   * @throws IOException                            -
   * @throws AuthenticationProviderMappingException - as per exception description.
   */
  public URI authenticate(final HttpServletRequest request, final String provider)
      throws IOException, AuthenticationProviderMappingException {
    return this.userAuthenticationManager.getThirdPartyAuthURI(request, provider);
  }

  /**
   * This method will start the authentication process for linking a user to a 3rd party provider. It will ultimately
   * provide an url for the client to redirect the user to. This url will be for a 3rd party authenticator who will use
   * the callback method provided after they have authenticated.
   * <br>
   * Users must already be logged in to use this method otherwise a 401 will be returned.
   *
   * @param request  - http request that we can attach the session to.
   * @param provider - the provider the user wishes to authenticate with.
   * @return A redirection URI - also this endpoint ensures that the request has a session attribute on so we know
   *     that this is a link request not a new user.
   * @throws IOException                            -
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
   * <br>
   * This method will either register a new user and attach the linkedAccount or locate the existing account of the
   * user and create a session for that.
   *
   * @param request  - http request from the user - should contain url encoded token details.
   * @param response to store the session in our own segue cookie.
   * @param provider - the provider who has just authenticated the user.
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
                                                final HttpServletResponse response, final String provider)
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
      if (providerUserDO.getEmail() != null && !providerUserDO.getEmail().isEmpty()
          && this.findUserByEmail(providerUserDO.getEmail()) != null) {
        log.warn("A user tried to use unknown provider '{}' to log in to an account with matching email ({}).",
            sanitiseExternalLogValue(capitalizeFully(provider)), providerUserDO.getEmail());
        throw new DuplicateAccountException("You do not use " + capitalizeFully(provider) + " to log on to Isaac."
            + " You may have registered using a different provider, or a username and password.");
      }
      // this must be a registration request
      RegisteredUser segueUserDO = this.registerUserWithFederatedProvider(
          authenticator.getAuthenticationProvider(), providerUserDO);
      RegisteredUserDTO segueUserDTO = this.logUserIn(request, response, segueUserDO);
      segueUserDTO.setFirstLogin(true);

      try {
        Map<String, Object> emailTokens = Map.of(EMAIL_TEMPLATE_TOKEN_PROVIDER, capitalizeFully(provider));

        emailManager.sendTemplatedEmailToUser(segueUserDTO,
            emailManager.getEmailTemplateDTO("email-template-registration-confirmation-federated"),
            emailTokens, EmailType.SYSTEM);

      } catch (ContentManagerException e) {
        log.error("Registration email could not be sent due to content issue", e);
      }

      return segueUserDTO;
    }
  }

  /**
   * This method will attempt to authenticate the user using the provided credentials and if successful will log the
   * user in and create a session.
   *
   * @param request  - http request that we can attach the session to.
   * @param response to store the session in our own segue cookie.
   * @param provider - the provider the user wishes to authenticate with.
   * @param email    - the email address of the account holder.
   * @param password - the plain text password.
   * @return A response containing the UserDTO object or a SegueErrorResponse.
   * @throws AuthenticationProviderMappingException    - if we cannot find an authenticator
   * @throws IncorrectCredentialsProvidedException     - if the password is incorrect
   * @throws NoCredentialsAvailableException           - If the account exists but does not have a local password
   * @throws AdditionalAuthenticationRequiredException - If the account has 2FA enabled and we need to initiate that
   *                                                         flow
   * @throws MFARequiredButNotConfiguredException      - If the account type requires 2FA to be configured but none is
   *                                                         enabled for the account
   * @throws SegueDatabaseException                    - if there is a problem with the database.
   */
  public final RegisteredUserDTO authenticateWithCredentials(final HttpServletRequest request,
                                                             final HttpServletResponse response, final String provider,
                                                             final String email, final String password)
      throws AuthenticationProviderMappingException, IncorrectCredentialsProvidedException,
      NoCredentialsAvailableException, SegueDatabaseException, AdditionalAuthenticationRequiredException,
      MFARequiredButNotConfiguredException, InvalidKeySpecException, NoSuchAlgorithmException {
    Validate.notBlank(email);
    Validate.notBlank(password);

    // get the current user based on their session id information.
    RegisteredUserDTO currentUser = this.convertUserDOToUserDTO(this.getCurrentRegisteredUserDO(request));
    if (null != currentUser) {
      log.debug("UserId ({}) already has a valid session - not bothering to reauthenticate", currentUser.getId());
      return currentUser;
    }

    RegisteredUser user = this.userAuthenticationManager.getSegueUserFromCredentials(provider, email, password);
    log.debug("UserId ({}) authenticated with credentials", user.getId());

    // check if user has MFA enabled, if so we can't just log them in - also they won't have the correct cookie
    if (secondFactorManager.has2FAConfigured(convertUserDOToUserDTO(user))) {
      // we can't just log them in we have to set a caveat cookie
      this.partialLogInForMFA(response, user);
      throw new AdditionalAuthenticationRequiredException();
    } else if (Role.ADMIN.equals(user.getRole())) {
      // Admins MUST have 2FA enabled to use password login, so if we reached this point login cannot proceed.
      throw new MFARequiredButNotConfiguredException(LOGIN_2FA_REQUIRED_MESSAGE);
    } else {
      return this.logUserIn(request, response, user);
    }
  }

  /**
   * Create a user object. This method allows new user objects to be created.
   *
   * @param request                - so that we can identify the user
   * @param userObjectFromClient   - the new user object from the clients' perspective.
   * @param newPassword            - the new password for the user.
   * @param userPreferenceObject   - the new preferences for this user
   * @param registeredUserContexts - a List of User Contexts (stage, exam board)
   * @return the updated user object.
   */
  public Response createNewUser(final HttpServletRequest request, final RegisteredUser userObjectFromClient,
                                final String newPassword,
                                final Map<String, Map<String, Boolean>> userPreferenceObject,
                                final List<UserContext> registeredUserContexts)
      throws InvalidKeySpecException, NoSuchAlgorithmException {
    try {
      RegisteredUserDTO savedUser =
          this.createAndSaveUserObject(request, userObjectFromClient, newPassword, registeredUserContexts);

      if (userPreferenceObject != null) {
        List<UserPreference> userPreferences = userPreferenceObjectToList(userPreferenceObject, savedUser.getId());
        userPreferenceManager.saveUserPreferences(userPreferences);
      }

      return Response.ok().build();
    } catch (InvalidPasswordException e) {
      log.warn("Invalid password exception occurred during registration!");
      return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
    } catch (MissingRequiredFieldException e) {
      log.warn("Missing field during update operation: {}", e.getMessage());
      return new SegueErrorResponse(Response.Status.BAD_REQUEST, "You are missing a required field. "
          + "Please make sure you have specified all mandatory fields in your response.").toResponse();
    } catch (DuplicateAccountException e) {
      log.warn("Duplicate account registration attempt for ({})",
          sanitiseExternalLogValue(userObjectFromClient.getEmail()));
      sendRegistrationDuplicateEmail(userObjectFromClient.getEmail());
      // For security reasons, an otherwise-valid request for existing account should return the same response as for
      // a new one
      return Response.ok().build();
    } catch (SegueDatabaseException e) {
      String errorMsg = "Unable to set a password, due to an internal database error.";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (EmailMustBeVerifiedException e) {
      log.warn("Someone attempted to register with an Isaac email address: {}",
          sanitiseExternalLogValue(userObjectFromClient.getEmail()));
      return new SegueErrorResponse(Response.Status.BAD_REQUEST,
          "You cannot register with an Isaac email address.").toResponse();
    } catch (InvalidNameException e) {
      log.warn("Invalid name provided during registration.");
      return new SegueErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()).toResponse();
    } catch (InvalidEmailException e) {
      log.warn("Invalid email address provided during registration.");
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
        log.warn("Unknown user preference type '{}' provided. Skipping.", sanitiseExternalLogValue(preferenceType));
        continue;
      }

      if (EnumUtils.isValidEnum(SegueUserPreferences.class, preferenceType)
          && SegueUserPreferences.EMAIL_PREFERENCE.equals(SegueUserPreferences.valueOf(preferenceType))) {
        // This is an email preference, which is treated specially:
        for (String preferenceName : userPreferenceObject.get(preferenceType).keySet()) {
          if (!EnumUtils.isValidEnum(EmailType.class, preferenceName)
              || !EmailType.valueOf(preferenceName).isValidEmailPreference()) {
            log.warn("Invalid email preference name '{}' provided for '{}", sanitiseExternalLogValue(preferenceName),
                sanitiseExternalLogValue(preferenceType));
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
          log.error("Failed to find allowed user preferences names for '{}'! Has it been configured?",
              sanitiseExternalLogValue(preferenceType));
          acceptedPreferenceNamesProperty = "";
        }
        List<String> acceptedPreferenceNames = Arrays.asList(acceptedPreferenceNamesProperty.split(","));
        for (String preferenceName : userPreferenceObject.get(preferenceType).keySet()) {
          if (!acceptedPreferenceNames.contains(preferenceName)) {
            log.warn("Invalid user preference name '{}' provided for type '{}'! Skipping.",
                sanitiseExternalLogValue(preferenceName), sanitiseExternalLogValue(preferenceType));
            continue;
          }
          boolean preferenceValue = userPreferenceObject.get(preferenceType).get(preferenceName);
          userPreferences.add(new UserPreference(userId, preferenceType, preferenceName, preferenceValue));
        }
      } else {
        log.warn(
            "Unexpected user preference type '{}' provided. Skipping.", sanitiseExternalLogValue(preferenceType));
      }
    }
    return userPreferences;
  }

  /**
   * Update a user object.
   * <br>
   * This method does all of the necessary security checks to determine who is allowed to edit what.
   *
   * @param request                - so that we can identify the user
   * @param userObjectFromClient   - the new user object from the clients' perspective.
   * @param passwordCurrent        - the current password, used if the password has changed
   * @param newPassword            - the new password, used if the password has changed
   * @param userPreferenceObject   - the preferences for this user
   * @param registeredUserContexts - a List of User Contexts (stage, exam board)
   * @return the updated user object.
   * @throws IncorrectCredentialsProvidedException - if the password is incorrect
   * @throws NoCredentialsAvailableException       - if the account exists but does not have a local password
   * @throws InvalidKeySpecException               - if the preconfigured key spec is invalid
   * @throws NoSuchAlgorithmException              - if the configured algorithm is not valid
   */
  public Response updateUserObject(final HttpServletRequest request,
                                   final RegisteredUser userObjectFromClient, final String passwordCurrent,
                                   final String newPassword,
                                   final Map<String, Map<String, Boolean>> userPreferenceObject,
                                   final List<UserContext> registeredUserContexts)
      throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException, InvalidKeySpecException,
      NoSuchAlgorithmException {
    requireNonNull(userObjectFromClient.getId());

    // this is an update as the user has an id
    // security checks
    try {
      // check that the current user has permissions to change this user's details.
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
      if (null == userObjectFromClient.getRole() || !existingUserFromDb.getRole()
          .equals(userObjectFromClient.getRole())) {
        return new SegueErrorResponse(Response.Status.FORBIDDEN,
            "You cannot change a users role.").toResponse();
      }

      if (registeredUserContexts != null) {
        // We always set the last confirmed date from code rather than trusting the client
        userObjectFromClient.setRegisteredContexts(registeredUserContexts);
        userObjectFromClient.setRegisteredContextsLastConfirmed(Instant.now());
      } else {
        /* Registered contexts should only ever be set via the registeredUserContexts object, so that it is the
           server that sets the time that they last confirmed their user context values.
           To ensure this, we overwrite the fields with the values already set in the db if registeredUserContexts is
           null
         */
        userObjectFromClient.setRegisteredContexts(existingUserFromDb.getRegisteredContexts());
        userObjectFromClient.setRegisteredContextsLastConfirmed(
            existingUserFromDb.getRegisteredContextsLastConfirmed());
      }

      if (userObjectFromClient.getTeacherPending() == null) {
        userObjectFromClient.setTeacherPending(existingUserFromDb.getTeacherPending());
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
      InvalidKeySpecException, NoSuchAlgorithmException, InvalidNameException, NoUserException {
    requireNonNull(updatedUser.getId());

    // We want to map to DTO first to make sure that the user cannot
    // change fields that aren't exposed to them
    RegisteredUserDTO userDTOContainingUpdates = this.dtoMapper.map(updatedUser);
    if (updatedUser.getId() == null) {
      throw new IllegalArgumentException(
          "The user object specified does not have an id. Users cannot be updated without a specific id set.");
    }

    // This is an update operation.
    final RegisteredUser existingUser = this.findUserById(updatedUser.getId());
    if (existingUser == null) {
      throw new NoUserException("User to be updated could not be found.");
    }

    IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
        .get(AuthenticationProvider.SEGUE);

    validateUpdatingUserDetails(updatedUser, newPassword, existingUser, authenticator);

    RegisteredUser userToSave =
        constructUpdatedUserObject(updatedUser, userDTOContainingUpdates, existingUser, authenticator);

    // save the user
    RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);
    if (null != newPassword && !newPassword.isEmpty()) {
      authenticator.setOrChangeUsersPassword(userToReturn, newPassword);
    }

    // return it to the caller
    return this.convertUserDOToUserDTO(userToReturn);
  }

  private RegisteredUser constructUpdatedUserObject(RegisteredUser updatedUser,
                                                    RegisteredUserDTO userDTOContainingUpdates,
                                                    RegisteredUser existingUser, IPasswordAuthenticator authenticator)
      throws MissingRequiredFieldException, SegueDatabaseException {
    RegisteredUser userToSave = dtoMapper.copy(existingUser);
    dtoMapper.merge(userDTOContainingUpdates, userToSave);
    // Don't modify email verification status, registration date, or role
    userToSave.setEmailVerificationStatus(existingUser.getEmailVerificationStatus());
    userToSave.setRegistrationDate(existingUser.getRegistrationDate());
    userToSave.setRole(existingUser.getRole());
    userToSave.setLastUpdated(Instant.now());

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
    if (!this.isUserValid(userToSave)) {
      throw new MissingRequiredFieldException(EXCEPTION_MESSAGE_INVALID_EMAIL);
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
        log.error("ContentManagerException during sendEmailVerificationChange {}", e.getMessage());
      }

      userToSave.setEmail(existingUser.getEmail());
    }
    return userToSave;
  }

  private void validateUpdatingUserDetails(RegisteredUser updatedUser, String newPassword, RegisteredUser existingUser,
                         IPasswordAuthenticator authenticator)
      throws SegueDatabaseException, InvalidNameException, InvalidPasswordException {
    // Check that the user isn't trying to take an existing users e-mail.
    if (this.findUserByEmail(updatedUser.getEmail()) != null && !existingUser.getEmail()
        .equals(updatedUser.getEmail())) {
      throw new DuplicateAccountException("An account with that e-mail address already exists.");
    }

    // validate names
    if (!isUserNameValid(updatedUser.getGivenName())) {
      throw new InvalidNameException("The given name provided is an invalid length or contains forbidden characters.");
    }

    if (!isUserNameValid(updatedUser.getFamilyName())) {
      throw new InvalidNameException("The family name provided is an invalid length or contains forbidden characters.");
    }

    // Check if there is a new password and it is invalid as early as possible:
    if (null != newPassword && !newPassword.isEmpty()) {
      authenticator.ensureValidPassword(newPassword);
    }
  }

  /**
   * Complete the MFA login process. If the correct TOTPCode is provided we will give the user a full session cookie
   * rather than a partial one.
   *
   * @param request  - containing the partially logged-in user.
   * @param response - response will be updated to include fully logged in cookie if TOTPCode is successfully verified
   * @param totpCode - code to verify
   * @return RegisteredUserDTO as they are now considered logged in.
   * @throws IncorrectCredentialsProvidedException - if the password is incorrect
   * @throws NoCredentialsAvailableException       - If the account exists but does not have a local password
   * @throws NoUserLoggedInException               - If the user hasn't completed the first step of the authentication
   *                                                     process.
   * @throws SegueDatabaseException                - if there is a problem with the database.
   */
  public RegisteredUserDTO authenticateMFA(final HttpServletRequest request, final HttpServletResponse response,
                                           final Integer totpCode)
      throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException, SegueDatabaseException,
      NoUserLoggedInException {
    RegisteredUser registeredUser = this.retrievePartialLogInForMFA(request);

    if (registeredUser == null) {
      throw new NoUserLoggedInException();
    }

    RegisteredUserDTO userToReturn = convertUserDOToUserDTO(registeredUser);
    this.secondFactorManager.authenticate2ndFactor(userToReturn, totpCode);

    // replace cookie to no longer have caveat
    return this.logUserIn(request, response, registeredUser);
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
   * <br>
   * Removes the link between a user and a provider.
   *
   * @param user           - user to affect.
   * @param providerString - provider to unassociated.
   * @throws SegueDatabaseException                 - if there is an error during the database update.
   * @throws MissingRequiredFieldException          - If the change will mean that the user will be unable to login
   *                                                      again.
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
  public boolean checkUserRole(final RegisteredUserDTO user, final Collection<Role> validRoles)
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
   * <br>
   * This method will validate the session and will throw a NoUserLoggedInException if invalid.
   *
   * @param request - to retrieve session information from
   * @return Returns the current UserDTO if we can get it or throw a NoUserLoggedInException
   *         if user is not currently logged in
   * @throws NoUserLoggedInException - When the session has expired or there is no user currently logged in.
   */
  public RegisteredUserDTO getCurrentRegisteredUser(final HttpServletRequest request)
      throws NoUserLoggedInException {
    requireNonNull(request);

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
   * <br>
   * Does not check session validity.
   *
   * @param request The request to extract the session information from
   * @return The session expiry as a Date
   */
  public Instant getSessionExpiry(final HttpServletRequest request) {
    return userAuthenticationManager.getSessionExpiry(request);
  }

  /**
   * Get the authentication settings of particular user.
   *
   * @param user - to retrieve settings from
   * @return Returns the current UserDTO if we can get it or null if user is not currently logged in
   * @throws SegueDatabaseException - If there is an internal database error
   */
  public final UserAuthenticationSettingsDTO getUsersAuthenticationSettings(final RegisteredUserDTO user)
      throws SegueDatabaseException {
    requireNonNull(user);

    UserAuthenticationSettings userAuthenticationSettings = this.database.getUserAuthenticationSettings(user.getId());
    if (userAuthenticationSettings != null) {
      return this.dtoMapper.map(userAuthenticationSettings);
    } else {
      return new UserAuthenticationSettingsDTO();
    }
  }

  /**
   * Find a list of users based on some user prototype.
   *
   * @param prototype - partially completed user object to base search on
   * @return list of registered user dtos.
   * @throws SegueDatabaseException - if there is a database error.
   */
  public List<RegisteredUserDTO> findUsers(final RegisteredUserDTO prototype) throws SegueDatabaseException {
    List<RegisteredUser> registeredUsersDOs = this.database.findUsers(this.dtoMapper.map(prototype));

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
    requireNonNull(userIds);
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
  public RegisteredUserDTO getUserDTOById(final Long id) throws NoUserException, SegueDatabaseException {
    return this.getUserDTOById(id, false);
  }

  /**
   * This function can be used to find user information about a user when given an id - EVEN if it is a deleted user.
   * <br>
   * WARNING - Do not expect complete RegisteredUser Objects as data may be missing if you include deleted users
   *
   * @param id             - the id of the user to search for.
   * @param includeDeleted - include deleted users in results - true for yes false for no
   * @return the userDTO
   * @throws NoUserException        - If we cannot find a valid user with the email address provided.
   * @throws SegueDatabaseException - If there is another database error
   */
  @Override
  public RegisteredUserDTO getUserDTOById(final Long id, final boolean includeDeleted) throws NoUserException,
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
   * <br>
   * If the user is currently logged in you will get a RegisteredUserDTO otherwise you will get an AnonymousUserDTO
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
  public void logUserOut(final HttpServletRequest request, final HttpServletResponse response)
      throws NoUserLoggedInException, SegueDatabaseException {
    requireNonNull(request);
    this.userAuthenticationManager.destroyUserSession(request, response);
  }

  /**
   * Method to create a user object in our database and log them in.
   * <br>
   * Note: this method is intended for creation of accounts in segue - not for linked account registration.
   *
   * @param request                to enable access to anonymous user information.
   * @param user                   - the user DO to use for updates - must not contain a user id.
   * @param newPassword            - new password for the account being created.
   * @param registeredUserContexts - a List of User Contexts (stage, exam board)
   * @return the user object as was saved.
   * @throws InvalidPasswordException      - the password provided does not meet our requirements.
   * @throws MissingRequiredFieldException - A required field is missing for the user object so cannot be saved.
   * @throws SegueDatabaseException        - If there is an internal database error.
   * @throws EmailMustBeVerifiedException  - if a user attempts to sign up with an email that must be verified before it
   *                                             can be used (i.e. an @isaaccomputerscience.org address).
   */
  public RegisteredUserDTO createAndSaveUserObject(
      final HttpServletRequest request, final RegisteredUser user, final String newPassword,
      final List<UserContext> registeredUserContexts)
      throws InvalidPasswordException, MissingRequiredFieldException, SegueDatabaseException,
      EmailMustBeVerifiedException, InvalidKeySpecException, NoSuchAlgorithmException, InvalidNameException,
      InvalidEmailException {
    Validate.isTrue(user.getId() == null, "When creating a new user the user id must not be set.");

    // Ensure nobody registers with Isaac email addresses. Users can change emails to restricted ones by verifying them,
    // however.
    if (null != restrictedSignupEmailRegex && restrictedSignupEmailRegex.matcher(user.getEmail()).find()) {
      log.warn("User attempted to register with Isaac email address '{}'!", sanitiseExternalLogValue(user.getEmail()));
      throw new EmailMustBeVerifiedException("You cannot register with an Isaac email address.");
    }

    // We want to map to DTO first to make sure that the user cannot change fields that aren't exposed to them
    RegisteredUserDTO userDtoForNewUser = this.dtoMapper.map(user);
    RegisteredUser userToSave = this.dtoMapper.map(userDtoForNewUser);

    // Set defaults
    userToSave.setRole(Role.STUDENT);
    userToSave.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
    userToSave.setRegistrationDate(Instant.now());
    userToSave.setLastUpdated(Instant.now());

    if (registeredUserContexts != null) {
      // We always set the last confirmed date from code rather than trusting the client
      userToSave.setRegisteredContexts(registeredUserContexts);
      userToSave.setRegisteredContextsLastConfirmed(Instant.now());
    }

    if (userToSave.getTeacherPending() == null) {
      userToSave.setTeacherPending(false);
    }

    IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
        .get(AuthenticationProvider.SEGUE);

    // Before saving, we should validate the user for mandatory fields
    validateNewUserDetails(user, newPassword, userToSave, authenticator);

    // Generate email verification token and add to user object
    authenticator.createEmailVerificationTokenForUser(userToSave, userToSave.getEmail());

    // save the user object to database and generate the new userId
    RegisteredUser userToReturn = this.database.createOrUpdateUser(userToSave);

    // Create password for the user (adds directly to credentials table)
    authenticator.setOrChangeUsersPassword(userToReturn, newPassword);

    // Send a confirmation email, including the verification token
    sendRegistrationConfirmationEmail(userToReturn);

    logManager.logEvent(this.convertUserDOToUserDTO(userToReturn), request, SegueServerLogType.USER_REGISTRATION,
        ImmutableMap.builder().put("provider", AuthenticationProvider.SEGUE.name()).build());

    // return it to the caller.
    return this.dtoMapper.map(userToReturn);
  }

  private void validateNewUserDetails(RegisteredUser user, String newPassword, RegisteredUser userToSave,
                                      IPasswordAuthenticator authenticator)
      throws InvalidNameException, InvalidPasswordException, MissingRequiredFieldException, SegueDatabaseException,
      InvalidEmailException {
    if (user.getGivenName() == null || user.getGivenName().isEmpty() || user.getFamilyName() == null
        || user.getFamilyName().isEmpty() || user.getEmail() == null || user.getEmail().isEmpty() || newPassword == null
        || newPassword.isEmpty()) {
      throw new MissingRequiredFieldException("One or more required fields are missing.");
    }

    // validate names
    if (!isUserNameValid(user.getGivenName())) {
      throw new InvalidNameException("The given name provided is an invalid length or contains forbidden characters.");
    }

    if (!isUserNameValid(user.getFamilyName())) {
      throw new InvalidNameException("The family name provided is an invalid length or contains forbidden characters.");
    }

    // FIXME: Before creating the user object, ensure password is valid. This should really be in a transaction.
    authenticator.ensureValidPassword(newPassword);

    // Validate email address and check for existing accounts last to help mitigate enumeration attacks
    if (!this.isUserValid(userToSave)) {
      throw new InvalidEmailException(EXCEPTION_MESSAGE_INVALID_EMAIL);
    }

    if (this.findUserByEmail(user.getEmail()) != null) {
      throw new DuplicateAccountException(EXCEPTION_MESSAGE_INVALID_EMAIL);
    }
  }

  private void sendRegistrationConfirmationEmail(RegisteredUser userToReturn) throws SegueDatabaseException {
    try {
      RegisteredUserDTO userToReturnDTO = this.getUserDTOById(userToReturn.getId());

      Map<String, Object> emailTokens = Map.of("verificationURL",
          generateEmailVerificationURL(userToReturnDTO, userToReturn.getEmailVerificationToken()));

      emailManager.sendTemplatedEmailToUser(userToReturnDTO,
          emailManager.getEmailTemplateDTO("email-template-registration-confirmation"),
          emailTokens, EmailType.SYSTEM);

    } catch (ContentManagerException e) {
      log.error("Registration email could not be sent due to content issue: {}", e.getMessage());
    } catch (NoUserException e) {
      log.error("Registration email could not be sent due to not being able to locate the user: {}", e.getMessage());
    }
  }

  private void sendRegistrationDuplicateEmail(String targetUserEmail) {
    try {
      RegisteredUserDTO existingUser = this.getUserDTOByEmail(targetUserEmail);

      Map<String, Object> emailTokens = Map.of(
          "givenName", existingUser.getGivenName(),
          "email", existingUser.getEmail(),
          "siteBaseURL", String.format("https://%s", properties.getProperty(HOST_NAME)),
          "contactUsURL", String.format("https://%s/contact", properties.getProperty(HOST_NAME)),
          "sig", properties.getProperty(EMAIL_SIGNATURE)
      );

      emailManager.sendTemplatedEmailToUser(existingUser,
          emailManager.getEmailTemplateDTO("email-template-registration-duplicate"),
          emailTokens, EmailType.SYSTEM);

    } catch (ContentManagerException e) {
      log.error("Duplicate registration email could not be sent due to content issue: {}", e.getMessage());
    } catch (NoUserException e) {
      log.error("Duplicate registration email could not be sent due to not being able to locate the user: {}",
          e.getMessage());
    } catch (SegueDatabaseException e) {
      log.error("Duplicate registration email could not be sent due to an error while constructing the email: {}",
          e.getMessage());
    }
  }

  /**
   * @param id            - the user id
   * @param requestedRole - the new role
   * @throws SegueDatabaseException - an exception when accessing the database
   */
  public void updateUserRole(final Long id, final Role requestedRole) throws SegueDatabaseException, NoUserException {
    requireNonNull(requestedRole);
    RegisteredUser userToSave = this.findUserById(id);
    if (userToSave == null) {
      // This shouldn't happen under current usage but guard against it just in case
      throw new NoUserException("No user found with this ID.");
    }

    // Send welcome email if user has become teacher or tutor, otherwise, role change notification
    try {
      RegisteredUserDTO existingUserDTO = this.getUserDTOById(id);
      if (userToSave.getRole() != requestedRole) {
        String emailTemplate = switch (requestedRole) {
          case TUTOR -> "email-template-tutor-welcome";
          case TEACHER -> "email-template-teacher-welcome";
          default -> "email-template-default-role-change";
        };
        emailManager.sendTemplatedEmailToUser(existingUserDTO,
            emailManager.getEmailTemplateDTO(emailTemplate),
            Map.of("oldrole", existingUserDTO.getRole().toString(), "newrole", requestedRole.toString()),
            EmailType.SYSTEM);
      }
    } catch (ContentManagerException | NoUserException e) {
      log.error("Error sending email", e);
    }

    userToSave.setRole(requestedRole);
    userToSave.setTeacherPending(false);
    this.database.createOrUpdateUser(userToSave);
  }

  /**
   * @param email                            - the user email
   * @param requestedEmailVerificationStatus - the new email verification status
   * @throws SegueDatabaseException - an exception when accessing the database
   */
  public void updateUserEmailVerificationStatus(final String email,
                                                final EmailVerificationStatus requestedEmailVerificationStatus)
      throws SegueDatabaseException {
    requireNonNull(requestedEmailVerificationStatus);
    RegisteredUser userToSave = this.findUserByEmail(email);
    if (null == userToSave) {
      log.warn("Could not update email verification status of email address ({}) - does not exist",
          sanitiseExternalLogValue(email));
      return;
    }
    userToSave.setEmailVerificationStatus(requestedEmailVerificationStatus);
    userToSave.setLastUpdated(Instant.now());
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
   * @return true if the request was successfully submitted or false if the user was not found
   * @throws SegueDatabaseException - If there is an internal database error.
   */
  public final boolean resetPasswordRequest(final RegisteredUserDTO userObject)
      throws SegueDatabaseException {
    RegisteredUser user = this.findUserByEmail(userObject.getEmail());

    if (null == user) {
      return false;
    }

    EXECUTOR.submit(() -> {
      RegisteredUserDTO userDTO = this.convertUserDOToUserDTO(user);
      try {
        this.userAuthenticationManager.resetPasswordRequest(user, userDTO);
        log.info("Password reset sent");
      } catch (CommunicationException e) {
        log.error("Error sending reset message.", e);
      } catch (SegueDatabaseException | InvalidKeySpecException | NoSuchAlgorithmException e) {
        log.error("Error generating password reset token.", e);
      }
    });
    return true;
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
      RegisteredUserDTO userDTO = getCurrentRegisteredUser(request);
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
          + "and user not logged in!", sanitiseExternalLogValue(email)));
    } catch (ContentManagerException e) {
      log.error("ContentManagerException", e);
    }
  }

  /**
   * This method will test if the specified token is a valid password reset token.
   * <br>
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
  public RegisteredUserDTO processEmailVerification(final Long userId, final String token)
      throws SegueDatabaseException, InvalidTokenException, NoUserException {
    IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
        .get(AuthenticationProvider.SEGUE);

    RegisteredUser user = this.findUserById(userId);

    if (null == user) {
      log.warn("Received an invalid email token request for ({})", userId);
      throw new NoUserException("No user found with this userId!");
    }

    if (!userId.equals(user.getId())) {
      log.warn("Received an invalid email token request by ({}) - provided bad userid", user.getId());
      throw new InvalidTokenException();
    }

    EmailVerificationStatus evStatus = user.getEmailVerificationStatus();
    if (evStatus == EmailVerificationStatus.VERIFIED
        && user.getEmail().equals(user.getEmailToVerify())) {
      log.warn("Received a duplicate email verification request for ({}) - already verified", user.getEmail());
      return this.convertUserDOToUserDTO(user);
    }

    if (authenticator.isValidEmailVerificationToken(user, token)) {
      user.setEmailVerificationStatus(EmailVerificationStatus.VERIFIED);
      user.setEmail(user.getEmailToVerify());
      user.setEmailVerificationToken(null);
      user.setEmailToVerify(null);
      user.setLastUpdated(Instant.now());

      // Save user
      RegisteredUser createOrUpdateUser = this.database.createOrUpdateUser(user);
      log.info("Email verification for user ({}) has completed successfully.", createOrUpdateUser.getId());
      return this.convertUserDOToUserDTO(createOrUpdateUser);
    } else {
      log.warn("Received an invalid email verification token for ({}) - invalid token", userId);
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
   * @return true if yes, false if not.
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
  public boolean activateMFAForUser(final RegisteredUserDTO user, final String sharedSecret,
                                    final Integer codeSubmitted) throws SegueDatabaseException {
    return this.secondFactorManager.activate2FAForUser(user, sharedSecret, codeSubmitted);
  }

  /**
   * Deactivate MFA for user's account - should only be used by admins!.
   *
   * @param user - the User to deactivate 2FA for
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
   * @param <T>             - The type parameter representing the detailed DTO class
   * @param userToConvert   - Full user object.
   * @param detailedDtoClass - The DTO class type into which the user object is to be converted
   * @return a summarised DTO object with details as per the specified detailedDTOClass
   */
  public <T extends UserSummaryDTO> T convertToUserSummary(final RegisteredUserDTO userToConvert,
                                                           final Class<T> detailedDtoClass) {
    return this.dtoMapper.map(userToConvert, detailedDtoClass);
  }

  /**
   * Helper method to convert user objects into cutdown userSummary DTOs.
   *
   * @param userListToConvert - full user objects.
   * @return a list of summarised objects with minimal personal information
   */
  public List<UserSummaryDTO> convertToUserSummaryObjectList(final List<RegisteredUserDTO> userListToConvert) {
    requireNonNull(userListToConvert);
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
  public List<UserSummaryWithEmailAddressDTO> convertToDetailedUserSummaryObjectList(
      final List<RegisteredUserDTO> userListToConvert,
      final Class<? extends UserSummaryWithEmailAddressDTO> detailedDTO) {
    requireNonNull(userListToConvert);
    List<UserSummaryWithEmailAddressDTO> resultList = Lists.newArrayList();
    for (RegisteredUserDTO user : userListToConvert) {
      resultList.add(this.convertToUserSummary(user, detailedDTO));
    }
    return resultList;
  }

  /**
   * Get the user object from the partially completed cookie.
   * <br>
   * WARNING: Do not use this method to determine if a user has successfully logged in or not as they could have omitted
   * the 2FA step.
   *
   * @param request to pull back the user
   * @return UserSummaryDTO of the partially logged-in user or will throw an exception if cannot be found.
   * @throws NoUserLoggedInException if they haven't started the flow.
   */
  public UserSummaryWithEmailAddressDTO getPartiallyIdentifiedUser(final HttpServletRequest request)
      throws NoUserLoggedInException {
    RegisteredUser registeredUser = this.retrievePartialLogInForMFA(request);
    if (null == registeredUser) {
      throw new NoUserLoggedInException();
    }
    return this.convertToUserSummary(this.convertUserDOToUserDTO(registeredUser),
        UserSummaryWithEmailAddressDTO.class);
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
        Map.of("verificationURL", this.generateEmailVerificationURL(userDTO, emailVerificationToken));

    log.info("Sending email verification message to {}", sanitiseExternalLogValue(userDTO.getEmail()));

    emailManager.sendTemplatedEmailToUser(userDTO, emailVerificationTemplate, emailTokens, EmailType.SYSTEM);
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
    Map<String, Object> emailTokens = Map.of("requestedemail", newEmail);

    log.info("Sending email for email address change for user ({}) from email ({}) to email ({})", userDTO.getId(),
        userDTO.getEmail(), sanitiseExternalLogValue(newEmail));
    emailManager.sendTemplatedEmailToUser(userDTO, emailChangeTemplate, emailTokens, EmailType.SYSTEM);

    // Defensive copy to ensure old email address is preserved (shouldn't change until new email is verified)
    RegisteredUserDTO temporaryUser = this.dtoMapper.copy(userDTO);
    temporaryUser.setEmail(newEmail);
    temporaryUser.setEmailVerificationStatus(EmailVerificationStatus.NOT_VERIFIED);
    this.sendVerificationEmailForCurrentEmail(temporaryUser, newEmailToken);
  }

  /**
   * Logs the user in and creates the signed sessions.
   *
   * @param request  - for the session to be attached
   * @param response - for the session to be attached.
   * @param user     - the user who is being logged in.
   * @return the DTO version of the user.
   * @throws SegueDatabaseException - if there is a problem with the database.
   */
  private RegisteredUserDTO logUserIn(final HttpServletRequest request, final HttpServletResponse response,
                                      final RegisteredUser user) throws SegueDatabaseException {
    AnonymousUser anonymousUser = this.getAnonymousUserDO(request);
    if (anonymousUser != null) {
      log.debug("Anonymous User ({}) located during login - need to merge question information",
          anonymousUser.getSessionId());
    }

    // now we want to clean up any data generated by the user while they weren't logged in.
    mergeAnonymousUserWithRegisteredUser(anonymousUser, user);

    return this.convertUserDOToUserDTO(this.userAuthenticationManager.createUserSession(response, user));
  }

  /**
   * Generate a partially logged-in session for the user based on successful password authentication.
   * <br>
   * To complete this the user must also complete MFA authentication.
   *
   * @param response - response to update cookie information
   * @param user     - user of interest
   */
  private void partialLogInForMFA(final HttpServletResponse response, final RegisteredUser user)
      throws SegueDatabaseException {
    this.userAuthenticationManager.createIncompleteLoginUserSession(response, user);
  }

  /**
   * Retrieve a partially logged-in session for the user based on successful password authentication.
   * <br>
   * NOTE: You should not treat users has having logged in using this method as they haven't completed login.
   *
   * @param request - http request containing the cookie
   * @return the user retrieved using the id extracted from the cookie
   */
  private RegisteredUser retrievePartialLogInForMFA(final HttpServletRequest request) {
    return this.userAuthenticationManager.getUserFromSession(request, true);
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
            this.dtoMapper.map(anonymousUser), userDTO);

        // may as well spawn a new thread to do the log migration stuff asynchronously
        // work now.
        Thread logMigrationJob = new Thread() {
          @Override
          public void run() {
            // run this asynchronously as there is no need to block and it is quite slow.
            logManager.transferLogEventsToRegisteredUser(anonymousUser.getSessionId(), user.getId()
                .toString());

            logManager.logInternalEvent(userDTO, SegueServerLogType.MERGE_USER,
                Map.of("oldAnonymousUserId", anonymousUser.getSessionId()));

            // delete the session attribute as merge has completed.
            try {
              temporaryUserCache.deleteAnonymousUser(anonymousUser);
            } catch (SegueDatabaseException e) {
              log.error("Unable to delete anonymous user during merge operation.", e);
            }
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
  public RegisteredUser findUserByEmail(final String email) throws SegueDatabaseException {
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
                                                           final UserFromAuthProvider userFromProvider)
      throws NoUserException, SegueDatabaseException {

    log.debug("New registration ({}) as user does not already exist.", federatedAuthenticator);

    if (null == userFromProvider) {
      log.warn("Unable to create user for the provider {}", federatedAuthenticator);
      throw new NoUserException("No user returned by the provider!");
    }

    RegisteredUser newLocalUser = this.dtoMapper.map(userFromProvider, RegisteredUser.class);
    newLocalUser.setRegistrationDate(Instant.now());

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
   * IsUserValid This function will check that the user object is valid.
   *
   * @param userToValidate - the user to validate.
   * @return true if it meets the internal storage requirements, false if not.
   */
  private boolean isUserValid(final RegisteredUser userToValidate) {
    return userToValidate.getEmail() != null && isEmailValid(userToValidate.getEmail());
  }

  public static boolean isEmailValid(final String email) {
    return email != null
        && !email.isEmpty()
        && email.matches("^.+(?:@(?:[a-zA-Z0-9-]{1,63}+\\.)++[a-zA-Z]{1,63}+|-(?:facebook|google|twitter))$")
        && EMAIL_PERMITTED_CHARS_REGEX.matcher(email).matches()
        && !EMAIL_CONSECUTIVE_FULL_STOP_REGEX.matcher(email).find();
  }

  /**
   * This function checks that the name provided is valid.
   *
   * @param name - the name to validate.
   * @return true if the name is valid, false otherwise.
   */
  public static boolean isUserNameValid(final String name) {
    return null != name
        && !name.isEmpty()
        && !name.isBlank()
        && name.length() <= USER_NAME_MAX_LENGTH
        && USER_NAME_PERMITTED_CHARS_REGEX.matcher(name).matches();
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
    List<RegisteredUser> userDOs = users.parallelStream().filter(Objects::nonNull).toList();
    if (userDOs.isEmpty()) {
      return new ArrayList<>();
    }

    return users.parallelStream().map(this.dtoMapper::map).toList();
  }

  /**
   * Get the RegisteredUserDO of the currently logged-in user. This is for internal use only.
   * <br>
   * This method will validate the session as well returning null if it is invalid.
   *
   * @param request - to retrieve session information from
   * @return Returns the current UserDTO if we can get it or null if user is not currently logged in / there is an
   *     invalid session
   */
  private RegisteredUser getCurrentRegisteredUserDO(final HttpServletRequest request) {
    return this.userAuthenticationManager.getUserFromSession(request, false);
  }

  /**
   * Retrieves anonymous user information if it is available.
   *
   * @param request - request containing session information.
   * @return An anonymous user containing any anonymous question attempts (which could be none)
   */
  private AnonymousUserDTO getAnonymousUserDTO(final HttpServletRequest request) throws SegueDatabaseException {
    return this.dtoMapper.map(this.getAnonymousUserDO(request));
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
      user.setDateCreated(Instant.now());
      // add the user reference to the session
      request.getSession().setAttribute(ANONYMOUS_USER, anonymousUserId);
      this.temporaryUserCache.storeAnonymousUser(user);

    } else {
      // reuse existing one
      if (request.getSession().getAttribute(ANONYMOUS_USER) instanceof String userId) {
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
      long minutesElapsed = Math.abs(Duration.between(Instant.now(), user.getLastSeen()).toMinutes());
      if (minutesElapsed > LAST_SEEN_UPDATE_FREQUENCY_MINUTES) {
        this.database.updateUserLastSeen(user);
      }
    }
  }

  /**
   * @param userDTO                the userDTO of interest
   * @param emailVerificationToken the verification token
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
   * @param timeIntervals An array of time ranges (in string format) for which to get the user counts.
   *                      Each time range is used in the SQL query to filter the results.
   * @return map of counts for each role
   * @throws SegueDatabaseException - if there is a problem with the database.
   */
  public Map<TimeInterval, Map<Role, Long>> getActiveRolesOverPrevious(final TimeInterval[] timeIntervals)
      throws SegueDatabaseException {
    return this.database.getRolesLastSeenOver(timeIntervals);
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

  public RegisteredUserDTO updateTeacherPendingFlag(final Long userId, boolean newFlagValue)
      throws SegueDatabaseException, NoUserException {
    RegisteredUser user = findUserById(userId);
    if (user == null) {
      throw new NoUserException(String.format("No user found with ID: %s", userId));
    }
    user.setTeacherPending(newFlagValue);
    RegisteredUser updatedUser = database.createOrUpdateUser(user);
    return dtoMapper.map(updatedUser);
  }

  @SuppressWarnings({"java:S3457", "java:S6126"})
  public void sendRoleChangeRequestEmail(final HttpServletRequest request, final RegisteredUserDTO user,
                                         final Role requestedRole, final Map<String, String> requestDetails)
      throws SegueDatabaseException, ContentManagerException, MissingRequiredFieldException {
    String userSchool = getSchoolNameWithPostcode(user);
    if (userSchool == null) {
      throw new MissingRequiredFieldException(
          String.format("School information could not be found for user with ID: %s", user.getId()));
    }

    String verificationDetails = requestDetails.get("verificationDetails");
    if (verificationDetails == null || verificationDetails.isEmpty()) {
      throw new MissingRequiredFieldException("No verification details provided");
    }

    String otherDetails = requestDetails.get("otherDetails");
    String otherDetailsLine;
    if (otherDetails == null || otherDetails.isEmpty()) {
      otherDetailsLine = "";
    } else {
      otherDetailsLine = String.format("Any other information: %s\n<br>\n<br>", otherDetails);
    }

    String roleName = requestedRole.toString();
    String emailSubject = String.format("%s Account Request", StringUtils.capitalize(roleName.toLowerCase()));
    String emailMessage = String.format(
        "Hello,\n<br>\n<br>"
            + "Please could you convert my Isaac account into a teacher account.\n<br>\n<br>"
            + "My school is: %s\n<br>"
            + "A link to my school website with a staff list showing my name and email"
            + " (or a phone number to contact the school) is: %s\n<br>\n<br>\n<br>"
            + "%s"
            + "Thanks, \n<br>\n<br>%s %s",
        userSchool, verificationDetails, otherDetailsLine, user.getGivenName(), user.getFamilyName());
    Map<String, Object> emailValues = new ImmutableMap.Builder<String, Object>()
        .put("contactGivenName", user.getGivenName())
        .put("contactFamilyName", user.getFamilyName())
        .put("contactUserId", user.getId())
        .put("contactUserRole", user.getRole())
        .put("contactEmail", user.getEmail())
        .put("contactSubject", emailSubject)
        .put("contactMessage", emailMessage)
        .put("replyToName", String.format("%s %s", user.getGivenName(), user.getFamilyName()))
        .build();
    emailManager.sendContactUsFormEmail(properties.getProperty(Constants.MAIL_RECEIVERS), emailValues);

    logManager.logEvent(user, request, SegueServerLogType.CONTACT_US_FORM_USED,
        Map.of("message", String.format("%s %s (%s) - %s", user.getGivenName(),
            user.getFamilyName(), user.getEmail(), emailMessage)));
  }

  public String getSchoolNameWithPostcode(final RegisteredUserDTO user) {
    if (user.getSchoolId() != null && !user.getSchoolId().isEmpty()) {
      try {
        School school = schoolListReader.findSchoolById(user.getSchoolId());
        if (school != null) {
          return String.format("%s, %s", school.getName(), school.getPostcode());
        }
        log.error("Could not find school matching URN: {}", user.getSchoolId());
      } catch (UnableToIndexSchoolsException | IOException | SegueSearchException e) {
        log.error(String.format("Could not find school matching URN: %s", user.getSchoolId()));
      }
    }
    if (user.getSchoolOther() != null && !user.getSchoolOther().isEmpty()) {
      return user.getSchoolOther();
    }
    log.warn("User with ID: {} has no defined school information", user.getId());
    return null;
  }
}
