/**
 * Copyright 2016 Ian Davies
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

import java.util.HashMap;

/**
 * DTO to represent a Quantity choice.
 *
 */
public class FormulaDTO extends ChoiceDTO {
    private String pythonExpression;
    private boolean requiresExactMatch;
    private HashMap<String, Integer> enumeratedVariables;

    /**
     * Default constructor required for mapping.
     */
    public FormulaDTO() {

    }

    /**
     * Gets the python expression.
     *
     * @return the python expression
     */
    public final String getPythonExpression() {
        return pythonExpression;
    }

    /**
     * Sets the python expression.
     *
     * @param pythonExpression
     *            the python expression to set
     */
    public final void setPythonExpression(final String pythonExpression) {
        this.pythonExpression = pythonExpression;
    }

    /**
     * @return Whether this formula requires an exact match. Believe it or not.
     */
    public boolean requiresExactMatch() {
        return requiresExactMatch;
    }

    /**
     * Yes, you guessed it. Sets whether this formula requires an exact match.
     *
     * @param requiresExactMatch Whether this formula requires an exact match. I'm not kidding.
     */
    public void setRequiresExactMatch(boolean requiresExactMatch) {
        this.requiresExactMatch = requiresExactMatch;
    }

    public final HashMap<String, Integer> getEnumeratedVariables() {return enumeratedVariables;}

    public final void setEnumeratedVariables(HashMap<String, Integer> enumeratedVariables) {
        this.enumeratedVariables = enumeratedVariables;
    }
}
