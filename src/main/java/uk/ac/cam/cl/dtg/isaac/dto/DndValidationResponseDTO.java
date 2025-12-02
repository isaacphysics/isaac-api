/*
 * Copyright 2022 James Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;

import java.util.Date;
import java.util.Map;

/**
 *  Class for providing correctness feedback about drag and drop questions in a submitted Choice.
 */
public class DndValidationResponseDTO extends QuestionValidationResponseDTO {
    private Map<String, Boolean> dropZonesCorrect;

    /**
     * Default constructor for Jackson.
     */
    public DndValidationResponseDTO() {
    }

    /**
     *  Full constructor.
     *
     * @param questionId - questionId.
     * @param answer - answer.
     * @param correct - correct.
     * @param dropZonesCorrect - map of correctness status of each submitted item. Key: dropZoneId, value: isCorrect
     * @param explanation - explanation.
     * @param dateAttempted - dateAttempted.
     */
    public DndValidationResponseDTO(final String questionId, final ChoiceDTO answer,
                                 final Boolean correct, final Map<String, Boolean> dropZonesCorrect,
                                 final ContentDTO explanation, final Date dateAttempted) {
        super(questionId, answer, correct, explanation, dateAttempted);
        this.dropZonesCorrect = dropZonesCorrect;
    }

    public Map<String, Boolean> getDropZonesCorrect() {
        return dropZonesCorrect;
    }

    public void setDropZonesCorrect(final Map<String, Boolean> dropZonesCorrect) {
        this.dropZonesCorrect = dropZonesCorrect;
    }
}
