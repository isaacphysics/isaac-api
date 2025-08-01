/*
 * Copyright 2015 Stephen Cummins
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

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateGameboardException;
import uk.ac.cam.cl.dtg.isaac.api.managers.FastTrackManger;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.InvalidGameboardException;
import uk.ac.cam.cl.dtg.isaac.api.managers.NoWildcardException;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardListDTO;
import uk.ac.cam.cl.dtg.isaac.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AnonymousUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Games boards Facade.
 */
@Path("/")
@Tag(name = "GameboardsFacade", description = "/gameboards")
public class GameboardsFacade extends AbstractIsaacFacade {
    private GameManager gameManager;
    private UserAccountManager userManager;

    private static final Logger log = LoggerFactory.getLogger(GameboardsFacade.class);
    private final QuestionManager questionManager;

    private final FastTrackManger fastTrackManger;

    private static final String VALID_GAMEBOARD_ID_REGEX = "^[a-z0-9_-]+$";

    private static List<String> splitCsvStringQueryParam(final String queryParamCsv) {
        if (null != queryParamCsv && !queryParamCsv.isEmpty()) {
            return Arrays.asList(queryParamCsv.split(","));
        } else {
            return null;
        }
    }

    /**
     * GamesFacade. For management of gameboards etc.
     * 
     * @param properties
     *            - global properties map
     * @param logManager
     *            - for managing logs.
     * @param gameManager
     *            - for games interaction
     * @param questionManager
     *            - for question content
     * @param userManager
     *            - to get user details
     */
    @Inject
    public GameboardsFacade(final AbstractConfigLoader properties, final ILogManager logManager,
                            final GameManager gameManager, final QuestionManager questionManager,
                            final UserAccountManager userManager, final FastTrackManger fastTrackManger) {
        super(properties, logManager);

        this.gameManager = gameManager;
        this.questionManager = questionManager;
        this.userManager = userManager;
        this.fastTrackManger = fastTrackManger;
    }

