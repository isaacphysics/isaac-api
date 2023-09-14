/**
 * Copyright 2014 Stephen Cummins
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

package uk.ac.cam.cl.dtg.isaac.dto;

import com.google.api.client.util.Lists;
import java.util.List;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * DTO for isaacNumericQuestions.
 */
@JsonContentType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestionDTO extends IsaacQuestionBaseDTO {
  private Boolean requireUnits;
  private Boolean disregardSignificantFigures;
  private List<String> availableUnits;
  private String displayUnit;

  /**
   * Gets the requireUnits.
   *
   * @return the requireUnits
   */
  public final Boolean getRequireUnits() {
    if (requireUnits == null) {
      return true;
    }

    return requireUnits;
  }

  /**
   * Sets the requireUnits.
   *
   * @param requireUnits the requireUnits to set
   */
  public final void setRequireUnits(final Boolean requireUnits) {
    this.requireUnits = requireUnits;
  }

  /**
   * Gets the knownUnits.
   * <br>
   * This is a hack so that the frontend can display all units available as a drop down list.
   *
   * @return the knownUnits
   */
  public List<String> getKnownUnits() {
    List<String> unitsToReturn = Lists.newArrayList();

    for (ChoiceDTO c : this.getChoices()) {
      if (c instanceof QuantityDTO) {
        QuantityDTO quantity = (QuantityDTO) c;
        if (quantity.getUnits() != null && !quantity.getUnits().isEmpty()) {
          unitsToReturn.add(quantity.getUnits());
        }
      }
    }

    if (unitsToReturn.isEmpty()) {
      return null;
    }

    return unitsToReturn;
  }

  public List<String> getAvailableUnits() {
    return this.availableUnits;
  }

  public void setAvailableUnits(final List<String> availableUnits) {
    this.availableUnits = availableUnits;
  }

  /**
   * Get the unit to be displayed to the user instead of the available units dropdown.
   *
   * @return the unit string
   */
  public String getDisplayUnit() {
    return displayUnit;
  }

  /**
   * Set the unit to be displayed to the user instead of the available units dropdown.
   *
   * @param displayUnit - the unit to be displayed.
   */
  public void setDisplayUnit(final String displayUnit) {
    this.displayUnit = displayUnit;
  }

  /**
   * Set whether the question accepts values equal to the answer without considering significant figures.
   *
   * @param disregardSignificantFigures - whether to accept values equal to the answer without considering sig figs.
   */
  public final void setDisregardSignificantFigures(final boolean disregardSignificantFigures) {
    this.disregardSignificantFigures = disregardSignificantFigures;
  }

  /**
   * Get whether the question accepts values equal to the answer without considering significant figures.
   *
   * @return whether to accept values equal to the answer without considering sig figs.
   */
  public final Boolean getDisregardSignificantFigures() {
    if (this.disregardSignificantFigures == null) {
      return false;
    }
    return this.disregardSignificantFigures;
  }
}
