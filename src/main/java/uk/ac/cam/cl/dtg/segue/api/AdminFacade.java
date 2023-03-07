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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.annotations.GZIP;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.EventBookingManager;
import uk.ac.cam.cl.dtg.segue.api.managers.ExternalAccountSynchronisationException;
import uk.ac.cam.cl.dtg.segue.api.managers.IExternalAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.SegueResourceMisuseException;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.monitors.IMisuseMonitor;
import uk.ac.cam.cl.dtg.segue.api.monitors.SegueMetrics;
import uk.ac.cam.cl.dtg.segue.api.monitors.UserSearchMisuseHandler;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.comm.EmailType;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.isaac.dos.AbstractUserPreferenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.UserPreference;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.isaac.dos.users.Role;
import uk.ac.cam.cl.dtg.isaac.dos.users.School;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserIdMergeDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryForAdminUsersDTO;
import uk.ac.cam.cl.dtg.segue.etl.GithubPushEventPayload;
import uk.ac.cam.cl.dtg.segue.scheduler.SegueJobService;
import uk.ac.cam.cl.dtg.segue.search.SegueSearchException;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.RequestIPExtractor;

import jakarta.annotation.Nullable;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Admin facade for segue.
 *
 * @author Stephen Cummins
 *
 */
@Path("/admin")
@Tag(name = "/admin")
public class AdminFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(AdminFacade.class);

    private final UserAccountManager userManager;
    private final GitContentManager contentManager;
    private final String contentIndex;

    private final StatisticsManager statsManager;

    private final SchoolListReader schoolReader;

    private final AbstractUserPreferenceManager userPreferenceManager;
    private final EventBookingManager eventBookingManager;
    private final IExternalAccountManager externalAccountManager;
    private final IMisuseMonitor misuseMonitor;
    private final SegueJobService segueJobService;

    /**
     * Create an instance of the administrators facade.
     *
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param userManager
     *            - The manager object responsible for users.
     * @param contentManager
     *            - The content manager used by the api.
     * @param logManager
     *            - So we can log events of interest.
     * @param statsManager
     *            - So we can report high level stats.
     * @param schoolReader
     *            - for looking up school information
     * @param eventBookingManager
     *            - for using the event booking system
     * @param misuseMonitor
     *            - misuse monitor.
     */
    @Inject
    public AdminFacade(final PropertiesLoader properties, final UserAccountManager userManager,
                       final GitContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex, final ILogManager logManager,
                       final StatisticsManager statsManager, final SchoolListReader schoolReader,
                       final AbstractUserPreferenceManager userPreferenceManager,
                       final EventBookingManager eventBookingManager, final SegueJobService segueJobService,
                       final IExternalAccountManager externalAccountManager, final IMisuseMonitor misuseMonitor) {
        super(properties, logManager);
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.statsManager = statsManager;
        this.schoolReader = schoolReader;
        this.userPreferenceManager = userPreferenceManager;
        this.eventBookingManager = eventBookingManager;
        this.externalAccountManager = externalAccountManager;
        this.misuseMonitor = misuseMonitor;
        this.segueJobService = segueJobService;
    }

    /**
     * Statistics endpoint.
     *
     * @param request
     *            - to determine access.
     * @return stats
     */
    @GET
    @Path("/stats/")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getStatistics(@Context final HttpServletRequest request) {
        try {
            if (!isUserStaff(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin to access this endpoint.")
                        .toResponse();
            }

            return Response.ok(statsManager.getGeneralStatistics())
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_FIVE_MINUTES, false)).build();
        } catch (SegueDatabaseException e) {
            log.error("Unable to load general statistics.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * This method will allow users to be mass-converted to a new role.
     *
     * @param request
     *            - to help determine access rights.
     * @param role
     *            - new role.
     * @param userIds
     *            - a list of user ids to change en-mass
     * @return Success shown by returning an ok response
     */
    @POST
    @Path("/users/change_role/{role}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public synchronized Response modifyUsersRole(@Context final HttpServletRequest request,
            @PathParam("role") final String role, final List<Long> userIds) {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdminOrEventManager(userManager, requestingUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be staff to access this endpoint.")
                        .toResponse();
            }

            Role requestedRole = Role.valueOf(role);

            if (userIds.contains(requestingUser.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "Aborted - you cannot modify your own role.")
                .toResponse();
            }

            // can't promote anyone to a role higher than yourself
            if (requestedRole.ordinal() >= requestingUser.getRole().ordinal()) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "Cannot change to role equal or higher than your own.").toResponse();
            }

            // fail fast - break if any of the users given already have the role they are being elevated to
            for (Long userid : userIds) {
                RegisteredUserDTO user = this.userManager.getUserDTOById(userid);

                if (null == user) {
                    throw new NoUserException("No user found with this ID.");
                }

                // if a user already has this role, abort
                if (user.getRole() != null && user.getRole() == requestedRole) {
                    return new SegueErrorResponse(Status.BAD_REQUEST,
                            "Aborted - cannot demote one or more users "
                                    + "who have roles equal or higher than new role").toResponse();
                }

                // if a user has a higher role than the requester, abort
                if (user.getRole() != null && user.getRole().ordinal() >= requestingUser.getRole().ordinal()) {
                    return new SegueErrorResponse(Status.FORBIDDEN,
                            "Aborted - cannot demote one or more users "
                                    + "who have roles equal or higher than you,").toResponse();
                }
            }

            for (Long userid : userIds) {
                RegisteredUserDTO user = this.userManager.getUserDTOById(userid);
                Role oldRole = user.getRole();
                this.userManager.updateUserRole(userid, requestedRole);
                log.info(String.format(
                        "ADMIN user %s has modified the role of %s [%s] to %s",
                        requestingUser.getEmail(), user.getEmail(), user.getId(), user.getRole()
                ));
                this.getLogManager().logEvent(requestingUser, request, SegueServerLogType.CHANGE_USER_ROLE,
                        ImmutableMap.of(USER_ID_FKEY_FIELDNAME, user.getId(),
                                        "oldRole", oldRole,
                                        "newRole", requestedRole));
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            log.error("NoUserException when attempting to demote users.", e);
            return new SegueErrorResponse(Status.BAD_REQUEST, "One or more users could not be found")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to change user role", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Could not save new role to the database").toResponse();
        }

        return Response.ok().build();
    }

    /**
     * This method will allow users' email verification status to be changed en-mass.
     *
     * @param request
     *            - to help determine access rights.
     * @param emailVerificationStatus
     *            - new emailVerificationStatus.
     * @param emails
     *            - a list of user emails that need to be changed
     * @param checkEmailsExistBeforeApplying
     *            - tells us whether to check whether all emails exist before applying
     * @return Success shown by returning an ok response
     */
    @POST
    @Path("/users/change_email_verification_status/{emailVerificationStatus}/{checkEmailsExistBeforeApplying}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public synchronized Response modifyUsersEmailVerificationStatus(
            @Context final HttpServletRequest request,
            @PathParam("emailVerificationStatus") final String emailVerificationStatus,
            @PathParam("checkEmailsExistBeforeApplying") final boolean checkEmailsExistBeforeApplying,
            final List<String> emails) {
        try {
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdminOrEventManager(userManager, requestingUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be staff to access this endpoint.")
                        .toResponse();
            }

            EmailVerificationStatus requestedEmailVerificationStatus = EmailVerificationStatus
                    .valueOf(emailVerificationStatus);

            if (emails.contains(requestingUser.getEmail())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Aborted - you cannot modify yourself.")
                        .toResponse();
            }


            if (checkEmailsExistBeforeApplying) {
                // fail fast - break if any of the users given already have the role they are being elevated to
                for (String email : emails) {
                    RegisteredUserDTO user = this.userManager.getUserDTOByEmail(email);

                    if (null == user) {
                        log.error(String.format("No user could be found with email (%s)", email));
                        throw new NoUserException("No user found with this email.");
                    }
                }
            }

            for (String email : emails) {
                this.userManager.updateUserEmailVerificationStatus(email, requestedEmailVerificationStatus);
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            log.error("NoUserException when attempting to change users verification status.", e);
            return new SegueErrorResponse(Status.BAD_REQUEST, "One or more users could not be found")
                    .toResponse();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Could not save new email verification status to the database").toResponse();
        }

        return Response.ok().build();
    }

    /**
     * This method allow user email verification statuses to be changed en-mass, authenticating with a token.
     *
     * @param request
     *            - to provide IP address information
     * @param providedAuthHeader
     *            - to provide the authentication token
     * @param emails
     *            - a list of user emails that need to be changed
     * @return Success shown by returning an ok response
     */
    @POST
    @Path("/users/change_email_verification_status/delivery_failed")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a list of possible account emails as delivery failed.",
            description = "This endpoint requires a Bearer token in the Authorization header and not a Segue cookie.")
    public synchronized Response setUsersEmailVerificationStatusFailed(
            @Context final HttpServletRequest request,
            @HeaderParam("Authorization") final String providedAuthHeader,
            final List<String> emails) {
        try {
            String endpointToken = this.getProperties().getProperty(Constants.EMAIL_VERIFICATION_ENDPOINT_TOKEN);
            if (null == endpointToken || endpointToken.isEmpty()) {
                log.error("Request attempted to set email delivery statuses using token, but no token configured!");
                return SegueErrorResponse.getNotImplementedResponse();
            }

            if (null == providedAuthHeader || providedAuthHeader.isEmpty()) {
                log.warn("Request attempted to set email delivery statuses without a token!");
                return SegueErrorResponse.getBadRequestResponse("Malformed Request");
            }

            String remoteIpAddress = RequestIPExtractor.getClientIpAddr(request);
            String expectedHeader = "Bearer " + endpointToken;
            if (!expectedHeader.equals(providedAuthHeader)) {
                log.warn(String.format("Request from (%s) attempted to set email delivery statuses with invalid token!", remoteIpAddress));
                return new SegueErrorResponse(Status.UNAUTHORIZED, "Unauthorised").toResponse();
            }

            for (String email : emails) {
                this.userManager.updateUserEmailVerificationStatus(email, EmailVerificationStatus.DELIVERY_FAILED);
            }
            log.info(String.format("Request from (%s) updated the status of %s emails to DELIVERY_FAILED.", remoteIpAddress, emails.size()));

        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Could not save new email verification status to the database").toResponse();
        }

        return Response.ok().build();
    }

    /* This method will allow users' email verification status to be changed en-mass.
     *
     * @param request
     *            - to help determine access rights.
     * @param webhookPayload
     *            - a list of user webhookPayload that need to be changed
     * @return Success shown by returning an ok response
     */
    @POST
    @Path("/users/delivery_failed_notification")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a possible account email as delivery failed.",
            description = "This endpoint requires a valid HMAC from MailGun.")
    public Response notifySingleDeliveryFailure(@Context final HttpServletRequest request,
                                                final Map<String, Object> webhookPayload) {

        String trustedSigningKey = getProperties().getProperty(MAILGUN_SECRET_KEY);
        if (null == trustedSigningKey || trustedSigningKey.isEmpty()) {
            return SegueErrorResponse.getNotImplementedResponse();
        }
        try {
            final String hmacAlgorithm = "HmacSHA256";
            Map<String, String> signatureJson = (Map<String, String>) webhookPayload.get("signature");
            String dataToSign = signatureJson.get("timestamp").concat(signatureJson.get("token"));
            SecretKeySpec signingKey = new SecretKeySpec(trustedSigningKey.getBytes(), hmacAlgorithm);
            Mac mac = Mac.getInstance(hmacAlgorithm);
            mac.init(signingKey);
            byte[] rawHmac = mac.doFinal(dataToSign.getBytes());
            String result = Hex.encodeHexString(rawHmac);
            if (!result.equals(signatureJson.get("signature"))) {
                return new SegueErrorResponse(Status.FORBIDDEN, "Validation failed.").toResponse();
            }
            Map <String, Object> eventDetails = (Map<String, Object>) webhookPayload.get("event-data");

            String recipientEmail = (String) eventDetails.get("recipient");

            this.userManager.updateUserEmailVerificationStatus(recipientEmail, EmailVerificationStatus.DELIVERY_FAILED);

            String remoteIpAddress = RequestIPExtractor.getClientIpAddr(request);
            log.info(String.format("Request from (%s) updated the status of 1 email to DELIVERY_FAILED.", remoteIpAddress));

            return Response.ok().build();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SegueDatabaseException | ClassCastException | NullPointerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to process request.").toResponse();
        }
    }

    /** This method will allow users' email verification status to be changed en-mass.
     *
     * @param request
     *            - to help determine access rights.
     * @param providerToken
     *            - a path parameter to restrict access
     * @param eventDetailsList
     *            - a list of objects in the MailJet JSON format
     * @return Success shown by returning an ok response
     */
    @POST
    @Path("/users/delivery_failed_notification/{providerToken}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update a list of possible account emails as delivery failed.",
            description = "This endpoint expects the body to be in MailJet format.")
    public Response notifyExternalDeliveryFailure(@Context final HttpServletRequest request,
                                                @PathParam("providerToken") final String providerToken,
                                                final List<Map<String, Object>> eventDetailsList) {
        try {
            final String expectedProviderToken = getProperties().getProperty(MAILJET_WEBHOOK_TOKEN);
            if (null == expectedProviderToken || expectedProviderToken.isEmpty()) {
                return SegueErrorResponse.getNotImplementedResponse();
            }
            if (!expectedProviderToken.equals(providerToken)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            for (Map<String, Object> eventDetails: eventDetailsList) {
                String recipientEmail = (String) eventDetails.get("email");
                Object permanentFailureObject = eventDetails.get("hard_bounce");
                String errorMessage = (String) eventDetails.get("error");

                // Assume a hard failure unless told otherwise or message triggered by duplication issue:
                boolean permanentFailure = !"duplicate in campaign".equals(errorMessage);
                // The "hard_bounce" parameter might be null, a String, or a Boolean value:
                if (permanentFailureObject instanceof Boolean) {
                    permanentFailure = (boolean) permanentFailureObject;
                } else if (permanentFailureObject instanceof String) {
                    permanentFailure = Boolean.parseBoolean((String) permanentFailureObject);
                }
                // Update delivery failure status if necessary:
                if (recipientEmail != null && !recipientEmail.isEmpty() && permanentFailure) {
                    this.userManager.updateUserEmailVerificationStatus(recipientEmail, EmailVerificationStatus.DELIVERY_FAILED);
                }
            }
            String remoteIpAddress = RequestIPExtractor.getClientIpAddr(request);
            log.info(String.format("Request from (%s) updated the status of %s emails to DELIVERY_FAILED.", remoteIpAddress, eventDetailsList.size()));
            return Response.ok().build();
        } catch (SegueDatabaseException | ClassCastException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to process request.").toResponse();
        }
    }

    /** This method will allow users' email preference status to be changed en-mass.
     *
     * @param request
     *            - to help determine access rights.
     * @param providerToken
     *            - a path parameter to restrict access
     * @param eventDetailsList
     *            - the unsubscription event list, currently in MailJet format
     * @return Success shown by returning an ok response
     */
    @POST
    @Path("/users/unsubscription_notification/{providerToken}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Unsubscribe a list of possible account emails from an email type.",
            description = "This endpoint expects the body to be in MailJet format.")
    public Response notifyExternalUnsubscriptionEvent(@Context final HttpServletRequest request,
                                                @PathParam("providerToken") final String providerToken,
                                                final List<Map<String, Object>> eventDetailsList) {
        try {
            final String expectedProviderToken = getProperties().getProperty(MAILJET_WEBHOOK_TOKEN);
            if (null == expectedProviderToken || expectedProviderToken.isEmpty()) {
                return SegueErrorResponse.getNotImplementedResponse();
            }
            if (!expectedProviderToken.equals(providerToken)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            List<UserPreference> userPreferencesToUpdate = Lists.newArrayList();

            for (Map<String, Object> eventDetails: eventDetailsList) {
                String recipientEmail = (String) eventDetails.get("email");
                // TODO: this is very MailJet specific:
                Integer mailjetListId = (Integer) eventDetails.get("mj_list_id");
                EmailType unsubscribedEmailType = EmailType.NEWS_AND_UPDATES;
                if (null != mailjetListId && getProperties().getProperty(MAILJET_NEWS_LIST_ID).equals(mailjetListId.toString())) {
                    unsubscribedEmailType = EmailType.NEWS_AND_UPDATES;
                } else if (null != mailjetListId && getProperties().getProperty(MAILJET_EVENTS_LIST_ID).equals(mailjetListId.toString())) {
                    unsubscribedEmailType = EmailType.EVENTS;
                } else {
                    log.warn(String.format("User with email (%s) attempted to unsubscribe from unrecognised list (%s)!", recipientEmail, mailjetListId));
                }
                // Find and unsubscribe user:
                if (recipientEmail != null && !recipientEmail.isEmpty()) {
                    try {
                        RegisteredUserDTO user = userManager.getUserDTOByEmail(recipientEmail);
                        UserPreference preferenceToSave = new UserPreference(user.getId(), SegueUserPreferences.EMAIL_PREFERENCE.name(), unsubscribedEmailType.name(), false);
                        userPreferencesToUpdate.add(preferenceToSave);
                    } catch (NoUserException e) {
                        log.warn(String.format("User with email (%s) attempted to unsubscribe, but no Isaac account found!", recipientEmail));
                    }
                }
            }
            userPreferenceManager.saveUserPreferences(userPreferencesToUpdate);
            String remoteIpAddress = RequestIPExtractor.getClientIpAddr(request);
            log.info(String.format("Request from (%s) unsubscribed %s emails from NEWS_AND_UPDATES emails.", remoteIpAddress, userPreferencesToUpdate.size()));
            return Response.ok().build();
        } catch (SegueDatabaseException | ClassCastException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to process request.").toResponse();
        }
    }

    /**
     * This method will delete all cached data from the CMS and any search indices.
     *
     * @param request
     *            - containing user session information.
     *
     * @return the latest version id that will be cached if content is requested.
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/reload_properties")
    public synchronized Response reloadProperties(@Context final HttpServletRequest request) {
        try {
            if (isUserAnAdmin(userManager, request)) {
                log.info("Triggering properties reload ...");
                this.getProperties().triggerPropertiesRefresh();

                ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>().put("result",
                        "success").build();

                return Response.ok(response).build();
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
                        .toResponse();
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (IOException e) {
            log.error("Unable to trigger property refresh", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to trigger properties refresh", e)
                    .toResponse();
        }
    }



    /**
     * Rest end point to allow content editors to see the content which failed to import into segue.
     *
     * @param request
     *            - to identify if the user is authorised.
     * @param requestForCaching
     *            - to determine if the content is still fresh..
     * @return a content object, such that the content object has children. The children represent each source file in
     *         error and the grand children represent each error.
     */
    @SuppressWarnings("unchecked")
    @GET
    @Path("/content_problems")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getContentProblems(@Context final HttpServletRequest request,
            @Context final Request requestForCaching) {
        Map<Content, List<String>> problemMap = this.contentManager.getProblemMap(
        );

        if (this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(EnvironmentType.PROD.name())) {
            try {
                if (!isUserStaff(userManager, request)) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin to access this endpoint.")
                            .toResponse();
                }
            } catch (NoUserLoggedInException e) {
                return SegueErrorResponse.getNotLoggedInResponse();
            }
        }

        // Calculate the ETag
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "");

        Response cachedResponse = generateCachedResponse(requestForCaching, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        if (null == problemMap) {
            return Response.ok(Maps.newHashMap()).build();
        }

        // build up a content object to return.
        int errors = 0;
        int failures = 0;
        Builder<String, Object> responseBuilder = ImmutableMap.builder();
        List<Map<String, Object>> errorList = Lists.newArrayList();

        Map<String, Map<String, Object>> lookupMap = Maps.newHashMap();

        // go through each errored content and list of errors
        for (Map.Entry<Content, List<String>> pair : problemMap.entrySet()) {
            Map<String, Object> errorRecord = Maps.newHashMap();

            Content partialContentWithErrors =  pair.getKey();

            errorRecord.put("partialContent", partialContentWithErrors);

            errorRecord.put("successfulIngest", false);
            failures++;

            if (partialContentWithErrors.getId() != null) {
                try {

                    boolean success = this.contentManager.getContentById(partialContentWithErrors.getId(),
                            true) != null;

                    errorRecord.put("successfulIngest", success);
                    if (success) {
                        failures--;
                    }

                } catch (ContentManagerException e) {
                    e.printStackTrace();
                }
            }

            List<String> listOfErrors = Lists.newArrayList();
            for (String s : pair.getValue()) {
                listOfErrors.add(s);
                // special case when duplicate ids allow one in.
                if (s.toLowerCase().contains("index failure") && errorRecord.get("successfulIngest").equals(true)) {
                    errorRecord.put("successfulIngest", false);
                    failures++;
                }
                errors++;
            }

            errorRecord.put("listOfErrors", listOfErrors);
            // we only want one error record per canonical path so batch them together if we have seen it before.
            if (lookupMap.containsKey(partialContentWithErrors.getCanonicalSourceFile())) {
                Map<String, Object> existingErrorRecord
                    = lookupMap.get(partialContentWithErrors.getCanonicalSourceFile());

                if (existingErrorRecord.get("successfulIngest").equals(false)
                        || errorRecord.get("successfulIngest").equals(false)) {
                    existingErrorRecord.put("successfulIngest", false);
                }

                ((List<String>) existingErrorRecord.get("listOfErrors")).addAll(listOfErrors);
            } else {
                errorList.add(errorRecord);
                lookupMap.put(partialContentWithErrors.getCanonicalSourceFile(), errorRecord);
            }
        }

        responseBuilder.put("brokenFiles", lookupMap.keySet().size());
        responseBuilder.put("totalErrors", errors);
        responseBuilder.put("errorsList", errorList);
        responseBuilder.put("failedFiles", failures);
        responseBuilder.put("currentLiveVersion", this.contentManager.getCurrentContentSHA());

        return Response.ok(responseBuilder.build())
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_MINUTE, false)).tag(etag)
                .build();
    }

    /**
     * List users by id or email.
     *
     * @param httpServletRequest
     *            - for checking permissions
     * @param request
     *            - for caching
     * @param userId
     *            - if searching by id
     * @param email
     *            - if searching by e-mail
     * @param familyName
     *            - if searching by familyName
     * @param role
     *            - if searching by role
     * @param schoolOther
     *            - if searching by school other field.
     * @param schoolURN
     *            - if searching by school by the URN.
     * @param subjectOfInterest
     *            - if searching by subject interest
     * @return a userDTO or a segue error response
     */
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response findUsers(@Context final HttpServletRequest httpServletRequest, @Context final Request request,
            @QueryParam("id") final Long userId, @QueryParam("email") @Nullable final String email,
            @QueryParam("familyName") @Nullable final String familyName, @QueryParam("role") @Nullable final Role role,
            @QueryParam("schoolOther") @Nullable final String schoolOther,
            @QueryParam("schoolURN") @Nullable final String schoolURN,
            @QueryParam("subjectOfInterest") @Nullable final String subjectOfInterest) {

        RegisteredUserDTO currentUser;
        try {
            currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdminOrEventManager(userManager, currentUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not authorised to access this function.")
                        .toResponse();
            }

            misuseMonitor.notifyEvent(currentUser.getId().toString(), UserSearchMisuseHandler.class.getSimpleName());

            if (!isUserAnAdmin(userManager, currentUser)
                    && (null == familyName || familyName.isEmpty())
                    && (null == schoolOther || schoolOther.isEmpty())
                    && (null == email || email.isEmpty())
                    && (null == schoolURN || schoolURN.isEmpty())) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You do not have permission to do wildcard searches.")
                        .toResponse();

            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueResourceMisuseException e) {
            return SegueErrorResponse
                    .getRateThrottledResponse("You have exceeded the number of requests allowed for this endpoint");
        }

        try {
            RegisteredUserDTO userPrototype = new RegisteredUserDTO();
            if (null != userId) {
                userPrototype.setId(userId);
            }

            if (null != email && !email.isEmpty()) {
                if (currentUser.getRole().equals(Role.EVENT_MANAGER) && email.replaceAll("[^A-z0-9]", "").length() < 4) {
                    return new SegueErrorResponse(Status.FORBIDDEN,
                            "You do not have permission to do wildcard searches with less than 4 characters.")
                            .toResponse();
                }
                userPrototype.setEmail(email);
            }

            if (null != familyName && !familyName.isEmpty()) {
                // Event managers aren't allowed to do short wildcard searches, but need surnames less than 4 chars too.
                if (currentUser.getRole().equals(Role.EVENT_MANAGER) && (familyName.replaceAll("[^A-z]", "").length() < 4)
                        && (familyName.length() != familyName.replaceAll("[^A-z]", "").length())) {
                    return new SegueErrorResponse(Status.FORBIDDEN, "You do not have permission to do wildcard searches with less than 4 characters.")
                            .toResponse();
                }
                userPrototype.setFamilyName(familyName);
            }

            if (null != role) {
                userPrototype.setRole(role);
            }

            if (null != schoolOther) {
                userPrototype.setSchoolOther(schoolOther);
            }

            if (null != schoolURN) {
                userPrototype.setSchoolId(schoolURN);
            }

            List<RegisteredUserDTO> foundUsers;

            // If a unique email address (without wildcards) provided, look up using this email immediately:
            if (null != email && !email.isEmpty() && !(email.contains("%") || email.contains("_"))) {
                try {
                    foundUsers = Collections.singletonList(this.userManager.getUserDTOByEmail(email));
                } catch (NoUserException e) {
                    foundUsers = Collections.emptyList();
                }
            } else {
                foundUsers = this.userManager.findUsers(userPrototype);
            }
            Map<Long, RegisteredUserDTO> userMapById = foundUsers.parallelStream().collect(Collectors.toMap(RegisteredUserDTO::getId, Function.identity()));

            // FIXME - this shouldn't really be in a segue class!
            if (subjectOfInterest != null && !subjectOfInterest.isEmpty()) {
                List<RegisteredUserDTO> subjectFilteredUsers = new ArrayList<>();
                Map<Long, List<UserPreference>> userPreferences = userPreferenceManager.getUserPreferences(IsaacUserPreferences.SUBJECT_INTEREST.name(), foundUsers);

                for (RegisteredUserDTO userToFilter: foundUsers) {
                    if (userPreferences.containsKey(userToFilter.getId())) {
                        for (UserPreference pref : userPreferences.get(userToFilter.getId())) {
                            if (pref.getPreferenceName().equals(subjectOfInterest) && pref.getPreferenceValue()) {
                                subjectFilteredUsers.add(userToFilter);
                            }
                        }
                    }
                }
                foundUsers = subjectFilteredUsers;
            }

            // Calculate the ETag
            EntityTag etag = new EntityTag(foundUsers.size() + foundUsers.toString().hashCode()
                    + userPrototype.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            int searchResultsLimit;
            try {
                searchResultsLimit = Integer.parseInt(this.getProperties().getProperty(Constants.SEARCH_RESULTS_HARD_LIMIT));
            } catch(NumberFormatException e) {
                searchResultsLimit = 2000; // Hard-coded, but only as a fail-safe.
            }

            if (foundUsers.size() > searchResultsLimit) {
                log.warn(String.format("%s user (%s) search returned %d results, limiting to " + searchResultsLimit + ".",
                        currentUser.getRole(), currentUser.getEmail(), foundUsers.size()));
                foundUsers = foundUsers.subList(0, searchResultsLimit);
            }
            log.info(String.format("%s user (%s) did a search across all users based on user prototype {%s}",
                    currentUser.getRole(), currentUser.getEmail(), userPrototype));

            return Response.ok(this.userManager.convertToDetailedUserSummaryObjectList(foundUsers, UserSummaryForAdminUsersDTO.class))
                    .tag(etag)
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                    .build();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.").toResponse();
        }
    }

    /**
     * Get a user by id or email.
     *
     * @param httpServletRequest
     *            - for checking permissions
     * @param userId
     *            - if searching by id
     * @return a userDTO or a segue error response
     */
    @GET
    @Path("/users/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response findUsers(@Context final HttpServletRequest httpServletRequest,
            @PathParam("user_id") final Long userId) {

        RegisteredUserDTO currentUser;
        try {
            currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdminOrEventManager(userManager, currentUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        try {
            log.info(String.format("%s user (%s) did a user id lookup based on user id {%s}", currentUser.getRole(),
                    currentUser.getEmail(), userId));

            return Response.ok(this.userManager.getUserDTOById(userId))
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.").toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the user with the requested id: "
                    + userId).toResponse();
        }
    }

    /**
     * Delete all user data for a particular user account.
     *
     * @param httpServletRequest
     *            - for checking permissions
     * @param userId
     *            - the id of the user to delete.
     * @return a userDTO or a segue error response
     */
    @DELETE
    @Path("/users/{user_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUserAccount(@Context final HttpServletRequest httpServletRequest,
            @PathParam("user_id") final Long userId) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

            if (currentlyLoggedInUser.getId().equals(userId)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are not allowed to delete yourself.")
                        .toResponse();
            }

            RegisteredUserDTO userToDelete = this.userManager.getUserDTOById(userId);

            this.userManager.deleteUserAccount(userToDelete);
            this.eventBookingManager.deleteUsersAdditionalInformationBooking(userToDelete);
            getLogManager().logEvent(currentlyLoggedInUser, httpServletRequest, SegueServerLogType.DELETE_USER_ACCOUNT,
                    ImmutableMap.of(USER_ID_FKEY_FIELDNAME, userToDelete.getId()));

            log.info("Admin User: " + currentlyLoggedInUser.getEmail() + " has just deleted the user account with id: "
                    + userId);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Unable to delete account", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.").toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the user with the requested id: "
                    + userId).toResponse();
        }
    }

    /**
     * Merge a source user into a target user. The source is deleted afterwards.
     *
     * @param httpServletRequest
     *            - for checking permissions
     * @param userIdMergeDTO
     *            - the source and target ids to merge
     * @return no content or a segue error response
     */
    @POST
    @Path("/users/merge")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response mergeUserAccounts(@Context final HttpServletRequest httpServletRequest,
                                      final UserIdMergeDTO userIdMergeDTO) {
        try {
            RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdmin(userManager, currentlyLoggedInUser)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

            if (currentlyLoggedInUser.getId().equals(userIdMergeDTO.getSourceId())) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are not allowed to be the merge source.")
                        .toResponse();
            }

            RegisteredUserDTO targetUser = this.userManager.getUserDTOById(userIdMergeDTO.getTargetId());
            RegisteredUserDTO sourceUser = this.userManager.getUserDTOById(userIdMergeDTO.getSourceId());

            this.userManager.mergeUserAccounts(targetUser, sourceUser);
            getLogManager().logEvent(currentlyLoggedInUser, httpServletRequest, SegueServerLogType.ADMIN_MERGE_USER,
                    ImmutableMap.of(USER_ID_FKEY_FIELDNAME, targetUser.getId(), OLD_USER_ID_FKEY_FIELDNAME, sourceUser.getId()));

            log.info("Admin User: " + currentlyLoggedInUser.getEmail() + " has just merged the target user account with id: " + userIdMergeDTO.getTargetId() +
                    " with the source user account with id: " + userIdMergeDTO.getSourceId());

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Unable to merge accounts", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.").toResponse();
        } catch (NoUserException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the users with the requested ids: "
                    + userIdMergeDTO.getTargetId() + ", " + userIdMergeDTO.getSourceId()).toResponse();
        }
    }

    /**
     * This method will allow the live version served by the site to be changed.
     *
     * @param request
     *            - to help determine access rights.
     * @param version
     *            - version to use as updated version of content store.
     * @return Success shown by returning the new liveSHA or failed message "Invalid version selected".
     */
    @POST
    @Path("/live_version/{version}")
    @Produces(MediaType.APPLICATION_JSON)
    public synchronized Response changeLiveVersion(@Context final HttpServletRequest request,
                                                   @PathParam("version") final String version) {

        try {
            RegisteredUserDTO currentUser = userManager.getCurrentRegisteredUser(request);
            if (isUserAnAdmin(userManager, currentUser)) {

                String oldLiveVersion = contentManager.getCurrentContentSHA();

                HttpClient httpClient = new DefaultHttpClient();

                HttpPost httpPost = new HttpPost("http://" + getProperties().getProperty("ETL_HOSTNAME") + ":"
                        + getProperties().getProperty("ETL_PORT") + "/isaac-api/api/etl/set_version_alias/"
                        + this.contentIndex + "/" + version);

                httpPost.addHeader("Content-Type", "application/json");

                HttpResponse httpResponse = httpClient.execute(httpPost);

                HttpEntity e = httpResponse.getEntity();

                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    log.info(currentUser.getEmail() + " changed live version from " + oldLiveVersion + " to " + version + ".");
                    return Response.ok().build();
                } else {
                    SegueErrorResponse r = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, IOUtils.toString(e.getContent()));
                    r.setBypassGenericSiteErrorPage(true);
                    return r.toResponse();
                }

            } else {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (Exception e) {
            log.error("Exception during version change.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
        }
    }

    /**
     * This method will reset the misuse monitor for the specified event and agent identifier.
     *
     * @param request
     *            - to help determine access rights.
     * @param details
     *            - a map containing: the misuse monitor eventLabel (i.e. what type of misuse monitor),
     *              and the misuse monitor agentIdentifier (i.e. which user to reset the count for)
     * @return Confirmation of success, or error message on incorrect role or incorrect details argument.
     */
    @POST
    @Path("/reset_misuse_monitor")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Reset a misuse monitor counter to zero.")
    public Response resetMisuseMonitor(@Context final HttpServletRequest request, final Map<String, String> details) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdmin(userManager, user)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }
            if (!details.containsKey("eventLabel") || !details.containsKey("agentIdentifier")) {
                return SegueErrorResponse.getBadRequestResponse("Request body should contain the keys 'eventLabel' and 'agentIdentifier'.");
            }
            String agentIdentifier = details.get("agentIdentifier");
            String eventLabel = details.get("eventLabel");
            misuseMonitor.resetMisuseCount(agentIdentifier, eventLabel);
            log.info(String.format("Admin user (%s) reset misuse monitor '%s' for agent id (%s)!", user.getEmail(),
                    eventLabel, agentIdentifier));
            return Response.ok().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Returns some metrics relating to the running Java API process.
     * @deprecated use Graphana to monitor these values instead of calling the endpoint.
     */
    @Deprecated
    @GET
    @Path("/diagnostics")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getDiagnostics(@Context final Request request, @Context final HttpServletRequest httpServletRequest) {

        try {

            if (isUserAnAdmin(userManager, httpServletRequest)) {

                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> diagnosticReport = Maps.newHashMap();
                Map<String, Object> websocketReport = Maps.newHashMap();
                Map<String, Object> runtimeReport = Maps.newHashMap();

                websocketReport.put("currentWebsocketsOpen", SegueMetrics.CURRENT_OPEN_WEBSOCKETS.get());
                websocketReport.put("usersCurrent", SegueMetrics.CURRENT_WEBSOCKET_USERS.get());
                websocketReport.put("totalWebsocketsOpened", SegueMetrics.WEBSOCKETS_OPENED_SUCCESSFULLY.get());
                websocketReport.put("totalWebsocketsClosed", SegueMetrics.WEBSOCKETS_CLOSED.get());
                diagnosticReport.put("websockets", websocketReport);

                // runtime reporting
                runtimeReport.put("processors", Runtime.getRuntime().availableProcessors());
                runtimeReport.put("memoryFree", Runtime.getRuntime().freeMemory());
                runtimeReport.put("memoryMax", Runtime.getRuntime().maxMemory());
                runtimeReport.put("memoryTotal", Runtime.getRuntime().totalMemory());
                runtimeReport.put("threadCount", Thread.activeCount());

                diagnosticReport.put("runtime", runtimeReport);

                // other reporting
                diagnosticReport.put("numAnonymousUsers", userManager.getNumberOfAnonymousUsers());

                return Response.ok(diagnosticReport).build();

            } else {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up number of anonymous users.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     *  Manually trigger a sync for testing or debugging purposes. Minimal success or failure reporting.
     */
    @POST
    @Path("/sync_external_accounts")
    @Operation(summary = "Trigger an update for external providers where account details have changed.")
    public Response syncExternalAccounts(@Context final HttpServletRequest httpServletRequest) {
        //TODO - automate this with Quartz, then review if this is still necessary?
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdmin(userManager, user)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            externalAccountManager.synchroniseChangedUsers();

            return Response.ok().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ExternalAccountSynchronisationException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Fatal error while attempting to synchronise users!", e).toResponse();
        }
    }

    @POST
    @Path("/start_quartz")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Start the Quartz Job Scheduler service if not already started.")
    public Response startQuartzJobService(@Context final HttpServletRequest request) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdmin(userManager, user)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }
            if (!segueJobService.isStarted()) {
                segueJobService.initialiseService();
                log.info(String.format("Admin user (%s) started Quartz scheduler successfully.", user.getEmail()));
                return Response.ok(ImmutableMap.of("status", "Started successfully!")).build();
            } else {
                return Response.ok(ImmutableMap.of("status", "Already running.")).build();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SchedulerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, e.getMessage()).toResponse();
        }
    }

    @GET
    @Path("/misuse_stats")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get a summary of the site misuse statistics.")
    public Response getUserMisuseStatistics(@Context final HttpServletRequest request, @QueryParam("limit") final Long limit) {
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            if (!isUserAnAdmin(userManager, user)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }
            return Response.ok(misuseMonitor.getMisuseStatistics(Objects.requireNonNullElse(limit, DEFAULT_MISUSE_STATISTICS_LIMIT))).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }
}
