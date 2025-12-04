/*
 * Copyright 2021 Chris Purdy
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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacClozeQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacDndQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacClozeValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacItemQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;


/**
 * Content DO for IsaacDndQuestions.
 *
 */
@DTOMapping(IsaacDndQuestionDTO.class)
@JsonContentType("isaacDndQuestion")
@ValidatesWith(IsaacDndValidator.class)
public class IsaacDndQuestion extends Question {
    private List<Item> items;
    private Boolean randomiseItems;

    public List<Item> getItems() {
        return items;
    }

    public void setItems(final List<Item> items) {
        this.items = items;
    }

    /**
     * Gets whether to randomiseItems.
     *
     * @return randomiseItems
     */
    public Boolean getRandomiseItems() {
        return randomiseItems;
    }

    /**
     * Sets the randomiseItems.
     *
     * @param randomiseItems
     *            the randomiseItems to set
     */
    public void setRandomiseItems(final Boolean randomiseItems) {
        this.randomiseItems = randomiseItems;
    }

    private Boolean withReplacement;
    // Detailed feedback option not needed in the client so not in DTO:
    private Boolean detailedItemFeedback;

    public Boolean getWithReplacement() {
        return withReplacement;
    }

    public void setWithReplacement(final Boolean withReplacement) {
        this.withReplacement = withReplacement;
    }

    public Boolean getDetailedItemFeedback() {
        return detailedItemFeedback;
    }

    public void setDetailedItemFeedback(final Boolean detailedItemFeedback) {
        this.detailedItemFeedback = detailedItemFeedback;
    }

    protected List<DndItemChoice> choices;
    protected Boolean randomiseChoices;

    /**
     * Gets the choices.
     *
     * @return the choices
     */
    public final List<DndItemChoice> getChoices() {
        return choices;
    }

    /**
     * Sets the choices.
     *
     * @param choices
     *            the choices to set
     */
    public final void setChoices(final List<DndItemChoice> choices) {
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
