/**
 * Copyright 2022 Chris Purdy, James Sharkey
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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacReorderQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

/**
 * Validator that provides functionality to validate reorder questions. It is essentially a copy of the cloze question
 * validator.
 */
public class IsaacReorderValidator implements IValidator {
  private static final Logger log = LoggerFactory.getLogger(IsaacReorderValidator.class);
  private static final int NO_MATCH = -1;

  @Override
  public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
    requireNonNull(question);
    requireNonNull(answer);

    if (!(question instanceof IsaacReorderQuestion)) {
      throw new IllegalArgumentException(String.format(
          "This validator only works with IsaacReorderQuestions (%s is not ReorderQuestion)", question.getId()));
    }

    if (!(answer instanceof ItemChoice)) {
      throw new IllegalArgumentException(String.format(
          "Expected ItemChoice for IsaacReorderQuestion: %s. Received (%s) ", question.getId(), answer.getClass()));
    }

    // These variables store the important features of the response we'll send.
    Content feedback = null;                        // The feedback we send the user
    boolean responseCorrect = false;                // Whether we're right or wrong

    IsaacReorderQuestion reorderQuestion = (IsaacReorderQuestion) question;
    ItemChoice submittedChoice = (ItemChoice) answer;

    List<String> submittedItemIds = Collections.emptyList();

    // STEP 0: Is it even possible to answer this question?

    if (null == reorderQuestion.getChoices() || reorderQuestion.getChoices().isEmpty()) {
      log.error("Question does not have any answers. {} src: {}", question.getId(), question.getCanonicalSourceFile());
      feedback = new Content("This question does not have any correct answers!");
    } else if (null == reorderQuestion.getItems() || reorderQuestion.getItems().isEmpty()) {
      log.error("ReorderQuestion does not have any items. {} src: {}", question.getId(),
          question.getCanonicalSourceFile());
      feedback = new Content("This question does not have any items to choose from!");
    }

    // STEP 1: Did they provide a valid answer?

    if (null == feedback) {
      if (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty()) {
        feedback = new Content("You did not provide an answer.");
      } else if (submittedChoice.getItems().stream().anyMatch(i -> i.getClass() != Item.class)) {
        feedback = new Content("Your answer is not in a recognised format!");
      } else {
        Set<String> allowedItemIds = reorderQuestion.getItems().stream().map(Item::getId).collect(Collectors.toSet());
        submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toList());
        if (!allowedItemIds.containsAll(submittedItemIds)) {
          feedback = new Content("Your answer contained unrecognised items.");
        }
      }
    }

    // STEP 2: If they did, does their answer match a known answer?

    if (null == feedback) {
      // Sort the choices:
      List<Choice> orderedChoices = getOrderedChoices(reorderQuestion.getChoices());

      // For all the choices on this question...
      for (Choice c : orderedChoices) {

        // ... that are ItemChoices (and not subclasses) ...
        if (!ItemChoice.class.equals(c.getClass())) {
          log.error(String.format("Expected ItemChoice in question (%s), instead found %s!", reorderQuestion.getId(),
              c.getClass().toString()));
          continue;
        }

        ItemChoice itemChoice = (ItemChoice) c;
        boolean allowSubsetMatch = null != itemChoice.isAllowSubsetMatch() && itemChoice.isAllowSubsetMatch();

        // ... and that have valid items ...
        if (null == itemChoice.getItems() || itemChoice.getItems().isEmpty()) {
          log.error(String.format("Expected list of Items, but none found in choice for question id (%s)!",
              reorderQuestion.getId()));
          continue;
        }
        if (itemChoice.getItems().stream().anyMatch(i -> i.getClass() != Item.class)) {
          log.error(String.format("Expected list of Items, but something else found in choice for question id (%s)!",
              reorderQuestion.getId()));
          continue;
        }
        List<String> trustedChoiceItemIds =
            itemChoice.getItems().stream().map(Item::getId).collect(Collectors.toList());

        // ... look for a match to the submitted answer.

        if (allowSubsetMatch) {
          // The indexOfSubList method is O(n^2) brute-force, but will work correctly if the lists are equal, too.
          // If the submission contains the trusted choice, and we allow subset matching, we have a match:
          int indexOfMatch = Collections.indexOfSubList(submittedItemIds, trustedChoiceItemIds);
          if (indexOfMatch > NO_MATCH) {
            responseCorrect = itemChoice.isCorrect();
            feedback = (Content) itemChoice.getExplanation();
            // We probably can't do better than this match, so stop looking:
            break;
          }
        } else {
          if (trustedChoiceItemIds.equals(submittedItemIds)) {
            responseCorrect = itemChoice.isCorrect();
            feedback = (Content) itemChoice.getExplanation();
            // This is an exact match, the best we can do, so stop looking:
            break;
          } else if (submittedItemIds.size() != trustedChoiceItemIds.size() && itemChoice.isCorrect()) {
            // We might later find a better match, but this might be useful feedback if not:
            if (submittedItemIds.size() < trustedChoiceItemIds.size()) {
              feedback = new Content("Your answer does not contain enough items.");
            } else {
              feedback = new Content("Your answer contains too many items.");
            }
          }
        }
      }
    }

    // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
    if (feedbackIsNullOrEmpty(feedback) && null != reorderQuestion.getDefaultFeedback()) {
      feedback = reorderQuestion.getDefaultFeedback();
    }

    return new QuestionValidationResponse(question.getId(), answer, responseCorrect, feedback, new Date());
  }

  @Override
  public List<Choice> getOrderedChoices(final List<Choice> choices) {
    return IsaacItemQuestionValidator.getOrderedChoicesWithSubsets(choices);
  }

}
