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

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

import com.google.inject.name.Named;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionSummaryPage;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionSummaryPageDTO;
import uk.ac.cam.cl.dtg.segue.api.SegueContentFacade;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * Pages Facade
 * 
 * This class specifically caters for displaying isaac specific content pages.
 */
@Path("/pages")
@Api(value = "/pages")
public class PagesFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(PagesFacade.class);

    private final SegueContentFacade api;
    private final MapperFacade mapper;
    private final UserAccountManager userManager;
    private final URIManager uriManager;
    private final QuestionManager questionManager;
    private final IContentManager contentManager;

    private final GameManager gameManager;
    private final String contentIndex;

    /**
     * Creates an instance of the pages controller which provides the REST endpoints for accessing page content.
     * 
     * @param api
     *            - Instance of segue Api
     * @param propertiesLoader
     *            - Instance of properties Loader
     * @param logManager
     *            - Instance of Log Manager
     * @param mapper
     *            - Instance of Mapper facade.
     * @param contentManager
     *            - so we can get the latest content.
     * @param userManager
     *            - So we can interrogate the user Manager.
     * @param uriManager
     *            - URI manager so we can augment uris
     * @param questionManager
     *            - So we can look up attempt information.
     * @param gameManager
     *            - For looking up gameboard information.
     */
    @Inject
    public PagesFacade(final SegueContentFacade api, final PropertiesLoader propertiesLoader,
                       final ILogManager logManager, final MapperFacade mapper, final IContentManager contentManager,
                       final UserAccountManager userManager, final URIManager uriManager, final QuestionManager questionManager,
                       final GameManager gameManager, @Named(CONTENT_INDEX) final String contentIndex) {
        super(propertiesLoader, logManager);
        this.api = api;
        this.mapper = mapper;
        this.contentManager = contentManager;
        this.userManager = userManager;
        this.uriManager = uriManager;
        this.questionManager = questionManager;
        this.gameManager = gameManager;
        this.contentIndex = contentIndex;
    }

    /**
     * REST end point to provide a list of concepts.
     * 
     * Uses ETag caching to attempt to reduce load on the server.
     *
     * @param request
     *            - used to determine if we can return a cache response.
     * @param ids
     *            - the ids of the concepts to request.
     * @param tags
     *            - a comma separated list of strings
     * @param startIndex
     *            - a string value to be converted into an integer which represents the start index of the results
     * @param limit
     *            - a string value to be converted into an integer that represents the number of results to return.
     * @return A response object which contains a list of concepts or an empty list.
     */
    @GET
    @Path("/concepts")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Gets the list of concept pages")
    public final Response getConceptList(@Context final Request request, @QueryParam("ids") final String ids,
            @QueryParam("tags") final String tags,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit) {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(CONCEPT_TYPE));

        StringBuilder etagCodeBuilder = new StringBuilder();

        Integer newLimit = null;

        if (limit != null) {
            newLimit = limit;
            etagCodeBuilder.append(limit.toString());
        }

        // options
        if (ids != null) {
            List<String> idsList = Arrays.asList(ids.split(","));
            fieldsToMatch.put(ID_FIELDNAME, idsList);
            newLimit = idsList.size();
            etagCodeBuilder.append(ids);
        }

        if (tags != null) {
            fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
            etagCodeBuilder.append(tags);
        }

        // Calculate the ETag on last modified date of tags list
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + etagCodeBuilder.toString().hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            return listContentObjects(fieldsToMatch, startIndex, newLimit).tag(etag)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                    .build();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
                    "Error locating the content requested", e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * Rest end point that gets a single concept based on a given id.
     * 
     * @param request
     *            - so we can deal with caching and ETags.
     * @param servletRequest
     *            - so we can extract user information for logging.
     * @param conceptId
     *            as a string
     * @return A Response object containing a concept object.
     */
    @GET
    @Path("/concepts/{concept_page_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getConcept(@Context final Request request, @Context final HttpServletRequest servletRequest,
            @PathParam("concept_page_id") final String conceptId) {
        if (null == conceptId || conceptId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid concept id.").toResponse();
        }

        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + "byId".hashCode()
                + conceptId.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(CONCEPT_TYPE));

        // options
        if (null != conceptId) {
            fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(conceptId));
        }

        Response result = this.findSingleResult(fieldsToMatch);

        if (result.getEntity() instanceof SeguePageDTO) {
            ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
                    .put(CONCEPT_ID_LOG_FIELDNAME, conceptId).put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA())
                    .build();

            // the request log
            getLogManager().logEvent(userManager.getCurrentUser(servletRequest), servletRequest,
                    IsaacLogType.VIEW_CONCEPT, logEntry);
        }

        Response cachableResult = Response.status(result.getStatus()).entity(result.getEntity())
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();

        return cachableResult;
    }

    /**
     * REST end point to provide a list of questions.
     * 
     * @param request
     *            - used to determine if we can return a cache response.
     * @param ids
     *            - the ids of the concepts to request.
     * @param searchString
     *            - an optional search string to allow finding of questions by title.
     * @param tags
     *            - a comma separated list of strings
     * @param level
     *            - a string value to be converted into an integer which represents the levels that must match the
     *            questions returned.
     * @param fasttrack
     *            - a flag to indicate whether to search isaacFasttrackQuestions or not.
     * @param startIndex
     *            - a string value to be converted into an integer which represents the start index of the results
     * @param limit
     *            - a string value to be converted into an integer that represents the number of results to return.
     * @return A response object which contains a list of questions or an empty list.
     */
    @GET
    @Path("/questions")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getQuestionList(@Context final Request request, @QueryParam("ids") final String ids,
            @QueryParam("searchString") final String searchString, @QueryParam("tags") final String tags,
            @QueryParam("levels") final String level, @DefaultValue("false") @QueryParam("fasttrack") final Boolean fasttrack,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit) {
        StringBuilder etagCodeBuilder = new StringBuilder();
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();

        if (fasttrack) {
            fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(FAST_TRACK_QUESTION_TYPE));
            etagCodeBuilder.append(FAST_TRACK_QUESTION_TYPE);
        } else {
            fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(QUESTION_TYPE));
            etagCodeBuilder.append(QUESTION_TYPE);
        }


        // defaults
        int newLimit = DEFAULT_RESULTS_LIMIT;
        int newStartIndex = 0;

        // options
        if (limit != null) {
            newLimit = limit;
        }

        if (startIndex != null) {
            newStartIndex = startIndex;
        }

        if (ids != null && !ids.isEmpty()) {
            List<String> idsList = Arrays.asList(ids.split(","));
            fieldsToMatch.put(ID_FIELDNAME, idsList);
            newLimit = idsList.size();
            etagCodeBuilder.append(ids);
        }

        if (tags != null && !tags.isEmpty()) {
            fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(tags.split(",")));
            etagCodeBuilder.append(tags);
        }

        if (level != null && !level.isEmpty()) {
            fieldsToMatch.put(LEVEL_FIELDNAME, Arrays.asList(level.split(",")));
            etagCodeBuilder.append(level);
        }

        // Calculate the ETag on last modified date of tags list
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + etagCodeBuilder.toString().hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }
        try {
            // Currently if you provide a search string we use a different
            // library call. This is because the previous one does not allow fuzzy
            // search.
            if (searchString != null && !searchString.isEmpty()) {
                ResultsWrapper<ContentDTO> c;

                c = api.segueSearch(searchString, this.contentIndex, fieldsToMatch, newStartIndex,
                            newLimit);

                ResultsWrapper<ContentSummaryDTO> summarizedContent = new ResultsWrapper<ContentSummaryDTO>(
                        this.extractContentSummaryFromList(c.getResults()),
                        c.getTotalResults());

                return Response.ok(summarizedContent).tag(etag)
                        .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                        .build();
            } else {
                return listContentObjects(fieldsToMatch, newStartIndex, newLimit).tag(etag)
                        .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).build();
            }
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
                    "Error locating the content requested", e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * Rest end point that gets a single question page based on a given id.
     * 
     * @param questionId
     *            to find as a string
     * @param request
     *            - so that we can do etag and cache resolution.
     * @param httpServletRequest
     *            - so that we can try and determine if the user is logged in. This will allow us to augment the
     *            question objects with any recorded state.
     * @return A Response object containing a question page object or a SegueErrorResponse.
     */
    @GET
    @Path("/questions/{question_page_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getQuestion(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, 
            @PathParam("question_page_id") final String questionId) {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();

        fieldsToMatch.put("type", Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));

        // options
        if (null != questionId) {
            fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(questionId));
        }
        AbstractSegueUserDTO user = userManager.getCurrentUser(httpServletRequest);
        Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts;

        try {
            userQuestionAttempts = questionManager.getQuestionAttemptsByUser(user);
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst trying to retrieve user question data";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }

        // Calculate the ETag
        EntityTag etag = new EntityTag(questionId.hashCode() + userQuestionAttempts.toString().hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Response response = this.findSingleResult(fieldsToMatch);

        if (response.getEntity() != null && response.getEntity() instanceof IsaacQuestionPageDTO) {
            SeguePageDTO content = (SeguePageDTO) response.getEntity();

            Map<String, String> logEntry = ImmutableMap.of(QUESTION_ID_LOG_FIELDNAME, content.getId(),
                    CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA());

            String userIdForRandomisation;
            if (user instanceof AnonymousUserDTO) {
                userIdForRandomisation = ((AnonymousUserDTO) user).getSessionId();
            } else {
                userIdForRandomisation = ((RegisteredUserDTO) user).getId().toString();
            }

            content = this.questionManager.augmentQuestionObjects(content, userIdForRandomisation,
                    userQuestionAttempts);

            // the request log
            getLogManager().logEvent(user, httpServletRequest, IsaacLogType.VIEW_QUESTION, logEntry);

            // return augmented content.
            return Response.ok(content)
                    .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                    .tag(etag)
                    .build();
        } else {
            String error = "Unable to locate a question with the id specified: " + questionId;
            log.warn(error);
            return SegueErrorResponse.getResourceNotFoundResponse(error);
        }
    }

    /**
     * Rest end point that gets a question summary page with augmented gameboard and question content..
     * 
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param pageId
     *            as a string
     * @return A Response object containing a page object or containing a SegueErrorResponse.
     */
    @GET
    @Path("question_summary/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getQuestionSummaryPage(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, @PathParam("page") final String pageId) {
        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + pageId.hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        
        try {
            Content contentDOById = this.contentManager.getContentDOById(
                    this.contentManager.getCurrentContentSHA(), pageId);

            if (!(contentDOById instanceof IsaacQuestionSummaryPage) || null == contentDOById) {
                return SegueErrorResponse.getResourceNotFoundResponse(String.format(
                        "Unable to locate question summary page with id: %s", pageId));
            }

            IsaacQuestionSummaryPage summaryPageDO = (IsaacQuestionSummaryPage) contentDOById;
            IsaacQuestionSummaryPageDTO summaryPageDTO = this.mapper.map(summaryPageDO,
                    IsaacQuestionSummaryPageDTO.class);
            // initialise collections
            summaryPageDTO.setTopBoards(new ArrayList<GameboardDTO>());
            summaryPageDTO.setFeaturedQuestions(new ArrayList<GameboardItem>());

            // augment gameboards
            for (String id : summaryPageDO.getTopBoards()) {
                try {
                    GameboardDTO liteGameboard = this.gameManager.getLiteGameboard(id);
                    if (liteGameboard != null) {
                        summaryPageDTO.getTopBoards().add(liteGameboard);    
                    } else {
                        log.error(String.format("Unable to locate gameboard requested for the summary page: %s.", id));
                    }
                    
                } catch (SegueDatabaseException e) {
                    log.info(String.format("Problem with retrieving gameboard: %s", id));
                }
            }

            // augment featuredQuestions
            for (GameboardItem id : summaryPageDO.getFeaturedQuestions()) {
                ContentDTO question = this.contentManager.getContentById(
                        this.contentManager.getCurrentContentSHA(), id.getId());
                if (!(question instanceof IsaacQuestionPageDTO) || question == null) {
                    log.error(String.format(
                            "Unable to locate question id: %s. Removing from the question summary page. ", id.getId()));
                }
                this.mapper.map(question, id);
                summaryPageDTO.getFeaturedQuestions().add(id);
            }

            Response cachableResult = Response.status(Status.OK).entity(summaryPageDTO)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
            return cachableResult;
        } catch (ContentManagerException e) {
            log.error("Content manager exception while trying to request question summary page.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve Content requested due to an internal server error.", e).toResponse();
        }
    }
    
    /**
     * Rest end point that gets a single page based on a given id.
     * 
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param pageId
     *            as a string
     * @return A Response object containing a page object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/{page}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getPage(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
            @PathParam("page") final String pageId) {
        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + pageId.hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(PAGE_TYPE, QUESTIONS_PAGE_TYPE));

        // options
        if (null != pageId) {
            fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(pageId));
        }

        Response result = this.findSingleResult(fieldsToMatch);

        if (result.getEntity() instanceof SeguePageDTO) {


            ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
                    .put(PAGE_ID_LOG_FIELDNAME, pageId)
                    .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();

            // the request log
            getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                    IsaacLogType.VIEW_PAGE, logEntry);
        }

        Response cachableResult = Response.status(result.getStatus()).entity(result.getEntity())
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
        return cachableResult;
    }

    /**
     * Rest end point that gets a single page fragment based on a given id.
     * 
     * @param request
     *            - so that we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param fragmentId
     *            as a string
     * @return A Response object containing a page fragment object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/fragments/{fragment_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getPageFragment(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
            @PathParam("fragment_id") final String fragmentId) {

        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + fragmentId.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(PAGE_FRAGMENT_TYPE));
        fieldsToMatch.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, Arrays.asList(fragmentId));

        Response result = this.findSingleResult(fieldsToMatch);

        getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                IsaacLogType.VIEW_PAGE_FRAGMENT, ImmutableMap.of(
                        FRAGMENT_ID_LOG_FIELDNAME, fragmentId,
                        CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()
                ));

        return Response.status(result.getStatus()).entity(result.getEntity())
                .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
    }

    /**
     * Rest end point that gets a all of the content marked as being type "pods".
     * 
     * @param request
     *            - so that we can deal with caching.
     * @return A Response object containing a page fragment object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/pods/{subject}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getPodList(@Context final Request request,
                                     @PathParam("subject") final String subject) {
        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + subject.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
            fieldsToMatch.put(TYPE_FIELDNAME, Arrays.asList(POD_FRAGMENT_TYPE));
            fieldsToMatch.put(TAGS_FIELDNAME, Arrays.asList(subject));

            ResultsWrapper<ContentDTO> pods = api.findMatchingContent(this.contentIndex,
                    SegueContentFacade.generateDefaultFieldToMatch(fieldsToMatch), 0, MAX_PODS_TO_RETURN);

            return Response.ok(pods).cacheControl(getCacheControl(NUMBER_SECONDS_IN_TEN_MINUTES, true))
                    .tag(etag)
                    .build();
        } catch (ContentManagerException e) {
            log.error("Content manager exception while trying to request the pods.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve Content requested due to an internal server error.", e).toResponse();
        }
    }

    /**
     * Utility method to allow related content to be populated as summary objects.
     * 
     * By default content summary objects may just have ids.
     * 
     * @param version
     *            - version of the content to use for augmentation.
     * @param contentToAugment
     *            - the content to augment.
     * @return content which has been augmented
     * @throws ContentManagerException
     *             - an exception when the content is not found
     */
    private ContentDTO augmentContentWithRelatedContent(final String version, final ContentDTO contentToAugment)
            throws ContentManagerException {
        return this.contentManager.populateRelatedContent(version, contentToAugment);
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
     * Utility method to convert a list of content objects into a list of ContentSummaryDTO Objects.
     * 
     * @param contentList
     *            - the list of content to summarise.
     * @return list of shorter ContentSummaryDTO objects.
     */
    private List<ContentSummaryDTO> extractContentSummaryFromList(final List<ContentDTO> contentList) {
        if (null == contentList) {
            return null;
        }

        List<ContentSummaryDTO> listOfContentInfo = new ArrayList<ContentSummaryDTO>();

        for (ContentDTO content : contentList) {
            ContentSummaryDTO contentInfo = extractContentSummary(content);
            if (null != contentInfo) {
                listOfContentInfo.add(contentInfo);
            }
        }
        return listOfContentInfo;
    }

    /**
     * For use when we expect to only find a single result.
     * 
     * By default related content ContentSummary objects will be fully augmented.
     * 
     * @param fieldsToMatch
     *            - expects a map of the form fieldname -> list of queries to match
     * @return A Response containing a single conceptPage or containing a SegueErrorResponse.
     */
    private Response findSingleResult(final Map<String, List<String>> fieldsToMatch) {
        try {
            ResultsWrapper<ContentDTO> resultList = api.findMatchingContent(this.contentIndex,
                    SegueContentFacade.generateDefaultFieldToMatch(fieldsToMatch), null, null); // includes
            // type
            // checking.
            ContentDTO c = null;
            if (resultList.getResults().size() > 1) {
                return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Multiple results ("
                        + resultList.getResults().size() + ") returned error. For search query: " + fieldsToMatch.values())
                        .toResponse();
            } else if (resultList.getResults().isEmpty()) {
                return new SegueErrorResponse(Status.NOT_FOUND, "No content found that matches the query with parameters: "
                        + fieldsToMatch.values()).toResponse();
            } else {
                c = resultList.getResults().get(0);
            }

            return Response.ok(this.augmentContentWithRelatedContent(this.contentIndex, c)).build();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * Helper method to query segue for a list of content objects.
     * 
     * This method will only use the latest version of the content.
     * 
     * @param fieldsToMatch
     *            - expects a map of the form fieldname -> list of queries to match
     * @param startIndex
     *            - the initial index for the first result.
     * @param limit
     *            - the maximums number of results to return
     * @return Response builder containing a list of content summary objects or containing a SegueErrorResponse
     */
    private Response.ResponseBuilder listContentObjects(final Map<String, List<String>> fieldsToMatch,
            final Integer startIndex, final Integer limit) throws ContentManagerException{
        ResultsWrapper<ContentDTO> c;

        c = api.findMatchingContent(this.contentIndex,
                SegueContentFacade.generateDefaultFieldToMatch(fieldsToMatch), startIndex, limit);

        ResultsWrapper<ContentSummaryDTO> summarizedContent = new ResultsWrapper<ContentSummaryDTO>(
                this.extractContentSummaryFromList(c.getResults()),
                c.getTotalResults());

        return Response.ok(summarizedContent);
    }
}
