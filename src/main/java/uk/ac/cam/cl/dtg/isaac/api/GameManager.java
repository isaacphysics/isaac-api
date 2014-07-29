package uk.ac.cam.cl.dtg.isaac.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import ma.glasnost.orika.MapperFacade;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dos.Wildcard;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dos.users.QuestionAttempt;
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
	private static final Logger log = LoggerFactory
			.getLogger(GameManager.class);

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
	 */
	public final GameboardDTO generateRandomGameboard() {
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
	 */
	public GameboardDTO generateRandomGameboard(
			final List<String> subjectsList, final List<String> fieldsList,
			final List<String> topicsList, final List<Integer> levelsList,
			final List<String> conceptsList, final User boardOwner) {

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
			// TODO: if there are not enough questions then should we
			// fill it with random questions or just say so?
			if (GAME_BOARD_SIZE > results.getResults().size()) {
				sizeOfGameboard = results.getResults().size();
			}

			List<ContentDTO> questionsForGameboard = results.getResults()
					.subList(0, sizeOfGameboard);

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
					gameboardReadyQuestions, null,
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
	 */
	public final void permanentlyStoreGameboard(
			final GameboardDTO gameboardToStore) {
		this.gameboardPersistenceManager
				.saveGameboardToPermanentStorage(gameboardToStore);
	}

	/**
	 * Get a gameboard by its id.
	 * 
	 * @param gameboardId
	 *            - to look up.
	 * @param user
	 *            - the user (if available) of who wants it. THis allows state
	 *            information to be retrieved.
	 * @return the gameboard or null.
	 */
	public final GameboardDTO getGameboard(final String gameboardId,
			final User user) {
		return augmentGameboardWithUserInformation(
				this.gameboardPersistenceManager.getGameboardById(gameboardId),
				user);
	}

	/**
	 * Lookup gameboards belonging to a current user.
	 * 
	 * @param userId
	 *            - the id of the user to search.
	 * @return a list of gameboards created and owned by the given userId
	 */
	public final List<GameboardDTO> getUsersGameboards(final String userId) {
		return this.gameboardPersistenceManager.getGameboardsByUserId(userId);
	}

	/**
	 * Find a wildcard object to add to a gameboard.
	 * 
	 * @return wildCard
	 */
	public final Wildcard getRandomWildcardTile() {
		// TODO: stub
		return null;
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

		for (GameboardItem gameItem : gameboardDTO.getQuestions()) {
			gameItem.setState(this.calculateQuestionState(gameItem, user));
		}

		return gameboardDTO;
	}

	/**
	 * CalculateQuestionState
	 * 
	 * This method will calculate the question state for use in gameboards based
	 * on the question.
	 * 
	 * @param item
	 *            - the gameboard item / question tile.
	 * @param user
	 *            - the user that may or may not have attempted questions in the
	 *            gameboard.
	 * @return The state of the gameboard item.
	 */
	private GameboardItemState calculateQuestionState(final GameboardItem item,
			final User user) {
		String questionPageId = item.getId();

		if (user.getQuestionAttempts() != null
				&& user.getQuestionAttempts().containsKey(questionPageId)) {
			// go through each question in the question page
			ResultsWrapper<ContentDTO> listOfQuestions = api.searchByIdPrefix(
					api.getLiveVersion(), questionPageId
							+ ID_SEPARATOR);

			for (ContentDTO contentDTO : listOfQuestions.getResults()) {
				if (!(contentDTO instanceof QuestionDTO)) {
					continue;
				}

				if (user.getQuestionAttempts().containsKey(questionPageId)) {
					QuestionAttempt attempt = user.getQuestionAttempts()
							.get(questionPageId).get(contentDTO.getId());
					if (null == attempt) {
						return GameboardItemState.IN_PROGRESS;
					}
					if (!attempt.isSuccess()) {
						return GameboardItemState.TRY_AGAIN;
					}
				}
			}
			return GameboardItemState.COMPLETED;
		}
		// default to not attempted
		return GameboardItemState.NOT_ATTEMPTED;
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
