/**
 * Copyright 2014 Stephen Cummins
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.cam.cl.dtg.isaac.quiz;

import java.math.BigDecimal;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.dto.QuantityValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponseDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.segue.dto.content.QuantityDTO;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 */
public class IsaacNumericValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);

    @Override
    public final QuestionValidationResponseDTO validateQuestionResponse(
            final Question question, final ChoiceDTO answer) {
        if (!(question instanceof IsaacNumericQuestion)) {
            throw new IllegalArgumentException(String.format(
                    "This validator only works with Isaac Numeric Questions... (%s is not numeric)", question.getId()));
        }

        if (!(answer instanceof QuantityDTO)) {
            throw new IllegalArgumentException(String.format(
                    "Expected Quantity for IsaacNumericQuestion: %s. Received (%s) ", question.getId(),
                    answer.getClass()));
        }

        IsaacNumericQuestion isaacNumericQuestion = (IsaacNumericQuestion) question;
        QuantityDTO answerFromUser = (QuantityDTO) answer;
        if (null == isaacNumericQuestion.getChoices() || isaacNumericQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            return new QuantityValidationResponseDTO(question.getId(), null, false, new Content(""), false, false,
                    new Date());
        }

        try {
            if (null == answerFromUser.getValue() || answerFromUser.getValue().isEmpty()) {
                return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
                        "You did not provide an answer."), false, false, new Date());
            } else if (null == answerFromUser.getUnits() && (isaacNumericQuestion.getRequireUnits())) {
                return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
                        "You did not provide any units."), null, false, new Date());

            } else if (!this.verifyCorrectNumberofSignificantFigures(answerFromUser.getValue(),
                    isaacNumericQuestion.getSignificantFigures())) {
                // make sure that the answer is to the right number of sig figs
                // before we proceed.
                
                // if we have unit information available put it in our response.
                Boolean validUnits = null;
                if (isaacNumericQuestion.getRequireUnits()) {
                    QuestionValidationResponseDTO valid = this.validateWithUnits(isaacNumericQuestion, answerFromUser);
                    
                    if (valid instanceof QuantityValidationResponseDTO) {
                        QuantityValidationResponseDTO quantity = (QuantityValidationResponseDTO) valid;
                        validUnits = quantity.getCorrectUnits();
                    }
                    
                } 

                // This is a hack as we don't actually know if it should be a quantity response or not.
                return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
                        "Please provide your answer to the correct number of significant figures."), false, validUnits,
                        new Date());
            }
        } catch (NumberFormatException e) {
            return new QuantityValidationResponseDTO(question.getId(), answerFromUser, false, new Content(
                    "The answer you provided is not a valid number."), false, false, new Date());
        }

        if (isaacNumericQuestion.getRequireUnits()) {
            return this.validateWithUnits(isaacNumericQuestion, answerFromUser);
        } else {
            return this.validateWithoutUnits(isaacNumericQuestion, answerFromUser);
        }
    }

    /**
     * Validate the students answer ensuring that the correct unit value is specified.
     * 
     * @param isaacNumericQuestion
     *            - question to validate.
     * @param answerFromUser
     *            - answer from user
     * @return the validation response
     */
    private QuestionValidationResponseDTO validateWithUnits(final IsaacNumericQuestion isaacNumericQuestion,
            final QuantityDTO answerFromUser) {
        QuantityValidationResponseDTO bestResponse = null;
        for (Choice c : isaacNumericQuestion.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                if (quantityFromQuestion.getUnits() == null) {
                    log.error("Expected units and no units can be found for question id: "
                            + isaacNumericQuestion.getId());
                    continue;
                }

                // match known choices
                if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())
                        && answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())) {
                    // exact match
                    bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), quantityFromQuestion.isCorrect(), new Date());

                    break;
                } else if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())
                        && !answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())
                        && quantityFromQuestion.isCorrect()) {
                    // matches value but not units of a correct choice.
                    bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content("Check your units."), true, false, new Date());
                } else if (!numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())
                        && answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())
                        && quantityFromQuestion.isCorrect()) {
                    // matches units but not value of a correct choice.
                    bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content("Check your working."), false, true, new Date());
                }
            } else {
                log.error("Isaac Numeric Validator for questionId: " + isaacNumericQuestion.getId()
                        + " expected there to be a Quantity. Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // tell them they got it wrong but we cannot find an
            // feedback for them.
            return new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser, false, new Content(
                    "Check your working."), false, false, new Date());

        } else {
            return bestResponse;
        }
    }

    /**
     * Question validation response without units being considered.
     * 
     * @param isaacNumericQuestion
     *            - question to validate.
     * @param answerFromUser
     *            - answer from user
     * @return the validation response
     */
    private QuestionValidationResponseDTO validateWithoutUnits(final IsaacNumericQuestion isaacNumericQuestion,
            final QuantityDTO answerFromUser) {
        QuantityValidationResponseDTO bestResponse = null;
        for (Choice c : isaacNumericQuestion.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                // match known choices
                if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())) {
                    // value match
                    bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), null, new Date());
                    break;
                } else {
                    // value doesn't match this choice
                    bestResponse = new QuantityValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content("Check your working."), false, null, new Date());
                }
            } else {
                log.error("Isaac Numeric Validator expected there to be a Quantity in ("
                        + isaacNumericQuestion.getCanonicalSourceFile() + ") Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // tell them they got it wrong but we cannot find an
            // feedback for them.
            return new QuestionValidationResponseDTO(isaacNumericQuestion.getId(), answerFromUser, false, null,
                    new Date());
        } else {
            return bestResponse;
        }
    }

    /**
     * Test whether two quantity values match. Parse the strings as doubles, supporting notation of 3x10^12 to mean
     * 3e12, then test that they match to N s.f.
     * 
     * @param trustedValue
     *            - first number
     * @param untrustedValue
     *            - second number
     * @param significantFiguresRequired
     *            - the number of significant figures that the answer provided should match
     * @return true when the numbers match
     */
    private boolean numericValuesMatch(final String trustedValue, final String untrustedValue,
            final int significantFiguresRequired) {
        double trustedDouble, untrustedDouble;

        // Replace "x10^" with "e";
        String untrustedParsedValue = untrustedValue.replace("x10^", "e");

        try {
            trustedDouble = Double.parseDouble(trustedValue.replace("x10^", "e"));
            untrustedDouble = Double.parseDouble(untrustedParsedValue);
        } catch (NumberFormatException e) {
            // One of the values was not a valid float.
            return false;
        }

        // Round to N s.f. for trusted value
        trustedDouble = roundToSigFigs(trustedDouble, significantFiguresRequired);
        untrustedDouble = roundToSigFigs(untrustedDouble, significantFiguresRequired);
        double epsilon = 1e-50;

        return Math.abs(trustedDouble - untrustedDouble) < Math.max(epsilon * Math.max(trustedDouble, untrustedDouble),
                epsilon);
    }

    /**
     * Round a double to a given number of significant figures.
     * 
     * @param f
     *            - number to round
     * @param sigFigs
     *            - number of significant figures required
     * @return the rounded number.
     */
    private double roundToSigFigs(final double f, final int sigFigs) {

        int mag = (int) Math.floor(Math.log10(Math.abs(f)));

        double normalised = f / Math.pow(10, mag);

        return Math.round(normalised * Math.pow(10, sigFigs - 1)) * Math.pow(10, mag) / Math.pow(10, sigFigs - 1);
    }

    /**
     * Helper method to verify if the answer given is to the correct number of significant figures.
     * 
     * @param valueToCheck
     *            - the value as a string from the user to check.
     * @param significantFigures
     *            - the number of significant figures that is expected for the answer to be correct.
     * @return true if yes false if not.
     */
    private boolean verifyCorrectNumberofSignificantFigures(final String valueToCheck, final int significantFigures) {
        // Replace "x10^" with "e";
        String untrustedParsedValue = valueToCheck.replace("x10^", "e");

        // check significant figures match
        BigDecimal bd = new BigDecimal(untrustedParsedValue);

        int untrustedValueSigfigs = bd.precision();
        if (untrustedParsedValue.contains(".")) {

            // if it contains a decimal we can be more confident of the significant figures.
            return untrustedValueSigfigs == significantFigures;
        } else {
            // if not we have to be flexible as integer values have undefined significant figure rules.
            char[] unscaledValueToCheck = bd.unscaledValue().toString().toCharArray();

            // count trailing zeroes
            int trailingZeroes = 0;
            for (int i = unscaledValueToCheck.length - 1; i >= 0; i--) {
                if (unscaledValueToCheck[i] == '0') {
                    trailingZeroes++;
                } else {
                    break;
                }
            }

            return bd.precision() - trailingZeroes <= significantFigures && bd.precision() >= significantFigures;
        }
    }
}
