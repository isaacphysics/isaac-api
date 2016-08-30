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
package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.quiz.ChoiceQuestionValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * Choice object The choice object is a specialised form of content and allows the storage of data relating to possible
 * answers to questions.
 * 
 */
@ValidatesWith(ChoiceQuestionValidator.class)
public class ChoiceQuestionDTO extends QuestionDTO {
    protected List<ChoiceDTO> choices;
    protected Boolean randomlyOrderChoices;

    /**
     * Default Constructor for mappers.
     */
    public ChoiceQuestionDTO() {

    }

    /**
     * Gets the choices.
     * 
     * @return the choices
     */
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
     * @return randomlyOrderChoices
     */
    public Boolean getRandomlyOrderChoices() {
        return randomlyOrderChoices;
    }

    /**
     * Sets the randomlyOrderChoices.
     *
     * @param randomlyOrderChoices
     *            the randomlyOrderChoices to set
     */
    public void setRandomlyOrderChoices(final Boolean randomlyOrderChoices) {
        this.randomlyOrderChoices = randomlyOrderChoices;
    }

}
