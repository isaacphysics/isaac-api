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

import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.eclipse.jetty.http.HttpCookie.SAME_SITE_LAX_COMMENT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DATE_EXPIRES;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_DATE_FORMAT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HMAC_SALT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.HOST_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.JSESSION_COOOKIE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGOUT_SESSION_ALREADY_INVALIDATED_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NO_SESSION_TOKEN_RESERVED_VALUE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.OAUTH_TOKEN_PARAM_NAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.PARTIAL_LOGIN_FLAG;
import static uk.ac.cam.cl.dtg.segue.api.Constants.PARTIAL_LOGIN_SESSION_EXPIRY_SECONDS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SEGUE_AUTH_COOKIE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_EXPIRY_SECONDS_DEFAULT;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_EXPIRY_SECONDS_FALLBACK;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_TOKEN;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SESSION_USER_ID;
import static uk.ac.cam.cl.dtg.segue.api.Constants.STATE_PARAM_NAME;
import static uk.ac.cam.cl.dtg.segue.dao.content.ContentMapperUtils.getSharedBasicObjectMapper;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.NewCookie;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.Validate;
import org.eclipse.jetty.websocket.api.UpgradeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserFromAuthProvider;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
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
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.users.IUserDataManager;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.RequestIpExtractor;

/**
 * This class handles all authentication details, including creation / destruction of sessions. It also handles adding
 * or removing authorised third party providers to Segue Accounts and dealing with password resets.
 */
public class UserAuthenticationManager {
  private static final Logger log = LoggerFactory.getLogger(UserAuthenticationManager.class);
  private static final String HMAC_SHA_ALGORITHM = "HmacSHA256";

  private final PropertiesLoader properties;
  private final IUserDataManager database;
  private final EmailManager emailManager;
  private final ObjectMapper serializationMapper;
  private final boolean checkOriginHeader;
  private final boolean setSecureCookies;

  private final Map<AuthenticationProvider, IAuthenticator> registeredAuthProviders;

  /**
   * Fully injectable constructor.
   *
   * @param database            - an IUserDataManager that will support persistence.
   * @param properties          - A property loader
   * @param providersToRegister - A map of known authentication providers.
   * @param emailQueue          - A communications queue for managing emails
   */
  @Inject
  public UserAuthenticationManager(final IUserDataManager database,
                                   final PropertiesLoader properties,
                                   final Map<AuthenticationProvider, IAuthenticator> providersToRegister,
                                   final EmailManager emailQueue) {
    requireNonNull(properties.getProperty(HMAC_SALT));
    requireNonNull(properties.getProperty(SESSION_EXPIRY_SECONDS_DEFAULT));
    requireNonNull(properties.getProperty(HOST_NAME));

    this.database = database;

    this.properties = properties;

    this.registeredAuthProviders = providersToRegister;

    this.emailManager = emailQueue;
    this.serializationMapper = getSharedBasicObjectMapper();
    boolean isProduction = properties.getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(EnvironmentType.PROD.name());
    this.checkOriginHeader = isProduction;
    this.setSecureCookies = isProduction;
  }

  /**
   * This method is used to extract user-identifying features of a request and return them in csv format.
   * NOTE: The validity of the session token is not checked against the database.
   *
   * @param request - http request from which to extract user identifying features.
   * @return A string of comma-separated user identifying values from the request.
   */
  public String getUserIdentifierCsv(final HttpServletRequest request) {
    String ipAddress = RequestIpExtractor.getClientIpAddr(request);

    String jsessionId = null;
    try {
      jsessionId = this.getJsessionIdFromRequest(request);
    } catch (InvalidSessionException e) { /* Do nothing - leave jsessionId as null */ }

    Map<String, String> sessionInformation = Maps.newHashMap();
    try {
      sessionInformation = getSegueSessionFromRequest(request);
    } catch (InvalidSessionException | IOException e) { /* Do nothing - leave session map empty */ }

    String segueUserId = sessionInformation.get(SESSION_USER_ID);
    String sessionToken = sessionInformation.get(SESSION_TOKEN);
    boolean isValidHmac = hasValidHmac(sessionInformation);

    return String.format("%s,%s,%s,%s,%b", ipAddress, jsessionId, segueUserId, sessionToken, isValidHmac);
  }

