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
    private Float passMark;

    // optional field if we want to use the gameboard item outside of the context of a board.
    @Nullable
    private String boardId;
    
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

    /**
     * Gets the number of questionPartsCorrect.
     * 
     * @return the number of questionPartsCorrect
     */
    public final Integer getQuestionPartsCorrect() {
        return questionPartsCorrect;
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
     * Gets the number of questionPartsNotAttempted.
     * 
     * @return the number of questionPartsNotAttempted
     */
    public final Integer getQuestionPartsNotAttempted() {
        return questionPartsNotAttempted;
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
     * A method to check that all status fields are set to non-null values
     *
     * @return true if the status fields are all non-null
     */
    private final boolean statusInformationIsNotNull() {
        return this.questionPartsCorrect != null && this.questionPartsIncorrect != null
                && this.questionPartsNotAttempted != null && this.passMark != null;
    }

    /**
     * Sets status information for the object.
     * Status results rely on all of these values being defined, hence why they are set together.
     *
     * @param questionPartsCorrect number of correct question parts
     * @param questionPartsIncorrect number of incorrect question parts
     * @param questionPartsNotAttempted number of question parts not attempted
     * @param passMark pass mark as a percentage
     */
    public final void setStatusInformation(final Integer questionPartsCorrect, final Integer questionPartsIncorrect,
                                           final Integer questionPartsNotAttempted, final Float passMark) {
        this.questionPartsCorrect = questionPartsCorrect;
        this.questionPartsIncorrect = questionPartsIncorrect;
        this.questionPartsNotAttempted = questionPartsNotAttempted;
        this.passMark = passMark;
    } 

    /**
     * Gets the number of total number of question parts.
     * 
     * @return the number of questionPartsTotal
     */
    public final Integer getQuestionPartsTotal() {
        Integer result = null;
        if (this.statusInformationIsNotNull()) {
            result = this.questionPartsCorrect + this.questionPartsIncorrect + this.questionPartsNotAttempted;
        }
        return result;
    }

    /**
     * Calculates the percentage ratio that the question parts argument represents for this question
     *
     * @param questionParts the number of question parts
     * @return question part percentage
     */
    public final Float calculatePercentageOfQuestionParts(final Integer questionParts) {
        Float result = null;
        if (this.statusInformationIsNotNull()) {
            result = 100f * questionParts / this.getQuestionPartsTotal();
        }
        return result;
    }

    /**
     * Calculates and returns the state if the required fields have been set for the object.
     *
     * @return the state
     */
    public final GameboardItemState getState() {
        GameboardItemState state = null;
        if (this.statusInformationIsNotNull()) {
            Integer questionPartsTotal = this.getQuestionPartsTotal();
            float percentCorrect = this.calculatePercentageOfQuestionParts(this.questionPartsCorrect);
            float percentIncorrect = this.calculatePercentageOfQuestionParts(this.questionPartsIncorrect);

            if (this.questionPartsCorrect == questionPartsTotal) {
                state = GameboardItemState.PERFECT;
            } else if (this.questionPartsNotAttempted == questionPartsTotal) {
                state = GameboardItemState.NOT_ATTEMPTED;
            } else if (percentCorrect >= this.passMark) {
                state = GameboardItemState.PASSED;
            } else if (percentIncorrect > (100 - this.passMark)) {
                state = GameboardItemState.FAILED;
            } else {
                state = GameboardItemState.IN_PROGRESS;
            }
        }
        return state;
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
