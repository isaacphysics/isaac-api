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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
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

import org.elasticsearch.common.collect.Lists;
import org.jboss.resteasy.annotations.GZIP;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.segue.api.Constants.EnvironmentType;
import uk.ac.cam.cl.dtg.segue.api.managers.ContentVersionController;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.LocationHistoryManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dao.schools.UnableToIndexSchoolsException;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.users.Role;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.locations.Location;
import uk.ac.cam.cl.dtg.util.locations.LocationServerException;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Admin facade for segue.
 * 
 * @author Stephen Cummins
 * 
 */
@Path("/admin")
public class AdminFacade extends AbstractSegueFacade {
    private static final Logger log = LoggerFactory.getLogger(AdminFacade.class);

    private final UserManager userManager;
    private final ContentVersionController contentVersionController;

    private StatisticsManager statsManager;

    private LocationHistoryManager locationManager;

    /**
     * Create an instance of the administrators facade.
     * 
     * @param properties
     *            - the fully configured properties loader for the api.
     * @param userManager
     *            - The manager object responsible for users.
     * @param contentVersionController
     *            - The content version controller used by the api.
     * @param logManager
     *            - So we can log events of interest.
     * @param statsManager
     *            - So we can report high level stats.
     * @param locationManager
     *            - for geocoding if we need it.
     */
    @Inject
    public AdminFacade(final PropertiesLoader properties, final UserManager userManager,
            final ContentVersionController contentVersionController, final ILogManager logManager,
            final StatisticsManager statsManager, final LocationHistoryManager locationManager) {
        super(properties, logManager);
        this.userManager = userManager;
        this.contentVersionController = contentVersionController;
        this.statsManager = statsManager;
        this.locationManager = locationManager;
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
                    .cacheControl(getCacheControl(CACHE_FOR_FIVE_MINUTES)).build();
        } catch (SegueDatabaseException e) {
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
     * @return stats
     */
    @GET
    @Path("/stats/users/last_locations")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getLastLocations(@Context final HttpServletRequest request,
            @Context final Request requestForCaching) {
        try {
            if (!isUserStaff(request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an admin to access this endpoint.")
                        .toResponse();
            }

            Calendar threshold = Calendar.getInstance();
            threshold.setTime(new Date());
            threshold.add(Calendar.MONTH, -1);

            Collection<Location> locationInformation = statsManager.getLocationInformation(threshold.getTime());

            return Response.ok(locationInformation).cacheControl(getCacheControl(CACHE_FOR_FIVE_MINUTES)).build();
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

            return Response.ok(schoolStatistics).tag(etag).cacheControl(getCacheControl(CACHE_FOR_FIVE_MINUTES))
                    .build();
        } catch (UnableToIndexSchoolsException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable To Index Schools Exception in admin facade", e).toResponse();
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
                IContentManager contentPersistenceManager = contentVersionController.getContentManager();
                String newVersion;
                if (!contentPersistenceManager.isValidVersion(version)) {
                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid version selected: "
                            + version);
                    log.warn(error.getErrorMessage());
                    return error.toResponse();
                }

                if (!contentPersistenceManager.getCachedVersionList().contains(version)) {
                    newVersion = contentVersionController.triggerSyncJob(version).get();
                } else {
                    newVersion = version;
                }

                Collection<String> availableVersions = contentPersistenceManager.getCachedVersionList();

                if (!availableVersions.contains(version)) {
                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid version selected: "
                            + version);
                    log.warn(error.getErrorMessage());
                    return error.toResponse();
                }

                contentVersionController.setLiveVersion(newVersion);
                log.info("Live version of the site changed to: " + newVersion + " by user: "
                        + this.userManager.getCurrentRegisteredUser(request).getEmail());

                return Response.ok().build();
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (InterruptedException e) {
            log.error("ExecutorException during version change.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while trying to terminate a process.", e)
                    .toResponse();
        } catch (ExecutionException e) {
            log.error("ExecutorException during version change.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
        }
    }

    /**
     * This method will try to bring the live version that Segue is using to host content up-to-date with the latest in
     * the database.
     * 
     * @param request
     *            - to enable security checking.
     * @return a response to indicate the synchronise job has triggered.
     */
    @POST
    @Path("/synchronise_datastores")
    public synchronized Response synchroniseDataStores(@Context final HttpServletRequest request) {
        try {
            // check if we are authorized to do this operation.
            // no authorisation required in DEV mode, but in PROD we need to be
            // an admin.
            if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT)
                    .equals(Constants.EnvironmentType.PROD.name())
                    || isUserAnAdmin(request)) {
                log.info("Informed of content change; " + "so triggering new synchronisation job.");
                contentVersionController.triggerSyncJob().get();
                return Response.ok("success - job started").build();
            } else {
                log.warn("Unable to trigger synch job as not an admin or this server is set to the PROD environment.");
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
                        .toResponse();
            }
        } catch (NoUserLoggedInException e) {
            log.warn("Unable to trigger synch job as not logged in.");
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (InterruptedException e) {
            log.error("ExecutorException during synchronise datastores operation.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error while trying to terminate a process.", e)
                    .toResponse();
        } catch (ExecutionException e) {
            log.error("ExecutorException during synchronise datastores operation.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error during verison change.", e).toResponse();
        }
    }

    /**
     * This method is only intended to be used on development / staging servers.
     * 
     * It will try to bring the live version that Segue is using to host content up-to-date with the latest in the
     * database.
     * 
     * @param request
     *            - to enable security checking.
     * @return a response to indicate the synchronise job has triggered.
     */
    @POST
    @Path("/new_version_alert")
    @Produces(MediaType.APPLICATION_JSON)
    public Response versionChangeNotification(@Context final HttpServletRequest request) {
        // check if we are authorized to do this operation.
        // no authorisation required in DEV mode, but in PROD we need to be
        // an admin.
        try {
            if (!this.getProperties().getProperty(Constants.SEGUE_APP_ENVIRONMENT)
                    .equals(Constants.EnvironmentType.PROD.name())
                    || this.isUserAnAdmin(request)) {
                log.info("Informed of content change; so triggering new async synchronisation job.");
                // on this occasion we don't want to wait for a response.
                contentVersionController.triggerSyncJob();
                return Response.ok().build();
            } else {
                log.warn("Unable to trigger synch job as this segue environment is "
                        + "configured in PROD mode unless you are an ADMIN.");
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be an administrator to use this function on the PROD environment.").toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return new SegueErrorResponse(Status.UNAUTHORIZED,
                    "You must be logged in to use this function on a PROD environment.").toResponse();
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
    @Path("/clear_caches")
    public synchronized Response clearCaches(@Context final HttpServletRequest request) {
        try {
            if (isUserAnAdmin(request)) {
                IContentManager contentPersistenceManager = contentVersionController.getContentManager();

                log.info("Clearing all caches...");
                contentPersistenceManager.clearCache();

                ImmutableMap<String, String> response = new ImmutableMap.Builder<String, String>().put("result",
                        "success").build();

                return Response.ok(response).build();
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
                        .toResponse();
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
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
     * This method will show a string representation of all jobs in the to index queue.
     * 
     * @param request
     *            - containing user session information.
     * 
     * @return the latest queue information
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/content_index_queue")
    public synchronized Response getCurrentIndexQueue(@Context final HttpServletRequest request) {
        try {
            if (isUserAnAdmin(request)) {
                ImmutableMap<String, Object> response = new ImmutableMap.Builder<String, Object>().put("queue",
                        contentVersionController.getToIndexQueue()).build();

                return Response.ok(response).build();
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
                        .toResponse();
            }

        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * This method will delete all jobs not yet started in the indexer queue.
     * 
     * @param request
     *            - containing user session information.
     * 
     * @return the new queue.
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/content_index_queue")
    public synchronized Response deleteAllInCurrentIndexQueue(@Context final HttpServletRequest request) {
        try {
            if (isUserAnAdmin(request)) {
                RegisteredUserDTO u = userManager.getCurrentRegisteredUser(request);
                log.info(String.format("Admin user (%s) requested to empty indexer queue.", u.getEmail()));

                contentVersionController.cleanUpTheIndexQueue();

                ImmutableMap<String, Object> response = new ImmutableMap.Builder<String, Object>().put("queue",
                        contentVersionController.getToIndexQueue()).build();

                return Response.ok(response).build();
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You must be an administrator to use this function.")
                        .toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Rest end point to allow content editors to see the content which failed to import into segue.
     * 
     * @param request
     *            - to identify if the user is authorised.
     * 
     * @return a content object, such that the content object has children. The children represent each source file in
     *         error and the grand children represent each error.
     */
    @GET
    @Path("/content_problems")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getContentProblems(@Context final HttpServletRequest request) {
        Map<Content, List<String>> problemMap = contentVersionController.getContentManager().getProblemMap(
                contentVersionController.getLiveVersion());

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

        if (null == problemMap) {
            return Response.ok(new Content("No problems found.")).build();
        }

        // build up a content object to return.
        int brokenFiles = 0;
        int errors = 0;

        Content c = new Content();
        c.setId("dynamic_problem_report");
        for (Map.Entry<Content, List<String>> pair : problemMap.entrySet()) {
            Content child = new Content();
            child.setTitle(pair.getKey().getTitle());
            child.setCanonicalSourceFile(pair.getKey().getCanonicalSourceFile());
            brokenFiles++;

            for (String s : pair.getValue()) {
                Content erroredContentObject = new Content(s);

                erroredContentObject.setId(pair.getKey().getId() + "_error_" + errors);

                child.getChildren().add(erroredContentObject);

                errors++;
            }

            c.getChildren().add(child);
            child.setId(pair.getKey().getId() + "_problem_report_" + errors);
        }

        c.setSubtitle("Total Broken files: " + brokenFiles + " Total errors : " + errors);

        return Response.ok(c).build();
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
     * @return a userDTO or a segue error response
     */
    @GET
    @Path("/users")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response findUsers(@Context final HttpServletRequest httpServletRequest, @Context final Request request,
            @QueryParam("id") final String userId, @QueryParam("email") @Nullable final String email,
            @QueryParam("familyName") @Nullable final String familyName, @QueryParam("role") @Nullable final Role role,
            @QueryParam("schoolOther") @Nullable final String schoolOther) {

        RegisteredUserDTO currentUser;
        try {
            currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdmin(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        try {
            RegisteredUserDTO userPrototype = new RegisteredUserDTO();
            if (null != userId && !userId.isEmpty()) {
                userPrototype.setDbId(userId);
            }

            if (null != email && !email.isEmpty()) {
                userPrototype.setEmail(email);
            }

            if (null != familyName && !familyName.isEmpty()) {
                userPrototype.setFamilyName(familyName);
            }

            if (null != role) {
                userPrototype.setRole(role);
            }

            if (null != schoolOther) {
                userPrototype.setSchoolOther(schoolOther);
            }

            List<RegisteredUserDTO> findUsers = this.userManager.findUsers(userPrototype);

            // Calculate the ETag
            EntityTag etag = new EntityTag(findUsers.toString().hashCode() + userPrototype.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            log.info(String.format("%s user (%s) did a search across all users based on user prototype {%s}",
                    currentUser.getRole(), currentUser.getEmail(), userPrototype));

            return Response.ok(findUsers).tag(etag).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK))
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
            @PathParam("user_id") final String userId) {

        RegisteredUserDTO currentUser;
        try {
            currentUser = userManager.getCurrentRegisteredUser(httpServletRequest);
            if (!isUserAnAdmin(httpServletRequest)) {
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
            @PathParam("user_id") final String userId) {
        try {
            if (!isUserAnAdmin(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

            RegisteredUserDTO currentlyLoggedInUser = this.userManager.getCurrentRegisteredUser(httpServletRequest);
            if (currentlyLoggedInUser.getDbId().equals(userId)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You are not allowed to delete yourself.")
                        .toResponse();
            }

            this.userManager.deleteUserAccount(userId);

            log.info("Admin User: " + currentlyLoggedInUser.getEmail() + " has just deleted the user account with id: "
                    + userId);

            return Response.noContent().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (SegueDatabaseException e) {
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

            return Response.ok(statsManager.getUsersBySchoolId(schoolId)).build();
        } catch (UnableToIndexSchoolsException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable To Index Schools Exception in admin facade", e).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ResourceNotFoundException e) {
            return new SegueErrorResponse(Status.NOT_FOUND, "We cannot locate the school requested").toResponse();
        } catch (SegueDatabaseException e) {
            log.error("Error while trying to list users belonging to a school.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error").toResponse();
        }
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

        final boolean binData;
        if (null == bin || !bin) {
            binData = false;
        } else {
            binData = true;
        }

        if (null == events || events.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must specify the events you are interested in.")
                    .toResponse();
        }

        if (null == fromDate || null == toDate) {
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You must specify the from_date and to_date you are interested in.").toResponse();
        }

        try {
            if (!isUserStaff(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

            Map<String, Map<LocalDate, Integer>> eventLogsByDate = this.statsManager.getEventLogsByDate(
                    Lists.newArrayList(events.split(",")), new Date(fromDate), new Date(toDate), binData);

            // Calculate the ETag
            EntityTag etag = new EntityTag(eventLogsByDate.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(requestForCaching, etag);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(eventLogsByDate).tag(etag).cacheControl(getCacheControl(CACHE_FOR_FIVE_MINUTES)).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
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
                + File.separator + "logs" + File.separator + "perf.log"));) {
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

            return Response.ok(output.toString()).build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (IOException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to read the log file requested", e).toResponse();
        }
    }
    
    /**
     * Batch job to start ip address processing.
     * 
     * Temporary endpoint for use once by admin user.
     * 
     * @param request
     *            - request information used for caching.
     * @param httpServletRequest
     *            - the request which may contain session information.
     * @return Returns a location from an ip address
     */
    @POST
    @Path("/create_geocode_history")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response createHistory(@Context final Request request, @Context final HttpServletRequest httpServletRequest) {

        try {
            if (!isUserAnAdmin(httpServletRequest)) {
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You must be logged in as an admin to access this function.").toResponse();
            }

            final Set<String> ipAddressesAlreadyGeoCoded = new HashSet<String>();
            log.info("Starting batch processing job for historic geocoding data..");
            final Set<String> allIpAddresses = super.getLogManager().getAllIpAddresses();
            final int updateInterval = 15;
            Thread generateLocationInfoJob = new Thread() {
                @Override
                public void run() {
                    int i = 0;
                    for (String ipAddress : allIpAddresses) {
                        String ipAddressOfInterest = ipAddress.split(",")[0];

                        if (!ipAddressesAlreadyGeoCoded.contains(ipAddressOfInterest)) {
                            ipAddressesAlreadyGeoCoded.add(ipAddressOfInterest);
                            if (i % updateInterval == 0) {
                                log.info("Batch job processing: " + i + "/" + allIpAddresses.size() + " complete");
                            }

                            try {
                                locationManager.refreshLocation(ipAddressOfInterest);

                            } catch (SegueDatabaseException | IOException e) {
                                log.error("Failed to resolve ip address on batch run: " + ipAddressOfInterest, e);
                            }
                        }
                        i++;
                    }
                    log.info("Batch processing complete.");

                }
            };
            generateLocationInfoJob.setDaemon(true);
            generateLocationInfoJob.start();

            return Response.ok().build();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
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
}