  /**
   * This method will trigger the authentication flow for a 3rd party authenticator.
   * <br>
   * This method can be used for regular logins, new registrations or for linking 3rd party authenticators to an
   * existing Segue user account.
   *
   * @param request  - http request that we can attach the session to and that already has a redirect url attached.
   * @param provider - the provider the user wishes to authenticate with.
   * @return A json response containing a URI to the authentication provider if authorization / login is required.
   *     Alternatively a SegueErrorResponse could be returned.
   * @throws IOException                            -
   * @throws AuthenticationProviderMappingException - as per exception description.
   */
  public URI getThirdPartyAuthURI(final HttpServletRequest request, final String provider)
      throws IOException, AuthenticationProviderMappingException {
    IAuthenticator federatedAuthenticator = mapToProvider(provider);

    // if we are an OAuthProvider redirect to the provider
    // authorisation URL.
    URI redirectLink;
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
   * Get the 3rd party authentication providers user object.
   * This can be used to look up existing segue users or create a new one.
   *
   * @param request  - to retrieve session params
   * @param provider - the provider we are interested in.
   * @return a user object with 3rd party data inside.
   * @throws AuthenticationProviderMappingException - if we cannot locate an appropriate authenticator.
   * @throws IOException                            - Problem reading something
   * @throws NoUserException                        - If the user doesn't exist with the provider.
   * @throws AuthenticatorSecurityException         - If there is a security probably with the authenticator.
   * @throws CrossSiteRequestForgeryException       - as per exception description.
   * @throws CodeExchangeException                  - as per exception description.
   * @throws AuthenticationCodeException            - as per exception description.
   */
  public UserFromAuthProvider getThirdPartyUserInformation(final HttpServletRequest request, final String provider)
      throws AuthenticationProviderMappingException, AuthenticatorSecurityException, NoUserException,
      IOException, AuthenticationCodeException, CodeExchangeException,
      CrossSiteRequestForgeryException {
    IAuthenticator authenticator = mapToProvider(provider);

    IOAuthAuthenticator oauthProvider;

    // this is a reference that the provider can use to look up user details.
    String providerSpecificUserLookupReference;

    // if we are an OAuth2Provider complete next steps of oauth
    if (authenticator instanceof IOAuthAuthenticator) {
      oauthProvider = (IOAuthAuthenticator) authenticator;

      providerSpecificUserLookupReference = this.getOauthInternalRefCode(oauthProvider, request);
    } else {
      throw new AuthenticationProviderMappingException("Unable to map to a known authenticator. The provider: "
          + provider + " is unknown");
    }

    UserFromAuthProvider userFromProvider = oauthProvider.getUserInfo(providerSpecificUserLookupReference);
    return userFromProvider;
  }

  /**
   * This method will attempt to find a segue user using a 3rd party provider and a unique id that identifies the user
   * to the provider.
   *
   * @param provider   - the provider that we originally validated with
   * @param providerId - the unique ID of the user as given to us from the provider.
   * @return A user object or null if we were unable to find the user with the information provided.
   * @throws SegueDatabaseException - If there is an internal database error.
   */
  public RegisteredUser getSegueUserFromLinkedAccount(final AuthenticationProvider provider, final String providerId)
      throws SegueDatabaseException {
    requireNonNull(provider);
    Validate.notBlank(providerId);

    RegisteredUser user = database.getByLinkedAccount(provider, providerId);
    if (null == user) {
      log.debug("Unable to locate user based on provider " + "information provided.");
    }
    return user;
  }

  /**
   * @param provider          - the provider the user wishes to authenticate with.
   * @param email             - the email the user wishes to use
   * @param plainTextPassword - the plain text password the user has provided
   * @return - a registered user object
   * @throws AuthenticationProviderMappingException - if we cannot find an authenticator
   * @throws SegueDatabaseException                 - if there is a problem with the database.
   * @throws IncorrectCredentialsProvidedException  - if the password is incorrect
   * @throws NoCredentialsAvailableException        - If the account exists but does not have a local password
   * @throws NoSuchAlgorithmException               - if the configured algorithm is not valid.
   * @throws InvalidKeySpecException                - if the preconfigured key spec is invalid.
   */
  public final RegisteredUser getSegueUserFromCredentials(final String provider, final String email,
                                                          final String plainTextPassword)
      throws AuthenticationProviderMappingException,
      SegueDatabaseException, IncorrectCredentialsProvidedException,
      NoCredentialsAvailableException, InvalidKeySpecException, NoSuchAlgorithmException {
    Validate.notBlank(email);
    requireNonNull(plainTextPassword);
    IAuthenticator authenticator = mapToProvider(provider);

    if (authenticator instanceof IPasswordAuthenticator) {
      IPasswordAuthenticator passwordAuthenticator = (IPasswordAuthenticator) authenticator;

      return passwordAuthenticator.authenticate(email, plainTextPassword);
    } else {
      throw new AuthenticationProviderMappingException("Unable to map to a known authenticator that accepts "
          + "raw credentials for the given provider: " + provider);
    }
  }

  /**
   * Checks to see if a user has valid way to authenticate with Segue.
   *
   * @param user - to check
   * @return true means the user should have a means of authenticating with their account as far as we are concerned
   */
  public boolean hasLocalCredentials(final RegisteredUser user) throws SegueDatabaseException {
    IPasswordAuthenticator passwordAuthenticator = (IPasswordAuthenticator) this.registeredAuthProviders
        .get(AuthenticationProvider.SEGUE);

    return passwordAuthenticator.hasPasswordRegistered(user);
  }

  /**
   * This method will look up a userDO based on the session information provided.
   *
   * @param request                           containing session information
   * @param allowIncompleteLoginsToReturnUser boolean if true will allow users that haven't completed MFA to be
   *                                              returned, false will be stricter and return null if user hasn't
   *                                              completed MFA.
   * @return either a user or null if we couldn't find the user for whatever reason.
   */
  public RegisteredUser getUserFromSession(final HttpServletRequest request,
                                           final boolean allowIncompleteLoginsToReturnUser) {
    // WARNING: There are two public getUserFromSession methods: ensure you check both!
    requireNonNull(request);

    Map<String, String> currentSessionInformation;
    try {
      currentSessionInformation = this.getSegueSessionFromRequest(request);
    } catch (IOException e1) {
      log.debug("Error parsing session information to retrieve user.");
      return null;
    } catch (InvalidSessionException e) {
      log.debug("We cannot read the session information. It probably doesn't exist");
      // assuming that no user is logged in.
      return null;
    }

    if (checkOriginHeader) {
      // Check if the request originated from Isaac, which should be unnecessary except for WebSockets given
      // correct CORS headers. This code will merely print warnings if something doesn't look right:
      String referrer = request.getHeader("Referer");  // Note HTTP Header misspelling!
      if (null == referrer) {
        log.warn("Authenticated request has no 'Referer' information set! Accessing: {}", request.getPathInfo());
      } else if (!referrer.startsWith("https://" + properties.getProperty(HOST_NAME) + "/")) {
        log.warn("Authenticated request has unexpected Referer: '{}'. Accessing: {}", referrer, request.getPathInfo());
      }
      // If the client sends an Origin header, we can check its value. If they do not send the header,
      // we can draw no conclusions.
      String origin = request.getHeader("Origin");
      if (null != origin && !origin.equals("https://" + properties.getProperty(HOST_NAME))) {
        log.warn("Authenticated request has unexpected Origin: '{}'. Accessing: {} {}", origin, request.getMethod(),
            request.getPathInfo());
      }
    }

    return getUserFromSessionInformationMap(currentSessionInformation, allowIncompleteLoginsToReturnUser);
  }

  /**
   * @param request - request to get the session and therefore user from
   * @return the current User
   * @see #getUserFromSession(HttpServletRequest, boolean) - the two types of "request" have identical methods but are
   *     not related by interfaces or inheritance and so require duplicated methods!
   */
  public RegisteredUser getUserFromSession(final UpgradeRequest request) {
    // WARNING: There are two public getUserFromSession methods: ensure you check both!
    requireNonNull(request);

    Map<String, String> currentSessionInformation;
    try {
      currentSessionInformation = this.getSegueSessionFromRequest(request);
    } catch (IOException e1) {
      log.error("Error parsing session information to retrieve user.");
      return null;
    } catch (InvalidSessionException e) {
      log.debug("We cannot read the session information. It probably doesn't exist");
      // assuming that no user is logged in.
      return null;
    }

    // WebSocket UpgradeRequests should never use a partial login:
    return getUserFromSessionInformationMap(currentSessionInformation, false);
  }

  /**
   * Extract the session expiry time from a request.
   * <br>
   * Does not check session validity.
   *
   * @param request The request to extract the session information from
   * @return The session expiry as an Instant
   */
  public Instant getSessionExpiry(final HttpServletRequest request) {
    try {
      Map<String, String> currentSessionInformation = getSegueSessionFromRequest(request);
      if (currentSessionInformation.containsKey(DATE_EXPIRES)) {
        DateTimeFormatter sessionDateFormat = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).withZone(UTC);
        return sessionDateFormat.parse(currentSessionInformation.get(DATE_EXPIRES), Instant::from);
      } else {
        return null;
      }
    } catch (InvalidSessionException | DateTimeParseException | IOException e) {
      log.debug("Error extracting session expiry from session information.", e);
      return null;
    }
  }


