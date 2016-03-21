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
import uk.ac.cam.cl.dtg.segue.dos.QuantityValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dos.content.Choice;
import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dos.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dos.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 */
public class IsaacNumericValidator implements IValidator {
    private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);
    
    private static final String DEFAULT_VALIDATION_RESPONSE = "Check your working.";
    private static final String DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE = "Check your units.";
    
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
        if (null == isaacNumericQuestion.getChoices() || isaacNumericQuestion.getChoices().isEmpty()) {
            log.error("Question does not have any answers. " + question.getId() + " src: "
                    + question.getCanonicalSourceFile());

            return new QuantityValidationResponse(question.getId(), null, false, new Content(""), false, false,
                    new Date());
        }

        try {
            if (null == answerFromUser.getValue() || answerFromUser.getValue().isEmpty()) {
                return new QuantityValidationResponse(question.getId(), answerFromUser, false, new Content(
                        "You did not provide an answer."), false, false, new Date());
            } else if (null == answerFromUser.getUnits() && (isaacNumericQuestion.getRequireUnits())) {
                return new QuantityValidationResponse(question.getId(), answerFromUser, false, new Content(
                        "You did not provide any units."), null, false, new Date());

            } 
            
            QuestionValidationResponse bestResponse;
            
            // Step 1 - exact (string based) matching first - handles case where editors enter two mathematically
            // equivalent known answers - won't check for sig figs.
            bestResponse = this.exactStringMatch(isaacNumericQuestion, answerFromUser);
            
            // Only return this if the answer is incorrect - as we don't know if the correct answers have always been
            // specified in the correct # of sig figs.
            if (bestResponse != null && !bestResponse.isCorrect()) {
                return bestResponse;
            }
            
            // Step 2 - then do correct answer numeric equivalence checking.
            if (isaacNumericQuestion.getRequireUnits()) {
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
                return bestResponse;
            }

            // Step 3 - then do sig fig checking iff we think they have a correct answer.           
            if (!this.verifyCorrectNumberofSignificantFigures(answerFromUser.getValue(),
                    isaacNumericQuestion.getSignificantFigures())) {
                // make sure that the answer is to the right number of sig figs
                // before we proceed.
                
                // if we have unit information available put it in our response.
                Boolean validUnits = null;
                if (isaacNumericQuestion.getRequireUnits()) {
                    QuestionValidationResponse valid = this.validateWithUnits(isaacNumericQuestion, answerFromUser);
                    
                    if (valid instanceof QuantityValidationResponse) {
                        QuantityValidationResponse quantity = (QuantityValidationResponse) valid;
                        validUnits = quantity.getCorrectUnits();
                    }
                    
                } 

                bestResponse = new QuantityValidationResponse(
                        question.getId(),
                        answerFromUser,
                        false,
                        new Content(
                                "Your <strong>Significant figures</strong> are incorrect, "
                                + "read our "
                                + "<strong><a target='_blank' href='/solving_problems#acc_solving_problems_sig_figs'>"
                                + "sig fig guide</a></strong>."),
                        false, validUnits, new Date());
            }
            
            // ok return the best response
            return bestResponse;
        } catch (NumberFormatException e) {
            return new QuantityValidationResponse(question.getId(), answerFromUser, false, new Content(
                    "The answer you provided is not a valid number."), false, false, new Date());
        }
    }

    /**
     * Numerically validate the students answer ensuring that the correct unit value is specified.
     * 
     * @param isaacNumericQuestion
     *            - question to validate.
     * @param answerFromUser
     *            - answer from user
     * @return the validation response
     */
    private QuestionValidationResponse validateWithUnits(final IsaacNumericQuestion isaacNumericQuestion,
            final Quantity answerFromUser) {
        QuantityValidationResponse bestResponse = null;
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
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), quantityFromQuestion.isCorrect(), new Date());

                    break;
                } else if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())
                        && !answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())
                        && quantityFromQuestion.isCorrect()) {
                    // matches value but not units of a correct choice.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_WRONG_UNIT_VALIDATION_RESPONSE), true, false, new Date());
                } else if (!numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())
                        && answerFromUser.getUnits().equals(quantityFromQuestion.getUnits())
                        && quantityFromQuestion.isCorrect()) {
                    // matches units but not value of a correct choice.
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_VALIDATION_RESPONSE), false, true, new Date());
                }
            } else {
                log.error("Isaac Numeric Validator for questionId: " + isaacNumericQuestion.getId()
                        + " expected there to be a Quantity. Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // tell them they got it wrong but we cannot find an
            // feedback for them.
            return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser, false, new Content(
                    DEFAULT_VALIDATION_RESPONSE), false, false, new Date());

        } else {
            return bestResponse;
        }
    }

    /**
     * Numerically validate the response without units being considered.
     * 
     * @param isaacNumericQuestion
     *            - question to validate.
     * @param answerFromUser
     *            - answer from user
     * @return the validation response
     */
    private QuestionValidationResponse validateWithoutUnits(final IsaacNumericQuestion isaacNumericQuestion,
            final Quantity answerFromUser) {
        QuantityValidationResponse bestResponse = null;
        for (Choice c : isaacNumericQuestion.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                // match known choices
                if (numericValuesMatch(quantityFromQuestion.getValue(), answerFromUser.getValue(),
                        isaacNumericQuestion.getSignificantFigures())) {
                    
                    // value match
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), null, new Date());
                    break;
                } else {
                    // value doesn't match this choice
                    bestResponse = new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            false, new Content(DEFAULT_VALIDATION_RESPONSE), false, null, new Date());
                }
            } else {
                log.error("Isaac Numeric Validator expected there to be a Quantity in ("
                        + isaacNumericQuestion.getCanonicalSourceFile() + ") Instead it found a Choice.");
            }
        }

        if (null == bestResponse) {
            // tell them they got it wrong but we cannot find an
            // feedback for them.
            return new QuestionValidationResponse(isaacNumericQuestion.getId(), answerFromUser, false, null,
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
            final int significantFiguresRequired) throws NumberFormatException {
        double trustedDouble, untrustedDouble;

        // Replace "x10^" with "e";
        String untrustedParsedValue = untrustedValue.replace("x10^", "e").replace("*10^", "e");

        trustedDouble = Double.parseDouble(trustedValue.replace("x10^", "e").replace("*10^", "e"));
        untrustedDouble = Double.parseDouble(untrustedParsedValue);
        
        // Round to N s.f. for trusted value
        trustedDouble = roundToSigFigs(trustedDouble, significantFiguresRequired);
        untrustedDouble = roundToSigFigs(untrustedDouble, significantFiguresRequired);
        final double epsilon = 1e-50;

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
    
    /**
     * Sometimes we want to do an exact string wise match before we do sig fig checks etc. This method is intended to
     * work for this case.
     * 
     * @param isaacNumericQuestion
     *            - question content object
     * @param answerFromUser
     *            - response form the user
     * @return either a QuestionValidationResponse if there is an exact String match or null if no string match.
     */
    private QuestionValidationResponse exactStringMatch(final IsaacNumericQuestion isaacNumericQuestion,
            final Quantity answerFromUser) {

        for (Choice c : isaacNumericQuestion.getChoices()) {
            if (c instanceof Quantity) {
                Quantity quantityFromQuestion = (Quantity) c;

                StringBuilder userStringForComparison = new StringBuilder();
                userStringForComparison.append(answerFromUser.getValue().trim());
                userStringForComparison.append(answerFromUser.getUnits());

                StringBuilder questionAnswerStringForComparison = new StringBuilder();
                questionAnswerStringForComparison.append(quantityFromQuestion.getValue().trim());
                questionAnswerStringForComparison.append(quantityFromQuestion.getUnits());

                if (questionAnswerStringForComparison.toString().trim()
                        .equals(userStringForComparison.toString().trim())) {
                    Boolean unitFeedback = null;
                    if (isaacNumericQuestion.getRequireUnits()) {
                        unitFeedback = quantityFromQuestion.getUnits().equals(answerFromUser.getUnits());
                    }

                    return new QuantityValidationResponse(isaacNumericQuestion.getId(), answerFromUser,
                            quantityFromQuestion.isCorrect(), (Content) quantityFromQuestion.getExplanation(),
                            quantityFromQuestion.isCorrect(), unitFeedback, new Date());

                }
            }
        }
        return null;
    }
}
