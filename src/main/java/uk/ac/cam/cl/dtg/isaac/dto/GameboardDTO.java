package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.Wildcard;

/**
 * DTO representation of a gameboard.
 * 
 */
public class GameboardDTO {
	private String id;
	private String title;
	private List<GameboardItem> questions;
	private Wildcard wildCard;
	private Integer wildCardPosition;
	private Date creationDate;
	private GameFilter gameFilter;
	private String ownerUserId;

	/**
	 * Default Gameboard Constructor.
	 */
	public GameboardDTO() {
		this.questions = new ArrayList<GameboardItem>();
	}

	/**
	 * Complete gameboard constructor with all dependencies.
	 * 
	 * @param id
	 *            - unique id for the gameboard
	 * @param title
	 *            - optional title for gameboard.
	 * @param questions
	 *            - list of gameboard items (shallow questions).
	 * @param wildCard
	 *            - wildcard content object for advertising purposes.
	 * @param wildcardPosition
	 *            - position for where the front end should display this.
	 * @param creationDate
	 *            - Date in which the gameboard was created.
	 * @param gameFilter
	 *            - simple DO that represents the filter criteria used to creat
	 *            the gameboard.
	 * @param ownerUserId
	 *            - User id of the owner of the gameboard.
	 * @throws IllegalArgumentException
	 */
	public GameboardDTO(final String id, final String title,
			final List<GameboardItem> questions, final Wildcard wildCard,
			final Integer wildcardPosition, final Date creationDate,
			final GameFilter gameFilter, final String ownerUserId) {
		this.id = id;
		this.title = title;
		this.questions = questions;
		this.wildCard = wildCard;
		this.wildCardPosition = wildcardPosition;
		this.creationDate = creationDate;
		this.gameFilter = gameFilter;
		this.ownerUserId = ownerUserId;
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
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * 
	 * @param title
	 *            the title to set
	 */
	public final void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Gets the gameboardItems.
	 * 
	 * @return the gameboardItems
	 */
	public final List<GameboardItem> getQuestions() {
		return questions;
	}

	/**
	 * Sets the gameboardItems.
	 * 
	 * @param questions
	 *            the gameboardItems to set
	 */
	public final void setQuestions(final List<GameboardItem> questions) {
		this.questions = questions;
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
	 * Gets the wildCardPosition.
	 * @return the wildCardPosition
	 */
	public final Integer getWildCardPosition() {
		return wildCardPosition;
	}
	
	/**
	 * Sets the wildCardPosition.
	 * @param wildCardPosition the wildCardPosition to set
	 */
	public final void setWildCardPosition(final Integer wildCardPosition) {
		this.wildCardPosition = wildCardPosition;
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
	 * Gets the userId.
	 * 
	 * @return the userId
	 */
	public final String getOwnerUserId() {
		return ownerUserId;
	}

	/**
	 * Sets the ownerUserId.
	 * 
	 * @param ownerUserId
	 *            the ownerUserId to set
	 */
	public final void setOwnerUserId(final String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}
}
