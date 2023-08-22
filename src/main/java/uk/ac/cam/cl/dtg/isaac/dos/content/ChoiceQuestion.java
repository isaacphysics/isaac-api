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
package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.ChoiceQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

/**
 * Choice object The choice object is a specialised form of content and allows the storage of data relating to possible
 * answers to questions.
 * 
 */
@DTOMapping(ChoiceQuestionDTO.class)
@JsonContentType("choiceQuestion")
@ValidatesWith(ChoiceQuestionValidator.class)
public class ChoiceQuestion extends Question {
    private List<Choice> choices;
    private Boolean randomiseChoices;

    /**
     * Default constructors for auto mappers.
     */
    public ChoiceQuestion() {

    }

    /**
     * Gets the choices.
     * 
     * @return the choices
     */
    public final List<Choice> getChoices() {
        return choices;
    }

    /**
     * Sets the choices.
     * 
     * @param choices
     *            the choices to set
     */
    public final void setChoices(final List<Choice> choices) {
        this.choices = choices;
    }

    /**
     * Gets the whether to randomlyOrderUnits.
     *
     * @return randomiseChoices
     */
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
