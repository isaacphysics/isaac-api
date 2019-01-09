/**
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
package uk.ac.cam.cl.dtg.segue.dos;

import java.util.Date;

import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;

/**
 * Question Validation Response DO.
 *
 */
@DTOMapping(QuestionValidationResponseDTO.class)
public class QuestionValidationResponse extends LightweightQuestionValidationResponse {
    private Choice answer;
    private Content explanation;

    /**
     * Default Constructor for mappers.
     */
    public QuestionValidationResponse() {
        super();
    }

    /**
     * Full constructor.
     * 
     * @param questionId
     *            -
     * @param answer
     *            -
     * @param correct
     *            -
     * @param explanation
     *            -
     * @param dateAttempted
     *            -
     */
    public QuestionValidationResponse(final String questionId, final Choice answer, final Boolean correct,
            final Content explanation, final Date dateAttempted) {
        super(questionId, correct, dateAttempted);
        this.answer = answer;
        this.explanation = explanation;
    }

    /**
     * Gets the answer.
     * 
     * @return the answer
     */
    public final Choice getAnswer() {
        return answer;
    }

    /**
     * Sets the answer.
     * 
     * @param answer
     *            the answer to set
     */
    public final void setAnswer(final Choice answer) {
        this.answer = answer;
    }

    /**
     * Gets the explanation.
     * 
     * @return the explanation
     */
    public final Content getExplanation() {
        return explanation;
    }

    /**
     * Sets the explanation.
     * 
     * @param explanation
     *            the explanation to set
     */
    public final void setExplanation(final Content explanation) {
        this.explanation = explanation;
    }

    @Override
    public String toString() {
        return "QuestionValidationResponse [questionId=" + super.getQuestionId() + ", answer=" + answer +
                ", correct=" + super.isCorrect() + ", explanation=" + explanation +
                ", dateAttempted=" + super.getDateAttempted() + "]";
    }

}
