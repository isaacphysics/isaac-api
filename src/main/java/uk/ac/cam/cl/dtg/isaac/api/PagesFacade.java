/*
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.api;

import com.google.api.client.util.Maps;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.glasnost.orika.MapperFacade;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.URIManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.UserAttemptManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacTopicSummaryPage;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.IsaacBookDetailPage;
import uk.ac.cam.cl.dtg.isaac.dos.content.IsaacRevisionDetailPage;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacBookIndexPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacConceptPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacPageFragmentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacTopicSummaryPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SearchResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.IsaacBookDetailPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.IsaacRevisionDetailPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.SeguePageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.services.ContentService;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.annotation.Nullable;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Pages Facade
 *
 * This class specifically caters for displaying isaac specific content pages.
 */
@Path("/pages")
@Tag(name = "PagesFacade", description = "/pages")
public class PagesFacade extends AbstractIsaacFacade {
    private static final Logger log = LoggerFactory.getLogger(PagesFacade.class);

    private final ContentService api;
    private final MapperFacade mapper;
    private final UserAccountManager userManager;
    private final URIManager uriManager;
    private final QuestionManager questionManager;
    private final GitContentManager contentManager;
    private final UserAttemptManager userAttemptManager;
    private final GameManager gameManager;

    /**
     * Creates an instance of the pages controller which provides the REST endpoints for accessing page content.
     *
     * @param api
     *            - Instance of ContentService
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
    public PagesFacade(final ContentService api, final AbstractConfigLoader propertiesLoader,
                       final ILogManager logManager, final MapperFacade mapper, final GitContentManager contentManager,
                       final UserAccountManager userManager, final URIManager uriManager,
                       final QuestionManager questionManager, final GameManager gameManager,
                       final UserAttemptManager userAttemptManager) {
        super(propertiesLoader, logManager);
        this.api = api;
        this.mapper = mapper;
        this.contentManager = contentManager;
        this.userManager = userManager;
        this.uriManager = uriManager;
        this.questionManager = questionManager;
        this.gameManager = gameManager;
        this.userAttemptManager = userAttemptManager;
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
    @Operation(summary = "List all concept page objects matching the provided criteria.")
    public final Response getConceptList(@Context final Request request, @QueryParam("ids") final String ids,
            @QueryParam("tags") final String tags,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("start_index") final Integer startIndex,
            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer limit) {
        Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
        fieldsToMatch.put(TYPE_FIELDNAME, List.of(CONCEPT_TYPE));

        StringBuilder etagCodeBuilder = new StringBuilder();

        Integer newLimit = null;

        if (limit != null) {
            newLimit = limit;
            etagCodeBuilder.append(limit);
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
        Map<String, BooleanOperator> booleanOperatorOverrideMap = ImmutableMap.of(TAGS_FIELDNAME, BooleanOperator.OR);

        // Calculate the ETag on last modified date of tags list
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode()
                + etagCodeBuilder.toString().hashCode() + "");

        Response cachedResponse = generateCachedResponse(request, etag);

        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            return listContentObjects(fieldsToMatch, booleanOperatorOverrideMap, startIndex, newLimit).tag(etag)
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
    @Operation(summary = "Get a concept page object by ID.")
    public final Response getConcept(@Context final Request request, @Context final HttpServletRequest servletRequest,
                                     @PathParam("concept_page_id") final String conceptId) {
        if (null == conceptId || conceptId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid concept id.").toResponse();
        }
        try {
            AbstractSegueUserDTO user = userManager.getCurrentUser(servletRequest);

            ContentDTO contentDTO = contentManager.getContentById(conceptId, true);
            if (contentDTO instanceof IsaacConceptPageDTO) {
                SeguePageDTO content = (SeguePageDTO) contentDTO;

                // Load attempts at related questions:
                Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> relatedQuestionAttempts;
                List<String> relatedQuestionIds = new ArrayList<>(getRelatedContentIds(content));
                if (user instanceof AnonymousUserDTO) {
                    // For anon users, must load all attempts:
                    relatedQuestionAttempts = questionManager.getQuestionAttemptsByUser(user);
                } else {
                    // For registered users, can load only relevant lightweight attempts:
                    RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;
                    relatedQuestionAttempts = questionManager.getMatchingLightweightQuestionAttempts(
                            registeredUser, relatedQuestionIds
                    );
                }

                // Check the cache status:
                EntityTag etag = new EntityTag(String.valueOf(
                        this.contentManager.getCurrentContentSHA().hashCode()
                        + conceptId.hashCode()
                        + relatedQuestionAttempts.hashCode()
                ));
                Response cachedResponse = generateCachedResponse(request, etag);
                if (cachedResponse != null) {
                    return cachedResponse;
                }

                // Augment related content with question attempts:
                augmentContentWithRelatedContent(content, relatedQuestionAttempts);

                // Log the request:
                ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
                        .put(CONCEPT_ID_LOG_FIELDNAME, conceptId)
                        .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA())
                        .build();
                getLogManager().logEvent(user, servletRequest, IsaacServerLogType.VIEW_CONCEPT, logEntry);

                return Response.ok(content)
                        .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                        .tag(etag)
                        .build();
            } else {
                String error = "Unable to locate a concept with the id specified: " + conceptId;
                log.warn(error);
                return SegueErrorResponse.getResourceNotFoundResponse(error);
            }

        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(
                Status.INTERNAL_SERVER_ERROR, "Database error while looking up user information.", e
            );
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(
                    Status.INTERNAL_SERVER_ERROR, "Error locating the content requested", e
            );
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     * REST end point to provide a list of questions.
     *
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
     * @param paramStartIndex
     *            - a string value to be converted into an integer which represents the start index of the results
     * @param paramLimit
     *            - a string value to be converted into an integer that represents the number of results to return.
     * @return A response object which contains a list of questions or an empty list.
     */
    @GET
    @Path("/questions")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List all question page objects matching the provided criteria.")
    public final Response getQuestionList(@Context final HttpServletRequest httpServletRequest,
            @QueryParam("ids") final String ids, @QueryParam("searchString") final String searchString,
            @QueryParam("tags") final String tags, @QueryParam("levels") final String level,
            @QueryParam("subjects") final String subjects, @QueryParam("fields") final String fields,
            @QueryParam("topics") final String topics,
            @QueryParam("stages") final String stages, @QueryParam("difficulties") final String difficulties,
            @QueryParam("examBoards") final String examBoards,
            @QueryParam("books") final String books, @QueryParam("questionCategories") final String questionCategories,
            @QueryParam("statuses") final String statuses,
            @DefaultValue("false") @QueryParam("fasttrack") final Boolean fasttrack,
            @DefaultValue(DEFAULT_START_INDEX_AS_STRING) @QueryParam("startIndex") final Integer paramStartIndex,
            @DefaultValue(DEFAULT_RESULTS_LIMIT_AS_STRING) @QueryParam("limit") final Integer paramLimit,
            @QueryParam("randomSeed") final Long randomSeed, @QueryParam("querySource") final String querySource) {
        Map<String, Set<String>> fieldsToMatch = Maps.newHashMap();
        Set<CompletionState> filterByStatuses;
        AbstractSegueUserDTO user;

        if (null != searchString && searchString.length() > SEARCH_TEXT_CHAR_LIMIT) {
            return SegueErrorResponse.getBadRequestResponse(
                    String.format("Search string exceeded %s character limit.", SEARCH_TEXT_CHAR_LIMIT));
        }

        // TODO: the limit ought to be lower when filtering by attempt status
        if (null != paramLimit && (paramLimit > MAX_SEARCH_RESULT_LIMIT || paramLimit <= 0)) {
            log.warn("Question search requested {} results!", paramLimit);
            return SegueErrorResponse.getBadRequestResponse("Unsupported search result limit!");
        }

        try {
            user = userManager.getCurrentUser(httpServletRequest);
        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(
                Status.INTERNAL_SERVER_ERROR, "Database error while looking up user information.", e
            ).toResponse();
        }

        // defaults
        int limit = SEARCH_RESULTS_PER_PAGE;
        int startIndex = 0;

        // options
        if (paramLimit != null) {
            limit = paramLimit;
        }

        if (paramStartIndex != null) {
            startIndex = paramStartIndex;
        }

        if (ids != null && !ids.isEmpty()) {
            Set<String> idsList = Set.of(ids.split(","));
            fieldsToMatch.put(ID_FIELDNAME, idsList);
            limit = idsList.size();
        }

        if (null == querySource || !QUESTION_SEARCH_LOG_SOURCE_IGNORES.contains(querySource)) {
            // Not an ImmutableMap since we may have null values, which will get nicely excluded from JSON.
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put(FIELDS_FIELDNAME, csvParamToLogValue(fields));
            logEntry.put(SUBJECTS_FIELDNAME, csvParamToLogValue(subjects));
            logEntry.put(TOPICS_FIELDNAME, csvParamToLogValue(topics));
            logEntry.put(BOOKS_FIELDNAME, csvParamToLogValue(books));
            logEntry.put(STAGES_FIELDNAME, csvParamToLogValue(stages));
            logEntry.put(DIFFICULTIES_FIELDNAME, csvParamToLogValue(difficulties));
            logEntry.put(EXAM_BOARDS_FIELDNAME, csvParamToLogValue(examBoards));
            logEntry.put(CATEGORIES_FIELDNAME, csvParamToLogValue(questionCategories));
            logEntry.put(TAGS_FIELDNAME, csvParamToLogValue(tags));
            logEntry.put(QUESTION_STATUSES_FIELDNAME, csvParamToLogValue(statuses));
            logEntry.put("levels", csvParamToLogValue(level));
            logEntry.put("questionIds", csvParamToLogValue(ids));
            logEntry.put(START_INDEX_FIELDNAME, String.valueOf(startIndex));
            logEntry.put(LIMIT_FIELDNAME, String.valueOf(limit));
            logEntry.put(SEARCH_STRING_FIELDNAME, !Objects.equals(searchString, "") ? searchString : null);
            logEntry.put("fasttrack", Objects.equals(fasttrack, true) ? String.valueOf(fasttrack) : null);
            logEntry.put("randomSeed", null != randomSeed ? String.valueOf(randomSeed) : null);
            logEntry.put("querySource", querySource);

            this.getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.QUESTION_FINDER_SEARCH, logEntry);
        }

