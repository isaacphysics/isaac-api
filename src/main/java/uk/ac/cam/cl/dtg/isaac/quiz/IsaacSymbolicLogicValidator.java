/**
 * Copyright 2016 Alistair Stead, James Sharkey, Ian Davies
 * <br>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * <br>
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * <br>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.ac.cam.cl.dtg.isaac.quiz;

import static uk.ac.cam.cl.dtg.util.LogUtils.sanitiseLogValue;

import com.google.api.client.util.Maps;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.FormulaValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacSymbolicLogicQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.LogicFormula;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

/**
 * Validator that provides functionality to validate symbolic logic questions.
 */
public class IsaacSymbolicLogicValidator implements IValidator {
  private static final Logger log = LoggerFactory.getLogger(IsaacSymbolicLogicValidator.class);

  private enum MatchType {
    NONE,
    SYMBOLIC,
    EXACT
  }

  private static class ValidationResult {
    private Content feedback;               // The feedback we send the user
    private MatchType responseMatchType;    // The match type we found
    private boolean responseCorrect;        // Whether we're right or wrong

    ValidationResult(final Content feedback, final MatchType responseMatchType, final boolean responseCorrect) {
      this.feedback = feedback;
      this.responseMatchType = responseMatchType;
      this.responseCorrect = responseCorrect;
    }

    public Content getFeedback() {
      return feedback;
    }

    public void setFeedback(final Content feedback) {
      this.feedback = feedback;
    }

    public MatchType getResponseMatchType() {
      return responseMatchType;
    }

    public void setResponseMatchType(final MatchType responseMatchType) {
      this.responseMatchType = responseMatchType;
    }

    public boolean isResponseCorrect() {
      return responseCorrect;
    }

    public void setResponseCorrect(final boolean responseCorrect) {
      this.responseCorrect = responseCorrect;
    }
  }

  private final String externalValidatorUrl;

  private static final Content INITIAL_CONTENT = null;
  private static final MatchType INITIAL_MATCH_TYPE = MatchType.NONE;
  private static final boolean INITIAL_RESPONSE_CORRECT = false;

  public IsaacSymbolicLogicValidator(final String hostname, final String port) {
    this.externalValidatorUrl = "http://" + hostname + ":" + port + "/check/logic";
  }

