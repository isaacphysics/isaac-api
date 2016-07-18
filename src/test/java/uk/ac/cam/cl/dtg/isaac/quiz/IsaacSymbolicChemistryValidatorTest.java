package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.api.client.util.Lists;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.legacy.PowerMockRunner;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicChemistryQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.ChemicalFormula;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests the ability of IsaacSymbolicChemistryValidator to validate chemistry answers correctly.
 *
 * Created by hhrl2 on 14/07/2016.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest({DefaultHttpClient.class, HttpPost.class, HttpResponse.class,
        HttpEntity.class, EntityUtils.class, IsaacSymbolicChemistryValidator.class})
public class IsaacSymbolicChemistryValidatorTest {

    @Before
    public final void setUp() {
        // Prevents AbstractHttpClient from throwing any useless exceptions.
        PowerMock.suppressConstructor(AbstractHttpClient.class);
    }

    /**
     * Mocks the server in IsaacSymbolicChemistryValidator. Returns fixed JSON objects provided in arguments.
     * @param answer The student answer.
     * @param choices List of possible choices.
     * @param mockJsonStrings The list of JSON objects that should be returned by the server during comparison of answer and choice.
     * @throws Exception
     */
    private final void prepareTest(ChemicalFormula answer, ArrayList<Choice> choices, ArrayList<String> mockJsonStrings) throws Exception {
        // Mock DefaultHttpClient, and its constructor method.
        DefaultHttpClient mockHttpClient = PowerMock.createMock(DefaultHttpClient.class);
        PowerMock.expectNew(DefaultHttpClient.class).andReturn(mockHttpClient);

        // Mock HttpPost
        HttpPost mockHttpPost = PowerMock.createMock(HttpPost.class);

        for (int i = 0; i < choices.size(); i++) {

            // Determine the url that IsaacSymbolicChemistryValidator will communicate with.
            String url = "http://localhost:9090/check?test=" + URLEncoder.encode(answer.getMhchemExpression(), "UTF-8")
                    + "&target=" + URLEncoder.encode(((ChemicalFormula) choices.get(i)).getMhchemExpression(), "UTF-8");

            // Mock HttpPost's constructor method.
            PowerMock.expectNew(HttpPost.class, url).andReturn(mockHttpPost);

            // Mock HttpResponse, .getEntity(), and HttpEntity
            HttpResponse mockHttpResponse = PowerMock.createMock(HttpResponse.class);
            HttpEntity mockHttpEntity = PowerMock.createMock(HttpEntity.class);
            EasyMock.expect(mockHttpClient.execute(mockHttpPost)).andReturn(mockHttpResponse);
            EasyMock.expect(mockHttpResponse.getEntity()).andReturn(mockHttpEntity);

            // Mock static class EntityUtils, and its static method toString.
            PowerMock.mockStaticPartial(EntityUtils.class, "toString");

            EasyMock.expect(EntityUtils.toString(mockHttpEntity)).andReturn(mockJsonStrings.get(i));

        }

        // Mock all objects now.
        PowerMock.replayAll();
    }

    /**
     * Tests if back end could detect type mismatch from answer.
     * Correct answer: Na + Cl -> NaCl (Chemical equation)
     * Student answer: Na (Chemical expression)
     * @throws Exception
     */
    @Test
    public final void typeMismatchTest() throws Exception {
        // Create the validator
        IsaacSymbolicChemistryValidator test = new IsaacSymbolicChemistryValidator();

        // Create the JSON object that server should return
        String mockJsonString = "{\n" +
                "  \"testString\" : \"Na\",\n" +
                "  \"targetString\" : \"Na+Cl->NaCl\",\n" +
                "  \"test\" : \"Na\",\n" +
                "  \"target\" : \"Na + Cl -> NaCl\",\n" +
                "  \"containsError\" : false,\n" +
                "  \"equal\" : false,\n" +
                "  \"typeMismatch\" : true,\n" +
                "  \"expectedType\" : \"equation\",\n" +
                "  \"receivedType\" : \"expression\",\n" +
                "  \"weaklyEquivalent\" : false,\n" +
                "  \"sameCoefficient\" : false,\n" +
                "  \"sameState\" : false,\n" +
                "  \"wrongTerms\" : [ ]\n" +
                "}";

        // Create a typical chemistry question
        IsaacSymbolicChemistryQuestion testQuestion = new IsaacSymbolicChemistryQuestion();
        ArrayList<Choice> answerList = Lists.newArrayList();

        ChemicalFormula correct = new ChemicalFormula();
        correct.setMhchemExpression("Na + Cl -> NaCl");
        correct.setCorrect(true);

        answerList.add(correct);
        testQuestion.setChoices(answerList);

        ChemicalFormula answer = new ChemicalFormula();
        answer.setMhchemExpression("Na");

        // Mock the objects
        prepareTest(answer, answerList, new ArrayList<>(Arrays.asList(mockJsonString)));

        QuestionValidationResponse response = test.validateQuestionResponse(testQuestion, answer);

        // Verify that objects are indeed mocked.
        PowerMock.verifyAll();

        assertFalse(response.isCorrect());
        assertTrue(response.getExplanation().equals(new Content("Type of input does not match with our correct answer.")));
    }

    @Test
    public final void errorTermTest(){}

    @Test
    public final void unbalancedEquationTest(){}

    @Test
    public final void invalidNuclearEquationTest(){}

    @Test
    public final void weakEquivalenceTest(){}


}
