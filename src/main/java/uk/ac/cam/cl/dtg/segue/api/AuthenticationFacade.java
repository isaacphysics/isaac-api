/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.segue.api;

import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_DATABASE_ERROR_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_INCORRECT_CREDENTIALS_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_MISSING_CREDENTIALS_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_RATE_THROTTLE_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGIN_UNKNOWN_PROVIDER_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGOUT_DATABASE_ERROR_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.LOGOUT_NO_ACTIVE_SESSION_MESSAGE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.REDIRECT_URL;
import static uk.ac.cam.cl.dtg.segue.api.Constants.SegueServerLogType;
import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseExternalLogValue;

import com.google.api.client.util.Maps;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dto.LocalAuthDTO;
import uk.ac.cam.cl.dtg.isaac.dto.MFAResponseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryWithEmailAddressDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AccountAlreadyLinkedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AdditionalAuthenticationRequiredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationCodeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticatorSecurityException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CodeExchangeException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.CrossSiteRequestForgeryException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MFARequiredButNotConfiguredException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.RequestIpExtractor;

/**
 * AuthenticationFacade.
 *
 * @author Stephen Cummins
 */
@Path("/auth")
@Tag(name = "/auth")
public class AuthenticationFacade extends AbstractSegueFacade {
  private static final Logger log = LoggerFactory.getLogger(AuthenticationFacade.class);

  private final UserAccountManager userManager;

  private final IMisuseMonitor misuseMonitor;

  /**
   * Create an instance of the authentication Facade.
   *
   * @param properties    - properties loader for the application
   * @param userManager   - user manager for the application
   * @param logManager    - so we can log interesting events.
   * @param misuseMonitor - so that we can prevent overuse of protected resources.
   */
  @Inject
  public AuthenticationFacade(final PropertiesLoader properties, final UserAccountManager userManager,
                              final ILogManager logManager, final IMisuseMonitor misuseMonitor) {
    super(properties, logManager);
    this.userManager = userManager;
    this.misuseMonitor = misuseMonitor;
  }

