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
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.LLMFreeTextQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.util.YamlLoader;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.advantageExamples;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.answer;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.client;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.createLLMFreeTextQuestion;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.formulaFn;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.formulaVar;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.genericExamples;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.pointExplanationExamples;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.propertiesForTest;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.question;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Factories.toMarkScheme;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.CORRECT;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.INCORRECT;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.NO_MARKS;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.ONE_MARK;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.TWO_MARKS;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.expectMark;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.getIntTestProperty;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacLLMFreeTextValidatorTest.Helpers.validate;
import static uk.ac.cam.cl.dtg.segue.api.Constants.*;

public class IsaacLLMFreeTextValidatorTest {

    @Nested
    @DisplayName("Test that a mark is awarded based on the marking formula")
    class TestFormulaBasedMarking {

        static Stream<Arguments> data() {
            return Stream.of(
                    genericOneMarkCases(),
                    genericTwoMarkCases(),
                    advantageCases(),
                    pointExplanationCases()
            ).flatMap(s -> s);
        }

        @ParameterizedTest(name = "{index}: {0}")
        @MethodSource("data")
        void test(final String description, final IsaacLLMFreeTextQuestion question, final String breakdown,
                  final boolean expectedResult, final int expectedMarkTotal) throws Exception {
            LLMFreeTextQuestionValidationResponse resp = validate(question, answer(), client(breakdown));
            expectMark(resp, expectedResult, expectedMarkTotal, toMarkScheme(breakdown));
        }

        /*
           Generic Mark Scheme = MIN(maxMarks, SUM(... all marks ...))
           This mark scheme is used when no other valid mark scheme is specified for the question.
           It is the total number of marks received, capped by the required maxMarks.
        */
        private static Stream<Arguments> genericOneMarkCases() {
            IsaacLLMFreeTextQuestion question = createLLMFreeTextQuestion("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 1}",
                    1, genericExamples(), null);

            return Stream.of(
                    Arguments.of("A one-mark answer for a default marking formula one-mark question gets recognised as correct",
                            question, "{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}", CORRECT, ONE_MARK),
                    Arguments.of("A three-mark answer for a default marking formula one-mark question gets recognised as correct",
                            question, "{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 0}", CORRECT, ONE_MARK),
                    Arguments.of("A zero-mark answer for a one-mark question gets recognised as incorrect",
                            question, "{\"reasonFoo\": 0, \"reasonBar\": 0, \"reasonFizz\": 0}", INCORRECT, NO_MARKS)
            );
        }

        private static Stream<Arguments> genericTwoMarkCases() {
            IsaacLLMFreeTextQuestion question = createLLMFreeTextQuestion("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 1}",
                    2, genericExamples(), null);

            return Stream.of(
                    Arguments.of("A two-mark answer for a default marking formula two-mark question gets recognised as correct",
                            question, "{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 0}", CORRECT, TWO_MARKS),
                    Arguments.of("A one-mark answer for a default marking formula two-mark question receives exactly one mark",
                            question, "{\"reasonFoo\": 1, \"reasonBar\": 0, \"reasonFizz\": 0}", CORRECT, ONE_MARK)
            );
        }

        /*
          Advantage/Disadvantage Mark Scheme = SUM(
            MAX(... All Advantage Marks ...), MAX(... All Disadvantage Marks ...)
          )
          This is a generally applicable mark scheme for any two mutually exclusive categories of marks.
          A common example of this is advantages and disadvantages.
        */
        private static Stream<Arguments> advantageCases() {
            LLMMarkingFunction formula = formulaFn("SUM",
                    formulaFn("MAX", formulaVar("adv1"), formulaVar("adv2")),
                    formulaFn("MAX", formulaVar("dis1"), formulaVar("dis2")));
            IsaacLLMFreeTextQuestion question = createLLMFreeTextQuestion("{\"adv1\": 1, \"adv2\": 1, \"dis1\": 1, \"dis2\": 1}",
                    2, advantageExamples(), formula);

            return Stream.of(
                    Arguments.of("An answer containing an advantage and a disadvantage mark receives two marks",
                            question, "{\"adv1\": 1, \"adv2\": 0, \"dis1\": 1, \"dis2\": 0}", CORRECT, TWO_MARKS),
                    Arguments.of("An answer containing only a disadvantage mark receives one mark",
                            question, "{\"adv1\": 0, \"adv2\": 0, \"dis1\": 1, \"dis2\": 0}", CORRECT, ONE_MARK),
                    Arguments.of("An answer containing two advantage marks receives one mark",
                            question, "{\"adv1\": 1, \"adv2\": 1, \"dis1\": 0, \"dis2\": 0}", CORRECT, ONE_MARK)
            );
        }