        Map<String, String> fieldNameToValues = new HashMap<>();
        fieldNameToValues.put(ID_FIELDNAME, ids);
        fieldNameToValues.put(TAGS_FIELDNAME, tags);
        fieldNameToValues.put(SUBJECTS_FIELDNAME, subjects);
        fieldNameToValues.put(FIELDS_FIELDNAME, fields);
        fieldNameToValues.put(TOPICS_FIELDNAME, topics);
        fieldNameToValues.put(BOOKS_FIELDNAME, books);
        fieldNameToValues.put(LEVEL_FIELDNAME, level);
        fieldNameToValues.put(STAGE_FIELDNAME, stages);
        fieldNameToValues.put(DIFFICULTY_FIELDNAME, difficulties);
        fieldNameToValues.put(EXAM_BOARD_FIELDNAME, examBoards);
        fieldNameToValues.put(CATEGORIES_FIELDNAME, questionCategories);

        for (Map.Entry<String, String> entry : fieldNameToValues.entrySet()) {
            String fieldName = entry.getKey();
            String queryStringValue = entry.getValue();
            if (queryStringValue != null && !queryStringValue.isEmpty()) {
                fieldsToMatch.put(fieldName, Set.of(queryStringValue.split(",")));
            }
        }

        try {
            if (null == statuses) {
                // If statuses isn't a URL param, we won't augment or filter!
                filterByStatuses = null;
            } else if (statuses.isEmpty()) {
                // If statuses is blank, that shouldn't mean "please match nothing at all"; make it match everything.
                filterByStatuses = CompletionState.getAllStates();
            } else {
                filterByStatuses = Arrays.stream(statuses.split(","))
                        .map(CompletionState::valueOf)
                        .collect(Collectors.toSet());
            }
        } catch (IllegalArgumentException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Invalid question status filter provided.").toResponse();
        }

