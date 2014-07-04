package uk.ac.cam.cl.dtg.isaac.models.content;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.app.Constants;

/**
 * DTO representation of a gameboard.
 *
 */
public class Gameboard {
	private static final Logger log = LoggerFactory.getLogger(Gameboard.class);
	private String id;
	private List<GameboardItem> gameboardItems;
	private Wildcard wildCard;
	private Date creationDate;
	private GameFilter gameFilter;

	/**
	 * Default Gameboard Constructor.
	 */
	public Gameboard() {
		this.gameboardItems = new ArrayList<GameboardItem>();
	}

	/**
	 * Complete gameboard constructor with all dependencies.
	 * 
	 * @param id
	 *            - unique id for the gameboard
	 * @param questions
	 *            - list of gameboard items (shallow questions).
	 * @param creationDate
	 *            - Date in which the gameboard was created.
	 * @param gameFilter
	 *            - simple DO that represents the filter criteria used to creat
	 *            the gameboard.
	 * @throws IllegalArgumentException
	 */
	public Gameboard(final String id, final List<GameboardItem> questions,
			final Date creationDate, final GameFilter gameFilter) {
		this.id = id;
		this.gameboardItems = questions;
		this.creationDate = creationDate;
		this.gameFilter = gameFilter;

		if (questions.size() > Constants.GAME_BOARD_SIZE) {
			throw new IllegalArgumentException(
					"Too many questions added to gameboard");
		} else if (questions.size() < Constants.GAME_BOARD_SIZE) {
			log.warn("Gameboard created without enough questions.");
		}
	}

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public final String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the id to set
	 */
	public final void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the gameboardItems.
	 * 
	 * @return the gameboardItems
	 */
	public final List<GameboardItem> getGameboardItems() {
		return gameboardItems;
	}

	/**
	 * Sets the gameboardItems.
	 * 
	 * @param gameboardItems
	 *            the gameboardItems to set
	 */
	public final void setGameboardItems(final List<GameboardItem> gameboardItems) {
		this.gameboardItems = gameboardItems;
	}

	/**
	 * Gets the wildCard.
	 * 
	 * @return the wildCard
	 */
	public final Wildcard getWildCard() {
		return wildCard;
	}

	/**
	 * Sets the wildCard.
	 * 
	 * @param wildCard
	 *            the wildCard to set
	 */
	public final void setWildCard(final Wildcard wildCard) {
		this.wildCard = wildCard;
	}

	/**
	 * Gets the creationDate.
	 * 
	 * @return the creationDate
	 */
	public final Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Sets the creationDate.
	 * 
	 * @param creationDate
	 *            the creationDate to set
	 */
	public final void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * Gets the gameFilter.
	 * 
	 * @return the gameFilter
	 */
	public final GameFilter getGameFilter() {
		return gameFilter;
	}

	/**
	 * Sets the gameFilter.
	 * 
	 * @param gameFilter
	 *            the gameFilter to set
	 */
	public final void setGameFilter(final GameFilter gameFilter) {
		this.gameFilter = gameFilter;
	}

	/**
	 * Gets the log.
	 * 
	 * @return the log
	 */
	public static final Logger getLog() {
		return log;
	}

}
