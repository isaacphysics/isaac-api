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
package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Sets;
import uk.ac.cam.cl.dtg.isaac.dto.GameFilter;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This class is the Domain Object used to store gameboards in the segue CMS. The primary difference between this DO and
 * the Gameboard DTO is the references to gameboardItems are ids rather than full objects.
 */
public class GameboardDO {
    @JsonProperty("_id")
    private String id;
    private String title;
    private List<String> questions;
    private List<GameboardContentDescriptor> contents;
    private IsaacWildcard wildCard;
    private Integer wildCardPosition;
    private Date creationDate;
    private GameFilter gameFilter;
    private Long ownerUserId;
    private GameboardCreationMethod creationMethod;
    private Set<String> tags;

    /**
     * Complete gameboard constructor with all dependencies.
     * 
     * @param id
     *            - unique id for the gameboard
     * @param title
     *            - optional title for gameboard.
     * @param questions
     *            - list of gameboard items (shallow questions).
     * @param contents
     *            - list of gameboard contents (can be questions or concepts).
     * @param wildCard
     *            - wildcard content object for advertising purposes.
     * @param wildcardPosition
     *            - position for where the front end should display this.
     * @param creationDate
     *            - Date in which the gameboard was created.
     * @param gameFilter
     *            - simple DO that represents the filter criteria used to creat the gameboard.
     * @param ownerUserId
     *            - User id of the owner of the gameboard.
     * @param creationMethod
     *            - Method used to construct this game board.
     */
    public GameboardDO(final String id, final String title, final List<String> questions,
                       final List<GameboardContentDescriptor> contents, final IsaacWildcard wildCard, final Integer wildcardPosition,
                       final Date creationDate, final GameFilter gameFilter, final Long ownerUserId,
                       final GameboardCreationMethod creationMethod, final Set<String> tags) {
        this.id = id;
        this.title = title;
        this.contents = contents;
        this.questions = questions;
        this.wildCard = wildCard;
        this.wildCardPosition = wildcardPosition;
        this.creationDate = creationDate;
        this.gameFilter = gameFilter;
        this.ownerUserId = ownerUserId;
        this.creationMethod = creationMethod;
        this.tags = tags;
    }

    /**
     * Default constructor required for AutoMapping.
     */
    public GameboardDO() {
        this.questions = Lists.newArrayList();
        this.contents = Lists.newArrayList();
        this.tags = Sets.newHashSet();
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
    public final IsaacWildcard getWildCard() {
        return wildCard;
    }

    /**
     * Sets the wildCard.
     * 
     * @param wildCard
     *            the wildCard to set
     */
    public final void setWildCard(final IsaacWildcard wildCard) {
        this.wildCard = wildCard;
    }

    /**
     * Gets the wildCardPosition.
     * 
     * @return the wildCardPosition
     */
    public final Integer getWildCardPosition() {
        return wildCardPosition;
    }

    /**
     * Sets the wildCardPosition.
     * 
     * @param wildCardPosition
     *            the wildCardPosition to set
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
    public final Long getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Sets the ownerUserId.
     * 
     * @param ownerUserId
     *            the ownerUserId to set
     */
    public final void setOwnerUserId(final Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    /**
     * Gets the creationMethod.
     * 
     * @return the creationMethod
     */
    public GameboardCreationMethod getCreationMethod() {
        return creationMethod;
    }

    /**
     * Sets the creationMethod.
     *
     * @param creationMethod
     *            the creationMethod to set
     */
    public void setCreationMethod(final GameboardCreationMethod creationMethod) {
        this.creationMethod = creationMethod;
    }

    /**
     * Gets the gameboard's tags.
     *
     * @return the gameboard's tags
     */
    public final Set<String> getTags() {
        return tags;
    }

    /**
     * Sets the gameboard's tags.
     *
     * @param tags
     *            the gameboard's tags to set
     */
    public final void setTags(final Set<String> tags) {
        this.tags = tags;
    }

    public List<GameboardContentDescriptor> getContents() {
        return contents;
    }

    public void setContents(List<GameboardContentDescriptor> contents) {
        this.contents = contents;
    }

    @Override
    public String toString() {
        return "GameboardDTO [id=" + id + ", title=" + title + ", contents=" + contents + ", questions=" + questions
                + ", wildCard=" + wildCard + ", wildCardPosition=" + wildCardPosition + ", creationDate=" + creationDate
                + ", gameFilter=" + gameFilter + ", ownerUserId=" + ownerUserId + ", creationMethod=" + creationMethod
                + ", tags=" + tags + "]";
    }
}
