/*
 * Copyright 2014 Stephen Cummins
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

import uk.ac.cam.cl.dtg.isaac.dto.IsaacNumericQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

import java.util.ArrayList;
import java.util.List;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES;

/**
 * DO for isaacNumericQuestions.
 *
 */
@DTOMapping(IsaacNumericQuestionDTO.class)
@JsonContentType("isaacNumericQuestion")
@ValidatesWith(IsaacNumericValidator.class)
public class IsaacNumericQuestion extends IsaacQuestionBase {
    private Boolean requireUnits;
    private Integer significantFiguresMin;
    private Integer significantFiguresMax;
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
     * @param requireUnits
     *            the requireUnits to set
     */
    public final void setRequireUnits(final Boolean requireUnits) {
        this.requireUnits = requireUnits;
    }

    /**
     * Gets the minimum allowed number of significant figures.
     * 
     * @return the number of sig figs.
     */
    public int getSignificantFiguresMin() {
        if (null == significantFiguresMin) {
            return NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES;
        }
        return significantFiguresMin;
    }

    /**
     * Sets the minimum allowed number of significant figures.
     * 
     * @param significantFigures
     *            - minimum allowed number of significant figures
     */
    public void setSignificantFiguresMin(final Integer significantFigures) {
        this.significantFiguresMin = significantFigures;
    }

    /**
     * Gets the maximum allowed number of significant figures.
     *
     * @return the maximum allowed number of sig figs.
     */
    public int getSignificantFiguresMax() {
        if (null == significantFiguresMax) {
            return NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES;
        }
        return significantFiguresMax;
    }

    /**
     * Sets the maximum allowed number of significant figures.
     *
     * @param significantFigures
     *            - maximum allowed number of significant figures
     */
    public void setSignificantFiguresMax(final Integer significantFigures) {
        this.significantFiguresMax = significantFigures;
    }

    public final void setAvailableUnits(final List<String> availableUnits) {
        this.availableUnits = availableUnits;
    }

    public List<String> getAvailableUnits() {
        if (null == availableUnits) {
            return new ArrayList<String>();
        }
        return availableUnits;
    }

    /**
     *  Get the unit to be displayed to the user instead of the available units dropdown.
     *
     * @return the unit string
     */
    public String getDisplayUnit() {
        return displayUnit;
    }

    /**
     * Set the unit to be displayed to the user instead of the available units dropdown.
     * @param displayUnit - the unit to be displayed.
     */
    public void setDisplayUnit(String displayUnit) {
        this.displayUnit = displayUnit;
    }
}