  @Override
  public QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer)
      throws ValidatorUnavailableException {
    Validate.notNull(question);
    Validate.notNull(answer);

    validateQuestionType(question, answer);

    IsaacSymbolicLogicQuestion symbolicLogicQuestion = (IsaacSymbolicLogicQuestion) question;
    LogicFormula submittedLogicFormula = (LogicFormula) answer;

    // These variables store the important features of the response we'll send.
    ValidationResult validationResult = new ValidationResult(
        INITIAL_CONTENT,            // The feedback we send the user
        INITIAL_MATCH_TYPE,         // The match type we found
        INITIAL_RESPONSE_CORRECT    // Whether we're right or wrong
    );

    // There are several specific responses the user can receive. Each of them will set feedback content, so
    // use that to decide whether to proceed to the next check in each case.

    // STEP 0: Do we even have any answers for this question? Always do this check, because we know we
    //         won't have feedback yet.

    validationResult.setFeedback(checkQuestionHasAnswer(symbolicLogicQuestion));

    // STEP 1: Did they provide an answer?

    if (null == validationResult.getFeedback()) {
      validationResult.setFeedback(checkUserHasProvidedAnswer(submittedLogicFormula));
    }

    // STEP 2: Otherwise, Does their answer match a choice exactly?

    if (null == validationResult.getFeedback()) {
      validationResult = checkForExactAnswerMatch(symbolicLogicQuestion, submittedLogicFormula);
    }

    // STEP 3: Otherwise, use the symbolic checker to analyse their answer

    if (null == validationResult.getFeedback()) {
      validationResult = checkForSymbolicAnswerMatch(symbolicLogicQuestion, submittedLogicFormula);
    }

    // STEP 4: If we still have no feedback to give, use the question's default feedback if any to use:

    if (feedbackIsNullOrEmpty(validationResult.getFeedback()) && null != symbolicLogicQuestion.getDefaultFeedback()) {
      validationResult.setFeedback(symbolicLogicQuestion.getDefaultFeedback());
    }

    // If we got this far and feedback is still null, they were wrong. There's no useful feedback we can give at this point.

    return new FormulaValidationResponse(symbolicLogicQuestion.getId(), answer, validationResult.getFeedback(),
        validationResult.isResponseCorrect(), validationResult.getResponseMatchType().toString(), new Date());
  }

  private ValidationResult checkForSymbolicAnswerMatch(final IsaacSymbolicLogicQuestion question,
                                                       final LogicFormula submittedLogicFormula)
      throws ValidatorUnavailableException {
    // Go through all choices, keeping track of the best match we've seen so far. A symbolic match terminates
    // this loop immediately. A numeric match may later be replaced with a symbolic match, but otherwise will suffice.

    LogicFormula closestMatch = null;
    MatchType closestMatchType = MatchType.NONE;

    // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
    List<Choice> orderedChoices = getOrderedChoices(question.getChoices());

    // For all the choices on this question...
    for (Choice c : orderedChoices) {

      // ... that are of the LogicFormula type, ...
      if (!(c instanceof LogicFormula)) {
        // Don't need to log this - it will have been logged above.
        continue;
      }

      LogicFormula logicFormulaChoice = (LogicFormula) c;

      // ... and that have a python expression ...
      if (null == logicFormulaChoice.getPythonExpression() || logicFormulaChoice.getPythonExpression().isEmpty()) {
        // Don't need to log this - it will have been logged above.
        continue;
      }

      // ... test their answer against this choice with the symbolic checker.

      // We don't do any sanitisation of user input here, we'll leave that to the python.

      MatchType matchType = MatchType.NONE;

      try {
        HashMap<String, String> req = Maps.newHashMap();
        req.put("target", logicFormulaChoice.getPythonExpression());
        req.put("test", submittedLogicFormula.getPythonExpression());
        req.put("description", question.getId());
        if (question.getAvailableSymbols() != null) {
          req.put("symbols", String.join(",", question.getAvailableSymbols()));
        }

        HashMap<String, Object> response = getResponseFromExternalValidator(externalValidatorUrl, req);

        if (response.containsKey("error")) {
          if (response.containsKey("code")) {
            log.error("Failed to check logic formula \"" + sanitiseLogValue(submittedLogicFormula.getPythonExpression())
                + "\" against \"" + sanitiseLogValue(logicFormulaChoice.getPythonExpression())
                + "\": " + sanitiseLogValue(response.get("error").toString()));
          } else if (response.containsKey("syntax_error")) {
            // There's a syntax error in the "test" expression, no use checking it further:
            Content feedback = new Content("Your answer does not seem to be valid boolean logic.<br>"
                + "Check for things like mismatched brackets or misplaced symbols.");
            feedback.setTags(new HashSet<>(Collections.singletonList("syntax_error")));
            return new ValidationResult(feedback, INITIAL_MATCH_TYPE, false);
          } else {
            log.warn("Problem checking logic formula \"" + submittedLogicFormula.getPythonExpression()
                + "\" for (" + question.getId() + ") with symbolic checker: " + response.get("error"));
          }
        } else {
          if (response.get("equal").equals("true")) {
            matchType = MatchType.valueOf(((String) response.get("equality_type")).toUpperCase());
          }
        }

      } catch (IOException e) {
        log.error("Failed to check formula with symbolic checker. Is the server running? Not trying again.");
        throw new ValidatorUnavailableException("We are having problems marking Logic Questions."
            + " Please try again later!");
      }

      if (matchType == MatchType.EXACT) {
        closestMatch = logicFormulaChoice;
        closestMatchType = MatchType.EXACT;
        break;
      } else if (matchType.compareTo(closestMatchType) > 0) {
        if (logicFormulaChoice.getRequiresExactMatch() && logicFormulaChoice.isCorrect()) {
          closestMatch = logicFormulaChoice;
          closestMatchType = matchType;
        } else {
          if (closestMatch == null || !closestMatch.getRequiresExactMatch()) {
            closestMatch = logicFormulaChoice;
            closestMatchType = matchType;
          } // ELSE: This is not as good a match as the one we already have.
        }
      }
    }

    if (null != closestMatch) {
      // We found a decent match. Of course, it still might be wrong.

      if (closestMatchType != MatchType.EXACT && closestMatch.getRequiresExactMatch()) {
        if (closestMatch.isCorrect()) {
          Content feedback =
              new Content("Your answer is not in the form we expected. Can you rearrange or simplify it?");
          feedback.setTags(new HashSet<>(Collections.singletonList("required_exact")));

          log.info("User submitted an answer that was close to an exact match, but not exact "
              + "for question " + sanitiseLogValue(question.getId())
              + ". Choice: " + sanitiseLogValue(closestMatch.getPythonExpression())
              + ", submitted: " + sanitiseLogValue(submittedLogicFormula.getPythonExpression()));

          return new ValidationResult(feedback, closestMatchType, false);
        } // ELSE: This is weak match to a wrong answer; we can't use the feedback for the choice.
      } else {
        return new ValidationResult((Content) closestMatch.getExplanation(), closestMatchType,
            closestMatch.isCorrect());
      }

    }
    return new ValidationResult(INITIAL_CONTENT, INITIAL_MATCH_TYPE, INITIAL_RESPONSE_CORRECT);
  }

  private static ValidationResult checkForExactAnswerMatch(final IsaacSymbolicLogicQuestion question,
                                                           final LogicFormula submittedLogicFormula) {
    // For all the choices on this question...
    for (Choice c : question.getChoices()) {

      // ... that are of the LogicFormula type, ...
      if (!(c instanceof LogicFormula)) {
        log.error("Validator for questionId: " + question.getId()
            + " expected there to be a LogicFormula. Instead it found a Choice.");
        continue;
      }

      LogicFormula logicFormulaChoice = (LogicFormula) c;

      // ... and that have a python expression ...
      if (null == logicFormulaChoice.getPythonExpression() || logicFormulaChoice.getPythonExpression().isEmpty()) {
        log.error("Expected python expression, but none found in choice for question id: "
            + question.getId());
        continue;
      }

      // ... look for an exact string match to the submitted answer.
      if (logicFormulaChoice.getPythonExpression().equals(submittedLogicFormula.getPythonExpression())) {
        return new ValidationResult(
            (Content) logicFormulaChoice.getExplanation(),
            MatchType.EXACT,
            logicFormulaChoice.isCorrect()
        );
      }
    }
    // If no match is found...
    return new ValidationResult(INITIAL_CONTENT, INITIAL_MATCH_TYPE, INITIAL_RESPONSE_CORRECT);
  }

  private static Content checkUserHasProvidedAnswer(final LogicFormula submittedLogicFormula) {
    if (null == submittedLogicFormula.getPythonExpression() || submittedLogicFormula.getPythonExpression().isEmpty()) {
      return new Content("You did not provide an answer");
    }
    return null;
  }

  private static Content checkQuestionHasAnswer(final IsaacSymbolicLogicQuestion question) {
    if (null == question.getChoices() || question.getChoices().isEmpty()) {
      log.error("Question does not have any answers. " + question.getId() + " src: "
          + question.getCanonicalSourceFile());

      return new Content("This question does not have any correct answers");
    }
    return null;
  }

  private static void validateQuestionType(final Question question, final Choice answer)
      throws IllegalArgumentException {
    if (!(question instanceof IsaacSymbolicLogicQuestion)) {
      throw new IllegalArgumentException(String.format(
          "This validator only works with Isaac Symbolic Questions... (%s is not symbolic)",
          question.getId()));
    }

    if (!(answer instanceof LogicFormula)) {
      throw new IllegalArgumentException(String.format(
          "Expected LogicFormula for IsaacSymbolicLogicQuestion: %s. Received (%s) ", question.getId(),
          answer.getClass()));
    }
  }

}
