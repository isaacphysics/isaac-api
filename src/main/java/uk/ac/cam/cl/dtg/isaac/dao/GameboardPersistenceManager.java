package uk.ac.cam.cl.dtg.isaac.dao;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dozer.Mapper;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.isaac.app.IsaacController;
import uk.ac.cam.cl.dtg.isaac.configuration.IsaacGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.isaac.dto.Gameboard;
import uk.ac.cam.cl.dtg.isaac.dto.GameboardItem;
import uk.ac.cam.cl.dtg.segue.api.Constants;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dao.IAppDataManager;
import uk.ac.cam.cl.dtg.segue.dto.ResultsWrapper;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import static java.util.concurrent.TimeUnit.*;

/**
 * This class is responsible for managing and persisting user data.
 */
public class GameboardPersistenceManager {
	private static final Logger log = LoggerFactory
			.getLogger(GameboardPersistenceManager.class);
	private static final Integer CACHE_TARGET_SIZE = 142;
	
	private static final Long GAMEBOARD_TTL_HOURS = MILLISECONDS.convert(30, MINUTES);

	private final IAppDataManager<uk.ac.cam.cl.dtg.isaac.dos.Gameboard> gameboardDataManager;

	private final Mapper mapper;
	private final SegueApiFacade api;
	
	private final Map<String, Gameboard> gameboardNonPersistentStorage;
	
	/**
	 * Creates a new user data manager object.
	 * @param database - the database reference used for persistence.
	 * @param api - handle to segue api so that we can perform queries to augment gameboard data
	 * before and after persistence.
	 */
	@Inject
	public GameboardPersistenceManager(
			final IAppDataManager<uk.ac.cam.cl.dtg.isaac.dos.Gameboard> database,
			final SegueApiFacade api) {
		this.gameboardDataManager = database;

		Injector injector = Guice.createInjector(
				new IsaacGuiceConfigurationModule(),
				new SegueGuiceConfigurationModule());
		
		this.mapper = injector.getInstance(Mapper.class);
		this.api = api;
		this.gameboardNonPersistentStorage = Maps.newConcurrentMap();
	}

	/**
	 * Save a gameboard.
	 * @param gameboard - gameboard to save
	 * @return internal database id for the saved gameboard.
	 */
	public final String saveGameboardToPermanentStorage(final Gameboard gameboard) {
		uk.ac.cam.cl.dtg.isaac.dos.Gameboard gameboardToSave = 
				mapper.map(gameboard, uk.ac.cam.cl.dtg.isaac.dos.Gameboard.class);
		// the mapping operation won't work for the list so we should just create a new one.
		gameboardToSave.setQuestions(new ArrayList<String>());
		
		// Map each question into an IsaacQuestionInfo object
		for (GameboardItem c : gameboard.getQuestions()) {
			gameboardToSave.getQuestions().add(c.getId());
		}

		String resultId = gameboardDataManager.save(gameboardToSave);
		
		log.info("Saving gameboard... Gameboard ID: " 
				+ gameboard.getId() 
				+ " DB id : " 
				+ resultId);
		
		// make sure that it is not still in temporary storage
		this.gameboardNonPersistentStorage.remove(gameboard.getId());
		
		return resultId;
	}
	
	/**
	 * Keep generated gameboard in non-persistent storage.
	 * 
	 * This will be removed if the gameboard is saved to persistent storage.
	 * 
	 * @param gameboard to temporarily store.
	 * @return gameboard id
	 */
	public final String temporarilyStoreGameboard(final Gameboard gameboard) {
		this.gameboardNonPersistentStorage.put(gameboard.getId(), gameboard);
		
		tidyTemporaryGameboardStorage();
		
		return gameboard.getId();
	}
	
	/**
	 * Find a gameboard by id.
	 * @param gameboardId - the id to search for.
	 * @return the gameboard or null if we can't find it..
	 */
	public final Gameboard getGameboardById(final String gameboardId) {
		// first try temporary storage
		if (this.gameboardNonPersistentStorage.containsKey(gameboardId)) {
			return this.gameboardNonPersistentStorage.get(gameboardId);
		}
		
		uk.ac.cam.cl.dtg.isaac.dos.Gameboard gameboardFromDb 
			= gameboardDataManager.getById(gameboardId);
		
		Gameboard gameboardDTO = mapper.map(gameboardFromDb, Gameboard.class);
		
		// build query the db to get full question information
		Map<Map.Entry<Constants.BooleanOperator, String>, List<String>> fieldsToMap 
			= new HashMap<Map.Entry<Constants.BooleanOperator, String>, List<String>>();

		fieldsToMap.put(com.google.common.collect.Maps.immutableEntry(
				Constants.BooleanOperator.OR, Constants.ID_FIELDNAME), 
				gameboardFromDb.getQuestions());
	
		// Search for questions that match the ids. 
		ResultsWrapper<Content> results = api.findMatchingContent(
				api.getLiveVersion(), fieldsToMap, 0, gameboardFromDb.getQuestions().size());
		
		List<Content> questionsForGameboard = results.getResults();
		
		// Map each Content object into an GameboardItem object
		Map<String, GameboardItem> gameboardReadyQuestions 
			= new HashMap<String, GameboardItem>();		

		for (Content c : questionsForGameboard) {
			GameboardItem questionInfo = mapper.map(c,
					GameboardItem.class);
			questionInfo.setUri(IsaacController.generateApiUrl(c));
			gameboardReadyQuestions.put(c.getId(), questionInfo);
		}
		
		// empty and repopulate the gameboard dto.
		gameboardDTO.setQuestions(new ArrayList<GameboardItem>());
		for (String questionid : gameboardFromDb.getQuestions()) {
			gameboardDTO.getQuestions().add(gameboardReadyQuestions.get(questionid));
		}
		
		return gameboardDTO;
	}
	
	/**
	 * Helper method to tidy temporary gameboard cache.
	 */
	private void tidyTemporaryGameboardStorage() {
		if (this.gameboardNonPersistentStorage.size() >= CACHE_TARGET_SIZE) {
			log.debug("Running gameboard temporary cache eviction as it is of size  " 
					+ this.gameboardNonPersistentStorage.size());
			
			for (Gameboard board : this.gameboardNonPersistentStorage.values()) {
				long duration = new Date().getTime() - board.getCreationDate().getTime();
				
				if (duration >= GAMEBOARD_TTL_HOURS) {
					this.gameboardNonPersistentStorage.remove(board.getId());
					log.debug("Deleting temporary board from cache " + board.getId());
				}
			}
		}
	}
}
