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
package uk.ac.cam.cl.dtg.isaac.api.managers;

import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;
import com.google.inject.name.Named;
import ma.glasnost.orika.MapperFacade;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.Constants.GameboardItemState;
import uk.ac.cam.cl.dtg.isaac.api.Constants.GameboardState;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionPage;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dto.*;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.api.Constants.SortOrder;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.ResourceNotFoundException;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dao.content.ContentManagerException;
import uk.ac.cam.cl.dtg.segue.dao.content.IContentManager;
import uk.ac.cam.cl.dtg.segue.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static com.google.common.collect.Maps.*;

/**
 * This class will be responsible for generating and managing gameboards used by users.
 * 
 */
public class GameManager {
    private static final Logger log = LoggerFactory.getLogger(GameManager.class);

    private static final float DEFAULT_QUESTION_PASS_MARK = 75;

    private static final int MAX_QUESTIONS_TO_SEARCH = 20;
    private static final String HIDE_FROM_FILTER_TAG = "nofilter";

    private final GameboardPersistenceManager gameboardPersistenceManager;
    private final Random randomGenerator;
    private final MapperFacade mapper;

    private final IContentManager contentManager;
    private final String contentIndex;

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
     * @param contentIndex
     */
    @Inject
    public GameManager(final IContentManager contentManager,
                       final GameboardPersistenceManager gameboardPersistenceManager, final MapperFacade mapper,
                       final QuestionManager questionManager, @Named(CONTENT_INDEX) final String contentIndex) {
        this.contentManager = contentManager;
        this.gameboardPersistenceManager = gameboardPersistenceManager;
        this.questionManager = questionManager;
        this.contentIndex = contentIndex;

        this.randomGenerator = new Random();

        this.mapper = mapper;
    }

    /**
     * This method expects only one of its 3 subject tag filter parameters to have more than one element due to
     * restrictions on the question filter interface.
     *
     * @param title
     *            title of the board
     * @param subjectsList
     *            list of subjects to include in filtered results
     * @param fieldsList
     *            list of fields to include in filtered results
     * @param topicsList
     *            list of topics to include in filtered results
     * @param levelsList
     *            list of levels to include in filtered results
     * @param conceptsList
     *            list of concepts (relatedContent) to include in filtered results
     * @param boardOwner
     *            The user that should be marked as the creator of the gameBoard.
     * @return a gameboard if possible that satisifies the conditions provided by the parameters. Will return null if no
     *         questions can be provided.
     * @throws NoWildcardException
     *             - when we are unable to provide you with a wildcard object.
     * @throws SegueDatabaseException
     *             - if there is an error contacting the database.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    public GameboardDTO generateRandomGameboard(final String title, final List<String> subjectsList,
            final List<String> fieldsList, final List<String> topicsList, final List<Integer> levelsList,
            final List<String> conceptsList, final AbstractSegueUserDTO boardOwner) throws NoWildcardException,
            SegueDatabaseException, ContentManagerException {

        Long boardOwnerId;
        if (boardOwner instanceof RegisteredUserDTO) {
            boardOwnerId = ((RegisteredUserDTO) boardOwner).getId();
        } else {
            // anonymous users do not get to own a board so just mark it as unowned.
            boardOwnerId = null;
        }

        Map<String, Map<String, List<QuestionValidationResponse>>> usersQuestionAttempts = questionManager
                .getQuestionAttemptsByUser(boardOwner);

        GameFilter gameFilter = new GameFilter(subjectsList, fieldsList, topicsList, levelsList, conceptsList);

        List<GameboardItem> selectionOfGameboardQuestions = this.getSelectedGameboardQuestions(gameFilter,
                usersQuestionAttempts);

        if (!selectionOfGameboardQuestions.isEmpty()) {
            String uuid = UUID.randomUUID().toString();

            // filter game board ready questions to make up a decent game board.
            log.debug("Created gameboard " + uuid);

            GameboardDTO gameboardDTO = new GameboardDTO(uuid, title, selectionOfGameboardQuestions,
                    getRandomWildcard(mapper, subjectsList), generateRandomWildCardPosition(), new Date(), gameFilter,
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
     * @param gameboardToUnlink
     *            - the DTO
     * @param user
     *            - DTO
     * @throws SegueDatabaseException
     *             - If there is a problem with the operation.
     */
    public void unlinkUserToGameboard(final GameboardDTO gameboardToUnlink, final RegisteredUserDTO user)
            throws SegueDatabaseException {
        this.gameboardPersistenceManager.removeUserLinkToGameboard(user.getId(), gameboardToUnlink.getId());
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

        GameboardDTO gameboardFound = this.gameboardPersistenceManager.getGameboardById(gameboardId);

        return gameboardFound;
    }

