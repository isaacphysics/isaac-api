package uk.ac.cam.cl.dtg.isaac.api;
import com.google.inject.Injector;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacStringMatchValidator;
import uk.ac.cam.cl.dtg.segue.api.QuestionFacade;
import uk.ac.cam.cl.dtg.segue.configuration.SegueGuiceConfigurationModule;

import jakarta.ws.rs.core.Response;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    public void incorrectAnswerToStringMatchQuestion() throws Exception {
        var response = subject().client().post(
            "/questions/_regression_test_|acc_stringmatch_q|_regression_test_stringmatch_/answer",
            "{\"type\": \"stringChoice\", \"value\": \"13\"}"
        ).readEntityAsJson();

        assertFalse(response.getBoolean("correct"));
        assertEquals("13", response.getJSONObject("answer").getString("value"));
    }

    @Test
    public void correctAnswerToStringMatchQuestion() throws Exception {
        var response = subject().client().post(
            "/questions/_regression_test_|acc_stringmatch_q|_regression_test_stringmatch_/answer",
            "{\"type\": \"stringChoice\", \"value\": \"hello\"}"
        ).readEntityAsJson();

        assertTrue(response.getBoolean("correct"));
        assertEquals("hello", response.getJSONObject("answer").getString("value"));
    }

    TestServer subject() throws Exception {
        Injector testInjector = createNiceMock(Injector.class);
        expect(testInjector.getInstance(IsaacStringMatchValidator.class)).andReturn(stringMatchValidator).anyTimes();
        replay(testInjector);
        SegueGuiceConfigurationModule.setInjector(testInjector);

        return startServer(
            new QuestionFacade(properties, contentMapper, contentManager, userAccountManager,
                userPreferenceManager, questionManager, logManager, misuseMonitor, null, userAssociationManager)
        );
    }

    private static final IsaacStringMatchValidator stringMatchValidator = new IsaacStringMatchValidator();
}
