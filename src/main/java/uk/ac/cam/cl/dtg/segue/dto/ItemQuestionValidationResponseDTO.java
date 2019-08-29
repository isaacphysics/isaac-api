/*
 * Copyright 2019 Connor Holloway
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
package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

import java.util.Date;
import java.util.List;

/**
 * The DO which can be used to inform clients of the result of an answered question.
 *
 */
public class ItemQuestionValidationResponseDTO extends QuestionValidationResponseDTO {
    private List<String> incorrectItemIds;

    /**
     * Default constructor.
     */
    public ItemQuestionValidationResponseDTO() {

    }

    /**
     * Full constructor.
     *
     * @param questionId
     *            -
     * @param answer
     *            -
     * @param correct
     *            -
     * @param explanation
     *            -
     * @param incorrectItemIds
     *            -
     * @param dateAttempted
     *            -
     */
    public ItemQuestionValidationResponseDTO(final String questionId, final ChoiceDTO answer, final Boolean correct,
                                             final ContentDTO explanation, final List<String> incorrectItemIds, final Date dateAttempted) {
        super(questionId, answer, correct, explanation, dateAttempted);
        this.incorrectItemIds = incorrectItemIds;
    }

    /**
     * Gets the answer.
     *
     * @return  incorrectItemIds
     */
    public List<String> getIncorrectItemIds() {
        return incorrectItemIds;
    }

    /**
     * Sets the incorrectItemIds.
     *
     * @param incorrectItemIds
     *            the incorrectItemIds to set
     */
    public void setIncorrectItemIds(final List<String> incorrectItemIds) {
        this.incorrectItemIds = incorrectItemIds;
    }

    @Override
    public String toString() {
        return "ItemQuestionValidationResponseDTO [incorrectItemIds=" + incorrectItemIds.toString() + "]";
    }
}