  /**
   * This method tries to address some of the duplication when extracting a user from a request.
   * <br>
   * Note: This method has an important security enforcing function. Users who haven't completed MFA will have a cookie
   * as per normal users but will have an additional status flag that indicates they haven't completed MFA.
   * This method will act upon that by refusing to return the user if the boolean parameter is set to false.
   *
   * @param currentSessionInformation         - the session information map extracted from the cookie.
   * @param allowIncompleteLoginsToReturnUser - boolean if true will allow users that haven't completed MFA to be
   *                                                returned, false will be stricter and return null if user hasn't
   *                                                completed MFA.
   * @return either the valid user from the cookie, or null if no valid user
   * @see #getUserFromSession(HttpServletRequest, boolean) - there are two types of "request" and they have identical
   *     methods
   * @see #getUserFromSession(UpgradeRequest) -     but unrelated by interfaces/inheritance, so require duplication!
   */
  private RegisteredUser getUserFromSessionInformationMap(final Map<String, String> currentSessionInformation,
                                                          final boolean allowIncompleteLoginsToReturnUser) {
    if (!allowIncompleteLoginsToReturnUser) {
      // check if the session has a caveat about incomplete MFA Login
      if (!Strings.isNullOrEmpty(currentSessionInformation.get(PARTIAL_LOGIN_FLAG))
          && Boolean.parseBoolean(currentSessionInformation.get(PARTIAL_LOGIN_FLAG))) {
        // login is incomplete we cannot proceed.
        log.debug("Incomplete MFA flow - no user object to be provided");
        return null;
      }
    }
    // Retrieve the user from database.
    try {
      // Check that the user's session is indeed valid:
      if (!isSessionValid(currentSessionInformation)) {
        log.debug("User session has failed validation. Treating as logged out. Session: {}", currentSessionInformation);
        return null;
      }

      // Get the user the cookie claims to belong to from the session information:
      long currentUserId = Long.parseLong(currentSessionInformation.get(SESSION_USER_ID));
      return database.getById(currentUserId);
    } catch (SegueDatabaseException e) {
      log.error("Internal Database error. Failed to resolve current user.", e);
      return null;
    } catch (NumberFormatException e) {
      log.info("Invalid user id detected in session. {}", currentSessionInformation.get(SESSION_USER_ID));
      return null;
    }
  }

