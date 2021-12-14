/*
 * Copyright 2015 Stephen Cummins
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

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
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
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardListDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserBadgeManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import javax.servlet.http.HttpServletRequest;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Games boards Facade.
 */
@Path("/")
@Api(value = "/gameboards")
public class GameboardsFacade extends AbstractIsaacFacade {
    private GameManager gameManager;
    private UserAccountManager userManager;
    private UserAssociationManager associationManager;
    private UserBadgeManager userBadgeManager;

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
     * @param associationManager
     *            - to enforce privacy policies.
     * @param userBadgeManager
     *            - for updating badge information.
     */
    @Inject
    public GameboardsFacade(final PropertiesLoader properties, final ILogManager logManager,
                            final GameManager gameManager, final QuestionManager questionManager,
                            final UserAccountManager userManager, final UserAssociationManager associationManager,
                            final UserBadgeManager userBadgeManager, final FastTrackManger fastTrackManger) {
        super(properties, logManager);

        this.userBadgeManager = userBadgeManager;
        this.gameManager = gameManager;
        this.questionManager = questionManager;
        this.userManager = userManager;
        this.associationManager = associationManager;
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
    @GET
    @Path("gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "Get a temporary board of questions matching provided constraints.")
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
        } catch (NoWildcardException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, "Unable to load the wildcard.").toResponse();
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
    @ApiOperation(value = "Get the details of a gameboard.")
    public final Response getGameboard(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, @PathParam("gameboard_id") final String gameboardId) {

        try {
            GameboardDTO gameboard;

            AbstractSegueUserDTO randomUser = this.userManager.getCurrentUser(httpServletRequest);
            Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts = this.questionManager
                    .getQuestionAttemptsByUser(randomUser);

            GameboardDTO unAugmentedGameboard = gameManager.getGameboard(gameboardId);
            if (null == unAugmentedGameboard) {
                return new SegueErrorResponse(Status.NOT_FOUND, "No Gameboard found for the id specified.")
                        .toResponse();
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
    @ApiOperation(value = "Get the progress of the current user at a FastTrack gameboard.")
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
    @ApiOperation(value = "Create a new gameboard from a template GameboardDTO.",
                  notes = "Only staff users can provide custom IDs, wildcards or tags.")
    public final Response createGameboard(
            @Context final HttpServletRequest request,
            final GameboardDTO newGameboardObject) {

        RegisteredUserDTO user;

        try {
            user = userManager.getCurrentRegisteredUser(request);

            if (null == newGameboardObject) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a gameboard object").toResponse();
            }

            if ((newGameboardObject.getId() != null || newGameboardObject.getTags().size() > 0
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

            if (persistedGameboard.getCreationMethod().equals(GameboardCreationMethod.BUILDER)) {
                this.userBadgeManager.updateBadge(user, UserBadgeManager.Badge.TEACHER_GAMEBOARDS_CREATED,
                        persistedGameboard.getId());
            }

        } catch (NoWildcardException e) {
            return new SegueErrorResponse(Status.BAD_REQUEST, "No wildcard available. Unable to construct gameboard.")
                    .toResponse();
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
     * @param newGameboardObject
     *            - as a GameboardDTO this should contain all of the updates.
     * @return a Response containing a list of gameboard objects or containing a SegueErrorResponse.
     */
    @POST
    @Path("gameboards/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create or update a gameboard and link the current user to it.",
                  notes = "It is only possible to change the name of a gameboard.")
    public final Response updateGameboard(@Context final HttpServletRequest request,
            @PathParam("id") final String gameboardId, final GameboardDTO newGameboardObject) {

        RegisteredUserDTO user;
        try {
            user = userManager.getCurrentRegisteredUser(request);
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        if (null == newGameboardObject || null == gameboardId || newGameboardObject.getId() == null) {
            // Gameboard object must be there and have an id.
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "You must provide a gameboard object with updates and the "
                            + "id of the gameboard object in both the object and the endpoint").toResponse();
        }

        // The id in the path param should match the id of the gameboard object you send me.
        if (!newGameboardObject.getId().equals(gameboardId)) {
            // user not logged in return not authorized
            return new SegueErrorResponse(Status.BAD_REQUEST,
                    "The gameboard ID sent in the request body does not match the end point you used.").toResponse();
        }

        // find what the existing gameboard looks like.
        GameboardDTO existingGameboard;
        try {
            existingGameboard = gameManager.getGameboard(gameboardId);

            if (null == existingGameboard) {
                // this is not an edit and is a create request operation.
                return this.createGameboard(request, newGameboardObject);
            } else if (!existingGameboard.equals(newGameboardObject)) {
                // The only editable field of a game board is its title.
                // If you are trying to change anything else this should fail.
                return new SegueErrorResponse(Status.BAD_REQUEST, "A different game board with that id already exists.")
                        .toResponse();
            }

            // go ahead and persist the gameboard (if it is only temporary) / link it to the users my boards account
            gameManager.linkUserToGameboard(existingGameboard, user);
            getLogManager().logEvent(user, request, IsaacServerLogType.ADD_BOARD_TO_PROFILE,
                    ImmutableMap.of(GAMEBOARD_ID_FKEY, existingGameboard.getId()));

        } catch (SegueDatabaseException e) {
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR,
                    "Error whilst trying to access the gameboard database.", e).toResponse();
        }

        // Now determine if the user is trying to change the title and if they have permission.
        if (!(existingGameboard.getTitle() == null ? newGameboardObject.getTitle() == null : existingGameboard
                .getTitle().equals(newGameboardObject.getTitle()))) {

            // do they have permission?
            if (existingGameboard.getOwnerUserId() != null 
                    && !existingGameboard.getOwnerUserId().equals(user.getId())) {
                // user not logged in return not authorized
                return new SegueErrorResponse(Status.FORBIDDEN,
                        "You are not allowed to change another user's gameboard.").toResponse();
            }

            // ok so now we can change the title
            GameboardDTO updatedGameboard;
            try {
                updatedGameboard = gameManager.updateGameboardTitle(newGameboardObject);
            } catch (SegueDatabaseException e) {
                String message = "Error whilst trying to update the gameboard.";
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
     *            - e.g. completed,incompleted
     * @return a Response containing a list of gameboard objects or a noContent Response.
     */
    @GET
    @Path("gameboards/user_gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    @ApiOperation(value = "List all gameboards linked to the current user.")
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
            if (showCriteria.toLowerCase().equals("completed")) {
                gameboardShowCriteria = GameboardState.COMPLETED;
            } else if (showCriteria.toLowerCase().equals("in_progress")) {
                gameboardShowCriteria = GameboardState.IN_PROGRESS;
            } else if (showCriteria.toLowerCase().equals("not_attempted")) {
                gameboardShowCriteria = GameboardState.NOT_ATTEMPTED;
            } else {
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

                if (instruction.equals("created")) {
                    parsedSortInstructions.add(immutableEntry(CREATED_DATE_FIELDNAME, s));
                } else if (instruction.equals("visited")) {
                    parsedSortInstructions.add(immutableEntry(VISITED_DATE_FIELDNAME, s));
                } else if (instruction.equals("title")) {
                    parsedSortInstructions.add(immutableEntry(TITLE_FIELDNAME, s));
                } else if (instruction.equals("completion")) {
                    parsedSortInstructions.add(immutableEntry(COMPLETION_FIELDNAME, s));
                } else {
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
                        .put("completedTotal", gameboards.getTotalCompleted())
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
     * @param gameboardTitle
     *            - So that the gameboard can ebe persisted to the user account with a custom
     *              title (used by the CS question finder to name temporary gameboards)
     * @return a Response containing a list of gameboard objects or containing a SegueErrorResponse.
     */
    @POST
    @Path("gameboards/user_gameboards/{gameboard_id}/{gameboard_title}")
    @ApiOperation(value = "Link a gameboard to the current user.",
                  notes = "This will save a persistent copy of the gameboard if it was a temporary board.")
    public final Response linkUserToGameboard(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId, @PathParam("gameboard_title") final String gameboardTitle) {

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

            // If a new title was supplied, set it as the gameboard title
            if (null != gameboardTitle) {
                existingGameboard.setTitle(gameboardTitle);
            }

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
    @ApiOperation(value = "Unlink the current user from one or a comma separated list of gameboards.",
                  notes = "This will not delete or modify the gameboard.")
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
    @ApiOperation(value = "List all possible gameboard wildcards.")
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
