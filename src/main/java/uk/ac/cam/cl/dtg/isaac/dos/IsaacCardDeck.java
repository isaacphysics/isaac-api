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
package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDeckDTO;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;

import java.util.List;

/**
 * A container for isaacCard objects.
 */
@DTOMapping(IsaacCardDeckDTO.class)
@JsonContentType("isaacCardDeck")
public class IsaacCardDeck extends Content {
    private List<IsaacCard> cards;

    /**
     * Get the cards.
     * @return the cards.
     */
    public List<IsaacCard> getCards() {
        return cards;
    }

    /**
     * Set the list of cards.
     * @param cards the list of cards to set.
     */
    public void setCards(List<IsaacCard> cards) {
        this.cards = cards;
    }
}
