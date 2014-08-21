package uk.ac.cam.cl.dtg.isaac.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;
import ma.glasnost.orika.MapperFacade;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardListDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuickQuestionDTO;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.Constants.SortOrder;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static com.google.common.collect.Maps.*;

/**
 * This class will be responsible for generating and managing gameboards used by
 * users.
 * 
 */
public class GameManager {
	private static final Logger log = LoggerFactory.getLogger(GameManager.class);

	private static final int MAX_QUESTIONS_TO_SEARCH = 20;

	private final SegueApiFacade api;
	private final GameboardPersistenceManager gameboardPersistenceManager;
	private final Random randomGenerator;

	/**
	 * Creates a game manager that operates using the provided api.
	 * 
	 * @param api
	 *            - the api that the game manager can use.
	 * @param gameboardPersistenceManager
	 *            - the gameboardPersistenceManager that handles storage and
	 *            retrieval of gameboards.
	 */
	@Inject
	public GameManager(final SegueApiFacade api,
			final GameboardPersistenceManager gameboardPersistenceManager) {
		this.api = api;

		this.gameboardPersistenceManager = gameboardPersistenceManager;

		this.randomGenerator = new Random();
	}

	/**
	 * Generate a random gameboard without any filter conditions specified.
	 * 
	 * @see generateRandomGambeoard
	 * @return gameboard containing random problems.
	 * @throws NoWildcardException
	 *             - when we are unable to provide you with a wildcard object.
	 * @throws SegueDatabaseException - 
	 */
	public final GameboardDTO generateRandomGameboard() throws NoWildcardException, SegueDatabaseException {
		return this.generateRandomGameboard(null, null, null, null, null, null);
	}

	/**
	 * This method expects only one of its 3 subject tag filter parameters to
	 * have more than one element due to restrictions on the question filter
	 * interface.
	 * 
	 * @param subjectsList
	 *            list of subjects to include in filtered results
	 * @param fieldsList
	 *            list of fields to include in filtered results
	 * @param topicsList
	 *            list of topics to include in filtered results
	 * @param levelsList
	 *            list of levels to include in filtered results
	 * @param conceptsList
	 *            list of concepts (relatedContent) to include in filtered
	 *            results
	 * @param boardOwner
	 *            The user that should be marked as the creator of the
	 *            gameBoard.
	 * @return a gameboard if possible that satisifies the conditions provided
	 *         by the parameters. Will return null if no questions can be
	 *         provided.
	 * @throws NoWildcardException
	 *             - when we are unable to provide you with a wildcard object.
	 * @throws SegueDatabaseException
	 *             - if there is an error contacting the database.
	 */
	public GameboardDTO generateRandomGameboard(
			final List<String> subjectsList, final List<String> fieldsList,
			final List<String> topicsList, final List<Integer> levelsList,
			final List<String> conceptsList, final AbstractSegueUserDTO boardOwner) 
		throws NoWildcardException, SegueDatabaseException {

		String boardOwnerId;
		if (boardOwner instanceof RegisteredUserDTO) {
			boardOwnerId = ((RegisteredUserDTO) boardOwner).getDbId();
		} else {
			boardOwnerId = null;
		}

		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps
				.newHashMap();

		fieldsToMap.put(
				immutableEntry(BooleanOperator.AND, TYPE_FIELDNAME),
				Arrays.asList(QUESTION_TYPE));

		GameFilter gameFilter = new GameFilter(subjectsList, fieldsList,
				topicsList, levelsList, conceptsList);

		fieldsToMap.putAll(generateFieldToMatchForQuestionFilter(gameFilter));

		// Search for questions that match the fields to map variable.
		ResultsWrapper<ContentDTO> results = api
				.findMatchingContentRandomOrder(api.getLiveVersion(),
						fieldsToMap, 0, MAX_QUESTIONS_TO_SEARCH);

		if (!results.getResults().isEmpty()) {
			String uuid = UUID.randomUUID().toString();

			Integer sizeOfGameboard = GAME_BOARD_SIZE;
			if (GAME_BOARD_SIZE > results.getResults().size()) {
				sizeOfGameboard = results.getResults().size();
			}

			List<ContentDTO> questionsForGameboard = results.getResults()
					.subList(0, sizeOfGameboard);
			// TODO: we should probably inject this properly.
			// build gameboard using automapper
			Injector injector = Guice.createInjector(
					new IsaacGuiceConfigurationModule(),
					new SegueGuiceConfigurationModule());

			MapperFacade mapper = injector.getInstance(MapperFacade.class);

			List<GameboardItem> gameboardReadyQuestions = new ArrayList<GameboardItem>();

			// Map each Content object into an IsaacQuestionInfo object
			for (ContentDTO c : questionsForGameboard) {
				GameboardItem questionInfo = mapper.map(c, GameboardItem.class);
				questionInfo.setUri(IsaacController.generateApiUrl(c));
				gameboardReadyQuestions.add(questionInfo);
			}

			log.debug("Created gameboard " + uuid);

			GameboardDTO gameboardDTO = new GameboardDTO(uuid, null,
					gameboardReadyQuestions, getRandomWildcard(mapper),
					generateRandomWildCardPosition(), new Date(), gameFilter,
					boardOwnerId);

			this.gameboardPersistenceManager
					.temporarilyStoreGameboard(gameboardDTO);

			return augmentGameboardWithQuestionAttemptInformation(gameboardDTO, 
					api.getQuestionAttemptsBySession(boardOwner));
		} else {
			// TODO: this should be an exception.
			return null;
		}
	}

