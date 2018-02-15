/**
 * Copyright 2017 Meurig Thomas
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

/**
 * DTO that extends GameboardItem with an additional list of QuestionPartConceptsDTOs which hold information about the
 * concepts related to each question part. Used for the FastTrack gameboard progress bar.
 */
public class FastTrackGameboardItem extends GameboardItem {
    private List<QuestionPartConceptDTO> questionPartConcepts;

    /**
     * Constructor which takes a gameboard item to set the super class's values.
     *
     * @param gameboardItem
     *            values to set for the super class.
     */
    public FastTrackGameboardItem(GameboardItem gameboardItem) {
        super(gameboardItem);
    }

    /**
     * Get question part concepts.
     *
     * @return question part concepts.
     */
    public final List<QuestionPartConceptDTO> getQuestionPartConcepts() { return this.questionPartConcepts; }

    /**
     * Set question part concepts.
     *
     * @param questionPartConcepts
     *            question parts to set.
     */
    public final void setQuestionPartConcepts(List<QuestionPartConceptDTO> questionPartConcepts) {
        this.questionPartConcepts = questionPartConcepts;
    }
}
