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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import ma.glasnost.orika.MapperFacade;
import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.AudienceContext;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardContentDescriptor;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.InlineRegion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardListDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.InlineRegionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.GitContentManager;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * This class will be responsible for generating and managing gameboards used by users.
 * 
 */
public class GameManager {
    private static final Logger log = LoggerFactory.getLogger(GameManager.class);

    private static final float DEFAULT_QUESTION_PASS_MARK = 75;

    private static final int MAX_QUESTIONS_TO_SEARCH = 20;
    private static final int GAMEBOARD_QUESTIONS_DEFAULT = 10;
    private static int gameboardQuestionsLimit;

    private final GameboardPersistenceManager gameboardPersistenceManager;
    private final Random randomGenerator;
    private final MapperFacade mapper;
    private final GitContentManager contentManager;
    private final QuestionManager questionManager;

    /**
     * Creates a game manager that operates using the provided api.
     * 
     * @param questionManager
     *            - so we can resolve game progress / user information.
     * @param contentManager
     *            - so we can augment game objects with actual detailed content
     * @param gameboardPersistenceManager
     *            - a persistence manager that deals with storing and retrieving gameboards.
     * @param mapper
     *            - allows mapping between DO and DTO object types.
     */
    @Inject
    public GameManager(final GitContentManager contentManager,
                       final GameboardPersistenceManager gameboardPersistenceManager, final MapperFacade mapper,
                       final QuestionManager questionManager,
                       final AbstractConfigLoader properties) {
        this.contentManager = contentManager;
        this.gameboardPersistenceManager = gameboardPersistenceManager;
        this.questionManager = questionManager;

        this.randomGenerator = new Random();

        this.mapper = mapper;

        try {
            GameManager.gameboardQuestionsLimit = Integer.parseInt(properties.getProperty(GAMEBOARD_QUESTION_LIMIT));
        } catch (NumberFormatException e) {
            GameManager.gameboardQuestionsLimit = GAMEBOARD_QUESTIONS_DEFAULT;
        }
    }

    /**
     * This method expects only one of its 3 subject tag filter parameters to have more than one element due to
     * restrictions on the question filter interface.
     *
     * @param title
     *            title of the board
     * @param subjects
     *            list of subjects to include in filtered results
     * @param fields
     *            list of fields to include in filtered results
     * @param topics
     *            list of topics to include in filtered results
     * @param levels
     *            list of levels to include in filtered results
     * @param concepts
     *            list of concepts (relatedContent) to include in filtered results
     * @param questionCategories
     *            list of question categories (i.e. problem_solving, book) to include in filtered results
     * @param boardOwner
     *            The user that should be marked as the creator of the gameBoard.
     * @return a gameboard if possible that satisfies the conditions provided by the parameters. Will return null if no
     *         questions can be provided.
     * @throws SegueDatabaseException
     *             - if there is an error contacting the database.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    @Deprecated
    public GameboardDTO generateRandomGameboard(
            final String title, final List<String> subjects, final List<String> fields, final List<String> topics,
            final List<Integer> levels, final List<String> concepts, final List<String> questionCategories,
            final List<String> stages, final List<String> difficulties, final List<String> examBoards,
            final AbstractSegueUserDTO boardOwner)
    throws SegueDatabaseException, ContentManagerException {

        Long boardOwnerId;
        if (boardOwner instanceof RegisteredUserDTO) {
            boardOwnerId = ((RegisteredUserDTO) boardOwner).getId();
        } else {
            // anonymous users do not get to own a board so just mark it as unowned.
            boardOwnerId = null;
        }

        Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts = questionManager
                .getQuestionAttemptsByUser(boardOwner);

        GameFilter gameFilter = new GameFilter(
                subjects, fields, topics, levels, concepts, questionCategories, stages, difficulties, examBoards);

        List<GameboardItem> selectionOfGameboardQuestions =
                this.getSelectedGameboardQuestions(gameFilter, usersQuestionAttempts);

        if (!selectionOfGameboardQuestions.isEmpty()) {
            String uuid = UUID.randomUUID().toString();

            // filter game board ready questions to make up a decent gameboard.
            log.debug("Created gameboard " + uuid);

            GameboardDTO gameboardDTO = new GameboardDTO(uuid, title, selectionOfGameboardQuestions,
                    null, null, new Date(), gameFilter,
                    boardOwnerId, GameboardCreationMethod.FILTER, Sets.newHashSet());

            this.gameboardPersistenceManager.temporarilyStoreGameboard(gameboardDTO);

            return augmentGameboardWithQuestionAttemptInformation(gameboardDTO, usersQuestionAttempts);
        } else {
            return null;
        }
    }
    
    /**
     * linkUserToGameboard. Persist the gameboard in permanent storage and also link it to the users account.
     * 
     * @param gameboardToLink
     *            - gameboard to store and use as link.
     * @param userToLinkTo
     *            - UserId to link to.
     * @throws SegueDatabaseException
     *             - if there is a problem persisting the gameboard in the database.
     */
    public void linkUserToGameboard(final GameboardDTO gameboardToLink, final RegisteredUserDTO userToLinkTo)
            throws SegueDatabaseException {
        // if the gameboard has no owner we should add the user who is first linking to it as the owner.
        if (gameboardToLink.getOwnerUserId() == null) {
            gameboardToLink.setOwnerUserId(userToLinkTo.getId());
        }

        // determine if we need to permanently store the gameboard
        if (!this.gameboardPersistenceManager.isPermanentlyStored(gameboardToLink.getId())) {
            this.permanentlyStoreGameboard(gameboardToLink);
        }

        this.gameboardPersistenceManager.createOrUpdateUserLinkToGameboard(userToLinkTo.getId(),
                gameboardToLink.getId());
    }

    /**
     * This method allows a user to gameboard link to be destroyed.
     *
     * @param user
     *            - DTO
     * @param gameboardsToUnlink
     *            - the collection of ids to unlink
     * @throws SegueDatabaseException
     *             - If there is a problem with the operation.
     */
    public void unlinkUserToGameboard(final RegisteredUserDTO user, final Collection<String> gameboardsToUnlink)
            throws SegueDatabaseException {
        this.gameboardPersistenceManager.removeUserLinkToGameboard(user.getId(), gameboardsToUnlink);
    }

    /**
     * Get a gameboard by its id.
     * 
     * Note: This gameboard will not be augmented with user information.
     * 
     * @param gameboardId
     *            - to look up.
     * @return the gameboard or null.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboard in the database or updating the users gameboard link
     *             table.
     */
    public final GameboardDTO getGameboard(final String gameboardId) throws SegueDatabaseException {
        if (null == gameboardId || gameboardId.isEmpty()) {
            return null;
        }

        return this.gameboardPersistenceManager.getGameboardById(gameboardId);
    }

