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
package uk.ac.cam.cl.dtg.isaac.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.GameboardCreationMethod;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacWildcard;

/**
 * DTO representation of a gameboard.
 * 
 */
public class GameboardDTO {
	private String id;
	private String title;
	private List<GameboardItem> questions;
	private IsaacWildcard wildCard;
	private Integer wildCardPosition;
	private Date creationDate;
	private GameFilter gameFilter;
	private String ownerUserId;
	private GameboardCreationMethod creationMethod;
	
	private Integer percentageCompleted;
	private Date lastVisited;

	// indicates whether or not a question in this board has at least been marked as in progress
	private boolean startedQuestion;
	
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
	 * @param creationMethod
	 *            - Method used to construct this game board.
	 */
	public GameboardDTO(final String id, final String title,
			final List<GameboardItem> questions, final IsaacWildcard wildCard,
			final Integer wildcardPosition, final Date creationDate,
			final GameFilter gameFilter, final String ownerUserId,
			final GameboardCreationMethod creationMethod) {
		this.id = id;
		this.title = title;
		this.questions = questions;
		this.wildCard = wildCard;
		this.wildCardPosition = wildcardPosition;
		this.creationDate = creationDate;
		this.gameFilter = gameFilter;
		this.ownerUserId = ownerUserId;
		this.creationMethod = creationMethod;
	}

	/**
	 * Gets the id.
	 * 
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 * 
	 * @param id
	 *            the id to set
	 */
	public void setId(final String id) {
		this.id = id;
	}

	/**
	 * Gets the title.
	 * 
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Sets the title.
	 * 
	 * @param title
	 *            the title to set
	 */
	public void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Gets the gameboardItems.
	 * 
	 * @return the gameboardItems
	 */
	public List<GameboardItem> getQuestions() {
		return questions;
	}

	/**
	 * Sets the gameboardItems.
	 * 
	 * @param questions
	 *            the gameboardItems to set
	 */
	public void setQuestions(final List<GameboardItem> questions) {
		this.questions = questions;
	}

	/**
	 * Gets the wildCard.
	 * 
	 * @return the wildCard
	 */
	public IsaacWildcard getWildCard() {
		return wildCard;
	}

	/**
	 * Sets the wildCard.
	 * 
	 * @param wildCard
	 *            the wildCard to set
	 */
	public void setWildCard(final IsaacWildcard wildCard) {
		this.wildCard = wildCard;
	}

	/**
	 * Gets the wildCardPosition.
	 * @return the wildCardPosition
	 */
	public Integer getWildCardPosition() {
		return wildCardPosition;
	}
	
	/**
	 * Sets the wildCardPosition.
	 * @param wildCardPosition the wildCardPosition to set
	 */
	public void setWildCardPosition(final Integer wildCardPosition) {
		this.wildCardPosition = wildCardPosition;
	}
	
	/**
	 * Gets the creationDate.
	 * 
	 * @return the creationDate
	 */
	public Date getCreationDate() {
		return creationDate;
	}

	/**
	 * Sets the creationDate.
	 * 
	 * @param creationDate
	 *            the creationDate to set
	 */
	public void setCreationDate(final Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * Gets the gameFilter.
	 * 
	 * @return the gameFilter
	 */
	public GameFilter getGameFilter() {
		return gameFilter;
	}

	/**
	 * Sets the gameFilter.
	 * 
	 * @param gameFilter
	 *            the gameFilter to set
	 */
	public void setGameFilter(final GameFilter gameFilter) {
		this.gameFilter = gameFilter;
	}

	/**
	 * Gets the userId.
	 * 
	 * @return the userId
	 */
	public String getOwnerUserId() {
		return ownerUserId;
	}

	/**
	 * Sets the ownerUserId.
	 * 
	 * @param ownerUserId
	 *            the ownerUserId to set
	 */
	public void setOwnerUserId(final String ownerUserId) {
		this.ownerUserId = ownerUserId;
	}

	/**
	 * Gets the creationMethod.
	 * @return the creationMethod
	 */
	public GameboardCreationMethod getCreationMethod() {
		return creationMethod;
	}

	/**
	 * Sets the creationMethod.
	 * @param creationMethod the creationMethod to set
	 */
	public void setCreationMethod(final GameboardCreationMethod creationMethod) {
		this.creationMethod = creationMethod;
	}

	/**
	 * Gets the percentageCompleted.
	 * @return the percentageCompleted
	 */
	public Integer getPercentageCompleted() {
		return percentageCompleted;
	}

	/**
	 * Sets the percentageCompleted.
	 * @param percentageCompleted the percentageCompleted to set
	 */
	public void setPercentageCompleted(final Integer percentageCompleted) {
		this.percentageCompleted = percentageCompleted;
	}

	/**
	 * Gets the lastVisited.
	 * @return the lastVisited
	 */
	public Date getLastVisited() {
		return lastVisited;
	}

	/**
	 * Sets the lastVisited.
	 * @param lastVisited the lastVisited to set
	 */
	public void setLastVisited(final Date lastVisited) {
		this.lastVisited = lastVisited;
	}

	/**
	 * Gets the startedQuestion.
	 * @return the startedQuestion
	 */
	public boolean isStartedQuestion() {
		return startedQuestion;
	}

	/**
	 * Sets the startedQuestion.
	 * @param startedQuestion the startedQuestion to set
	 */
	public void setStartedQuestion(final boolean startedQuestion) {
		this.startedQuestion = startedQuestion;
	}

	@Override
	public String toString() {
		return "GameboardDTO [id=" + id + ", title=" + title + ", questions=" + questions + ", wildCard="
				+ wildCard + ", wildCardPosition=" + wildCardPosition + ", creationDate=" + creationDate
				+ ", gameFilter=" + gameFilter + ", ownerUserId=" + ownerUserId + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((creationDate == null) ? 0 : creationDate.hashCode());
		result = prime * result + ((gameFilter == null) ? 0 : gameFilter.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((ownerUserId == null) ? 0 : ownerUserId.hashCode());
		result = prime * result + ((questions == null) ? 0 : questions.hashCode());
		result = prime * result + ((wildCard == null) ? 0 : wildCard.hashCode());
		result = prime * result + ((wildCardPosition == null) ? 0 : wildCardPosition.hashCode());
		return result;
	}

	/**
	 * The only mutable field is the title field and so this is excluded from the equality check.
	 * @param obj - object to test equality of.
	 * @return true if the same or false if different.
	 */
	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof GameboardDTO)) {
			return false;
		}
		GameboardDTO other = (GameboardDTO) obj;
		if (creationDate == null) {
			if (other.creationDate != null) {
				return false;
			}
		} else if (!creationDate.equals(other.creationDate)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (ownerUserId == null) {
			if (other.ownerUserId != null) {
				return false;
			}
		} else if (!ownerUserId.equals(other.ownerUserId)) {
			return false;
		}
		if (questions == null) {
			if (other.questions != null) {
				return false;
			}
		} else if (!questions.equals(other.questions)) {
			return false;
		}
		if (wildCard == null) {
			if (other.wildCard != null) {
				return false;
			}
		} else if (!wildCard.equals(other.wildCard)) {
			return false;
		}
		if (wildCardPosition == null) {
			if (other.wildCardPosition != null) {
				return false;
			}
		} else if (!wildCardPosition.equals(other.wildCardPosition)) {
			return false;
		}
		return true;
	}
}