        /*
            Point/Explanation Mark Scheme = SUM(
                MAX(pointOne, ... pointN),
                MAX(MIN(pointOne, explanationOne), MIN(pointTwo, explanationTwo), ... MIN(pointN, explanationN)
            )
            This is a generally applicable mark scheme for any grouped sets of marks where a prerequisite mark is
            required to receive a secondary mark. A common example of this is points and explanations.
        */
        private static Stream<Arguments> pointExplanationCases() {
            LLMMarkingFunction formula = formulaFn("SUM",
                    formulaFn("MAX", formulaVar("pnt1"), formulaVar("pnt2")),
                    formulaFn("MAX",
                            formulaFn("MIN", formulaVar("pnt1"), formulaVar("expl1")),
                            formulaFn("MIN", formulaVar("pnt2"), formulaVar("expl2"))));
            IsaacLLMFreeTextQuestion question = createLLMFreeTextQuestion("{\"pnt1\": 1, \"pnt2\": 1, \"expl1\": 1, \"expl2\": 1}",
                    2, pointExplanationExamples(), formula);

            return Stream.of(
                    Arguments.of("An answer containing a point and matching explanation receives two marks",
                            question, "{\"pnt1\": 1, \"pnt2\": 0, \"expl1\": 1, \"expl2\": 0}", CORRECT, TWO_MARKS),
                    Arguments.of("An answer containing an explanation without a matching point receives zero marks",
                            question, "{\"pnt1\": 0, \"pnt2\": 0, \"expl1\": 1, \"expl2\": 0}", INCORRECT, NO_MARKS),
                    Arguments.of("An answer containing a point and a mismatched explanation receives one mark",
                            question, "{\"pnt1\": 1, \"pnt2\": 0, \"expl1\": 0, \"expl2\": 0}", CORRECT, ONE_MARK)
            );
        }
    }

    @Nested
    @DisplayName("Test application behaviour in case of errors.")
    class TestErrorHandling {
        @Test
        @DisplayName("A response from the client not in the expected json format returns zero marks")
        void isaacLLMFreeTextValidator_ResponseInvalidFormat_MarkSchemeShouldIncludeNoMarks() throws Exception {
            IsaacLLMFreeTextQuestion question = createLLMFreeTextQuestion("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 1}",
                    1, genericExamples(), null);
            LLMFreeTextQuestionValidationResponse response = validate(question, answer(), client("Not a valid JSON response"));
            expectMark(response, INCORRECT, 0,
                    toMarkScheme("{\"reasonFoo\": 0, \"reasonBar\": 0, \"reasonFizz\": 0}"));
        }

        @Test
        @DisplayName("An answer exceeding the maximum answer length is handled with an exception")
        void isaacLLMFreeTextValidator_AnswerOverLengthLimit_ExceptionShouldBeThrown() throws Exception {
            int maxAnswerLength = getIntTestProperty(LLM_MARKER_MAX_ANSWER_LENGTH, 4096);
            LLMFreeTextChoice choice = new LLMFreeTextChoice();
            choice.setValue(String.join("", Collections.nCopies((maxAnswerLength / 10 + 1), "Repeat Me ")));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validate(question(), choice, client()));

            assertEquals("Answer is too long for LLM free-text question marking", exception.getMessage());
        }

        @Test
        @DisplayName("Error from the client (e.g. timeout, rate limit, out of credits) is handled with an exception")
        void isaacLLMFreeTextValidator_ResponseError_ExceptionShouldBeThrown() {
            OpenAIClient client = mock(OpenAIClient.class);
            when(client.getChatCompletions(anyString(), any(ChatCompletionsOptions.class)))
                    .thenThrow(new RuntimeException("Test OpenAI Exception"));

            ValidatorUnavailableException exception = assertThrows(ValidatorUnavailableException.class, () -> validate(question(), answer(), client));

            assertEquals("We are having problems marking LLM marked questions. Please try again later!",
                    exception.getMessage());
        }

