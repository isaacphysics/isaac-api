/**
 * Copyright 2016 Alistair Stead
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
package uk.ac.cam.cl.dtg.segue.dos.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.ac.cam.cl.dtg.segue.dto.content.FormulaDTO;

/**
 * Formula is a specialised choice object that allows a python expression representing the formula to be stored.
 *
 * @author Alistair Stead
 *
 */
@DTOMapping(FormulaDTO.class)
@JsonContentType("formula")
@JsonIgnoreProperties({ "_id" })
public class Formula extends Choice {
    private String pythonExpression;
    private boolean requiresExactMatch;
    
    public Formula() {
        
    }
    
    /**
     * @return the pythonExpression
     */
    public String getPythonExpression() {
        return pythonExpression;
    }

    /**
     * @param pythonExpression the pythonExpression to set
     */
    public void setPythonExpression(final String pythonExpression) {
        this.pythonExpression = pythonExpression;
    }

    /**
     * @return Whether this formula requires an exact match. Believe it or not.
     */
    public boolean getRequiresExactMatch() {
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

}
