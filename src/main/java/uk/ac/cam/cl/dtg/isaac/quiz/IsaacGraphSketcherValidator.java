package uk.ac.cam.cl.dtg.isaac.quiz;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import org.apache.commons.lang3.Validate;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacGraphSketcherQuestion;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.GraphChoice;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatorUnavailableException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Validator that only provides functionality to validate graph questions.
 *
 * Created by hhrl2 on 01/08/2016.
 */
public class IsaacGraphSketcherValidator implements IValidator {

    /**
     * Private logger for printing error messages on console.
     */
    private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicChemistryValidator.class);

    /**
     * Given two formulae, where one is student answer, and another is the target mhchem string,
     * this method generates a JSON object of them, and sends it to a back end chemistry checker
     * for comparison. Comparison results are sent back from server as a JSON string and returned here.
     *
     * @param submittedGraph GraphChoice submitted by user.
     * @param graphChoice Formula of one of the choice in content editor.
     * @return The JSON string returned from the ChemicalChecker server.
     * @throws IOException Trouble connecting to the ChemicalChecker server.
     */
    private String jsonPostAndGet(final String submittedGraph, final String graphChoice) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        // Complicated: Put formulae into a JSON object
        HashMap<String, String> req = Maps.newHashMap();
        req.put("target", submittedGraph);
        req.put("test", graphChoice);
//      req.put("description", symbolicQuestion.getId());

        StringWriter sw = new StringWriter();
        JsonGenerator g = new JsonFactory().createGenerator(sw);
        mapper.writeValue(g, req);
        g.close();
        String requestString = sw.toString();

        // Do some real checking through HTTP
        HttpClient httpClient = new DefaultHttpClient();
        //TODO: factor this out into a constant along with the symbolic URL.
        HttpPost httpPost = new HttpPost("http://localhost:5000/test");

        // Send JSON object across ChemistryChecker server.
        httpPost.setEntity(new StringEntity(requestString));
        httpPost.addHeader("Content-Type", "application/json");

        // Receive JSON response from server.
        HttpResponse httpResponse = httpClient.execute(httpPost);
        HttpEntity responseEntity = httpResponse.getEntity();

        return EntityUtils.toString(responseEntity);
    }

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer)
            throws ValidatorUnavailableException {

        // Asserts that both question and answer is not null.
        Validate.notNull(question);
        Validate.notNull(answer);

        // Checks if question is indeed related to graph.
        // If it is not, throw exceptions.
        if (!(question instanceof IsaacGraphSketcherQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac GraphChoice Sketcher Questions... "
                            + "(%s is not graph)",
                    question.getId()));
        }

        // Checks if answer is indeed related to graph.
        // If it is not, throw exceptions.
        if (!(answer instanceof GraphChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected GraphChoice for IsaacGraphSketcherQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        /**
         * GraphChoice question, in better type.
         */
        IsaacGraphSketcherQuestion graphQuestion = (IsaacGraphSketcherQuestion) question;

        /**
         * GraphChoice answer, in better type.
         */
        GraphChoice submittedGraphChoice = (GraphChoice) answer;

        /**
         * The feedback we send the user.
         */
        Content feedback = null;

        /**
         * Boolean. Is the student giving a good answer?
         */
        boolean responseCorrect = false;


        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.
        if (null == graphQuestion.getChoices() || graphQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer?
        if (null == feedback && (null == submittedGraphChoice.getGraphData()
                || submittedGraphChoice.getGraphData().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: Any exact match with a choice?
        if (null == feedback) {

            // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
            List<Choice> orderedChoices = Lists.newArrayList(graphQuestion.getChoices());

            Collections.sort(orderedChoices, new Comparator<Choice>() {
                @Override
                public int compare(final Choice o1, final Choice o2) {
                    int o1Val = o1.isCorrect() ? 0 : 1;
                    int o2Val = o2.isCorrect() ? 0 : 1;
                    return o1Val - o2Val;
                }
            });

            // For all choices in this question...
            for (Choice c : orderedChoices) {
                // ... that are of the ChemicalFormula type, ...
                if (!(c instanceof GraphChoice)) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                GraphChoice graphChoice = (GraphChoice) c;

                // ... and that have graph data ...
                if (null == graphChoice.getGraphData() || graphChoice.getGraphData().isEmpty()) {
                    // Don't need to log this - it will have been logged above.
                    continue;
                }

                // ... test their answer against this choice with the graph checker.
                HashMap<String, Object> response;

                // Pass some JSON to a REST endpoint and get some JSON back.
                try {

                    ObjectMapper mapper = new ObjectMapper();
                    String responseString = jsonPostAndGet(submittedGraphChoice.getGraphData(),
                            graphChoice.getGraphData());
                    response = mapper.readValue(responseString, HashMap.class);

                    // Checks if student answer exactly matches one of the choices.
                    if (response.get("isCorrect").equals(true)) {
                        // Feedback <- choice's explanation.
                        responseCorrect = graphChoice.isCorrect();
                        feedback = (Content) graphChoice.getExplanation();
                        break;
                    }

                } catch (IOException e) {

                    log.error("Failed to check formula with chemistry checker. "
                            + "Is the server running? Not trying again.");
                    throw new ValidatorUnavailableException("We are having problems marking Chemistry Questions."
                            + " Please try again later!");

                }
            }
        }

        return new QuestionValidationResponse(graphQuestion.getId(), answer, false,
                new Content("GraphChoice cannot yet be marked"), new Date());
    }
}
