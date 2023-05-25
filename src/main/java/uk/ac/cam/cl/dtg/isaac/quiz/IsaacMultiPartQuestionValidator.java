package uk.ac.cam.cl.dtg.isaac.quiz;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiPartQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.MultiPartAnswer;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;

public class IsaacMultiPartQuestionValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacMultiPartQuestionValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Validate.notNull(question);
        Validate.notNull(answer);

        if (!(question instanceof IsaacMultiPartQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacMultiPartQuestion (%s is not MultiPartQuestion)", question.getId()));
        }

        if (!(answer instanceof MultiPartAnswer)) {
            throw new IllegalArgumentException(String.format(
                    "Expected MultiPartChoice for IsaacMultiPartQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        IsaacMultiPartQuestion multiPartQuestion = (IsaacMultiPartQuestion) question;
        MultiPartAnswer submittedChoice = (MultiPartAnswer) answer;

        // TODO

        // First, order the possible choices by correctness (might get more complicated with blank answers?)

        // Then, for each choice...

        // ... if the question is ordered, simply populate the choice template with the choice for each position.

        // ... if the question is unordered, for each template position, check if the provided answer in that position
        // matches any in the category for that position. If all positions get a match, the answer corresponds to that
        // choice. If the question does not allow duplicates, then a matched item in the category cannot be used again
        // (should be removed from the category for the rest of the checking).

        // There might be a way to check unordered questions more efficiently by sorting, but this would have to take
        // into account the fact that each category could have more items than the question has positions for that
        // category.

        return new QuestionValidationResponse(question.getId(), answer, false, null, new Date());
    }
}
