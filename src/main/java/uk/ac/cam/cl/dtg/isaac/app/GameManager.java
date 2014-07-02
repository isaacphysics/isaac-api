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
import uk.ac.cam.cl.dtg.isaac.models.content.Gameboard;
import uk.ac.cam.cl.dtg.isaac.models.content.GameboardItem;
import uk.ac.cam.cl.dtg.isaac.models.content.IsaacQuestionInfo;
import uk.ac.cam.cl.dtg.isaac.models.content.Wildcard;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.app.Constants.*;

/**
 * This class will be responsible for generating and managing gameboards used by
 * users.
 * 
 */
public class GameManager {
	private static final Logger log = LoggerFactory.getLogger(GameManager.class);
	private final SegueApiFacade api;

	/**
	 * Creates a game manager that operates using the provided api.
	 * 
	 * @param api
	 *            - the api that the game manager can use.
	 */
	public GameManager(final SegueApiFacade api) {
		this.api = api;
	}

	/**
	 * Generate a random gameboard without any filter conditions specified.
	 * 
	 * @see generateRandomGambeoard
	 * @return gameboard containing random problems.
	 */
	public final Gameboard generateRandomGameboard() {
		return this.generateRandomGameboard(null, null, null, null, null);
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
	 * @return a gameboard if possible that satisifies the conditions provided
	 *         by the parameters.
	 */
	public final Gameboard generateRandomGameboard(
			final List<String> subjectsList, final List<String> fieldsList,
			final List<String> topicsList, final List<String> levelsList,
			final List<String> conceptsList) {
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap 
			= new HashMap<Map.Entry<Constants.BooleanOperator, String>, List<String>>();
		
		fieldsToMap.put(com.google.common.collect.Maps.immutableEntry(
				Constants.BooleanOperator.AND, TYPE_FIELDNAME), Arrays
				.asList(QUESTION_TYPE));

		fieldsToMap.putAll(generateFieldToMatchForQuestionFilter(subjectsList,
				fieldsList, topicsList, levelsList, conceptsList));

		// Search for questions that match the fields to map variable. //TODO:
		// fix magic numbers
		ResultsWrapper<Content> results = api.findMatchingContentRandomOrder(
				api.getLiveVersion(), fieldsToMap, 0, 20);

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
				IsaacQuestionInfo questionInfo = mapper.map(c,
						IsaacQuestionInfo.class);
				questionInfo.setUri(IsaacController.generateApiUrl(c));
				gameboardReadyQuestions.add(questionInfo);
			}

			log.debug("Created gameboard " + uuid);
			return new Gameboard(uuid, gameboardReadyQuestions, new Date());
		} else {
			return new Gameboard();
		}
	}

	public boolean storeGameboard(final Gameboard gameboardToStore) {
		// TODO: stub
		return false;
	}

	public Wildcard getRandomWildcardTile() {
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
	 * @param subjects from filter
	 * @param fields from filter
	 * @param topics from filter
	 * @param levels from filter
	 * @param concepts from filter
	 * @return A map ready to be passed to a content provider
	 */
	public static Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> 
	generateFieldToMatchForQuestionFilter(
			final List<String> subjects, final List<String> fields, final List<String> topics,
			final List<String> levels, final List<String> concepts) {
		// Validate that the field sizes are as we expect for tags
		// Check that the query provided adheres to the rules we expect
		if (!validateFilterQuery(subjects, fields, topics, levels, concepts)) {
			throw new IllegalArgumentException("Error validating filter query.");
		}

		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMatchOutput = Maps
				.newHashMap();

		// Deal with tags which represent subjects, fields and topics
		List<String> ands = Lists.newArrayList();
		List<String> ors = Lists.newArrayList();

		if (null != subjects) {
			if (subjects.size() > 1) {
				ors.addAll(subjects);
			} else { // should be exactly 1
				ands.addAll(subjects);

				// ok now we are allowed to look at the fields
				if (null != fields) {
					if (fields.size() > 1) {
						ors.addAll(fields);
					} else { // should be exactly 1
						ands.addAll(fields);

						if (null != topics) {
							if (topics.size() > 1) {
								ors.addAll(topics);
							} else {
								ands.addAll(topics);
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
		if (null != levels) {
			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.OR,
							Constants.LEVEL_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, levels);
		}

		if (null != concepts) {
			Map.Entry<Constants.BooleanOperator, String> newEntry = com.google.common.collect.Maps
					.immutableEntry(Constants.BooleanOperator.AND,
							RELATED_CONTENT_FIELDNAME);
			fieldsToMatchOutput.put(newEntry, concepts);
		}

		return fieldsToMatchOutput;
	}

	/**
	 * Currently only validates subjects, fields and topics.
	 * 
	 * @param subjects
	 *            - multiple subjects are only ok if there are not any fields or
	 *            topics
	 * @param fields
	 *            - multiple fields are only ok if there are not any topics.
	 * @param topics
	 *            - You can have multiple fields only if there is precisely one
	 *            subject and field.
	 * @param levels
	 *            - currently not used for validation
	 * @param concepts
	 *            - currently not used for validation
	 * @return true if the query adheres to the rules specified, false if not.
	 */
	private static boolean validateFilterQuery(final List<String> subjects,
			final List<String> fields, final List<String> topics,
			final List<String> levels, final List<String> concepts) {
		if (null == subjects && null == fields && null == topics) {
			return true;
		} else if (null == subjects && (null != fields || null != topics)) {
			log.warn("Error validating query: You cannot have a "
					+ "null subject and still specify fields or topics.");
			return false;
		} else if (null != subjects && null == fields && null != topics) {
			log.warn("Error validating query: You cannot have a null field"
					+ " and still specify subject and topics.");
			return false;
		}

		// this variable indicates whether we have found a multiple term query
		// already.
		boolean foundMultipleTerms = false;

		// Now check that the subjects are of the correct size
		if (null != subjects) {
			if (subjects.size() > 1) {
				foundMultipleTerms = true;
			}
		}

		if (null != fields) {
			if (foundMultipleTerms) {
				log.warn("Error validating query: multiple subjects and fields specified.");
				return false;
			}

			if (fields.size() > 1) {
				foundMultipleTerms = true;
			}
		}

		if (null != topics) {
			if (foundMultipleTerms) {
				log.warn("Error validating query: multiple fields and topics specified.");
				return false;
			}

			if (topics.size() > 1) {
				foundMultipleTerms = true;
			}
		}
		return true;
	}
}
