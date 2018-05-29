/**
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

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import io.swagger.annotations.Api;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

import org.jboss.resteasy.annotations.GZIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.api.Constants.GameboardState;
import uk.ac.cam.cl.dtg.isaac.api.managers.DuplicateGameboardException;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.api.managers.InvalidGameboardException;
import uk.ac.cam.cl.dtg.isaac.api.managers.NoWildcardException;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardListDTO;
import uk.ac.cam.cl.dtg.segue.api.Constants.SortOrder;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAssociationManager;
import uk.ac.cam.cl.dtg.segue.api.managers.UserAccountManager;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserException;
import uk.ac.cam.cl.dtg.segue.auth.exceptions.NoUserLoggedInException;
import uk.ac.cam.cl.dtg.segue.dao.ILogManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.SegueErrorResponse;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

/**
 * Games boards Facade.
 */
@Path("/")
@Api(value = "/gameboards")
public class GameboardsFacade extends AbstractIsaacFacade {
    private GameManager gameManager;
    private UserAccountManager userManager;
    private UserAssociationManager associationManager;

    private static final Logger log = LoggerFactory.getLogger(GameboardsFacade.class);
    private final QuestionManager questionManager;
    private final Set<String> fastTrackGamebaordIds;

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
     */
    @Inject
    public GameboardsFacade(final PropertiesLoader properties, final ILogManager logManager,
            final GameManager gameManager, final QuestionManager questionManager, final UserAccountManager userManager,
            final UserAssociationManager associationManager) {
        super(properties, logManager);

        this.gameManager = gameManager;
        this.questionManager = questionManager;
        this.userManager = userManager;
        this.associationManager = associationManager;
        String commaSeparatedIds = this.getProperties().getProperty(Constants.FASTTRACK_GAMEBOARD_WHITELIST);
        this.fastTrackGamebaordIds = new HashSet<>(Arrays.asList(commaSeparatedIds.split(",")));
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
     * @return a Response containing a gameboard object or containing a SegueErrorResponse.
     */
    @GET
    @Path("gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response generateTemporaryGameboard(@Context final HttpServletRequest request,
            @QueryParam("title") final String title, @QueryParam("subjects") final String subjects,
            @QueryParam("fields") final String fields, @QueryParam("topics") final String topics,
            @QueryParam("levels") final String levels, @QueryParam("concepts") final String concepts) {
        List<String> subjectsList = null;
        List<String> fieldsList = null;
        List<String> topicsList = null;
        List<Integer> levelsList = null;
        List<String> conceptsList = null;

        if (null != subjects && !subjects.isEmpty()) {
            subjectsList = Arrays.asList(subjects.split(","));
        }

        if (null != fields && !fields.isEmpty()) {
            fieldsList = Arrays.asList(fields.split(","));
        }

        if (null != topics && !topics.isEmpty()) {
            topicsList = Arrays.asList(topics.split(","));
        }

        if (null != levels && !levels.isEmpty()) {
            String[] levelsAsString = levels.split(",");

            levelsList = Lists.newArrayList();
            for (int i = 0; i < levelsAsString.length; i++) {
                try {
                    levelsList.add(Integer.parseInt(levelsAsString[i]));
                } catch (NumberFormatException e) {
                    return new SegueErrorResponse(Status.BAD_REQUEST, "Levels must be numbers if specified.", e)
                            .toResponse();
                }
            }
        }

        if (null != concepts && !concepts.isEmpty()) {
            conceptsList = Arrays.asList(concepts.split(","));
        }

        AbstractSegueUserDTO boardOwner = this.userManager.getCurrentUser(request);

        try {
            GameboardDTO gameboard;

            gameboard = gameManager.generateRandomGameboard(title, subjectsList, fieldsList, topicsList, levelsList,
                    conceptsList, boardOwner);

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
     * REST end point to retrieve FastTrack gameboard progress by ID.
     *
     * @param request
     *             - so that we can deal with caching and etags.
     * @param httpServletRequest
     *            - so that we can extract the users session information if available.
     * @param gameboardId
     *            - the unique ID of the FastTrack gameboard to be requested, checked against a whitelist.
     * @return a Response containing a gameboard object or a SequeErrorResponse.
     */
    @GET
    @Path("gameboards/fasttrack/{gameboard_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getFastTrackGameboard(@Context final Request request,
            @Context final HttpServletRequest httpServletRequest, @PathParam("gameboard_id") final String gameboardId) {

        try {
            GameboardDTO gameboard;
            if (!fastTrackGamebaordIds.contains(gameboardId)) {
                return new SegueErrorResponse(Status.NOT_FOUND, "Gameboard id not a valid FastTrack gameboard id.")
                        .toResponse();
            }
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
            gameboard = gameManager.getFastTrackGameboard(gameboardId, randomUser, userQuestionAttempts);

            // We decided not to log this on the backend as the front end uses this lots.
            return Response.ok(gameboard).cacheControl(
                    getCacheControl(NEVER_CACHE_WITHOUT_ETAG_CHECK, false)).tag(etag).build();
        } catch (IllegalArgumentException e) {
            return new SegueErrorResponse(
                    Status.BAD_REQUEST, "Your FastTrack gameboard filter request is invalid.").toResponse();
        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to access the FastTrack gameboard in the database.";
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
     * getBoardPopularityList.
     * 
     * @param request
     *            for checking if the user is authorised to use this endpoint
     * @return list of popular boards.
     */
    @GET
    @Path("gameboards/popular")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBoardPopularityList(@Context final HttpServletRequest request) {
        final String connections = "connections";
        try {
            RegisteredUserDTO currentUser = this.userManager.getCurrentRegisteredUser(request);

            if (!isUserStaff(userManager, request)) {
                return SegueErrorResponse.getIncorrectRoleResponse();
            }

            Map<String, Integer> numberOfConnectedUsersByGameboard = this.gameManager
                    .getNumberOfConnectedUsersByGameboard();

            List<Map<String, Object>> resultList = Lists.newArrayList();

            for (Entry<String, Integer> e : numberOfConnectedUsersByGameboard.entrySet()) {
                if (e.getValue() > 1) {
                    GameboardDTO liteGameboard = this.gameManager.getLiteGameboard(e.getKey());

                    if (liteGameboard.getOwnerUserId() != null) {
                        RegisteredUserDTO ownerUser = userManager.getUserDTOById(liteGameboard.getOwnerUserId());
                        liteGameboard.setOwnerUserInformation(associationManager.enforceAuthorisationPrivacy(
                                currentUser, userManager.convertToUserSummaryObject(ownerUser)));
                    }

                    resultList.add(ImmutableMap.of("gameboard", liteGameboard, "connections", e.getValue()));
                }
            }

            Collections.sort(resultList, new Comparator<Map<String, Object>>() {
                /**
                 * Descending numerical order
                 */
                @Override
                public int compare(final Map<String, Object> o1, final Map<String, Object> o2) {

                    if ((Integer) o1.get(connections) < (Integer) o2.get(connections)) {
                        return 1;
                    }

                    if ((Integer) o1.get(connections) > (Integer) o2.get(connections)) {
                        return -1;
                    }

                    return 0;
                }
            });

            Integer sharedBoards = 0;

            for (Map<String, Object> e : resultList) {
                if ((Integer) e.get(connections) > 1) {
                    sharedBoards++;
                }
            }

            ImmutableMap<String, Object> resultMap = ImmutableMap.of("boardList", resultList, "sharedBoards",
                    sharedBoards);

            return Response.ok(resultMap).build();
        } catch (SegueDatabaseException | NoUserException e) {
            String message = "Error whilst trying to get the gameboard popularity list.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }
    }

    /**
     * createGameboard.
     * 
     * @param request 
     * @param newGameboardObject 
     * @return Gameboard DTO which has been persisted.
     */
    @POST
    @Path("gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public final Response createGameboard(
            @Context final HttpServletRequest request,
            final GameboardDTO newGameboardObject) {

        RegisteredUserDTO user;

        try {
            user = userManager.getCurrentRegisteredUser(request);

            if (null == newGameboardObject) {
                return new SegueErrorResponse(Status.BAD_REQUEST, "You must provide a gameboard object").toResponse();
            }

            if ((newGameboardObject.getId() != null || newGameboardObject.getTags().size() > 0) && !isUserStaff(userManager, request)) {
                return new SegueErrorResponse(Status.FORBIDDEN, "You cannot provide a gameboard ID or tags.").toResponse();
            }
        } catch (NoUserLoggedInException e1) {
            return SegueErrorResponse.getNotLoggedInResponse();
        }

        newGameboardObject.setCreationMethod(GameboardCreationMethod.BUILDER);

        GameboardDTO persistedGameboard;

        try {
            persistedGameboard = gameManager.saveNewGameboard(newGameboardObject, user);
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

        this.getLogManager().logEvent(user, request, CREATE_GAMEBOARD,
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
            getLogManager().logEvent(user, request, ADD_BOARD_TO_PROFILE,
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
    @Path("users/current_user/gameboards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
    public final Response getGameboardsByCurrentUser(@Context final HttpServletRequest request,
            @QueryParam("start_index") final String startIndex, @QueryParam("limit") final String limit,
            @QueryParam("sort") final String sortInstructions, @QueryParam("show_only") final String showCriteria) {
        RegisteredUserDTO currentUser;
        // TODO: change endpoint path to be more consistent with the gameboards facade
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
            for (String instruction : Arrays.asList(sortInstructions.toLowerCase().split(","))) {
                SortOrder s = SortOrder.ASC;
                if (instruction.startsWith("-")) {
                    s = SortOrder.DESC;
                    instruction = instruction.substring(1, instruction.length());
                }

                if (instruction.equals("created")) {
                    parsedSortInstructions.add(immutableEntry(CREATED_DATE_FIELDNAME, s));
                } else if (instruction.equals("visited")) {
                    parsedSortInstructions.add(immutableEntry(VISITED_DATE_FIELDNAME, s));
                } else if (instruction.equals("title")) {
                    parsedSortInstructions.add(immutableEntry(TITLE_FIELDNAME, s));
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

        if (null == gameboards) {
            return Response.noContent().build();
        }

        getLogManager().logEvent(
                currentUser,
                request,
                VIEW_MY_BOARDS_PAGE,
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
     * @return a Response containing a list of gameboard objects or containing a SegueErrorResponse.
     */
    @POST
    @Path("users/current_user/gameboards/{gameboard_id}")
    public final Response linkUserToGameboard(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId) {
        // TODO: change endpoint path to be more consistent with the gameboards facade
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
            getLogManager().logEvent(user, request, ADD_BOARD_TO_PROFILE,
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
     * This does not delete the gameboard from the system just removes it.
     * 
     * @param request
     *            - So that we can find the user information.
     * @param gameboardId
     *            -
     * @return noContent response if successful a SegueErrorResponse if not.
     */
    @DELETE
    @Path("users/current_user/gameboards/{gameboard_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unlinkUserFromGameboard(@Context final HttpServletRequest request,
            @PathParam("gameboard_id") final String gameboardId) {
        // TODO: change endpoint path to be more consistent with the gameboards facade
        try {
            RegisteredUserDTO user = userManager.getCurrentRegisteredUser(request);

            Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts = questionManager
                    .getQuestionAttemptsByUser(user);

            GameboardDTO gameboardDTO = this.gameManager.getGameboard(gameboardId, user, userQuestionAttempts);

            if (null == gameboardDTO) {
                return new SegueErrorResponse(Status.NOT_FOUND, "Unable to locate the gameboard specified.")
                        .toResponse();
            }

            this.gameManager.unlinkUserToGameboard(gameboardDTO, user);
            getLogManager().logEvent(user, request, DELETE_BOARD_FROM_PROFILE,
                    ImmutableMap.of(GAMEBOARD_ID_FKEY, gameboardDTO.getId()));

        } catch (SegueDatabaseException e) {
            String message = "Error whilst trying to delete a gameboard.";
            log.error(message, e);
            return new SegueErrorResponse(Status.INTERNAL_SERVER_ERROR, message).toResponse();
        } catch (NoUserLoggedInException e) {
            return SegueErrorResponse.getNotLoggedInResponse();
        } catch (ContentManagerException e1) {
            SegueErrorResponse error = new SegueErrorResponse(Status.NOT_FOUND, "Error locating the version requested",
                    e1);
            log.error(error.getErrorMessage(), e1);
            return error.toResponse();
        }

        return Response.noContent().build();
    }

    /**
     * REST end point to retrieve a specific gameboard by Id.
     * 
     * @param request
     *            - so that we can deal with caching and etags.
     * @return a Response containing a gameboard object or containing a SegueErrorResponse.
     */
    @GET
    @Path("gameboards/wildcards")
    @Produces(MediaType.APPLICATION_JSON)
    @GZIP
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
