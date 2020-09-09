/**
 * Copyright 2020 Connor Holloway
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

import uk.ac.cam.cl.dtg.isaac.api.Constants.*;

import java.util.List;

/**
 * DTO to provide information about the completion of parts of a question
 * 
 */
public class QuestionCompletionDTO {
    protected String id;
    protected List<QuestionPartState> questionPartStates;

    /**
     * Default constructor for mappers.
     */
    public QuestionCompletionDTO() {}

    /**
     * Gets the id
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id
     *            the id to set
     */
    public void setId(final String id) {
        this.id = id;
    }

    /**
     * Gets the list of question part states
     * @return the list of question part states
     */
    public List<QuestionPartState> getState() {
        return questionPartStates;
    }

    /**
     * Sets the list of question part states.
     * 
     * @param questionPartStates
     *            the list of question part states to set
     */
    public void setState(final List<QuestionPartState> questionPartStates) {
        this.questionPartStates = questionPartStates;
    }

}