    /**
     * Get a list of gameboards by their ids.
     *
     * Note: These gameboards will not be augmented with user information.
     *
     * @param gameboardIds
     *            - to look up.
     * @return the gameboards or null.
     * @throws SegueDatabaseException
     *             - if there is a problem retrieving the gameboards in the database or updating the users gameboards
     *             link table.
     */
    public final List<GameboardDTO> getGameboards(final List<String> gameboardIds) throws SegueDatabaseException {
        if (null == gameboardIds || gameboardIds.isEmpty()) {
            return null;
        }

        List<GameboardDTO> gameboardsFound = this.gameboardPersistenceManager.getGameboardsByIds(gameboardIds);

        return gameboardsFound;
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
            final Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts)
            throws SegueDatabaseException, ContentManagerException {


        // we need to augment the DTO with whether this gameboard is in a users my boards list.
        GameboardDTO gameboardFound = augmentGameboardWithQuestionAttemptInformationAndUserInformation(
                this.gameboardPersistenceManager.getGameboardById(gameboardId), userQuestionAttempts, user);

        return gameboardFound;
    }

    public final List<GameboardItem> getFastTrackConceptProgress(final String gameboardId, final String conceptTitle,
             final Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts)
            throws ContentManagerException {
        List<ContentDTO> fastTrackAssociatedQuestions = this.getFastTrackConceptQuestions(gameboardId, conceptTitle);
        return this.getGameboardItemProgress(fastTrackAssociatedQuestions, userQuestionAttempts);
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
        Validate.notNull(user);

        List<GameboardDTO> usersGameboards = this.gameboardPersistenceManager.getGameboardsByUserId(user);
        Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsFromUser = questionManager
                .getQuestionAttemptsByUser(user);

        if (null == usersGameboards || usersGameboards.isEmpty()) {
            return new GameboardListDTO();
        }

        List<GameboardDTO> resultToReturn = Lists.newArrayList();

        Long totalCompleted = 0L;
        Long totalInProgress = 0L;
        Long totalNotStarted = 0L;

        // filter gameboards based on selection.
        for (GameboardDTO gameboard : usersGameboards) {
            this.augmentGameboardWithQuestionAttemptInformation(gameboard, questionAttemptsFromUser);

            // we know that the user already has these boards in their my boards page so just set them to true
            if (user instanceof RegisteredUserDTO) {
                gameboard
                        .setSavedToCurrentUser(true);
            }

            if (null == showOnly) {
                resultToReturn.add(gameboard);
            } else if (gameboard.isStartedQuestion() && showOnly.equals(GameboardState.IN_PROGRESS)) {
                resultToReturn.add(gameboard);
            } else if (!gameboard.isStartedQuestion() && showOnly.equals(GameboardState.NOT_ATTEMPTED)) {
                resultToReturn.add(gameboard);
            } else if (gameboard.getPercentageCompleted() == 100 && showOnly.equals(GameboardState.COMPLETED)) {
                resultToReturn.add(gameboard);
            }

            // counts
            if (!gameboard.isStartedQuestion()) {
                totalNotStarted++;
            } else if (gameboard.getPercentageCompleted() == 100) {
                totalCompleted++;
            } else if (gameboard.isStartedQuestion()) {
                totalInProgress++;
            }
        }

        ComparatorChain<GameboardDTO> comparatorForSorting = new ComparatorChain<GameboardDTO>();
        Comparator<GameboardDTO> defaultComparitor = new Comparator<GameboardDTO>() {
            public int compare(final GameboardDTO o1, final GameboardDTO o2) {
                return o1.getLastVisited().getTime() > o2.getLastVisited().getTime() ? -1 : 1;
            }
        };

        // assume we want reverse date order for visited date for now.
        if (null == sortInstructions || sortInstructions.isEmpty()) {
            comparatorForSorting.addComparator(defaultComparitor);
        } else {
            // we have to use a more complex sorting Comparator.

            for (Map.Entry<String, SortOrder> sortInstruction : sortInstructions) {
                Boolean reverseOrder = false;
                if (sortInstruction.getValue().equals(SortOrder.DESC)) {
                    reverseOrder = true;
                }

                if (sortInstruction.getKey().equals(CREATED_DATE_FIELDNAME)) {
                    comparatorForSorting.addComparator((o1, o2) -> {
                        if (o1.getCreationDate().getTime() == o2.getCreationDate().getTime()) {
                            return 0;
                        } else {
                            return o1.getCreationDate().getTime() > o2.getCreationDate().getTime() ? -1 : 1;
                        }
                    }, reverseOrder);
                } else if (sortInstruction.getKey().equals(VISITED_DATE_FIELDNAME)) {
                    comparatorForSorting.addComparator((o1, o2) -> {
                        if (o1.getLastVisited().getTime() == o2.getLastVisited().getTime()) {
                            return 0;
                        } else {
                            return o1.getLastVisited().getTime() > o2.getLastVisited().getTime() ? -1 : 1;
                        }
                    }, reverseOrder);
                }  else if (sortInstruction.getKey().equals(TITLE_FIELDNAME)) {
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
                }
            }
        }

        if (comparatorForSorting.size() == 0) {
            comparatorForSorting.addComparator(defaultComparitor);
        }

        Collections.sort(resultToReturn, comparatorForSorting);

        int toIndex;
        if (limit == null || startIndex + limit > resultToReturn.size()) {
            toIndex = resultToReturn.size();
        } else {
            toIndex = startIndex + limit;
        }

        List<GameboardDTO> sublistOfGameboards = resultToReturn.subList(startIndex, toIndex);

        // fully augment only those we are returning.
        this.gameboardPersistenceManager.augmentGameboardItems(sublistOfGameboards);

        GameboardListDTO myBoardsResults = new GameboardListDTO(sublistOfGameboards, (long) resultToReturn.size(),
                totalNotStarted, totalInProgress, totalCompleted);

        return myBoardsResults;
    }

