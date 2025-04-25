/*
 * Copyright 2014 Stephen Cummins, 2019 James Sharkey, 2025 Sol Dubock
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatResponseMessage;
import org.easymock.EasyMock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.LLMFreeTextQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

/**
 * Test class for the user manager class.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenAIClient.class, ChatCompletions.class, ChatChoice.class, ChatResponseMessage.class})
@PowerMockIgnore({"jakarta.ws.*"})
public class IsaacLLMFreeTextValidatorTest {
    private AbstractConfigLoader propertiesForTest;
    private IsaacLLMFreeTextValidator validator;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionOneMark;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionTwoMarks;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionAdvantageDisadvantage;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionPointExplanation;

    // Mocks
    private OpenAIClient client;
    private ChatCompletions chatCompletions;
    private ChatChoice chatChoice;
    private ChatResponseMessage chatResponseMessage;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    /**
     * Initial configuration of tests.
     */
    @Before
    public final void setUp() throws Exception {
        propertiesForTest = new YamlLoader(
            "src/test/resources/segue-integration-test-config.yaml,"
            + "src/test/resources/segue-unit-test-llm-validator-override.yaml"
        );

        // Create mock objects for the open AI client and each layer of the response it provides
        // These must be PowerMocked since the classes are final in the Azure OpenAI library
        client = createMock(OpenAIClient.class);
        chatCompletions = createMock(ChatCompletions.class);
        chatChoice = createMock(ChatChoice.class);
        chatResponseMessage = createMock(ChatResponseMessage.class);

        validator = new IsaacLLMFreeTextValidator(propertiesForTest, client);

        // Set up generic mark scheme with three available marks:
        JSONArray jsonMarkScheme = new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("shortDescription", "Foo reason").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonBar").put("shortDescription", "Bar reason").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("shortDescription", "Fizz reason").put("marks", 1));
        List<LLMFreeTextMarkSchemeEntry> markScheme = generateMarkScheme(jsonMarkScheme);

        // Set up a question object with a default marking formula worth one mark:
        JSONArray jsonMarkedExamplesOneMark = new JSONArray()
            .put(new JSONObject().put("answer", "Foo and Bar").put("marksAwarded", 1).put("marks", new JSONObject()
                .put("reasonFoo", 1).put("reasonBar", 1).put("reasonFizz", 0)))
            .put(new JSONObject().put("answer", "Fizz").put("marksAwarded", 1).put("marks", new JSONObject()
                .put("reasonFoo", 0).put("reasonBar", 0).put("reasonFizz", 1)));
        List<LLMFreeTextMarkedExample> markedExamplesOneMark = generateMarkedExamples(jsonMarkedExamplesOneMark);

        llmFreeTextQuestionOneMark = createLLMFreeTextQuestion(markScheme, 1, markedExamplesOneMark, null);

        // Set up a question object with a default marking formula worth two marks:
        JSONArray jsonMarkedExamplesTwoMarks = new JSONArray()
            .put(new JSONObject().put("answer", "Foo and Bar").put("marksAwarded", 2).put("marks", new JSONObject()
                .put("reasonFoo", 1).put("reasonBar", 1).put("reasonFizz", 0)))
            .put(new JSONObject().put("answer", "Fizz").put("marksAwarded", 1).put("marks", new JSONObject()
                .put("reasonFoo", 0).put("reasonBar", 0).put("reasonFizz", 1)));
        List<LLMFreeTextMarkedExample> markedExamplesTwoMarks = generateMarkedExamples(jsonMarkedExamplesTwoMarks);

        llmFreeTextQuestionTwoMarks = createLLMFreeTextQuestion(markScheme, 2, markedExamplesTwoMarks, null);

        // Set up a question object with an advantage/disadvantage marking formula worth two marks:
        JSONArray jsonMarkSchemeAdvantageDisadvantage = new JSONArray()
            .put(new JSONObject().put("jsonField", "advantageOne").put("shortDescription", "Advantage reason").put("marks", 1))
            .put(new JSONObject().put("jsonField", "advantageTwo").put("shortDescription", "Another advantage reason").put("marks", 1))
            .put(new JSONObject().put("jsonField", "disadvantageOne").put("shortDescription", "Disadvantage reason").put("marks", 1))
            .put(new JSONObject().put("jsonField", "disadvantageTwo").put("shortDescription", "Another disadvantage reason").put("marks", 1));
        List<LLMFreeTextMarkSchemeEntry> markSchemeAdvantageDisadvantage = generateMarkScheme(jsonMarkSchemeAdvantageDisadvantage);

        JSONArray jsonMarkedExamplesAdvantageDisadvantage = new JSONArray()
            .put(new JSONObject().put("answer", "Advantage").put("marksAwarded", 1).put("marks", new JSONObject()
                .put("advantageOne", 1).put("advantageTwo", 0).put("disadvantageOne", 0).put("disadvantageTwo", 0)))
            .put(new JSONObject().put("answer", "Disadvantage Disadvantage").put("marksAwarded", 1).put("marks", new JSONObject()
                .put("advantageOne", 1).put("advantageTwo", 0).put("disadvantageOne", 1).put("disadvantageTwo", 1)));
        List<LLMFreeTextMarkedExample> markedExamplesAdvantageDisadvantage = generateMarkedExamples(jsonMarkedExamplesAdvantageDisadvantage);

        llmFreeTextQuestionAdvantageDisadvantage = createLLMFreeTextQuestion(markSchemeAdvantageDisadvantage, 2, markedExamplesAdvantageDisadvantage, advantageDisadvantageMarkingFormula);

        // Set up a question object with a point/explanation marking formula worth two marks:
        JSONArray jsonMarkSchemePointExplanation = new JSONArray()
            .put(new JSONObject().put("jsonField", "pointOne").put("shortDescription", "First point").put("marks", 1))
            .put(new JSONObject().put("jsonField", "pointTwo").put("shortDescription", "Second point").put("marks", 1))
            .put(new JSONObject().put("jsonField", "explanationOne").put("shortDescription", "Explaining first point").put("marks", 1))
            .put(new JSONObject().put("jsonField", "explanationTwo").put("shortDescription", "Explaining second point").put("marks", 1));
        List<LLMFreeTextMarkSchemeEntry> markSchemePointExplanation = generateMarkScheme(jsonMarkSchemePointExplanation);

        JSONArray jsonMarkedExamplesPointExplanation = new JSONArray()
            .put(new JSONObject().put("answer", "Explanation").put("marksAwarded", 0).put("marks", new JSONObject()
                .put("pointOne", 0).put("pointTwo", 0).put("explanationOne", 1).put("explanationTwo", 0)))
            .put(new JSONObject().put("answer", "Point2 Explanation2").put("marksAwarded", 2).put("marks", new JSONObject()
                .put("pointOne", 0).put("pointTwo", 1).put("explanationOne", 0).put("explanationTwo", 1)));
        List<LLMFreeTextMarkedExample> markedExamplesPointExplanation = generateMarkedExamples(jsonMarkedExamplesPointExplanation);

        llmFreeTextQuestionPointExplanation = createLLMFreeTextQuestion(markSchemePointExplanation, 2, markedExamplesPointExplanation, pointExplanationMarkingFormula);
    }

    /*
        Test that a one-mark answer for a default marking formula one-mark question gets recognised as correct
     */
    @SuppressWarnings("checkstyle:MethodName")
    @Test
    public final void isaacLLMFreeTextValidator_OneMarkQuestionOneMarkAnswer_MarkSchemeShouldIncludeMark() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonBar").put("marks", 0))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    /*
        Test that a three-mark answer for a default marking formula one-mark question gets recognised as correct
     */
    @Test
    public final void isaacLLMFreeTextValidator_OneMarkQuestionThreeMarkAnswer_MarkSchemeShouldIncludeMarks() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 1}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonBar").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("marks", 1))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    /*
        Test that a zero-mark answer for a one-mark question gets recognised as incorrect
    */
    @Test
    public final void isaacLLMFreeTextValidator_OneMarkQuestionZeroMarkAnswer_MarkSchemeShouldIncludeNoMarks() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Buzz");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"reasonFoo\": 0, \"reasonBar\": 0, \"reasonFizz\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "reasonBar").put("marks", 0))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertFalse(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(0, (long) response.getMarksAwarded());
    }

    /*
        Test that a two-mark answer for a default marking formula two-mark question gets recognised as correct
    */
    @Test
    public final void isaacLLMFreeTextValidator_TwoMarkQuestionTwoMarkAnswer_MarkSchemeShouldIncludeMarks() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo Bar");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionTwoMarks, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonBar").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(2, (long) response.getMarksAwarded());
    }

    /*
        Test that a one-mark answer for a default marking formula two-mark question receives exactly one mark
    */
    @Test
    public final void isaacLLMFreeTextValidator_TwoMarkQuestionOneMarkAnswer_MarkSchemeShouldIncludeMarks() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionTwoMarks, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("marks", 1))
            .put(new JSONObject().put("jsonField", "reasonBar").put("marks", 0))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    /*
        Tests that an answer containing an advantage and a disadvantage mark for a two-mark advantage/disadvantage question receives two marks
    */
    @Test
    public final void isaacLLMFreeTextValidator_AdvantageDisadvantageQuestionADMarks_MarkTotalShouldBeTwo() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Advantage Disadvantage");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"advantageOne\": 1, \"advantageTwo\": 0, \"disadvantageOne\": 1, \"disadvantageTwo\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionAdvantageDisadvantage, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "advantageOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "advantageTwo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "disadvantageOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "disadvantageTwo").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(2, (long) response.getMarksAwarded());
    }

    /*
        Tests that an answer containing only a disadvantage mark for a two-mark advantage/disadvantage question receives one mark
    */
    @Test
    public final void isaacLLMFreeTextValidator_AdvantageDisadvantageQuestionDMarks_MarkTotalShouldBeOne() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Disadvantage");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"advantageOne\": 0, \"advantageTwo\": 0, \"disadvantageOne\": 1, \"disadvantageTwo\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionAdvantageDisadvantage, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "advantageOne").put("marks", 0))
            .put(new JSONObject().put("jsonField", "advantageTwo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "disadvantageOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "disadvantageTwo").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    /*
       Tests that an answer containing two advantage marks for a two-mark advantage/disadvantage question receives one mark
    */
    @Test
    public final void isaacLLMFreeTextValidator_AdvantageDisadvantageQuestionAAMarks_MarkTotalShouldBeOne() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Advantage Advantage");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"advantageOne\": 1, \"advantageTwo\": 1, \"disadvantageOne\": 0, \"disadvantageTwo\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionAdvantageDisadvantage, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "advantageOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "advantageTwo").put("marks", 1))
            .put(new JSONObject().put("jsonField", "disadvantageOne").put("marks", 0))
            .put(new JSONObject().put("jsonField", "disadvantageTwo").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    /*
       Tests that an answer containing a point and matching explanation for a two-mark point/explanation question receives two marks
    */
    @Test
    public final void isaacLLMFreeTextValidator_PointExplanationQuestionPEMarks_MarkTotalShouldBeTwo() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Point1 Explanation1");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"pointOne\": 1, \"pointTwo\": 0, \"explanationOne\": 1, \"explanationTwo\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionPointExplanation, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "pointOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "pointTwo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "explanationOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "explanationTwo").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(2, (long) response.getMarksAwarded());
    }

    /*
        Tests that an answer containing an explanation without a matching point for a two-mark point/explanation question receives zero marks
    */
    @Test
    public final void isaacLLMFreeTextValidator_PointExplanationQuestionEMark_MarkTotalShouldBeZero() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Explanation1");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"pointOne\": 0, \"pointTwo\": 0, \"explanationOne\": 1, \"explanationTwo\": 0}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionPointExplanation, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "pointOne").put("marks", 0))
            .put(new JSONObject().put("jsonField", "pointTwo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "explanationOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "explanationTwo").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertFalse(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(0, (long) response.getMarksAwarded());
    }

    /*
        Tests that an answer containing a point and a mismatched explanation for a
        two-mark point/explanation question receives one mark
    */
    @Test
    public final void isaacLLMFreeTextValidator_PointExplanationQuestionPEMismatchMarks_MarkTotalShouldBeOne() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Point1 Explanation2");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("{\"pointOne\": 1, \"pointTwo\": 0, \"explanationOne\": 0, \"explanationTwo\": 1}");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionPointExplanation, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "pointOne").put("marks", 1))
            .put(new JSONObject().put("jsonField", "pointTwo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "explanationOne").put("marks", 0))
            .put(new JSONObject().put("jsonField", "explanationTwo").put("marks", 1))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertTrue(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    /*
        Test that a response from the client not in the expected json format returns zero marks
    */
    @Test
    public final void isaacLLMFreeTextValidator_ResponseInvalidFormat_MarkSchemeShouldIncludeNoMarks() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo Bar Fizz");

        // Set up mocked OpenAI response to the user answer:
        setUpMockResponse("Not a valid JSON response");

        // Test response:
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);

        List<LLMFreeTextMarkSchemeEntry> expectedMarks = generateMarkScheme(new JSONArray()
            .put(new JSONObject().put("jsonField", "reasonFoo").put("marks", 0))
            .put(new JSONObject().put("jsonField", "reasonBar").put("marks", 0))
            .put(new JSONObject().put("jsonField", "reasonFizz").put("marks", 0))
        );
        List<LLMFreeTextMarkSchemeEntry> awardedMarks = response.getMarkBreakdown();

        assertFalse(response.isCorrect());
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
        assertEquals(0, (long) response.getMarksAwarded());
    }

    /*
        Test that an answer exceeding the maximum answer length is handled with an exception and no other response is output
    */
    @Test
    public final void isaacLLMFreeTextValidator_AnswerOverLengthLimit_ExceptionShouldBeThrown() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        int maxAnswerLength = 4096;
        try {
            maxAnswerLength = Integer.parseInt(propertiesForTest.getProperty(LLM_MARKER_MAX_ANSWER_LENGTH));
        } catch (final NumberFormatException ignored) { /* Use default value */ }
        c.setValue(String.join("", Collections.nCopies((maxAnswerLength / 10 + 1), "Repeat Me ")));

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("Answer is too long for LLM free-text question marking");

        // Test response
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);
        assertNull(response);
    }

    /*
        Test that an error from the client (e.g. timeout, rate limit, out of credits) is handled with an exception
        and no other response is output
    */
    @Test
    public final void isaacLLMFreeTextValidator_ResponseError_ExceptionShouldBeThrown() throws Exception {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo Bar Fizz");

        // Set up mocked OpenAI exception response to the user answer
        EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class))).andThrow(new RuntimeException("Test Exception"));
        replay(client);

        expectedException.expect(ValidatorUnavailableException.class);

        // Test response
        LLMFreeTextQuestionValidationResponse response = (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);
        assertNull(response);
    }

    // --- Helper Functions ---

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * mocks given response as if generated by the OpenAI client.
     *
     * @param llmResponse      - mock message to return from client
     */
    private void setUpMockResponse(final String llmResponse) {
        // Mock each layer of the response generated by the client's model
        EasyMock.expect(chatResponseMessage.getContent()).andReturn(llmResponse);
        EasyMock.expect(chatChoice.getMessage()).andReturn(chatResponseMessage);
        EasyMock.expect(chatCompletions.getChoices()).andReturn(Collections.singletonList(chatChoice)).times(2);
        EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class))).andReturn(chatCompletions);

        // Set all mocked objects into replay mode
        replayAll();
    }

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * generates a list of marks and their values from json input.
     *
     * @param jsonMarkScheme      - a JSON array containing marks in the format {"jsonField": markName, "marks": markValue}
     * @return A list of marks with corresponding "jsonField" and "marks" fields
     */
    private List<LLMFreeTextMarkSchemeEntry> generateMarkScheme(final JSONArray jsonMarkScheme) {
        List<LLMFreeTextMarkSchemeEntry> markScheme = new LinkedList<>();

        for (int i = 0; i < jsonMarkScheme.length(); i++) {
            JSONObject jsonMarkSchemeEntry = jsonMarkScheme.getJSONObject(i);
            LLMFreeTextMarkSchemeEntry markSchemeEntry = new LLMFreeTextMarkSchemeEntry();

            // Extract "answer" and "marksAwarded" fields from json to add to the marked example
            markSchemeEntry.setJsonField(jsonMarkSchemeEntry.getString("jsonField"));
            markSchemeEntry.setMarks(jsonMarkSchemeEntry.getInt("marks"));

            markScheme.add(markSchemeEntry);
        }

        return markScheme;
    }

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * generates a list of marked example answers (consisting of a written answer and its marks) from json input.
     *
     * @param jsonMarkedExamples      - a JSON array containing examples in the format:
     *                                  {"answer": exampleAnswer, "marksAwarded: exampleValue, "marks": {...exampleMarkScheme...}}
     * @return A list of marked examples with corresponding "answer", "marksAwarded", and "marks" fields
     */
    private List<LLMFreeTextMarkedExample> generateMarkedExamples(final JSONArray jsonMarkedExamples) {
        List<LLMFreeTextMarkedExample> markedExamples = new LinkedList<>();

        for (int i = 0; i < jsonMarkedExamples.length(); i++) {
            JSONObject jsonMarkedExample = jsonMarkedExamples.getJSONObject(i);
            LLMFreeTextMarkedExample example = new LLMFreeTextMarkedExample();

            // Extract "answer" and "marksAwarded" fields from json to add to the marked example
            example.setAnswer(jsonMarkedExample.getString("answer"));
            example.setMarksAwarded(jsonMarkedExample.getInt("marksAwarded"));

            // Extract each of the individual marks from json to add to the marked example
            JSONObject jsonMarkedExampleMarks = jsonMarkedExample.getJSONObject("marks");
            HashMap<String, Integer> marks = new HashMap<>();
            jsonMarkedExampleMarks.keys().forEachRemaining(mark ->
                    marks.put(mark, jsonMarkedExampleMarks.getInt(mark)));
            example.setMarks(marks);

            // Add current example to list of marked examples
            markedExamples.add(example);
        }

        return markedExamples;
    }

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * generates an IsaacLLMFreeTextQuestion with provided marking information.
     *
     * @param markScheme              - Each available mark for the question and its corresponding value
     * @param maxMarks                - Maximum available number of marks for the question
     * @param markedExamples          - Example marked answers to the question
     * @param markingFormula          - Formula used to calculate the mark total.
     *                                  If null, defaults to MIN(maxMarks, SUM(... all marks ...))
     * @return The new IsaacLLMFreeTextQuestion
     */
    private IsaacLLMFreeTextQuestion createLLMFreeTextQuestion(final List<LLMFreeTextMarkSchemeEntry> markScheme,
                                                               final Integer maxMarks,
                                                               final List<LLMFreeTextMarkedExample> markedExamples,
                                                               final LLMMarkingExpression markingFormula) {
        IsaacLLMFreeTextQuestion question = new IsaacLLMFreeTextQuestion();
        question.setMarkScheme(markScheme);
        question.setMaxMarks(maxMarks);
        question.setMarkedExamples(markedExamples);
        question.setMarkingFormula(markingFormula);

        return question;
    }

    //  --- LLMMarkingElement Syntax Sugar ---

    private static LLMMarkingFunction markingFormulaFunction(final String name, final List<LLMMarkingExpression> args) {
        LLMMarkingFunction function = new LLMMarkingFunction();
        if (Objects.equals(name, "SUM")) {
            function.setName(LLMMarkingFunction.FunctionName.SUM);
        } else if (Objects.equals(name, "MIN")) {
            function.setName(LLMMarkingFunction.FunctionName.MIN);
        } else if (Objects.equals(name, "MAX")) {
            function.setName(LLMMarkingFunction.FunctionName.MAX);
        }
        function.setArguments(args);
        return function;
    }

    private static LLMMarkingVariable markingFormulaVariable(final String name) {
        LLMMarkingVariable variable = new LLMMarkingVariable();
        variable.setName(name);
        return variable;
    }

    //  --- Useful Example Marking Formulae ---

    /*
       - advantageDisadvantage = SUM(MAX(... All Advantage Marks ...), MAX(... All Disadvantage Marks ...))
       Labelled here and in documentation as advantage/disadvantage, although this structure can also be used
       for any two mutually exclusive categories each required to get full marks
    */
    private final LLMMarkingFunction advantageDisadvantageMarkingFormula = markingFormulaFunction("SUM",
        Arrays.asList(
            markingFormulaFunction("MAX",
                Arrays.asList(
                    markingFormulaVariable("advantageOne"),
                    markingFormulaVariable("advantageTwo")
                )
            ),
            markingFormulaFunction("MAX",
                Arrays.asList(
                    markingFormulaVariable("disadvantageOne"),
                    markingFormulaVariable("disadvantageTwo")
                )
            )
        )
    );

    /*
        - pointExplanation = SUM(MAX(pointOne, pointTwo, ... pointN), MAX(MIN(pointOne, explanationOne), MIN(pointTwo, explanationTwo), ... MIN(pointN, explanationN))
        Used for questions where a point is a prerequisite for its matching explanation
    */
    private final LLMMarkingFunction pointExplanationMarkingFormula = markingFormulaFunction("SUM",
        Arrays.asList(
            markingFormulaFunction("MAX",
                Arrays.asList(
                    markingFormulaVariable("pointOne"),
                    markingFormulaVariable("pointTwo")
                )
            ),
            markingFormulaFunction("MAX",
                Arrays.asList(
                    markingFormulaFunction("MIN",
                        Arrays.asList(
                            markingFormulaVariable("pointOne"),
                            markingFormulaVariable("explanationOne")
                        )
                    ),
                    markingFormulaFunction("MIN",
                        Arrays.asList(
                            markingFormulaVariable("pointTwo"),
                            markingFormulaVariable("explanationTwo")
                        )
                    )
                )
            )
        )
    );
}
