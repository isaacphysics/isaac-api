/**
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

import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.IsaacUserPreferences; // FIXME: Isaac class in Segue!

import com.google.api.client.util.Maps;
import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Validate;

import org.jboss.resteasy.annotations.GZIP;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.PasswordResetRequestMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.RegistrationMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueLoginMisuseHandler;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics;
import uk.ac.cam.cl.dtg.segue.auth.AuthenticationProvider;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.AuthenticationProviderMappingException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.DuplicateAccountException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.FailedToHashPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.IncorrectCredentialsProvidedException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidPasswordException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.InvalidTokenException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.MissingRequiredFieldException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoCredentialsAvailableException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.CommunicationException;
import uk.ac.cam.cl.dtg.segue.comm.EmailMustBeVerifiedException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.segue.dos.UserPreference;
import uk.ac.cam.cl.dtg.segue.dos.users.RegisteredUser;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dos.users.UserSettings;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Sets;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

/**
 * User facade.
 *
 * @author Stephen Cummins
 *
 */
@Path("/")
@Api(value = "/users")
public class UsersFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(UsersFacade.class);
    private final UserAccountManager userManager;
    private final StatisticsManager statsManager;
    private final UserAssociationManager userAssociationManager;
    private final IMisuseMonitor misuseMonitor;
    private final AbstractUserPreferenceManager userPreferenceManager;
    private final SchoolListReader schoolListReader;
    private final Supplier<Set<School>> schoolOtherSupplier;

    /**
     * Construct an instance of the UsersFacade.
     *
     * @param properties
     *            - properties loader for the application
     * @param userManager
     *            - user manager for the application
     * @param logManager
     *            - so we can log interesting events.
     * @param statsManager
     *            - so we can view stats on interesting events.
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
                       final ILogManager logManager, final StatisticsManager statsManager,
                       final UserAssociationManager userAssociationManager, final IMisuseMonitor misuseMonitor,
                       final AbstractUserPreferenceManager userPreferenceManager, final SchoolListReader schoolListReader) {
        super(properties, logManager);
        this.userManager = userManager;
        this.statsManager = statsManager;
        this.userAssociationManager = userAssociationManager;
        this.misuseMonitor = misuseMonitor;
        this.userPreferenceManager = userPreferenceManager;
        this.schoolListReader = schoolListReader;

        this.schoolOtherSupplier = Suppliers.memoizeWithExpiration(new Supplier<Set<School>>() {
            @Override
            public Set<School> get() {
                try {
                    List<RegisteredUserDTO> users = userManager.findUsers(new RegisteredUserDTO());

                    Set<School> schoolOthers = Sets.newHashSet();

                    for (RegisteredUserDTO user : users) {
                        if (user.getSchoolOther() != null) {
                            School pseudoSchool = new School();
                            pseudoSchool.setUrn(Integer.toString(user.getSchoolOther().hashCode()));
                            pseudoSchool.setName(user.getSchoolOther());
                            pseudoSchool.setDataSource(School.SchoolDataSource.USER_ENTERED);
                            schoolOthers.add(pseudoSchool);
                        }
                    }
                    return schoolOthers;
                } catch (SegueDatabaseException e) {
                    return null;
                }
            }
        }, 1, TimeUnit.DAYS);
    }

    /**
     * Get the details of the currently logged in user.
     *
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @return Returns the current user DTO if we can get it or null response if we can't. It will be a 204 No Content
     */
    @GET
    @Path("users/current_user")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get information about the current user.")
    public Response getCurrentUserEndpoint(@Context final Request request,
                                           @Context final HttpServletRequest httpServletRequest) {
        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);

            // Calculate the ETag based on User we just retrieved from the DB
            EntityTag etag = new EntityTag("currentUser".hashCode() + currentUser.toString().hashCode() + "");
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
    @ApiOperation(value = "Create a new user or update an existing user.")
    public Response createOrUpdateUserSettings(@Context final HttpServletRequest request,
                                               @Context final HttpServletResponse response, final String userObjectString) {

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

        if (null != registeredUser.getId()) {

            try {
                return this.updateUserObject(request, response, registeredUser,
                        userSettingsObjectFromClient.getPasswordCurrent(), newPassword, userPreferences);
            } catch (IncorrectCredentialsProvidedException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Incorrect credentials provided.", e)
                        .toResponse();
            } catch (NoCredentialsAvailableException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "No credentials available.", e)
                        .toResponse();
            }
        } else {
            try {
                misuseMonitor.notifyEvent(RequestIPExtractor.getClientIpAddr(request), RegistrationMisuseHandler.class.toString());
                SegueMetrics.USER_REGISTRATION.inc();
                return this.createUserObjectAndLogIn(request, response, registeredUser, newPassword, userPreferences);
            } catch (SegueResourceMisuseException e) {
                log.error(String.format("Blocked a registration attempt by (%s) after misuse limit hit!", RequestIPExtractor.getClientIpAddr(request)));
                return SegueErrorResponse.getRateThrottledResponse("Too many registration requests. Please try again later ot contact us!");
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
    @ApiOperation(value = "Get the user preferences of the current user.")
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
    @ApiOperation(value = "Request password reset for another user.")
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

            // decide if the user is allowed to view this data.
            if (!currentUser.getId().equals(userIdOfInterest)
                    && !userAssociationManager.hasPermission(currentUser, userOfInterestSummaryObject)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            misuseMonitor.notifyEvent(currentUser.getEmail() + "_group_member_reset", PasswordResetRequestMisuseHandler.class.toString());
            SegueMetrics.PASSWORD_RESET.inc();
            userManager.resetPasswordRequest(userOfInterest);

            this.getLogManager()
                    .logEvent(currentUser, httpServletRequest, SegueLogType.PASSWORD_RESET_REQUEST_RECEIVED,
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
    @ApiOperation(value = "Request password reset for an email address.",
                  notes = "The email address must be provided as a RegisteredUserDTO object, although only the 'email' field is required.")
    public Response generatePasswordResetToken(final RegisteredUserDTO userObject,
                                               @Context final HttpServletRequest request) {
        if (null == userObject) {
            log.debug("User is null");
            return new SegueErrorResponse(Status.BAD_REQUEST, "No user settings provided.").toResponse();
        }

        try {
            misuseMonitor.notifyEvent(userObject.getEmail(), PasswordResetRequestMisuseHandler.class.toString());
            userManager.resetPasswordRequest(userObject);

            this.getLogManager()
                    .logEvent(userManager.getCurrentUser(request), request, SegueLogType.PASSWORD_RESET_REQUEST_RECEIVED,
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
            log.error("Too many password resets requested by: (" + userObject.getEmail() + ")", e.toString());
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
    @ApiOperation(value = "Verify a password reset token is valid for use.")
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
    @ApiOperation(value = "Reset an account password using a reset token.",
                  notes = "The 'token' should be generated using one of the endpoints for requesting a password reset.")
    public Response resetPassword(@PathParam("token") final String token, final Map<String, String> clientResponse,
                                  @Context final HttpServletRequest request) {
        try {
            String newPassword = clientResponse.get("password");
            RegisteredUserDTO userDTO = userManager.resetPassword(token, newPassword);

            this.getLogManager().logEvent(userDTO, request, SegueLogType.PASSWORD_RESET_REQUEST_SUCCESSFUL,
                    ImmutableMap.of(LOCAL_AUTH_EMAIL_FIELDNAME, userDTO.getEmail()));

            // we can reset the misuse monitor for incorrect logins now.
            misuseMonitor.resetMisuseCount(userDTO.getEmail().toLowerCase(), SegueLoginMisuseHandler.class.toString());

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
    @ApiOperation(value = "Generate a new 2FA secret for the current user.")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response generateMFACode(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            return Response.ok(this.userManager.getNewSharedSecret(user)).build();

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Endpoint to determine whether the current user has MFA enabled or not.
     *
     * @param request - http request so we can determine the user.
     * @return TOTPSharedSecret to allow user next step of setup process
     */
    @GET
    @Path("users/current_user/mfa")
    @ApiOperation(value = "Does the current user have MFA enabled?.")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response getAccountMFAStatus(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = this.userManager.getCurrentRegisteredUser(request);

            return Response.ok(ImmutableMap.of("mfaStatus", this.userManager.has2FAConfigured(user))).build();
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
    @ApiOperation(value = "Setup MFA based on successful challenge / response")
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
     * @param otherUserId - userId of interest
     * @return success response or error response
     */
    @DELETE
    @Path("users/{user_id}/mfa")
    @ApiOperation(value = "Admin endpoint for disabling MFA for a user")
    @Produces(MediaType.APPLICATION_JSON)
    public final Response deleteMFASettingsForAccount(@Context final HttpServletRequest request, @PathParam("user_id") final String otherUserId) {
        try {
            final RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(request);
            if (!(Role.ADMIN.equals(currentlyLoggedInUser.getRole()))) {
                // Non-admins should not be able to disable other users' 2FA.
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            final RegisteredUserDTO userOfInterest = this.userManager.getUserDTOById(Long.parseLong(otherUserId));

            if (currentlyLoggedInUser.getId().equals(userOfInterest.getId())) {
                return Response.status(Status.FORBIDDEN).entity("Unable to change the MFA status of the account you are "
                        + "currently using. Ask another Admin for help.").build();
            }

            this.userManager.deactivateMFAForUser(userOfInterest);
            log.info(String.format("Admin userid (%s) deactivated MFA on account owned by userid (%s)", currentlyLoggedInUser.getId(), userOfInterest.getId()));

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
     * Get the event data for a specified user.
     *
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @param userIdOfInterest
     *            - userId of interest - currently only supports looking at own data.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @param events
     *            - comma separated list of events of interest.
     * @param bin
     *            - Should we group data into the first day of the month? true or false.
     * @return Returns a map of eventType to Map of dates to total number of events.
     */
    @GET
    @Path("users/{user_id}/event_data/over_time")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get log data counts for a specific user.")
    public Response getEventDataForUser(@Context final Request request,
                                        @Context final HttpServletRequest httpServletRequest, @PathParam("user_id") final Long userIdOfInterest,
                                        @QueryParam("from_date") final Long fromDate, @QueryParam("to_date") final Long toDate,
                                        @QueryParam("events") final String events, @QueryParam("bin_data") final Boolean bin) {
        final boolean binData;
        if (null == bin || !bin) {
            binData = false;
        } else {
            binData = true;
        }

        if (null == events) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must specify the events you are interested in.")
                    .toResponse();
        }

        if (null == fromDate || null == toDate) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You must specify the from_date and to_date you are interested in.").toResponse();
        }

        if (fromDate > toDate) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "The from_date must be before the to_date!").toResponse();
        }

        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);

            RegisteredUserDTO userOfInterest = userManager.getUserDTOById(userIdOfInterest);
            if (userOfInterest == null) {
                throw new NoUserException("No user found with this ID.");
            }

            UserSummaryDTO userOfInterestSummaryObject = userManager.convertToUserSummaryObject(userOfInterest);

            if (!events.equals(SegueLogType.ANSWER_QUESTION.name()) && currentUser.getRole() != Role.ADMIN) {
                // Non-admins should not be able to choose random log events.
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            // decide if the user is allowed to view this data.
            if (!currentUser.getId().equals(userIdOfInterest)
                    && !userAssociationManager.hasPermission(currentUser, userOfInterestSummaryObject)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            // No point looking for stats from before the user registered (except for merged logs at registration and
            // these will only be ANONYMOUS_SESSION_DURATION_IN_MINUTES before registration anyway: less than 1 month):
            Date fromDateObject = new Date(fromDate);
            if (fromDateObject.before(userOfInterest.getRegistrationDate())) {
                fromDateObject = userOfInterest.getRegistrationDate();
            }

            Map<String, Map<LocalDate, Long>> eventLogsByDate = this.statsManager.getEventLogsByDateAndUserList(
                    Lists.newArrayList(events.split(",")), fromDateObject, new Date(toDate),
                    Arrays.asList(userOfInterest), binData);

            return Response.ok(eventLogsByDate).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to find user with the id provided.").toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Unable to look up user event history for user " + userIdOfInterest, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while looking up event information")
                    .toResponse();
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
    @ApiOperation(value = "Get the school information of specified users.")
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


    /**
     * Get a Set of all schools reported by users in the school other field.
     *
     * @param request
     *            for caching purposes.
     * @return list of strings.
     */
    @GET
    @Path("users/schools_other")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get a list of all custom provided schools.",
                  notes = "This data only contains schools listed in the 'School (Other)' field on user accounts.")
    public Response getAllSchoolOtherResponses(@Context final Request request) {

        Set<School> schoolOthers = schoolOtherSupplier.get();
        if (null != schoolOthers) {
            EntityTag etag = new EntityTag(schoolOthers.toString().hashCode() + "");
            Response cachedResponse = generateCachedResponse(request, etag, Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(schoolOthers).tag(etag)
                    .cacheControl(getCacheControl(Constants.NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();

        } else {
            log.error("Unable to get school list");
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while looking up schools")
                    .toResponse();
        }
    }



    /**
     * Update a user object.
     *
     * This method does all of the necessary security checks to determine who is allowed to edit what.
     *
     * @param request
     *            - so that we can identify the user
     * @param response
     *            - so we can modify the session
     * @param userObjectFromClient
     *            - the new user object from the clients perspective.
     * @param passwordCurrent
     * 			  - the current password, used if the password has changed
     * @param newPassword
     * 			  - the new password, used if the password has changed
     * @param userPreferenceObject
     * 			  - the preferences for this user
     * @return the updated user object.
     * @throws NoCredentialsAvailableException
     * @throws IncorrectCredentialsProvidedException
     */
    private Response updateUserObject(final HttpServletRequest request, final HttpServletResponse response,
                                      final RegisteredUser userObjectFromClient, final String passwordCurrent, final String newPassword,
                                      final Map<String, Map<String, Boolean>> userPreferenceObject)
                                throws IncorrectCredentialsProvidedException, NoCredentialsAvailableException {
        Validate.notNull(userObjectFromClient.getId());

        // this is an update as the user has an id
        // security checks
        try {
            // check that the current user has permissions to change this users details.
            RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(request);
            if (!currentlyLoggedInUser.getId().equals(userObjectFromClient.getId())
                    && currentlyLoggedInUser.getRole() != Role.ADMIN
                    && currentlyLoggedInUser.getRole() != Role.EVENT_MANAGER) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot change someone else's user settings.")
                        .toResponse();
            }

            // check if they are trying to change a password
            if (newPassword != null && !newPassword.isEmpty()) {
                // only admins and the account owner can change passwords 
                if (!currentlyLoggedInUser.getId().equals(userObjectFromClient.getId())
                        && currentlyLoggedInUser.getRole() != Role.ADMIN) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You cannot change someone else's password.")
                            .toResponse();
                }

                // Password change requires auth check unless admin is modifying non-admin user account
                if (!(currentlyLoggedInUser.getRole() == Role.ADMIN && userObjectFromClient.getRole() != Role.ADMIN)) {
                    // authenticate the user to check they are allowed to change the password

                    if (null == passwordCurrent) {
                        return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide your current password"
                            + " to change your password!").toResponse();
                    }

                    this.userManager.ensureCorrectPassword(AuthenticationProvider.SEGUE.name(),
                            userObjectFromClient.getEmail(), passwordCurrent);
                }
            }

            // check that any changes to protected fields being made are allowed.
            RegisteredUserDTO existingUserFromDb = this.userManager.getUserDTOById(userObjectFromClient
                    .getId());

            if (Role.EVENT_MANAGER.equals(currentlyLoggedInUser.getRole())) {
                if (Role.ADMIN.equals(existingUserFromDb.getRole())
                        || Role.ADMIN.equals(userObjectFromClient.getRole())) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You cannot modify admin roles.").toResponse();
                }
            }

            // check that the user is allowed to change the role of another user
            // if that is what they are doing.
            if ((currentlyLoggedInUser.getRole() != Role.ADMIN && currentlyLoggedInUser.getRole() != Role.EVENT_MANAGER)
                    && userObjectFromClient.getRole() != null
                    && !userObjectFromClient.getRole().equals(existingUserFromDb.getRole())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You do not have permission to change a users role.")
                        .toResponse();
            }

            RegisteredUserDTO updatedUser = userManager.updateUserObject(userObjectFromClient, newPassword);

            // If the user's role has changed, record it. Check this using Objects.equals() to be null safe!
            if (!Objects.equals(updatedUser.getRole(), existingUserFromDb.getRole())) {
                log.info("ADMIN user " + currentlyLoggedInUser.getEmail() + " has modified the role of "
                        + updatedUser.getEmail() + "[" + updatedUser.getId() + "]" + " to "
                        + updatedUser.getRole());
                this.getLogManager().logEvent(currentlyLoggedInUser, request, SegueLogType.CHANGE_USER_ROLE,
                        ImmutableMap.of(USER_ID_FKEY_FIELDNAME, updatedUser.getId(),
                                        "oldRole", existingUserFromDb.getRole(),
                                        "newRole", updatedUser.getRole()));
            }

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
                    this.getLogManager().logEvent(currentlyLoggedInUser, request, SegueLogType.ADMIN_CHANGE_USER_SCHOOL,
                            eventDetails);
                } else {
                    this.getLogManager().logEvent(currentlyLoggedInUser, request, SegueLogType.USER_SCHOOL_CHANGE,
                            eventDetails);
                }
            }

            if (userPreferenceObject != null) {
                List<UserPreference> userPreferences = userPreferenceObjectToList(userPreferenceObject, updatedUser.getId());
                userPreferenceManager.saveUserPreferences(userPreferences);
            }

            return Response.ok(updatedUser).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "The user specified does not exist.").toResponse();
        } catch (DuplicateAccountException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "An account already exists with the e-mail address specified.").toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Unable to modify user", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while modifying the user").toResponse();
        } catch (InvalidPasswordException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (MissingRequiredFieldException e) {
            log.warn("Missing field during update operation. ", e);
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (AuthenticationProviderMappingException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to map to a known authenticator. The provider: is unknown").toResponse();
        }
    }

    /**
     * Create a user object. This method allows new user objects to be created.
     *
     * @param request
     *            - so that we can identify the user
     * @param response
     *            to tell the browser to store the session in our own segue cookie.
     * @param userObjectFromClient
     *            - the new user object from the clients perspective.
     * @param newPassword
     *            - the new password for the user.
     * @param userPreferenceObject
     * 			  - the new preferences for this user
     * @return the updated user object.
     */
    private Response createUserObjectAndLogIn(final HttpServletRequest request, final HttpServletResponse response,
                                              final RegisteredUser userObjectFromClient, final String newPassword,
                                              final Map<String, Map<String, Boolean>> userPreferenceObject) {
        try {
            RegisteredUserDTO savedUser = userManager.createUserObjectAndSession(request, response,
                    userObjectFromClient, newPassword);

            if (userPreferenceObject != null) {
                List<UserPreference> userPreferences = userPreferenceObjectToList(userPreferenceObject, savedUser.getId());
                userPreferenceManager.saveUserPreferences(userPreferences);
            }

            return Response.ok(savedUser).build();
        } catch (InvalidPasswordException e) {
            log.warn("Invalid password exception occurred during registration!");
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (FailedToHashPasswordException e) {
            log.error("Failed to hash password during user registration!");
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to set a password.").toResponse();
        } catch (MissingRequiredFieldException e) {
            log.warn("Missing field during update operation. ", e);
            return new SegueErrorResponse(Status.BAD_REQUEST, "You are missing a required field. "
                    + "Please make sure you have specified all mandatory fields in your response.").toResponse();
        } catch (DuplicateAccountException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "An account already exists with the e-mail address specified.").toResponse();
        } catch (SegueDatabaseException e) {
            String errorMsg = "Unable to set a password, due to an internal database error.";
            log.error(errorMsg, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, errorMsg).toResponse();
        } catch (AuthenticationProviderMappingException e) {
            log.warn("Unable to map to a known authenticator during registration. The provider is unknown!");
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to map to a known authenticator. The provider: is unknown").toResponse();
        } catch (EmailMustBeVerifiedException e) {
            log.warn("Someone attempted to register with an Isaac email address: " + userObjectFromClient.getEmail());
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You cannot register with an Isaac email address.").toResponse();
        }
    }


    /**
     * Convert user-provided preference maps to UserPreference lists.
     *     Contains Isaac classes which ideally should not be here . . .
     *
     * @param userPreferenceObject
     *            - the user-provided preference object
     * @param userId
     *            - the userId of the user
     * @return whether the preference is valid
     */
    private List<UserPreference> userPreferenceObjectToList(final Map<String, Map<String, Boolean>> userPreferenceObject, final long userId) {
        List<UserPreference> userPreferences = Lists.newArrayList();
        if (null == userPreferenceObject) {
            return userPreferences;
        }
        // FIXME: This entire method is horrible, but required to sanitise what is stored in the database . . .
        for (String preferenceType: userPreferenceObject.keySet()) {

            // Check if the given preference type is one we support:
            if (!EnumUtils.isValidEnum(IsaacUserPreferences.class, preferenceType)
                    && !EnumUtils.isValidEnum(SegueUserPreferences.class, preferenceType)) {
                log.warn("Unknown user preference type '" + preferenceType + "' provided. Skipping.");
                continue;
            }

            if (EnumUtils.isValidEnum(SegueUserPreferences.class, preferenceType)
                    && SegueUserPreferences.EMAIL_PREFERENCE.equals(SegueUserPreferences.valueOf(preferenceType))) {
                // This is an email preference, which is treated specially:
                for (String preferenceName : userPreferenceObject.get(preferenceType).keySet()) {
                    if (!EnumUtils.isValidEnum(EmailType.class, preferenceName) || !EmailType.valueOf(preferenceName).isValidEmailPreference()) {
                        log.warn("Invalid email preference name '" + preferenceName + "' provided for '"
                                + preferenceType + "'! Skipping.");
                        continue;
                    }
                    boolean preferenceValue = userPreferenceObject.get(preferenceType).get(preferenceName);
                    userPreferences.add(new UserPreference(userId, preferenceType, preferenceName, preferenceValue));
                }
            } else if (EnumUtils.isValidEnum(IsaacUserPreferences.class, preferenceType)) {
                // Isaac user preference names are configured in the config files:
                String acceptedPreferenceNamesProperty = getProperties().getProperty(preferenceType);
                if (null == acceptedPreferenceNamesProperty) {
                    log.error("Failed to find allowed user preferences names for '" + preferenceType + "'! Has it been configured?");
                    acceptedPreferenceNamesProperty = "";
                }
                List<String> acceptedPreferenceNames = Arrays.asList(acceptedPreferenceNamesProperty.split(","));
                for (String preferenceName : userPreferenceObject.get(preferenceType).keySet()) {
                    if (!acceptedPreferenceNames.contains(preferenceName)) {
                        log.warn("Invalid user preference name '" + preferenceName + "' provided for type '" + preferenceType + "'! Skipping.");
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
}
