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
package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ContentDTO;

import java.util.Date;

/**
 * The DTO which can be used to inform clients of the result of an answered question.
 * 
 * 
 */
public class FormulaValidationResponseDTO extends QuestionValidationResponseDTO {
    private Boolean correctExact;
    private Boolean correctSymbolic;
    private Boolean correctNumeric;

    /**
     * Default constructor.
     */
    public FormulaValidationResponseDTO() {

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
    public FormulaValidationResponseDTO(final String questionId, final ChoiceDTO answer,
                                        final ContentDTO explanation, final Boolean correctExact,
                                        final Boolean correctSymbolic, final Boolean correctNumeric,
                                        final Date dateAttempted) {
        super(questionId, answer, correctSymbolic || correctNumeric, explanation, dateAttempted);
        this.correctExact = correctExact;
        this.correctSymbolic = correctSymbolic;
        this.correctNumeric = correctNumeric;
    }

    /**
     * Gets the correctExact.
     *
     * @return the correctExact
     */
    public final Boolean getCorrectExact() {
        return correctExact;
    }

    /**
     * Sets the correctExact.
     *
     * @param correctExact
     *            the correctExact to set
     */
    public final void setCorrectExact(final Boolean correctExact) {
        this.correctExact = correctExact;
        // N.B. If we ever get here, it's likely that this.correct is now out of date.
        // This should really be an immutable object, so we shouldn't need this method.
    }

    /**
     * Gets the correctSymbolic.
     *
     * @return the correctSymbolic
     */
    public final Boolean getCorrectSymbolic() {
        return correctSymbolic;
    }

    /**
     * Sets the correctSymbolic.
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
     * Gets the correctNumeric.
     * 
     * @return the correctNumeric
     */
    public final Boolean getCorrectNumeric() {
        return correctNumeric;
    }

    /**
     * Sets the correctNumeric.
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
        return "FormulaValidationResponseDTO [correctExact=" + correctExact + "correctSymbolic=" + correctSymbolic + ", correctNumeric=" + correctNumeric + "]";
    }
}
