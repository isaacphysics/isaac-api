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
package uk.ac.cam.cl.dtg.isaac.quiz;

import java.util.Date;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

/**
 * Quiz validator for UnmarkedInputQuestionValidator.
 * 
 * This validator will not bother to try and mark the answer but will store it exactly as entered and return the correct
 * answer in the explanation field.
 * 
 * This relies on the annotation {@link ValidatesWith} being used.
 */
public class UnmarkedInputQuestionValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(UnmarkedInputQuestionValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, 
            final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        // check that the question is of type ChoiceQuestion before we go ahead
        ChoiceQuestion choiceQuestion;
        if (question instanceof ChoiceQuestion) {
            choiceQuestion = (ChoiceQuestion) question;
            
            return new QuestionValidationResponse(question.getId(), answer, null, (Content) choiceQuestion.getAnswer(),
                    new Date());
        } else {
            log.error("Expected to be able to cast the question as a ChoiceQuestion " + "but this cast failed.");
            throw new ClassCastException("Incorrect type of question received. Unable to validate.");
        }
    }
}