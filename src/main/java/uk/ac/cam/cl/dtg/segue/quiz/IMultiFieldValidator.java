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

import java.util.List;

import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Interface that allows questions to have multiple fields.
 * 
 * @author Stephen Cummins
 */
public interface IMultiFieldValidator extends IValidator {

    /**
     * ValidateMultiFieldQuestionResponses This will allow a questoin with multiple fields to be validated by the quiz
     * engine.
     * 
     * @param question
     *            question to be validated.
     * @param answer
     *            answer from the user to validate.
     * @return a List of QuestionValidationResponseDTOs.
     */
    List<QuestionValidationResponseDTO> validateMultiFieldQuestionResponses(Question question, List<ChoiceDTO> answer);
}
