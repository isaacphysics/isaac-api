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

import ma.glasnost.orika.MapperFacade;

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
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.users.User;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;
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
	 */
	public final GameboardDTO generateRandomGameboard() throws NoWildcardException {
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
	 */
	public GameboardDTO generateRandomGameboard(
			final List<String> subjectsList, final List<String> fieldsList,
			final List<String> topicsList, final List<Integer> levelsList,
			final List<String> conceptsList, final User boardOwner) throws NoWildcardException {

		String boardOwnerId = null;
		if (boardOwner != null) {
			boardOwnerId = boardOwner.getDbId();
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

			return augmentGameboardWithUserInformation(gameboardDTO, boardOwner);
		} else {
			return null;
		}
	}

	/**
	 * Store a gameboard in a public location.
	 * 
	 * @param gameboardToStore
	 *            - Gameboard object to persist.
	 * @throws SegueDatabaseException - if there is a problem persisting the gameboard in the database.
	 */
	public final void permanentlyStoreGameboard(
			final GameboardDTO gameboardToStore) throws SegueDatabaseException {
		this.gameboardPersistenceManager
				.saveGameboardToPermanentStorage(gameboardToStore);
	}

	/**
	 * Get a gameboard by its id.
	 * 
	 * @param gameboardId
	 *            - to look up.
	 * @param user
	 *            - the user (if available) of who wants it. This allows state
	 *            information to be retrieved.
	 * @return the gameboard or null.
	 * @throws SegueDatabaseException
	 *             - if there is a problem retrieving the gameboard in the
	 *             database or updating the users gameboard link table.
	 */
	public final GameboardDTO getGameboard(final String gameboardId,
			final User user) throws SegueDatabaseException {
		
		GameboardDTO gameboardFound = augmentGameboardWithUserInformation(
				this.gameboardPersistenceManager.getGameboardById(gameboardId),
				user);
		
		// decide whether or not to create / update the link to the user and the gameboard.
		if (null != gameboardFound && null != user) {
			this.gameboardPersistenceManager.createOrUpdateUserLinkToGameboard(user.getDbId(), gameboardId);
		}
		
		return gameboardFound;
	}

	/**
	 * Lookup gameboards belonging to a current user.
	 * 
	 * @param user
	 *            - the user object for the user of interest.
	 * @param startIndex
	 *            - the initial index to return.
	 * @param limit
	 *            - the limit of the number of results to return
	 * @param showOnly
	 *            - show only gameboards matching the given state.
	 * @return a list of gameboards (without full question information) which are associated with a given user. 
	 * @throws SegueDatabaseException
	 *             - if there is a problem retrieving the gameboards from the
	 *             database.
	 */
	public final List<GameboardDTO> getUsersGameboards(final User user, final Integer startIndex, final Integer limit,
			final GameboardState showOnly) throws SegueDatabaseException {
		Validate.notNull(user);
		
		List<GameboardDTO> usersGameboards = this.gameboardPersistenceManager.getGameboardsByUserId(user);
		
		if (null == usersGameboards || usersGameboards.isEmpty()) {
			return Lists.newArrayList();
		}
		
		List<GameboardDTO> resultToReturn = Lists.newArrayList();
		
		// filter gameboards based on selection and also clear out unnecessary question data.
		for (GameboardDTO gameboard : usersGameboards) {
			this.augmentGameboardWithUserInformation(gameboard, user);
			gameboard.setQuestions(null);
			
			if (null == showOnly) {
				resultToReturn.add(gameboard);
			} else if (gameboard.getPercentageCompleted() == 100 && showOnly.equals(GameboardState.COMPLETED)) {
				resultToReturn.add(gameboard);
			} else if (gameboard.getPercentageCompleted() > 0 && showOnly.equals(GameboardState.IN_PROGRESS)) {
				resultToReturn.add(gameboard);
			}
		}
		
		// assume we want reverse date order for creation date for now.
		Collections.sort(resultToReturn, new Comparator<GameboardDTO>() {
			public int compare(final GameboardDTO o1, final GameboardDTO o2) {
				return o1.getCreationDate().getTime() > o2.getCreationDate().getTime() ? -1 : 1;
			}
		});
		
		int toIndex = startIndex + limit > resultToReturn.size() ? resultToReturn.size() : startIndex + limit;
		
		return resultToReturn.subList(startIndex, toIndex);
	}

	/**
	 * Attempt to calculate the gameboard state from a users history.
	 * 
	 * @param gameboardDTO
	 *            - the DTO of the gameboard
	 * @param user
	 *            - that we are using for the augmentation.
	 * @return Augmented Gameboard
	 */
	public final GameboardDTO augmentGameboardWithUserInformation(
			final GameboardDTO gameboardDTO, final User user) {
		if (null == gameboardDTO) {
			return null;
		}
		if (null == user) {
			return gameboardDTO;
		}

		int totalCompleted = 0;
		
		for (GameboardItem gameItem : gameboardDTO.getQuestions()) {
			GameboardItemState state = this.calculateQuestionState(gameItem.getId(), user);
			gameItem.setState(state);
			if (state.equals(GameboardItemState.COMPLETED)) {
				totalCompleted++;
			}
		}
		
		double percentageCompleted = totalCompleted * 100 / gameboardDTO.getQuestions().size();
		gameboardDTO.setPercentageCompleted((int) Math.round(percentageCompleted));
		
		return gameboardDTO;
	}

	/**
	 * CalculateQuestionState
	 * 
	 * This method will calculate the question state for use in gameboards based
	 * on the question.
	 * 
	 * @param questionPageId
	 *            - the gameboard item id.
	 * @param user
	 *            - the user that may or may not have attempted questions in the
	 *            gameboard.
	 * @return The state of the gameboard item.
	 */
	private GameboardItemState calculateQuestionState(final String questionPageId,
			final User user) {

		if (user.getQuestionAttempts() != null
				&& user.getQuestionAttempts().containsKey(questionPageId)) {
			// go through each question in the question page
			ResultsWrapper<ContentDTO> listOfQuestions = api.searchByIdPrefix(
					api.getLiveVersion(), questionPageId + ID_SEPARATOR);
			
			// go through all of the questions that make up this gameboard item.
			boolean allQuestionsCorrect = true;
			for (ContentDTO contentDTO : listOfQuestions.getResults()) {
				if (!(contentDTO instanceof QuestionDTO)) {
					// we are not interested if this is not a question.
					continue;
				}
				
				// get the attempts for this particular question.
				List<QuestionValidationResponse> questionAttempts = user
						.getQuestionAttempts().get(questionPageId)
						.get(contentDTO.getId());
				
				// If we have an entry for the question page and do not have
				// any attempts for this question then it means that we have
				// done something on this question but have not yet answered
				// all parts.
				if (user.getQuestionAttempts().get(questionPageId) != null
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
