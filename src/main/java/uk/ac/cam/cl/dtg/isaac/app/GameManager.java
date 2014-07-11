package uk.ac.cam.cl.dtg.isaac.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.dozer.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dao.GameboardPersistenceManager;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.dto.Wildcard;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.users.User;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.app.Constants.*;

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

	/**
	 * Creates a game manager that operates using the provided api.
	 * 
	 * @param api
	 *            - the api that the game manager can use.
	 */
	public GameManager(final SegueApiFacade api) {
		this.api = api;
		this.gameboardPersistenceManager = new GameboardPersistenceManager(
				api.requestAppDataManager(GAMEBOARD_COLLECTION_NAME,
						uk.ac.cam.cl.dtg.isaac.dos.GameboardDO.class), api);
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
	 *         by the parameters. Will return null if no questions can be provided.
	 */
	public final GameboardDTO generateRandomGameboard(
			final List<String> subjectsList, final List<String> fieldsList,
			final List<String> topicsList, final List<Integer> levelsList,
			final List<String> conceptsList, final User boardOwner) {
		String boardOwnerId = null;
		if (boardOwner != null) {
			boardOwnerId = boardOwner.getDbId();
		}

		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap 
			= new HashMap<Map.Entry<Constants.BooleanOperator, String>, List<String>>();

		fieldsToMap.put(com.google.common.collect.Maps.immutableEntry(
				Constants.BooleanOperator.AND, TYPE_FIELDNAME), Arrays
				.asList(QUESTION_TYPE));

		GameFilter gameFilter = new GameFilter(subjectsList, fieldsList,
				topicsList, levelsList, conceptsList);

		fieldsToMap.putAll(generateFieldToMatchForQuestionFilter(gameFilter));

		// Search for questions that match the fields to map variable.
		ResultsWrapper<Content> results = api.findMatchingContentRandomOrder(
				api.getLiveVersion(), fieldsToMap, 0, MAX_QUESTIONS_TO_SEARCH);

		if (!results.getResults().isEmpty()) {
			String uuid = UUID.randomUUID().toString();

			Integer sizeOfGameboard = GAME_BOARD_SIZE;
			// TODO: if there are not enough questions then really we should
			// fill it with random questions or just say no
			if (GAME_BOARD_SIZE > results.getResults().size()) {
				sizeOfGameboard = results.getResults().size();
			}

			List<Content> questionsForGameboard = results.getResults().subList(
					0, sizeOfGameboard);

			// build gameboard
			Injector injector = Guice.createInjector(
					new IsaacGuiceConfigurationModule(),
					new SegueGuiceConfigurationModule());

			Mapper mapper = injector.getInstance(Mapper.class);
			List<GameboardItem> gameboardReadyQuestions = new ArrayList<GameboardItem>();

			// Map each Content object into an IsaacQuestionInfo object
			for (Content c : questionsForGameboard) {
				GameboardItem questionInfo = mapper.map(c, GameboardItem.class);
				questionInfo.setUri(IsaacController.generateApiUrl(c));
				gameboardReadyQuestions.add(questionInfo);
			}

			log.debug("Created gameboard " + uuid);
			GameboardDTO gameboard = new GameboardDTO(uuid,
					gameboardReadyQuestions, new Date(), gameFilter,
					boardOwnerId);
			this.gameboardPersistenceManager
					.temporarilyStoreGameboard(gameboard);

			return gameboard;
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
	 * @return the gameboard or null.
	 */
	public final GameboardDTO getGameboard(final String gameboardId) {
		return this.gameboardPersistenceManager.getGameboardById(gameboardId);
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
	private static Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> 
	generateFieldToMatchForQuestionFilter(
			final GameFilter gameFilter) {

		// Validate that the field sizes are as we expect for tags
		// Check that the query provided adheres to the rules we expect
		if (!validateFilterQuery(gameFilter)) {
			throw new IllegalArgumentException("Error validating filter query.");
		}

		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatchOutput = Maps
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
			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.AND,
							Constants.TAGS_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, ands);
		}
		if (ors.size() > 0) {
			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.OR,
							Constants.TAGS_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, ors);
		}

		// now deal with levels
		if (null != gameFilter.getLevels()) {
			List<String> levelsAsString = Lists.newArrayList();
			for (Integer levelInt : gameFilter.getLevels()) {
				levelsAsString.add(levelInt.toString());
			}

			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.OR,
							Constants.LEVEL_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, levelsAsString);
		}

		if (null != gameFilter.getConcepts()) {
			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.AND,
							RELATED_CONTENT_FIELDNAME);
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
