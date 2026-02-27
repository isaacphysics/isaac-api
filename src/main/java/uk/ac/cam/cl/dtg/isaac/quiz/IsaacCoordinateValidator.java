package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCoordinateQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.CoordinateChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.CoordinateItem;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.isaac.quiz.IsaacNumericValidator.DEFAULT_VALIDATION_RESPONSE;

public class IsaacCoordinateValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacCoordinateValidator.class);

    @Override
    public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
        Objects.requireNonNull(question);
        Objects.requireNonNull(answer);

        if (!(question instanceof IsaacCoordinateQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with IsaacCoordinateQuestion (%s is not IsaacCoordinateQuestion)", question.getId()));
        }

        if (!(answer instanceof CoordinateChoice)) {
            throw new IllegalArgumentException(String.format(
                    "Expected CoordinateChoice for IsaacCoordinateQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
        }

        // These variables store the important features of the response we'll send.
        Content feedback = null;                        // The feedback we send the user
        boolean responseCorrect = false;                // Whether we're right or wrong

        IsaacCoordinateQuestion coordinateQuestion = (IsaacCoordinateQuestion) question;
        CoordinateChoice submittedChoice = (CoordinateChoice) answer;

        Integer sigFigsMin = coordinateQuestion.getSignificantFiguresMin();
        Integer sigFigsMax = coordinateQuestion.getSignificantFiguresMax();

        // STEP 0: Is it even possible to answer this question?

        if (null == coordinateQuestion.getChoices() || coordinateQuestion.getChoices().isEmpty()) {
            log.error("Question ({}) does not have any answers. src: {}", question.getId(), question.getCanonicalSourceFile());
            feedback = new Content(FEEDBACK_NO_CORRECT_ANSWERS);
        }

        if (null == coordinateQuestion.getNumberOfDimensions() || coordinateQuestion.getNumberOfDimensions() < 1) {
            log.error("Question ({}) does not have dimensions set. src: {}", question.getId(), question.getCanonicalSourceFile());
            feedback = new Content("This question cannot be answered correctly.");
        }

        // STEP 1: Did they provide a valid answer?

        if (null == feedback && (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty())) {
            feedback = new Content(FEEDBACK_NO_ANSWER_PROVIDED);
        }

        // Check that all the items in the submitted answer are CoordinateItems.
        for (Object item : submittedChoice.getItems()) {
            if (!(item instanceof CoordinateItem)) {
                log.warn("User submitted unexpected item type for coordinate question ({}).", coordinateQuestion.getId());
                feedback = new Content(FEEDBACK_UNRECOGNISED_ITEMS);
                break;
            }
        }

        if (feedback != null)  {
            return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
        }

        // Then cast the submitted items to CoordinateItems.
        List<CoordinateItem> submittedItems = submittedChoice.getItems().stream().map(i -> (CoordinateItem) i).collect(Collectors.toList());

        // Check if any coordinates are invalid or missing:
        if (submittedItems.stream().anyMatch(i -> null == i.getCoordinates() || i.getCoordinates().size() != coordinateQuestion.getNumberOfDimensions())) {
            feedback = new Content("You did not provide the expected number of dimensions in your answer.");
        }

        if (submittedItems.stream().anyMatch(i -> null == i.getCoordinates() || i.getCoordinates().stream().anyMatch(String::isEmpty))) {
            feedback = new Content(FEEDBACK_INCOMPLETE_ANSWER);
        }

        if (null != coordinateQuestion.getNumberOfCoordinates() && submittedItems.size() != coordinateQuestion.getNumberOfCoordinates()) {
            feedback = new Content("You did not provide the correct number of coordinates.");
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {
            // Sort the choices so that strict match choices are checked before subset match choices.
            // Within these categories, match incorrect choices last, giving precedence to correct ones.
            List<Choice> orderedChoices = getOrderedChoices(coordinateQuestion.getChoices());

            // For all the choices on this question...
            for (Choice c : orderedChoices) {

                // ... that are of the CoordinateChoice type, ...
                if (!(c instanceof CoordinateChoice)) {
                    log.error("Expected CoordinateChoice for question ({}). Instead found {}.", coordinateQuestion.getId(), c.getClass());
                    continue;
                }

                CoordinateChoice coordinateChoice = (CoordinateChoice) c;

                // ... and that contain items ...
                if (null == coordinateChoice.getItems() || coordinateChoice.getItems().isEmpty()) {
                    log.error("Expected list of CoordinateItems, but none found in choice for question ({})", coordinateQuestion.getId());
                    continue;
                }

                // ... look for a match to the submitted answer.

                // Ensure that all items in the choice are CoordinateItems.
                boolean allCoordinateItems = coordinateChoice.getItems().stream().allMatch(i -> i instanceof CoordinateItem);
                if (!allCoordinateItems) {
                    log.error("Expected list of CoordinateItems, but found different type in choice for question ({})", coordinateQuestion.getId());
                    // Hopefully another choice will be better...
                    continue;
                }
                List<CoordinateItem> choiceItems = coordinateChoice.getItems().stream().map(i -> (CoordinateItem) i).collect(Collectors.toList());

                // Check that the items in the submitted answer match the items in the choice numerically

                boolean allItemsMatch = false;
                boolean allItemsMatchWithoutSigFigs = false;
                try {
                    // If the question is unordered, then we need to (paradoxically) order the items to compare them.
                    if (null == coordinateQuestion.getOrdered() || !coordinateQuestion.getOrdered()) {
                        choiceItems = orderCoordinates(choiceItems);
                        submittedItems = orderCoordinates(submittedItems);
                    }

                    // Only attempt strict validation if the number of submitted items matches the choice
                    if (choiceItems.size() == submittedItems.size()) {
                        allItemsMatch = true;
                        allItemsMatchWithoutSigFigs = true;
                        // For each coordinate in the list of coordinates:
                        for (int coordIndex = 0; coordIndex < choiceItems.size(); coordIndex++) {
                            CoordinateItem choiceItem = choiceItems.get(coordIndex);
                            CoordinateItem submittedItem = submittedItems.get(coordIndex);
                            // Check that the submitted item matches the choice item
                            if (!coordinateItemsMatch(submittedItem, choiceItem, sigFigsMin, sigFigsMax, false)) {
                                allItemsMatch = false;
                                // Check if this is just a significant figures mismatch
                                if (!coordinateItemsMatch(submittedItem, choiceItem, sigFigsMin, sigFigsMax, true)) {
                                    allItemsMatchWithoutSigFigs = false;
                                    // Exit early on mismatch:
                                    break;
                                }
                            }
                        }
                    }

                    if (allItemsMatch) {
                        responseCorrect = coordinateChoice.isCorrect();
                        feedback = (Content) coordinateChoice.getExplanation();
                        break;
                    } else if (allItemsMatchWithoutSigFigs) {
                        feedback = new Content(DEFAULT_VALIDATION_RESPONSE);
                        // Too many sig figs, or too few sig figs but the choice has trailing zeros; otherwise correct
                        feedback.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_many")));
                        break;
                    }

                    // For unordered questions, if no strict match was found, check for a subset match in two ways:
                    if (null == coordinateQuestion.getOrdered() || !coordinateQuestion.getOrdered()) {

                        // For correct choices, check if the submitted items are a proper subset of the choice
                        if (coordinateChoice.isCorrect() && (choiceItems.size() > submittedItems.size())) {
                            boolean allSubmittedItemsInChoiceItems = true;
                            boolean allItemsInChoiceWithoutSigFigs = true;
                            for (CoordinateItem submittedItem : submittedItems) {
                                boolean submittedItemInChoiceItem = false;
                                boolean itemInChoiceWithoutSigFigs = false;
                                for (CoordinateItem choiceItem : choiceItems) {
                                    if (coordinateItemsMatch(submittedItem, choiceItem, sigFigsMin, sigFigsMax,
                                            false)) {
                                        submittedItemInChoiceItem = true;
                                        break;
                                    } else if (coordinateItemsMatch(submittedItem, choiceItem, sigFigsMin, sigFigsMax,
                                            true)) {
                                        itemInChoiceWithoutSigFigs = true;
                                    }
                                }
                                if (!submittedItemInChoiceItem) {
                                    allSubmittedItemsInChoiceItems = false;
                                    // Check if this is just a significant figures mismatch
                                    if (!itemInChoiceWithoutSigFigs) {
                                        allItemsInChoiceWithoutSigFigs = false;
                                        break;
                                    }
                                }
                            }
                            if (allSubmittedItemsInChoiceItems) {
                                feedback = new Content((submittedItems.size() == 1 ? "This is" : "These are")
                                        + " correct, but can you find more?");
                                feedback.setTags(new HashSet<>()); // Clear tags in case we previously set sig figs tags
                                break;
                            } else if (allItemsInChoiceWithoutSigFigs) {
                                feedback = new Content(DEFAULT_VALIDATION_RESPONSE);
                                feedback.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_many")));
                                // Don't break; we could find a better match
                            }
                        }

                        // If subset matching is allowed for this choice, check if the choice is a proper subset of the
                        // submitted items
                        boolean allowSubsetMatch = (null != coordinateChoice.isAllowSubsetMatch() && coordinateChoice.isAllowSubsetMatch());
                        if (null == feedback && allowSubsetMatch && (submittedItems.size() > choiceItems.size())) {
                            boolean allChoiceItemsInSubmittedItems = true;
                            boolean allItemsInSubmittedWithoutSigFigs = true;
                            for (CoordinateItem choiceItem : choiceItems) {
                                boolean choiceItemInSubmittedItems = false;
                                boolean itemInSubmittedWithoutSigFigs = false;
                                for (CoordinateItem submittedItem : submittedItems) {
                                    if (coordinateItemsMatch(submittedItem, choiceItem, sigFigsMin, sigFigsMax, false)) {
                                        choiceItemInSubmittedItems = true;
                                        break;
                                    } else if (coordinateItemsMatch(submittedItem, choiceItem, sigFigsMin, sigFigsMax, true)) {
                                        itemInSubmittedWithoutSigFigs = true;
                                    }
                                }
                                if (!choiceItemInSubmittedItems) {
                                    allChoiceItemsInSubmittedItems = false;
                                    // Check if this is just a significant figures mismatch
                                    if (!itemInSubmittedWithoutSigFigs) {
                                        allItemsInSubmittedWithoutSigFigs = false;
                                    }
                                    break;
                                }
                            }
                            if (allChoiceItemsInSubmittedItems) {
                                responseCorrect = coordinateChoice.isCorrect();
                                feedback = (Content) coordinateChoice.getExplanation();
                                feedback.setTags(new HashSet<>()); // Clear tags in case we previously set sig figs tags
                                break;
                            } else if (allItemsInSubmittedWithoutSigFigs) {
                                feedback = new Content(DEFAULT_VALIDATION_RESPONSE);
                                feedback.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_many")));
                                // Don't break; we could find a better match
                            }
                        }
                    }

                } catch (final NumberFormatException e) {
                    feedback = new Content(FEEDBACK_UNRECOGNISED_FORMAT);
                    break;
                }
            }
        }

        // STEP 3: If incorrect and we still have no feedback to give, use the question's default feedback if any to use:
        if (!responseCorrect && feedbackIsNullOrEmpty(feedback)) {
            if (null != coordinateQuestion.getDefaultFeedback()) {
                feedback = coordinateQuestion.getDefaultFeedback();
            } else {
                // If there was no default feedback, check for too few significant figures
                if (submittedItems.stream().anyMatch(i -> i.getCoordinates().stream()
                        .anyMatch(c -> ValidationUtils.tooFewSignificantFigures(c, sigFigsMin, log)))) {
                    feedback = new Content(DEFAULT_VALIDATION_RESPONSE);
                    feedback.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_few")));
                }
            }
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
    }

    private boolean coordinateItemsMatch(final CoordinateItem submittedItem, final CoordinateItem choiceItem,
                                         final Integer sigFigsMin, final Integer sigFigsMax,
                                         final boolean allowTooManySigFigs) {

        if (submittedItem.getCoordinates().size() != choiceItem.getCoordinates().size()) {
            return false;
        }

        for (int dimension = 0; dimension < submittedItem.getCoordinates().size(); dimension++) {
            String submittedValue = submittedItem.getCoordinates().get(dimension);
            String choiceValue = choiceItem.getCoordinates().get(dimension);

            if (allowTooManySigFigs) {
                // Check if the submission has more significant figures than the allowed maximum
                if (null != sigFigsMax && ValidationUtils.tooManySignificantFigures(submittedValue, sigFigsMax, log)) {
                    // Check if the submission would match the choice if we ignore the excess significant figures
                    return ValidationUtils.numericValuesMatch(choiceValue, submittedValue, sigFigsMax, log);
                }
            }

            int sigFigs = ValidationUtils.numberOfSignificantFiguresToValidateWith(submittedValue, sigFigsMin, sigFigsMax, log);
            if (!ValidationUtils.numericValuesMatch(choiceValue, submittedValue, sigFigs, log)
                    || null != sigFigsMin && ValidationUtils.tooFewSignificantFigures(submittedValue, sigFigsMin, log)
                        || null != sigFigsMax && ValidationUtils.tooManySignificantFigures(submittedValue, sigFigsMax, log)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Numerically order CoordinateItems in a list by first-coordinate, then by second-coordinate, etc.
     *
     * This allows for comparisons where the coordinates do not need to be provided in a specified order,
     * since this imposes a consistent order.
     *
     * @param items     The list of CoordinateItems to order.
     *
     * @return          The (numerically) ordered list of coordinate items
     */
    List<CoordinateItem> orderCoordinates(final List<CoordinateItem> items) {
        return items.stream().sorted((a, b) -> {
            int numDimensions = Math.min(a.getCoordinates().size(), b.getCoordinates().size());
            for (int i = 0; i < numDimensions; i++) {
                String valueA = a.getCoordinates().get(i);
                String valueB = b.getCoordinates().get(i);
                if (valueA.isEmpty() || valueB.isEmpty()) {
                    return valueA.compareTo(valueB);
                }
                if (!ValidationUtils.compareNumericValues(valueA, valueB, 3, ValidationUtils.ComparisonType.EQUAL_TO, log)) {
                    return ValidationUtils.compareNumericValues(valueA, valueB, 3, ValidationUtils.ComparisonType.LESS_THAN, log) ? -1 : 1;
                }
            }
            return 0;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Choice> getOrderedChoices(final List<Choice> choices) {
        return IsaacItemQuestionValidator.getOrderedChoicesWithSubsets(choices);
    }
}
