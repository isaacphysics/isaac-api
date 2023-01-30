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
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.services.ContentSummarizerService;
import uk.ac.cam.cl.dtg.segue.api.managers.IStatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.isaac.dos.IUserStreaksManager;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Isaac Controller
 * 
 * This class specifically caters for the Rutherford physics server and is expected to provide extended functionality to
 * the Segue api for use only on the Isaac site.
 * 
 * TODO: This class should be refactored, as it is just a random collection of endpoints.
 * 
 */
@Path("/")
@Tag(name = "/")
public class IsaacController extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(IsaacController.class);
    private static final String ETAG_SEPARATOR = "/";

    private final IStatisticsManager statsManager;
    private final UserAccountManager userManager;
    private final UserAssociationManager associationManager;
    private final String contentIndex;
    private final GitContentManager contentManager;
    private final UserBadgeManager userBadgeManager;
    private final IUserStreaksManager userStreaksManager;
    private final ContentSummarizerService contentSummarizerService;

    private static long lastQuestionCount = 0L;

    // Question counts are slow to calculate, so cache for up to 10 minutes. We may want to move this to a more
    // reusable place (such as statsManager.getLogCount) if we find ourselves using this pattern more).
    private final Supplier<Long> questionCountCache = Suppliers.memoizeWithExpiration(new Supplier<Long>() {
        public Long get() {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        log.info("Triggering question answer count query.");
                        lastQuestionCount = statsManager.getLogCount(SegueServerLogType.ANSWER_QUESTION.name());
                        log.info("Question answer count query complete.");
                    } catch (SegueDatabaseException e) {
                        lastQuestionCount = 0L;
                    }
                }
            });

            return lastQuestionCount;
        }
    }, 10, TimeUnit.MINUTES);

    /**
     * Creates an instance of the isaac controller which provides the REST endpoints for the isaac api.
     * 
     * @param propertiesLoader
     *            - Instance of properties Loader
     * @param logManager
     *            - Instance of Log Manager
     * @param statsManager
     *            - Instance of the Statistics Manager.
     * @param contentManager
     *            - so we can find out the latest content version.
     * @param userManager
     *            - So we can interrogate the user Manager.
     * @param associationManager
     *            - So we can check user permissions.
     * @param contentSummarizerService
     *            - So we can summarize search results
     */
    @Inject
    public IsaacController(final PropertiesLoader propertiesLoader,
                           final ILogManager logManager, final IStatisticsManager statsManager,
                           final UserAccountManager userManager, final GitContentManager contentManager,
                           final UserAssociationManager associationManager,
                           @Named(CONTENT_INDEX) final String contentIndex,
                           final IUserStreaksManager userStreaksManager,
                           final UserBadgeManager userBadgeManager,
                           final ContentSummarizerService contentSummarizerService) {
        super(propertiesLoader, logManager);
        this.statsManager = statsManager;
        this.userManager = userManager;
        this.associationManager = associationManager;
        this.contentIndex = contentIndex;
        this.contentManager = contentManager;
        this.userBadgeManager = userBadgeManager;
        this.userStreaksManager = userStreaksManager;
        this.contentSummarizerService = contentSummarizerService;
    }

    /**
     * Rest end point that searches the api for some search string.
     * 
     * @param request
     *            - so that we can handle caching of search responses.
     * @param httpServletRequest
     *            - so we can extract user information for logging.
     * @param searchString
     *            - to pass to the search engine.
     * @param types
     *            - a comma separated list of types to include in the search.
     * @param startIndex
     *            - the start index for the search results.
     * @param limit
     *            - the max number of results to return.
     * @return a response containing the search results (results wrapper) or an empty list.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/search")
    @GZIP
    @Operation(summary = "Search for content objects matching the provided criteria.")
    public final Response search(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
            @QueryParam("query") final String searchString,
            @DefaultValue(DEFAULT_TYPE_FILTER) @QueryParam("types") final String types,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
            @DefaultValue(DEFAULT_SEARCH_RESULT_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit) {

        // Check the search string is sensible:
        if (null == searchString || searchString.isEmpty()) {
            return SegueErrorResponse.getBadRequestResponse("Search string must not be blank.");
        }
        if (searchString.length() > SEARCH_TEXT_CHAR_LIMIT) {
            return SegueErrorResponse.getBadRequestResponse(String.format("Search string exceeded %s character limit.", SEARCH_TEXT_CHAR_LIMIT));
        }

        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentIndex.hashCode() + searchString.hashCode()
                + types.hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            AbstractSegueUserDTO currentUser = userManager.getCurrentUser(httpServletRequest);
            boolean showHiddenContent = false;
            if (currentUser instanceof RegisteredUserDTO) {
                showHiddenContent = isUserStaff(userManager, (RegisteredUserDTO) currentUser);
            }
            List<String> documentTypes = !types.isEmpty() ? Arrays.asList(types.split(",")) : null;
            // Return an error if any of the proposed document types are invalid
            if (documentTypes != null && !SITE_WIDE_SEARCH_VALID_DOC_TYPES.containsAll(documentTypes)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Invalid document types.").toResponse();
            }

            ResultsWrapper<ContentDTO> searchResults = this.contentManager.siteWideSearch(
                    searchString, documentTypes, showHiddenContent, startIndex, limit);

            ImmutableMap<String, String> logMap = new ImmutableMap.Builder<String, String>()
                    .put(TYPE_FIELDNAME, types)
                    .put("searchString", searchString)
                    .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();

            getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                    IsaacServerLogType.GLOBAL_SITE_SEARCH, logMap);

            ResultsWrapper results = this.contentSummarizerService.extractContentSummaryFromResultsWrapper(searchResults);
            return Response.ok(results).tag(etag)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                    .build();

        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to retrieve content requested", e)
                    .toResponse();
        } catch (NoUserLoggedInException e) {
            // This should never happen as we do not pass null to isUserStaff(...)
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Rest end point to allow images to be requested from the database.
     *
     * @param request
     *            - used for intelligent cache responses.
     * @param httpServletRequest
     *            - used for the Referer header for helpful error messages.
     * @param path
     *            of image in the database
     * @return a Response containing the image file contents or containing a SegueErrorResponse.
     */
    @GET
    @Produces("*/*")
    @Path("images/{path:.*}")
    @GZIP
    @Operation(summary = "Get a binary object from the current content version.",
                  description = "This can only be used to get images from the content database.")
    public final Response getImageByPath(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
                                         @PathParam("path") final String path) {
        if (null == path || Files.getFileExtension(path).isEmpty()) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid file path or filename.");
            return error.toResponse();
        }

        // Determine if we can use the cache; this has two parts: a "quick and cheap" early check that does not need
        // the file contents, and a more thorough check using the actual file contents.
        // The ETag has two halves, separated by an ETAG_SEPARATOR.
        String sha = this.contentManager.getCurrentContentSHA();
        String earlyCacheCheckTag = String.valueOf(sha.hashCode() + path.hashCode());

        String rawETagHeader = httpServletRequest.getHeader("If-None-Match");
        String rawETag = null != rawETagHeader ? rawETagHeader.replaceAll("\"", "") : null;

        // If the "early" cache check based on the path and content SHA matches, the file cannot have changed and
        // we don't need to load it to return a 304 Not Modified:
        if (null != rawETag && earlyCacheCheckTag.equals(rawETag.split(ETAG_SEPARATOR)[0])) {
            return Response.notModified().cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true)).tag(rawETag).build();
        }

        // If the content version has changed, we do need to load the file to see if it has been modified:
        ByteArrayOutputStream fileContent;
        String mimeType;

        switch (Files.getFileExtension(path).toLowerCase()) {
            case "svg":
                mimeType = "image/svg+xml";
                break;

            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;

            case "png":
                mimeType = "image/png";
                break;

            case "gif":
                mimeType = "image/gif";
                break;

            default:
                // Unsupported filetype, don't allow this.
                SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid file extension requested");
                log.debug(error.getErrorMessage());
                return error.toResponse();
        }

        try {
            fileContent = this.contentManager.getFileBytes(path);
        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error reading from file repository", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (UnsupportedOperationException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Multiple files match the search path provided.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }

        if (null == fileContent) {
            String refererHeader = httpServletRequest.getHeader("Referer");
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the file: " + path);
            log.warn(String.format("Unable to locate the file: (%s). Referer: (%s)", path, refererHeader));
            return error.toResponse();
        }

        byte[] fileContentBytes = fileContent.toByteArray();

        // If the "late" cache check based on the file contents matches, the file still has not changed:
        String lateCacheCheckTag = String.valueOf(Arrays.hashCode(fileContentBytes));
        if (null != rawETag && rawETag.contains(ETAG_SEPARATOR) && lateCacheCheckTag.equals(rawETag.split(ETAG_SEPARATOR)[1])) {
            return Response.notModified().cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true)).tag(rawETag).build();
        }

        // Otherwise, just return the full image to the client:
        EntityTag etag = new EntityTag(earlyCacheCheckTag + ETAG_SEPARATOR + lateCacheCheckTag);
        return Response.ok(fileContentBytes).type(mimeType)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true))
                .tag(etag).build();
    }
    /**
     * Endpoint to allow documents to be requested from the content database.
     *
     * @param request
     *            - used for intelligent cache responses.
     * @param httpServletRequest
     *            - used for the Referer header for helpful error messages.
     * @param path
     *            - file path of document in the content database
     * @return a Response containing the file contents as Content-Disposition: attachment, or a SegueErrorResponse.
     */
    @GET
    @Produces("*/*")
    @Path("documents/{path:.*}")
    @GZIP
    @Operation(summary = "Get a binary object from the current content version.",
                  description = "This can only be used to get PDF documents from the content database.")
    public final Response getDocumentByPath(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
                                         @PathParam("path") final String path) {
        if (null == path || Files.getFileExtension(path).isEmpty()) {
            SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid file path or filename.");
            return error.toResponse();
        }

        try {
            // All users with an Isaac account are allowed to access these resources
            RegisteredUserDTO currentlyLoggedInUser = userManager.getCurrentRegisteredUser(httpServletRequest);

            // determine if we can use the cache if so return cached response.
            String sha = this.contentManager.getCurrentContentSHA();
            EntityTag etag = new EntityTag(sha.hashCode() + path.hashCode() + "");
            Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_ONE_DAY);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            ByteArrayOutputStream fileContent;
            String mimeType;
            switch (Files.getFileExtension(path).toLowerCase()) {
                case "pdf":
                    mimeType = "application/pdf";
                    break;

                default:
                    // if it is an unknown type return an error
                    SegueErrorResponse error = new SegueErrorResponse(Status.BAD_REQUEST, "Invalid file type requested");
                    return error.toResponse(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, false), etag);
            }

            fileContent = this.contentManager.getFileBytes(path);
            if (null == fileContent) {
                String refererHeader = httpServletRequest.getHeader("Referer");
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the file: " + path);
                log.warn(String.format("Unable to locate the file: (%s). Referer: (%s)", path, refererHeader));
                return error.toResponse(getCacheControl(NUMBER_SECONDS_IN_TEN_MINUTES, false), etag);
            }

            ImmutableMap<String, String> logMap = new ImmutableMap.Builder<String, String>()
                    .put(DOCUMENT_PATH_LOG_FIELDNAME, path)
                    .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();
            getLogManager().logEvent(currentlyLoggedInUser, httpServletRequest, IsaacServerLogType.DOWNLOAD_FILE, logMap);

            return Response.ok(fileContent.toByteArray()).type(mimeType)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_DAY, true))
                    .header("Content-Disposition", "attachment")  // Do not show this file in the browser.
                    .tag(etag).build();

        } catch (IOException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error reading file!");
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (UnsupportedOperationException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Multiple files match the path provided.");
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * Get some statistics out of how many questions the user has completed.
     * 
     * @param request
     *            - so we can find the current user.
     * @return a map containing the information.
     */
    @GET
    @Path("users/current_user/progress")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get progress information for the current user.")
    public final Response getCurrentUserProgressInformation(@Context final HttpServletRequest request) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        return getUserProgressInformation(request, user.getId());
    }

    /**
     * Get snapshot for the current user.
     *
     * @param request
     *            - so we can find the current user.
     * @return a map containing the information.
     */
    @GET
    @Path("users/current_user/snapshot")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get snapshot for the current user.")
    public final Response getCurrentUserSnapshot(@Context final HttpServletRequest request) {
        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        Map<String, Object> dailyStreakRecord = userStreaksManager.getCurrentStreakRecord(user);
        dailyStreakRecord.put("largestStreak", userStreaksManager.getLongestStreak(user));

        Map<String, Object> weeklyStreakRecord = userStreaksManager.getCurrentWeeklyStreakRecord(user);
        weeklyStreakRecord.put("largestStreak", userStreaksManager.getLongestWeeklyStreak(user));

        Map<String, Object> userSnapshot = ImmutableMap.of(
                "dailyStreakRecord", dailyStreakRecord,
                "weeklyStreakRecord", weeklyStreakRecord,
                "achievementsRecord", userBadgeManager.getAllUserBadges(user)
        );

        return Response.ok(userSnapshot).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
    }

    /**
     * Get some statistics out of how many questions the user has completed.
     * 
     * Only users with permission can use this endpoint.
     * 
     * @param request
     *            - so we can authenticate the current user.
     * @param userIdOfInterest
     *            - to look up the user of interest
     * @return a map containing the information.
     */
    @GET
    @Path("users/{user_id}/progress")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get progress information for a specified user.")
    public final Response getUserProgressInformation(@Context final HttpServletRequest request,
            @PathParam("user_id") final Long userIdOfInterest) {
        RegisteredUserDTO user;
        UserSummaryDTO userOfInterestSummary;
        RegisteredUserDTO userOfInterestFull;
        try {
            user = userManager.getCurrentRegisteredUser(request);
            userOfInterestFull = userManager.getUserDTOById(userIdOfInterest);
            if (null == userOfInterestFull) {
                throw new NoUserException("No user found with this ID.");
            }
            userOfInterestSummary = userManager.convertToUserSummaryObject(userOfInterestFull);

            // If user is fetching their own progress, or the user is at least a teacher (tutors cannot see their
            // students progress) and has a valid connection with this user...
            if (associationManager.hasTeacherPermission(user, userOfInterestSummary)) {
                Map<String, Object> userProgressInformation = statsManager.getUserQuestionInformation(userOfInterestFull);

                // augment details with user snapshot data (perhaps one day we will replace the entire endpoint with this call)
                Map<String, Object> dailyStreakRecord = userStreaksManager.getCurrentStreakRecord(userOfInterestFull);
                dailyStreakRecord.put("largestStreak", userStreaksManager.getLongestStreak(userOfInterestFull));

                Map<String, Object> weeklyStreakRecord = userStreaksManager.getCurrentWeeklyStreakRecord(userOfInterestFull);
                weeklyStreakRecord.put("largestStreak", userStreaksManager.getLongestWeeklyStreak(userOfInterestFull));

                Map<String, Object> userSnapshot = ImmutableMap.of(
                        "dailyStreakRecord", dailyStreakRecord,
                        "weeklyStreakRecord", weeklyStreakRecord,
                        "achievementsRecord", userBadgeManager.getAllUserBadges(userOfInterestFull)
                );

                userProgressInformation.put("userSnapshot", userSnapshot);

                this.getLogManager().logEvent(user, request, IsaacServerLogType.VIEW_USER_PROGRESS,
                        ImmutableMap.of(USER_ID_FKEY_FIELDNAME, userOfInterestFull.getId()));

                return Response.ok(userProgressInformation).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
            } else {
                return new SegueErrorResponse(Status.FORBIDDEN, "You do not have permission to view this users data.")
                        .toResponse();
            }
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (NoUserException e) {
            return SegueErrorResponse.getResourceNotFoundResponse("No user found with this id");
        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to access user statistics.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            String message = "Error whilst trying to access user statistics; Content cannot be resolved";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }
}
