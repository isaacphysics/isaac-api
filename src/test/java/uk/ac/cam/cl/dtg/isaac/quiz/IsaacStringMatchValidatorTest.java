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

import com.google.api.client.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.StringChoice;

import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the String Match Validator class.
 *
 */
@PowerMockIgnore({"javax.ws.*"})
public class IsaacStringMatchValidatorTest {
    private IsaacStringMatchValidator validator;
    private IsaacStringMatchQuestion someStringMatchQuestion;
    private String caseSensitiveAnswer = "CaseSensitiveAnswer";
    private String caseInsensitiveAnswer = "CaseInsensitiveAnswer";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Initial configuration of tests.
     *
     */
    @Before
    public final void setUp() {
        validator = new IsaacStringMatchValidator();

        // Set up the question object:
        someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();

        StringChoice caseSensitiveChoice = new StringChoice();
        caseSensitiveChoice.setValue(caseSensitiveAnswer);
        caseSensitiveChoice.setCorrect(true);
        answerList.add(caseSensitiveChoice);

        StringChoice caseInsensitiveChoice = new StringChoice();
        caseInsensitiveChoice.setValue(caseInsensitiveAnswer);
        caseInsensitiveChoice.setCaseInsensitive(true);
        caseInsensitiveChoice.setCorrect(true);
        answerList.add(caseInsensitiveChoice);

        someStringMatchQuestion.setChoices(answerList);
    }

    /*
        Test that the "did not provide an answer" response is returned for empty input.
     */
    @Test
    public final void isaacStringMatchValidator_EmptyValue_InvalidResponseShouldBeReturned() {
        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("did not provide an answer"));
    }

    /*
       Test that the default behavior for StringChoice is case-sensitive matching.
    */
    @Test
    public final void isaacStringMatchValidator_LowerCaseValueDefaultChoice_IncorrectResponseShouldBeReturned() {
        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue(caseSensitiveAnswer.toLowerCase());

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
    }

    /*
       Test that correct answers are recognised (case-sensitive default).
    */
    @Test
    public final void isaacStringMatchValidator_CorrectValueDefaultChoice_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue(caseSensitiveAnswer);

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
        Test that correct answers are recognised (when case-insensitive allowed).
    */
    @Test
    public final void isaacStringMatchValidator_CorrectValueCaseInsensitive_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue(caseInsensitiveAnswer);

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that case-insensitive correct answers are recognised (when case-insensitive allowed).
    */
    @Test
    public final void isaacStringMatchValidator_LowercaseCorrectValueCaseInsensitive_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue(caseInsensitiveAnswer.toLowerCase());

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that the default behavior for String Match Questions trims trailing spaces.
    */
    @Test
    public final void isaacStringMatchValidator_CorrectWithTrailingSpaces_CorrectResponseShouldBeReturned() {
        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue(String.format("%s     ", caseSensitiveAnswer));

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that trailing whitespace on multiple lines is removed before matching by default.
    */
    @Test
    public final void isaacStringMatchValidator_TrailingSpacesMultiline_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("THIS\nIS\nA\nTEST\nMULTILINE\nVALUE");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches correct but with trailing spaces on each line:
        StringChoice c = new StringChoice();
        c.setValue("THIS  \nIS  \nA  \nTEST  \nMULTILINE  \nVALUE   ");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that trailing whitespace on multiple lines is preserved when flag set.
    */
    @Test
    public final void isaacStringMatchValidator_TrailingSpacesMultilinePreserved_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();
        someStringMatchQuestion.setPreserveTrailingWhitespace(true);

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("THIS\nIS\nA\nTEST\nMULTILINE\nVALUE");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches correct but with trailing spaces on each line:
        StringChoice c = new StringChoice();
        c.setValue("THIS  \nIS  \nA  \nTEST  \nMULTILINE  \nVALUE   ");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
    }

    /*
   Test that leading whitespace on multiple lines is removed before matching by default.
*/
    @Test
    public final void isaacStringMatchValidator_LeadingSpacesMultiline_CorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("THIS\nIS\nA\nTEST\nMULTILINE\nVALUE");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches correct but with trailing spaces on each line:
        StringChoice c = new StringChoice();
        c.setValue("   THIS\n    IS\n    A\n    TEST\n    MULTILINE\n    VALUE");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertTrue(response.isCorrect());
    }

    /*
       Test that leading whitespace on multiple lines is preserved when flag set.
    */
    @Test
    public final void isaacStringMatchValidator_LeadingSpacesMultilinePreserved_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();
        someStringMatchQuestion.setPreserveLeadingWhitespace(true);

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("THIS\nIS\nA\nTEST\nMULTILINE\nVALUE");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches correct but with trailing spaces on each line:
        StringChoice c = new StringChoice();
        c.setValue("    THIS\n    IS\n    A\n    TEST\n    MULTILINE\n    VALUE");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
    }

    /*
       Test that whitespace stripping does not remove newlines as well.
    */
    @Test
    public final void isaacStringMatchValidator_TrailingSpacesNoNewlines_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("THIS\nIS\nA\nTEST\nMULTILINE\nVALUE");
        someCorrectAnswer.setCorrect(true);
        someCorrectAnswer.setCaseInsensitive(true);
        answerList.add(someCorrectAnswer);

        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer, matches correct but with trailing spaces on each line:
        StringChoice c = new StringChoice();
        c.setValue("THISISATESTMULTILINEVALUE");

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


    //  ---------- Tests from here test invalid questions themselves ----------

    /*
     Test that missing choices are detected.
    */
    @Test
    public final void isaacStringMatchValidator_NoChoices_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();
        someStringMatchQuestion.setChoices(Lists.newArrayList());

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("not have any correct answers"));
    }

    /*
     Test that incorrect choice types in question are detected.
    */
    @Test
    public final void isaacStringMatchValidator_WrongChoiceType_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();

        Choice someCorrectAnswer = new Choice();
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);
        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("testing123");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that choices in questions missing values are not matched.
    */
    @Test
    public final void isaacStringMatchValidator_EmptyValueInQuestion_IncorrectResponseShouldBeReturned() {
        // Set up the question object:
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();

        List<Choice> answerList = Lists.newArrayList();
        StringChoice someCorrectAnswer = new StringChoice();
        someCorrectAnswer.setValue("");
        someCorrectAnswer.setCorrect(true);
        answerList.add(someCorrectAnswer);
        someStringMatchQuestion.setChoices(answerList);

        // Set up user answer:
        StringChoice c = new StringChoice();
        c.setValue("TEST");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(someStringMatchQuestion, c);
        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    /*
     Test that incorrect question types are detected.
    */
    @Test
    public final void isaacStringMatchValidator_WrongQuestionType_ExceptionShouldBeThrown() {
        IsaacQuickQuestion invalidQuestionType = new IsaacQuickQuestion();
        invalidQuestionType.setId("invalidQuestionType");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("only works with Isaac String Match Questions");

        // This should throw an exception:
        validator.validateQuestionResponse(invalidQuestionType, new StringChoice());
    }

    /*
     Test that incorrect submitted choice types are detected.
    */
    @Test
    public final void isaacStringMatchValidator_WrongChoiceType_ExceptionShouldBeThrown() {
        IsaacStringMatchQuestion someStringMatchQuestion = new IsaacStringMatchQuestion();
        someStringMatchQuestion.setId("invalidQuestionType");

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Expected StringChoice for IsaacStringMatchQuestion");

        // This should throw an exception:
        validator.validateQuestionResponse(someStringMatchQuestion, new Choice());
    }
}
