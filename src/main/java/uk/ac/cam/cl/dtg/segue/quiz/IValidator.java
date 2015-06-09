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
package uk.ac.cam.cl.dtg.segue.quiz;

import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Interface that allows the quiz engine to validate questions and answers.
 * 
 * Note: It is expected that the classes implementing this interface can be automatically instantiated using the default
 * constructor.
 * 
 * @author Stephen Cummins
 *
 */
public interface IValidator {

    /**
     * validateQuestionResponse This method is specifically for single field questions.
     * 
     * i.e. when a question expects a single answer from the user.
     * 
     * @param question
     *            - question to check against.
     * @param answer
     *            - answer from the user.
     * @return a QuestionValidationResponseDTO
     */
    QuestionValidationResponseDTO validateQuestionResponse(Question question, ChoiceDTO answer);
}