  /**
   * Create a signed session based on the user DO provided and the http request and response.
   *
   * @param response - for creating the session
   * @param user     - the user who should be logged in.
   * @return the request and response will be modified and the original userDO will be returned for convenience.
   */
  public RegisteredUser createUserSession(final HttpServletResponse response, final RegisteredUser user)
      throws SegueDatabaseException {
    this.createSession(response, user, false);
    return user;
  }

  /**
   * Create a signed session based on the user DO provided and the http request and response.
   *
   * @param response - for creating the session
   * @param user     - the user who should be logged in.
   * @return the request and response will be modified and the original userDO will be returned for convenience.
   */
  public RegisteredUser createIncompleteLoginUserSession(final HttpServletResponse response, final RegisteredUser user)
      throws SegueDatabaseException {
    this.createSession(response, user, true);
    return user;
  }

  /**
   * Destroy a session attached to the request.
   *
   * @param request  containing the tomcat session to destroy
   * @param response to destroy the segue cookie.
   * @throws NoUserLoggedInException - if a user cannot be retrieved from the session information
   * @throws SegueDatabaseException  - if accessing the database fails
   */
  public void destroyUserSession(final HttpServletRequest request, final HttpServletResponse response)
      throws NoUserLoggedInException, SegueDatabaseException {
    requireNonNull(request);
    try {
      request.getSession().invalidate();
      invalidateSessionToken(request);
      Cookie logoutCookie = createAuthLogoutCookie();

      response.addCookie(logoutCookie);
    } catch (IllegalStateException e) {
      log.info(LOGOUT_SESSION_ALREADY_INVALIDATED_MESSAGE, e);
    }
  }

  /**
   * Takes a request holding an authentication cookie and invalidates the associated session token stored in the
   * database.
   *
   * @param request - a servlet request holding an auth cookie for the user session to be invalidated
   * @throws NoUserLoggedInException - if a user cannot be retrieved from the session information
   * @throws SegueDatabaseException  - if accessing the database fails
   */
  public void invalidateSessionToken(final HttpServletRequest request)
      throws NoUserLoggedInException, SegueDatabaseException {
    requireNonNull(request);
    RegisteredUser currentUser = this.getUserFromSession(request, false);
    if (null == currentUser) {
      throw new NoUserLoggedInException();
    }
    // By nullifying the token in the database, all previously existing authentication cookies and their
    // associated sessions will be invalidated as their token value will no longer match the database value.
    // A new session token will need to generated and assigned when reauthenticating the user.
    this.database.invalidateSessionToken(currentUser);
  }

  /**
   * Attempts to map a string to a known provider.
   *
   * @param provider - String representation of the provider requested
   * @return the FederatedAuthenticator object which can be used to get a user.
   * @throws AuthenticationProviderMappingException if we are unable to locate the provider requested.
   */
  public IAuthenticator mapToProvider(final String provider) throws AuthenticationProviderMappingException {
    Validate.notEmpty(provider, "Provider name must not be empty or null if we are going "
        + "to map it to an implementation.");

    AuthenticationProvider enumProvider;
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

    log.debug("Mapping provider: " + sanitiseExternalLogValue(provider) + " to " + enumProvider);

    return this.registeredAuthProviders.get(enumProvider);
  }

  /**
   * Link Provider To Existing Account.
   *
   * @param currentUser            - the current user to link provider to.
   * @param federatedAuthenticator the federatedAuthenticator we are using for authentication
   * @param providerUserObject     - the user object provided by the 3rd party authenticator.
   * @throws SegueDatabaseException - If there is an internal database error.
   */
  public void linkProviderToExistingAccount(final RegisteredUser currentUser,
                                            final AuthenticationProvider federatedAuthenticator,
                                            final UserFromAuthProvider providerUserObject)
      throws SegueDatabaseException {
    requireNonNull(currentUser);
    requireNonNull(federatedAuthenticator);
    requireNonNull(providerUserObject);

    this.database.linkAuthProviderToAccount(currentUser, federatedAuthenticator,
        providerUserObject.getProviderUserId());
  }

