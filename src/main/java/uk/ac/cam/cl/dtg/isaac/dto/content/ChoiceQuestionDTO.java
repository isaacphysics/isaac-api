/**
 * Copyright 2014 Stephen Cummins
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <p>
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ac.cam.cl.dtg.isaac.quiz.ChoiceQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * Choice object The choice object is a specialised form of content and allows the storage of data relating to possible
 * answers to questions.
 * 
 */
@ValidatesWith(ChoiceQuestionValidator.class)
public class ChoiceQuestionDTO extends QuestionDTO {
    private List<ChoiceDTO> choices;
    private Boolean randomiseChoices;

    /**
     * Default Constructor for mappers.
     */
    public ChoiceQuestionDTO() {

    }

    /**
     * Gets the choices. Defaults to not return choices.
     * Manually unignore, e.g. multi-choice questions
     * @return the choices
     */
    @JsonIgnore
    public List<ChoiceDTO> getChoices() {
        return choices;
    }

    /**
     * Sets the choices.
     * 
     * @param choices
     *            the choices to set
     */
    public final void setChoices(final List<ChoiceDTO> choices) {
        this.choices = choices;
    }

    /**
     * Gets the whether to randomlyOrderUnits.
     *
     * @return randomiseChoices
     */
    @JsonIgnore
    public Boolean getRandomiseChoices() {
        return randomiseChoices;
    }

    /**
     * Sets the randomiseChoices.
     *
     * @param randomiseChoices
     *            the randomiseChoices to set
     */
    public void setRandomiseChoices(final Boolean randomiseChoices) {
        this.randomiseChoices = randomiseChoices;
    }

}
