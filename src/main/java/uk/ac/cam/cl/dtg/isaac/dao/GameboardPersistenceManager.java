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
package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ma.glasnost.orika.MapperFacade;

import org.apache.commons.lang3.Validate;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dos.GameboardDO;
import uk.ac.cam.cl.dtg.isaac.dos.UserGameboardsDO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardDTO;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.Constants.BooleanOperator;
import uk.ac.cam.cl.dtg.segue.api.managers.URIManager;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.dao.IAppDatabaseManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.segue.dto.users.RegisteredUserDTO;
import static java.util.concurrent.TimeUnit.*;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static com.google.common.collect.Maps.*;

/**
 * This class is responsible for managing and persisting user data.
 */
public class GameboardPersistenceManager {
	private static final Logger log = LoggerFactory.getLogger(GameboardPersistenceManager.class);

	private static final Integer CACHE_TARGET_SIZE = 142;
	private static final Long GAMEBOARD_TTL_HOURS = MILLISECONDS.convert(30, MINUTES);

	private static final String USER_ID_FKEY = "userId";
	private static final String DB_ID_FIELD = "_id";

	private final IAppDatabaseManager<GameboardDO> gameboardDataManager;
	private final IAppDatabaseManager<UserGameboardsDO> userToGameboardMappingsDatabase;

	private final MapperFacade mapper;
	private final SegueApiFacade api;

	private final Map<String, GameboardDO> gameboardNonPersistentStorage;

	/**
	 * Creates a new user data manager object.
	 * 
	 * @param database
	 *            - the database reference used for persistence.
	 * @param userToGameboardMappings
	 *            - the database reference used for persistence of user to
	 *            gameboard relationships.
	 * @param api
	 *            - handle to segue api so that we can perform queries to
	 *            augment gameboard data before and after persistence.
	 * @param mapper
	 *            - An instance of an automapper that can be used for mapping to
	 *            and from GameboardDOs and DTOs.
	 */
	@Inject
	public GameboardPersistenceManager(final IAppDatabaseManager<GameboardDO> database,
			final IAppDatabaseManager<UserGameboardsDO> userToGameboardMappings, final SegueApiFacade api,
			final MapperFacade mapper) {
		this.gameboardDataManager = database;
		this.userToGameboardMappingsDatabase = userToGameboardMappings;
		this.mapper = mapper;
		this.api = api;
		this.gameboardNonPersistentStorage = Maps.newConcurrentMap();
	}

	/**
	 * Save a gameboard.
	 * 
	 * @param gameboard
	 *            - gameboard to save
	 * @return internal database id for the saved gameboard.
	 * @throws SegueDatabaseException
	 *             - if there is a problem saving the gameboard in the database.
	 */
	public final String saveGameboardToPermanentStorage(final GameboardDTO gameboard)
		throws SegueDatabaseException {
		GameboardDO gameboardToSave = mapper.map(gameboard, GameboardDO.class);
		// the mapping operation won't work for the list so we should just
		// create a new one.
		gameboardToSave.setQuestions(new ArrayList<String>());

		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboard.getQuestions()) {
			gameboardToSave.getQuestions().add(c.getId());
		}

		// This operation may not be atomic due to underlying DB. Gameboard create first then link to user view second.
		String resultId = gameboardDataManager.save(gameboardToSave);
		log.debug("Saving gameboard... Gameboard ID: " + gameboard.getId() + " DB id : " + resultId);

		// add the gameboard to the users myboards list.
		this.createOrUpdateUserLinkToGameboard(gameboardToSave.getOwnerUserId(), resultId);
		log.debug("Saving gameboard to user relationship...");

		// make sure that it is not still in temporary storage
		this.gameboardNonPersistentStorage.remove(gameboard.getId());