  /**
   * Unlink User From AuthenticationProvider
   * <br>
   * Removes the link between a user and a provider.
   *
   * @param userDO         - user to affect.
   * @param providerString - provider to unassociated.
   * @throws SegueDatabaseException                 - if there is an error during the database update.
   * @throws MissingRequiredFieldException          - If the change will mean that the user will be unable to login
   *                                                      again.
   * @throws AuthenticationProviderMappingException - if we are unable to locate the authentication provider specified.
   */
  public void unlinkUserAndProvider(final RegisteredUser userDO, final String providerString)
      throws SegueDatabaseException, MissingRequiredFieldException, AuthenticationProviderMappingException {

    // check if the provider is there to delete in the first place. If not just return.
    if (!this.database.getAuthenticationProvidersByUser(userDO).contains(
        this.mapToProvider(providerString).getAuthenticationProvider())) {
      return;
    }

    IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this
        .mapToProvider(AuthenticationProvider.SEGUE.name());

    // make sure that the change doesn't prevent the user from logging in again.
    if (this.database.getAuthenticationProvidersByUser(userDO).size() > 1 || authenticator.hasPasswordRegistered(
        userDO)) {
      this.database.unlinkAuthProviderFromUser(userDO, this.mapToProvider(providerString)
          .getAuthenticationProvider());
    } else {
      throw new MissingRequiredFieldException("This modification would mean that you"
          + " no longer have a way to log in and has been ignored.");
    }
  }


  /**
   * This method will use an email address to check a local user exists and if so, will send an email with a unique
   * token to allow a password reset. This method does not indicate whether or not the email actually existed.
   *
   * @param userDO    - A user object containing the email address of the user to reset the password for.
   * @param userAsDTO - A user DTO object sanitised so that we can send it to the email manager.
   * @throws NoSuchAlgorithmException - if the configured algorithm is not valid.
   * @throws InvalidKeySpecException  - if the preconfigured key spec is invalid.
   * @throws CommunicationException   - if a fault occurred whilst sending the communique
   * @throws SegueDatabaseException   - If there is an internal database error.
   */
  public final void resetPasswordRequest(final RegisteredUser userDO, final RegisteredUserDTO userAsDTO)
      throws InvalidKeySpecException,
      NoSuchAlgorithmException, CommunicationException, SegueDatabaseException {
    try {
      IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
          .get(AuthenticationProvider.SEGUE);

      // Before we create a reset token, record if they already have a password:
      boolean userHadPasswordRegistered = authenticator.hasPasswordRegistered(userDO);

      // Generate reset token, whether or not they have a local password set up:
      String token = authenticator.createPasswordResetTokenForUser(userDO);
      log.info("Sending password reset message to {}", userDO.getEmail());

      Map<String, Object> emailValues = ImmutableMap.of("resetURL",
          String.format("https://%s/resetpassword/%s",
              properties.getProperty(HOST_NAME), token));

      if (this.database.hasALinkedAccount(userDO) && !userHadPasswordRegistered) {
        // If user wasn't previously authenticated locally, and has a linked account
        // allow them to reset their password but tell them about their provider(s):
        this.sendFederatedAuthenticatorResetMessage(userDO, userAsDTO, emailValues);
      } else {
        this.emailManager.sendTemplatedEmailToUser(userAsDTO,
            emailManager.getEmailTemplateDTO("email-template-password-reset"),
            emailValues, EmailType.SYSTEM);
      }

    } catch (ContentManagerException e) {
      log.error("ContentManagerException", e);
    }
  }

  /**
   * This method will use a unique password reset token to set a new password.
   *
   * @param token       - the password reset token
   * @param newPassword - New password to set in plain text
   * @return the user which has had the password reset.
   * @throws InvalidTokenException    - If the token provided is invalid.
   * @throws InvalidPasswordException - If the password provided is invalid.
   * @throws SegueDatabaseException   - If there is an internal database error.
   */
  public RegisteredUser resetPassword(final String token, final String newPassword)
      throws InvalidTokenException, InvalidPasswordException, SegueDatabaseException, InvalidKeySpecException,
      NoSuchAlgorithmException {

    IPasswordAuthenticator authenticator = (IPasswordAuthenticator) this.registeredAuthProviders
        .get(AuthenticationProvider.SEGUE);

    // Ensure new password is valid
    authenticator.ensureValidPassword(newPassword);

    // Ensure reset token is valid
    RegisteredUser user = authenticator.getRegisteredUserByToken(token);
    if (null == user) {
      throw new InvalidTokenException();
    }

    // Set user's password
    authenticator.setOrChangeUsersPassword(user, newPassword);
    return user;
  }

