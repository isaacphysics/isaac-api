package uk.ac.cam.cl.dtg.isaac.quiz;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiPartQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacQuestionBase;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacStringMatchQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.MultiPartValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.MultiPartAnswer;
import uk.ac.cam.cl.dtg.isaac.dos.content.MultiPartChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

        if (!(answer instanceof MultiPartAnswer)) {
            throw new IllegalArgumentException(String.format(
                    "Expected MultiPartChoice for IsaacMultiPartQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        IsaacMultiPartQuestion multiPartQuestion = (IsaacMultiPartQuestion) question;
        MultiPartAnswer multiPartAnswer = (MultiPartAnswer) answer;

        if (null == multiPartQuestion.getChoices() || multiPartQuestion.getChoices().isEmpty()) {
            throw new IllegalArgumentException(String.format(
                    "IsaacMultiPartQuestion %s does not have any correct answers, so it cannot be answered", question.getId()));
        }

        // First, order the possible choices by correctness (might get more complicated with blank answers?)

        List<Choice> orderedChoices = getOrderedChoices(multiPartQuestion.getChoices());

        // Then, for each choice...
        for (Choice choice : orderedChoices) {
            // Check that choice is valid for this question type
            if (!(choice instanceof MultiPartChoice)) {
                log.error(String.format("IsaacMultiPartQuestion %s contains a choice of class %s, should be a multiPartChoice!", question.getId(), choice.getClass().getName()));
                continue;
            }

            // TODO Make sure that for each category specified in the template, the set of items corresponding to that
            //  category has length greater than or equal to the number of positions that category occurs in the template

            MultiPartChoice multiPartChoice = (MultiPartChoice) choice;

            List<QuestionValidationResponse> validationResponses;

            if (multiPartQuestion.getOrdered()) {
                // ... if the question is ordered, simply populate the choice template with the choice for each position.
                List<Choice> populatedChoice = multiPartChoice.getChoiceTemplate().stream().map(n -> {
                    // TODO handle multiple items in each category? I don't know what kind of question this would correspond to?
                    // Just take the first of each category - the check earlier guarantees that this is defined.
                    return multiPartChoice.getItemCategories().get(n).get(0);
                }).collect(Collectors.toList());

                validationResponses = IntStream.range(0, populatedChoice.size())
                    .mapToObj(i -> {
                        // FIXME unsafe cast...?
                        IsaacQuestionBase subQuestion = (IsaacQuestionBase) multiPartQuestion.getParts().get(i);
                        Choice subChoice = populatedChoice.get(i);
                        Choice answerSubChoice = multiPartAnswer.getAnswers().get(i);
                        return this.validateWithSingleChoice(subQuestion, subChoice, answerSubChoice);
                    })
                    .collect(Collectors.toList());
            } else {
                // ... if the question is unordered, for each template position, check if the provided answer in that position
                // matches any in the category for that position. If all positions get a match, the answer corresponds to that
                // choice. If the question does not allow duplicates, then a matched item in the category cannot be used again
                // (should be removed from the category for the rest of the checking).

                // Create a deep copy of the itemCategories list, so that we can remove items from it.
                List<List<Choice>> itemCategories = multiPartChoice.getItemCategories().stream().map(ArrayList::new).collect(Collectors.toList());

                validationResponses = IntStream.range(0, multiPartChoice.getChoiceTemplate().size())
                    .mapToObj(i -> {
                        Integer category = multiPartChoice.getChoiceTemplate().get(i);

                        IsaacQuestionBase subQuestion = (IsaacQuestionBase) multiPartQuestion.getParts().get(i);
                        List<Choice> subChoicesForThisPosition = itemCategories.get(category);
                        Choice answerSubChoice = multiPartAnswer.getAnswers().get(i);

                        // Iterate over the options for this position, and check if any match the answer
                        Iterator<Choice> subChoiceIterator = subChoicesForThisPosition.iterator();
                        while (subChoiceIterator.hasNext()) {
                            Choice subChoice = subChoiceIterator.next();
                            QuestionValidationResponse subValidation = this.validateWithSingleChoice(subQuestion, subChoice, answerSubChoice);
                            if (subValidation.isCorrect()) {
                                if (!multiPartQuestion.getAllowDuplicates()) {
                                    // If duplicate choices are not allowed, remove the choice from the list of options for this position
                                    subChoiceIterator.remove();
                                }
                                return subValidation;
                            }
                        }

                        // None match, so return a validation response indicating this
                        return new QuestionValidationResponse(subQuestion.getId(), answerSubChoice, false, null, new Date());
                    })
                    .collect(Collectors.toList());
            }
            boolean allCorrect = validationResponses.stream().allMatch(QuestionValidationResponse::isCorrect);
            return new MultiPartValidationResponse(multiPartQuestion.getId(), multiPartAnswer, allCorrect, null, new Date(), validationResponses);
        }

        // There might be a way to check unordered questions more efficiently by sorting, but this would have to take
        // into account the fact that each category could have more items than the question has positions for that
        // category.

        return new QuestionValidationResponse(question.getId(), answer, false, null, new Date());
    }

    /**
     * Injects a single choice into a question template, and validates an answer against it using the validator
     * subclass that corresponds to the question type.
     *
     * @param questionTemplate - question object to inject the given choice into (for validation)
     * @param choice - choice to compare to the given answer
     * @param answer - the users answer (should be same subclass of Choice as choice)
     * @return a validation response that indicates whether the choice given matches the answer
     */
    private QuestionValidationResponse validateWithSingleChoice(final IsaacQuestionBase questionTemplate, final Choice choice, final Choice answer) {
        // Create question object from the given "template" (we only care about the id and type of the question)

        if (!choice.getType().equals(answer.getType())) {
            return new QuestionValidationResponse(
                    questionTemplate.getId(),
                    answer,
                    false,
                    new Content("Submitted answer is not compatible with the choice type specified in the question."),
                    new Date()
            );
        }

        // A bit hacky - we need to instantiate a question object from the template, but we don't know what subclass
        // of IsaacQuestionBase it is. We can't use the template directly because we would mutate it.
        IsaacQuestionBase question;
        if (questionTemplate instanceof IsaacNumericQuestion) {
            IsaacNumericQuestion numericQuestion = new IsaacNumericQuestion();
            nu
            question = numericQuestion;
        } else if (questionTemplate instanceof IsaacSymbolicQuestion) {
            question = new IsaacSymbolicQuestion();
        } else if (questionTemplate instanceof IsaacStringMatchQuestion) {
            question = new IsaacStringMatchQuestion();
        } else {
            // Error
            throw new IllegalArgumentException("Unsupported question subclass for IsaacMultiPartQuestion: " + questionTemplate.getClass().getName());
        }
        question.setId(questionTemplate.getId());
        question.setType(questionTemplate.getType());
        question.setChoices(List.of(choice));
        question.setDefaultFeedback(questionTemplate.getDefaultFeedback());


        IValidator validator = IValidator.locateValidator(question.getClass());
        if (null == validator) {
            log.error("Unable to locate a valid validator for this question " + question.getId());
            return new QuestionValidationResponse(question.getId(), null, false, new Content("Problem validating!"), new Date());
        } else {
            try {
                return validator.validateQuestionResponse(question, answer);
            } catch (ValidatorUnavailableException e) {
                return new QuestionValidationResponse(question.getId(),
                    answer,
                    false,
                    new Content("Problem validating!"),
                    new Date()
                );
            }
        }
    }
}
