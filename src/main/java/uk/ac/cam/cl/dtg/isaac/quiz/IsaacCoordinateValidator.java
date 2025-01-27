package uk.ac.cam.cl.dtg.isaac.quiz;

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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

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

        boolean shouldValidateWithSigFigs = null != coordinateQuestion.getSignificantFiguresMin() && null != coordinateQuestion.getSignificantFiguresMax();

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

        // Check if any coordinates are missing
        if (submittedItems.stream().anyMatch(i -> null == i.getCoordinates() || i.getCoordinates().size() != coordinateQuestion.getNumberOfDimensions())) {
            feedback = new Content("You did not provide the expected number of dimensions in your answer.");
        }

        if (null != coordinateQuestion.getNumberOfCoordinates() && submittedItems.size() != coordinateQuestion.getNumberOfCoordinates()) {
            feedback = new Content("You did not provide the correct number of coordinates.");
        }

        // STEP 2: If they did, does their answer match a known answer?

        if (null == feedback) {
            // Sort the choices so that we match incorrect choices last, giving precedence to correct ones.
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
                if (coordinateChoice.getItems().size() != submittedItems.size()) {
                    // We know that we don't have a match if the number of items is different.
                    continue;
                }

                // Ensure that all items in the choice are CoordinateItems.
                boolean allCoordinateItems = coordinateChoice.getItems().stream().allMatch(i -> i instanceof CoordinateItem);
                if (!allCoordinateItems) {
                    log.error("Expected list of CoordinateItems, but found different type in choice for question ({})", coordinateQuestion.getId());
                    // Hopefully another choice will be better...
                    continue;
                }
                List<CoordinateItem> choiceItems = coordinateChoice.getItems().stream().map(i -> (CoordinateItem) i).collect(Collectors.toList());

                // Check that the items in the submitted answer match the items in the choice numerically

                // If the question is unordered, then we need to (paradoxically) order the items to compare them.
                if (null == coordinateQuestion.getOrdered() || !coordinateQuestion.getOrdered()) {
                    choiceItems = orderCoordinates(choiceItems);
                    submittedItems = orderCoordinates(submittedItems);
                }

                boolean allItemsMatch = true;

                // For each coordinate in the list of coordinates:
                //    (labelled loop to allow short circuiting)
                outerloop: for (int coordIndex = 0; coordIndex < choiceItems.size(); coordIndex++) {
                    CoordinateItem choiceItem = choiceItems.get(coordIndex);
                    CoordinateItem submittedItem = submittedItems.get(coordIndex);
                    // Check that each dimension has the same coordinate value as the choice:
                    for (int dimensionIndex = 0; dimensionIndex < coordinateQuestion.getNumberOfDimensions(); dimensionIndex++) {
                        String choiceValue = choiceItem.getCoordinates().get(dimensionIndex);
                        String submittedValue = submittedItem.getCoordinates().get(dimensionIndex);

                        boolean valuesMatch = false;

                        if (submittedValue.isEmpty()) {
                            feedback = new Content(FEEDBACK_INCOMPLETE_ANSWER);
                        } else if (shouldValidateWithSigFigs) {
                            Integer sigFigs = ValidationUtils.numberOfSignificantFiguresToValidateWith(submittedValue,
                                    coordinateQuestion.getSignificantFiguresMin(), coordinateQuestion.getSignificantFiguresMax(), log);

                            boolean tooFewSF = ValidationUtils.tooFewSignificantFigures(submittedValue, coordinateQuestion.getSignificantFiguresMin(), log);
                            boolean tooManySF = ValidationUtils.tooManySignificantFigures(submittedValue, coordinateQuestion.getSignificantFiguresMax(), log);
                            if (tooFewSF || tooManySF) {
                                feedback = new Content("Whether your answer is correct or not, at least one value has the wrong number of significant figures.");
                            } else {
                                valuesMatch = ValidationUtils.numericValuesMatch(choiceValue, submittedValue, sigFigs, log);
                            }
                        } else {
                            valuesMatch = ValidationUtils.numericValuesMatch(choiceValue, submittedValue, null, log);
                        }

                        if (!valuesMatch) {
                            allItemsMatch = false;
                            // Exit early on mismatch:
                            break outerloop;
                        }
                    }
                }

                if (allItemsMatch) {
                    responseCorrect = coordinateChoice.isCorrect();
                    feedback = (Content) coordinateChoice.getExplanation();
                    break;
                }
            }
        }

        // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
        if (feedbackIsNullOrEmpty(feedback) && null != coordinateQuestion.getDefaultFeedback()) {
            feedback = coordinateQuestion.getDefaultFeedback();
        }

        return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
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
}
