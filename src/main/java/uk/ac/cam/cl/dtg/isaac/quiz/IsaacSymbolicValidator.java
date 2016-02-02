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
package uk.ac.cam.cl.dtg.isaac.quiz;

import java.util.Date;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Formula;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate symbolic questions.
 *
 * @author Alistair Stead
 *
 */
public class IsaacSymbolicValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicValidator.class);

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacSymbolicQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Symbolic Questions... (%s is not symbolic)",
                    question.getId()));
        }
        
        if (!(answer instanceof Formula)) {
            throw new IllegalArgumentException(String.format(
                    "Expected Formula for IsaacSymbolicQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacSymbolicQuestion symbolicQuestion = (IsaacSymbolicQuestion) question;
        Formula submittedFormula = (Formula) answer;

        // TODO: Look through available choices on question, deciding whether the submitted answer is right.

        // For now, just say it was wrong.
        return new QuestionValidationResponse(symbolicQuestion.getId(), answer, false,
                new Content("This was wrong. Sorry."), new Date());
    }
}
