package uk.ac.cam.cl.dtg.isaac.quiz;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiPartQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.MultiPartValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.ContentBase;
import uk.ac.cam.cl.dtg.isaac.dos.content.MultiPartChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        if (!(answer instanceof MultiPartChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected MultiPartChoice for IsaacMultiPartQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        IsaacMultiPartQuestion multiPartQuestion = (IsaacMultiPartQuestion) question;
        MultiPartChoice submittedChoice = (MultiPartChoice) answer;

        Map<String, Choice> choiceMap = submittedChoice.getChoices().stream()
                .filter(c -> c instanceof Choice)
                .map(c -> Map.entry(c.getId(), (Choice) c))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));

        List<QuestionValidationResponse> validationResponses = multiPartQuestion.getParts().stream().map((ContentBase innerQuestionContent) -> {
            if (!(innerQuestionContent instanceof Question)) {
                throw new IllegalArgumentException("Question part content objects in multi-part question to not extend Question superclass!");
            }
            Question innerQuestion = (Question) innerQuestionContent;
            Choice innerChoice = choiceMap.get(innerQuestion.getId());
            IValidator validator = IValidator.locateValidator(innerQuestion.getClass());
            if (null == validator || null == innerChoice) {
                log.error("Unable to locate a valid validator for this question " + question.getId());
                return new QuestionValidationResponse(innerQuestion.getId(), null, false, new Content("Incorrect!"), new Date());
            } else {
                try {
                    return validator.validateQuestionResponse(innerQuestion, innerChoice);
                } catch (ValidatorUnavailableException e) {
                    return new QuestionValidationResponse(innerQuestion.getId(), null, false, new Content("Problem validating!"), new Date());
                }
            }
        }).collect(Collectors.toList());

        Boolean allPartsCorrect = validationResponses.stream()
                .map(QuestionValidationResponse::isCorrect)
                .reduce(Boolean::logicalAnd)
                .orElse(false);

        // Create composite feedback based on feedback for inner questions. Only supports inner validation responses
        // where the explanation content is value, not children.
        String feedbackMessage = IntStream.range(0, validationResponses.size())
                .mapToObj(i -> {
                    if (null == validationResponses.get(i).getExplanation() || null == validationResponses.get(i).getExplanation().getValue()) {
                        return Map.entry(i + 1, "");
                    }
                    return Map.entry(i + 1, validationResponses.get(i).getExplanation().getValue());
                })
                .reduce(Map.entry(0, ""), (acc, c) -> {
                    if (c.getValue().isEmpty()) {
                        return acc;
                    }
                    return Map.entry(0, String.format("%s\n%d. %s", acc.getValue(), c.getKey(), c.getValue()));
                }).getValue();
        Content feedback = new Content(feedbackMessage);

        return new MultiPartValidationResponse(question.getId(), answer, allPartsCorrect,
                feedback, new Date(), validationResponses);
    }
}