    /**
     * Get a list of gameboards by their ids.
     *
     * Note: These gameboards will not be augmented with any user information.
     *
     * @param gameboardIds
     *            - to look up.
     * @return the gameboards or null.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboards in the database or updating the users gameboards
     *             link table.
     */
    public final List<GameboardDTO> getGameboards(final List<String> gameboardIds) throws SegueDatabaseException {
        return this.gameboardPersistenceManager.getGameboardsByIds(gameboardIds);
    }

    /**
     * Get a list of gameboards by their ids.
     *
     * Note: These gameboards WILL be augmented with user attempt information, but not whether the gameboard is saved
     * to the user's boards.
     *
     * @param gameboardIds
     *            - to look up.
     * @param userQuestionAttempts
     *            - so that we can augment the gameboard.
     * @return the gameboards or null.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboards in the database.
     * @throws ContentManagerException
     *             - if there is a problem resolving content
     */
    public final List<GameboardDTO> getGameboards(final List<String> gameboardIds,
                                                  final Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts)
            throws SegueDatabaseException, ContentManagerException {
        if (null == gameboardIds || gameboardIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<GameboardDTO> gameboardsByIds = this.gameboardPersistenceManager.getGameboardsByIds(gameboardIds);
        for (GameboardDTO gb : gameboardsByIds) {
            augmentGameboardWithQuestionAttemptInformation(gb, userQuestionAttempts);
        }

        return gameboardsByIds;
    }

    /**
     * Get a list of gameboards by their ids, augmented with attempt information.
     *
     * Note: These gameboards WILL be augmented with user attempt information, but not whether the gameboard is saved
     * to the user's boards.
     *
     * @param gameboardIds
     *            - to look up.
     * @param user
     *            - the user to augment the gameboard for.
     * @return the gameboards or null.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboards in the database.
     * @throws ContentManagerException
     *             - if there is a problem resolving content
     */
    public final List<GameboardDTO> getGameboardsWithAttempts(final List<String> gameboardIds, final RegisteredUserDTO user)
            throws SegueDatabaseException, ContentManagerException {
        if (null == gameboardIds || gameboardIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<GameboardDTO> gameboardsByIds = this.gameboardPersistenceManager.getGameboardsByIds(gameboardIds);
        List<String> questionPageIds = gameboardsByIds.stream().map(GameboardDTO::getContents).flatMap(Collection::stream).map(GameboardItem::getId).collect(Collectors.toList());
        Map<String, Map<String, List<LightweightQuestionValidationResponse>>> userQuestionAttempts =
                questionManager.getMatchingLightweightQuestionAttempts(user, questionPageIds);
        for (GameboardDTO gb : gameboardsByIds) {
            augmentGameboardWithQuestionAttemptInformation(gb, userQuestionAttempts);
        }

        return gameboardsByIds;
    }

    /**
     * Get a gameboard by its id.
     * 
     * @param gameboardId
     *            - the id to find.
     * @return a lite gameboard without the question data fully expanded.
     * @throws SegueDatabaseException
     *             - a database error has occurred.
     */
    public GameboardDTO getLiteGameboard(final String gameboardId) throws SegueDatabaseException {
        return this.gameboardPersistenceManager.getLiteGameboardById(gameboardId);
    }

    /**
     * Get a list of gameboards by their ids.
     *
     * @param gameboardId
     *            - the ids to find.
     * @return a list of lite gameboards without their question data fully expanded.
     * @throws SegueDatabaseException
     *             - a database error has occurred.
     */
    public List<GameboardDTO> getLiteGameboards(final Collection<String> gameboardId) throws SegueDatabaseException {
        return this.gameboardPersistenceManager.getLiteGameboardsByIds(gameboardId);
    }

    /**
     * Get a gameboard by its id and augment with user information.
     * 
     * @param gameboardId
     *            - to look up.
     * @param user
     *            - This allows state information to be retrieved.
     * @param userQuestionAttempts
     *            - so that we can augment the gameboard.
     * @return the gameboard or null.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboard in the database or updating the users gameboard link
     *             table.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    public final GameboardDTO getGameboard(final String gameboardId, final AbstractSegueUserDTO user,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> userQuestionAttempts)
            throws SegueDatabaseException, ContentManagerException {


        // we need to augment the DTO with whether this gameboard is in a users my boards list.
        return augmentGameboardWithQuestionAttemptInformationAndUserInformation(
                this.gameboardPersistenceManager.getGameboardById(gameboardId), userQuestionAttempts, user);
    }

    /**
     * Lookup gameboards belonging to a current user.
     * 
     * @param user
     *            - containing so that we can augment the response with personalised information
     * @param startIndex
     *            - the initial index to return.
     * @param limit
     *            - the limit of the number of results to return
     * @param showOnly
     *            - show only gameboards matching the given state.
     * @param sortInstructions
     *            - List of instructions of the form fieldName -> SortOrder. Can be null.
     * @return a list of gameboards (without full question information) which are associated with a given user.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboards from the database.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    public final GameboardListDTO getUsersGameboards(final RegisteredUserDTO user, @Nullable final Integer startIndex,
            @Nullable final Integer limit, @Nullable final GameboardState showOnly,
            @Nullable final List<Map.Entry<String, SortOrder>> sortInstructions) throws SegueDatabaseException,
            ContentManagerException {
        Objects.requireNonNull(user);

        List<GameboardDTO> usersGameboards = this.gameboardPersistenceManager.getGameboardsByUserId(user);
        if (null == usersGameboards || usersGameboards.isEmpty()) {
            return new GameboardListDTO();
        }

        List<String> questionPageIds = usersGameboards.stream().map(GameboardDTO::getContents).flatMap(Collection::stream).map(GameboardItem::getId).collect(Collectors.toList());
        Map<String, Map<String, List<LightweightQuestionValidationResponse>>> questionAttemptsFromUser =
                questionManager.getMatchingLightweightQuestionAttempts(user, questionPageIds);

        List<GameboardDTO> resultToReturn = Lists.newArrayList();

        long totalAllAttempted = 0L;
        long totalInProgress = 0L;
        long totalNotStarted = 0L;

        // filter gameboards based on selection.
        for (GameboardDTO gameboard : usersGameboards) {
            this.augmentGameboardWithQuestionAttemptInformation(gameboard, questionAttemptsFromUser);

            // we know that the user already has these boards in their my boards page so just set them to true
            gameboard.setSavedToCurrentUser(true);

            if (null == showOnly) {
                resultToReturn.add(gameboard);
            } else if (gameboard.isStartedQuestion() && showOnly.equals(GameboardState.IN_PROGRESS)) {
                resultToReturn.add(gameboard);
            } else if (!gameboard.isStartedQuestion() && showOnly.equals(GameboardState.NOT_ATTEMPTED)) {
                resultToReturn.add(gameboard);
            } else if (gameboard.getPercentageAttempted() == 100 && showOnly.equals(GameboardState.ALL_ATTEMPTED)) {
                resultToReturn.add(gameboard);
            } else if (gameboard.getPercentageCorrect() == 100 && showOnly.equals(GameboardState.ALL_CORRECT)) {
                resultToReturn.add(gameboard);
            }

            // counts
            if (!gameboard.isStartedQuestion()) {
                totalNotStarted++;
            } else if (gameboard.getPercentageAttempted() == 100) {
                totalAllAttempted++;
            } else if (gameboard.isStartedQuestion()) {
                totalInProgress++;
            }
        }

        ComparatorChain<GameboardDTO> comparatorForSorting = new ComparatorChain<>();
        Comparator<GameboardDTO> defaultComparitor = (o1, o2) ->
                o1.getLastVisited().getTime() > o2.getLastVisited().getTime() ? -1 : 1;

        // assume we want reverse date order for visited date for now.
        if (null == sortInstructions || sortInstructions.isEmpty()) {
            comparatorForSorting.addComparator(defaultComparitor);
        } else {
            // we have to use a more complex sorting Comparator.

            for (Map.Entry<String, SortOrder> sortInstruction : sortInstructions) {
                boolean reverseOrder = false;
                if (sortInstruction.getValue().equals(SortOrder.DESC)) {
                    reverseOrder = true;
                }

                switch (sortInstruction.getKey()) {
                    case CREATED_DATE_FIELDNAME:
                        comparatorForSorting.addComparator((o1, o2) -> {
                            if (o1.getCreationDate().getTime() == o2.getCreationDate().getTime()) {
                                return 0;
                            } else {
                                return o1.getCreationDate().getTime() > o2.getCreationDate().getTime() ? -1 : 1;
                            }
                        }, reverseOrder);
                        break;
                    case VISITED_DATE_FIELDNAME:
                        comparatorForSorting.addComparator((o1, o2) -> {
                            if (o1.getLastVisited().getTime() == o2.getLastVisited().getTime()) {
                                return 0;
                            } else {
                                return o1.getLastVisited().getTime() > o2.getLastVisited().getTime() ? -1 : 1;
                            }
                        }, reverseOrder);
                        break;
                    case TITLE_FIELDNAME:
                        comparatorForSorting.addComparator((o1, o2) -> {
                            if (o1.getTitle() == null && o2.getTitle() == null) {
                                return 0;
                            }
                            if (o1.getTitle() == null) {
                                return 1;
                            }
                            if (o2.getTitle() == null) {
                                return -1;
                            }
                            return o1.getTitle().compareTo(o2.getTitle());
                        }, reverseOrder);
                        break;
                    case PERCENTAGE_ATTEMPTED_FIELDNAME:
                        comparatorForSorting.addComparator((o1, o2) -> {
                            if (o1.getPercentageAttempted() == null && o2.getPercentageAttempted() == null) {
                                return 0;
                            }
                            if (o1.getPercentageAttempted() == null) {
                                return 1;
                            }
                            if (o2.getPercentageAttempted() == null) {
                                return -1;
                            }
                            return o1.getPercentageAttempted().compareTo(o2.getPercentageAttempted());
                        }, reverseOrder);
                        break;
                    case PERCENTAGE_CORRECT_FIELDNAME:
                        comparatorForSorting.addComparator((o1, o2) -> {
                            if (o1.getPercentageCorrect() == null && o2.getPercentageCorrect() == null) {
                                return 0;
                            }
                            if (o1.getPercentageCorrect() == null) {
                                return 1;
                            }
                            if (o2.getPercentageCorrect() == null) {
                                return -1;
                            }
                            return o1.getPercentageCorrect().compareTo(o2.getPercentageCorrect());
                        }, reverseOrder);
                        break;
                    default:
                        // This should not happen?
                        break;
                }
            }
        }

        if (comparatorForSorting.size() == 0) {
            comparatorForSorting.addComparator(defaultComparitor);
        }

        resultToReturn.sort(comparatorForSorting);

        int toIndex;
        if (limit == null || startIndex + limit > resultToReturn.size()) {
            toIndex = resultToReturn.size();
        } else {
            toIndex = startIndex + limit;
        }

        List<GameboardDTO> sublistOfGameboards = resultToReturn.subList(startIndex, toIndex);

        // fully augment only those we are returning.
        this.gameboardPersistenceManager.augmentGameboardItemsWithContentData(sublistOfGameboards);

        return new GameboardListDTO(sublistOfGameboards, (long) resultToReturn.size(),
                totalNotStarted, totalInProgress, totalAllAttempted);
    }

    /**
     * @param gameboardDTO
     *            - to save
     * @param owner
     *            - user to make owner of gameboard.
     * @return gameboardDTO as persisted
     * @throws InvalidGameboardException
     *             - if the gameboard already exists with the given id.
     * @throws SegueDatabaseException
     *             - If we cannot save the gameboard.
     * @throws DuplicateGameboardException
     *             - If a gameboard already exists with the given id.
     * @throws ContentManagerException
     *             - if we are unable to lookup the required content.
     */
    public GameboardDTO saveNewGameboard(final GameboardDTO gameboardDTO, final RegisteredUserDTO owner)
            throws InvalidGameboardException, SegueDatabaseException, DuplicateGameboardException,
            ContentManagerException {
        Objects.requireNonNull(gameboardDTO);
        Objects.requireNonNull(owner);

        String gameboardId = gameboardDTO.getId();
        if (gameboardId == null) {
            gameboardDTO.setId(UUID.randomUUID().toString());
        } else if (this.getGameboard(gameboardId.toLowerCase()) != null) {
            throw new DuplicateGameboardException();
        } else {
            gameboardDTO.setId(gameboardId.toLowerCase());
        }

        // set creation date to now.
        gameboardDTO.setCreationDate(new Date());
        gameboardDTO.setOwnerUserId(owner.getId());

        // this will throw an exception if it doesn't validate.
        validateGameboard(gameboardDTO);

        this.permanentlyStoreGameboard(gameboardDTO);

        return gameboardDTO;
    }

    /**
     * Update the gameboards title.
     * 
     * @param gameboardWithUpdatedTitle
     *            - only the title will be updated.
     * @return the fully updated gameboard.
     * @throws SegueDatabaseException
     *             - if there is a problem updating the gameboard.
     */
    public GameboardDTO updateGameboardTitle(final GameboardDTO gameboardWithUpdatedTitle)
            throws SegueDatabaseException, InvalidGameboardException {
        this.validateGameboard(gameboardWithUpdatedTitle);
        return this.gameboardPersistenceManager.updateGameboardTitle(gameboardWithUpdatedTitle);
    }

    /**
     * Returns game states for a number of users for a given gameboard.
     * 
     * @param users
     *            - of interest
     * @param gameboard
     *            - gameboard containing questions.
     * @return map of users to their gameboard item results.
     * @throws SegueDatabaseException
     *             - if there is a problem with the database
     * @throws ContentManagerException
     *             - if we can't look up the question page details.
     */
    public List<ImmutablePair<RegisteredUserDTO, List<GameboardItem>>> gatherGameProgressData(
            final List<RegisteredUserDTO> users, final GameboardDTO gameboard) throws SegueDatabaseException,
            ContentManagerException {
        Objects.requireNonNull(users);
        Objects.requireNonNull(gameboard);

        List<ImmutablePair<RegisteredUserDTO, List<GameboardItem>>> result = Lists.newArrayList();

        List<String> questionPageIds =
                gameboard.getContents().stream().map(GameboardItem::getId).collect(Collectors.toList());

        Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>>
                questionAttemptsForAllUsersOfInterest =
                questionManager.getMatchingLightweightQuestionAttempts(users, questionPageIds);

        for (RegisteredUserDTO user : users) {
            List<GameboardItem> userGameItems = Lists.newArrayList();

            for (GameboardItem observerGameItem : gameboard.getContents()) {
                GameboardItem userGameItem = new GameboardItem(observerGameItem);
                this.augmentGameItemWithAttemptInformation(userGameItem,
                        questionAttemptsForAllUsersOfInterest.get(user.getId()));
                userGameItems.add(userGameItem);
            }
            result.add(new ImmutablePair<>(user, userGameItems));
        }

        return result;
    }

    
    /**
     * Find all wildcards.
     * 
     * @return wildCard object.
     * @throws NoWildcardException
     *             - when we are unable to provide you with a wildcard object.
     * @throws ContentManagerException
     *             - if we cannot access the content requested.
     */
    public List<IsaacWildcard> getWildcards() throws NoWildcardException, ContentManagerException {
        List<GitContentManager.BooleanSearchClause> fieldsToMap = Lists.newArrayList();

        fieldsToMap.add(new GitContentManager.BooleanSearchClause(
                TYPE_FIELDNAME, BooleanOperator.OR, Collections.singletonList(WILDCARD_TYPE)));

        Map<String, SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(TITLE_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, SortOrder.ASC);

        ResultsWrapper<ContentDTO> wildcardResults = this.contentManager.findByFieldNames(
                fieldsToMap, 0, -1, sortInstructions);

        if (wildcardResults.getTotalResults() == 0) {
            throw new NoWildcardException();
        }

        List<IsaacWildcard> result = Lists.newArrayList();
        for (ContentDTO c : wildcardResults.getResults()) {
            IsaacWildcard wildcard = mapper.map(c, IsaacWildcard.class);
            result.add(wildcard);
        }

        return result;
    }
    
    /**
     * Get all questions in the question page: depends on each question. This method will conduct a DFS traversal and
     * ensure the collection is ordered as per the DFS.
     * 
     * @param questionPageId
     *            - results depend on each question having an id prefixed with the question page id.
     * @return collection of markable question parts (questions).
     * @throws ContentManagerException
     *             if there is a problem with the content requested.
     */
    public Collection<Question> getAllMarkableDOQuestionPartsDFSOrder(final String questionPageId)
            throws ContentManagerException {
        Validate.notBlank(questionPageId);

        // do a depth first traversal of the question page to get the correct order of questions
        Content questionPage = this.contentManager.getContentDOById(questionPageId);
        return getAllMarkableDOQuestionPartsDFSOrder(questionPage);
    }

    /**
     * Get all questions in a piece of content. This method will conduct a DFS traversal and
     * ensure the collection is ordered as per the DFS. Quick questions will be filtered out.
     *
     * See also {@link QuestionManager#extractQuestionObjectsRecursively(ContentDTO, List)}
     * which does basically the same thing.
     *
     * @param content
     *            - results depend on each question having an id prefixed with the question page id.
     * @return collection of markable question parts (questions).
     */
    public static List<QuestionDTO> getAllMarkableQuestionPartsDFSOrder(final ContentDTO content) {
        List<ContentDTO> dfs = Lists.newArrayList();
        dfs = depthFirstQuestionSearch(content, dfs);

        return filterQuestionParts(dfs);
    }

    /**
     * Copy of {@link #getAllMarkableQuestionPartsDFSOrder(ContentDTO)} for DOs not DTOs.
     */
    public static List<Question> getAllMarkableDOQuestionPartsDFSOrder(final Content content) {
        List<Content> dfs = Lists.newArrayList();
        dfs = depthFirstDOQuestionSearch(content, dfs);

        return filterDOQuestionParts(dfs);
    }

    /**
     * Augments the gameboards with question attempt information AND whether or not the user has it in their boards.
     *
     * @param gameboardDTO
     *            - the DTO of the gameboard.
     * @param questionAttemptsFromUser
     *            - the users question attempt data.
     * @param user
     *            - the user to check whether the board is in their boards list
     * @return Augmented Gameboard.
     * @throws SegueDatabaseException
     *             - if there is an error retrieving the content requested.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    private GameboardDTO augmentGameboardWithQuestionAttemptInformationAndUserInformation(final GameboardDTO gameboardDTO,
                                                                                          final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> questionAttemptsFromUser,
                                                                                          final AbstractSegueUserDTO user)
            throws SegueDatabaseException, ContentManagerException {
        if (user instanceof RegisteredUserDTO) {
            gameboardDTO
                    .setSavedToCurrentUser(this.isBoardLinkedToUser((RegisteredUserDTO) user, gameboardDTO.getId()));
        }

        this.augmentGameboardWithQuestionAttemptInformation(gameboardDTO, questionAttemptsFromUser);

        return gameboardDTO;
    }


    /**
     * Augments the gameboards with question attempt information NOT whether the user has it in their my board page.
     *
     * @param gameboardDTO
     *            - the DTO of the gameboard.
     * @param questionAttemptsFromUser
     *            - the users question attempt data.
     * @return Augmented Gameboard.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    private GameboardDTO augmentGameboardWithQuestionAttemptInformation(final GameboardDTO gameboardDTO,
                                                                        final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> questionAttemptsFromUser)
            throws ContentManagerException {
        if (null == gameboardDTO) {
            return null;
        }

        if (null == questionAttemptsFromUser || gameboardDTO.getContents().isEmpty()) {
            return gameboardDTO;
        }

        boolean gameboardStarted = false;
        List<GameboardItem> questions = gameboardDTO.getContents();
        int totalNumberOfQuestionsParts = 0;
        int totalNumberOfAttemptedQuestionParts = 0;
        int totalNumberOfCorrectQuestionParts = 0;
        for (GameboardItem gameItem : questions) {
            try {
                this.augmentGameItemWithAttemptInformation(gameItem, questionAttemptsFromUser);
            } catch (ResourceNotFoundException e) {
                log.info(String.format(
                        "The gameboard '%s' references an unavailable question '%s' - treating it as if it never existed for marking!",
                        gameboardDTO.getId(), gameItem.getId()));
                continue;
            }

            if (!gameboardStarted && !gameItem.getState().equals(CompletionState.NOT_ATTEMPTED)) {
                gameboardStarted = true;
                gameboardDTO.setStartedQuestion(gameboardStarted);
            }
            totalNumberOfQuestionsParts += gameItem.getQuestionPartsTotal();
            totalNumberOfCorrectQuestionParts += gameItem.getQuestionPartsCorrect();
            totalNumberOfAttemptedQuestionParts += gameItem.getQuestionPartsCorrect() + gameItem.getQuestionPartsIncorrect();
        }

        gameboardDTO.setPercentageAttempted(Math.round(100f * totalNumberOfAttemptedQuestionParts / totalNumberOfQuestionsParts));
        gameboardDTO.setPercentageCorrect(Math.round(100f * totalNumberOfCorrectQuestionParts / totalNumberOfQuestionsParts));

        return gameboardDTO;
    }

    /**
     * Convert a list of questions to gameboard items and augment with user question attempt information.
     *
     * @param questions list of questions.
     * @param userQuestionAttempts the user's question attempt history.
     * @param gameFilter optional filter for deriving a context for viewing users.
     * @return list of augmented gameboard items.
     */
    public List<GameboardItem> getGameboardItemProgress(
            @NotNull final List<ContentDTO> questions,
            final Map<String, ? extends Map<String, ? extends List<QuestionValidationResponse>>> userQuestionAttempts,
            @Nullable final GameFilter gameFilter) {

        return questions.stream()
                .map(q -> this.gameboardPersistenceManager.convertToGameboardItem(
                        q, new GameboardContentDescriptor(q.getId(), QUESTION_TYPE, AudienceContext.fromFilter(gameFilter))))
                .map(questionItem -> {
                    try {
                        this.augmentGameItemWithAttemptInformation(questionItem, userQuestionAttempts);
                    } catch (ContentManagerException | ResourceNotFoundException e) {
                        log.error("Unable to augment '" + questionItem.getId() + "' with user attempt information");
                    }
                    return questionItem;
                }).collect(Collectors.toList());
    }

    /**
     * Utility method to extract a list of questionDTOs only.
     * 
     * @param contentToFilter
     *            list of content.
     * @return list of question dtos.
     */
    private static List<QuestionDTO> filterQuestionParts(final Collection<ContentDTO> contentToFilter) {
        List<QuestionDTO> results = Lists.newArrayList();
        for (ContentDTO possibleQuestion : contentToFilter) {
            if (!(possibleQuestion instanceof QuestionDTO) || possibleQuestion instanceof IsaacQuickQuestionDTO) {
                // we are not interested if this is not a question or if it is a quick question.
                continue;
            }
            QuestionDTO question = (QuestionDTO) possibleQuestion;
            results.add(question);
        }

        return results;
    }

    /**
     * Copy of {@link #filterQuestionParts} for DOs not DTOs.
     */
    private static List<Question> filterDOQuestionParts(final Collection<Content> contentToFilter) {
        List<Question> results = Lists.newArrayList();
        for (Content possibleQuestion : contentToFilter) {
            if (!(possibleQuestion instanceof Question) || possibleQuestion instanceof IsaacQuickQuestion) {
                // we are not interested if this is not a question or if it is a quick question.
                continue;
            }
            Question question = (Question) possibleQuestion;
            results.add(question);
        }

        return results;
    }
    
    /**
     * We want to list the questions in the order they are seen.
     * @param c - content to search
     * @param result - the list of questions
     * @return a list of questions ordered by DFS.
     */
    private static List<ContentDTO> depthFirstQuestionSearch(final ContentDTO c, final List<ContentDTO> result) {
        if (c == null || c.getChildren() == null || c.getChildren().size() == 0) {
            return result;
        }

        if (c instanceof InlineRegionDTO) {
            // extract inline questions
            InlineRegionDTO inlineRegionDTO = (InlineRegionDTO) c;
            result.addAll(inlineRegionDTO.getInlineQuestions());
        }
        
        for (ContentBaseDTO child : c.getChildren()) {
            if (child instanceof QuestionDTO) {
                result.add((QuestionDTO) child);
                // assume that we can't have nested questions
            } else {
                depthFirstQuestionSearch((ContentDTO) child, result);
            }
        }
        return result;
    }

    /**
     * Copy of {@link #depthFirstQuestionSearch(ContentDTO, List)} for DOs not DTOs.
     */
    private static List<Content> depthFirstDOQuestionSearch(final Content c, final List<Content> result) {
        if (c == null || c.getChildren() == null || c.getChildren().size() == 0) {
            return result;
        }

        for (ContentBase child : c.getChildren()) {
            if (child instanceof Question) {
                result.add((Question) child);
                // assume that we can't have nested questions
            } else {
                depthFirstDOQuestionSearch((Content) child, result);
            }

            if (child instanceof InlineRegion) {
                // extract inline questions
                InlineRegion inlineRegion = (InlineRegion) child;
                result.addAll(inlineRegion.getInlineQuestions());
            }
        }
        return result;
    }

    /**
     * Determines whether a given game board is already in a users my boards list.
     * 
     * @param user
     *            to check
     * @param gameboardId
     *            to look up
     * @return true if it is false if not
     * @throws SegueDatabaseException
     *             if there is a database error
     */
    private boolean isBoardLinkedToUser(final RegisteredUserDTO user, final String gameboardId)
            throws SegueDatabaseException {
        return this.gameboardPersistenceManager.isBoardLinkedToUser(user.getId(), gameboardId);
    }    
    
    /**
     * Store a gameboard in a public location.
     * 
     * @param gameboardToStore
     *            - Gameboard object to persist.
     * @throws SegueDatabaseException
     *             - if there is a problem persisting the gameboard in the database.
     */
    private void permanentlyStoreGameboard(final GameboardDTO gameboardToStore) throws SegueDatabaseException {
        this.gameboardPersistenceManager.saveGameboardToPermanentStorage(gameboardToStore);
    }

    /**
     * This method aims to (somewhat) intelligently select some useful gameboard questions.
     * 
     * @param gameFilter
     *            - the filter query that should be used to make up the gameboard.
     * @param usersQuestionAttempts
     *            - the users question attempt information if available.
     * @return Gameboard questions
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    @Deprecated
    private List<GameboardItem> getSelectedGameboardQuestions(final GameFilter gameFilter,
            final Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts)
            throws ContentManagerException {

        Long seed = new Random().nextLong();
        int searchIndex = 0;
        List<GameboardItem> selectionOfGameboardQuestions = this.getNextQuestionsForFilter(gameFilter, searchIndex,
                seed);

        if (selectionOfGameboardQuestions.isEmpty()) {
            // no questions found just return an empty list.
            return Lists.newArrayList();
        }

        Set<GameboardItem> gameboardReadyQuestions = Sets.newHashSet();
        List<GameboardItem> completedQuestions = Lists.newArrayList();
        // choose the gameboard questions to include.
        while (gameboardReadyQuestions.size() < GAMEBOARD_QUESTIONS_DEFAULT && !selectionOfGameboardQuestions.isEmpty()) {
            for (GameboardItem gameboardItem : selectionOfGameboardQuestions) {
                CompletionState questionState;
                try {
                    this.augmentGameItemWithAttemptInformation(gameboardItem, usersQuestionAttempts);
                    questionState = gameboardItem.getState();
                } catch (ResourceNotFoundException e) {
                    throw new ContentManagerException(
                            "Resource not found exception, this shouldn't happen as the selectionOfGameboardQuestions "
                            + "should only show available content.");
                }
                
                if (questionState.equals(CompletionState.ALL_ATTEMPTED)
                        || questionState.equals(CompletionState.ALL_CORRECT)) {
                    completedQuestions.add(gameboardItem);
                } else {
                    gameboardReadyQuestions.add(gameboardItem);
                }

                // stop inner loop if we have reached our target
                if (gameboardReadyQuestions.size() == GAMEBOARD_QUESTIONS_DEFAULT) {
                    break;
                }
            }

            if (gameboardReadyQuestions.size() == GAMEBOARD_QUESTIONS_DEFAULT) {
                break;
            }

            // increment search start index to see if we can get more questions for the given criteria.
            searchIndex = searchIndex + selectionOfGameboardQuestions.size();

            // we couldn't fill it up on the last round of questions so lets get more if no more this will be empty
            selectionOfGameboardQuestions = this.getNextQuestionsForFilter(gameFilter, searchIndex, seed);
        }

        // Try and make up the difference with completed ones if we haven't reached our target size
        if (gameboardReadyQuestions.size() < GAMEBOARD_QUESTIONS_DEFAULT && !completedQuestions.isEmpty()) {
            for (GameboardItem completedQuestion : completedQuestions) {
                if (gameboardReadyQuestions.size() < GAMEBOARD_QUESTIONS_DEFAULT) {
                    gameboardReadyQuestions.add(completedQuestion);
                } else if (gameboardReadyQuestions.size() == GAMEBOARD_QUESTIONS_DEFAULT) {
                    break;
                }
            }
        }

        // Convert to List and randomise the questions again, as we may have injected some completed questions.
        List<GameboardItem> gameboardQuestionList = Lists.newArrayList(gameboardReadyQuestions);
        Collections.shuffle(gameboardQuestionList);

        return gameboardQuestionList;
    }

    /**
     * Gets you the next set of questions that match the given filter.
     * 
     * @param gameFilter
     *            - to enable search
     * @param index
     *            - the starting index of the query.
     * @param randomSeed
     *            - so that we can use search pagination and not repeat ourselves.
     * @return a list of gameboard items.
     * @throws ContentManagerException
     *             - if there is a problem accessing the content repository.
     */
    @Deprecated
    public List<GameboardItem> getNextQuestionsForFilter(final GameFilter gameFilter, final int index,
            final Long randomSeed) throws ContentManagerException {
        // get some questions
        List<GitContentManager.BooleanSearchClause> fieldsToMap = Lists.newArrayList();
        fieldsToMap.add(new GitContentManager.BooleanSearchClause(
                TYPE_FIELDNAME, BooleanOperator.AND, Collections.singletonList(QUESTION_TYPE)));
        fieldsToMap.addAll(generateFieldToMatchForQuestionFilter(gameFilter));

        // Search for questions that match the fields to map variable.

        ResultsWrapper<ContentDTO> results = this.contentManager.findByFieldNamesRandomOrder(
                fieldsToMap, index, MAX_QUESTIONS_TO_SEARCH, randomSeed);

        List<ContentDTO> questionsForGameboard = results.getResults();

        List<GameboardItem> selectionOfGameboardQuestions = Lists.newArrayList();

        // Map each Content object into an GameboardItem object
        for (ContentDTO c : questionsForGameboard) {
            // Only keep questions that have not been superseded.
            // Yes, this should probably be done in the fieldsToMap filter above, but this is simpler.
            if (c instanceof IsaacQuestionPageDTO) {
                IsaacQuestionPageDTO qp = (IsaacQuestionPageDTO) c;
                if (qp.getSupersededBy() != null && !qp.getSupersededBy().isEmpty()) {
                    // This question has been superseded. Don't include it.
                    continue;
                }
            }

            GameboardItem questionInfo = this.gameboardPersistenceManager.convertToGameboardItem(
                    c, new GameboardContentDescriptor(c.getId(), QUESTION_TYPE, AudienceContext.fromFilter(gameFilter)));
            selectionOfGameboardQuestions.add(questionInfo);
        }

        return selectionOfGameboardQuestions;
    }

    /**
     * AugmentGameItemWithAttemptInformation
     * 
     * This method will calculate the question state for use in gameboards based on the question.
     * 
     * @param gameItem
     *             - the gameboard item.
     * @param questionAttemptsFromUser
     *             - the user that may or may not have attempted questions in the gameboard.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     * @throws ResourceNotFoundException
     *             - if we cannot find the question specified.
     */
    private void augmentGameItemWithAttemptInformation(
            final GameboardItem gameItem,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>>
                    questionAttemptsFromUser)
            throws ContentManagerException, ResourceNotFoundException {
        Objects.requireNonNull(gameItem, "gameItem cannot be null");
        Objects.requireNonNull(questionAttemptsFromUser, "questionAttemptsFromUser cannot be null");

        List<QuestionPartState> questionPartStates = Lists.newArrayList();
        int questionPartsCorrect = 0;
        int questionPartsIncorrect = 0;
        int questionPartsNotAttempted = 0;
        List<Integer> questionMarksCorrect = Lists.newArrayList();
        List<Integer> questionMarksIncorrect = Lists.newArrayList();
        List<Integer> questionMarksNotAttempted = Lists.newArrayList();
        List<Integer> questionMarksTotal = Lists.newArrayList();
        String questionPageId = gameItem.getId();

        IsaacQuestionPage questionPage = (IsaacQuestionPage) this.contentManager.getContentDOById(questionPageId);
        // get all question parts in the question page: depends on each question
        // having an id that starts with the question page id.
        Collection<Question> listOfQuestionParts = getAllMarkableDOQuestionPartsDFSOrder(questionPage);
        Map<String, ? extends List<? extends LightweightQuestionValidationResponse>> questionAttempts =
                questionAttemptsFromUser.get(questionPageId);
        if (questionAttempts != null) {
            for (Content questionPart : listOfQuestionParts) {
                List<? extends LightweightQuestionValidationResponse> questionPartAttempts =
                        questionAttempts.get(questionPart.getId());
                int maximumMarksForThisQuestion = 1;
                if (questionPart instanceof IsaacLLMFreeTextQuestion) {
                    maximumMarksForThisQuestion = ((IsaacLLMFreeTextQuestion) questionPart).getMaxMarks();
                }
                questionMarksTotal.add(maximumMarksForThisQuestion);
                if (questionPartAttempts != null) {
                    // Go through the attempts in reverse chronological order for this question part to determine if
                    // there is a correct answer somewhere.
                    boolean foundCorrectForThisQuestion = false;
                    for (int i = questionPartAttempts.size() - 1; i >= 0; i--) {
                        if (questionPartAttempts.get(i).isCorrect() != null
                                && questionPartAttempts.get(i).isCorrect()) {
                            foundCorrectForThisQuestion = true;
                            break;
                        }
                    }
                    int greatestMarksForThisQuestion = 0;
                    for (LightweightQuestionValidationResponse attempt: questionPartAttempts) {
                        if (attempt.getMarks() > greatestMarksForThisQuestion) {
                            greatestMarksForThisQuestion = attempt.getMarks();
                        }
                    }
                    if (foundCorrectForThisQuestion) {
                        questionPartStates.add(QuestionPartState.CORRECT);
                        questionPartsCorrect++;
                    } else {
                        questionPartStates.add(QuestionPartState.INCORRECT);
                        questionPartsIncorrect++;
                    }
                    questionMarksCorrect.add(greatestMarksForThisQuestion);
                    questionMarksIncorrect.add(maximumMarksForThisQuestion - greatestMarksForThisQuestion);
                } else {
                    questionPartStates.add(QuestionPartState.NOT_ATTEMPTED);
                    questionPartsNotAttempted++;
                    questionMarksNotAttempted.add(maximumMarksForThisQuestion);
                }
            }
        } else {
            questionPartsNotAttempted = listOfQuestionParts.size();
            questionPartStates = listOfQuestionParts.stream()
                    .map(_q -> QuestionPartState.NOT_ATTEMPTED).collect(Collectors.toList());
            questionMarksNotAttempted = listOfQuestionParts.stream()
                    .map(q -> q instanceof IsaacLLMFreeTextQuestion ?
                            ((IsaacLLMFreeTextQuestion) q).getMaxMarks() :
                            1
                    ).collect(Collectors.toList());
            questionMarksTotal = questionMarksNotAttempted;
        }

        // Get the pass mark for the question page
        if (questionPage == null) {
            throw new ResourceNotFoundException(String.format("Unable to locate the question: %s for augmenting",
                    questionPageId));
        }

        float passMark = questionPage.getPassMark() != null ? questionPage.getPassMark() : DEFAULT_QUESTION_PASS_MARK;
        gameItem.setPassMark(passMark);

        gameItem.setQuestionPartsCorrect(questionPartsCorrect);
        gameItem.setQuestionPartsIncorrect(questionPartsIncorrect);
        gameItem.setQuestionPartsNotAttempted(questionPartsNotAttempted);
        int questionPartsTotal = questionPartsCorrect + questionPartsIncorrect + questionPartsNotAttempted;
        gameItem.setQuestionPartsTotal(questionPartsTotal);
        gameItem.setQuestionPartStates(questionPartStates);

        gameItem.setQuestionMarksCorrect(questionMarksCorrect);
        gameItem.setQuestionMarksIncorrect(questionMarksIncorrect);
        gameItem.setQuestionMarksNotAttempted(questionMarksNotAttempted);
        gameItem.setQuestionMarksTotal(questionMarksTotal);

        CompletionState state = UserAttemptManager.getCompletionState(questionPartsTotal, questionPartsCorrect, questionPartsIncorrect);
        gameItem.setState(state);
    }

    /**
     * Get a wildcard by id.
     * 
     * @param id
     *            - of wildcard
     * @return wildcard or an exception.
     * @throws ContentManagerException
     *             - if we cannot access the content requested.
     */
    private IsaacWildcard getWildCardById(final String id) throws ContentManagerException {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(BooleanOperator.AND, ID_FIELDNAME), Collections.singletonList(id));
        fieldsToMap.put(immutableEntry(BooleanOperator.AND, TYPE_FIELDNAME), Collections.singletonList(WILDCARD_TYPE));

        Content wildcardResults = this.contentManager.getContentDOById(id);

        return mapper.map(wildcardResults, IsaacWildcard.class);
    }

    /**
     * Helper method to generate field to match requirements for search queries (specialised for isaac-filtering rules)
     * 
     * This method will decide what should be AND and what should be OR based on the field names used.
     * 
     * @param gameFilter
     *            - filter object containing all the filter information used to make this board.
     * @return A map ready to be passed to a content provider
     */
    private static List<GitContentManager.BooleanSearchClause> generateFieldToMatchForQuestionFilter(
            final GameFilter gameFilter) {

        // Validate that the field sizes are as we expect for tags
        // Check that the query provided adheres to the rules we expect
        if (!validateFilterQuery(gameFilter)) {
            throw new IllegalArgumentException("Error validating filter query.");
        }

        List<GitContentManager.BooleanSearchClause> fieldsToMatch = Lists.newArrayList();

        // handle question categories
        if (null != gameFilter.getQuestionCategories()) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                    TAGS_FIELDNAME, BooleanOperator.OR, gameFilter.getQuestionCategories()));
        }

        // Filter on content tags
        List<String> tagAnds = Lists.newArrayList();
        List<String> tagOrs = Lists.newArrayList();

        // deal with tags which represent subjects, fields and topics
        if (null != gameFilter.getSubjects()) {
            if (gameFilter.getSubjects().size() > 1) {
                tagOrs.addAll(gameFilter.getSubjects());
            } else { // should be exactly 1
                tagAnds.addAll(gameFilter.getSubjects());

                // ok now we are allowed to look at the fields
                if (null != gameFilter.getFields()) {
                    // If multiple fields are chosen, don't filter by field at all, unless there are no topics
                    // /!\ This was changed for the CS question finder, and doesn't break the PHY question finder
                    if (gameFilter.getFields().size() == 1) {
                        tagAnds.addAll(gameFilter.getFields());
                    } else if (null == gameFilter.getTopics()) {
                        tagOrs.addAll(gameFilter.getFields());
                    }
                    // Now we look at topics
                    if (null != gameFilter.getTopics()) {
                        if (gameFilter.getTopics().size() > 1) {
                            tagOrs.addAll(gameFilter.getTopics());
                        } else {
                            tagAnds.addAll(gameFilter.getTopics());
                        }
                    }
                }
            }
        }

        // deal with adding overloaded tags field for subjects, fields and topics
        if (tagAnds.size() > 0) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(TAGS_FIELDNAME, BooleanOperator.AND, tagAnds));
        }
        if (tagOrs.size() > 0) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(TAGS_FIELDNAME, BooleanOperator.OR, tagOrs));
        }

        // now deal with levels
        if (null != gameFilter.getLevels()) {
            List<String> levelsAsStrings = Lists.newArrayList();
            for (Integer levelInt : gameFilter.getLevels()) {
                levelsAsStrings.add(levelInt.toString());
            }
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(LEVEL_FIELDNAME, BooleanOperator.OR, levelsAsStrings));
        }

        // Handle the nested audience fields: stage, difficulty and examBoard
        if (null != gameFilter.getStages()) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                    STAGE_FIELDNAME, BooleanOperator.OR, gameFilter.getStages()));
        }
        if (null != gameFilter.getDifficulties()) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                    DIFFICULTY_FIELDNAME, BooleanOperator.OR, gameFilter.getDifficulties()));
        }
        if (null != gameFilter.getExamBoards()) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                    EXAM_BOARD_FIELDNAME, BooleanOperator.OR, gameFilter.getExamBoards()));
        }

        // handle concepts
        if (null != gameFilter.getConcepts()) {
            fieldsToMatch.add(new GitContentManager.BooleanSearchClause(
                    RELATED_CONTENT_FIELDNAME, BooleanOperator.OR, gameFilter.getConcepts()));
        }

        // handle exclusions
        // exclude questions with no-filter tag
        fieldsToMatch.add(new GitContentManager.BooleanSearchClause(TAGS_FIELDNAME, BooleanOperator.NOT,
                Collections.singletonList(HIDE_FROM_FILTER_TAG)));

        // exclude questions marked deprecated
        fieldsToMatch.add(new GitContentManager.BooleanSearchClause(DEPRECATED_FIELDNAME, BooleanOperator.NOT,
                Collections.singletonList("true")));

        return fieldsToMatch;
    }

    /**
     * Currently only validates subjects, fields and topics.
     * 
     * @param gameFilter
     *            containing the following data: (1) subjects - multiple subjects are only ok if there are not any
     *            fields or topics (2) fields - you can have multiple fields if there is precisely one subject.
     *            (3) topics - you can have multiple topics if there is precisely one subject. (4) levels -
     *            currently not used for validation (5) concepts - currently not used for validation
     * @return true if the query adheres to the rules specified, false if not.
     */
    private static boolean validateFilterQuery(final GameFilter gameFilter) {
        if (null == gameFilter.getSubjects() && null == gameFilter.getFields() && null == gameFilter.getTopics()) {
            return true;
        } else if (null == gameFilter.getSubjects()
                && (null != gameFilter.getFields() || null != gameFilter.getTopics())) {
            log.warn("Error validating query: You cannot have a " + "null subject and still specify fields or topics.");
            return false;
        } else if (null != gameFilter.getSubjects() 
                && null == gameFilter.getFields() && null != gameFilter.getTopics()) {
            log.warn("Error validating query: You cannot have a null field" + " and still specify subject and topics.");
            return false;
        }

        // this variable indicates whether we have found a multiple term query
        // already.
        boolean foundMultipleTerms = false;

        // Now check that the subjects are of the correct size
        if (null != gameFilter.getSubjects() && !gameFilter.getSubjects().isEmpty()) {
            if (gameFilter.getSubjects().size() > 1) {
                foundMultipleTerms = true;
            }
        }

        if (null != gameFilter.getFields() && !gameFilter.getFields().isEmpty()) {
            if (foundMultipleTerms) {
                log.warn("Error validating query: multiple subjects and fields specified.");
                return false;
            }
        }

        // We don't check topics since it no longer matters if multiple fields are specified (after CS question finder addition).
        //  This doesn't change how the PHY question finder works, unless the user uses the URL to specify multiple fields

        return true;
    }

    /**
     * Validate gameboard or throw an exception. For use prior to persistence.
     * 
     * @param gameboardDTO
     *            - to check
     * @throws InvalidGameboardException
     *             - If the gameboard is considered to be invalid.
     */
    private void validateGameboard(final GameboardDTO gameboardDTO) throws InvalidGameboardException {
        if (gameboardDTO.getId() != null && gameboardDTO.getId().contains(" ")) {
            throw new InvalidGameboardException(
                    "Your gameboard must not contain illegal characters e.g. spaces");
        }

        if (gameboardDTO.getContents().size() > gameboardQuestionsLimit) {
            throw new InvalidGameboardException(String.format("Your gameboard must not contain more than %s questions",
                    gameboardQuestionsLimit));
        }

        if (gameboardDTO.getGameFilter() == null || !validateFilterQuery(gameboardDTO.getGameFilter())) {
            throw new InvalidGameboardException("Your gameboard must have some valid filter information " +
                    "e.g. subject must be set.");
        }

        List<String> badQuestions = this.gameboardPersistenceManager.getInvalidQuestionIdsFromGameboard(gameboardDTO);
        if (!badQuestions.isEmpty()) {
            throw new InvalidGameboardException(String.format(
                    "The gameboard provided contains %s invalid (or missing) questions - [%s]", badQuestions.size(),
                    badQuestions));
        }

        if (null == gameboardDTO.getTitle() || gameboardDTO.getTitle().isEmpty()
                || gameboardDTO.getTitle().length() > GAMEBOARD_MAX_TITLE_LENGTH) {
            throw new InvalidGameboardException(String.format(
                    "The gameboard title provided is invalid; the maximum length is %s", GAMEBOARD_MAX_TITLE_LENGTH));
        }

    }
}
