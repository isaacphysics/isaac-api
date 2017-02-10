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
package uk.ac.cam.cl.dtg.isaac.api;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.GLOBAL_SITE_SEARCH;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.PROXY_PATH;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.VIEW_USER_PROGRESS;
import static uk.ac.cam.cl.dtg.segue.api.Constants.ANSWER_QUESTION;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_RESULTS_LIMIT_AS_STRING;
import static uk.ac.cam.cl.dtg.segue.api.Constants.DEFAULT_START_INDEX_AS_STRING;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_MINUTE;
import static uk.ac.cam.cl.dtg.segue.api.Constants.TYPE_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.USER_ID_FKEY_FIELDNAME;
import static uk.ac.cam.cl.dtg.segue.api.Constants.NUMBER_SECONDS_IN_ONE_HOUR;
import static uk.ac.cam.cl.dtg.segue.api.Constants.CONTENT_INDEX;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.inject.name.Named;
import io.swagger.annotations.Api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
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

import ma.glasnost.orika.MapperFacade;

import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.segue.api.SegueContentFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.StatisticsManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.UserSummaryDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

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
@Api(value = "/")
public class IsaacController extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(IsaacController.class);

    private final SegueContentFacade api;
    private final MapperFacade mapper;
    private final StatisticsManager statsManager;
    private final UserAccountManager userManager;
    private final UserAssociationManager associationManager;
    private final URIManager uriManager;
    private final String contentIndex;
    private final IContentManager contentManager;

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
                        lastQuestionCount = statsManager.getLogCount(ANSWER_QUESTION);
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
     * @param api
     *            - Instance of segue Api
     * @param propertiesLoader
     *            - Instance of properties Loader
     * @param logManager
     *            - Instance of Log Manager
     * @param mapper
     *            - Instance of Mapper facade.
     * @param statsManager
     *            - Instance of the Statistics Manager.
     * @param contentManager
     *            - so we can find out the latest content version.
     * @param userManager
     *            - So we can interrogate the user Manager.
     * @param associationManager
     *            - So we can check user permissions.
     * @param uriManager
     *            - URI manager so we can augment uris
     */
    @Inject
    public IsaacController(final SegueContentFacade api, final PropertiesLoader propertiesLoader,
                           final ILogManager logManager, final MapperFacade mapper, final StatisticsManager statsManager,
                           final UserAccountManager userManager, final IContentManager contentManager,
                           final UserAssociationManager associationManager, final URIManager uriManager,
                           @Named(CONTENT_INDEX) final String contentIndex) {
        super(propertiesLoader, logManager);
        this.api = api;
        this.mapper = mapper;
        this.statsManager = statsManager;
        this.userManager = userManager;
        this.associationManager = associationManager;
        this.uriManager = uriManager;
        this.contentIndex = contentIndex;
        this.contentManager = contentManager;
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
    @Path("search/{searchString}")
    @GZIP
    public final Response search(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
            @PathParam("searchString") final String searchString, @QueryParam("types") final String types,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit) {

        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentIndex.hashCode() + searchString.hashCode()
                + types.hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        ResultsWrapper<ContentDTO> searchResults;
        try {
            Map<String, List<String>> typesThatMustMatch = null;

            if (null != types) {
                typesThatMustMatch = Maps.newHashMap();
                typesThatMustMatch.put(TYPE_FIELDNAME, Arrays.asList(types.split(",")));
            }

            searchResults = this.contentManager.searchForContent(this.contentIndex,
                    searchString, typesThatMustMatch, startIndex, limit);
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to retrieve content requested", e)
                    .toResponse();
        }

        // TODO: Log content sha, not "live"/"latest"
        ImmutableMap<String, String> logMap = new ImmutableMap.Builder<String, String>().put(TYPE_FIELDNAME, types)
                .put("searchString", searchString).put(CONTENT_INDEX, this.contentIndex).build();

        getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                GLOBAL_SITE_SEARCH, logMap);

        return Response
                .ok(this.extractContentSummaryFromResultsWrapper(searchResults,
                        this.getProperties().getProperty(PROXY_PATH))).tag(etag)
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                .build();
    }

    /**
     * Rest end point to allow images to be requested from the database.
     * 
     * @param request
     *            - used for intelligent cache responses.
     * @param path
     *            of image in the database
     * @return a Response containing the image file contents or containing a SegueErrorResponse.
     */
    @GET
    @Produces("*/*")
    @Path("images/{path:.*}")
    @GZIP
    public final Response getImageByPath(@Context final Request request, @PathParam("path") final String path) {
        // entity tags etc are already added by segue
        return api.getImageFileContent(request, this.contentIndex, path);
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
    public final Response getUserProgressInformation(@Context final HttpServletRequest request,
            @PathParam("user_id") final Long userIdOfInterest) {
        RegisteredUserDTO user;
        UserSummaryDTO userOfInterestSummary;
        RegisteredUserDTO userOfInterestFull;
        try {
            user = userManager.getCurrentRegisteredUser(request);
            userOfInterestFull = userManager.getUserDTOById(userIdOfInterest);
            if (null == userOfInterestFull) {
                throw new NoUserException();
            }
            userOfInterestSummary = userManager.convertToUserSummaryObject(userOfInterestFull);

            if (associationManager.hasPermission(user, userOfInterestSummary)) {
                Map<String, Object> userQuestionInformation = statsManager
                        .getUserQuestionInformation(userOfInterestFull);

                this.getLogManager().logEvent(user, request, VIEW_USER_PROGRESS,
                        ImmutableMap.of(USER_ID_FKEY_FIELDNAME, userOfInterestFull.getId()));

                return Response.ok(userQuestionInformation).build();
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

    /**
     * Statistics endpoint.
     * 
     * @param request
     *            - to determine access.
     * @return stats
     */
    @GET
    @Path("stats/questions_answered/count")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public Response getQuestionCount(@Context final HttpServletRequest request) {
        // Update the question count if it's expired
        questionCountCache.get();

        // Return the old question count
        return Response.ok(ImmutableMap.of("answeredQuestionCount", lastQuestionCount))
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_MINUTE, false)).build();
    }
    
    /**
     * This method will extract basic information from a content object so the lighter ContentInfo object can be sent to
     * the client instead.
     * 
     * @param content
     *            - the content object to summarise
     * @return ContentSummaryDTO.
     */
    private ContentSummaryDTO extractContentSummary(final ContentDTO content) {
        if (null == content) {
            return null;
        }

        // try auto-mapping
        ContentSummaryDTO contentInfo = mapper.map(content, ContentSummaryDTO.class);
        contentInfo.setUrl(uriManager.generateApiUrl(content));

        return contentInfo;
    }

    /**
     * Utility method to convert a ResultsWrapper of content objects into one with ContentSummaryDTO objects.
     * 
     * @param contentList
     *            - the list of content to summarise.
     * @param proxyPath
     *            - the path used for augmentation of urls.
     * @return list of shorter ContentSummaryDTO objects.
     */
    private ResultsWrapper<ContentSummaryDTO> extractContentSummaryFromResultsWrapper(
            final ResultsWrapper<ContentDTO> contentList, final String proxyPath) {
        if (null == contentList) {
            return null;
        }

        ResultsWrapper<ContentSummaryDTO> contentSummaryResults = new ResultsWrapper<ContentSummaryDTO>(
                new ArrayList<ContentSummaryDTO>(), contentList.getTotalResults());

        for (ContentDTO content : contentList.getResults()) {
            ContentSummaryDTO contentInfo = extractContentSummary(content);
            if (null != contentInfo) {
                contentSummaryResults.getResults().add(contentInfo);
            }
        }
        return contentSummaryResults;
    }
}
