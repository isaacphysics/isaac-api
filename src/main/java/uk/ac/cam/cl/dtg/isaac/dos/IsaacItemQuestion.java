/*
 * Copyright 2019 James Sharkey
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
package uk.ac.cam.cl.dtg.isaac.dos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacItemQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacItemQuestionValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.dos.content.Item;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

import java.util.List;


/**
 * Content DO for IsaacItemQuestions.
 *
 */
@DTOMapping(IsaacItemQuestionDTO.class)
@JsonContentType("isaacItemQuestion")
@ValidatesWith(IsaacItemQuestionValidator.class)
public class IsaacItemQuestion extends IsaacQuestionBase {

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
    @JsonIgnore
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

}