package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.Date;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;
import uk.ac.cam.cl.dtg.isaac.dto.Wildcard;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is the Domain Object used to store gameboards in the segue CMS.
 * The primary difference between this DO and the Gameboard DTO is the references to 
 * gameboardItems are ids rather than full objects.
 */
public class Gameboard {
	@JsonProperty("_id")
	private String id;
	private List<String> questions;
	private Wildcard wildCard;
	private Date creationDate;
	private GameFilter gameFilter;
	private String ownerUserId;
	
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
	 * Gets the gameboardItem ids.
	 * 
	 * @return the gameboardItems (ids)
	 */
	public final List<String> getQuestions() {
		return questions;
	}

	/**
	 * Sets the gameboardItem ids.
	 * 
	 * @param questions
	 *            the gameboardItems ids to set
	 */
	public final void setQuestions(final List<String> questions) {
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
	 * @return the userId
	 */
	public final String getOwnerUserId() {
		return ownerUserId;
	}

	/**
	 * Sets the ownerUserId.
	 * @param ownerUserId the ownerUserId to set
	 */
	public final void setOwnerUserId(final String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}
}
