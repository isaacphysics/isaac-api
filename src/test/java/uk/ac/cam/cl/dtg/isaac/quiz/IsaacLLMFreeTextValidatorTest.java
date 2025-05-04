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
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.LLMFreeTextQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.easymock.PowerMock.createMock;
import static org.powermock.api.easymock.PowerMock.replayAll;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

import static uk.ac.cam.cl.dtg.isaac.QuestionFactory.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ OpenAIClient.class, ChatCompletions.class, ChatChoice.class, ChatResponseMessage.class })
@PowerMockIgnore({ "jakarta.ws.*" })
public class IsaacLLMFreeTextValidatorTest {
    @Test
    @DisplayName("A one-mark answer for a default marking formula one-mark question gets recognised as correct")
    public void isaacLLMFreeTextValidator_OneMarkQuestionOneMarkAnswer_MarkSchemeShouldIncludeMark() throws Exception {
        var response = validate(genericOneMarkQuestion(), "{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("reasonFoo").setMarks(1),
                field().setName("reasonBar").setMarks(0),
                field().setName("reasonFizz").setMarks(0)));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("A three-mark answer for a default marking formula one-mark question gets recognised as correct")
    public void isaacLLMFreeTextValidator_OneMarkQuestionThreeMarkAnswer_MarkSchemeShouldIncludeMarks() throws Exception {
        var response = validate(genericOneMarkQuestion(), "{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 1}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("reasonFoo").setMarks(1),
                field().setName("reasonBar").setMarks(1),
                field().setName("reasonFizz").setMarks(1)));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("A zero-mark answer for a one-mark question gets recognised as incorrect")
    public void isaacLLMFreeTextValidator_OneMarkQuestionZeroMarkAnswer_MarkSchemeShouldIncludeNoMarks() throws Exception {
        var response = validate(genericOneMarkQuestion(), "{\"reasonFoo\": 0, \"reasonBar\": 0, \"reasonFizz\": 0}");

        assertFalse(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("reasonFoo").setMarks(0),
                field().setName("reasonBar").setMarks(0),
                field().setName("reasonFizz").setMarks(0)));
        assertEquals(0, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("A two-mark answer for a default marking formula two-mark question gets recognised as correct")
    public void isaacLLMFreeTextValidator_TwoMarkQuestionTwoMarkAnswer_MarkSchemeShouldIncludeMarks() throws Exception {
        var response = validate(genericTwoMarkQuestion(), "{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("reasonFoo").setMarks(1),
                field().setName("reasonBar").setMarks(1),
                field().setName("reasonFizz").setMarks(0)));
        assertEquals(2, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("A one-mark answer for a default marking formula two-mark question receives exactly one mark")
    public void isaacLLMFreeTextValidator_TwoMarkQuestionOneMarkAnswer_MarkSchemeShouldIncludeMarks() throws Exception {
        var response = validate(genericTwoMarkQuestion(), "{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("reasonFoo").setMarks(1),
                field().setName("reasonBar").setMarks(0),
                field().setName("reasonFizz").setMarks(0)));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer containing an advantage and a disadvantage mark for a two-mark advantage/disadvantage question receives two marks")
    public void isaacLLMFreeTextValidator_AdvantageDisadvantageQuestionADMarks_MarkTotalShouldBeTwo() throws Exception {
        var response = validate(advantageQuestion(),
                "{\"advantageOne\": 1, \"advantageTwo\": 0, \"disadvantageOne\": 1, \"disadvantageTwo\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("advantageOne").setMarks(1),
                field().setName("advantageTwo").setMarks(0),
                field().setName("disadvantageOne").setMarks(1),
                field().setName("disadvantageTwo").setMarks(0)));
        assertEquals(2, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer containing only a disadvantage mark for a two-mark advantage/disadvantage question receives one mark")
    public void isaacLLMFreeTextValidator_AdvantageDisadvantageQuestionDMarks_MarkTotalShouldBeOne() throws Exception {
        var response = validate(advantageQuestion(),
                "{\"advantageOne\": 0, \"advantageTwo\": 0, \"disadvantageOne\": 1, \"disadvantageTwo\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("advantageOne").setMarks(0),
                field().setName("advantageTwo").setMarks(0),
                field().setName("disadvantageOne").setMarks(1),
                field().setName("disadvantageTwo").setMarks(0)));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer containing two advantage marks for a two-mark advantage/disadvantage question receives one mark")
    public void isaacLLMFreeTextValidator_AdvantageDisadvantageQuestionAAMarks_MarkTotalShouldBeOne() throws Exception {
        var response = validate(advantageQuestion(),
                "{\"advantageOne\": 1, \"advantageTwo\": 1, \"disadvantageOne\": 0, \"disadvantageTwo\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("advantageOne").setMarks(1),
                field().setName("advantageTwo").setMarks(1),
                field().setName("disadvantageOne").setMarks(0),
                field().setName("disadvantageTwo").setMarks(0)));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer containing a point and matching explanation for a two-mark point/explanation question receives two marks")
    public void isaacLLMFreeTextValidator_PointExplanationQuestionPEMarks_MarkTotalShouldBeTwo() throws Exception {
        var response = validate(pointExplanationQuestion(),
                "{\"pointOne\": 1, \"pointTwo\": 0, \"explanationOne\": 1, \"explanationTwo\": 0}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("pointOne").setMarks(1),
                field().setName("pointTwo").setMarks(0),
                field().setName("explanationOne").setMarks(1),
                field().setName("explanationTwo").setMarks(0)));
        assertEquals(2, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer containing an explanation without a matching point for a two-mark point/explanation question receives zero marks")
    public void isaacLLMFreeTextValidator_PointExplanationQuestionEMark_MarkTotalShouldBeZero() throws Exception {
        var response = validate(pointExplanationQuestion(),
                "{\"pointOne\": 0, \"pointTwo\": 0, \"explanationOne\": 1, \"explanationTwo\": 0}");

        assertFalse(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("pointOne").setMarks(0),
                field().setName("pointTwo").setMarks(0),
                field().setName("explanationOne").setMarks(1),
                field().setName("explanationTwo").setMarks(0)));
        assertEquals(0, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer containing a point and a mismatched explanation for a two-mark point/explanation question receives one mark")
    public void isaacLLMFreeTextValidator_PointExplanationQuestionPEMismatchMarks_MarkTotalShouldBeOne() throws Exception {
        var response = validate(pointExplanationQuestion(),
                "{\"pointOne\": 1, \"pointTwo\": 0, \"explanationOne\": 0, \"explanationTwo\": 1}");

        assertTrue(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("pointOne").setMarks(1),
                field().setName("pointTwo").setMarks(0),
                field().setName("explanationOne").setMarks(0),
                field().setName("explanationTwo").setMarks(1)));
        assertEquals(1, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("A response from the client not in the expected json format returns zero marks")
    public void isaacLLMFreeTextValidator_ResponseInvalidFormat_MarkSchemeShouldIncludeNoMarks() throws Exception {
        var response = validate(genericOneMarkQuestion(), "Not a valid JSON response");

        assertFalse(response.isCorrect());
        expectMarkBreakdown(response, generateMarkScheme(
                field().setName("reasonFoo").setMarks(0),
                field().setName("reasonBar").setMarks(0),
                field().setName("reasonFizz").setMarks(0)));
        assertEquals(0, (long) response.getMarksAwarded());
    }

    @Test
    @DisplayName("An answer exceeding the maximum answer length is handled with an exception")
    public void isaacLLMFreeTextValidator_AnswerOverLengthLimit_ExceptionShouldBeThrown() throws Exception {
        int maxAnswerLength = getIntTestProperty(LLM_MARKER_MAX_ANSWER_LENGTH, 4096);
        var answr = answer(String.join("", Collections.nCopies((maxAnswerLength / 10 + 1), "Repeat Me ")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> createValidator(client("")).validateQuestionResponse(genericOneMarkQuestion(), answr));

        assertEquals("Answer is too long for LLM free-text question marking", exception.getMessage());
    }

    /*
     * Test that an error from the client (e.g. timeout, rate limit, out of credits)
     * is handled with an exception
     */
    @Test
    @DisplayName("Error from the client (e.g. timeout, rate limit, out of credits) is handled with an exception")
    public void isaacLLMFreeTextValidator_ResponseError_ExceptionShouldBeThrown() {
        var clnt = createMock(OpenAIClient.class);
        EasyMock.expect(clnt.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class)))
                .andThrow(new RuntimeException("Test OpenAI Exception"));
        replay(clnt);

        ValidatorUnavailableException exception = assertThrows(
                ValidatorUnavailableException.class,
                () -> createValidator(clnt).validateQuestionResponse(genericOneMarkQuestion(), answer("")));
        assertEquals("We are having problems marking LLM marked questions. Please try again later!",
                exception.getMessage());
    }

    @Test
    @DisplayName("Invalid question (i.e. missing maxMarks field/not LLMFreeTextQuestion) is handled with an exception")
    public void isaacLLMFreeTextValidator_InvalidQuestion_ExceptionShouldBeThrown() {
        IsaacLLMFreeTextQuestion invalidQuestionFields = createLLMFreeTextQuestion(null, null, null, null);
        Question invalidQuestionType = new Question();

        assertEquals("This question cannot be answered correctly",
                assertThrows(IllegalArgumentException.class, () -> validate(invalidQuestionFields, "")).getMessage());

        assertEquals(invalidQuestionType.getId() + " is not a LLM free-text question",
                assertThrows(IllegalArgumentException.class, () -> validate(invalidQuestionType, "")).getMessage());
    }

    // --- Helper Functions ---

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * mocks given response as if generated by the OpenAI client.
     *
     * @param llmResponse - mock message to return from client
     */
    private static OpenAIClient client(final String llmResponse) {
        // Create mock objects for the open AI client and each layer of the response it
        // provides
        // These must be PowerMocked since the classes are final in the Azure OpenAI
        // library

        var client = createMock(OpenAIClient.class);
        var chatCompletions = createMock(ChatCompletions.class);
        var chatChoice = createMock(ChatChoice.class);
        var chatResponseMessage = createMock(ChatResponseMessage.class);

        // Mock each layer of the response generated by the client's model
        EasyMock.expect(chatResponseMessage.getContent()).andReturn(llmResponse);
        
        // TODO: are these even necessary?
        EasyMock.expect(chatChoice.getMessage()).andReturn(chatResponseMessage);
        EasyMock.expect(chatCompletions.getChoices()).andReturn(Collections.singletonList(chatChoice)).times(2);
        EasyMock.expect(client.getChatCompletions(anyString(), isA(ChatCompletionsOptions.class)))
                .andReturn(chatCompletions);

        // Set all mocked objects into replay mode
        replayAll();
        return client;
    }

    private static IsaacLLMFreeTextValidator createValidator(OpenAIClient client) throws IOException {
        return new IsaacLLMFreeTextValidator(propertiesForTest(), client);
    }

    private static int getIntTestProperty(String key, int defaultValue) throws IOException {
        try {
            return Integer.parseInt(propertiesForTest().getProperty(LLM_MARKER_MAX_ANSWER_LENGTH));
        } catch (final NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static YamlLoader propertiesForTest() throws IOException {
        return new YamlLoader(
                "src/test/resources/segue-integration-test-config.yaml,"
                        + "src/test/resources/segue-unit-test-llm-validator-override.yaml");
    }

    private static LLMFreeTextChoice answer(String answerString) {
        LLMFreeTextChoice answer = new LLMFreeTextChoice();
        answer.setValue(answerString);
        return answer;
    }

    private LLMFreeTextQuestionValidationResponse validate(Question question, String response) throws IOException, ValidatorUnavailableException {
        var validator = createValidator(client(response));
        return (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(
                question,
                answer("The user's answer does not matter because we've mocked the endpoint that evaluates it."));
    }

    private static void expectMarkBreakdown(LLMFreeTextQuestionValidationResponse response,
            List<LLMFreeTextMarkSchemeEntry> expectedMarks) {
        var awardedMarks = response.getMarkBreakdown();
        assertTrue(expectedMarks.containsAll(awardedMarks));
        assertTrue(awardedMarks.containsAll(expectedMarks));
    }
}