	/**
	 * linkUserToGameboard.
	 * Persist the gameboard in permanent storage and also link it to the users account.
	 * @param gameboardToLink - gameboard to store and use as link.
	 * @param userToLinkTo - UserId to link to.
	 * @throws SegueDatabaseException - if there is a problem persisting the gameboard in the database.
	 */
	public void linkUserToGameboard(final GameboardDTO gameboardToLink, final RegisteredUserDTO userToLinkTo)
		throws SegueDatabaseException {
		// if the gameboard has no owner we should add the user who is first linking to it as the owner.
		if (gameboardToLink.getOwnerUserId() == null) {
			gameboardToLink.setOwnerUserId(userToLinkTo.getDbId());
		}
		
		// determine if we need to permanently store the gameboard
		if (!this.gameboardPersistenceManager.isPermanentlyStored(gameboardToLink)) {
			this.permanentlyStoreGameboard(gameboardToLink);	
		}

		this.gameboardPersistenceManager.createOrUpdateUserLinkToGameboard(userToLinkTo.getDbId(),
				gameboardToLink.getId());
	}
	
	/**
	 * This method allows a user to gameboard link to be destroyed. 
	 * @param gameboardToUnlink - the DTO
	 * @param user - DTO
	 * @throws SegueDatabaseException - If there is a problem with the operation.
	 */
	public void unlinkUserToGameboard(final GameboardDTO gameboardToUnlink, final RegisteredUserDTO user)
		throws SegueDatabaseException {		
		this.gameboardPersistenceManager.removeUserLinkToGameboard(user.getDbId(), gameboardToUnlink.getId());
	}
	
	/**
	 * Store a gameboard in a public location.
	 * 
	 * @param gameboardToStore
	 *            - Gameboard object to persist.
	 * @throws SegueDatabaseException - if there is a problem persisting the gameboard in the database.
	 */
	private void permanentlyStoreGameboard(
			final GameboardDTO gameboardToStore) throws SegueDatabaseException {
		this.gameboardPersistenceManager
				.saveGameboardToPermanentStorage(gameboardToStore);
	}

	/**
	 * Get a gameboard by its id.
	 * 
	 * @param gameboardId
	 *            - to look up.
	 * @return the gameboard or null.
	 * @throws SegueDatabaseException
	 *             - if there is a problem retrieving the gameboard in the
	 *             database or updating the users gameboard link table.
	 */
	public final GameboardDTO getGameboard(final String gameboardId) throws SegueDatabaseException {
				
		GameboardDTO gameboardFound = 
				this.gameboardPersistenceManager.getGameboardById(gameboardId);
		
		return gameboardFound;
	}
	
