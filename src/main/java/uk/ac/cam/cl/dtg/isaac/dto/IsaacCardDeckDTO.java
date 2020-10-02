/**
 * Copyright 2020 Connor Holloway
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


import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

import java.util.List;

/**
 * DTO to represent a card.
 */
public class IsaacCardDeckDTO extends ContentDTO {
    private List<IsaacCardDTO> cards;

    /**
     * Default constructor required for mapping.
     */
    public IsaacCardDeckDTO() {}

    /**
     * Get the cards.
     * @return the cards.
     */
    public List<IsaacCardDTO> getCards() {
        return cards;
    }

    /**
     * Set the cards.
     * @param cards the cards to set.
     */
    public void setCards(List<IsaacCardDTO> cards) {
        this.cards = cards;
    }
}