		return resultId;
	}

	/**
	 * Link a user to a gameboard or update an existing link.
	 * 
	 * @param userId
	 *            - userId to link
	 * @param gameboardId
	 *            - gameboard to link
	 * @throws SegueDatabaseException
	 *             - if there is a problem persisting the link in the database.
	 */
	public void createOrUpdateUserLinkToGameboard(final String userId, final String gameboardId)
		throws SegueDatabaseException {
		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

		Map.Entry<BooleanOperator, String> userIdFieldParam = immutableEntry(BooleanOperator.AND, "userId");
		Map.Entry<BooleanOperator, String> gameboardIdFieldParam = immutableEntry(BooleanOperator.AND,
				"gameboardId");
		fieldsToMatch.put(userIdFieldParam, Arrays.asList(userId));
		fieldsToMatch.put(gameboardIdFieldParam, Arrays.asList(gameboardId));

		List<UserGameboardsDO> userGameboardDOs = this.userToGameboardMappingsDatabase.find(fieldsToMatch);

		if (userGameboardDOs.size() == 0) {
			// if this user is not already connected make a connection.
			UserGameboardsDO userGameboardConnection = new UserGameboardsDO(null, userId, gameboardId,
					new Date(), new Date());

			this.userToGameboardMappingsDatabase.save(userGameboardConnection);
		} else if (userGameboardDOs.size() == 1) {
			// if the user is already connected to the game board update their
			// link.
			UserGameboardsDO userGameboardConnection = userGameboardDOs.get(0);
			userGameboardConnection.setLastVisited(new Date());
			this.userToGameboardMappingsDatabase.save(userGameboardConnection);
		} else {
			log.error("Expected one result and found multiple gameboard -  user relationships.");
		}
	}
	
	/**
	 * Allows a link between users and a gameboard to be destroyed.
	 * 
	 * @param userId - users id.
	 * @param gameboardId - gameboard's id
	 * @throws SegueDatabaseException - if there is an error during the delete operation.
	 */
	public void removeUserLinkToGameboard(final String userId, final String gameboardId) throws SegueDatabaseException {
		
		// verify that the link exists and retrieve the link id.
		Map<Map.Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

		Map.Entry<BooleanOperator, String> userIdFieldParam = immutableEntry(BooleanOperator.AND, "userId");
		Map.Entry<BooleanOperator, String> gameboardIdFieldParam = immutableEntry(BooleanOperator.AND,
				"gameboardId");
		fieldsToMatch.put(userIdFieldParam, Arrays.asList(userId));
		fieldsToMatch.put(gameboardIdFieldParam, Arrays.asList(gameboardId));

		List<UserGameboardsDO> userGameboardDOs = this.userToGameboardMappingsDatabase.find(fieldsToMatch);
		
		if (userGameboardDOs.size() == 1) {
			// delete it
			this.userToGameboardMappingsDatabase.delete(userGameboardDOs.get(0).getId());
		} else if (userGameboardDOs.size() == 0) {
			// unable to find it.
			log.info("Attempted to remove user to gameboard link but there was none to remove.");
		} else {
			// too many gameboards found.
			throw new SegueDatabaseException(
					"Unable to delete the gameboard as there is more than one "
					+ "linking that matches the search terms. Found: "
							+ userGameboardDOs.size());
		}
	}

	/**
	 * Keep generated gameboard in non-persistent storage.
	 * 
	 * This will be removed if the gameboard is saved to persistent storage.
	 * 
	 * @param gameboard
	 *            to temporarily store.
	 * @return gameboard id
	 */
	public final String temporarilyStoreGameboard(final GameboardDTO gameboard) {
		this.gameboardNonPersistentStorage.put(gameboard.getId(), this.convertToGameboardDO(gameboard));

		tidyTemporaryGameboardStorage();

		return gameboard.getId();
	}
	
	/**
	 * Determine if the gameboard only exists in temporary storage.
	 * 
	 * @param gameboardToTest - the gameboard to check the existence of.
	 * @return true if the gameboard with that id exists in permanent storage false if not.
	 * @throws SegueDatabaseException if there is a database error.
	 */
	public final boolean isPermanentlyStored(final GameboardDTO gameboardToTest) throws SegueDatabaseException {
		return this.gameboardDataManager.getById(gameboardToTest.getId()) != null;
	}

	/**
	 * Find a gameboard by id.
	 * 
	 * @param gameboardId
	 *            - the id to search for.
	 * @return the gameboard or null if we can't find it..
	 * @throws SegueDatabaseException  - if there is a problem accessing the database.
	 */
	public final GameboardDTO getGameboardById(final String gameboardId) throws SegueDatabaseException {
		// first try temporary storage
		if (this.gameboardNonPersistentStorage.containsKey(gameboardId)) {
			return this.convertToGameboardDTO(this.gameboardNonPersistentStorage.get(gameboardId));
		}

		GameboardDO gameboardFromDb = gameboardDataManager.getById(gameboardId);

		if (null == gameboardFromDb) {
			return null;
		}

		GameboardDTO gameboardDTO = this.convertToGameboardDTO(gameboardFromDb);

		return gameboardDTO;
	}

	/**
	 * Retrieve all gameboards (without underlying Gameboard Items) for a given
	 * user.
	 * 
	 * @param user
	 *            - to search for
	 * @return gameboards as a list - note these gameboards will not have the
	 *         questions fully populated as it is expected only summary objects
	 *         are required.
	 * @throws SegueDatabaseException
	 *             - if there is an error when accessing the database.
	 */
	public final List<GameboardDTO> getGameboardsByUserId(final RegisteredUserDTO user) throws SegueDatabaseException {
		// find all gameboards related to this user.
		Map<String, UserGameboardsDO> gameboardLinksToUser = this.findLinkedGameboardIdsForUser(user
				.getDbId());

		List<String> gameboardIdsLinkedToUser = Lists.newArrayList();
		gameboardIdsLinkedToUser.addAll(gameboardLinksToUser.keySet());

		if (null == gameboardIdsLinkedToUser || gameboardIdsLinkedToUser.isEmpty()) {
			return Lists.newArrayList();
		}

		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatch = Maps.newHashMap();

		fieldsToMatch
				.put(immutableEntry(Constants.BooleanOperator.OR, DB_ID_FIELD), gameboardIdsLinkedToUser);

		List<GameboardDO> gameboardsFromDb = this.gameboardDataManager.find(fieldsToMatch);

		List<GameboardDTO> gameboardDTOs = this.convertToGameboardDTOs(gameboardsFromDb, false);

		// we need to augment each gameboard with its visited date.
		for (GameboardDTO gameboardDTO : gameboardDTOs) {
			gameboardDTO.setLastVisited(gameboardLinksToUser.get(gameboardDTO.getId()).getLastVisited());
		}

		return gameboardDTOs;
	}

	/**
	 * Allows a gameboard title to be updated.
	 * 
	 * @param gameboard
	 *            - with updated title.
	 * @return new gameboard after update.
	 * @throws SegueDatabaseException - if there is a problem setting the title.
	 */
	public GameboardDTO updateGameboardTitle(final GameboardDTO gameboard) throws SegueDatabaseException {
		return this.convertToGameboardDTO(this.gameboardDataManager.updateField(gameboard.getId(),
				Constants.TITLE_FIELDNAME, gameboard.getTitle()));
	}
	
	/**
	 * Helper method to tidy temporary gameboard cache.
	 */
	private void tidyTemporaryGameboardStorage() {
		if (this.gameboardNonPersistentStorage.size() >= CACHE_TARGET_SIZE) {
			log.debug("Running gameboard temporary cache eviction as it is of size  "
					+ this.gameboardNonPersistentStorage.size());

			for (GameboardDO board : this.gameboardNonPersistentStorage.values()) {
				long duration = new Date().getTime() - board.getCreationDate().getTime();

				if (duration >= GAMEBOARD_TTL_HOURS) {
					this.gameboardNonPersistentStorage.remove(board.getId());
					log.debug("Deleting temporary board from cache " + board.getId());
				}
			}
		}
	}

	/**
	 * Convert form a list of gameboard DOs to a list of Gameboard DTOs.
	 * 
	 * @param gameboardDOs
	 *            to convert
	 * @param populateGameboardItems
	 *            - true if we should fully populate the gameboard DTO with
	 *            gameboard items false if a summary is ok do?
	 * @return gameboard DTO
	 */
	private List<GameboardDTO> convertToGameboardDTOs(final List<GameboardDO> gameboardDOs,
			final boolean populateGameboardItems) {
		Validate.notNull(gameboardDOs);

		List<GameboardDTO> gameboardDTOs = Lists.newArrayList();

		for (GameboardDO gameboardDO : gameboardDOs) {
			gameboardDTOs.add(this.convertToGameboardDTO(gameboardDO, populateGameboardItems));
		}

		return gameboardDTOs;
	}

	/**
	 * Convert form a gameboard DO to a Gameboard DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param gameboardDO
	 *            - to convert
	 * @return gameboard DTO
	 */
	private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO) {
		return this.convertToGameboardDTO(gameboardDO, true);
	}

	/**
	 * Convert form a gameboard DO to a Gameboard DTO.
	 * 
	 * This method relies on the api to fully resolve questions.
	 * 
	 * @param gameboardDO
	 *            - to convert
	 * @param populateGameboardItems
	 *            - true if we should fully populate the gameboard DTO with
	 *            gameboard items false if just the question ids will do?
	 * @return gameboard DTO
	 */
	private GameboardDTO convertToGameboardDTO(final GameboardDO gameboardDO,
			final boolean populateGameboardItems) {
		GameboardDTO gameboardDTO = mapper.map(gameboardDO, GameboardDTO.class);

		if (!populateGameboardItems) {
			List<GameboardItem> listOfSparseGameItems = Lists.newArrayList();

			for (String questionPageId : gameboardDO.getQuestions()) {
				GameboardItem gameboardItem = new GameboardItem();
				gameboardItem.setId(questionPageId);
				listOfSparseGameItems.add(gameboardItem);
			}
			gameboardDTO.setQuestions(listOfSparseGameItems);
			return gameboardDTO;
		}

		// build query the db to get full question information
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap = Maps.newHashMap();

		fieldsToMap.put(
				immutableEntry(Constants.BooleanOperator.OR, Constants.ID_FIELDNAME + '.'
						+ Constants.UNPROCESSED_SEARCH_FIELD_SUFFIX), gameboardDO.getQuestions());

		fieldsToMap.put(immutableEntry(Constants.BooleanOperator.OR, Constants.TYPE_FIELDNAME),
				Arrays.asList(QUESTION_TYPE));

		// Search for questions that match the ids.
		ResultsWrapper<ContentDTO> results = api.findMatchingContent(api.getLiveVersion(), fieldsToMap, 0,
				gameboardDO.getQuestions().size());

		List<ContentDTO> questionsForGameboard = results.getResults();

		// Map each Content object into an GameboardItem object
		Map<String, GameboardItem> gameboardReadyQuestions = new HashMap<String, GameboardItem>();

		for (ContentDTO c : questionsForGameboard) {
			GameboardItem questionInfo = mapper.map(c, GameboardItem.class);
			questionInfo.setUri(URIManager.generateApiUrl(c));
			gameboardReadyQuestions.put(c.getId(), questionInfo);
		}

		// empty and repopulate the gameboard dto.
		gameboardDTO.setQuestions(new ArrayList<GameboardItem>());
		for (String questionid : gameboardDO.getQuestions()) {
			gameboardDTO.getQuestions().add(gameboardReadyQuestions.get(questionid));
		}
		return gameboardDTO;
	}

	/**
	 * Convert from a gameboard DTO to a gameboard DO.
	 * 
	 * @param gameboardDTO
	 *            - DTO to convert.
	 * @return GameboardDO.
	 */
	private GameboardDO convertToGameboardDO(final GameboardDTO gameboardDTO) {
		GameboardDO gameboardDO = mapper.map(gameboardDTO, GameboardDO.class);
		// the mapping operation won't work for the list so we should just
		// create a new one.
		gameboardDO.setQuestions(new ArrayList<String>());

		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboardDTO.getQuestions()) {
			gameboardDO.getQuestions().add(c.getId());
		}

		return gameboardDO;
	}

	/**
	 * Find all gameboardIds that are connected to a given user.
	 * 
	 * @param userId
	 *            to search against.
	 * @return A Map of ids to UserGameboardsDO.
	 * @throws SegueDatabaseException - if there is a problem accessing the database.
	 */
	private Map<String, UserGameboardsDO> findLinkedGameboardIdsForUser(final String userId)
		throws SegueDatabaseException {
		// find all gameboards related to this user.
		Map<Entry<BooleanOperator, String>, List<String>> fieldsToMatchForGameboardSearch = Maps.newHashMap();

		fieldsToMatchForGameboardSearch.put(immutableEntry(Constants.BooleanOperator.AND, USER_ID_FKEY),
				Arrays.asList(userId));

		List<UserGameboardsDO> userGameboardsDO = this.userToGameboardMappingsDatabase
				.find(fieldsToMatchForGameboardSearch);

		Map<String, UserGameboardsDO> resultToReturn = Maps.newHashMap();
		for (UserGameboardsDO objectToConvert : userGameboardsDO) {
			resultToReturn.put(objectToConvert.getGameboardId(), objectToConvert);
		}

		return resultToReturn;
	}
}