	/**
	 * Get a gameboard by its id and augment with user information.
	 * 
	 * @param gameboardId
	 *            - to look up.
	 * @param user
	 *            - This allows state information to be retrieved.
	 * @return the gameboard or null.
	 * @throws SegueDatabaseException
	 *             - if there is a problem retrieving the gameboard in the
	 *             database or updating the users gameboard link table.
	 */
	public final GameboardDTO getGameboard(final String gameboardId,
			final AbstractSegueUserDTO user) throws SegueDatabaseException {
				
		GameboardDTO gameboardFound = augmentGameboardWithQuestionAttemptInformation(
				this.gameboardPersistenceManager.getGameboardById(gameboardId),
				api.getQuestionAttemptsBySession(user));
		
		return gameboardFound;
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
	 *             - if there is a problem retrieving the gameboards from the
	 *             database.
	 */
	public final GameboardListDTO getUsersGameboards(final RegisteredUserDTO user, 
			@Nullable final Integer startIndex, 
			@Nullable final Integer limit,
			@Nullable final GameboardState showOnly, 
			@Nullable final List<Map.Entry<String, SortOrder>> sortInstructions)
		throws SegueDatabaseException {
		Validate.notNull(user);
		
		List<GameboardDTO> usersGameboards = this.gameboardPersistenceManager.getGameboardsByUserId(user);
		
		Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsFromUser = api
				.getQuestionAttemptsBySession(user);
		
		if (null == usersGameboards || usersGameboards.isEmpty()) {
			return new GameboardListDTO();
		}
		
		List<GameboardDTO> resultToReturn = Lists.newArrayList();
		
		Long totalCompleted = 0L;
		Long totalInProgress = 0L;
		Long totalNotStarted = 0L;
		
		// filter gameboards based on selection and also clear out unnecessary question data.
		for (GameboardDTO gameboard : usersGameboards) {
			this.augmentGameboardWithQuestionAttemptInformation(gameboard, questionAttemptsFromUser);
			gameboard.setQuestions(null);
			
			if (null == showOnly) {
				resultToReturn.add(gameboard);
			} else if (gameboard.getPercentageCompleted() == 0 && showOnly.equals(GameboardState.NOT_ATTEMPTED)) {
				resultToReturn.add(gameboard);
			} else if (gameboard.getPercentageCompleted() == 100 && showOnly.equals(GameboardState.COMPLETED)) {
				resultToReturn.add(gameboard);
			} else if (gameboard.getPercentageCompleted() > 0 && showOnly.equals(GameboardState.IN_PROGRESS)) {
				// in_progress
				resultToReturn.add(gameboard);
			} 
			
			// counts
			if (gameboard.getPercentageCompleted() == 0 && !gameboard.isStartedQuestion()) {
				totalNotStarted++;
			} else if (gameboard.getPercentageCompleted() == 100) {
				totalCompleted++;
			} else if (gameboard.getPercentageCompleted() > 0 || gameboard.isStartedQuestion()) {
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
					comparatorForSorting.addComparator(new Comparator<GameboardDTO>() {
						public int compare(final GameboardDTO o1, final GameboardDTO o2) {
							return o1.getCreationDate().getTime() > o2.getCreationDate().getTime() ? -1 : 1;
						}
					}, reverseOrder);
				} else if (sortInstruction.getKey().equals(VISITED_DATE_FIELDNAME)) {
					comparatorForSorting.addComparator(new Comparator<GameboardDTO>() {
						public int compare(final GameboardDTO o1, final GameboardDTO o2) {
							return o1.getLastVisited().getTime() > o2.getLastVisited().getTime() ? -1 : 1;
						}
					}, reverseOrder);
				}
			}
		}

		if (comparatorForSorting.size() == 0) {
			comparatorForSorting.addComparator(defaultComparitor);
		}
		
		Collections.sort(resultToReturn, comparatorForSorting);
		
		int toIndex = startIndex + limit > resultToReturn.size() ? resultToReturn.size() : startIndex + limit;
		
		GameboardListDTO myBoardsResults = new GameboardListDTO(resultToReturn.subList(startIndex, toIndex),
				(long) resultToReturn.size(), totalNotStarted, totalInProgress, totalCompleted);
		
		return myBoardsResults;
	}

