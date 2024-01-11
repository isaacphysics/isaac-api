package uk.ac.cam.cl.dtg.isaac.quiz;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import org.isaacphysics.graphchecker.data.Input;
import org.isaacphysics.graphchecker.dos.GraphAnswer;
import org.isaacphysics.graphchecker.features.Features;
import org.isaacphysics.graphchecker.translation.AnswerToInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacGraphSketcherQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.GraphChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

/**
 * Validator that only provides functionality to validate graph questions.
 * <br>
 * Created by hhrl2 on 01/08/2016.
 */
public class IsaacGraphSketcherValidator implements IValidator, ISpecifier {

  /**
   * Private logger for printing error messages on console.
   */
  private static final Logger log = LoggerFactory.getLogger(IsaacGraphSketcherValidator.class);

  private static final AnswerToInput ANSWER_TO_INPUT = new AnswerToInput();
  private static final Features FEATURES = new Features(new IsaacGraphSketcherSettings());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
    requireNonNull(question);
    requireNonNull(answer);

    if (!(question instanceof IsaacGraphSketcherQuestion)) {
      throw new IllegalArgumentException(String.format(
          "This validator only works with Isaac Graph Sketcher Questions... (%s is not string match)",
          question.getId()));
    }

    if (!(answer instanceof GraphChoice)) {
      throw new IllegalArgumentException(String.format(
          "Expected GraphChoice for IsaacGraphSketcherQuestion: %s. Received (%s) ", question.getId(),
          answer.getClass()));
    }

    IsaacGraphSketcherQuestion graphSketcherQuestion = (IsaacGraphSketcherQuestion) question;

    // These variables store the important features of the response we'll send.
    Content feedback = null;                        // The feedback we send the user
    boolean responseCorrect = false;                // Whether we're right or wrong

    if (null == graphSketcherQuestion.getChoices() || graphSketcherQuestion.getChoices().isEmpty()) {
      log.error("Question does not have any answers. " + question.getId() + " src: "
          + question.getCanonicalSourceFile());

      feedback = new Content("This question does not have any correct answers");
    }

    // STEP 1: Did they provide an answer at all?

    GraphAnswer graphAnswer = null;

    if (null == feedback && (null == answer.getValue() || answer.getValue().isEmpty())) {
      feedback = new Content("You did not provide an answer");
    } else {
      try {
        graphAnswer = OBJECT_MAPPER.readValue(answer.getValue(), GraphAnswer.class);
      } catch (IOException e) {
        log.error("Expected a GraphAnswer, but couldn't parse it for question id: "
            + graphSketcherQuestion.getId(), e);
        feedback = new Content("Your graph could not be read");
      }
    }

    // STEP 2: If they did, does their answer match a known answer?

    if (null == feedback) {

      Input input = ANSWER_TO_INPUT.apply(graphAnswer);

      List<Choice> orderedChoices = getOrderedChoices(graphSketcherQuestion.getChoices());

      // For all the choices on this question...
      for (Choice c : orderedChoices) {

        // ... that are of the GraphChoice type, ...
        if (!(c instanceof GraphChoice)) {
          log.error("Isaac GraphSketcher Validator for questionId: " + graphSketcherQuestion.getId()
              + " expected there to be a GraphChoice . Instead it found a Choice.");
          continue;
        }
        GraphChoice graphChoice = (GraphChoice) c;

        if (null == graphChoice.getGraphSpec() || graphChoice.getGraphSpec().isEmpty()) {
          log.error("Expected a spec to match, but none found in choice for question id: "
              + graphSketcherQuestion.getId());
          continue;
        }

        Features.Matcher matcher = FEATURES.matcher(graphChoice.getGraphSpec());

        if (matcher.test(input)) {
          feedback = (Content) graphChoice.getExplanation();
          responseCorrect = graphChoice.isCorrect();
          break;
        }
      }
    }

    // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
    if (feedbackIsNullOrEmpty(feedback) && null != graphSketcherQuestion.getDefaultFeedback()) {
      feedback = graphSketcherQuestion.getDefaultFeedback();
    }

    return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
  }

  /**
   * Create a new list of Choice objects, with no change in sort order.
   * <br>
   * This is desirable if you ever need to use a feature in a negative way to identify an incorrect answer.
   *
   * @param choices - the Choices from a Question
   * @return the ordered list of Choices
   */
  @Override
  public List<Choice> getOrderedChoices(final List<Choice> choices) {
    return Lists.newArrayList(choices);
  }

  @Override
  public String createSpecification(final Choice answer) throws ValidatorUnavailableException {
    if (!(answer instanceof GraphChoice)) {
      log.error("Isaac GraphSketcher specifier expected there to be a GraphChoice . Instead it found a Choice.");
      throw new ValidatorUnavailableException("Incorrect choice type");
    }
    GraphChoice graphChoice = (GraphChoice) answer;

    GraphAnswer graphAnswer = null;

    try {
      graphAnswer = OBJECT_MAPPER.readValue(answer.getValue(), GraphAnswer.class);
    } catch (IOException e) {
      log.error("Expected a GraphAnswer, but couldn't parse it for specification", e);
      throw new ValidatorUnavailableException("Couldn't parse your GraphAnswer");
    }

    Input input = ANSWER_TO_INPUT.apply(graphAnswer);

    return FEATURES.generate(input);
  }
}
