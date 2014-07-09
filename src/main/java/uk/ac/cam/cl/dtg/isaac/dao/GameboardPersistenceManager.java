package uk.ac.cam.cl.dtg.isaac.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.isaac.dto.Gameboard;
import uk.ac.cam.cl.dtg.segue.dao.IAppDataManager;

/**
 * This class is responsible for managing and persisting user data.
 * @author Stephen Cummins
 */
public class GameboardPersistenceManager {

	private static final Logger log = LoggerFactory
			.getLogger(GameboardPersistenceManager.class);
	
	//private static final String USER_COLLECTION_NAME = "users";

	private final IAppDataManager<Gameboard> gameboardDataManager;

	/**
	 * Creates a new user data maanger object.
	 * @param database - the database reference used for persistence.
	 */
	@Inject
	public GameboardPersistenceManager(final IAppDataManager<Gameboard> database) {
		this.gameboardDataManager = database;
	}

	/**
	 * Save a gameboard.
	 * @param gameboard - gameboard to save
	 * @return internal database id for the saved gameboard.
	 */
	public final String saveGameboard(final Gameboard gameboard) {
		
		String resultId = gameboardDataManager.save(gameboard);
		
		log.info("Saving gameboard... Gameboard ID: " 
				+ gameboard.getId() 
				+ " DB id : " 
				+ resultId);
		
		return resultId;
	}
	
	/**
	 * Find a gameboard by id.
	 * @param gameboardId - the id to search for.
	 * @return the gameboard or null if we can't find it..
	 */
	public final Gameboard getGameboardById(final String gameboardId) {
		return gameboardDataManager.getById(gameboardId);
	}

}
