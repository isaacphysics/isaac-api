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
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
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
import java.util.Map;
import java.util.Objects;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replay;

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
    private AbstractConfigLoader propertiesForTest;
    private OpenAIClient client;
    private ChatCompletions chatCompletions;
    private ChatChoice chatChoice;
    private ChatResponseMessage chatResponseMessage;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    public IsaacLLMFreeTextQuestion gosh(List<LLMFreeTextMarkSchemeEntry> markScheme, Integer maxMarks,
                                         String additionalMarkingInstructions, List<LLMFreeTextMarkedExample> markedExamples,
                                         LLMMarkingExpression markingFormula) {
        IsaacLLMFreeTextQuestion a = new IsaacLLMFreeTextQuestion();
        a.setMarkScheme(markScheme);
        a.setMaxMarks(maxMarks);
        a.setAdditionalMarkingInstructions(additionalMarkingInstructions);
        a.setMarkedExamples(markedExamples);
        a.setMarkingFormula(markingFormula);
        return a;
    }

    /**
     * Initial configuration of tests.
     *
     */
    @Before
    public final void setUp() throws Exception {
        AbstractConfigLoader propertiesForTest = new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml," +
                        "src/test/resources/segue-integration-test-teacher-override.yaml"
        );

        client = createMock(OpenAIClient.class);
        chatCompletions = createMock(ChatCompletions.class);
        chatChoice = createMock(ChatChoice.class);
        chatResponseMessage = createMock(ChatResponseMessage.class);

        validator = new IsaacLLMFreeTextValidator(propertiesForTest, client);

        // Set up a question object worth one mark:
        //llmFreeTextQuestionOneMark = new IsaacLLMFreeTextQuestion();
        //llmFreeTextQuestionOneMark.setMaxMarks(1);
        LLMFreeTextMarkSchemeEntry markSchemeEntry = new LLMFreeTextMarkSchemeEntry();
        markSchemeEntry.setJsonField("pointComputers");
        markSchemeEntry.setShortDescription("Computers are cool :)");
        markSchemeEntry.setMarks(1);
        List<LLMFreeTextMarkSchemeEntry> markScheme = Arrays.asList(markSchemeEntry);
        //llmFreeTextQuestionOneMark.setMarkScheme(markScheme);
        //llmFreeTextQuestionOneMark.setMarkingFormula(defaultMarkingFormula(llmFreeTextQuestionOneMark.getMarkScheme()));
        LLMFreeTextMarkedExample markedExample = new LLMFreeTextMarkedExample();
        markedExample.setAnswer("I like computing");
        HashMap<String, Integer> capitalCities = new HashMap<String, Integer>();
        capitalCities.put("pointComputers", 1);
        markedExample.setMarks(capitalCities);
        markedExample.setMarksAwarded(1);
        List<LLMFreeTextMarkedExample> markedExamples = Arrays.asList(markedExample);
        //llmFreeTextQuestionOneMark.setMarkedExamples(markedExamples);

        IsaacLLMFreeTextQuestion b = gosh(markScheme, 1, "", markedExamples, defaultMarkingFormula(markScheme));

        /* YOU ARE HERE
            private String promptInstructionOverride; DONE
            private List<LLMFreeTextMarkSchemeEntry> markScheme; DONE
            private Integer maxMarks; DONE
            private String additionalMarkingInstructions; NO
            private String markCalculationInstructions; NO
            private List<LLMFreeTextMarkedExample> markedExamples; On it!
            private LLMMarkingExpression markingFormula; DONE
            private String markingFormulaString; Useful?
         */

        // Set up a question object worth two marks:
        llmFreeTextQuestionTwoMarks = new IsaacLLMFreeTextQuestion();
        llmFreeTextQuestionTwoMarks.setMaxMarks(2);
        llmFreeTextQuestionTwoMarks.setMarkingFormula(defaultMarkingFormula(llmFreeTextQuestionOneMark.getMarkScheme()));
    }


    /*
        Test that a correct one-mark answer for a one-mark question gets recognised as correct
     */

    @Test
    public final void isaacLLMFreeTextValidator_CorrectOneMarkAnswer_MarkSchemeShouldIncludeMark() {

        // Set up user answer:
        LLMFreeTextChoice c = new LLMFreeTextChoice();

        c.setValue("I like computers");

        String llmResponse = "{\"pointComputers\": 1, \"}";

        EasyMock.expect(chatResponseMessage.getContent() ).andReturn(llmResponse);
        EasyMock.expect(chatChoice.getMessage() ).andReturn(chatResponseMessage);
        EasyMock.expect(chatCompletions.getChoices() ).andReturn(Collections.singletonList(chatChoice));
        EasyMock.expect(chatCompletions.getChoices() ).andReturn(Collections.singletonList(chatChoice));
        EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class))).andReturn(chatCompletions);

        // PowerMock.replay(..) must be used.
        replay(chatResponseMessage);
        replay(chatChoice);
        replay(chatCompletions);
        replay(client);

        QuestionValidationResponse response = validator.validateQuestionResponse(llmFreeTextQuestionOneMark, c);
        System.out.println(response);
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

    // default = MIN(maxMarks, SUM(... All Marks ...))
    private static LLMMarkingFunction defaultMarkingFormula(List<LLMFreeTextMarkSchemeEntry> markScheme) {
        List<LLMMarkingExpression> markingVariables = new LinkedList<>();
        for (LLMFreeTextMarkSchemeEntry mark : markScheme) {
            markingVariables.add(markingFormulaVariable(mark.getJsonField()));
        }

        return markingFormulaFunction("MIN",
                Arrays.asList(
                        markingFormulaVariable("maxMarks"),
                        markingFormulaFunction("SUM",
                                markingVariables
                        )
                )
        );
    }

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