        @Test
        @DisplayName("Invalid question (missing maxMarks fields) is handled with an exception")
        void isaacLLMFreeTextValidator_MissingMaxMarks_ExceptionShouldBeThrown() {
            IsaacLLMFreeTextQuestion badFields = createLLMFreeTextQuestion(null, null, null, null);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validate(badFields, answer(), client()));
            assertEquals("This question cannot be answered correctly", exception.getMessage());
        }

        @Test
        @DisplayName("Invalid question (not LLMFreeTextQuestion) is handled with an exception")
        void isaacLLMFreeTextValidator_NotLLMFreeTextQuestion_ExceptionShouldBeThrown() {
            Question badType = new Question();
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> validate(badType, answer(), client()));
            assertEquals(badType.getId() + " is not a LLM free-text question", exception.getMessage());
        }
    }

    protected static class Helpers {
        public static boolean CORRECT = true;
        public static boolean INCORRECT = false;
        public static int NO_MARKS = 0;
        public static int ONE_MARK = 1;
        public static int TWO_MARKS = 2;

        /**
         * Use this helper as the "act" part of a test.
         *
         * @param question  - create using createLLMFreeTextQuestion factory method.
         * @param answer 1  - this is what OpenAI would validate. As these tests mock the API, the answer doesn't
         *                    usually matter, but still matters for cases where we check validation. For example, even
         *                    these mocked tests notice if an answer exceeds the maximum length.
         * @param client    - use the mocked client instance to specify the mark breakdown the API should return
         * @return the response from the validator, examine this object in "assert".
         */
        public static LLMFreeTextQuestionValidationResponse validate(Question question, LLMFreeTextChoice answer,
                                                                     OpenAIClient client) throws Exception {
            IsaacLLMFreeTextValidator validator = new IsaacLLMFreeTextValidator(propertiesForTest(), client);
            return (LLMFreeTextQuestionValidationResponse) validator.validateQuestionResponse(question, answer);
        }

        /**
         * Use this helper as the "assert" part of a test.
         *
         * @param response      - response from the validator, the object we examine by performing assertions.
         * @param isCorrect     - whether the response should be marked as correct or incorrect. Use provided constants
         *                        CORRECT, INCORRECT.
         * @param marksAwarded  - how many marks should be awarded in total for the question. This is calculated from
         *                        the breakdown specified during the act part. Use provided constants NO_MARKS,
         *                        ONE_MARK, TWO_MARKS.
         * @param expectedMarks - double check that the breakdown is returned in the expected format
         */
        public static void expectMark(LLMFreeTextQuestionValidationResponse response, boolean isCorrect,
                                      int marksAwarded, List<LLMFreeTextMarkSchemeEntry> expectedMarks) {
            assertEquals(isCorrect, response.isCorrect());
            assertEquals(marksAwarded, (long) response.getMarks());
            assertTrue(expectedMarks.containsAll(response.getMarkBreakdown()));
            assertTrue(response.getMarkBreakdown().containsAll(expectedMarks));
        }

        /**
         * A helper for accessing configuration properties safely.
         *
         * @param key       - the configuration property key to look up.
         * @param fallback  - the value to return if the property is not found.
         */

        public static int getIntTestProperty(String key, int fallback) throws IOException {
            try {
                return Integer.parseInt(propertiesForTest().getProperty(key));
            } catch (final NumberFormatException ignored) {
                return fallback;
            }
        }
    }

    protected static class Factories {
        public static LLMMarkingFunction formulaFn(final String name, final LLMMarkingExpression... args) {
            LLMMarkingFunction function = new LLMMarkingFunction();
            if (Objects.equals(name, "SUM")) {
                function.setName(LLMMarkingFunction.FunctionName.SUM);
            } else if (Objects.equals(name, "MIN")) {
                function.setName(LLMMarkingFunction.FunctionName.MIN);
            } else if (Objects.equals(name, "MAX")) {
                function.setName(LLMMarkingFunction.FunctionName.MAX);
            }
            function.setArguments(Arrays.asList(args));
            return function;
        }

        public static LLMMarkingVariable formulaVar(final String name) {
            LLMMarkingVariable variable = new LLMMarkingVariable();
            variable.setName(name);
            return variable;
        }

        public static LLMFreeTextChoice answer() {
            LLMFreeTextChoice answer = new LLMFreeTextChoice();
            answer.setValue("The actual answer does not matter because Open AI API is mocked.");
            return answer;
        }

        public static OpenAIClient client() {
            return client("");
        }

        public static OpenAIClient client(final String llmResponse) {
            OpenAIClient client = mock(OpenAIClient.class);
            ChatCompletions chatCompletions = mock(ChatCompletions.class);
            ChatChoice chatChoice = mock(ChatChoice.class);
            ChatResponseMessage chatResponseMessage = mock(ChatResponseMessage.class);

            when(chatResponseMessage.getContent()).thenReturn(llmResponse);
            when(chatChoice.getMessage()).thenReturn(chatResponseMessage);
            when(chatCompletions.getChoices()).thenReturn(Collections.singletonList(chatChoice));
            when(client.getChatCompletions(anyString(), any(ChatCompletionsOptions.class)))
                    .thenReturn(chatCompletions);

            return client;
        }

        public static IsaacLLMFreeTextQuestion question() {
            return createLLMFreeTextQuestion("{\"reasonFoo\": 1, \"reasonBar\": 1, \"reasonFizz\": 1}",
                    1, genericExamples(), null);
        }

        /**
         * Factory method for creating a valid IsaacLLMFreeTextQuestion.
         * @param scheme         - JSON string for mark scheme. Keys as mark names, possible marks awarded as values.
         * @param maxMarks       - total number of possible marks awarded for a question
         * @param markedExamples - a list of examples to train the AI. Test invalid input, or use provided sample
         *                         examples genericExamples, advantageExamples, pointExplanationExamples.
         * @param markingFormula - derives marks awarded for question from a breakdown of marks received from LLM.
         *                         use formulaFn factory method to create these.
         */
        public static IsaacLLMFreeTextQuestion createLLMFreeTextQuestion(final String scheme,
                                                                         final Integer maxMarks,
                                                                         final List<LLMFreeTextMarkedExample> markedExamples,
                                                                         final LLMMarkingExpression markingFormula) {
            IsaacLLMFreeTextQuestion question = new IsaacLLMFreeTextQuestion();
            question.setMarkScheme(scheme == null ? null : toMarkScheme(scheme));
            question.setMaxMarks(maxMarks);
            question.setMarkedExamples(markedExamples);
            question.setMarkingFormula(markingFormula);
            return question;
        }

        public static List<LLMFreeTextMarkedExample> genericExamples() {
            return generateMarkedExamples(
                    new JSONObject().put("answer", "Foo and Bar").put("marksAwarded", 1).put("marks",
                            new JSONObject().put("reasonFoo", 1).put("reasonBar", 1).put("reasonFizz", 0)));
        }

        public static List<LLMFreeTextMarkedExample> advantageExamples() {
            return generateMarkedExamples(
                    new JSONObject().put("answer", "Advantage").put("marksAwarded", 1).put("marks", new JSONObject()
                            .put("adv1", 1).put("adv2", 0).put("dis1", 0).put("dis2", 0)));
        }

        public static List<LLMFreeTextMarkedExample> pointExplanationExamples() {
            return generateMarkedExamples(
                    new JSONObject().put("answer", "Explanation").put("marksAwarded", 0).put("marks", new JSONObject()
                            .put("pnt1", 0).put("pnt2", 0).put("expl1", 1).put("expl2", 0)));
        }

        public static YamlLoader propertiesForTest() throws IOException {
            return new YamlLoader(
                    "src/test/resources/segue-integration-test-config.yaml,"
                            + "src/test/resources/segue-unit-test-llm-validator-override.yaml");
        }

        public static List<LLMFreeTextMarkSchemeEntry> toMarkScheme(String json) {
            JSONObject parsed = new JSONObject(json);
            return parsed.keySet().stream().map(key -> {
                LLMFreeTextMarkSchemeEntry output = new LLMFreeTextMarkSchemeEntry();
                output.setJsonField(key);
                output.setMarks(parsed.getInt(key));
                output.setShortDescription("Some description that does not matter for the test.");
                return output;
            }).collect(Collectors.toList());
        }

        private static List<LLMFreeTextMarkedExample> generateMarkedExamples(JSONObject... jsonMarkedExamples) {
            return Stream.of(jsonMarkedExamples).map(input -> {
                LLMFreeTextMarkedExample output = new LLMFreeTextMarkedExample();
                output.setAnswer(input.getString("answer"));
                output.setMarksAwarded(input.getInt("marksAwarded"));

                JSONObject jsonMarkedExampleMarks = input.getJSONObject("marks");
                Map<String, Integer> marks = jsonMarkedExampleMarks.keySet().stream()
                        .collect(Collectors.toMap(mark -> mark, jsonMarkedExampleMarks::getInt));
                output.setMarks(marks);

                return output;
            }).collect(Collectors.toList());
        }
    }
}