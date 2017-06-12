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
package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Choice object The choice object is a specialized form of content and allows the storage of data relating to possible
 * answers to questions.
 * 
 */
@DTOMapping(ChoiceDTO.class)
@JsonContentType("choice")
public class Choice extends Content {
    protected boolean correct;
    protected ContentBase explanation;

    /**
     * Default Constructor required for mappers.
     */
    public Choice() {

    }

    /**
     * Gets the correct.
     * 
     * @return the correct
     */
    public final boolean isCorrect() {
        return correct;
    }

    /**
     * Sets the correct.
     * 
     * @param correct
     *            the correct to set
     */
    public final void setCorrect(final boolean correct) {
        this.correct = correct;
    }

    /**
     * Gets the explanation.
     * 
     * @return the explanation
     */
    public final ContentBase getExplanation() {
        return explanation;
    }

    /**
     * Sets the explanation.
     * 
     * @param explanation
     *            the explanation to set
     */
    public final void setExplanation(final ContentBase explanation) {
        this.explanation = explanation;
    }
}
