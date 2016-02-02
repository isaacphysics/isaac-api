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
package uk.ac.cam.cl.dtg.segue.dos;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dto.FormulaValidationResponseDTO;

import java.util.Date;

/**
 * The DO which can be used to inform clients of the result of an answered question.
 * 
 */
@DTOMapping(FormulaValidationResponseDTO.class)
public class FormulaValidationResponse extends QuestionValidationResponse {
    private Boolean correctSymbolic;
    private Boolean correctNumeric;

    /**
     * Default constructor.
     */
    public FormulaValidationResponse() {

    }

    /**
     * Full constructor.
     *
     * @param questionId
     *            -
     * @param answer
     *            -
     * @param explanation
     *            -
     * @param correctSymbolic
     *            -
     * @param correctNumeric
     *            -
     * @param dateAttempted
     *            -
     */
    public FormulaValidationResponse(final String questionId, final Choice answer,
                                     final Content explanation, final Boolean correctSymbolic,
                                     final Boolean correctNumeric, final Date dateAttempted) {
        super(questionId, answer, correctSymbolic || correctNumeric, explanation, dateAttempted);
        this.correctSymbolic = correctSymbolic;
        this.correctNumeric = correctNumeric;
    }

    /**
     * Gets the correctValue.
     * 
     * @return the correctValue
     */
    public final Boolean getCorrectSymbolic() {
        return correctSymbolic;
    }

    /**
     * Sets the correctValue.
     * 
     * @param correctSymbolic
     *            the correctSymbolic to set
     */
    public final void setCorrectSymbolic(final Boolean correctSymbolic) {
        this.correctSymbolic = correctSymbolic;
        // N.B. If we ever get here, it's likely that this.correct is now out of date.
        // This should really be an immutable object, so we shouldn't need this method.
    }

    /**
     * Gets the correctUnits.
     * 
     * @return the correctNumeric
     */
    public final Boolean getCorrectNumeric() {
        return correctNumeric;
    }

    /**
     * Sets the correctUnits.
     * 
     * @param correctNumeric
     *            the correctNumeric to set
     */
    public final void setCorrectNumeric(final Boolean correctNumeric) {
        this.correctNumeric = correctNumeric;
        // N.B. If we ever get here, it's likely that this.correct is now out of date.
        // This should really be an immutable object, so we shouldn't need this method.
    }

    @Override
    public String toString() {
        return "QuantityValidationResponse [correctSymbolic=" + correctSymbolic + ", correctNumeric=" + correctNumeric + "]";
    }
}