	/**
	 * Augments the gameboards with user information.
	 * 
	 * @param gameboardDTO
	 *            - the DTO of the gameboard
	 * @param questionAttemptsFromUser
	 *            - the users question data.
	 * @return Augmented Gameboard
	 */
	public final GameboardDTO augmentGameboardWithQuestionAttemptInformation(
			final GameboardDTO gameboardDTO,
			final Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsFromUser) {
		if (null == gameboardDTO) {
			return null;
		}
		
		if (null == questionAttemptsFromUser) {
			return gameboardDTO;
		}

		int totalCompleted = 0;
		
		for (GameboardItem gameItem : gameboardDTO.getQuestions()) {
			GameboardItemState state = this.calculateQuestionState(gameItem.getId(), questionAttemptsFromUser);
			gameItem.setState(state);
			if (state.equals(GameboardItemState.COMPLETED)) {
				totalCompleted++;
			}
			
			if (state.equals(GameboardItemState.COMPLETED) || state.equals(GameboardItemState.IN_PROGRESS)) {
				gameboardDTO.setStartedQuestion(true);
			}
		}
		
		double percentageCompleted = totalCompleted * 100 / gameboardDTO.getQuestions().size();
		gameboardDTO.setPercentageCompleted((int) Math.round(percentageCompleted));
		
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
	public final GameboardDTO updateGameboardTitle(final GameboardDTO gameboardWithUpdatedTitle)
		throws SegueDatabaseException {
		return this.gameboardPersistenceManager.updateGameboardTitle(gameboardWithUpdatedTitle);
	}

	/**
	 * CalculateQuestionState
	 * 
	 * This method will calculate the question state for use in gameboards based
	 * on the question.
	 * 
	 * @param questionPageId
	 *            - the gameboard item id.
	 * @param questionAttemptsFromUser
	 *            - the user that may or may not have attempted questions in the
	 *            gameboard.
	 * @return The state of the gameboard item.
	 */
	private GameboardItemState calculateQuestionState(final String questionPageId,
			final Map<String, Map<String, List<QuestionValidationResponse>>> questionAttemptsFromUser) {
		if (questionAttemptsFromUser != null
				&& questionAttemptsFromUser.containsKey(questionPageId)) {
			// go through each question in the question page
			ResultsWrapper<ContentDTO> listOfQuestions = api.searchByIdPrefix(
					api.getLiveVersion(), questionPageId + ID_SEPARATOR);
			
			// go through all of the questions that make up this gameboard item.
			boolean allQuestionsCorrect = true;
			for (ContentDTO contentDTO : listOfQuestions.getResults()) {
				if (!(contentDTO instanceof QuestionDTO) || contentDTO instanceof IsaacQuickQuestionDTO) {
					// we are not interested if this is not a question or if it is a quick question.
					continue;
				}
				
				// get the attempts for this particular question.
				List<QuestionValidationResponse> questionAttempts = questionAttemptsFromUser.get(questionPageId)
						.get(contentDTO.getId());
				
				// If we have an entry for the question page and do not have
				// any attempts for this question then it means that we have
				// done something on this question but have not yet answered
				// all parts.
				if (questionAttemptsFromUser.get(questionPageId) != null
						&& null == questionAttempts) {
					return GameboardItemState.IN_PROGRESS;
				}
				
				boolean foundCorrectForThisQuestion = false;
				
				// Go through the attempts in reverse chronological order
				// for this question to determine if there is a 
				// correct answer somewhere.
				for (int i = questionAttempts.size() - 1; i >= 0; i--) {
					if (questionAttempts.get(i).isCorrect()) {
						foundCorrectForThisQuestion = true;
						break;
					} 
				}
				
				// update the all questions correct variable with the 
				// information from this question.
				allQuestionsCorrect = allQuestionsCorrect & foundCorrectForThisQuestion;

				// if We know that this is now false we can just return and stop looking.
				if (!allQuestionsCorrect) {
					return GameboardItemState.TRY_AGAIN;
				}
				
			}
			// if we get to the end and haven't sent a false back then they must have completed it.
			return GameboardItemState.COMPLETED;
		} else {
			return GameboardItemState.NOT_ATTEMPTED;
		}
	}

	/**
	 * Generate a random integer value to represent the position of the wildcard
	 * tile in the gameboard.
	 * 
	 * @return integer between one and GAME_BOARD_SIZE+1
	 */
	private Integer generateRandomWildCardPosition() {
		return randomGenerator.nextInt(GAME_BOARD_SIZE + 1);
	}

	/**
	 * Find a wildcard object to add to a gameboard.
	 * 
	 * @param mapper
	 *            - to convert between contentDTO to wildcard.
	 * @return wildCard object.
	 * @throws NoWildcardException
	 *             - when we are unable to provide you with a wildcard object.
	 */
	private IsaacWildcard getRandomWildcard(final MapperFacade mapper) throws NoWildcardException {
		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

		fieldsToMap.put(immutableEntry(
				BooleanOperator.OR, TYPE_FIELDNAME), Arrays
				.asList(WILDCARD_TYPE));

		ResultsWrapper<ContentDTO> wildcardResults = api.findMatchingContentRandomOrder(null, fieldsToMap, 0, 1);

		if (wildcardResults.getTotalResults() == 0) {
			throw new NoWildcardException();
		}

		return mapper.map(wildcardResults.getResults().get(0), IsaacWildcard.class);
	}

	/**
	 * Helper method to generate field to match requirements for search queries
	 * (specialised for isaac-filtering rules)
	 * 
	 * This method will decide what should be AND and what should be OR based on
	 * the field names used.
	 * 
	 * @param gameFilter
	 *            - filter object containing all the filter information used to
	 *            make this board.
	 * @return A map ready to be passed to a content provider
	 */
	private static Map<Map.Entry<BooleanOperator, String>, List<String>>
	generateFieldToMatchForQuestionFilter(
			final GameFilter gameFilter) {

		// Validate that the field sizes are as we expect for tags
		// Check that the query provided adheres to the rules we expect
		if (!validateFilterQuery(gameFilter)) {
			throw new IllegalArgumentException("Error validating filter query.");
		}

		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatchOutput = Maps
				.newHashMap();

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
			Map.Entry<BooleanOperator, String> newEntry = immutableEntry(
					BooleanOperator.AND, TAGS_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, ands);
		}
		if (ors.size() > 0) {
			Map.Entry<BooleanOperator, String> newEntry = immutableEntry(
					BooleanOperator.OR, TAGS_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, ors);
		}

		// now deal with levels
		if (null != gameFilter.getLevels()) {
			List<String> levelsAsString = Lists.newArrayList();
			for (Integer levelInt : gameFilter.getLevels()) {
				levelsAsString.add(levelInt.toString());
			}

			Map.Entry<BooleanOperator, String> newEntry = immutableEntry(
					BooleanOperator.OR, LEVEL_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, levelsAsString);
		}

		if (null != gameFilter.getConcepts()) {
			Map.Entry<BooleanOperator, String> newEntry = immutableEntry(
					BooleanOperator.AND, RELATED_CONTENT_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, gameFilter.getConcepts());
		}

		return fieldsToMatchOutput;
	}