  /**
   * This method will send a message to a user explaining that they only use a federated authenticator.
   *
   * @param user                  - a user with the givenName, email and token fields set
   * @param userAsDTO             - A user DTO object sanitised so that we can send it to the email manager.
   * @param additionalEmailValues - Additional email values to find and replace including any password reset urls.
   * @throws SegueDatabaseException - If there is an internal database error.
   */
  private void sendFederatedAuthenticatorResetMessage(final RegisteredUser user, final RegisteredUserDTO userAsDTO,
                                                      final Map<String, Object> additionalEmailValues)
      throws
      SegueDatabaseException {
    requireNonNull(user);

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
      Map<String, Object> emailTokens = Maps.newHashMap();
      emailTokens.putAll(ImmutableMap.of("providerString", providersString,
          "providerWord", providerWord));
      emailTokens.putAll(additionalEmailValues);

      emailManager.sendTemplatedEmailToUser(userAsDTO,
          emailManager.getEmailTemplateDTO("email-template-federated-password-reset"),
          emailTokens, EmailType.SYSTEM);

    } catch (ContentManagerException contentException) {
      log.error(String.format("Error sending federated email verification message - %s",
          contentException.getMessage()));
    }
  }

  /**
   * This method is an oauth2 specific method which will ultimately provide an internal reference number that the
   * oauth2 provider can use to lookup the information of the user who has just authenticated.
   *
   * @param oauthProvider - The provider to authenticate against.
   * @param request       - The request that will contain session information.
   * @return an internal reference number that will allow retrieval of the users information from the provider.
   * @throws AuthenticationCodeException      - possible authentication code issues.
   * @throws CodeExchangeException            - exception whilst exchanging codes
   * @throws CrossSiteRequestForgeryException - Unable to guarantee no CSRF
   */
  private String getOauthInternalRefCode(final IOAuthAuthenticator oauthProvider, final HttpServletRequest request)
      throws AuthenticationCodeException, CodeExchangeException,
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
   * Verify with the request that there is no CSRF violation.
   *
   * @param request       - http request to verify there is no CSRF
   * @param oauthProvider -
   * @return true if we are happy , false if we think a violation has occurred.
   * @throws CrossSiteRequestForgeryException - if we suspect cross site request forgery.
   */
  private boolean ensureNoCSRF(final HttpServletRequest request, final IOAuthAuthenticator oauthProvider)
      throws CrossSiteRequestForgeryException {
    requireNonNull(request);

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

    if (null == csrfTokenFromUser || !csrfTokenFromUser.equals(csrfTokenFromProvider)) {
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
   * Create a session and attach it to the request provided.
   *
   * @param response         to store the session in our own segue cookie.
   * @param user             account to associate the session with.
   * @param partialLoginFlag Boolean to indicate whether or not this cookie represents a partial login (true) or full
   *                             (false)
   */
  private void createSession(
      final HttpServletResponse response,
      final RegisteredUser user,
      final boolean partialLoginFlag
  ) throws SegueDatabaseException {
    requireNonNull(response);
    requireNonNull(user);
    requireNonNull(user.getId());
    int sessionExpiryTimeInSeconds =
        properties.getIntegerPropertyOrFallback(SESSION_EXPIRY_SECONDS_DEFAULT, SESSION_EXPIRY_SECONDS_FALLBACK);

    if (partialLoginFlag) {
      // use shortened expiry time if partial login
      createSession(response, user, PARTIAL_LOGIN_SESSION_EXPIRY_SECONDS, String.valueOf(true));
    } else {
      createSession(response, user, sessionExpiryTimeInSeconds, null);
    }
  }

  /**
   * Create a session with a specified expiry time and attach it to the request provided.
   *
   * @param response                   to store the session in our own segue cookie.
   * @param user                       account to associate the session with.
   * @param sessionExpiryTimeInSeconds max age of the cookie.
   * @param partialLoginFlagString     either null if this is a full login cookie or a string value of true if this is
   *                                       a partial login cookie
   */
  private void createSession(final HttpServletResponse response, final RegisteredUser user,
                             final int sessionExpiryTimeInSeconds,
                             @Nullable final String partialLoginFlagString) throws SegueDatabaseException {
    DateTimeFormatter sessionDateFormat = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).withZone(UTC);
    String newUserSessionToken = this.database.regenerateSessionToken(user).toString();
    String userId = user.getId().toString();
    String hmacKey = properties.getProperty(HMAC_SALT);

    try {
      String sessionExpiryDate = sessionDateFormat.format(Instant.now().plusSeconds(sessionExpiryTimeInSeconds));

      Map<String, String> sessionInformation =
          prepareSessionInformation(userId, newUserSessionToken, sessionExpiryDate, hmacKey, partialLoginFlagString);

      Cookie authCookie = createAuthCookie(sessionInformation, sessionExpiryTimeInSeconds);

      log.debug("Creating AuthCookie for user ({}) with value {}", userId, authCookie.getValue());

      response.addCookie(authCookie);  // lgtm [java/insecure-cookie]  false positive due to conditional above!

    } catch (JsonProcessingException e1) {
      log.error("Unable to save cookie.", e1);
    }
  }

  /**
   * Executes checks on the users sessions to ensure it is valid.
   * <br>
   * Verifies the HMAC for userId, expiry date, session token and partial login status; but DOES NOT enforce
   * partial login as invalid! I.e. this method will return true for partial logins.
   *
   * @param sessionInformation map containing session information retrieved from the cookie
   * @param sessionTokenFromDatabase the real session token to validate this cookie against
   * @return true if it is still valid, false if not.
   */
  public boolean isValidUsersSession(final Map<String, String> sessionInformation,
                                      final Integer sessionTokenFromDatabase) {
    requireNonNull(sessionInformation);
    requireNonNull(sessionTokenFromDatabase);

    DateTimeFormatter sessionDateFormat = DateTimeFormatter.ofPattern(DEFAULT_DATE_FORMAT).withZone(UTC);

    String userId = sessionInformation.get(SESSION_USER_ID);
    String userSessionToken = sessionInformation.get(SESSION_TOKEN);
    String sessionDate = sessionInformation.get(DATE_EXPIRES);

    // Check that there is a user ID provided:
    if (null == userId) {
      log.debug("No user ID provided by cookie, cannot be a valid session.");
      return false;
    }

    // Check the expiry time has not passed:
    if (null == sessionDate) {
      log.debug("No session date provided by cookie, invalid!");
      return false;
    }
    try {
      if (Instant.now().isAfter(sessionDateFormat.parse(sessionDate, Instant::from))) {
        log.debug("Session expired");
        return false;
      }
    } catch (DateTimeParseException e) {
      return false;
    }

    // Check no one has tampered with the cookie:
    if (!hasValidHmac(sessionInformation)) {
      log.warn("Invalid Cookie HMAC detected for user id ({})!", userId);
      return false;
    }

    // Check that the session token is still valid:
    if (sessionTokenFromDatabase == NO_SESSION_TOKEN_RESERVED_VALUE
        || !sessionTokenFromDatabase.toString().equals(userSessionToken)) {
      log.debug("Invalid session token detected for user id {}", userId);
      return false;
    }

    return true;
  }

  /**
   * Calculate the session HMAC value based on the properties of interest.
   *
   * @param key              - secret key.
   * @param userId           - User Id
   * @param currentDate      - Current date
   * @param sessionToken     - a token allowing session invalidation
   * @param partialLoginFlag - Boolean data to encode in the cookie - true if a partial login
   * @return HMAC signature.
   */
  public String calculateSessionHMAC(final String key, final String userId, final String currentDate,
                                      final String sessionToken,
                                      @Nullable final String partialLoginFlag) {
    StringBuilder sb = new StringBuilder();
    sb.append(userId);
    sb.append("|").append(currentDate);
    sb.append("|").append(sessionToken);

    if (partialLoginFlag != null) {
      sb.append("|").append(partialLoginFlag);
    }

    return UserAuthenticationManager.calculateHMAC(key, sb.toString());
  }

  /**
   * This method is used to check whether a Segue Session's reported HMAC matches our recalculation. Assuming we've
   * kept our HMAC_SALT secret and non-guessable, that will mean the session information has not been tampered with.
   * <br>
   * NOTE: Even if the HMAC is correct, it does not mean the session is valid, for that, use #isValidUsersSession(...).
   *
   * @param sessionInformation Map of keys and values representing the session.
   * @return Whether or not the reported HMAC matches our computation.
   */
  private boolean hasValidHmac(final Map<String, String> sessionInformation) {
    String hmacKey = properties.getProperty(HMAC_SALT);
    String supposedUserId = sessionInformation.get(SESSION_USER_ID);
    String userSessionToken = sessionInformation.get(SESSION_TOKEN);
    String sessionDate = sessionInformation.get(DATE_EXPIRES);
    String partialLoginFlag = sessionInformation.get(PARTIAL_LOGIN_FLAG);
    String sessionHMAC = sessionInformation.get(HMAC);

    String ourHMAC = calculateSessionHMAC(hmacKey, supposedUserId, sessionDate, userSessionToken, partialLoginFlag);
    return ourHMAC.equals(sessionHMAC);
  }

  private String getJsessionIdFromRequest(final HttpServletRequest request) throws InvalidSessionException {
    Cookie jsessionCookie = null;
    if (request.getCookies() == null) {
      throw new InvalidSessionException("There are no cookies set.");
    }

    for (Cookie c : request.getCookies()) {
      if (c.getName().equals(JSESSION_COOOKIE)) {
        jsessionCookie = c;
      }
    }

    if (null == jsessionCookie) {
      throw new InvalidSessionException("There are no cookies set.");
    }

    return jsessionCookie.getValue();
  }

  /**
   * This method will extract the segue session information from a given request.
   *
   * @param request - possibly containing a segue cookie.
   * @return The segue session information (unchecked or validated)
   * @throws IOException             - problem parsing session information.
   * @throws InvalidSessionException - if there is no session set or if it is not valid.
   */
  private Map<String, String> getSegueSessionFromRequest(final HttpServletRequest request) throws IOException,
      InvalidSessionException {
    // WARNING: There are two getSegueSessionFromRequest methods: ensure you update both!
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
    Map<String, String> sessionInformation =
        this.serializationMapper.readValue(Base64.decodeBase64(segueAuthCookie.getValue()),
            HashMap.class);

    return sessionInformation;
  }

  /**
   * @param request - request to get the session cookie from
   * @return a Map of session information
   * @see #getSegueSessionFromRequest(HttpServletRequest) - except for some reason a WebSocket UpgradeRrequest is not
   *     an HttpServletRequest. Worse, the cookies from an HttpServletRequest are Cookie objects, but those from the
   *     WebSocket UpgradeRequest are HttpCookies!
   */
  private Map<String, String> getSegueSessionFromRequest(final UpgradeRequest request) throws IOException,
      InvalidSessionException {
    // WARNING: There are two getSegueSessionFromRequest methods: ensure you update both!
    HttpCookie segueAuthCookie = null;
    if (request.getCookies() == null) {
      throw new InvalidSessionException("There are no cookies set.");
    }

    for (HttpCookie c : request.getCookies()) {
      if (c.getName().equals(SEGUE_AUTH_COOKIE)) {
        segueAuthCookie = c;
      }
    }

    if (null == segueAuthCookie) {
      throw new InvalidSessionException("There are no cookies set.");
    }

    @SuppressWarnings("unchecked")
    Map<String, String> sessionInformation =
        this.serializationMapper.readValue(Base64.decodeBase64(segueAuthCookie.getValue()),
            HashMap.class);

    return sessionInformation;
  }

  /**
   * Generate an HMAC using a key and the data to sign.
   *
   * @param key        - HMAC key for signing
   * @param dataToSign - data to be signed
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
      log.warn("Unexpected error while creating hash", e);
      throw new IllegalArgumentException();
    }
  }

  public Map<String, String> prepareSessionInformation(String userId, String newUserSessionToken,
                                                       String sessionExpiryDate, String hmacKey,
                                                       String partialLoginFlagString) {
    ImmutableMap.Builder<String, String> sessionInformationBuilder = new ImmutableMap.Builder<>();
    sessionInformationBuilder.put(SESSION_USER_ID, userId);
    sessionInformationBuilder.put(SESSION_TOKEN, newUserSessionToken);
    sessionInformationBuilder.put(DATE_EXPIRES, sessionExpiryDate);

    if (partialLoginFlagString != null) {
      sessionInformationBuilder.put(PARTIAL_LOGIN_FLAG, partialLoginFlagString);
    }

    String sessionHmac =
        calculateSessionHMAC(hmacKey, userId, sessionExpiryDate, newUserSessionToken, partialLoginFlagString);
    sessionInformationBuilder.put(HMAC, sessionHmac);

    return sessionInformationBuilder.build();
  }

  public boolean isSessionValid(final HttpServletRequest request) {
    Map<String, String> currentSessionInformation;
    try {
      currentSessionInformation = this.getSegueSessionFromRequest(request);
    } catch (InvalidSessionException | IOException e) {
      log.warn("User session has failed validation. Could not parse session information.");
      return false;
    }
    return isSessionValid(currentSessionInformation);
  }

  public boolean isSessionValid(final Map<String, String> currentSessionInformation) {
    try {
      long currentUserId = Long.parseLong(currentSessionInformation.get(SESSION_USER_ID));
      Integer databaseSessionToken = database.getSessionToken(currentUserId);
      if (null == databaseSessionToken || !this.isValidUsersSession(currentSessionInformation, databaseSessionToken)) {
        log.warn("User session has failed validation. Validation checks did not pass.");
        return false;
      }
      return true;
    } catch (NumberFormatException e) {
      log.warn("User session has failed validation. Could not parse session information.");
      return false;
    } catch (SegueDatabaseException e) {
      log.warn("User session has failed validation. Error accessing database.");
      return false;
    }
  }

  public Map<String, String> decodeCookie(final jakarta.ws.rs.core.Cookie segueAuthCookie) throws IOException {
    return this.serializationMapper.readValue(Base64.decodeBase64(segueAuthCookie.getValue()), HashMap.class);
  }

  public Map<String, String> decodeCookie(final Cookie segueAuthCookie) throws IOException {
    return this.serializationMapper.readValue(Base64.decodeBase64(segueAuthCookie.getValue()), HashMap.class);
  }

  public String calculateUpdatedHMAC(final Map<String, String> sessionInformation) {
    String hmacKey = properties.getProperty(HMAC_SALT);
    String userId = sessionInformation.get(SESSION_USER_ID);
    String sessionExpiryDate = sessionInformation.get(DATE_EXPIRES);
    String userSessionToken = sessionInformation.get(SESSION_TOKEN);
    String partialLoginFlagString = sessionInformation.get(PARTIAL_LOGIN_FLAG);

    return calculateSessionHMAC(hmacKey, userId, sessionExpiryDate, userSessionToken, partialLoginFlagString);
  }

  public Cookie createAuthCookie(final Map<String, String> sessionInformation, final int sessionExpiryTimeInSeconds)
      throws JsonProcessingException {
    Cookie authCookie = new Cookie(SEGUE_AUTH_COOKIE,
        Base64.encodeBase64String(serializationMapper.writeValueAsString(sessionInformation).getBytes()));
    authCookie.setMaxAge(sessionExpiryTimeInSeconds);
    authCookie.setPath("/");
    authCookie.setHttpOnly(true);
    authCookie.setSecure(setSecureCookies);
    authCookie.setComment(SAME_SITE_LAX_COMMENT);
    return authCookie;
  }

  // The logout cookies should have matching properties despite the difference in type
  public Cookie createAuthLogoutCookie() {
    Cookie logoutCookie = new Cookie(SEGUE_AUTH_COOKIE, "");
    logoutCookie.setPath("/");
    logoutCookie.setMaxAge(0);  // This will lead to it being removed by the browser immediately.
    logoutCookie.setHttpOnly(true);
    logoutCookie.setSecure(setSecureCookies);
    logoutCookie.setComment(SAME_SITE_LAX_COMMENT);
    return logoutCookie;
  }

  public NewCookie createAuthLogoutNewCookie() {
    NewCookie logoutCookie = new NewCookie.Builder(SEGUE_AUTH_COOKIE)
        .value("")
        .path("/")
        .maxAge(0)
        .httpOnly(true)
        .secure(setSecureCookies)
        .comment(SAME_SITE_LAX_COMMENT)
        .build();
    return logoutCookie;
  }
}
