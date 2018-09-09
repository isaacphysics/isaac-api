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

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.api.Constants.GameboardItemState;

/**
 * DTO that provides high level information for Isaac Questions.
 * 
 * Used for gameboards to represent cut down versions of questions
 */
public class GameboardItem {
    private String id;
    private String title;
    private String description;
    private String uri;
    private List<String> tags;

    private Integer level;
    private Integer questionPartsCorrect;
    private Integer questionPartsIncorrect;
    private Integer questionPartsNotAttempted;
    private Integer questionPartsTotal;
    private Float passMark;
    private GameboardItemState state;
    private List<Constants.QuestionPartState> questionPartStates = Lists.newArrayList();
    
    // optional field if we want to use the gameboard item outside of the context of a board.
    @Nullable
    private String boardId;
    
    /**
     * Generic constructor.
     */
    public GameboardItem() {}

    /**
     * Creates a GameboardItem from (shallow) copying the passed in GameboardItem.
     *
     * @param original
     *          the original gameboard item to copy
     */
    public GameboardItem(GameboardItem original) {
        this.setId(original.getId());
        this.setTitle(original.getTitle());
        this.setDescription(original.getDescription());
        this.setUri(original.getUri());
        this.setLevel(original.getLevel());
        this.setQuestionPartsCorrect(original.getQuestionPartsCorrect());
        this.setQuestionPartsIncorrect(original.getQuestionPartsIncorrect());
        this.setQuestionPartsNotAttempted(original.getQuestionPartsNotAttempted());
        this.setPassMark(original.getPassMark());
        this.setState(original.getState());
        this.setTags(original.getTags());
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
     * Gets the description.
     * 
     * @return the description
     */
    public final String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * 
     * @param description
     *            the description to set
     */
    public final void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the uri.
     * 
     * @return the uri
     */
    public final String getUri() {
        return uri;
    }

    /**
     * Sets the uri.
     * 
     * @param uri
     *            the uri to set
     */
    public final void setUri(final String uri) {
        this.uri = uri;
    }

    /**
     * Gets the tags.
     * 
     * @return the tags
     */
    public final List<String> getTags() {
        return tags;
    }

    /**
     * Sets the tags.
     * 
     * @param tags
     *            the tags to set
     */
    public final void setTags(final List<String> tags) {
        this.tags = tags;
    }

    /**
     * Gets the level.
     * 
     * @return the level
     */
    public final Integer getLevel() {
        return level;
    }

    /**
     * Sets the level.
     * 
     * @param level
     *            the level to set
     */
    public final void setLevel(final Integer level) {
        this.level = level;
    }

    public final List<Constants.QuestionPartState> getQuestionPartStates(){
        return this.questionPartStates;
    }

    public final void setQuestionPartStates(final List<Constants.QuestionPartState> questionPartStates) {
        this.questionPartStates = questionPartStates;
    }

    // TODO in time we should be able to remove the question part counters and just use questionPartStates instead,
    // that will require altering some of the front end code - the assignment progress page in particular.
    /**
     * Gets the number of questionPartsCorrect.
     * 
     * @return the number of questionPartsCorrect
     */
    public final Integer getQuestionPartsCorrect() {
        return questionPartsCorrect;
    }

    /**
     * Sets the number of correct question parts.
     *
     * @param questionPartsCorrect
     *            the number of correct question parts to set
     */
    public final void setQuestionPartsCorrect(final Integer questionPartsCorrect) {
        this.questionPartsCorrect = questionPartsCorrect;
    }

    /**
     * Gets the number of questionPartsIncorrect.
     * 
     * @return the number of questionPartsIncorrect
     */
    public final Integer getQuestionPartsIncorrect() {
        return questionPartsIncorrect;
    }

    /**
     * Sets the number of incorrect question parts.
     *
     * @param questionPartsIncorrect
     *            the number of incorrect question parts to set
     */
    public final void setQuestionPartsIncorrect(final Integer questionPartsIncorrect) {
        this.questionPartsIncorrect = questionPartsIncorrect;
    }

    /**
     * Gets the number of questionPartsNotAttempted.
     * 
     * @return the number of questionPartsNotAttempted
     */
    public final Integer getQuestionPartsNotAttempted() {
        return questionPartsNotAttempted;
    }

    /**
     * Sets the number of question parts not attempted.
     *
     * @param questionPartsNotAttempted
     *            the number of question parts to set
     */
    public final void setQuestionPartsNotAttempted(final Integer questionPartsNotAttempted) {
        this.questionPartsNotAttempted = questionPartsNotAttempted;
    }

    /**
     * When question part information is included gets the total number of question parts.
     *
     * @return the total number of question parts
     */
    public final Integer getQuestionPartsTotal() {
        return this.questionPartsTotal;
    }

    /**
     * Sets the total number of question parts.
     *
     * @param questionPartsTotal
     *            the number of question parts to set
     */
    public final void setQuestionPartsTotal(final Integer questionPartsTotal) {
        this.questionPartsTotal = questionPartsTotal;
    }

    /**
     * Gets the passMark as a percentage.
     *
     * @return the passMark as a percentage
     */
    public Float getPassMark() {
        return this.passMark;
    }

    /**
     * Sets the pass mark for the question, sets it to be 100 if passed a null.
     *
     * @param passMark
     *            the pass mark to set
     */
    public final void setPassMark(final Float passMark) {
        this.passMark = passMark;
    }

    /**
     * Gets the state.
     * 
     * @return the state
     */
    public final GameboardItemState getState() {
        return state;
    }

    /**
     * Sets the state.
     * 
     * @param state
     *            the state to set
     */
    public final void setState(final GameboardItemState state) {
        this.state = state;
    }

    /**
     * Gets the boardId.
     * @return the boardId
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the boardId.
     * @param boardId the boardId to set
     */
    public void setBoardId(final String boardId) {
        this.boardId = boardId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GameboardItem)) {
            return false;
        }
        GameboardItem other = (GameboardItem) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return true;
    }
}
