/*
 * Copyright 2024 James Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.api.client.util.Lists;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCoordinateQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.CoordinateChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.CoordinateItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IsaacCoordinateValidatorTest {

    private IsaacCoordinateValidator validator;
    private IsaacCoordinateQuestion someCoordinateQuestion;

    private final CoordinateItem item1 = new CoordinateItem(List.of("1.0", "2.0"));
    private final CoordinateItem item2 = new CoordinateItem(List.of("2.0", "1.0"));
    private final CoordinateItem item3 = new CoordinateItem(List.of("1.0", "3.0"));
    private final CoordinateItem item4 = new CoordinateItem(List.of("3.0", "1.0"));
    private final CoordinateItem item2Again = new CoordinateItem(List.of("2.0", "1.0"));  // Ensure no == comparisons.

    private final CoordinateItem item1ExtraSigFig = new CoordinateItem(List.of("1.00", "2.00"));
    private final CoordinateItem item1TooFewSigFigs = new CoordinateItem(List.of("1", "2"));

    private final Content someIncorrectExplanation = new Content("Some incorrect explanation.");

    /**
     * Initial configuration of tests.
     */
    @BeforeEach
    public final void setUp() {
        validator = new IsaacCoordinateValidator();

        // Set up the question objects:
        someCoordinateQuestion = new IsaacCoordinateQuestion();
        someCoordinateQuestion.setNumberOfDimensions(2);
        someCoordinateQuestion.setSignificantFiguresMin(2);
        someCoordinateQuestion.setSignificantFiguresMax(2);
        someCoordinateQuestion.setOrdered(true);

        List<Choice> answerList = Lists.newArrayList();
        ItemChoice someIncorrectChoice = new CoordinateChoice();
        ItemChoice someCorrectChoice = new CoordinateChoice();

        // Correct and incorrect choices:
        someCorrectChoice.setItems(List.of(item1, item2));
        someCorrectChoice.setCorrect(true);
        someIncorrectChoice.setItems(ImmutableList.of(item3, item1));
        someIncorrectChoice.setCorrect(false);
        someIncorrectChoice.setExplanation(someIncorrectExplanation);
        someIncorrectChoice.setAllowSubsetMatch(true);

        // Add both choices to question, incorrect first:
        answerList.add(someIncorrectChoice);
        answerList.add(someCorrectChoice);
        someCoordinateQuestion.setChoices(answerList);
    }

    @Test
    public final void isaacCoordinateValidator_TestCorrectAnswer() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1, item2Again));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertTrue(response.isCorrect());
    }

    @Test
    public final void isaacCoordinateValidator_TestCompletelyIncorrectAnswer() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item3, item2));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    @Test
    public final void isaacCoordinateValidator_TestWrongOrderAnswer() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item2, item1));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());
    }

    @Test
    public final void isaacCoordinateValidator_TestKnownWrongAnswer() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item3, item1));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertEquals(someIncorrectExplanation, response.getExplanation());
    }

    @Test
    public final void isaacCoordinateValidator_TestMismatchedNumberOfCoordinates_ExpectNoExplanation() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1, item3, item4));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertNull(response.getExplanation());  // If the number is not set, there is no feedback to the user.
    }

    @Test
    public final void isaacCoordinateValidator_TestWrongNumberOfCoordinatesForQuestion_ExpectFeedback() {
        IsaacCoordinateQuestion coordinateQuestion = new IsaacCoordinateQuestion();
        coordinateQuestion.setNumberOfDimensions(2);
        coordinateQuestion.setNumberOfCoordinates(1);  // If this is set, expect feedback on mismatch.
        coordinateQuestion.setSignificantFiguresMin(1);

        CoordinateChoice correctChoice = new CoordinateChoice();
        correctChoice.setItems(List.of(item1));
        correctChoice.setCorrect(true);
        coordinateQuestion.setChoices(List.of(correctChoice));

        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1, item2));
        QuestionValidationResponse response = validator.validateQuestionResponse(coordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("did not provide the correct number of coordinates"));
    }

    @Test
    public final void isaacCoordinateValidator_TestWrongNumberOfDimensions() {
        CoordinateItem ci = new CoordinateItem(List.of("1"));  // 1D not 2D
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(ci, ci));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("did not provide the expected number of dimensions"));
    }

    @Test
    public final void isaacCoordinateValidator_TestTooManySignificantFigures() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1ExtraSigFig, item2));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response.getExplanation().getTags().contains("sig_figs_too_many"));
    }

    @Test
    public final void isaacCoordinateValidator_TestTooFewSignificantFigures() {
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1TooFewSigFigs, item2));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response.getExplanation().getTags().contains("sig_figs_too_few"));
    }

    @Test
    public final void isaacCoordinateValidator_TestDefaultSigFigsMax() {
        // If max sig figs is not set, it should default to 2
        someCoordinateQuestion.setSignificantFiguresMax(null);

        // 3 sig figs should be too many
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1ExtraSigFig, item2));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response.getExplanation().getTags().contains("sig_figs_too_many"));

        // 2 sig figs should be correct
        c.setItems(List.of(item1, item2));
        response = validator.validateQuestionResponse(someCoordinateQuestion, c);
        assertTrue(response.isCorrect());
    }

    @Test
    public final void isaacCoordinateValidator_TestDefaultSigFigsMin() {
        // If min sig figs is not set, it should default to 2
        someCoordinateQuestion.setSignificantFiguresMin(null);

        // 1 sig fig should be too few
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1TooFewSigFigs, item2));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response.getExplanation().getTags().contains("sig_figs_too_few"));

        // 2 sig figs should be correct
        c.setItems(List.of(item1, item2));
        response = validator.validateQuestionResponse(someCoordinateQuestion, c);
        assertTrue(response.isCorrect());
    }

    @Test
    public final void isaacCoordinateValidator_TestSubsetOfCorrectChoice() {
        someCoordinateQuestion.setOrdered(false);
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getValue().contains("but can you find more?"));
    }

    @Test
    public final void isaacCoordinateValidator_TestSupersetOfSubsetMatchChoice() {
        someCoordinateQuestion.setOrdered(false);
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1, item3, item4));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertEquals(someIncorrectExplanation, response.getExplanation());
    }

    @Test
    public final void isaacCoordinateValidator_TestSubsetOfCorrectChoiceTooManySigFigs() {
        someCoordinateQuestion.setOrdered(false);
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1ExtraSigFig));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response.getExplanation().getTags().contains("sig_figs_too_many"));
    }

    @Test
    public final void isaacCoordinateValidator_TestSupersetOfSubsetMatchChoiceTooManySigFigs() {
        someCoordinateQuestion.setOrdered(false);
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1ExtraSigFig, item3, item4));

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("sig_figs"));
        assertTrue(response.getExplanation().getTags().contains("sig_figs_too_many"));
    }

    @Test
    public final void isaacCoordinateValidator_TestDefaultFeedback() {
        // If default feedback is set, prefer it over "too few sig figs"
        CoordinateChoice c = new CoordinateChoice();
        c.setItems(List.of(item1TooFewSigFigs, item2));

        Content defaultExplanation = new Content("Default feedback.");
        someCoordinateQuestion.setDefaultFeedback(defaultExplanation);

        QuestionValidationResponse response = validator.validateQuestionResponse(someCoordinateQuestion, c);

        assertFalse(response.isCorrect());
        assertEquals(defaultExplanation, response.getExplanation());
    }

    // Test the internals of the item-ordering:

    @Test
    public final void isaacCoordinateValidator_TestCoordinateNumericOrdering() {
        CoordinateItem itemA = new CoordinateItem(List.of("1", "2", "0"));
        CoordinateItem itemB = new CoordinateItem(List.of("1", "2", "3"));
        CoordinateItem itemC = new CoordinateItem(List.of("1", "2", "5"));
        CoordinateItem itemD = new CoordinateItem(List.of("010", "02", "03"));

        List<CoordinateItem> unsorted = List.of(itemC, itemB, itemD, itemA);
        List<CoordinateItem> sorted = List.of(itemA, itemB, itemC, itemD);
        List<CoordinateItem> test = validator.orderCoordinates(unsorted);
        assertEquals(sorted, test);
    }

    @Test
    public final void isaacCoordinateValidator_TestCoordinateSigFigOrdering() {
        CoordinateItem itemA = new CoordinateItem(List.of("1.00", "2.000", "0"));
        CoordinateItem itemB = new CoordinateItem(List.of("1.00000000", "2", "3"));
        CoordinateItem itemC = new CoordinateItem(List.of("1", "2.000000", "5"));

        List<CoordinateItem> unsorted = List.of(itemC, itemB, itemA);
        List<CoordinateItem> sorted = List.of(itemA, itemB, itemC);
        List<CoordinateItem> test = validator.orderCoordinates(unsorted);
        assertEquals(sorted, test);
    }

    @Test
    public final void isaacCoordinateValidator_TestCoordinateNegativeOrdering() {
        CoordinateItem itemA = new CoordinateItem(List.of("-1", "-2"));
        CoordinateItem itemB = new CoordinateItem(List.of("-1", "2"));
        CoordinateItem itemC = new CoordinateItem(List.of("1", "-2"));
        CoordinateItem itemD = new CoordinateItem(List.of("1", "2"));

        List<CoordinateItem> unsorted = List.of(itemC, itemD, itemB, itemA);
        List<CoordinateItem> sorted = List.of(itemA, itemB, itemC, itemD);
        List<CoordinateItem> test = validator.orderCoordinates(unsorted);
        assertEquals(sorted, test);
    }
}
