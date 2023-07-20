/*
 * Copyright 2022 James Sharkey
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

import uk.ac.cam.cl.dtg.isaac.dos.IsaacItemQuestion;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import java.util.Date;
import java.util.List;

/**
 *  Class for providing correctness feedback about individual items in a submitted Choice.
 *
 *  This is unlikely to be useful for {@link IsaacItemQuestion}'s, however, since to provide
 *  detailed correctness feedback on them would enable questions to be answered trivially.
 */
public class ItemValidationResponseDTO extends QuestionValidationResponseDTO {
    private List<Boolean> itemsCorrect;

    /**
     * Default constructor for Jackson.
     */
    public ItemValidationResponseDTO() {
    }

    /**
     *  Full constructor.
     *
     * @param questionId - questionId.
     * @param answer - answer.
     * @param correct - correct.
     * @param itemsCorrect - ordered list of correctness status of each submitted item.
     * @param explanation - explanation.
     * @param dateAttempted - dateAttempted.
     */
    public ItemValidationResponseDTO(final String questionId, final ChoiceDTO answer,
                                     final Boolean correct, final List<Boolean> itemsCorrect,
                                     final ContentDTO explanation, final Date dateAttempted) {
        super(questionId, answer, correct, explanation, dateAttempted);
        this.itemsCorrect = itemsCorrect;
    }

    public List<Boolean> getItemsCorrect() {
        return itemsCorrect;
    }

    public void setItemsCorrect(final List<Boolean> itemsCorrect) {
        this.itemsCorrect = itemsCorrect;
    }
}
