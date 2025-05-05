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
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import uk.ac.cam.cl.dtg.isaac.Mark;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.LLMFreeTextQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

import static uk.ac.cam.cl.dtg.isaac.QuestionFactory.*;
import static uk.ac.cam.cl.dtg.isaac.quiz.Helpers.*;

@RunWith(Enclosed.class)
public class IsaacLLMFreeTextValidatorTest {
    @RunWith(PowerMockRunner.class)
    @PowerMockRunnerDelegate(Parameterized.class)
    @PrepareForTest({ OpenAIClient.class, ChatCompletions.class, ChatChoice.class, ChatResponseMessage.class })
    @DisplayName("Test that a mark is awarded based on the marking formula")
    public static class FormulaBasedMarking {
        @Parameter(0)
        public String testDescription;
        @Parameter(1)
        public IsaacLLMFreeTextQuestion question;
        @Parameter(2)
        public Mark response;
        @Parameter(3)
        public boolean expectedResult;
        @Parameter(4)
        public int expectedMark;

        @Parameters
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][] {
                    { "A one-mark answer for a default marking formula one-mark question gets recognised as correct",
                            genericOneMarkQuestion(), mark().setReasonFoo(1), CORRECT, 1 },
                    { "A three-mark answer for a default marking formula one-mark question gets recognised as correct",
                            genericOneMarkQuestion(), mark().setReasonFoo(1).setReasonBar(1).setReasonFizz(1), CORRECT,
                            1 },
                    { "A zero-mark answer for a one-mark question gets recognised as incorrect",
                            genericOneMarkQuestion(), mark(), INCORRECT, 0 },

                    { "A two-mark answer for a default marking formula two-mark question gets recognised as correct",
                            genericTwoMarkQuestion(), mark().setReasonFoo(1).setReasonBar(1), CORRECT, 2 },
                    { "A one-mark answer for a default marking formula two-mark question receives exactly one mark",
                            genericTwoMarkQuestion(), mark().setReasonFoo(1), CORRECT, 1 },

                    { "An answer containing an advantage and a disadvantage mark for a two-mark advantage/disadvantage question receives two marks",
                            advantageQuestion(), advantageMark().setAdvantageOne(1).setDisadvantageOne(1), CORRECT, 2 },
                    { "An answer containing only a disadvantage mark for a two-mark advantage/disadvantage question receives one mark",
                            advantageQuestion(), advantageMark().setDisadvantageOne(1), CORRECT, 1 },
                    { "An answer containing two advantage marks for a two-mark advantage/disadvantage question receives one mark",
                            advantageQuestion(), advantageMark().setAdvantageOne(1).setAdvantageTwo(1), CORRECT, 1 },

                    { "An answer containing a point and matching explanation for a two-mark point/explanation question receives two marks",
                            pointExplanationQuestion(), pointMark().setPointOne(1).setExplanationOne(1), CORRECT, 2 },
                    { "An answer containing an explanation without a matching point for a two-mark point/explanation question receives zero marks",
                            pointExplanationQuestion(), pointMark().setExplanationOne(1), INCORRECT, 0 },
                    { "An answer containing a point and a mismatched explanation for a two-mark point/explanation question receives one mark",
                            pointExplanationQuestion(), pointMark().setPointOne(1).setExplanationTwo(1), CORRECT, 1 }
            });
        }

        @Test
        public void test() throws Exception {
            var resp = validate(question, response);
            expectMark(resp, expectedResult, expectedMark, response);
        }
    }

    @RunWith(PowerMockRunner.class)
    @PrepareForTest({ OpenAIClient.class, ChatCompletions.class, ChatChoice.class, ChatResponseMessage.class })
    @DisplayName("Test application behaviour in case of errors.")
    public static class ErrorHandling {
        @Test
        @DisplayName("A response from the client not in the expected json format returns zero marks")
        public void isaacLLMFreeTextValidator_ResponseInvalidFormat_MarkSchemeShouldIncludeNoMarks() throws Exception {
            var response = validate(genericOneMarkQuestion(), "Not a valid JSON response");
            expectMark(response, INCORRECT, 0, mark());
        }

        @Test
        @DisplayName("An answer exceeding the maximum answer length is handled with an exception")
        public void isaacLLMFreeTextValidator_AnswerOverLengthLimit_ExceptionShouldBeThrown() throws Exception {
            var maxAnswerLength = getIntTestProperty(LLM_MARKER_MAX_ANSWER_LENGTH, 4096);
            var answr = answer(String.join("", Collections.nCopies((maxAnswerLength / 10 + 1), "Repeat Me ")));

            var exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> createValidator(client()).validateQuestionResponse(genericOneMarkQuestion(), answr));

            assertEquals("Answer is too long for LLM free-text question marking", exception.getMessage());
        }

        @Test
        @DisplayName("Error from the client (e.g. timeout, rate limit, out of credits) is handled with an exception")
        public void isaacLLMFreeTextValidator_ResponseError_ExceptionShouldBeThrown() {
            var clnt = createMock(OpenAIClient.class);
            EasyMock.expect(clnt.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class)))
                    .andThrow(new RuntimeException("Test OpenAI Exception"));
            replay(clnt);

            assertEquals("We are having problems marking LLM marked questions. Please try again later!",
                    assertThrows(
                            ValidatorUnavailableException.class,
                            () -> createValidator(clnt).validateQuestionResponse(genericOneMarkQuestion(), answer("")))
                            .getMessage());
        }

        @Test
        @DisplayName("Invalid question (i.e. missing maxMarks field/not LLMFreeTextQuestion) is handled with an exception")
        public void isaacLLMFreeTextValidator_InvalidQuestion_ExceptionShouldBeThrown() {
            IsaacLLMFreeTextQuestion invalidQuestionFields = createLLMFreeTextQuestion(null, null, null, null);
            Question invalidQuestionType = new Question();

            assertEquals("This question cannot be answered correctly",
                    assertThrows(IllegalArgumentException.class, () -> validate(invalidQuestionFields, ""))
                            .getMessage());

            assertEquals(invalidQuestionType.getId() + " is not a LLM free-text question",
                    assertThrows(IllegalArgumentException.class, () -> validate(invalidQuestionType, "")).getMessage());
        }
    }
}