        String validatedSearchString = (null == searchString || searchString.isBlank()) ? null : searchString;

        // Show "nofilter" content to staff, superseded content to teachers, except when finding random questions:
        boolean showNoFilterContent = false;
        boolean showSupersededContent = false;
        boolean isRandomQuestion = Objects.equals(QUESTION_SEARCH_RANDOM_QUESTION, querySource);
        try {
            if (!isRandomQuestion && user instanceof RegisteredUserDTO) {
                showNoFilterContent = isUserStaff(userManager, (RegisteredUserDTO) user);
                showSupersededContent = isUserTeacherOrAbove(userManager, (RegisteredUserDTO) user);
            }
        } catch (NoUserLoggedInException e) {
            // This cannot happen!
        }

        List<ContentSummaryDTO> combinedResults = new ArrayList<>();
        int nextSearchStartIndex = startIndex;
        Long totalResults = 0L;

        List<ContentSummaryDTO> summarizedResults;

        for (int iterationLimit = 5;
             iterationLimit > 0 && (limit < 0 || combinedResults.size() < limit);
             iterationLimit--) {
            try {
                ResultsWrapper<ContentDTO> c;
                c = contentManager.questionSearch(
                        validatedSearchString,
                        randomSeed,
                        fieldsToMatch,
                        nextSearchStartIndex,
                        limit,
                        fasttrack,
                        showNoFilterContent,
                        showSupersededContent
                );

                summarizedResults = extractContentSummaryFromList(c.getResults());

                if (summarizedResults.isEmpty()) {
                    break;
                }

                List<ContentSummaryDTO> unfilteredSummarizedResults = new ArrayList<>(summarizedResults);

                if (null != filterByStatuses) {
                    // Only augment when filtering by statuses:
                    summarizedResults = userAttemptManager.augmentContentSummaryListWithAttemptInformation(user, summarizedResults);
                    // Optimise out unnecessary filtering:
                    if (!filterByStatuses.equals(CompletionState.getAllStates())) {
                        summarizedResults = summarizedResults.stream()
                                .filter(q -> filterByStatuses.contains(q.getState()))
                                .collect(Collectors.toList());
                    }
                }

                if (limit < 0 || combinedResults.size() + summarizedResults.size() <= limit) {
                    combinedResults.addAll(summarizedResults);
                    nextSearchStartIndex += unfilteredSummarizedResults.size();
                } else {
                    int remainingResults = limit - combinedResults.size();
                    combinedResults.addAll(summarizedResults.subList(0, remainingResults));
                    // we want to skip to the index of the last result added, *including the indices of the filtered results*.
                    // Because indices start at 0, cannot just add indices; we must add one to get the *number* of items to skip
                    nextSearchStartIndex += unfilteredSummarizedResults.indexOf(combinedResults.get(combinedResults.size() - 1)) + 1;
                }
                totalResults = c.getTotalResults();

            } catch (ContentManagerException e1) {
                SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND,
                        "Error locating the content requested", e1);
                log.error(error.getErrorMessage(), e1);
                return error.toResponse();
            } catch (SegueDatabaseException e2) {
                SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                        "SegueDatabaseException whilst trying to retrieve user data", e2);
                log.error(error.getErrorMessage(), e2);
                return error.toResponse();
            }
        }

        ResultsWrapper<ContentSummaryDTO> wrappedResults = new SearchResultsWrapper<>(
                combinedResults,
                totalResults,
                nextSearchStartIndex
        );

        return Response.ok(wrappedResults).build();
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
    @Operation(summary = "Get a question page object by ID.")
    public final Response getQuestion(@Context final Request request,
                                      @Context final HttpServletRequest httpServletRequest,
                                      @PathParam("question_page_id") final String questionId) {

        if (null == questionId || questionId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid question id.").toResponse();
        }

        try {
            AbstractSegueUserDTO user = userManager.getCurrentUser(httpServletRequest);

            ContentDTO contentDTO = contentManager.getContentById(questionId, true);

            if (contentDTO instanceof IsaacQuestionPageDTO) {
                SeguePageDTO content = (SeguePageDTO) contentDTO;

                String userIdForRandomisation;
                Map<String, Map<String, List<QuestionValidationResponse>>> questionAttempts;
                Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> relatedQuestionAttempts;
                // We have to cope with both anonymous and registered users:
                if (user instanceof AnonymousUserDTO) {
                    // For anonymous users, we just load all their question attempts.
                    userIdForRandomisation = ((AnonymousUserDTO) user).getSessionId();

                    Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts =
                            questionManager.getQuestionAttemptsByUser(user);
                    relatedQuestionAttempts = userQuestionAttempts;
                    questionAttempts = userQuestionAttempts;

                } else {
                    // For registered users, we can restrict our search to question attempts at this question
                    // (for which we need full validation responses), and attempts at related questions
                    // (for which we only need lightweight validation responses).
                    RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;
                    userIdForRandomisation = registeredUser.getId().toString();

                    List<String> relatedQuestionIds = new ArrayList<>(getRelatedContentIds(content));
                    relatedQuestionAttempts = questionManager.getMatchingLightweightQuestionAttempts(
                            registeredUser, relatedQuestionIds
                    );

                    questionAttempts = questionManager.getQuestionAttemptsByUserForQuestion(registeredUser, questionId);
                }

                // Check the cache status:
                EntityTag etag = new EntityTag(String.valueOf(
                        this.contentManager.getCurrentContentSHA().hashCode()
                        + questionId.hashCode()
                        + questionAttempts.toString().hashCode()
                        + relatedQuestionAttempts.toString().hashCode()
                ));
                Response cachedResponse = generateCachedResponse(request, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);
                if (cachedResponse != null) {
                    return cachedResponse;
                }

                // Then augment the page with attempt and related content information:
                augmentContentWithRelatedContent(content, relatedQuestionAttempts);
                questionManager.augmentQuestionObjects(content, userIdForRandomisation, questionAttempts);

                // Log the request:
                Map<String, String> logEntry = ImmutableMap.of(
                        QUESTION_ID_LOG_FIELDNAME, content.getId(),
                        CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA());
                getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.VIEW_QUESTION, logEntry);

                // Return augmented content:
                return Response.ok(content)
                        .cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false))
                        .tag(etag)
                        .build();
            } else {
                String error = "Unable to locate a question with the id specified: " + questionId;
                log.warn(error);
                return SegueErrorResponse.getResourceNotFoundResponse(error);
            }
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst trying to retrieve user data";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e) {
            String message = "Error whilst trying to load question content!";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }
    }


    /**
     *  Get and augment a topic summary.
     *
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param topicId
     *            as a string
     * @return A Response object containing a page object or containing a SegueErrorResponse.
     */
    @GET
    @Path("topics/{topic_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a topic summary with a list of related material.")
    public final Response getTopicSummaryPage(@Context final Request request,
                                              @Context final HttpServletRequest httpServletRequest,
                                              @PathParam("topic_id") final String topicId) {

        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        // Should this include the question attempts?
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + topicId.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        // Topic summary pages have the ID convention "topic_summary_[tag_name]"
        String summaryPageId = String.format("topic_summary_%s", topicId);

        try {
            // Load the summary page:
            Content contentDOById = this.contentManager.getContentDOById(summaryPageId, true);
            ContentDTO contentDTOById = this.contentManager.getContentDTOByDO(contentDOById);

            if (!(contentDOById instanceof IsaacTopicSummaryPage
                    && contentDTOById instanceof IsaacTopicSummaryPageDTO)) {
                return SegueErrorResponse.getResourceNotFoundResponse(String.format(
                        "Unable to locate topic summary page with id: %s", summaryPageId));
            }
            IsaacTopicSummaryPage topicSummaryDO = (IsaacTopicSummaryPage) contentDOById;
            IsaacTopicSummaryPageDTO topicSummaryDTO = (IsaacTopicSummaryPageDTO) contentDTOById;

            AbstractSegueUserDTO user = userManager.getCurrentUser(httpServletRequest);

            Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> relatedQuestionAttempts;
            // We have to cope with both anonymous and registered users:
            if (user instanceof AnonymousUserDTO) {
                relatedQuestionAttempts = questionManager.getQuestionAttemptsByUser(user);
            } else {
                List<String> relatedQuestionIds = getRelatedContentIds(topicSummaryDTO);
                relatedQuestionAttempts = questionManager.getMatchingLightweightQuestionAttempts(
                        (RegisteredUserDTO) user, relatedQuestionIds
                );
            }

            this.augmentContentWithRelatedContent(topicSummaryDTO, relatedQuestionAttempts);

            // Augment linked gameboards using the list in the DO:
            // FIXME: this requires loading both the DO and DTO separately, since augmenting things is hard right now.
            ArrayList<GameboardDTO> linkedGameboards = new ArrayList<>();
            if (null != topicSummaryDO.getLinkedGameboards()) {
                for (String linkedGameboardId : topicSummaryDO.getLinkedGameboards()) {
                    try {
                        GameboardDTO liteGameboard = this.gameManager.getLiteGameboard(linkedGameboardId);
                        if (liteGameboard != null) {
                            linkedGameboards.add(liteGameboard);
                        } else {
                            log.error(String.format("Unable to locate gameboard (%s) for topic summary page (%s)!",
                                    linkedGameboardId, topicId));
                        }

                    } catch (SegueDatabaseException e) {
                        log.info(String.format("Problem with retrieving gameboard: %s", linkedGameboardId));
                    }
                }
            }
            topicSummaryDTO.setLinkedGameboards(linkedGameboards);

            // Log the request:
            ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
                    .put(PAGE_ID_LOG_FIELDNAME, summaryPageId)
                    .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();
            getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.VIEW_TOPIC_SUMMARY_PAGE, logEntry);

            // If there are no question attempts, this is safe to cache.
            boolean isPublicData = relatedQuestionAttempts.isEmpty();

            return Response.status(Status.OK).entity(topicSummaryDTO)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, isPublicData)).tag(etag).build();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(
                    Status.INTERNAL_SERVER_ERROR, "Failed to load topic summary.", e
            ).toResponse();
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
    @Operation(summary = "Get a content page object by ID.")
    public final Response getPage(@Context final Request request, @Context final HttpServletRequest httpServletRequest,
            @PathParam("page") final String pageId) {

        if (null == pageId || pageId.isEmpty()) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a valid page id.").toResponse();
        }

        // Calculate the ETag on current live version of the content:
        EntityTag etag = new EntityTag(String.valueOf(
                this.contentManager.getCurrentContentSHA().hashCode() + pageId.hashCode()
        ));

        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            ContentDTO contentDTO = contentManager.getContentById(pageId, true);
            // We must not allow subclasses here, since general pages are the base class for all other page types!
            if (null != contentDTO && SeguePageDTO.class.equals(contentDTO.getClass())) {
                SeguePageDTO content = (SeguePageDTO) contentDTO;
                // Unlikely we want to augment with a user's actual question attempts. Use an empty Map.
                augmentContentWithRelatedContent(content, Collections.emptyMap());
                contentManager.populateSidebar(content);

                // the request log
                ImmutableMap<String, String> logEntry = ImmutableMap.of(
                        PAGE_ID_LOG_FIELDNAME, pageId,
                        CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()
                );
                getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                        IsaacServerLogType.VIEW_PAGE, logEntry);

                return Response.ok(content)
                        .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                        .tag(etag)
                        .build();
            } else {
                String error = "Unable to locate a page with the id specified: " + pageId;
                log.warn(error);
                return SegueErrorResponse.getResourceNotFoundResponse(error);
            }
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(
                    Status.INTERNAL_SERVER_ERROR, "Error locating the content requested", e
            );
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
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
    @Operation(summary = "Get a content page fragment by ID.")
    public final Response getPageFragment(@Context final Request request,
                                          @Context final HttpServletRequest httpServletRequest,
                                          @PathParam("fragment_id") final String fragmentId) {

        // Calculate the ETag on current live version of the content
        EntityTag etag = new EntityTag(String.valueOf(
            this.contentManager.getCurrentContentSHA().hashCode() + fragmentId.hashCode()
        ));
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }
        try {
            ContentDTO contentDTO = contentManager.getContentById(fragmentId, true);
            if (contentDTO instanceof IsaacPageFragmentDTO) {
                // Unlikely we want to augment with related content here!

                // the request log
                ImmutableMap<String, String> logEntry = ImmutableMap.of(
                        FRAGMENT_ID_LOG_FIELDNAME, fragmentId,
                        CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()
                );
                getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                        IsaacServerLogType.VIEW_PAGE, logEntry);

                return Response.ok(contentDTO)
                        .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                        .tag(etag)
                        .build();
            } else {
                String error = "Unable to locate a page fragment with the id specified: " + fragmentId;
                log.warn(error);
                return SegueErrorResponse.getResourceNotFoundResponse(error);
            }
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(
                    Status.INTERNAL_SERVER_ERROR, "Error locating the content requested", e
            );
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     * Endpoint that gets a single book index page from the given id.
     *
     * @param request
     *            - so that we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param bookId
     *            as a string
     * @return A Response object containing a page fragment object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/books/index/{book_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a book index page by ID.")
    public final Response getBookPage(@Context final Request request,
                                          @Context final HttpServletRequest httpServletRequest,
                                          @PathParam("book_id") final String bookId) {

        // Calculate the ETag on current live version of the content
        EntityTag etag = new EntityTag(String.valueOf(this.contentManager.getCurrentContentSHA().hashCode() + bookId.hashCode()));
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            ContentDTO contentDTO = contentManager.getContentById(bookId, true);
            if (contentDTO instanceof IsaacBookIndexPageDTO) {
                IsaacBookIndexPageDTO indexPageDTO = (IsaacBookIndexPageDTO) contentDTO;

                // Unlikely we want to augment with related content here!
                contentManager.populateSidebar(indexPageDTO);

                // Log the page view:
                getLogManager().logEvent(userManager.getCurrentUser(httpServletRequest), httpServletRequest,
                        IsaacServerLogType.VIEW_BOOK_INDEX_PAGE, ImmutableMap.of(
                                PAGE_ID_LOG_FIELDNAME, bookId,
                                CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()
                        ));

                return Response.ok(indexPageDTO)
                        .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true))
                        .tag(etag)
                        .build();
            } else {
                log.warn("Unable to locate a book index page with the id '{}'!", bookId);
                return SegueErrorResponse.getResourceNotFoundResponse("Unable to locate a book index page with the id specified!");
            }
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while processing request.");
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Error locating the content requested");
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        }
    }

    /**
     *   Endpoint that gets a single book detail page from the given id.
     *
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param bookPageId
     *            as a string
     * @return A Response object containing a page object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/books/page/{book_page_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a single book details page by ID.")
    public final Response getBookDetailPage(@Context final Request request,
                                              @Context final HttpServletRequest httpServletRequest,
                                              @PathParam("book_page_id") final String bookPageId) {

        // Calculate the ETag on current live version of the content
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + bookPageId.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            // Load the summary page:
            Content contentDOById = this.contentManager.getContentDOById(bookPageId, true);
            ContentDTO contentDTOById = this.contentManager.getContentDTOByDO(contentDOById);

            if (!(contentDOById instanceof IsaacBookDetailPage
                    && contentDTOById instanceof IsaacBookDetailPageDTO)) {
                return SegueErrorResponse.getResourceNotFoundResponse(String.format(
                        "Unable to locate book detail page with id: %s", bookPageId));
            }
            IsaacBookDetailPage bookPageDO = (IsaacBookDetailPage) contentDOById;
            IsaacBookDetailPageDTO bookPageDTO = (IsaacBookDetailPageDTO) contentDTOById;

            AbstractSegueUserDTO user = userManager.getCurrentUser(httpServletRequest);

            // Augment related content:
            this.augmentContentWithRelatedContent(bookPageDTO, Collections.emptyMap());
            contentManager.populateSidebar(bookPageDTO);

            // Augment linked gameboards using the list in the DO:
            // FIXME: this requires both the DO and DTO separately, since augmenting things is hard right now.
            List<String> gameboardIds = Objects.requireNonNullElse(bookPageDO.getGameboards(), Collections.emptyList());
            List<String> additionalGameboardIds = Objects.requireNonNullElse(bookPageDO.getExtensionGameboards(), Collections.emptyList());
            List<String> allGameboardIds = Stream.of(gameboardIds, additionalGameboardIds)
                    .flatMap(Collection::stream).collect(Collectors.toList());
            List<GameboardDTO> linkedGameboards = gameManager.getGameboards(allGameboardIds);

            bookPageDTO.setGameboards(linkedGameboards
                    .stream()
                    .filter(gb -> gameboardIds.contains(gb.getId()))
                    .sorted(Comparator.comparingInt(o -> gameboardIds.indexOf(o.getId())))  // maintain original order
                    .collect(Collectors.toList())
            );

            bookPageDTO.setExtensionGameboards(linkedGameboards
                    .stream()
                    .filter(gb -> additionalGameboardIds.contains(gb.getId()))
                    .sorted(Comparator.comparingInt(o -> additionalGameboardIds.indexOf(o.getId())))
                    .collect(Collectors.toList())
            );

            // Log the request:
            ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
                    .put(PAGE_ID_LOG_FIELDNAME, bookPageId)
                    .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();
            getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.VIEW_BOOK_DETAIL_PAGE, logEntry);


            return Response.status(Status.OK).entity(bookPageDTO)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Failed to load book detail page.").toResponse();
        }
    }

    /**
     *   Endpoint that gets a revision detail page from the given id.
     *
     * @param request
     *            - so we can deal with caching.
     * @param httpServletRequest
     *            - so that we can extract user information.
     * @param revisionPageId
     *            as a string
     * @return A Response object containing a page object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/revision/detail/{revision_page_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a revision detail page by ID.")
    public final Response getRevisionDetailPage(@Context final Request request,
                                            @Context final HttpServletRequest httpServletRequest,
                                            @PathParam("revision_page_id") final String revisionPageId) {

        // Calculate the ETag on current live version of the content
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + revisionPageId.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            // Load the summary page:
            Content contentDOById = this.contentManager.getContentDOById(revisionPageId, true);
            ContentDTO contentDTOById = this.contentManager.getContentDTOByDO(contentDOById);

            if (!(contentDOById instanceof IsaacRevisionDetailPage
                    && contentDTOById instanceof IsaacRevisionDetailPageDTO)) {
                return SegueErrorResponse.getResourceNotFoundResponse(String.format(
                        "Unable to locate revision detail page with id: %s", revisionPageId));
            }
            IsaacRevisionDetailPage revisionPage = (IsaacRevisionDetailPage) contentDOById;
            IsaacRevisionDetailPageDTO revisionPageDTO = (IsaacRevisionDetailPageDTO) contentDTOById;

            AbstractSegueUserDTO user = userManager.getCurrentUser(httpServletRequest);

            // Augment related content, without user-specific question attempts:
            this.augmentContentWithRelatedContent(revisionPageDTO, Collections.emptyMap());
            contentManager.populateSidebar(revisionPageDTO);

            // Augment linked gameboards using the list in the DO:
            // FIXME: this requires both the DO and DTO separately, since augmenting things is hard right now.
            List<String> gameboardIds = Objects.requireNonNullElse(revisionPage.getGameboards(), Collections.emptyList());
            List<GameboardDTO> linkedGameboards = gameManager.getGameboards(gameboardIds);
            revisionPageDTO.setGameboards(linkedGameboards);

            // Log the request:
            ImmutableMap<String, String> logEntry = new ImmutableMap.Builder<String, String>()
                    .put(PAGE_ID_LOG_FIELDNAME, revisionPageId)
                    .put(CONTENT_VERSION_FIELDNAME, this.contentManager.getCurrentContentSHA()).build();
            getLogManager().logEvent(user, httpServletRequest, IsaacServerLogType.VIEW_REVISION_DETAIL_PAGE, logEntry);


            return Response.status(Status.OK).entity(revisionPageDTO)
                    .cacheControl(getCacheControl(NUMBER_SECONDS_IN_ONE_HOUR, true)).tag(etag).build();
        } catch (SegueDatabaseException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Database error while looking up user information.", e);
            log.error(error.getErrorMessage(), e);
            return error.toResponse();
        } catch (ContentManagerException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Failed to load revision detail page.").toResponse();
        }
    }

    /**
     * Rest endpoint retrieving the first MAX_PODS_TO_RETURN pods by ID.
     *
     * @param request
     *            - so that we can deal with caching.
     * @return A Response object containing a page fragment object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/pods/{subject}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List pods matching the subject provided.")
    public final Response getPodList(@Context final Request request,
                                     @PathParam("subject") final String subject) {
        return getPodList(request, subject, 0);
    }

    /**
     * Rest endpoint retrieving MAX_PODS_TO_RETURN pods, sorted by ID, starting from startIndex.
     * 
     * @param request
     *            - so that we can deal with caching.
     * @return A Response object containing a page fragment object or containing a SegueErrorResponse.
     */
    @GET
    @Path("/pods/{subject}/{startIndex}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List pods matching the subject provided.")
    public final Response getPodList(@Context final Request request,
                                     @PathParam("subject") final String subject,
                                     @PathParam("startIndex") final int startIndex) {
        // Calculate the ETag on current live version of the content
        // NOTE: Assumes that the latest version of the content is being used.
        EntityTag etag = new EntityTag(this.contentManager.getCurrentContentSHA().hashCode() + subject.hashCode() + "");
        Response cachedResponse = generateCachedResponse(request, etag);
        if (cachedResponse != null) {
            return cachedResponse;
        }

        try {
            Map<String, List<String>> fieldsToMatch = Maps.newHashMap();
            fieldsToMatch.put(TYPE_FIELDNAME, List.of(POD_FRAGMENT_TYPE));
            fieldsToMatch.put(TAGS_FIELDNAME, List.of(subject));

            Map<String, SortOrder> sortInstructions = new HashMap<>();
            sortInstructions.put("id.raw", SortOrder.DESC); // Sort by ID (i.e. most recent; all pod ids start yyyymmdd)
            // We would ideally also sort by presence of 'featured' tag, tricky with current implementation

            ResultsWrapper<ContentDTO> pods = api.findMatchingContent(
                    ContentService.generateDefaultFieldToMatch(fieldsToMatch), startIndex, MAX_PODS_TO_RETURN,
                    sortInstructions);

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
     * Return the IDs of all related content for a content object, including those of nested children.
     *
     * Note that some IDs returned may not be question page IDs, but may be concept pages etc.
     *
     * @param content
     * @return
     */
    private List<String> getRelatedContentIds(final ContentDTO content) {
        List<String> relatedContent = new ArrayList<>();

        if (null != content.getRelatedContent()) {
            // This might include concept page IDs too.
            relatedContent.addAll(content
                    .getRelatedContent()
                    .stream()
                    .map(ContentSummaryDTO::getId)
                    .collect(Collectors.toList()));
        }

        List<ContentBaseDTO> children = content.getChildren();
        if (children != null) {
            for (ContentBaseDTO child : children) {
                if (child instanceof ContentDTO) {
                    ContentDTO childContent = (ContentDTO) child;
                    relatedContent.addAll(getRelatedContentIds(childContent));
                }
            }
        }
        return relatedContent;
    }

    /**
     * Utility method to allow related content to be populated as summary objects.
     *
     * By default content summary objects may just have ids.
     *
     * @param contentToAugment
     *            - the content to augment.
     * @param usersQuestionAttempts
     *            - nullable question attempt information to support augmentation of content.
     * @throws ContentManagerException
     *             - an exception when the content is not found
     */
    public void augmentContentWithRelatedContent(final ContentDTO contentToAugment,
            @Nullable Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> usersQuestionAttempts)
        throws ContentManagerException {

        ContentDTO augmentedDTO = this.contentManager.populateRelatedContent(contentToAugment);

        if (usersQuestionAttempts != null) {
            UserAttemptManager.augmentRelatedQuestionsWithAttemptInformation(augmentedDTO, usersQuestionAttempts);
        }
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
        GitContentManager.populateContentSummaryValues(content, contentInfo);

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

        List<ContentSummaryDTO> listOfContentInfo = new ArrayList<>();

        for (ContentDTO content : contentList) {
            ContentSummaryDTO contentInfo = extractContentSummary(content);
            if (null != contentInfo) {
                listOfContentInfo.add(contentInfo);
            }
        }
        return listOfContentInfo;
    }

    /**
     * Helper method to query segue for a list of content objects.
     * 
     * This method will only use the latest version of the content.
     * 
     * @param fieldsToMatch
     *            - expects a map of the form fieldname -> list of queries to match
     * @param booleanOperatorOverrideMap
     *            - an optional map of the form fieldname -> one of 'AND', 'OR' or 'NOT', to specify the
     *              type of matching needed for that field. Overrides any other default matching behaviour
     *              for the given fields
     * @param startIndex
     *            - the initial index for the first result.
     * @param limit
     *            - the maximums number of results to return
     * @return Response builder containing a list of content summary objects or containing a SegueErrorResponse
     */
    private Response.ResponseBuilder listContentObjects(final Map<String,
                List<String>> fieldsToMatch,
                @Nullable final Map<String, BooleanOperator> booleanOperatorOverrideMap,
                final Integer startIndex,
                final Integer limit)
            throws ContentManagerException {
        ResultsWrapper<ContentDTO> c;

        c = api.findMatchingContent(
                ContentService.generateDefaultFieldToMatch(fieldsToMatch, booleanOperatorOverrideMap),
                startIndex,
                limit
        );

        ResultsWrapper<ContentSummaryDTO> summarizedContent = new ResultsWrapper<>(
                this.extractContentSummaryFromList(c.getResults()),
                c.getTotalResults());

        return Response.ok(summarizedContent);
    }

    /**
     * Convert an optional comma-separated URL parameter into a value suitable for logging.
     * @param urlParam - the URL parameter value, potentially null or empty.
     * @return
     */
    private Object csvParamToLogValue(final String urlParam) {
        if (null == urlParam || urlParam.isEmpty()) {
            return null;
        }
        return urlParam.split(",");
    }
}