	/**
	 * Currently only validates subjects, fields and topics.
	 * 
	 * @param gameFilter
	 *            containing the following data: (1) subjects - multiple
	 *            subjects are only ok if there are not any fields or topics (2)
	 *            fields - multiple fields are only ok if there are not any
	 *            topics. (3) topics - You can have multiple fields only if
	 *            there is precisely one subject and field. (4) levels -
	 *            currently not used for validation (5) concepts - currently not
	 *            used for validation
	 * @return true if the query adheres to the rules specified, false if not.
	 */
	private static boolean validateFilterQuery(final GameFilter gameFilter) {
		if (null == gameFilter.getSubjects() && null == gameFilter.getFields()
				&& null == gameFilter.getTopics()) {
			return true;
		} else if (null == gameFilter.getSubjects()
				&& (null != gameFilter.getFields() || null != gameFilter
						.getTopics())) {
			log.warn("Error validating query: You cannot have a "
					+ "null subject and still specify fields or topics.");
			return false;
		} else if (null != gameFilter.getSubjects()
				&& null == gameFilter.getFields()
				&& null != gameFilter.getTopics()) {
			log.warn("Error validating query: You cannot have a null field"
					+ " and still specify subject and topics.");
			return false;
		}

		// this variable indicates whether we have found a multiple term query
		// already.
		boolean foundMultipleTerms = false;

		// Now check that the subjects are of the correct size
		if (null != gameFilter.getSubjects()) {
			if (gameFilter.getSubjects().size() > 1) {
				foundMultipleTerms = true;
			}
		}

		if (null != gameFilter.getFields()) {
			if (foundMultipleTerms) {
				log.warn("Error validating query: multiple subjects and fields specified.");
				return false;
			}

			if (gameFilter.getFields().size() > 1) {
				foundMultipleTerms = true;
			}
		}

		if (null != gameFilter.getTopics()) {
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

}
