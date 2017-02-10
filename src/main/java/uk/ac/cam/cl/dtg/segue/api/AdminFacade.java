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

import com.google.common.collect.Lists;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;

import com.opencsv.CSVWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringWriter;
import java.io.IOException;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.ArrayList;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.annotations.GZIP;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LocationManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.SchoolListReader;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.users.EmailVerificationStatus;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dos.users.School;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.etl.GithubPushEventPayload;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;
import uk.ac.cam.cl.dtg.util.locations.PostCodeRadius;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Admin facade for segue.
 * 
 * @author Stephen Cummins
 * 
 */
@Path("/admin")
@Api(value = "/admin")
public class AdminFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(AdminFacade.class);

    private final UserAccountManager userManager;
    private final IContentManager contentManager;
    private final String contentIndex;

    private final StatisticsManager statsManager;

    private final LocationManager locationManager;

    private final SchoolListReader schoolReader;

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
     * @param locationManager
     *            - for geocoding if we need it.
     * @param schoolReader
     *            - for looking up school information
     */
    @Inject
    public AdminFacade(final PropertiesLoader properties, final UserAccountManager userManager,
                       final IContentManager contentManager, @Named(CONTENT_INDEX) final String contentIndex, final ILogManager logManager,
                       final StatisticsManager statsManager, final LocationManager locationManager,
                       final SchoolListReader schoolReader) {
        super(properties, logManager);
        this.userManager = userManager;
        this.contentManager = contentManager;
        this.contentIndex = contentIndex;
        this.statsManager = statsManager;
        this.locationManager = locationManager;
        this.schoolReader = schoolReader;
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
            if (!isUserStaff(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin to access this endpoint.")
                        .toResponse();
            }
            
            return Response.ok(statsManager.outputGeneralStatistics())
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_FIVE_MINUTES, false)).build();
        } catch (SegueDatabaseException e) {
            log.error("Unable to load general statistics.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Locations stats.
     * 
     * @param request
     *            - to determine access.
     * @param requestForCaching
     *            - to speed up access.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @return stats
     */
    @GET
    @Path("/stats/users/last_locations")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getLastLocations(@Context final HttpServletRequest request,
            @Context final Request requestForCaching, @QueryParam("from_date") final Long fromDate,
                                     @QueryParam("to_date") final Long toDate) {
        try {
            if (!isUserStaff(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be a staff member to access this endpoint.")
                        .toResponse();
            }
            if (null == fromDate || null == toDate) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "You must specify the from_date and to_date you are interested in.").toResponse();
            }
            if (toDate < fromDate) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "The from_date must be before the to_date.").toResponse();
            }

            Collection<Location> locationInformation = statsManager.getLocationInformation(new Date(fromDate), new Date(toDate));

            return Response.ok(locationInformation).cacheControl(getCacheControl(NUMBER_SECONDS_IN_FIVE_MINUTES, false))
                    .build();
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Statistics endpoint.
     * 
     * @param request
     *            - to determine access.
     * @param requestForCache
     *            - to determine caching.
     * @return stats
     */
    @GET
    @Path("/stats/schools")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getSchoolsStatistics(@Context final HttpServletRequest request,
            @Context final Request requestForCache) {
        try {
            if (!isUserStaff(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            List<Map<String, Object>> schoolStatistics = statsManager.getSchoolStatistics();

            // Calculate the ETag
            EntityTag etag = new EntityTag(schoolStatistics.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(requestForCache, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(schoolStatistics).tag(etag)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_FIVE_MINUTES, false))
                    .build();
        } catch (UnableToIndexSchoolsException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable To Index SchoolIndexer Exception in admin facade", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e1) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error during user lookup")
                    .toResponse();
        }
    }

    /**
     * Get user last seen information map.
     * 
     * @param request
     *            - to determine access.
     * @return stats
     */
    @GET
    @Path("/stats/users/last_access")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getUserLastAccessInformation(@Context final HttpServletRequest request) {
        try {
            if (!isUserAnAdmin(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            return Response.ok(statsManager.getLastSeenUserMap()).build();
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
            if (!isUserAnAdminOrEventManager(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be staff to access this endpoint.")
                        .toResponse();
            }

            Role requestedRole = Role.valueOf(role);
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);
            
            if (userIds.contains(requestingUser.getId())) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "Aborted - you cannoted modify your own role.")
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
                    throw new NoUserException();
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
                this.userManager.updateUserRole(userid, requestedRole);
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            log.error("NoUserException when attempting to demote users.", e);
            return new SegueErrorResponse(Status.BAD_REQUEST, "One or more users could not be found")
                    .toResponse();
        } catch (SegueDatabaseException e) {
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
            if (!isUserAnAdminOrEventManager(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be staff to access this endpoint.")
                        .toResponse();
            }

            EmailVerificationStatus requestedEmailVerificationStatus = EmailVerificationStatus
                    .valueOf(emailVerificationStatus);
            RegisteredUserDTO requestingUser = userManager.getCurrentRegisteredUser(request);

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
                        throw new NoUserException();
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
            if (isUserAnAdmin(request)) {
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
                this.contentIndex);

        if (this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT).equals(EnvironmentType.PROD.name())) {
            try {
                if (!isUserStaff(request)) {
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
                    
                    boolean success = this.contentManager.getContentById(
                            this.contentIndex,
                            partialContentWithErrors.getId()) != null;
                    
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
     * @param postcode
     *            - if searching by postcode.
     * @param schoolURN
     *            - if searching by school by the URN.
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
            @QueryParam("postcode") @Nullable final String postcode,
            @QueryParam("postcodeRadius") @Nullable final String postcodeRadius,
            @QueryParam("schoolURN") @Nullable final String schoolURN) {

        RegisteredUserDTO currentUser;
        try {
            currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdminOrEventManager(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You are not authorised to access this function.")
                        .toResponse();
            }
            
            if (!currentUser.getRole().equals(Role.ADMIN)
                    && (null != familyName) && familyName.isEmpty() && (null == schoolOther) && (null != email)
                    && email.isEmpty() && (null == schoolURN) && (null == postcode)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You do not have permission to do wildcard searches.")
                        .toResponse();

            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        try {
            RegisteredUserDTO userPrototype = new RegisteredUserDTO();
            if (null != userId) {
                userPrototype.setId(userId);
            }

            if (null != email && !email.isEmpty()) {
                if (currentUser.getRole().equals(Role.EVENT_MANAGER) && email.replaceAll("[^A-z]", "").length() < 4) {
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

            List<RegisteredUserDTO> findUsers;

            // If a unique email address (without wildcards) provided, look up using this email immediately:
            if (null != email && !email.isEmpty() && !(email.contains("%") || email.contains("_"))) {
                try {
                    findUsers = Collections.singletonList(this.userManager.getUserDTOByEmail(email));
                } catch (NoUserException e) {
                    findUsers = Collections.emptyList();
                }
            } else {
                findUsers = this.userManager.findUsers(userPrototype);
            }

            // if postcode is set, filter found users
            if (null != postcode) {
                try {
                    Map<String, List<Long>> postCodeAndUserIds = Maps.newHashMap();
                    for (RegisteredUserDTO userDTO : findUsers) {
                        if (userDTO.getSchoolId() != null) {
                            School school = this.schoolReader.findSchoolById(userDTO.getSchoolId());
                            if (school != null) {
                                String schoolPostCode = school.getPostcode();
                                List<Long> ids;
                                if (postCodeAndUserIds.containsKey(schoolPostCode)) {
                                    ids = postCodeAndUserIds.get(schoolPostCode);
                                } else {
                                    ids = Lists.newArrayList();
                                }
                                ids.add(userDTO.getId());
                                postCodeAndUserIds.put(schoolPostCode, ids);
                            }
                        }
                    }

                    PostCodeRadius radius = PostCodeRadius.valueOf(postcodeRadius);

                    List<Long> userIdsWithinRadius = locationManager.getUsersWithinPostCodeDistanceOf(
                            postCodeAndUserIds, postcode, radius);

                    // Make sure the list returned is users who have schools in our postcode radius
                    List<RegisteredUserDTO> nearbyUsers = new ArrayList<>();
                    for (Long id : userIdsWithinRadius) {
                        RegisteredUserDTO user = this.userManager.getUserDTOById(id);
                        if (user != null) {
                            nearbyUsers.add(user);
                        }
                    }
                    findUsers = nearbyUsers;

                } catch (LocationServerException e) {
                    return new SegueErrorResponse(Status.SERVICE_UNAVAILABLE,
                            "Unable to process request using 3rd party location provider").toResponse();
                } catch (UnableToIndexSchoolsException e) {
                    return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                            "Unable to process schools information").toResponse();
                } catch (JsonParseException | JsonMappingException e) {
                    log.error("Problem parsing school", e);
                    return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to read school")
                            .toResponse();
                } catch (IOException e) {
                    log.error("Problem parsing school", e);
                    return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                            "IOException while trying to communicate with the school service.").toResponse();
                } catch (NoUserException e) {
                    log.error("User cannot be found from user Id", e);
                }
                
            }

            // Calculate the ETag
            EntityTag etag = new EntityTag(findUsers.size() + findUsers.toString().hashCode()
                    + userPrototype.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            log.info(String.format("%s user (%s) did a search across all users based on user prototype {%s}",
                    currentUser.getRole(), currentUser.getEmail(), userPrototype));

            return Response.ok(findUsers).tag(etag).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
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
            if (!isUserAnAdminOrEventManager(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        try {
            log.info(String.format("%s user (%s) did a user id lookup based on user id {%s}", currentUser.getRole(),
                    currentUser.getEmail(), userId));

            return Response.ok(this.userManager.getUserDTOById(userId)).build();
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
            if (!isUserAnAdmin(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

            RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(httpServletRequest);
            if (currentlyLoggedInUser.getId().equals(userId)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are not allowed to delete yourself.")
                        .toResponse();
            }

            RegisteredUserDTO userToDelete = this.userManager.getUserDTOById(userId);
            
            this.userManager.deleteUserAccount(userToDelete);
            
            getLogManager().logEvent(currentlyLoggedInUser, httpServletRequest, DELETE_USER_ACCOUNT,
                    ImmutableMap.of("userIdDeleted", userToDelete.getId()));
            
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
     * Get users by school id.
     * 
     * @param request
     *            - to determine access.
     * @param schoolId
     *            - of the school of interest.
     * @return stats
     */
    @GET
    @Path("/users/schools/{school_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getSchoolStatistics(@Context final HttpServletRequest request,
            @PathParam("school_id") final String schoolId) {
        try {
            if (!isUserStaff(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin user to access this endpoint.")
                        .toResponse();
            }

            School school = schoolReader.findSchoolById(schoolId);
            
            Map<String, Object> result = ImmutableMap.of("school", school, "users",
                    statsManager.getUsersBySchoolId(schoolId));
            
            return Response.ok(result).build();
        } catch (UnableToIndexSchoolsException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable To Index SchoolIndexer Exception in admin facade", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ResourceNotFoundException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "We cannot locate the school requested").toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Error while trying to list users belonging to a school.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error").toResponse();
        } catch (NumberFormatException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "The school id provided is invalid.").toResponse();
        } catch (JsonParseException | JsonMappingException e) {
            log.error("Problem parsing school", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to read school").toResponse();
        } catch (IOException e) {
            log.error("Problem parsing school", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "IOException while trying to communicate with the school service.").toResponse();
        }
    }

    /**
     * Service method to fetch the event data for a specified user.
     *
     * @param request
     *            - request information used authentication
     * @param requestForCaching
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @param events
     *            - comma separated list of events of interest.,
     * @param bin
     *            - Should we group data into the first day of the month? true or false.
     * @return Returns a map of eventType to Map of dates to total number of events.
     * @throws BadRequestException
     *            - because the request is missing various essential parameters
     * @throws ForbiddenException
     *            - because the user is not an admin
     * @throws NoUserLoggedInException
     *            - because the user is not logged in
     * @throws SegueDatabaseException
     *            - because there has been some problem with database access
     */
    private Map<String, Map<LocalDate, Long>> fetchEventDataForAllUsers(@Context final Request request,
             @Context final HttpServletRequest httpServletRequest, @Context final Request requestForCaching,
             @QueryParam("from_date") final Long fromDate, @QueryParam("to_date") final Long toDate,
             @QueryParam("events") final String events, @QueryParam("bin_data") final Boolean bin)
            throws BadRequestException, ForbiddenException, NoUserLoggedInException, SegueDatabaseException {

        final boolean binData = null != bin && bin;

        if (null == events || events.isEmpty()) {
            throw new BadRequestException("You must specify the events you are interested in.");
        }

        if (null == fromDate || null == toDate) {
            throw new BadRequestException("You must specify the from_date and to_date you are interested in.");
        }

        if (!isUserStaff(httpServletRequest)) {
            throw new ForbiddenException("You must be logged in as an admin to access this function.");
        }

        Map<String, Map<LocalDate, Long>> eventLogsByDate = this.statsManager.getEventLogsByDate(
                Lists.newArrayList(events.split(",")), new Date(fromDate), new Date(toDate), binData);

        return eventLogsByDate;
    }

    /**
     * Get the event data for a specified user.
     * 
     * @param request
     *            - request information used authentication
     * @param requestForCaching
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @param events
     *            - comma separated list of events of interest.,
     * @param bin
     *            - Should we group data into the first day of the month? true or false.
     * @return Returns a map of eventType to Map of dates to total number of events.
     */
    @GET
    @Path("/users/event_data/over_time")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getEventDataForAllUsers(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, @Context final Request requestForCaching,
            @QueryParam("from_date") final Long fromDate, @QueryParam("to_date") final Long toDate,
            @QueryParam("events") final String events, @QueryParam("bin_data") final Boolean bin) {

        Map<String, Map<LocalDate, Long>> eventLogsByDate;
        try {
            eventLogsByDate = fetchEventDataForAllUsers(request, httpServletRequest, requestForCaching, fromDate,
                    toDate, events, bin);
        } catch (BadRequestException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (ForbiddenException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, e.getMessage()).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while getting event details for a user.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to complete the request.").toResponse();
        }

        EntityTag etag = new EntityTag(eventLogsByDate.toString().hashCode() + "");

        Response cachedResponse = generateCachedResponse(requestForCaching, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        return Response.ok(eventLogsByDate).tag(etag)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_FIVE_MINUTES, false)).build();
    }

    /**
     * Get the event data for a specified user, in CSV format.
     *
     * @param request
     *            - request information used authentication
     * @param requestForCaching
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @param fromDate
     *            - date to start search
     * @param toDate
     *            - date to end search
     * @param events
     *            - comma separated list of events of interest.,
     * @param bin
     *            - Should we group data into the first day of the month? true or false.
     * @return Returns a map of eventType to Map of dates to total number of events.
     */
    @GET
    @Path("/users/event_data/over_time/download")
    @Produces("text/csv")
    @GZIP
    public Response getEventDataForAllUsersDownloadCSV(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, @Context final Request requestForCaching,
            @QueryParam("from_date") final Long fromDate, @QueryParam("to_date") final Long toDate,
            @QueryParam("events") final String events, @QueryParam("bin_data") final Boolean bin) {

        try {
            Map<String, Map<LocalDate, Long>> eventLogsByDate;
            eventLogsByDate = fetchEventDataForAllUsers(request, httpServletRequest, requestForCaching, fromDate,
                    toDate, events, bin);

            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            List<String[]> rows = Lists.newArrayList();
            rows.add(new String[]{"event_type", "timestamp", "value"});

            for(Map.Entry<String, Map<LocalDate, Long>> eventType : eventLogsByDate.entrySet()) {
                String eventTypeKey = eventType.getKey();
                for(Map.Entry<LocalDate, Long> record : eventType.getValue().entrySet()) {
                    rows.add(new String[]{eventTypeKey, record.getKey().toString(), record.getValue().toString()});
                }
            }
            csvWriter.writeAll(rows);
            csvWriter.close();

            EntityTag etag = new EntityTag(eventLogsByDate.toString().hashCode() + "");
            Response cachedResponse = generateCachedResponse(requestForCaching, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(stringWriter.toString()).tag(etag)
                    .header("Content-Disposition", "attachment; filename=admin_stats.csv")
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_FIVE_MINUTES, false)).build();
        } catch (BadRequestException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, e.getMessage()).toResponse();
        } catch (ForbiddenException e) {
            return new SegueErrorResponse(Status.FORBIDDEN, e.getMessage()).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
            log.error("Database error while getting event details for a user.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to complete the request.").toResponse();
        } catch (IOException e) {
            log.error("IO error while creating the CSV file.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while creating the CSV file").toResponse();
        }
    }

    /**
     * Get the ip address location information.
     * 
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @param ipaddress
     *            - ip address to geocode.
     * @return Returns a location from an ip address
     */
    @GET
    @Path("/geocode_ip")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response findIP(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
            @QueryParam("ip") final String ipaddress) {

        if (null == ipaddress) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must specify the ip address you are interested in.")
                    .toResponse();
        }

        try {
            if (!isUserStaff(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as staff to access this function.").toResponse();
            }

            return Response.ok(locationManager.resolveAllLocationInformation(ipaddress)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to contact server to resolve location information", e).toResponse();
        } catch (LocationServerException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Problem resolving ip address", e).toResponse();
        }
    }

    /**
     * Get current perf log for analytics purposes.
     * 
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @return Returns a location from an ip address
     */
    @GET
    @Path("/view_perf_log")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response viewPerfLog(@Context final Request request, @Context final HttpServletRequest httpServletRequest) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(System.getProperty("catalina.base")
                + File.separator + "logs" + File.separator + "perf.log"))) {
            if (!isUserAnAdmin(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as staff to access this function.").toResponse();
            }

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                
                if (line.contains("callback")) {
                    // strip out query params that are provided on callback urls for security.
                    output.append(line.substring(0, line.indexOf("?")));
                } else {
                    output.append(line);    
                }
                output.append("\n");
            }
            log.info(String.format("User (%s) has accessed the perf logs.",
                    userManager.getCurrentRegisteredUser(httpServletRequest).getEmail()));
            return Response.ok(output.toString()).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to read the log file requested", e).toResponse();
        }
    }

    /**
     * Get current questionMetaData for analytics purposes.
     * 
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @return Returns a location from an ip address
     */
    @GET
    @Path("/download_meta_data")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getMetaData(@Context final Request request, @Context final HttpServletRequest httpServletRequest) {
        try {
            if (!isUserStaff(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as staff to access this function.").toResponse();
            }

            ResultsWrapper<ContentDTO> allByType = this.contentManager.getAllByTypeRegEx(
                    this.contentIndex, ".*", 0, -1);
            
            List<ContentSummaryDTO> results = Lists.newArrayList();
            for (ContentDTO c : allByType.getResults()) {
                results.add(this.contentManager.extractContentSummary(c));
            }

            ResultsWrapper<ContentSummaryDTO> toReturn = new ResultsWrapper<>(results,
                    allByType.getTotalResults());
            return Response.ok(toReturn).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to read the log file requested", e)
                    .toResponse();
        }
    }   
    
    /**
     * Is the current user an admin.
     * 
     * @param request
     *            - with session information
     * @return true if user is logged in as an admin, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    private boolean isUserAnAdmin(final HttpServletRequest request) throws NoUserLoggedInException {
        return isUserAnAdmin(userManager, request);
    }

    /**
     * Is the current user an admin.
     * 
     * @param request
     *            - with session information
     * @return true if user is logged in as an admin, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    private boolean isUserAnAdminOrEventManager(final HttpServletRequest request) throws NoUserLoggedInException {
        return isUserAnAdminOrEventManager(userManager, request);
    }

    
    /**
     * Is the current user in a staff role.
     * 
     * @param request
     *            - with session information
     * @return true if user is logged in as an admin, false otherwise.
     * @throws NoUserLoggedInException
     *             - if we are unable to tell because they are not logged in.
     */
    private boolean isUserStaff(final HttpServletRequest request) throws NoUserLoggedInException {
        return isUserStaff(userManager, request);
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
            if (isUserAnAdmin(request)) {

                HttpClient httpClient = new DefaultHttpClient();

                HttpPost httpPost = new HttpPost("http://" + getProperties().getProperty("ETL_HOSTNAME") + ":" +
                        getProperties().getProperty("ETL_PORT") + "/isaac-api/api/etl/set_version_alias/" + this.contentIndex + "/" + version);

                httpPost.addHeader("Content-Type", "application/json");

                HttpResponse httpResponse = httpClient.execute(httpPost);

                HttpEntity e = httpResponse.getEntity();

                if (httpResponse.getStatusLine().getStatusCode() == 200) {
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

    @POST
    @Path("/new_version_alert")
    @Produces(MediaType.APPLICATION_JSON)
    public Response versionChangeNotification(GithubPushEventPayload payload) {

        // TODO: Verify webhook secret.
        try {
            // We are only interested in the master branch
            if(payload.getRef().equals("refs/heads/master")) {
                String newVersion = payload.getAfter();

                HttpPost httpPost = new HttpPost("http://" + getProperties().getProperty("ETL_HOSTNAME") + ":" +
                        getProperties().getProperty("ETL_PORT") + "/isaac-api/api/etl/new_version_alert/" + newVersion);

                HttpResponse httpResponse = null;
                httpResponse = new DefaultHttpClient().execute(httpPost);
                HttpEntity e = httpResponse.getEntity();

                if (httpResponse.getStatusLine().getStatusCode() == 200) {
                    return Response.ok().build();
                } else {
                    SegueErrorResponse r = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, IOUtils.toString(e.getContent()));
                    r.setBypassGenericSiteErrorPage(true);
                    return r.toResponse();
                }
            }
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    e.getMessage()).toResponse();
        }
        return Response.ok().build();
    }

}