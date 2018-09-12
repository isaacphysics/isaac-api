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
    private static final Logger log = LoggerFactory.getLogger(IsaacGraphSketcherValidator.class);

    private static final String DEFAULT_VALIDATION_RESPONSE = "TODO incorrect placeholder";


    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {

    	if (!(question instanceof IsaacGraphSketcherQuestion)) {
    		throw new IllegalArgumentException(String.format("This validator only works with Isaac Graph Sketcher Questions... (%s is not numeric)", question.getId()));
    	}

    	if (!(answer instanceof GraphChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected graphChoice for IsaacGraphSketcherQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        IsaacGraphSketcherQuestion graphSketcherQuestion = (IsaacGraphSketcherQuestion) question;
        GraphChoice answerFromUser = (GraphChoice) answer;

        Content feedback = null;
        boolean responseCorrect = false;

        log.debug("Starting validation of '" + answerFromUser.getValue() + "' for '" + graphSketcherQuestion.getId() + "'");

        // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
        //         won't have feedback yet.

 		if (null == graphSketcherQuestion.getChoices() || graphSketcherQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: " + question.getCanonicalSourceFile());

            feedback = new Content("This question does not have any correct answers");
        }

        // STEP 1: Did they provide an answer?

        if (null == feedback && (null == answerFromUser)) {
            feedback = new Content("You did not provide an answer");
        }

        // STEP 2: Otherwise, Does their answer match a choice exactly?

        if (null == feedback) {

            // For all the choices on this question...
            for (Choice c : graphSketcherQuestion.getChoices()) {

                // ... that are of the GraphChoice type, ...
                if (!(c instanceof GraphChoice)) {
                    log.error("Isaac Graph Validator for questionId: " + graphSketcherQuestion.getId()
                            + " expected there to be a GraphChoice. Instead it found a Choice.");
                    continue;
                }

                GraphChoice graphChoice = (GraphChoice) c;

                // ... and that have graphData ...
                if (null == graphChoice.getGraphData() || graphChoice.getGraphData().isEmpty()) {
                    log.error("Expected python expression, but none found in choice for question id: "
                            + graphSketcherQuestion.getId());
                    continue;
                }

                // ... look for an exact string match to the submitted answer.
                if (graphChoice.getGraphData().equals(answerFromUser.getGraphData())) {
                    feedback = (Content) graphChoice.getExplanation();
                    responseCorrect = graphChoice.isCorrect();
                }
            }
        }



        //feedback = new Content("This is Ben looking at your answer");

        return new QuestionValidationResponse(graphSketcherQuestion.getId(), answer, responseCorrect, feedback, new Date());
    }
}