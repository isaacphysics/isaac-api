/*
 * Copyright 2017 James Sharkey
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

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

import com.google.api.client.util.Lists;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.StringChoice;

/**
 * Test class for the String Match Validator class.
 *
 */
@PowerMockIgnore({"javax.ws.*"})
public class IsaacStringMatchValidatorTest {
    private IsaacStringMatchValidator validator;

    /**
     * Initial configuration of tests.
     *
     */
    @Before
    public final void setUp() {
        validator = new IsaacStringMatchValidator();
    }

    /*
        Test that the "did not provide an answer" response is returned for empty input.
     */
    @Test
    public final void isaacStringMatchValidator_EmptyValue_InvalidResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        String explanationShouldContain = "did not provide an answer";

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains(explanationShouldContain));
    }

    /*
       Test that the default behavior for StringChoice is case-sensitive matching.
    */
    @Test
    public final void isaacStringMatchValidator_LowerCaseValueDefaultChoice_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
    }

    /*
       Test that correct answers are recognised (case-sensitive default).
    */
    @Test
    public final void isaacStringMatchValidator_CorrectValueDefaultChoice_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("Testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
        Test that correct answers are recognised (when case-insensitive allowed).
    */
    @Test
    public final void isaacStringMatchValidator_CorrectValueCaseInsensitive_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("Testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that case-insensitive correct answers are recognised (when case-insensitive allowed).
    */
    @Test
    public final void isaacStringMatchValidator_LowercaseCorrectValueCaseInsensitive_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that incorrect case-sensitive match takes priority over case-insensitive correct match.
    */
    @Test
    public final void isaacStringMatchValidator_CaseSensitivePriority_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("testing123");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        StringChoice someIncorrectAnswer = new StringChoice();
        someIncorrectAnswer.setValue("TESTing123");
        someIncorrectAnswer.setCorrect(false);
        someIncorrectAnswer.setCaseInsensitive(false);
        answerList.add(someIncorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches both correct and incorrect but incorrect answer more strongly:
        StringChoice c = new StringChoice();
        c.setValue("TESTing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
    }

    /*
       Test that correct case-sensitive match takes priority over case-insensitive correct match.
    */
    @Test
    public final void isaacStringMatchValidator_CaseSensitivePriority_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("testing123");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        someCorrectAnswer.setExplanation(new Content("Some other explanation"));
        answerList.add(someCorrectAnswer);

        String someExplanation = "Some explanation";
        StringChoice betterCorrectAnswer = new StringChoice();
        betterCorrectAnswer.setValue("TESTing123");
        betterCorrectAnswer.setCorrect(true);
        betterCorrectAnswer.setCaseInsensitive(false);
        betterCorrectAnswer.setExplanation(new Content(someExplanation));
        answerList.add(betterCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches both correct and incorrect but incorrect answer more strongly:
        StringChoice c = new StringChoice();
        c.setValue("TESTing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains(someExplanation));
    }

    /*
       Test that case-insensitive correct matches take priority over case-insensitive incorrect matches.
    */
    @Test
    public final void isaacStringMatchValidator_CaseInsensitivePriorityCorrect_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someIncorrectAnswer = new StringChoice();
        someIncorrectAnswer.setValue("TESTing123");
        someIncorrectAnswer.setCorrect(false);
        someIncorrectAnswer.setCaseInsensitive(true);
        answerList.add(someIncorrectAnswer);

        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("Testing123");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches both correct and incorrect but correct answer more strongly:
        StringChoice c = new StringChoice();
        c.setValue("testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }
}
