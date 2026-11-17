package uk.ac.cam.cl.dtg.isaac.quiz;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacCompletionQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;

/**
 * Validator for IsaacCompletionQuestions: it's just a checkbox, so it doesn't mark anything.
 * Only thing it does is check if another type of question was provided.
 */
public class IsaacCompletionQuestionValidator implements IValidator {

    @Override
    public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {

        if (!(question instanceof IsaacCompletionQuestion)) {
            throw new IllegalArgumentException(question.getId() + " is not a completion question");
        }

        return new QuestionValidationResponse(question.getId(), answer, true, question.getDefaultFeedback(), new Date());

    }
}