  /**
   * Get authentication provider information for the current user.
   *
   * @param request - the http request containing session information of the user currently logged in
   * @return a user authentication settings object showing information about how a given user can authenticate
   */
  @GET
  @Path("/user_authentication_settings")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "The current users authentication settings, e.g. linked accounts and whether they have segue or"
      + " not")
  public final Response getCurrentUserAuthorisationSettings(@Context final HttpServletRequest request) {

    return this.getCurrentUserAuthorisationSettings(request, null);
  }

  /**
   * Get authentication provider information for the specified user.
   *
   * @param request - the http request containing session information of the user currently logged in
   * @param userId  - the id of the user of interest, null assumes current user
   * @return a user authentication settings object showing information about how a given user can authenticate
   */
  @GET
  @Path("/user_authentication_settings/{user_id}")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "The current users authentication settings, e.g. linked accounts and whether they have segue or"
      + " not")
  public final Response getCurrentUserAuthorisationSettings(@Context final HttpServletRequest request,
                                                            @PathParam("user_id") final Long userId) {
    try {
      RegisteredUserDTO currentRegisteredUser = this.userManager.getCurrentRegisteredUser(request);
      return getUserAuthenticationSettings(Objects.requireNonNullElse(userId, currentRegisteredUser.getId()),
          currentRegisteredUser);
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (SegueDatabaseException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Unable to load authentication settings due to a problem with the database.", e).toResponse();
    } catch (NoUserException e) {
      return new SegueErrorResponse(Status.NOT_FOUND, "User ID specified could not be found").toResponse();
    }
  }

  private Response getUserAuthenticationSettings(final Long userId, final RegisteredUserDTO currentRegisteredUser)
      throws NoUserLoggedInException, NoUserException, SegueDatabaseException {
    if (!userId.equals(currentRegisteredUser.getId()) && !isUserAnAdmin(userManager, currentRegisteredUser)) {
      return new SegueErrorResponse(Status.FORBIDDEN,
          "You must be an admin member to view this setting for another user.")
          .toResponse();
    }

    RegisteredUserDTO userToLookup;
    if (currentRegisteredUser.getId().equals(userId)) {
      userToLookup = currentRegisteredUser;
    } else {
      userToLookup = this.userManager.getUserDTOById(userId);
    }

    return Response.ok(this.userManager.getUsersAuthenticationSettings(userToLookup))
        .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
  }

  /**
   * This is the initial step of the authentication process.
   *
   * @param request        - the http request of the user wishing to authenticate
   * @param signinProvider - string representing the supported auth provider so that we know who to redirect the user to
   * @return Redirect response to the auth providers site.
   */
  @GET
  @Path("/{provider}/authenticate")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get the SSO login redirect URL for an authentication provider.")
  public final Response authenticate(@Context final HttpServletRequest request,
                                     @PathParam("provider") final String signinProvider) {

    if (userManager.isRegisteredUserLoggedIn(request)) {
      // if they are already logged in then we do not want to proceed with
      // this authentication flow. We can just return an error response
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "The user is already logged in. You cannot authenticate again.").toResponse();
    }

    try {
      Map<String, URI> redirectResponse = new ImmutableMap.Builder<String, URI>()
          .put(REDIRECT_URL, userManager.authenticate(request, signinProvider)).build();

      return Response.ok(redirectResponse).build();
    } catch (IOException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "IOException when trying to redirect to OAuth provider", e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    } catch (AuthenticationProviderMappingException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
          "Error mapping to a known authenticator. The provider: " + sanitiseExternalLogValue(signinProvider)
              + " is unknown");
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    }
  }

  /**
   * Link existing user to provider.
   *
   * @param request              - the http request of the user wishing to authenticate
   * @param authProviderAsString - string representing the supported auth provider so that we know who to redirect the
   *                                   user to.
   * @return a redirect to where the client asked to be redirected to.
   */
  @GET
  @Path("/{provider}/link")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get the SSO redirect URL for an authentication provider.",
      description = "Very similar to the login case, but records this is a link request not an account creation"
          + " request.")
  public final Response linkExistingUserToProvider(@Context final HttpServletRequest request,
                                                   @PathParam("provider") final String authProviderAsString) {
    if (!this.userManager.isRegisteredUserLoggedIn(request)) {
      return SegueErrorResponse.getNotLoggedInResponse();
    }

    try {
      Map<String, URI> redirectResponse = new ImmutableMap.Builder<String, URI>()
          .put(REDIRECT_URL, this.userManager.initiateLinkAccountToUserFlow(request, authProviderAsString))
          .build();

      return Response.ok(redirectResponse).build();
    } catch (IOException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "IOException when trying to redirect to OAuth provider", e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    } catch (AuthenticationProviderMappingException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST,
          "Error mapping to a known authenticator. The provider: " + sanitiseExternalLogValue(authProviderAsString)
              + " is unknown");
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    }

  }

  /**
   * End point that allows the user to remove a third party auth provider.
   *
   * @param request              - request so we can authenticate the user.
   * @param authProviderAsString - the provider to dis-associate.
   * @return successful response.
   */
  @DELETE
  @Path("/{provider}/link")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Remove an SSO provider from the current user's account.")
  public final Response unlinkUserFromProvider(@Context final HttpServletRequest request,
                                               @PathParam("provider") final String authProviderAsString) {
    try {
      RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);
      this.userManager.unlinkUserFromProvider(user, authProviderAsString);
    } catch (SegueDatabaseException e) {
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Unable to remove account due to a problem with the database.", e).toResponse();
    } catch (MissingRequiredFieldException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "Unable to remove account as this will mean that the user cannot login again in the future.", e)
          .toResponse();
    } catch (NoUserLoggedInException e) {
      return SegueErrorResponse.getNotLoggedInResponse();
    } catch (AuthenticationProviderMappingException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to map to a known authenticator. The provider: "
          + authProviderAsString + " is unknown").toResponse();
    }

    return Response.status(Status.NO_CONTENT).build();
  }

  /**
   * This is the callback url that auth providers should use to send us information about users.
   *
   * @param request        - http request from user
   * @param response       to tell the browser to store the session in our own segue cookie if successful.
   * @param signinProvider - requested signing provider string
   * @return Redirect response to send the user to the home page.
   */
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("/{provider}/callback")
  @Operation(summary = "SSO callback URL for a given provider.")
  public final Response authenticationCallback(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               @PathParam("provider") final String signinProvider) {

    try {
      RegisteredUserDTO userToReturn = userManager.authenticateCallback(request, response, signinProvider);
      this.getLogManager().logEvent(userToReturn, request, SegueServerLogType.LOG_IN, Maps.newHashMap());
      return Response.ok(userToReturn).build();
    } catch (IOException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Exception while trying to authenticate a user" + " - during callback step.", e);
      log.error(error.getErrorMessage(), e);
      return error.toResponse();
    } catch (NoUserException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.UNAUTHORIZED,
          "Unable to locate user information.");
      log.error("No userID exception received. Unable to locate user.", e);
      return error.toResponse();
    } catch (AuthenticationCodeException | CrossSiteRequestForgeryException | AuthenticatorSecurityException
             | CodeExchangeException e) {
      SegueErrorResponse error = new SegueErrorResponse(Status.UNAUTHORIZED, e.getMessage());
      log.info("Error detected during authentication: " + e.getClass().toString());
      return error.toResponse();
    } catch (DuplicateAccountException e) {
      log.debug("Duplicate user already exists in the database.", e);
      return new SegueErrorResponse(Status.FORBIDDEN, e.getMessage()).toResponse();
    } catch (AccountAlreadyLinkedException e) {
      log.error("Internal Database error during authentication", e);
      return new SegueErrorResponse(Status.BAD_REQUEST,
          "The account you are trying to link is already attached to a user of this system.").toResponse();
    } catch (SegueDatabaseException e) {
      log.error("Internal Database error during authentication", e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
          "Internal database error during authentication.").toResponse();
    } catch (AuthenticationProviderMappingException e) {
      return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to map to a known authenticator. The provider: "
          + signinProvider + " is unknown").toResponse();
    }

  }

  /**
   * This is the initial step of the authentication process for users who have a local account.
   *
   * @param request        - the http request of the user wishing to authenticate
   * @param response       to tell the browser to store the session in our own segue cookie if successful.
   * @param signinProvider - string representing the supported auth provider so that we know who to redirect the user to
   * @param localAuthDTO   - for local authentication only, credentials should be specified within a LocalAuthDTO object
   *                       e.g. email and password.
   * @return The users DTO or a SegueErrorResponse
   */
  @POST
  @Path("/{provider}/authenticate")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Initiate login with an email address and password.")
  public final Response authenticateWithCredentials(@Context final HttpServletRequest request,
                                                    @Context final HttpServletResponse response,
                                                    @PathParam("provider") final String signinProvider,
                                                    final LocalAuthDTO localAuthDTO)
      throws InvalidKeySpecException, NoSuchAlgorithmException {

    // In this case we expect a username and password in the JSON request body:
    if (areCredentialsMissing(localAuthDTO)) {
      return new SegueErrorResponse(Status.BAD_REQUEST, LOGIN_MISSING_CREDENTIALS_MESSAGE).toResponse();
    }

    String email = localAuthDTO.getEmail();
    String password = localAuthDTO.getPassword();
    SegueMetrics.LOG_IN_ATTEMPT.inc();

    try {
      notifySegueLoginRateLimiters(request, email);
    } catch (SegueResourceMisuseException e) {
      log.error(String.format("Segue Login Blocked for (%s). Rate limited - too many logins.",
          sanitiseExternalLogValue(email)));
      return SegueErrorResponse.getRateThrottledResponse(LOGIN_RATE_THROTTLE_MESSAGE);
    }

    try {
      // ok we need to hand over to user manager
      RegisteredUserDTO userToReturn =
          userManager.authenticateWithCredentials(request, response, signinProvider, email, password);

      this.getLogManager().logEvent(userToReturn, request, SegueServerLogType.LOG_IN, Maps.newHashMap());
      SegueMetrics.LOG_IN.inc();

      return Response.ok(userToReturn).build();
    } catch (AdditionalAuthenticationRequiredException e) {
      // in this case the users account has been configured to require a second factor
      // The password challenge has completed successfully but they must complete the second step of the flow
      return Response.accepted(ImmutableMap.of("2FA_REQUIRED", true)).build();
    } catch (AuthenticationProviderMappingException e) {
      log.error(LOGIN_UNKNOWN_PROVIDER_MESSAGE, e);
      return new SegueErrorResponse(Status.BAD_REQUEST, LOGIN_UNKNOWN_PROVIDER_MESSAGE).toResponse();
    } catch (IncorrectCredentialsProvidedException | NoCredentialsAvailableException e) {
      log.info(String.format("Incorrect credentials received for (%s). Error: %s", sanitiseExternalLogValue(email),
          e.getMessage()));
      return new SegueErrorResponse(Status.UNAUTHORIZED, LOGIN_INCORRECT_CREDENTIALS_MESSAGE).toResponse();
    } catch (MFARequiredButNotConfiguredException e) {
      log.warn(String.format("Login blocked for ADMIN account (%s) which does not have 2FA configured.",
          sanitiseExternalLogValue(email)));
      return new SegueErrorResponse(Status.UNAUTHORIZED, e.getMessage()).toResponse();
    } catch (SegueDatabaseException e) {
      log.error(LOGIN_DATABASE_ERROR_MESSAGE, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, LOGIN_DATABASE_ERROR_MESSAGE).toResponse();
    }
  }

  private void notifySegueLoginRateLimiters(final HttpServletRequest request, final String email)
      throws SegueResourceMisuseException {
    String requestingIpAddress = RequestIpExtractor.getClientIpAddr(request);
    // Stop users logging in who have already locked their account.
    misuseMonitor.notifyEvent(email.toLowerCase(), SegueLoginByEmailMisuseHandler.class.getSimpleName());
    misuseMonitor.notifyEvent(requestingIpAddress, SegueLoginByIPMisuseHandler.class.getSimpleName());
  }

  private static boolean areCredentialsMissing(final LocalAuthDTO localAuthDTO) {
    return null == localAuthDTO || null == localAuthDTO.getEmail() || localAuthDTO.getEmail().isEmpty()
        || null == localAuthDTO.getPassword() || localAuthDTO.getPassword().isEmpty();
  }

  /**
   * End point that allows the user to logout - i.e. destroy our cookie.
   *
   * @param request  so that we can destroy the associated session
   * @param response to tell the browser to delete the session for segue.
   * @return successful response to indicate any cookies were destroyed.
   */
  @POST
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.WILDCARD)
  @Path("/logout")
  @Operation(summary = "Initiate logout for the current user.")
  public final Response userLogout(@Context final HttpServletRequest request,
                                   @Context final HttpServletResponse response) {
    try {
      this.getLogManager().logEvent(this.userManager.getCurrentUser(request), request, SegueServerLogType.LOG_OUT,
          Maps.newHashMap());
      SegueMetrics.LOG_OUT.inc();

      userManager.logUserOut(request, response);

      return Response.ok().build();
    } catch (SegueDatabaseException e) {
      log.error(LOGOUT_DATABASE_ERROR_MESSAGE, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, LOGOUT_DATABASE_ERROR_MESSAGE).toResponse();
    } catch (NoUserLoggedInException e) {
      log.warn(LOGOUT_NO_ACTIVE_SESSION_MESSAGE);
      return new SegueErrorResponse(Status.BAD_REQUEST, LOGOUT_NO_ACTIVE_SESSION_MESSAGE).toResponse();
    }
  }

  /**
   * 2nd step of authentication process for local user/passwords to check TOTP token.
   *
   * @param request     - http request
   * @param response    - http response - to add modified cookie
   * @param mfaResponse - mfaResponse
   * @return successfully logged in user or error
   */
  @POST
  @Path("/mfa/challenge")
  @Operation(summary = "Continuation of login flow for users who have 2FA enabled")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public final Response mfaCompleteAuthentication(@Context final HttpServletRequest request,
                                                  @Context final HttpServletResponse response,
                                                  final MFAResponseDTO mfaResponse) {
    if (Strings.isNullOrEmpty(mfaResponse.getMfaVerificationCode())) {
      return SegueErrorResponse.getBadRequestResponse("Response must include mfaVerificationCode");
    }

    UserSummaryWithEmailAddressDTO partiallyLoggedInUser = null;
    try {
      final Integer verificationCode = Integer.parseInt(mfaResponse.getMfaVerificationCode());
      partiallyLoggedInUser = this.userManager.getPartiallyIdentifiedUser(request);

      if (misuseMonitor.hasMisused(partiallyLoggedInUser.getEmail().toLowerCase(),
          SegueLoginByEmailMisuseHandler.class.getSimpleName())) {

        log.error("Segue Login Blocked for (" + partiallyLoggedInUser.getEmail()
            + ") during 2FA step. Rate limited - too many logins.");
        return SegueErrorResponse.getRateThrottledResponse(LOGIN_RATE_THROTTLE_MESSAGE);
      }

      RegisteredUserDTO userToReturn = this.userManager.authenticateMFA(request, response, verificationCode);

      this.getLogManager().logEvent(userToReturn, request, SegueServerLogType.LOG_IN, Maps.newHashMap());
      SegueMetrics.LOG_IN.inc();

      return Response.ok(userToReturn).build();
    } catch (NumberFormatException e) {
      return SegueErrorResponse.getBadRequestResponse("Verification code is not in the correct format.");
    } catch (IncorrectCredentialsProvidedException | NoCredentialsAvailableException e) {
      log.info("Incorrect 2FA code received for (" + partiallyLoggedInUser.getEmail()
          + "). Error reason: " + e.getMessage());
      try {
        misuseMonitor.notifyEvent(partiallyLoggedInUser.getEmail().toLowerCase(),
            SegueLoginByEmailMisuseHandler.class.getSimpleName());

        return new SegueErrorResponse(Status.UNAUTHORIZED, "Incorrect code provided.").toResponse();
      } catch (SegueResourceMisuseException e1) {
        log.error("Segue 2FA verification Blocked for (" + partiallyLoggedInUser.getEmail()
            + "). Rate limited - too many logins.");
        return SegueErrorResponse.getRateThrottledResponse(LOGIN_RATE_THROTTLE_MESSAGE);
      }
    } catch (SegueDatabaseException e) {
      String errorMsg = "Internal Database error has occurred during mfa challenge.";
      log.error(errorMsg, e);
      return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
    } catch (NoUserLoggedInException e) {
      log.error("Unable to retrieve partial session information for MFA flow. "
          + "Maybe users' partial session expired?");
      return SegueErrorResponse.getBadRequestResponse("Unable to complete 2FA login. "
          + "Your login session may have expired. Please enter your password again.");
    }
  }
}