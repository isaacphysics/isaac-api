package uk.ac.cam.cl.dtg.isaac.api;
import com.google.inject.Injector;
import org.json.JSONObject;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacDndQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DndItemChoice;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacStringMatchValidator;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.shaded.com.google.common.collect.Maps.immutableEntry;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacDndValidatorTest.*;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class QuestionFacadeIT extends IsaacIntegrationTestWithREST {
    @Test
    public void shows400ForMissingAnswer() throws Exception {
        var response = subject().client().post("/questions/no_such_question/answer", null);
        response.assertError("No answer received.", Response.Status.BAD_REQUEST);
    }

    @Test
    public void shows404ForMissingQuestion() throws Exception {
        var response = subject().client().post("/questions/no_such_question/answer", "{}");
        response.assertError("No question object found for given id: no_such_question", Response.Status.NOT_FOUND);
    }

    @Nested
    class StringMatchQuestion {
        @Test
        public void wrongAnswer() throws Exception {
            var response = subject().client().post(
                url("_regression_test_|acc_stringmatch_q|_regression_test_stringmatch_"),
                "{\"type\": \"stringChoice\", \"value\": \"13\"}"
            ).readEntityAsJson();

            assertFalse(response.getBoolean("correct"));
            assertEquals("13", response.getJSONObject("answer").getString("value"));
        }

        @Test
        public void rightAnswer() throws Exception {
            var response = subject().client().post(
                url("_regression_test_|acc_stringmatch_q|_regression_test_stringmatch_"),
                "{\"type\": \"stringChoice\", \"value\": \"hello\"}"
            ).readEntityAsJson();

            assertTrue(response.getBoolean("correct"));
            assertEquals("hello", response.getJSONObject("answer").getString("value"));
        }
    }

    @Nested
    class DndQuestion {
        @ParameterizedTest
        @CsvSource(value = {
            "{};Unable to map response to a Choice;404",
            "{\"type\": \"unknown\"};This validator only works with DndItemChoices;400",
            "{\"type\": \"dndChoice\", \"items\": [{\"dropZoneId\": \"leg_1\"}]};Cannot validate answer with missing ids;400",
            "{\"type\": \"dndChoice\", \"items\": [{\"id\": \"6d3d\"}]};Cannot validate answer with missing dropZoneIds;400",
            "{\"type\": \"dndChoice\", \"items\": [{\"id\": \"6d3d\", \"dropZoneId\": \"leg_1\", \"a\": \"a\"}]};Unable to map response to a Choice;404",
            "{\"type\": \"dndChoice\", \"items\": \"some_string\"};Unable to map response to a Choice;404",
            "{\"type\": \"dndChoice\", \"items\": [{\"id\": [{}], \"dropZoneId\": \"leg_1\"}]};Unable to map response to a Choice;404"
        }, delimiter = ';')
        public void invalidAnswer(final String answerStr, final String emsg, final String estate) throws Exception {
            var dndQuestion = persist(createQuestion(correct(answer(choose(item_3cm, "leg_1")))));
            var response = subject().client().post(url(dndQuestion.getId()), answerStr);
            response.assertError(emsg, estate);
        }

        @ParameterizedTest
        @CsvSource(value = {
            "{\"type\": \"dndChoice\"}",
            "{\"type\": \"dndChoice\", \"items\": []}"
        }, delimiter = ';')
        public void validEmptyAnswer(final String answerStr) throws Exception {
            var dndQuestion = persist(createQuestion(correct(answer(choose(item_3cm, "leg_1")))));
            var response = subject().client().post(url(dndQuestion.getId()), answerStr).readEntityAsJson();
            assertFalse(response.getBoolean("correct"));
            assertEquals(
                readEntity(new JSONObject(answerStr), DndItemChoice.class),
                readEntity(response.getJSONObject("answer"), DndItemChoice.class)
            );
        }

        @ParameterizedTest
        @CsvSource(value = {
            "{\"type\": \"dndChoice\", \"items\": [{\"id\": \"6d3d\", \"dropZoneId\": \"leg_1\"}]}",
            "{\"type\": \"dndChoice\", \"items\": [{\"id\": \"6d3d\", \"dropZoneId\": \"leg_1\", \"type\": \"dndItem\"}]}",
            "{\"type\": \"dndChoice\", \"items\": [{\"id\": \"6d3d\", \"dropZoneId\": \"leg_1\", \"type\": \"unknown\"}]}"
        }, delimiter = ';')
        public void validCorrectAnswer(final String answerStr) throws Exception {
            var dndQuestion = persist(createQuestion(correct(answer(choose(item_3cm, "leg_1")))));

            var response = subject().client().post(url(dndQuestion.getId()), answerStr).readEntityAsJson();
            assertTrue(response.getBoolean("correct"));
            assertEquals(
                readEntity(new JSONObject(answerStr), DndItemChoice.class),
                readEntity(response.getJSONObject("answer"), DndItemChoice.class)
            );
        }

        @Test
        public void wrongAnswer() throws Exception {
            var dndQuestion = persist(createQuestion(
                correct(answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")))
            ));
            var answer = answer(choose(item_3cm, "leg_2"), choose(item_4cm, "hypothenuse"), choose(item_5cm, "leg_1"));

            var response = subject().client().post(url(dndQuestion.getId()), answer).readEntityAsJson();

            assertFalse(response.getBoolean("correct"));
            assertEquals(answer, readEntity(response.getJSONObject("answer"), DndItemChoice.class));
        }



        @Test
        public void explanation() throws Exception {
            var explanation = new Content("That's right!");
            var dndQuestion = persist(createQuestion(correct(
                answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse")),
                explanation
            )));
            var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

            var response = subject().client().post(url(dndQuestion.getId()), answer).readEntityAsJson();

            assertTrue(response.getBoolean("correct"));
            assertEquals(explanation, readEntity(response.getJSONObject("explanation"), Content.class));
        }

        @Test
        public void dropZonesCorrect() throws Exception {
            var dndQuestion = createQuestion(correct(
                    answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"))
            ));
            dndQuestion.setDetailedItemFeedback(true);
            persist(dndQuestion);
            var answer = answer(choose(item_3cm, "leg_1"), choose(item_4cm, "leg_2"), choose(item_5cm, "hypothenuse"));

            var response = subject().client().post(url(dndQuestion.getId()), answer).readEntityAsJson();

            assertTrue(response.getBoolean("correct"));
            assertEquals(
                new DropZonesCorrectFactory().setLeg1(true).setLeg2(true).setHypothenuse(true).build(),
                readEntity(response.getJSONObject("dropZonesCorrect"), Map.class)
            );
        }
    }

    private IsaacDndQuestion persist(final IsaacDndQuestion question) throws Exception {
        elasticSearchProvider.bulkIndexWithIDs(
            "6c2ba42c5c83d8f31b3b385b3a9f9400a12807c9",
            "content",
            List.of(immutableEntry(
                question.getId(), contentMapper.getSharedContentObjectMapper().writeValueAsString(question))
            )
        );
        return question;
    }

    private TestServer subject() throws Exception {
        Injector testInjector = createNiceMock(Injector.class);
        expect(testInjector.getInstance(IsaacStringMatchValidator.class)).andReturn(stringMatchValidator).anyTimes();
        expect(testInjector.getInstance(IsaacDndValidator.class)).andReturn(dndValidator).anyTimes();
        replay(testInjector);
        SegueGuiceConfigurationModule.setInjector(testInjector);

        return startServer(
            new QuestionFacade(properties, contentMapper, contentManager, userAccountManager,
                userPreferenceManager, questionManager, logManager, misuseMonitor, null, userAssociationManager)
        );
    }

    private String url(final String questionId) {
        return String.format("/questions/%s/answer", questionId);
    }

    private <T> T readEntity(final JSONObject value, final Class<T> klass)  throws Exception {
        return contentMapper.getSharedContentObjectMapper().readValue(value.toString(), klass);
    }

    private static final IsaacStringMatchValidator stringMatchValidator = new IsaacStringMatchValidator();
    private static final IsaacDndValidator dndValidator = new IsaacDndValidator();
}
