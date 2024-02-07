package uk.ac.cam.cl.dtg.isaac.quiz;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacInlineQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.InlineChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.List;

public class IsaacInlineValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacCoordinateValidator.class);

    @Override
    public QuestionValidationResponse validateQuestionResponse(Question question, Choice answer) throws ValidatorUnavailableException {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacInlineQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacInlineQuestion (%s is not IsaacInlineQuestion)", question.getId()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        IsaacInlineQuestion inlineQuestion = (IsaacInlineQuestion) question;
        InlineChoice userAnswer = (InlineChoice) answer;

        // STEP 0: Is it even possible to answer this question?

        if (null == inlineQuestion.getChoices() || inlineQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());
            feedback = new Content("This question does not have any correct answers!");
        }
        List<IsaacStringMatchQuestion> subQuestions = inlineQuestion.getInlineQuestions();
        IsaacStringMatchValidator stringMatchValidator = new IsaacStringMatchValidator();
        for (IsaacStringMatchQuestion subQuestion : subQuestions) {
            stringMatchValidator.validateQuestionResponse(subQuestion, answer);
        }

        // STEP 1: Did they provide an answer at all?

        if (null == feedback && (null == userAnswer.getValue() || userAnswer.getValue().isEmpty())) {
            feedback = new Content("You did not provide an answer");
        }

        // TODO: is the answer given correct?

        return new QuestionValidationResponse(question.getId(), userAnswer, responseCorrect, feedback, new Date());
    }
}
