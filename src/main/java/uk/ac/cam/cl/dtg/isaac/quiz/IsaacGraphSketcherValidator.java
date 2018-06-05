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

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        return new QuestionValidationResponse(question.getId(), answer, false,
                new Content(answer.getValue() + " (" + Integer.toString(answer.getValue().length()) + ")"), new Date());
    }
}