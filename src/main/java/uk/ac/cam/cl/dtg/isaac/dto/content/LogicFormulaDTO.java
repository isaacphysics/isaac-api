/**
 * Copyright 2019 James Sharkey
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

package uk.ac.cam.cl.dtg.isaac.dto.content;

/**
 * DTO to represent a Logic Formula choice.
 */
public class LogicFormulaDTO extends ChoiceDTO {
  private String pythonExpression;
  private boolean requiresExactMatch;

  /**
   * Default constructor required for mapping.
   */
  public LogicFormulaDTO() {

  }

  public final String getPythonExpression() {
    return pythonExpression;
  }

  public final void setPythonExpression(final String pythonExpression) {
    this.pythonExpression = pythonExpression;
  }

  public boolean requiresExactMatch() {
    return requiresExactMatch;
  }

  public void setRequiresExactMatch(final boolean requiresExactMatch) {
    this.requiresExactMatch = requiresExactMatch;
  }
}
