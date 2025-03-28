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
import com.google.api.client.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuickQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingConstant;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.util.AbstractConfigLoader;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for the user manager class.
 *
 */
@PowerMockIgnore({"jakarta.ws.*"})
public class IsaacLLMFreeTextValidatorTest {
    private IsaacLLMFreeTextValidator validator;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionOneMark;
    private IsaacLLMFreeTextQuestion llmFreeTextQuestionTwoMarks;
    private AbstractConfigLoader propertiesForTest;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
        OpenAIClientBuilder builder = new OpenAIClientBuilder();
        OpenAIClient client = builder.buildClient();
        validator = new IsaacLLMFreeTextValidator(propertiesForTest, client);

        // Set up a question object worth one mark:
        llmFreeTextQuestionOneMark = new IsaacLLMFreeTextQuestion();
        llmFreeTextQuestionOneMark.setMaxMarks(1);
        llmFreeTextQuestionOneMark.setMarkingFormula(defaultMarkingFormula());

        // Set up a question object worth two marks:
        llmFreeTextQuestionTwoMarks = new IsaacLLMFreeTextQuestion();
        llmFreeTextQuestionTwoMarks.setMaxMarks(2);
        llmFreeTextQuestionTwoMarks.setMarkingFormula(defaultMarkingFormula());
    }


    /*
        Test that the "not a valid number" response is returned for non-numeric input.

    @Test
    public final void isaacNumericValidator_NonNumericValue_InvalidResponseShouldBeReturned() {
        // Set up user answer:
        Quantity q = new Quantity("NOT_A_NUMBER");

        // Test response:
        QuestionValidationResponse response = validator.validateQuestionResponse(numericQuestionNoUnits, q);
        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().getTags().contains("unrecognised_format"));
    }

    /*
        Test a correct integer answer with correct units gets recognised as correct.

    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithCorrectUnits_CorrectResponseShouldHappen() {
        // Set up user answer:
        Quantity q = new Quantity(correctIntegerAnswer, correctUnits);

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);
        assertTrue(response.isCorrect());
        assertTrue(response.getCorrectUnits());
    }

    /*
        Test an incorrect integer answer with correct units gets recognised as incorrect, but with correct units.

    @Test
    public final void isaacNumericValidator_CheckCorrectAnswerWithIncorrectValue_IncorrectResponseShouldHappen() {
        // Set up user answer:
        Quantity q = new Quantity("43", correctUnits);

        // Test response:
        QuantityValidationResponse response = (QuantityValidationResponse) validator.validateQuestionResponse(numericQuestionWithUnits, q);

        // Check answer is wrong,
        assertFalse(response.isCorrect());
        assertFalse(response.getCorrectValue());
        // but units are right:
        assertTrue(response.getCorrectUnits());
    }

    */


    //  ---------- Helper methods to test internal functionality of the validator class ----------

    private LLMMarkingFunction markingFormulaFunction(String name, List<LLMMarkingExpression> args) {
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

    private LLMMarkingConstant markingFormulaConstant(Integer value) {
        LLMMarkingConstant constant = new LLMMarkingConstant();
        constant.setValue(value);
        return constant;
    }

    private LLMMarkingVariable markingFormulaVariable(String name) {
        LLMMarkingVariable variable = new LLMMarkingVariable();
        variable.setName(name);
        return variable;
    }

    private LLMMarkingFunction defaultMarkingFormula() {
        // TODO get marking variables from markscheme
        return markingFormulaFunction("MIN",
                Arrays.asList(
                        markingFormulaVariable("maxMarks"),
                        markingFormulaFunction("SUM",
                                Arrays.asList(
                                        markingFormulaVariable("x"),
                                        markingFormulaVariable("y"),
                                        markingFormulaVariable("z")
                                ))
                )
        );
    }
}
