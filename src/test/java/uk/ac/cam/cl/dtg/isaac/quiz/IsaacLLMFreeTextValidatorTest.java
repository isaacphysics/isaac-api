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
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingConstant;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;

/**
 * Test class for the user manager class.
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({OpenAIClient.class, ChatCompletions.class, ChatChoice.class, ChatResponseMessage.class})
@PowerMockIgnore({"jakarta.ws.*"})
public class IsaacLLMFreeTextValidatorTest {
    private IsaacLLMFreeTextValidator validator;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionOneMark;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionTwoMarks;

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
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml," +
                        "src/test/resources/segue-unit-test-llm-validator-override.yaml"
        );

        // Create mock objects for the open AI client and each layer of the response it provides
        // These must be PowerMocked since the classes are final in the Azure OpenAI library
        client = createMock(OpenAIClient.class);
        chatCompletions = createMock(ChatCompletions.class);
        chatChoice = createMock(ChatChoice.class);
        chatResponseMessage = createMock(ChatResponseMessage.class);

        validator = new IsaacLLMFreeTextValidator(propertiesForTest, client);

        // Set up a question object with a default marking formula worth one mark:
        JSONArray jsonMarkScheme = new JSONArray()
                .put(new JSONObject().put("jsonField", "reasonFoo").put("shortDescription", "Foo reason").put("marks", 1))
                .put(new JSONObject().put("jsonField", "reasonBar").put("shortDescription", "Bar reason").put("marks", 1))
                .put(new JSONObject().put("jsonField", "reasonFizz").put("shortDescription", "Fizz reason").put("marks", 1));
        List<LLMFreeTextMarkSchemeEntry> markScheme = generateMarkScheme(jsonMarkScheme);

        JSONArray jsonMarkedExamplesOneMark = new JSONArray()
                .put(new JSONObject().put("answer", "Foo and Bar").put("marksAwarded", 1).put("marks", new JSONObject()
                        .put("reasonFoo", 1).put("reasonBar", 1).put("reasonFizz", 0)))
                .put(new JSONObject().put("answer", "Fizz").put("marksAwarded", 1).put("marks", new JSONObject()
                        .put("reasonFoo", 0).put("reasonBar", 0).put("reasonFizz", 1)));
        List<LLMFreeTextMarkedExample> markedExamplesOneMark = generateMarkedExamples(jsonMarkedExamplesOneMark);

        llmFreeTextQuestionOneMark = generateLLMFreeTextQuestion(markScheme, markedExamplesOneMark, 1, null);

        // Set up a question object with a default marking formula worth two marks:
        JSONArray jsonMarkedExamplesTwoMarks = new JSONArray()
                .put(new JSONObject().put("answer", "Foo and Bar").put("marksAwarded", 2).put("marks", new JSONObject()
                        .put("reasonFoo", 1).put("reasonBar", 1).put("reasonFizz", 0)))
                .put(new JSONObject().put("answer", "Fizz").put("marksAwarded", 1).put("marks", new JSONObject()
                        .put("reasonFoo", 0).put("reasonBar", 0).put("reasonFizz", 1)));
        List<LLMFreeTextMarkedExample> markedExamplesTwoMarks = generateMarkedExamples(jsonMarkedExamplesTwoMarks);

        llmFreeTextQuestionTwoMarks = generateLLMFreeTextQuestion(markScheme, markedExamplesTwoMarks, 2,   null);
    }

    /*
        Test that a correct one-mark answer for a one-mark question gets recognised as correct
     */
    @Test
    public final void isaacLLMFreeTextValidator_CorrectOneMarkAnswer_MarkSchemeShouldIncludeMark() {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo");

        // Set up mocked OpenAI response to the user answer
        setUpMockResponse("{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}");

        // Test response
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
        assertEquals((long) response.getMarksAwarded(), 1);
    }

    /*
        Test that a correct two-mark answer for a two-mark question gets recognised as correct
    */
    @Test
    public final void isaacLLMFreeTextValidator_CorrectTwoMarkAnswer_MarkSchemeShouldIncludeMarks() {
        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();
        c.setValue("Foo Bar");

        // Set up mocked OpenAI response to the user answer
        setUpMockResponse("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 0}");

        // Test response
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
        assertEquals((long) response.getMarksAwarded(), 2);
    }

    // Helper Functions

    public IsaacLLMFreeTextQuestion generateLLMFreeTextQuestion(List<LLMFreeTextMarkSchemeEntry> markScheme,
                                                                List<LLMFreeTextMarkedExample> markedExamples, Integer maxMarks,
                                                                LLMMarkingExpression markingFormula) {
        IsaacLLMFreeTextQuestion question = new IsaacLLMFreeTextQuestion();
        question.setMarkScheme(markScheme);
        question.setMarkedExamples(markedExamples);
        question.setMaxMarks(maxMarks);
        question.setMarkingFormula(markingFormula);
        return question;
    }

    public List<LLMFreeTextMarkSchemeEntry> generateMarkScheme(JSONArray jsonMarkScheme) {
        List<LLMFreeTextMarkSchemeEntry> markScheme = new LinkedList<>();

        for (int i = 0; i < jsonMarkScheme.length(); i++) {
            JSONObject jsonMarkSchemeEntry = jsonMarkScheme.getJSONObject(i);
            LLMFreeTextMarkSchemeEntry markSchemeEntry = new LLMFreeTextMarkSchemeEntry();
            markSchemeEntry.setJsonField(jsonMarkSchemeEntry.getString("jsonField"));
            //  markSchemeEntry.setShortDescription(jsonMarkSchemeEntry.getString("shortDescription"));
            markSchemeEntry.setMarks(jsonMarkSchemeEntry.getInt("marks"));
            markScheme.add(markSchemeEntry);
        }

        return markScheme;
    }

    public List<LLMFreeTextMarkedExample> generateMarkedExamples(JSONArray jsonMarkedExamples) {
        List<LLMFreeTextMarkedExample> markedExamples = new LinkedList<>();

        for (int i = 0; i < jsonMarkedExamples.length(); i++) {
            // Extract "answer" and "marksAwarded" fields from json to LLMFreeTextMarkedExample class
            JSONObject jsonMarkedExample = jsonMarkedExamples.getJSONObject(i);
            LLMFreeTextMarkedExample example = new LLMFreeTextMarkedExample();
            example.setAnswer(jsonMarkedExample.getString("answer"));
            example.setMarksAwarded(jsonMarkedExample.getInt("marksAwarded"));

            // Extract each of the individual json fields under "marks" to LLMFreeTextMarkedExample class
            JSONObject jsonMarkedExampleMarks = jsonMarkedExample.getJSONObject("marks");
            HashMap<String, Integer> marks = new HashMap<>();
            jsonMarkedExampleMarks.keys().forEachRemaining(mark -> {
                marks.put(mark, jsonMarkedExampleMarks.getInt(mark));
            });
            example.setMarks(marks);

            // Add current example to list of marked examples
            markedExamples.add(example);
        }

        return markedExamples;
    }

    public final void setUpMockResponse(String llmResponse) {
        // Mock each layer of the response generated by the client's model
        EasyMock.expect(chatResponseMessage.getContent() ).andReturn(llmResponse).anyTimes();
        EasyMock.expect(chatChoice.getMessage() ).andReturn(chatResponseMessage).anyTimes();
        EasyMock.expect(chatCompletions.getChoices() ).andReturn(Collections.singletonList(chatChoice)).anyTimes();
        EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class))).andReturn(chatCompletions).anyTimes();

        // Set all mocked objects into replay mode
        replayAll();
    }

    //  --- LLMMarkingElement Syntax Sugar ---

    private static LLMMarkingFunction markingFormulaFunction(String name, List<LLMMarkingExpression> args) {
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

    private static LLMMarkingConstant markingFormulaConstant(Integer value) {
        LLMMarkingConstant constant = new LLMMarkingConstant();
        constant.setValue(value);
        return constant;
    }

    private static LLMMarkingVariable markingFormulaVariable(String name) {
        LLMMarkingVariable variable = new LLMMarkingVariable();
        variable.setName(name);
        return variable;
    }

    //  --- Useful Example Marking Formulas ---

    // advantageDisadvantage = SUM(MAX(... All Advantage Marks ...), MAX(... All Disadvantage Marks ...))
    private static LLMMarkingFunction advantageDisadvantageMarkingFormula(List<LLMFreeTextMarkSchemeEntry> markScheme) {
        List<LLMMarkingExpression> markingVariablesDisadvantage = new LinkedList<>();
        List<LLMMarkingExpression> markingVariablesAdvantage = new LinkedList<>();
        for (LLMFreeTextMarkSchemeEntry mark : markScheme) {
            String variableName = mark.getJsonField();
            if (variableName.contains("disadvantage")) {
                markingVariablesDisadvantage.add(markingFormulaVariable(variableName));
            } else if (variableName.contains("advantage")) {
                markingVariablesAdvantage.add(markingFormulaVariable(variableName));
            }
        }

        return markingFormulaFunction("SUM",
                Arrays.asList(
                        markingFormulaFunction("MAX",
                                markingVariablesAdvantage
                        ),
                        markingFormulaFunction("SUM",
                                markingVariablesDisadvantage
                        )
                )
        );
    }

    // pointExplanation = SUM(MAX(pointOne, pointTwo, ... pointN), MAX(MIN(pointOne, explanationOne), MIN(pointTwo, explanationTwo), ... MIN(pointN, explanationN))
    private static LLMMarkingFunction pointExplanationMarkingFormula(List<LLMFreeTextMarkSchemeEntry> markScheme) {
        return markingFormulaFunction("SUM",
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
                                                        markingFormulaVariable("pointOne"),
                                                        markingFormulaVariable("explanationTwo")
                                                )
                                        )
                                )
                        )
                )
        );
    }
}