class Helpers {
    public static boolean CORRECT = true;
    public static boolean INCORRECT = false;

    public static LLMFreeTextQuestionValidationResponse validate(Question question, Mark response) throws Exception {
        return validate(question, response.toJSON());
    }

    public static LLMFreeTextQuestionValidationResponse validate(Question question, String response) throws Exception {
        var validator = createValidator(client(response));
        return (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(
                question,
                answer("The user's answer does not matter because we've mocked the endpoint that evaluates it."));
    }

    public static void expectMark(LLMFreeTextQuestionValidationResponse response, boolean isCorrect, int marksAwarded,
            Mark markBreakDown) {
        assertEquals(isCorrect, response.isCorrect());
        assertEquals(marksAwarded, (long) response.getMarksAwarded());
        expectMarkBreakdown(response, markBreakDown.toMarkScheme());
    }

    public static LLMFreeTextChoice answer(String answerString) {
        LLMFreeTextChoice answer = new LLMFreeTextChoice();
        answer.setValue(answerString);
        return answer;
    }

    public static IsaacLLMFreeTextValidator createValidator(OpenAIClient client) throws IOException {
        return new IsaacLLMFreeTextValidator(propertiesForTest(), client);
    }

    public static int getIntTestProperty(String key, int defaultValue) throws IOException {
        try {
            return Integer.parseInt(propertiesForTest().getProperty(LLM_MARKER_MAX_ANSWER_LENGTH));
        } catch (final NumberFormatException ignored) {
            return defaultValue;
        }
    }

    public static OpenAIClient client() {
        return client("");
    }

    public static OpenAIClient client(final String llmResponse) {
        // These must be PowerMocked since the classes are final in the Azure OpenAI
        // library

        var client = createMock(OpenAIClient.class);
        var chatCompletions = createMock(ChatCompletions.class);
        var chatChoice = createMock(ChatChoice.class);
        var chatResponseMessage = createMock(ChatResponseMessage.class);

        EasyMock.expect(chatResponseMessage.getContent()).andReturn(llmResponse);
        EasyMock.expect(chatChoice.getMessage()).andReturn(chatResponseMessage);
        EasyMock.expect(chatCompletions.getChoices()).andReturn(Collections.singletonList(chatChoice)).times(2);
        EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class)))
                .andReturn(chatCompletions);

        replayAll();
        return client;
    }

    private static YamlLoader propertiesForTest() throws IOException {
        return new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml,"
                        + "src/test/resources/segue-unit-test-llm-validator-override.yaml");
    }

    private static void expectMarkBreakdown(LLMFreeTextQuestionValidationResponse response,
            List<LLMFreeTextMarkSchemeEntry> expectedMarks) {
        var awardedMarks = response.getMarkBreakdown();
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
    }
}
