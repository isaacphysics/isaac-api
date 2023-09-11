/**
 * Copyright 2020 Connor Holloway
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.dos;

import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCardDeckDTO;

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
  public void setCards(final List<IsaacCard> cards) {
    this.cards = cards;
  }
}
