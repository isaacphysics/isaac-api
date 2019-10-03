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
package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dto.ItemQuestionValidationResponseDTO;

import java.util.Date;
import java.util.List;

/**
 * The DO which can be used to inform clients of the result of an answered question.
 *
 */
@DTOMapping(ItemQuestionValidationResponseDTO.class)
public class ItemQuestionValidationResponse extends QuestionValidationResponse {
    public static ItemQuestionValidationResponse createDefaultFailedResponse(
            final String questionId, final Choice submittedChoice) {
        return new ItemQuestionValidationResponse(questionId, submittedChoice, false, null, null, new Date());
    }

    private List<String> incorrectItemIds;

    /**
     * Default constructor.
     */
    public ItemQuestionValidationResponse() {

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
    public ItemQuestionValidationResponse(final String questionId, final Choice answer, final Boolean correct,
                                      final Content explanation, final List<String> incorrectItemIds, final Date dateAttempted) {
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
        String value = "null";
        if (null != incorrectItemIds) {
            value = incorrectItemIds.toString();
        }
        return "ItemQuestionValidationResponse [incorrectItemIds=" + value + "]";
    }
}
