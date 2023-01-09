/*
 * Copyright 2014 Stephen Cummins
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
package uk.ac.cam.cl.dtg.segue.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserContext;
import uk.ac.cam.cl.dtg.isaac.dos.users.UserSettings;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByEmailMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetByIPMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics;
import uk.ac.cam.cl.dtg.segue.api.monitors.TeacherPasswordResetMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * User facade.
 *
 * @author Stephen Cummins
 *
 */
@Path("/")
@Tag(name = "/users")
public class UsersFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(UsersFacade.class);
    private final UserAccountManager userManager;
    private final UserAssociationManager userAssociationManager;
    private final IMisuseMonitor misuseMonitor;
    private final AbstractUserPreferenceManager userPreferenceManager;
    private final SchoolListReader schoolListReader;

    /**
     * Construct an instance of the UsersFacade.
     *
     * @param properties
     *            - properties loader for the application
     * @param userManager
     *            - user manager for the application
     * @param logManager
     *            - so we can log interesting events.
     * @param userAssociationManager
     *            - so we can check permissions..
     * @param misuseMonitor
     *            - so we can check for misuse
     * @param userPreferenceManager
     *            - so we can provide user preferences
     * @param schoolListReader
     *            - so we can augment school info
     */
    @Inject
    public UsersFacade(final PropertiesLoader properties, final UserAccountManager userManager,
                       final ILogManager logManager, final UserAssociationManager userAssociationManager,
                       final IMisuseMonitor misuseMonitor, final AbstractUserPreferenceManager userPreferenceManager,
                       final SchoolListReader schoolListReader) {
        super(properties, logManager);
        this.userManager = userManager;
        this.userAssociationManager = userAssociationManager;
        this.misuseMonitor = misuseMonitor;
        this.userPreferenceManager = userPreferenceManager;
        this.schoolListReader = schoolListReader;
    }

    /**
     * Get the details of the currently logged in user.
     *
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @param response
     *            - the response to set session expiry information headers on.
     * @return Returns the current user DTO if we can get it or null response if we can't. It will be a 204 No Content
     */
    @GET
    @Path("users/current_user")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get information about the current user.")
    public Response getCurrentUserEndpoint(@Context final Request request,
                                           @Context final HttpServletRequest httpServletRequest,
                                           @Context final HttpServletResponse response) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);

            Date sessionExpiry = userManager.getSessionExpiry(httpServletRequest);
            int sessionExpiryHashCode = 0;
            if (null != sessionExpiry) {
                sessionExpiryHashCode = sessionExpiry.hashCode();
                response.setDateHeader("X-Session-Expires", sessionExpiry.getTime());
            }

            // Calculate the ETag based on the user we just retrieved and the session expiry:
            EntityTag etag = new EntityTag(currentUser.toString().hashCode() + sessionExpiryHashCode + "");
            Response cachedResponse = generateCachedResponse(request, etag, Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(currentUser).tag(etag)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * This method allows users to create a local account or update their settings.
     *
     * It will also allow administrators to change any user settings.
     *
     * @param request
     *            - the http request of the user wishing to authenticate
     * @param response
     *            to tell the browser to store the session in our own segue cookie.
     * @param userObjectString
     *            - object containing all user account information including passwords.
     * @return the updated users object.
     */
    @POST
    @Path("users")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Create a new user or update an existing user.")
    public Response createOrUpdateUserSettings(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response,
                                               final String userObjectString) throws InvalidKeySpecException, NoSuchAlgorithmException {

        UserSettings userSettingsObjectFromClient;
        String newPassword;
        try {
            ObjectMapper tmpObjectMapper = new ObjectMapper();
            tmpObjectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            //TODO: We need to change the way the frontend sends passwords to reduce complexity
            Map<String, Object> mapRepresentation = tmpObjectMapper.readValue(userObjectString, HashMap.class);
            newPassword = (String) ((Map)mapRepresentation.get("registeredUser")).get("password");
            ((Map)mapRepresentation.get("registeredUser")).remove("password");
            userSettingsObjectFromClient = tmpObjectMapper.convertValue(mapRepresentation, UserSettings.class);
            
            if (null == userSettingsObjectFromClient) {
                return new SegueErrorResponse(Status.BAD_REQUEST,  "No user settings provided.").toResponse();
            }
        } catch (IOException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to parse the user object you provided.", e)
                    .toResponse();
        }

        RegisteredUser registeredUser = userSettingsObjectFromClient.getRegisteredUser();

        // Extract the user preferences, which are validated before saving by userPreferenceObjectToList(...).
        Map<String, Map<String, Boolean>> userPreferences = userSettingsObjectFromClient.getUserPreferences();

        List<UserContext> registeredUserContexts = userSettingsObjectFromClient.getRegisteredUserContexts();

        if (null != registeredUser.getId()) {
            try {
                return userManager.updateUserObject(request, response, registeredUser,
                        userSettingsObjectFromClient.getPasswordCurrent(), newPassword,
                        userPreferences, registeredUserContexts);
            } catch (IncorrectCredentialsProvidedException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Incorrect credentials provided.", e)
                        .toResponse();
            } catch (NoCredentialsAvailableException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "No credentials available.", e)
                        .toResponse();
            }
        } else {
            try {
                String ipAddress = RequestIPExtractor.getClientIpAddr(request);
                misuseMonitor.notifyEvent(ipAddress, RegistrationMisuseHandler.class.getSimpleName());
                SegueMetrics.USER_REGISTRATION.inc();

                // Add some logging for what ought to be an impossible case; that of a registration attempt coming from a client
                // which has not made any other authenticated/logged request to Isaac beforehand.
                // This _might_ be suspicious, and this logging will help establish that.
                if (request.getSession() == null || request.getSession().getAttribute(ANONYMOUS_USER) == null) {
                    log.error(String.format("Registration attempt from (%s) for (%s) without corresponding anonymous user!", ipAddress, registeredUser.getEmail()));
                }

                // TODO rememberMe is set as true. Do we assume a user will want to be remembered on the machine the register on?
                return userManager.createUserObjectAndLogIn(request, response, registeredUser, newPassword, userPreferences, true);
            } catch (SegueResourceMisuseException e) {
                log.error(String.format("Blocked a registration attempt by (%s) after misuse limit hit!", RequestIPExtractor.getClientIpAddr(request)));
                return SegueErrorResponse.getRateThrottledResponse("Too many registration requests. Please try again later or contact us!");
            }
        }

    }

    /**
     * Provides access to user preferences, mapping preference types to an inner map of preference names and values.
     * @param httpServletRequest - the request, to work out the current user
     * @return user preference map
     */
    @GET
    @Path("users/user_preferences")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the user preferences of the current user.")
    public Response getUserPreferences(@Context final HttpServletRequest httpServletRequest) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            List<UserPreference> allUserPreferences = userPreferenceManager.getAllUserPreferences(currentUser.getId());

            Map <String, Map<String, Boolean>> userPreferences = Maps.newHashMap();
            for (UserPreference pref : allUserPreferences) {
                if (!userPreferences.containsKey(pref.getPreferenceType())) {
                    userPreferences.put(pref.getPreferenceType(), Maps.newHashMap());
                }
                userPreferences.get(pref.getPreferenceType()).put(pref.getPreferenceName(), pref.getPreferenceValue());
            }

            return Response.ok(userPreferences)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Can't load user preferences!", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     * An endpoint for group managers (often teachers) to send a password reset request email to group members without
     * having to know the group members account email.
     *
     * @param request - request information used for caching
     * @param httpServletRequest - the request, to work ou the current user
     * @param userIdOfInterest - userId of interest - usually a the teacher's student
     * @return a successful response regardless of whether the email exists or an error code if there is a technical
     *         fault
     */
    @POST
    @Path("users/{user_id}/resetpassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Request password reset for another user.")
    public Response generatePasswordResetTokenForOtherUser(@Context final Request request,
                                                           @Context final HttpServletRequest httpServletRequest,
                                                           @PathParam("user_id") final Long userIdOfInterest) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);

            RegisteredUserDTO userOfInterest = userManager.getUserDTOById(userIdOfInterest);
            if (userOfInterest == null) {
                throw new NoUserException("No user found with this ID.");
            }

            UserSummaryDTO userOfInterestSummaryObject = userManager.convertToUserSummaryObject(userOfInterest);

            // Decide if the user is allowed to view this data. User must have at least a teacher account to request
            // a password reset for another user.
            if (!currentUser.getId().equals(userIdOfInterest)
                    && !userAssociationManager.hasTeacherPermission(currentUser, userOfInterestSummaryObject)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            misuseMonitor.notifyEvent(currentUser.getEmail(), TeacherPasswordResetMisuseHandler.class.getSimpleName());
            SegueMetrics.PASSWORD_RESET.inc();
            userManager.resetPasswordRequest(userOfInterest);

            this.getLogManager()
                    .logEvent(currentUser, httpServletRequest, SegueServerLogType.PASSWORD_RESET_REQUEST_RECEIVED,
                            ImmutableMap.of(
                                    LOCAL_AUTH_EMAIL_FIELDNAME, userOfInterest.getEmail(),
                                    LOCAL_AUTH_GROUP_MANAGER_EMAIL_FIELDNAME, currentUser.getEmail(),
                                    LOCAL_AUTH_GROUP_MANAGER_INITIATED_FIELDNAME, true));
            return Response.ok().build();

        } catch (NoUserException e) {
            log.warn("Password reset requested for account that does not exist: " + e.getMessage());
            // Return OK so we don't leak account existence.
            return Response.ok().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (CommunicationException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error sending reset message.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (SegueDatabaseException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error generating password reset token.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (SegueResourceMisuseException e) {
            String message = "You have exceeded the number of requests allowed for this endpoint. "
                    + "Please try again later.";
            return SegueErrorResponse.getRateThrottledResponse(message);
        }
    }


    /**
     * End point that allows a local user to generate a password reset request.
     *
     * Step 1 of password reset process - send user an e-mail
     *
     * @param userObject
     *            - A user object containing the email of the user requesting a reset
     * @param request
     *            - For logging purposes.
     * @return a successful response regardless of whether the email exists or an error code if there is a technical
     *         fault
     */
    @POST
    @Path("users/resetpassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Request password reset for an email address.",
                  description = "The email address must be provided as a RegisteredUserDTO object, although only the 'email' field is required.")
    public Response generatePasswordResetToken(final RegisteredUserDTO userObject,
                                               @Context final HttpServletRequest request) {
        if (null == userObject || null == userObject.getEmail() || userObject.getEmail().isEmpty()) {
            log.debug("User is null");
            return new SegueErrorResponse(Status.BAD_REQUEST, "No account email address provided.").toResponse();
        }

        try {
            String requestingIPAddress = RequestIPExtractor.getClientIpAddr(request);
            misuseMonitor.notifyEvent(userObject.getEmail(), PasswordResetByEmailMisuseHandler.class.getSimpleName());
            misuseMonitor.notifyEvent(requestingIPAddress, PasswordResetByIPMisuseHandler.class.getSimpleName());
            userManager.resetPasswordRequest(userObject);

            this.getLogManager()
                    .logEvent(userManager.getCurrentUser(request), request, SegueServerLogType.PASSWORD_RESET_REQUEST_RECEIVED,
                            ImmutableMap.of(LOCAL_AUTH_EMAIL_FIELDNAME, userObject.getEmail()));

            return Response.ok().build();
        } catch (NoUserException e) {
            log.warn("Password reset requested for account that does not exist: (" + userObject.getEmail() + ")");
            // Return OK so we don't leak account existence.
            return Response.ok().build();
        } catch (CommunicationException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error sending reset message.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (SegueDatabaseException | InvalidKeySpecException | NoSuchAlgorithmException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error generating password reset token.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (SegueResourceMisuseException e) {
            String message = "You have exceeded the number of requests allowed for this endpoint. "
                    + "Please try again later.";
            log.error("Password reset request blocked for email: (" + userObject.getEmail() + ")", e.toString());
            return SegueErrorResponse.getRateThrottledResponse(message);
        }
    }

    /**
     * End point that verifies whether or not a password reset token is valid.
     *
     * Optional Step 2 - validate token is correct
     *
     * @param token
     *            - A password reset token
     * @return Success if the token is valid, otherwise returns not found
     */
    @GET
    @Path("users/resetpassword/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Verify a password reset token is valid for use.")
    public Response validatePasswordResetRequest(@PathParam("token") final String token) {
        try {
            if (userManager.validatePasswordResetToken(token)) {
                return Response.ok().build();
            }
        } catch (SegueDatabaseException e) {
            log.error("Internal database error, while validating Password Reset Request.", e);
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error has occurred. Unable to access token list.");
            return error.toResponse();
        }

        SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Invalid password reset token.");
        log.debug(String.format("Invalid password reset token: %s", token));
        return error.toResponse();
    }

    /**
     * Final step of password reset process. Change password.
     *
     * @param token
     *            - A password reset token
     * @param clientResponse
     *            - A user object containing password information.
     * @param request
     *            - For logging purposes.
     * @return successful response.
     */
    @POST
    @Path("users/resetpassword/{token}")
    @Consumes(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Reset an account password using a reset token.",
                  description = "The 'token' should be generated using one of the endpoints for requesting a password reset.")
    public Response resetPassword(@PathParam("token") final String token, final Map<String, String> clientResponse,
                                  @Context final HttpServletRequest request)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        try {
            String newPassword = clientResponse.get("password");
            RegisteredUserDTO userDTO = userManager.resetPassword(token, newPassword);

            this.getLogManager().logEvent(userDTO, request, SegueServerLogType.PASSWORD_RESET_REQUEST_SUCCESSFUL,
                    ImmutableMap.of(LOCAL_AUTH_EMAIL_FIELDNAME, userDTO.getEmail()));

            // we can reset the misuse monitor for incorrect logins now.
            misuseMonitor.resetMisuseCount(userDTO.getEmail().toLowerCase(), SegueLoginMisuseHandler.class.getSimpleName());

        } catch (InvalidTokenException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid password reset token.");
            log.error("Invalid password reset token supplied: " + token);
            return error.toResponse();
        } catch (InvalidPasswordException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage());
            return error.toResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Database error has occurred during reset password process. Please try again later";
            log.error(errorMsg, e);
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg);
            return error.toResponse();
        }

        return Response.ok().build();
    }

    /**
     * Endpoint to generate non-persistent new secret for the client.
     *
     * This can be used with an appropriate challenge to setup 2FA on the account.
     *
     * @param request - http request so we can determine the user.
     * @return TOTPSharedSecret to allow user next step of setup process
     */
    @GET
    @Path("users/current_user/mfa/new_secret")
    @Operation(summary = "Generate a new 2FA secret for the current user.")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response generateMFACode(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            return Response.ok(this.userManager.getNewSharedSecret(user))
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Endpoint to determine whether the current user has MFA enabled or not.
     *
     * @param request - http request so we can determine the user.
     * @return whether the user has MFA enabled
     */
    @GET
    @Path("users/current_user/mfa")
    @Operation(summary = "Does the current user have MFA enabled?.")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getAccountMFAStatus(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            return Response.ok(ImmutableMap.of("mfaStatus", this.userManager.has2FAConfigured(user)))
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Internal Database error has occurred during setup of MFA.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        }
    }

    /**
     * This endpoint is used to setup MFA on a local segue account. User must have requested a shared secret already.
     *
     * @param request - containing user information
     * @param mfaResponse - map which must contain the sharedSecret and mfaVerificationCode
     * @return success response or error response
     */
    @POST
    @Path("users/current_user/mfa")
    @Operation(summary = "Setup MFA based on successful challenge / response")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response setupMFA(@Context final HttpServletRequest request, final Map<String, String> mfaResponse) {
        try {
            if (Strings.isNullOrEmpty(mfaResponse.get("sharedSecret")) || Strings.isNullOrEmpty(mfaResponse.get("mfaVerificationCode"))) {
                return SegueErrorResponse.getBadRequestResponse("Response must include full sharedSecret object and mfaVerificationCode");
            }

            final String sharedSecret = mfaResponse.get("sharedSecret");
            final Integer verificationCode = Integer.parseInt(mfaResponse.get("mfaVerificationCode"));

            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);
            if (this.userManager.activateMFAForUser(user, sharedSecret, verificationCode)) {
                return Response.ok().build();
            } else {
                return SegueErrorResponse.getBadRequestResponse("Verification code is incorrect");
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Internal Database error has occurred during setup of MFA.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (NumberFormatException e) {
            return SegueErrorResponse.getBadRequestResponse("Verification code is not in the correct format.");
        }
    }

    /**
     * This endpoint is used to delete MFA from a local segue account. User must be an admin.
     *
     * @param request - containing current user information
     * @param userIdOfInterest - userId of interest
     * @return success response or error response
     */
    @DELETE
    @Path("users/{user_id}/mfa")
    @Operation(summary = "Admin endpoint for disabling MFA for a user")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deleteMFASettingsForAccount(@Context final HttpServletRequest request,
                                                      @PathParam("user_id") final Long userIdOfInterest) {
        try {
            final RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                // Non-admins should not be able to disable other users' 2FA.
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            final RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(userIdOfInterest);

            if (currentlyLoggedInUser.getId().equals(userOfInterest.getId())) {
                return Response.status(Status.FORBIDDEN).entity("Unable to change the MFA status of the account you are "
                        + "currently using. Ask another Admin for help.").build();
            }

            this.userManager.deactivateMFAForUser(userOfInterest);
            log.info(String.format("Admin (%s) deactivated MFA on account (%s)!",
                    currentlyLoggedInUser.getEmail(), userOfInterest.getId()));

            return Response.ok().build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Internal Database error has occurred during deletion of MFA.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (NumberFormatException e) {
            return SegueErrorResponse.getBadRequestResponse("UserId must be a number");
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("Unable to complete MFA removal as user account could not be found.");
        }
    }

    /**
     * This method allows the requester to provide a list of user ids and get back a mapping of the user
     * id to the school information. This is useful for building up tables of users school information.
     *
     * @param httpServletRequest
     *            Authentication and authorisation
     * @param userIdsQueryParam
     *            The comma seperated list of user ids to try and find schools for.
     * @return A map mapping the userId to a school if we found one for it.
     */
    @GET
    @Path("users/school_lookup")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the school information of specified users.")
    public Response getUserIdToSchoolMap(@Context final HttpServletRequest httpServletRequest,
                                         @QueryParam("user_ids") final String userIdsQueryParam) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserStaff(userManager, currentUser) && !Role.EVENT_LEADER.equals(currentUser.getRole())) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            if (null == userIdsQueryParam || userIdsQueryParam.isEmpty()) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a comma separated list of user_ids in the query param")
                        .toResponse();
            }

            List<Long> userIds = Arrays.stream(userIdsQueryParam.split(","))
                    .map(schoolId -> Long.parseLong(schoolId))
                    .collect(Collectors.toList());

            // Restrict event leader queries to users who have granted access to their data
            if (Role.EVENT_LEADER.equals(currentUser.getRole())) {
                userIds = userAssociationManager.filterUnassociatedRecords(currentUser, userIds);
            }

            final List<RegisteredUserDTO> users = this.userManager.findUsers(userIds);
            final ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            for (RegisteredUserDTO user : users) {
                if (user.getSchoolId() != null) {
                    School school = schoolListReader.findSchoolById(user.getSchoolId());
                    if (null != school) {
                        builder.put(user.getId().toString(), school);
                    } else {
                        // The school once existed in the list but no longer does. Set the name to be the URN:
                        builder.put(user.getId().toString(), ImmutableMap.of("name", user.getSchoolId()));
                    }
                } else if (user.getSchoolOther() != null && !user.getSchoolOther().isEmpty()) {
                    builder.put(user.getId().toString(), ImmutableMap.of("name", user.getSchoolOther()));
                }
            }

            return Response.ok(builder.build()).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to parse all parameters as integers.")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while looking up users", e)
                    .toResponse();
        } catch (IOException | UnableToIndexSchoolsException | SegueSearchException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while looking up schools", e)
                    .toResponse();
        }
    }
}
