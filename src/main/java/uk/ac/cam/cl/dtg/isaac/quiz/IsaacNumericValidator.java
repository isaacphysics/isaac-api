/*
 * Copyright 2014 Stephen Cummins, 2017 James Sharkey
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Quantity;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES;


/**
 * Validator that only provides functionality to validate Numeric questions.
 */
public class IsaacNumericValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);

    protected static final String DEFAULT_VALIDATION_RESPONSE = "Check your working.";
    protected static final String DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE = "Check your units.";
    protected static final String DEFAULT_NO_ANSWER_VALIDATION_RESPONSE = "You did not provide an answer.";
    protected static final String DEFAULT_NO_UNIT_VALIDATION_RESPONSE = "You did not choose any units. To give an answer with no units, select \"None\".";
    private static final String INVALID_NEGATIVE_STANDARD_FORM = ".*?10-([0-9]+).*?";

    @Override
    public final QuestionValidationResponse validateQuestionResponse(
            final Question question, final Choice answer) {

        if (!(question instanceof IsaacNumericQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Numeric Questions... (%s is not numeric)", question.getId()));
        }

        if (!(answer instanceof Quantity)) {
            throw new IllegalArgumentException(String.format(
                    "Expected Quantity for IsaacNumericQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacNumericQuestion isaacNumericQuestion = (IsaacNumericQuestion) question;
        Quantity answerFromUser = (Quantity) answer;

        // Extract significant figure bounds, defaulting to NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES either are missing
        int significantFiguresMax = Objects.requireNonNullElse(isaacNumericQuestion.getSignificantFiguresMax(), NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES);
        int significantFiguresMin = Objects.requireNonNullElse(isaacNumericQuestion.getSignificantFiguresMin(), NUMERIC_QUESTION_DEFAULT_SIGNIFICANT_FIGURES);

        log.debug("Starting validation of '" + answerFromUser.getValue() + " " + answerFromUser.getUnits() + "' for '"
                + isaacNumericQuestion.getId() + "'");

        // check there are no obvious issues with the question (e.g. no correct answers, nonsensical sig fig requirements)
        if (null == isaacNumericQuestion.getChoices() || isaacNumericQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            return new QuantityValidationResponse(question.getId(), answerFromUser, false,
                            new Content("This question does not have any correct answers"),
                            false, false, new Date());
        }

        // Only worry about broken significant figure rules if we are going to use them (i.e. if "exact match" is false)
        if (!isaacNumericQuestion.getDisregardSignificantFigures()) {
            if (significantFiguresMin < 1 || significantFiguresMax < 1
                    || significantFiguresMax < significantFiguresMin) {
                log.error("Question has broken significant figure rules! " + question.getId() + " src: "
                        + question.getCanonicalSourceFile());

                return new QuantityValidationResponse(question.getId(), answerFromUser, false,
                        new Content("This question cannot be answered correctly."),
                        false, false, new Date());
            }
        }

        try {
            // Should this answer include units?
            boolean shouldValidateWithUnits = isaacNumericQuestion.getRequireUnits();
            if (shouldValidateWithUnits && null != isaacNumericQuestion.getDisplayUnit() && !isaacNumericQuestion.getDisplayUnit().isEmpty()) {
                log.warn(String.format("Question has inconsistent units settings, overriding requiresUnits: %s! src: %s",
                        question.getId(), question.getCanonicalSourceFile()));
                shouldValidateWithUnits = false;
            }

            if (null == answerFromUser.getValue() || answerFromUser.getValue().isEmpty()) {
                return new QuantityValidationResponse(question.getId(), answerFromUser, false,
                        new Content(DEFAULT_NO_ANSWER_VALIDATION_RESPONSE), false, false, new Date());
            } else if (null == answerFromUser.getUnits() && shouldValidateWithUnits) {
                return new QuantityValidationResponse(question.getId(), answerFromUser, false,
                        new Content(DEFAULT_NO_UNIT_VALIDATION_RESPONSE), null, false, new Date());
            }

            QuantityValidationResponse bestResponse;

            // Step 1 - Do correct answer numeric equivalence checking.
            if (shouldValidateWithUnits) {
                bestResponse = this.validateWithUnits(isaacNumericQuestion, answerFromUser);
            } else {
                bestResponse = this.validateWithoutUnits(isaacNumericQuestion, answerFromUser);
            }

            // If incorrect and we have not used the default validation response then go ahead and return it 
            // - this provides more helpful feedback than sig figs errors.
            if (!bestResponse.isCorrect() && bestResponse.getExplanation() != null
                    && !(DEFAULT_VALIDATION_RESPONSE.equals(bestResponse.getExplanation().getValue())
                    || DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE
                    .equals(bestResponse.getExplanation().getValue()))) {
                return useDefaultFeedbackIfNecessary(isaacNumericQuestion, bestResponse);
            }

            // Step 2 - do sig fig checking (unless specified otherwise by question):
            if (!isaacNumericQuestion.getDisregardSignificantFigures()) {
                if (tooFewSignificantFigures(answerFromUser.getValue(), significantFiguresMin)) {
                    // If too few sig figs then give feedback about this.

                    // If we have unit information available put it in our response.
                    Boolean validUnits = null;
                    if (isaacNumericQuestion.getRequireUnits()) {
                        // Whatever the current bestResponse is, it contains all we need to know about the units:
                        validUnits = bestResponse.getCorrectUnits();
                    }
                    // Our new bestResponse is about incorrect significant figures:
                    Content sigFigResponse = new Content(DEFAULT_VALIDATION_RESPONSE);
                    sigFigResponse.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_few")));
                    bestResponse = new QuantityValidationResponse(question.getId(), answerFromUser, false, sigFigResponse,
                            false, validUnits, new Date());
                }
                if (tooManySignificantFigures(answerFromUser.getValue(), significantFiguresMax)
                        && bestResponse.isCorrect()) {
                    // If (and only if) _correct_, but to too many sig figs, give feedback about this.

                    // If we have unit information available put it in our response.
                    Boolean validUnits = null;
                    if (isaacNumericQuestion.getRequireUnits()) {
                        // Whatever the current bestResponse is, it contains all we need to know about the units:
                        validUnits = bestResponse.getCorrectUnits();
                    }
                    // Our new bestResponse is about incorrect significant figures:
                    Content sigFigResponse = new Content(DEFAULT_VALIDATION_RESPONSE);
                    sigFigResponse.setTags(new HashSet<>(ImmutableList.of("sig_figs", "sig_figs_too_many")));
                    bestResponse = new QuantityValidationResponse(question.getId(), answerFromUser, false, sigFigResponse,
                            false, validUnits, new Date());
                }
            }

            // And then return the bestResponse:
            log.debug("Finished validation: correct=" + bestResponse.isCorrect() + ", correctValue="
                    + bestResponse.getCorrectValue() + ", correctUnits=" + bestResponse.getCorrectUnits());
            return useDefaultFeedbackIfNecessary(isaacNumericQuestion, bestResponse);
        } catch (NumberFormatException e) {
            log.debug("Validation failed for '" + answerFromUser.getValue() + " " + answerFromUser.getUnits() + "': "
                    + "cannot parse as number!");
            HashSet<String> responseTags = new HashSet<>(ImmutableList.of("unrecognised_format"));
            if (answerFromUser.getValue().matches(INVALID_NEGATIVE_STANDARD_FORM)) {
                responseTags.add("invalid_std_form");
            }
            Content invalidFormatResponse = new Content("Your answer is not in a format we recognise, please enter your answer as a decimal number.");
            invalidFormatResponse.setTags(responseTags);
            return new QuantityValidationResponse(question.getId(), answerFromUser, false, invalidFormatResponse,
                    false, false, new Date());
        }
    }

    /**
     * Numerically validate the students answer ensuring that the correct unit value is specified.
     *
     * @param isaacNumericQuestion - question to validate.
     * @param answerFromUser       - answer from user
     * @return the validation response
     */
    private QuantityValidationResponse validateWithUnits(final IsaacNumericQuestion isaacNumericQuestion,
                                                         final Quantity answerFromUser) {
        log.debug("\t[validateWithUnits]");
        QuantityValidationResponse bestResponse = null;
        Integer sigFigsToValidateWith = null;

        if (!isaacNumericQuestion.getDisregardSignificantFigures()) {
            sigFigsToValidateWith = ValidationUtils.numberOfSignificantFiguresToValidateWith(
                    answerFromUser.getValue(),
                    isaacNumericQuestion.getSignificantFiguresMin(),
                    isaacNumericQuestion.getSignificantFiguresMax(),
                    log
            );
        }

        String unitsFromUser = answerFromUser.getUnits().trim();

        List<Choice> orderedChoices = getOrderedChoices(isaacNumericQuestion.getChoices());
        for (Choice c : orderedChoices) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                if (quantityFromQuestion.getUnits() == null) {
                    log.error("Expected units and no units can be found for question id: " + isaacNumericQuestion.getId());
                    continue;
                }

                String unitsFromChoice = quantityFromQuestion.getUnits().trim();
                String quantityFromChoice = quantityFromQuestion.getValue().trim();

                boolean numericValuesMatched = ValidationUtils.numericValuesMatch(
                        quantityFromChoice,
                        answerFromUser.getValue(),
                        sigFigsToValidateWith,
                        log
                );

                // What sort of match do we have:
                if (numericValuesMatched && unitsFromUser.equals(unitsFromChoice)) {
                    // Exact match: nothing else can do better, but previous match may tell us if units are also correct:
                    Boolean unitsCorrect = (null != bestResponse && bestResponse.getCorrectUnits()) || quantityFromQuestion.isCorrect();
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), unitsCorrect, new Date());
                    break;
                } else if (numericValuesMatched && !unitsFromUser.equals(unitsFromChoice) && quantityFromQuestion.isCorrect()) {
                    // Matches value but not units of a correct choice.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE), true, false, new Date());
                } else if (!numericValuesMatched && unitsFromUser.equals(unitsFromChoice) && quantityFromQuestion.isCorrect()) {
                    // Matches units but not value of a correct choice.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_VALIDATION_RESPONSE), false, true, new Date());
                }
            } else {
                log.error("Isaac Numeric Validator for questionId: " + isaacNumericQuestion.getId()
                        + " expected there to be a Quantity. Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // No matches; tell them they got it wrong but we cannot find any more detailed feedback for them.
            return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser, false, new Content(
                    DEFAULT_VALIDATION_RESPONSE), false, false, new Date());
        } else {
            return bestResponse;
        }
    }

    /**
     * Numerically validate the response without units being considered.
     *
     * @param isaacNumericQuestion - question to validate.
     * @param answerFromUser       - answer from user
     * @return the validation response
     */
    private QuantityValidationResponse validateWithoutUnits(final IsaacNumericQuestion isaacNumericQuestion,
                                                            final Quantity answerFromUser) {
        log.debug("\t[validateWithoutUnits]");
        QuantityValidationResponse bestResponse = null;
        Integer sigFigsToValidateWith = null;

        if (!isaacNumericQuestion.getDisregardSignificantFigures()) {
            sigFigsToValidateWith = ValidationUtils.numberOfSignificantFiguresToValidateWith(
                    answerFromUser.getValue(),
                    isaacNumericQuestion.getSignificantFiguresMin(),
                    isaacNumericQuestion.getSignificantFiguresMax(),
                    log
            );
        }

        List<Choice> orderedChoices = getOrderedChoices(isaacNumericQuestion.getChoices());

        for (Choice c : orderedChoices) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                // Do we have a match? Since only comparing values, either an exact match or not a match at all.
                if (ValidationUtils.numericValuesMatch(
                        quantityFromQuestion.getValue(),
                        answerFromUser.getValue(),
                        sigFigsToValidateWith,
                        log
                )) {
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), null, new Date());
                    break;
                }
            } else {
                log.error("Isaac Numeric Validator expected there to be a Quantity in ("
                        + isaacNumericQuestion.getCanonicalSourceFile() + ") Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // No matches; tell them they got it wrong but we cannot find any more detailed feedback for them.
            return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                    false, new Content(DEFAULT_VALIDATION_RESPONSE), false, null, new Date());
        } else {
            return bestResponse;
        }
    }

    /**
     * Helper method to verify if the answer given is to too few significant figures.
     *
     * @param valueToCheck      - the value as a string from the user to check.
     * @param minAllowedSigFigs - the minimum number of significant figures that is expected for the answer to be correct.
     * @return true if too few, false if not.
     */
    private boolean tooFewSignificantFigures(final String valueToCheck, final int minAllowedSigFigs) {
        log.debug("\t[tooFewSignificantFigures]");

        ValidationUtils.SigFigResult sigFigsFromUser = ValidationUtils.extractSignificantFigures(valueToCheck, log);

        return sigFigsFromUser.sigFigsMax < minAllowedSigFigs;
    }

    /**
     * Helper method to verify if the answer given is to too many significant figures.
     *
     * @param valueToCheck      - the value as a string from the user to check.
     * @param maxAllowedSigFigs - the maximum number of significant figures that is expected for the answer to be correct.
     * @return true if too many, false if not.
     */
    private boolean tooManySignificantFigures(final String valueToCheck, final int maxAllowedSigFigs) {
        log.debug("\t[tooManySignificantFigures]");

        ValidationUtils.SigFigResult sigFigsFromUser = ValidationUtils.extractSignificantFigures(valueToCheck, log);

        return sigFigsFromUser.sigFigsMin > maxAllowedSigFigs;
    }

    /**
     *  Replace explanation of validation response if question has default feedback and existing feedback blank or generic.
     *
     *  This method could be void, since it modifies the object passed in by reference, but it makes for shorter and
     *  simpler code when it is used if it just returns the same object it is passed.
     *
     * @param question - the question to use the default feedback of
     * @param response - the response to modify if necessary
     * @return the modified response object
     */
    private QuantityValidationResponse useDefaultFeedbackIfNecessary(final IsaacNumericQuestion question,
                                                                     final QuantityValidationResponse response) {
        Content feedback = response.getExplanation();
        boolean feedbackEmptyOrGeneric = feedbackIsNullOrEmpty(feedback) || DEFAULT_VALIDATION_RESPONSE.equals(feedback.getValue())
                || DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE.equals(feedback.getValue());

        if (null != question.getDefaultFeedback() && feedbackEmptyOrGeneric) {
            log.debug("Replacing generic or blank explanation with default feedback from question.");
            response.setExplanation(question.getDefaultFeedback());
            // TODO - should this preserve the 'sig_figs' tag? If so, how to do it without modifying the referenced default feedback?
        }
        return response;
    }
}