    /**
     * @param gameboardDTO
     *            - to save
     * @param owner
     *            - user to make owner of gameboard.
     * @return gameboardDTO as persisted
     * @throws NoWildcardException
     *             - if we cannot add a wildcard.
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
            throws NoWildcardException, InvalidGameboardException, SegueDatabaseException, DuplicateGameboardException,
            ContentManagerException {
        Validate.notNull(gameboardDTO);
        Validate.notNull(owner);

        String gameboardId = gameboardDTO.getId();
        if (gameboardId == null) {
            gameboardDTO.setId(UUID.randomUUID().toString());
        } else if (this.getGameboard(gameboardId.toLowerCase()) != null) {
            throw new DuplicateGameboardException();
        } else {
            gameboardDTO.setId(gameboardId.toLowerCase());
        }

        if (gameboardDTO.getWildCard() == null) {
            gameboardDTO.setWildCard(getRandomWildcard(mapper, gameboardDTO.getGameFilter().getSubjects()));
        } 
        
        if (gameboardDTO.getWildCardPosition() == null) {
            gameboardDTO.setWildCardPosition(this.generateRandomWildCardPosition());
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
     * @return useful for providing an indication of how many people are sharing boards.
     * @throws SegueDatabaseException
     *             - if there is a problem updating the gameboard.
     */
    public Map<String, Integer> getNumberOfConnectedUsersByGameboard() 
            throws SegueDatabaseException {
        Map<String, List<String>> boardToUserIdMapping = this.gameboardPersistenceManager.getBoardToUserIdMapping();

        Map<String, Integer> result = Maps.newTreeMap();

        for (java.util.Map.Entry<String, List<String>> e : boardToUserIdMapping.entrySet()) {
            result.put(e.getKey(), e.getValue().size());
        }

        return result;
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
            throws SegueDatabaseException {
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
        Validate.notNull(users);
        Validate.notNull(gameboard);

        List<ImmutablePair<RegisteredUserDTO, List<GameboardItem>>> result = Lists.newArrayList();

        List<String> questionPageIds =
                gameboard.getQuestions().stream().map(GameboardItem::getId).collect(Collectors.toList());

        Map<Long, Map<String, Map<String, List<LightweightQuestionValidationResponse>>>>
                questionAttemptsForAllUsersOfInterest =
                questionManager.getMatchingQuestionAttempts(users, questionPageIds);

        for (RegisteredUserDTO user : users) {
            List<GameboardItem> userGameItems = Lists.newArrayList();

            for (GameboardItem observerGameItem : gameboard.getQuestions()) {
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
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, TYPE_FIELDNAME), Collections.singletonList(WILDCARD_TYPE));

        Map<String, SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(TITLE_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, SortOrder.ASC);

        ResultsWrapper<ContentDTO> wildcardResults = this.contentManager.findByFieldNames(
                this.contentIndex, fieldsToMap, 0, -1, sortInstructions);

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
    public Collection<QuestionDTO> getAllMarkableQuestionPartsDFSOrder(final String questionPageId)
            throws ContentManagerException {
        Validate.notBlank(questionPageId);

        // do a depth first traversal of the question page to get the correct order of questions
        ContentDTO questionPage = this.contentManager.getContentById(this.contentManager.getCurrentContentSHA(),
                questionPageId);
        List<ContentDTO> dfs = Lists.newArrayList();
        dfs = depthFirstQuestionSearch(questionPage, dfs);

        return this.filterQuestionParts(dfs);
    }

    /**
     * Augments the gameboards with question attempt information AND whether or not the user has it in their my board page.
     *
     * @param gameboardDTO
     *            - the DTO of the gameboard.
     * @param questionAttemptsFromUser
     *            - the users question attempt data.
     * @return Augmented Gameboard.
     * @throws SegueDatabaseException
     *             - if there is an error retrieving the content requested.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     */
    private GameboardDTO augmentGameboardWithQuestionAttemptInformationAndUserInformation(final GameboardDTO gameboardDTO,
                                                                                          final Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsFromUser, AbstractSegueUserDTO user)
            throws SegueDatabaseException, ContentManagerException {
        if (user instanceof RegisteredUserDTO) {
            gameboardDTO
                    .setSavedToCurrentUser(this.isBoardLinkedToUser((RegisteredUserDTO) user, gameboardDTO.getId()));
        }

        this.augmentGameboardWithQuestionAttemptInformation(gameboardDTO, questionAttemptsFromUser);

        return gameboardDTO;
    }


    /**
     * Augments the gameboards with question attempt information NOT whether the user has it in their my board page
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
                                                                        final Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsFromUser)
            throws ContentManagerException {
        if (null == gameboardDTO) {
            return null;
        }

        if (null == questionAttemptsFromUser || gameboardDTO.getQuestions().size() == 0) {
            return gameboardDTO;
        }

        boolean gameboardStarted = false;
        List<GameboardItem> questions = gameboardDTO.getQuestions();
        int totalNumberOfQuestionsParts = 0;
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

            if (!gameboardStarted && !gameItem.getState().equals(Constants.GameboardItemState.NOT_ATTEMPTED)) {
                gameboardStarted = true;
                gameboardDTO.setStartedQuestion(gameboardStarted);
            }
            totalNumberOfQuestionsParts += gameItem.getQuestionPartsTotal();
            totalNumberOfCorrectQuestionParts += gameItem.getQuestionPartsCorrect();
        }

        float boardPercentage = 100f * totalNumberOfCorrectQuestionParts / totalNumberOfQuestionsParts;
        gameboardDTO.setPercentageCompleted(Math.round(boardPercentage));

        return gameboardDTO;
    }

    /**
     * Convert a list of questions to gameboard items and augment with user question attempt information.
     *
     * @param questions list of questions.
     * @param userQuestionAttempts the user's question attempt history.
     * @return list of augmented gameboard items.
     */
    private List<GameboardItem> getGameboardItemProgress(List<ContentDTO> questions,
                                                         final Map<String, Map<String, List<QuestionValidationResponse>>> userQuestionAttempts) {

        return questions.stream()
                .map(this.gameboardPersistenceManager::convertToGameboardItem)
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
    private Collection<QuestionDTO> filterQuestionParts(final Collection<ContentDTO> contentToFilter) {
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
     * We want to list the questions in the order they are seen.
     * @param c - content to search
     * @param result - the list of questions
     * @return a list of questions ordered by DFS.
     */
    private List<ContentDTO> depthFirstQuestionSearch(final ContentDTO c, final List<ContentDTO> result) {
        if (c == null || c.getChildren() == null || c.getChildren().size() == 0) {
            return result;
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
        while (gameboardReadyQuestions.size() < GAME_BOARD_TARGET_SIZE && !selectionOfGameboardQuestions.isEmpty()) {
            for (GameboardItem gameboardItem : selectionOfGameboardQuestions) {
                GameboardItemState questionState;
                try {
                    this.augmentGameItemWithAttemptInformation(gameboardItem, usersQuestionAttempts);
                    questionState = gameboardItem.getState();
                } catch (ResourceNotFoundException e) {
                    throw new ContentManagerException(
                            "Resource not found exception, this shouldn't happen as the selectionOfGameboardQuestions "
                            + "should only show available content.");
                }
                
                if (questionState.equals(GameboardItemState.PASSED) 
                        || questionState.equals(GameboardItemState.PERFECT)) {
                    completedQuestions.add(gameboardItem);
                }
                gameboardReadyQuestions.add(gameboardItem);

                // stop inner loop if we have reached our target
                if (gameboardReadyQuestions.size() == GAME_BOARD_TARGET_SIZE) {
                    break;
                }
            }

            if (gameboardReadyQuestions.size() == GAME_BOARD_TARGET_SIZE) {
                break;
            }

            // increment search start index to see if we can get more questions for the given criteria.
            searchIndex = searchIndex + selectionOfGameboardQuestions.size();

            // we couldn't fill it up on the last round of questions so lets get more if no more this will be empty
            selectionOfGameboardQuestions = this.getNextQuestionsForFilter(gameFilter, searchIndex, seed);
        }

        // Try and make up the difference with completed ones if we haven't reached our target size
        if (gameboardReadyQuestions.size() < GAME_BOARD_TARGET_SIZE && !completedQuestions.isEmpty()) {
            for (GameboardItem completedQuestion : completedQuestions) {
                if (gameboardReadyQuestions.size() < GAME_BOARD_TARGET_SIZE) {
                    gameboardReadyQuestions.add(completedQuestion);
                } else if (gameboardReadyQuestions.size() == GAME_BOARD_TARGET_SIZE) {
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
     * Queries the search provider for questions tagged with this board name and concept title.
     * The result is returned sorted.
     *
     * @param boardTag the tag which marks question's association with a certain board - the board's ID.
     * @param conceptTitle the title of the concept which is being searched for.
     * @return ordered list of concept questions associated with the board.
     * @throws ContentManagerException if there is a problem with the content manager (i.e. Elasticsearch)
     */
    private List<ContentDTO> getFastTrackConceptQuestions(final String boardTag, final String conceptTitle)
            throws ContentManagerException {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(
                BooleanOperator.OR, TYPE_FIELDNAME), Arrays.asList(QUESTION_TYPE, FAST_TRACK_QUESTION_TYPE));
        fieldsToMap.put(immutableEntry(
                BooleanOperator.AND, TITLE_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX), Collections.singletonList(conceptTitle));
        fieldsToMap.put(immutableEntry(
                BooleanOperator.AND, TAGS_FIELDNAME), Collections.singletonList(boardTag));

        Map<String, SortOrder> sortInstructions = Maps.newHashMap();
        sortInstructions.put(ID_FIELDNAME + "." + UNPROCESSED_SEARCH_FIELD_SUFFIX, SortOrder.ASC);

        List<ContentDTO> conceptQuestions = this.contentManager.findByFieldNames(
                    this.contentIndex, fieldsToMap, 0, SEARCH_MAX_WINDOW_SIZE, sortInstructions).getResults();
        return conceptQuestions;
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
    private List<GameboardItem> getNextQuestionsForFilter(final GameFilter gameFilter, final int index,
            final Long randomSeed) throws ContentManagerException {
        // get some questions
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();
        fieldsToMap.put(immutableEntry(BooleanOperator.AND, TYPE_FIELDNAME), Collections.singletonList(QUESTION_TYPE));
        fieldsToMap.putAll(generateFieldToMatchForQuestionFilter(gameFilter));

        // Search for questions that match the fields to map variable.

        ResultsWrapper<ContentDTO> results = this.contentManager.findByFieldNamesRandomOrder(
                this.contentIndex, fieldsToMap, index, MAX_QUESTIONS_TO_SEARCH, randomSeed);

        List<ContentDTO> questionsForGameboard = results.getResults();

        List<GameboardItem> selectionOfGameboardQuestions = Lists.newArrayList();

        // Map each Content object into an GameboardItem object
        for (ContentDTO c : questionsForGameboard) {
            // Only keep questions that have not been superseded.
            // Yes, this should probably be done in the fieldsToMap filter above, but this is simpler.
            if (c instanceof IsaacQuestionPageDTO) {
                IsaacQuestionPageDTO qp = (IsaacQuestionPageDTO)c;
                if (qp.getSupersededBy() != null && !qp.getSupersededBy().equals("")) {
                    // This question has been superseded. Don't include it.
                    continue;
                }
            }

            GameboardItem questionInfo = this.gameboardPersistenceManager.convertToGameboardItem(c);
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
     * @return gameItem
     *             - the gameItem passed in having been modified (augmented)), returned for possiblity of chaining.
     * @throws ContentManagerException
     *             - if there is an error retrieving the content requested.
     * @throws ResourceNotFoundException
     *             - if we cannot find the question specified.
     */
    private GameboardItem augmentGameItemWithAttemptInformation(
            final GameboardItem gameItem,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>>
                    questionAttemptsFromUser)
            throws ContentManagerException, ResourceNotFoundException {
        Validate.notNull(gameItem, "gameItem cannot be null");
        Validate.notNull(questionAttemptsFromUser, "questionAttemptsFromUser cannot be null");

        List<QuestionPartState> questionPartStates = Lists.newArrayList();
        int questionPartsCorrect = 0;
        int questionPartsIncorrect = 0;
        int questionPartsNotAttempted = 0;
        String questionPageId = gameItem.getId();

        // get all question parts in the question page: depends on each question
        // having an id that starts with the question page id.
        Collection<QuestionDTO> listOfQuestionParts = getAllMarkableQuestionPartsDFSOrder(questionPageId);
        Map<String, ? extends List<? extends LightweightQuestionValidationResponse>> questionAttempts =
                questionAttemptsFromUser.get(questionPageId);
        if (questionAttempts != null) {
            for (ContentDTO questionPart : listOfQuestionParts) {
                List<? extends LightweightQuestionValidationResponse> questionPartAttempts =
                        questionAttempts.get(questionPart.getId());
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
                    if (foundCorrectForThisQuestion) {
                        questionPartStates.add(QuestionPartState.CORRECT);
                        questionPartsCorrect++;
                    } else {
                        questionPartStates.add(QuestionPartState.INCORRECT);
                        questionPartsIncorrect++;
                    }
                } else {
                    questionPartStates.add(QuestionPartState.NOT_ATTEMPTED);
                    questionPartsNotAttempted++;
                }
            }
        } else {
            questionPartsNotAttempted = listOfQuestionParts.size();
            questionPartStates = listOfQuestionParts.stream()
                    .map(_q -> QuestionPartState.NOT_ATTEMPTED).collect(Collectors.toList());
        }

        // Get the pass mark for the question page
        IsaacQuestionPage questionPage = (IsaacQuestionPage) this.contentManager.getContentDOById(
                this.contentManager.getCurrentContentSHA(), questionPageId);
        if (questionPage == null) {
            throw new ResourceNotFoundException(String.format("Unable to locate the question: %s for augmenting",
                    questionPageId));
        }
        float passMark = questionPage.getPassMark() != null ? questionPage.getPassMark() : DEFAULT_QUESTION_PASS_MARK;
        gameItem.setPassMark(passMark);
        gameItem.setQuestionPartsCorrect(questionPartsCorrect);
        gameItem.setQuestionPartsIncorrect(questionPartsIncorrect);
        gameItem.setQuestionPartsNotAttempted(questionPartsNotAttempted);
        gameItem.setQuestionPartStates(questionPartStates);
        Integer questionPartsTotal = questionPartsCorrect + questionPartsIncorrect + questionPartsNotAttempted;
        gameItem.setQuestionPartsTotal(questionPartsTotal);
        Float percentCorrect = 100f * questionPartsCorrect / questionPartsTotal;
        Float percentIncorrect = 100f * questionPartsIncorrect / questionPartsTotal;

        GameboardItemState state;
        if (questionPartsCorrect == questionPartsTotal) {
            state = GameboardItemState.PERFECT;
        } else if (questionPartsNotAttempted == questionPartsTotal) {
            state = GameboardItemState.NOT_ATTEMPTED;
        } else if (percentCorrect >= gameItem.getPassMark()) {
            state = GameboardItemState.PASSED;
        } else if (percentIncorrect > (100 - gameItem.getPassMark())) {
            state = GameboardItemState.FAILED;
        } else {
            state = GameboardItemState.IN_PROGRESS;
        }
        gameItem.setState(state);

        return gameItem;
    }
    
    /**
     * Generate a random integer value to represent the position of the wildcard tile in the gameboard.
     * 
     * @return integer between one and GAME_BOARD_SIZE+1
     */
    private Integer generateRandomWildCardPosition() {
        return randomGenerator.nextInt(GAME_BOARD_TARGET_SIZE + 1);
    }

    /**
     * Find a wildcard object to add to a gameboard.
     * 
     * @param mapper
     *            - to convert between contentDTO to wildcard.
     * @return wildCard object.
     * @throws NoWildcardException
     *             - when we are unable to provide you with a wildcard object.
     * @throws ContentManagerException
     *             - if we cannot access the content requested.
     */
    private IsaacWildcard getRandomWildcard(final MapperFacade mapper, final List<String> subjectsList) throws NoWildcardException,
            ContentManagerException {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(BooleanOperator.OR, TYPE_FIELDNAME), Collections.singletonList(WILDCARD_TYPE));

        // FIXME - the 999 is a magic number because using NO_SEARCH_LIMIT doesn't work for all elasticsearch queries!
        ResultsWrapper<ContentDTO> wildcardResults = this.contentManager.findByFieldNamesRandomOrder(
                this.contentIndex, fieldsToMap, 0, 999);

        // try to increase randomness of wildcard results.
        Collections.shuffle(wildcardResults.getResults());

        List<ContentDTO> wildcards = new ArrayList<>();

        if (null == subjectsList) {
            // If we have no subject info, just use any wildcard; to match behavior of questions.
            wildcards.addAll(wildcardResults.getResults());
        } else {
            for (ContentDTO c : wildcardResults.getResults()) {
                boolean match = false;
                for (String s : subjectsList) {
                    if (c.getTags().contains(s)) {
                        match = true;
                        break;
                    }
                }

                if (match) {
                    wildcards.add(c);
                }
            }
        }

        if (wildcards.size() == 0) {
            throw new NoWildcardException();
        }

        return mapper.map(wildcards.get(0), IsaacWildcard.class);
    }

    /**
     * Get a wildcard by id.
     * 
     * @param id
     *            - of wildcard
     * @return wildcard or an exception.
     * @throws NoWildcardException
     *             - if we cannot locate the exception.
     * @throws ContentManagerException
     *             - if we cannot access the content requested.
     */
    private IsaacWildcard getWildCardById(final String id) throws NoWildcardException, ContentManagerException {
        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

        fieldsToMap.put(immutableEntry(BooleanOperator.AND, ID_FIELDNAME), Collections.singletonList(id));
        fieldsToMap.put(immutableEntry(BooleanOperator.AND, TYPE_FIELDNAME), Collections.singletonList(WILDCARD_TYPE));

        Content wildcardResults = this.contentManager.getContentDOById(this.contentManager.getCurrentContentSHA(), id);

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
    private static Map<Map.Entry<BooleanOperator, String>, List<String>> generateFieldToMatchForQuestionFilter(
            final GameFilter gameFilter) {

        // Validate that the field sizes are as we expect for tags
        // Check that the query provided adheres to the rules we expect
        if (!validateFilterQuery(gameFilter)) {
            throw new IllegalArgumentException("Error validating filter query.");
        }

        Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatchOutput = Maps.newHashMap();

        // Deal with tags which represent subjects, fields and topics
        List<String> ands = Lists.newArrayList();
        List<String> ors = Lists.newArrayList();

        if (null != gameFilter.getSubjects()) {
            if (gameFilter.getSubjects().size() > 1) {
                ors.addAll(gameFilter.getSubjects());
            } else { // should be exactly 1
                ands.addAll(gameFilter.getSubjects());

                // ok now we are allowed to look at the fields
                if (null != gameFilter.getFields()) {
                    if (gameFilter.getFields().size() > 1) {
                        ors.addAll(gameFilter.getFields());
                    } else { // should be exactly 1
                        ands.addAll(gameFilter.getFields());

                        if (null != gameFilter.getTopics()) {
                            if (gameFilter.getTopics().size() > 1) {
                                ors.addAll(gameFilter.getTopics());
                            } else {
                                ands.addAll(gameFilter.getTopics());
                            }
                        }
                    }
                }
            }
        }

        // deal with adding overloaded tags field for subjects, fields and
        // topics
        if (ands.size() > 0) {
            Map.Entry<BooleanOperator, String> newEntry = immutableEntry(BooleanOperator.AND, TAGS_FIELDNAME);
            fieldsToMatchOutput.put(newEntry, ands);
        }
        if (ors.size() > 0) {
            Map.Entry<BooleanOperator, String> newEntry = immutableEntry(BooleanOperator.OR, TAGS_FIELDNAME);
            fieldsToMatchOutput.put(newEntry, ors);
        }

        // now deal with levels
        if (null != gameFilter.getLevels()) {
            List<String> levelsAsString = Lists.newArrayList();
            for (Integer levelInt : gameFilter.getLevels()) {
                levelsAsString.add(levelInt.toString());
            }

            Map.Entry<BooleanOperator, String> newEntry = immutableEntry(BooleanOperator.OR, LEVEL_FIELDNAME);
            fieldsToMatchOutput.put(newEntry, levelsAsString);
        }

        if (null != gameFilter.getConcepts()) {
            Map.Entry<BooleanOperator, String> newEntry 
                = immutableEntry(BooleanOperator.AND, RELATED_CONTENT_FIELDNAME);
            fieldsToMatchOutput.put(newEntry, gameFilter.getConcepts());
        }

        // add no filter constraint
        fieldsToMatchOutput.put(
                immutableEntry(BooleanOperator.NOT, TAGS_FIELDNAME), Collections.singletonList(HIDE_FROM_FILTER_TAG));

        return fieldsToMatchOutput;
    }

    /**
     * Currently only validates subjects, fields and topics.
     * 
     * @param gameFilter
     *            containing the following data: (1) subjects - multiple subjects are only ok if there are not any
     *            fields or topics (2) fields - multiple fields are only ok if there are not any topics. (3) topics -
     *            You can have multiple fields only if there is precisely one subject and field. (4) levels - currently
     *            not used for validation (5) concepts - currently not used for validation
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

            if (gameFilter.getFields().size() > 1) {
                foundMultipleTerms = true;
            }
        }

        if (null != gameFilter.getTopics() && !gameFilter.getTopics().isEmpty()) {
            if (foundMultipleTerms) {
                log.warn("Error validating query: multiple fields and topics specified.");
                return false;
            }

            if (gameFilter.getTopics().size() > 1) {
                foundMultipleTerms = true;
            }
        }
        return true;
    }

    /**
     * Provides validation for a given gameboard. For use prior to persistence.
     * 
     * @param gameboardDTO
     *            - to check
     * @return the gameboard (unchanged) if everything is ok, otherwise an exception will be thrown.
     * @throws InvalidGameboardException
     *             - If the gameboard is considered to be invalid.
     * @throws NoWildcardException
     *             - if the wildcard cannot be found.
     */
    private GameboardDTO validateGameboard(final GameboardDTO gameboardDTO) throws InvalidGameboardException,
            NoWildcardException {
        if (gameboardDTO.getId() != null && gameboardDTO.getId().contains(" ")) {
            throw new InvalidGameboardException(
                    String.format("Your gameboard must not contain illegal characters e.g. spaces"));
        }

        if (gameboardDTO.getQuestions().size() > Constants.GAME_BOARD_TARGET_SIZE) {
            throw new InvalidGameboardException(String.format("Your gameboard must not contain more than %s questions",
                    GAME_BOARD_TARGET_SIZE));
        }

        if (gameboardDTO.getGameFilter() == null || !validateFilterQuery(gameboardDTO.getGameFilter())) {
            throw new InvalidGameboardException(String.format(
                    "Your gameboard must have some valid filter information e.g. subject must be set.",
                    GAME_BOARD_TARGET_SIZE));
        }

        List<String> badQuestions = this.gameboardPersistenceManager.getInvalidQuestionIdsFromGameboard(gameboardDTO);
        if (badQuestions.size() > 0) {
            throw new InvalidGameboardException(String.format(
                    "The gameboard provided contains %s invalid (or missing) questions - [%s]", badQuestions.size(),
                    badQuestions));
        }

        if (gameboardDTO.getWildCard() == null) {
            throw new NoWildcardException();
        }

        // This will throw a NoWildCardException if we cannot locate a valid
        // wildcard for this gameboard.
        try {
            this.getWildCardById(gameboardDTO.getWildCard().getId());
        } catch (NoWildcardException e) {
            throw new InvalidGameboardException(String.format(
                    "The gameboard provided contains an invalid wildcard with id [%s]", gameboardDTO.getWildCard()
                            .getId()));
        } catch (ContentManagerException e) {
            log.error("Error validating gameboard.", e);
            throw new InvalidGameboardException(
                    "There was a problem validating the gameboard due to ContentManagerException another exception.");
        }

        return gameboardDTO;
    }
}