    /**
     * REST end point to provide a Temporary Gameboard stored in volatile storage.
     * 
     * @param request
     *            - this allows us to check to see if a user is currently loggedin.
     * @param title
     *            - the title of the generated board
     * @param subjects
     *            - a comma separated list of subjects
     * @param fields
     *            - a comma separated list of fields
     * @param topics
     *            - a comma separated list of topics
     * @param levels
     *            - a comma separated list of levels
     * @param concepts
     *            - a comma separated list of conceptIds
     * @param questionCategories
     *            - a comma separated list of question categories
     * @return a Response containing a gameboard object or containing a SegueErrorResponse.
     */
    @Deprecated
    @GET
    @Path("gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get a temporary board of questions matching provided constraints.")
    public final Response generateTemporaryGameboard(@Context final HttpServletRequest request,
            @QueryParam("title") final String title, @QueryParam("subjects") final String subjects,
            @QueryParam("fields") final String fields, @QueryParam("topics") final String topics,
            @QueryParam("stages") final String stages, @QueryParam("difficulties") final String difficulties,
            @QueryParam("examBoards") final String examBoards, @QueryParam("levels") final String levels,
            @QueryParam("concepts") final String concepts, @QueryParam("questionCategories") final String questionCategories) {
        List<String> subjectsList = splitCsvStringQueryParam(subjects);
        List<String> fieldsList = splitCsvStringQueryParam(fields);
        List<String> topicsList = splitCsvStringQueryParam(topics);
        List<Integer> levelsList = null;
        List<String> stagesList = splitCsvStringQueryParam(stages);
        List<String> difficultiesList = splitCsvStringQueryParam(difficulties);
        List<String> examBoardsList = splitCsvStringQueryParam(examBoards);
        List<String> conceptsList = splitCsvStringQueryParam(concepts);
        List<String> questionCategoriesList = splitCsvStringQueryParam(questionCategories);

        if (null != levels && !levels.isEmpty()) {
            String[] levelsAsString = levels.split(",");

            levelsList = Lists.newArrayList();
            for (String s : levelsAsString) {
                try {
                    levelsList.add(Integer.parseInt(s));
                } catch (NumberFormatException e) {
                    return new SegueErrorResponse(Status.BAD_REQUEST, "Levels must be numbers if specified.", e)
                            .toResponse();
                }
            }
        }

        try {
            log.warn("Method generateTemporaryGameboard was called by an API request!");
            AbstractSegueUserDTO boardOwner = this.userManager.getCurrentUser(request);
            GameboardDTO gameboard;

            gameboard = gameManager.generateRandomGameboard(title, subjectsList, fieldsList, topicsList, levelsList,
                    conceptsList, questionCategoriesList, stagesList, difficultiesList, examBoardsList,boardOwner);

            if (null == gameboard) {
                return new SegueErrorResponse(Status.NO_CONTENT,
                        "We cannot find any questions based on your filter criteria.").toResponse();
            }

            return Response.ok(gameboard).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
        } catch (IllegalArgumentException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Your gameboard filter request is invalid.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "SegueDatabaseException whilst generating a gameboard";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * REST end point to retrieve a specific gameboard by Id.
     * 
     * @param request
     *            - so that we can deal with caching and etags.
     * @param httpServletRequest
     *            - so that we can extract the users session information if available.
     * @param gameboardId
     *            - the unique id of the gameboard to be requested
     * @return a Response containing a gameboard object or containing a SegueErrorResponse.
     */
    @GET
    @Path("gameboards/{gameboard_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the details of a gameboard.")
    public final Response getGameboard(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, @PathParam("gameboard_id") final String gameboardId) {

        try {
            GameboardDTO gameboard;

            AbstractSegueUserDTO randomUser = this.userManager.getCurrentUser(httpServletRequest);

            GameboardDTO unAugmentedGameboard = gameManager.getLiteGameboard(gameboardId);
            if (null == unAugmentedGameboard) {
                return new SegueErrorResponse(Status.NOT_FOUND, "No Gameboard found for the id specified.")
                        .toResponse();
            }

            Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> userQuestionAttempts;

            if (randomUser instanceof AnonymousUserDTO) {
                userQuestionAttempts = this.questionManager.getQuestionAttemptsByUser(randomUser);
            } else {
                List<String> gameboardPageIds = unAugmentedGameboard.getContents().stream().map(GameboardItem::getId).collect(Collectors.toList());
                userQuestionAttempts = this.questionManager.getMatchingLightweightQuestionAttempts((RegisteredUserDTO) randomUser, gameboardPageIds);
            }

            // Calculate the ETag
            EntityTag etag = new EntityTag(unAugmentedGameboard.toString().hashCode()
                    + userQuestionAttempts.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag, NEVER_CACHE_WITHOUT_ETAG_CHECK);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            // attempt to augment the gameboard with user information.
            gameboard = gameManager.getGameboard(gameboardId, randomUser, userQuestionAttempts);

            // We decided not to log this on the backend as the front end uses this lots.
            return Response.ok(gameboard).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).tag(etag)
                    .build();
        } catch (IllegalArgumentException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "Your gameboard filter request is invalid.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to access the gameboard in the database.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * REST endpoint to retrieve every question (and its status) associated with a particular FastTrack gameboard and
     * history.
     *
     * @param request usually used for caching.
     * @param httpServletRequest so that we can extract the users session information if available.
     * @param gameboardId the unique id of the FastTrack gameboard which links the questions.
     * @param currentConceptTitle the concept title that the user is currently working on.
     * @param upperQuestionId the latest upper level question that is in the history.
     * @return a Response containing a list of augmented gameboard items for the gamebaord-concept pair or an error.
     */
    @GET
    @Path("fasttrack/{gameboard_id}/concepts")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Get the progress of the current user at a FastTrack gameboard.")
    public final Response getFastTrackConceptFromHistory(@Context final Request request,
                                                         @Context final HttpServletRequest httpServletRequest,
                                                         @PathParam("gameboard_id") final String gameboardId,
                                                         @QueryParam("concept") final String currentConceptTitle,
                                                         @QueryParam("upper_question_id") final String upperQuestionId) {

        try {
            if (!fastTrackManger.isValidFasTrackGameboardId(gameboardId)) {
                return new SegueErrorResponse(Status.NOT_FOUND, "Gameboard id not a valid FastTrack gameboard id.")
                        .toResponse();
            }
            AbstractSegueUserDTO currentUser = this.userManager.getCurrentUser(httpServletRequest);
            GameboardDTO gameboard = this.gameManager.getGameboard(gameboardId);

            Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts =
                    this.questionManager.getQuestionAttemptsByUser(currentUser);

            List<GameboardItem> conceptQuestionsProgress = Lists.newArrayList();
            if (upperQuestionId.isEmpty()) {
                List<FASTTRACK_LEVEL> upperAndLower = Arrays.asList(FASTTRACK_LEVEL.ft_upper, FASTTRACK_LEVEL.ft_lower);
                conceptQuestionsProgress.addAll(fastTrackManger.getConceptProgress(
                        gameboard, upperAndLower, currentConceptTitle, userQuestionAttempts));
            } else {
                String upperConceptTitle = fastTrackManger.getConceptFromQuestionId(upperQuestionId);
                conceptQuestionsProgress.addAll(fastTrackManger.getConceptProgress(
                        gameboard, Collections.singletonList(FASTTRACK_LEVEL.ft_upper), upperConceptTitle, userQuestionAttempts));
                conceptQuestionsProgress.addAll(fastTrackManger.getConceptProgress(
                        gameboard, Collections.singletonList(FASTTRACK_LEVEL.ft_lower), currentConceptTitle, userQuestionAttempts));
            }

            return Response.ok(conceptQuestionsProgress).build();

        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to access the FastTrack progress in the database.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(
                    Status.NOT_FOUND, "Error locating the version requested", e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }
    }

    /**
     * createGameboard.
     *
     * @param request
     *            - for getting the user information
     * @param newGameboardObject
     *            - the new gameboard to save in the database
     * @return Gameboard DTO which has been persisted.
     */
    @POST
    @Path("gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a new gameboard from a template GameboardDTO.",
                  description = "Only staff users can provide custom IDs, wildcards or tags.")
    public final Response createGameboard(
            @Context final HttpServletRequest request,
            final GameboardDTO newGameboardObject) {

        RegisteredUserDTO user;

        try {
            user = userManager.getCurrentRegisteredUser(request);

            if (null == newGameboardObject) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a gameboard object").toResponse();
            }

            if ((newGameboardObject.getId() != null || !newGameboardObject.getTags().isEmpty()
                    || newGameboardObject.getWildCard() != null) && !isUserStaff(userManager, user)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot provide a gameboard wildcard, ID or tags.").toResponse();
            }
            
            if (newGameboardObject.getId() != null && !newGameboardObject.getId().matches(VALID_GAMEBOARD_ID_REGEX)) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "Invalid gameboard ID provided").toResponse();
            }
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        newGameboardObject.setCreationMethod(GameboardCreationMethod.BUILDER);
        GameboardDTO persistedGameboard;
        try {
            persistedGameboard = gameManager.saveNewGameboard(newGameboardObject, user);

        } catch (InvalidGameboardException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, String.format("The gameboard you provided is invalid"), e)
                    .toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Database Error whilst trying to save the gameboard.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (DuplicateGameboardException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, String.format(
                    "Gameboard with that id (%s) already exists. ", newGameboardObject.getId())).toResponse();
        } catch (ContentManagerException e) {
            String message = "Content Error whilst trying to save the gameboard.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        }

        this.getLogManager().logEvent(user, request, IsaacServerLogType.CREATE_GAMEBOARD,
                ImmutableMap.of(GAMEBOARD_ID_FKEY, persistedGameboard.getId()));

        return Response.ok(persistedGameboard).build();
    }

    /**
     * REST end point to allow gameboards to be persisted into permanent storage and for the title to be updated by
     * users.
     * 
     * Currently we only support updating the title and saving the gameboard that exists in temporary storage into
     * permanent storage. No other fields can be updated at the moment.
     * 
     * TODO: This will need to change if we want to change more than the board title.
     * 
     * @param request
     *            - so that we can find out the currently logged in user
     * @param gameboardId
     *            - So that we can look up an existing gameboard to modify.
     * @param newGameboardTitle
     *            - a string containing the new title to save the gameboard with
     * @return a Response containing a list of gameboard objects or containing a SegueErrorResponse.
     */
    @POST
    @Path("gameboards/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "Change a gameboards title and link the current user to it.",
                  description = "It is only possible to change the name of a gameboard.")
    public final Response renameAndSaveGameboard(@Context final HttpServletRequest request,
            @PathParam("id") final String gameboardId, @QueryParam("title") final String newGameboardTitle) {

        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        if (null == newGameboardTitle || null == gameboardId) {
            // Gameboard title and id must be there
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You must provide a new gameboard title and the "
                            + "id of the gameboard object in the endpoint path.").toResponse();
        }

        // Find the existing gameboard with the id given
        GameboardDTO existingGameboard;
        try {
            existingGameboard = gameManager.getGameboard(gameboardId);
            if (null == existingGameboard) {
                // there is no gameboard to persist (the id does not belong to an existing gameboard)
                return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                        "A gameboard with that id was not found.").toResponse();
            }
            // go ahead and persist the gameboard (if it is only temporary) / link it to the users my boards account
            gameManager.linkUserToGameboard(existingGameboard, user);
            getLogManager().logEvent(user, request, IsaacServerLogType.ADD_BOARD_TO_PROFILE,
                    ImmutableMap.of(GAMEBOARD_ID_FKEY, existingGameboard.getId()));

        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error whilst trying to access the gameboard database.", e).toResponse();
        }

        // Now check if the current title is either null, or not equal to the new title. If so, we want to change it
        if (null == existingGameboard.getTitle() || !existingGameboard.getTitle().equals(newGameboardTitle)) {

            // do they have permission?
            if (null != existingGameboard.getOwnerUserId()
                    && !existingGameboard.getOwnerUserId().equals(user.getId())) {
                // user not logged in return not authorized
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not allowed to change another user's gameboard.").toResponse();
            }

            // We can now change the title, and persist this change to the database
            GameboardDTO updatedGameboard;
            try {
                existingGameboard.setTitle(newGameboardTitle);
                updatedGameboard = gameManager.updateGameboardTitle(existingGameboard);
            } catch (SegueDatabaseException e) {
                String message = "Error whilst trying to change the gameboards title.";
                log.error(message, e);
                return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
            } catch (InvalidGameboardException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        String.format("The gameboard you provided is invalid: " + e.getMessage()), e).toResponse();
            }
            return Response.ok(updatedGameboard).build();
        }
        return Response.ok(existingGameboard).build();
    }

    /**
     * REST end point to find all of a user's gameboards. The My Boards endpoint.
     * 
     * @param request
     *            - so that we can find out the currently logged in user
     * @param startIndex
     *            - the first board index to return.
     * @param limit
     *            - the number of gameboards to return.
     * @param sortInstructions
     *            - the criteria to use for sorting. Default is reverse chronological by created date.
     * @param showCriteria
     *            - e.g. all_correct, all_attempted
     * @return a Response containing a list of gameboard objects or a noContent Response.
     */
    @GET
    @Path("gameboards/user_gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List all gameboards linked to the current user.")
    public final Response getGameboardsByCurrentUser(@Context final HttpServletRequest request,
            @QueryParam("start_index") final String startIndex, @QueryParam("limit") final String limit,
            @QueryParam("sort") final String sortInstructions, @QueryParam("show_only") final String showCriteria) {
        RegisteredUserDTO currentUser;

        try {
            currentUser = userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        Integer gameboardLimit;
        if (limit != null) {
            try {
                if (limit.equals(Constants.ALL_BOARDS)) {
                    gameboardLimit = null;
                } else {
                    gameboardLimit = Integer.parseInt(limit);
                }
            } catch (NumberFormatException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "The number you entered as the results limit is not valid.").toResponse();
            }
        } else {
            gameboardLimit = Constants.DEFAULT_GAMEBOARDS_RESULTS_LIMIT;
        }

        Integer startIndexAsInteger = 0;
        if (startIndex != null) {
            try {
                startIndexAsInteger = Integer.parseInt(startIndex);
            } catch (NumberFormatException e) {
                return new SegueErrorResponse(Status.BAD_REQUEST,
                        "The number you entered as the start_index is not valid.").toResponse();
            }
        }

        GameboardState gameboardShowCriteria = null;
        if (showCriteria != null) {
            switch (showCriteria.toLowerCase()) {
                case "all_correct":
                    gameboardShowCriteria = GameboardState.ALL_CORRECT;
                    break;
                case "all_attempted":
                    gameboardShowCriteria = GameboardState.ALL_ATTEMPTED;
                    break;
                case "in_progress":
                    gameboardShowCriteria = GameboardState.IN_PROGRESS;
                    break;
                case "not_attempted":
                    gameboardShowCriteria = GameboardState.NOT_ATTEMPTED;
                    break;
                default:
                    return new SegueErrorResponse(Status.BAD_REQUEST, "Unable to interpret showOnly criteria specified "
                            + showCriteria).toResponse();
            }
        }

        List<Map.Entry<String, SortOrder>> parsedSortInstructions = null;
        // sort instructions
        if (sortInstructions != null && !sortInstructions.isEmpty()) {
            parsedSortInstructions = Lists.newArrayList();
            for (String instruction : sortInstructions.toLowerCase().split(",")) {
                SortOrder s = SortOrder.ASC;
                if (instruction.startsWith("-")) {
                    s = SortOrder.DESC;
                    instruction = instruction.substring(1);
                }

                switch (instruction) {
                    case "created":
                        parsedSortInstructions.add(immutableEntry(CREATED_DATE_FIELDNAME, s));
                        break;
                    case "visited":
                        parsedSortInstructions.add(immutableEntry(VISITED_DATE_FIELDNAME, s));
                        break;
                    case "title":
                        parsedSortInstructions.add(immutableEntry(TITLE_FIELDNAME, s));
                        break;
                    case "attempted":
                        parsedSortInstructions.add(immutableEntry(PERCENTAGE_ATTEMPTED_FIELDNAME, s));
                        break;
                    case "correct":
                        parsedSortInstructions.add(immutableEntry(PERCENTAGE_CORRECT_FIELDNAME, s));
                        break;
                    default:
                        return new SegueErrorResponse(Status.BAD_REQUEST, "Sorry we do not recognise the sort instruction "
                                + instruction).toResponse();
                }
            }
        }

        GameboardListDTO gameboards;
        try {
            gameboards = gameManager.getUsersGameboards(currentUser, startIndexAsInteger, gameboardLimit,
                    gameboardShowCriteria, parsedSortInstructions);
        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to access the gameboard in the database.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }

        getLogManager().logEvent(
                currentUser,
                request,
                IsaacServerLogType.VIEW_MY_BOARDS_PAGE,
                ImmutableMap.builder().put("totalBoards", gameboards.getTotalResults())
                        .put("notStartedTotal", gameboards.getTotalNotStarted())
                        .put("allAttemptedTotal", gameboards.getTotalAllAttempted())
                        .put("inProgressTotal", gameboards.getTotalInProgress()).build());

        return Response.ok(gameboards).cacheControl(getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).build();
    }

    /**
     * REST end point to allow gameboards to be persisted into permanent storage and for it to be added to the users' my
     * boards collection.
     * 
     * @param request
     *            - so that we can find out the currently logged in user
     * @param gameboardId
     *            - So that we can look up an existing gameboard to modify.
     * @return a Response containing a list of gameboard objects or containing a SegueErrorResponse.
     */
    @POST
    @Path("gameboards/user_gameboards/{gameboard_id}")
    @Operation(summary = "Link a gameboard to the current user.",
                  description = "This will save a persistent copy of the gameboard if it was a temporary board.")
    public final Response linkUserToGameboard(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId) {

        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        if (null == gameboardId) {
            // Gameboard object must be there and have an id.
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You must provide a gameboard id to be able to link to it.").toResponse();
        }

        // find the existing gameboard.
        GameboardDTO existingGameboard;
        try {
            existingGameboard = gameManager.getGameboard(gameboardId);

            if (null == existingGameboard) {
                return new SegueErrorResponse(Status.NOT_FOUND, "No gameboard found for that id.").toResponse();
            }

            // go ahead and persist the gameboard (if it is only temporary) / link it to the users my boards account
            gameManager.linkUserToGameboard(existingGameboard, user);
            getLogManager().logEvent(user, request, IsaacServerLogType.ADD_BOARD_TO_PROFILE,
                    ImmutableMap.of(GAMEBOARD_ID_FKEY, existingGameboard.getId()));

        } catch (SegueDatabaseException e) {
            log.error("Database error while trying to save gameboard to user link.", e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error whilst trying to access the gameboard database.", e).toResponse();
        }

        return Response.ok().build();
    }

    /**
     * Rest Endpoint that allows a user to remove a gameboard from their my boards page.
     * 
     * This does not delete the gameboard from the system just removes it from the user's my boards page.
     * 
     * @param request
     *            - So that we can find the user information.
     * @param gameboardIds
     *            - comma separated list of gameboard ids to unlink
     * @return noContent response if successful a SegueErrorResponse if not.
     */
    @DELETE
    @Path("gameboards/user_gameboards/{gameboard_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Unlink the current user from one or a comma separated list of gameboards.",
                  description = "This will not delete or modify the gameboard.")
    public Response unlinkUserFromGameboard(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardIds) {

        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);
            Collection<String> gameboardIdsForDeletion = Arrays.asList(gameboardIds.split(","));

            this.gameManager.unlinkUserToGameboard(user, gameboardIdsForDeletion);

            getLogManager().logEvent(user, request, IsaacServerLogType.DELETE_BOARD_FROM_PROFILE,
                    ImmutableMap.of(GAMEBOARD_ID_FKEYS, gameboardIdsForDeletion));

        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to delete a gameboard.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        return Response.noContent().build();
    }

    /**
     * REST end point to list all possible wildcards.
     * 
     * @param request
     *            - so that we can deal with caching and etags.
     * @return a Response containing wildcard objects or containing a SegueErrorResponse.
     */
    @GET
    @Path("gameboards/wildcards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @Operation(summary = "List all possible gameboard wildcards.")
    public final Response getWildCards(@Context final Request request) {

        try {
            List<IsaacWildcard> wildcards = gameManager.getWildcards();
            if (null == wildcards || wildcards.isEmpty()) {
                return new SegueErrorResponse(Status.NOT_FOUND, "No wildcards found.").toResponse();
            }

            // Calculate the ETag
            EntityTag etag = new EntityTag(wildcards.toString().hashCode() + "");

            Response cachedResponse = generateCachedResponse(request, etag, NUMBER_SECONDS_IN_TEN_MINUTES);
            if (cachedResponse != null) {
                return cachedResponse;
            }

            return Response.ok(wildcards).cacheControl(getCacheControl(NUMBER_SECONDS_IN_TEN_MINUTES, true)).tag(etag)
                    .build();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        } catch (NoWildcardException e) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating any wildcards");
            log.error(error.getErrorMessage());
            return error.toResponse();
        }
    }
}
