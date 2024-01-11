/**
 * Copyright 2021 Chris Purdy, 2022 James Sharkey
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

import com.google.api.client.util.Lists;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacClozeQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.ItemValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.Item;
import uk.ac.cam.cl.dtg.isaac.dos.content.ItemChoice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;

/**
 * Validator that only provides functionality to validate Cloze questions.
 */
public class IsaacClozeValidator implements IValidator {
  private static final Logger log = LoggerFactory.getLogger(IsaacClozeValidator.class);
  protected static final String NULL_CLOZE_ITEM_ID = "NULL_CLOZE_ITEM";

  @Override
  public final QuestionValidationResponse validateQuestionResponse(final Question question, final Choice answer) {
    requireNonNull(question);
    requireNonNull(answer);

    if (!(question instanceof IsaacClozeQuestion)) {
      throw new IllegalArgumentException(String.format(
          "This validator only works with IsaacClozeQuestions (%s is not ClozeQuestion)", question.getId()));
    }

    if (!(answer instanceof ItemChoice)) {
      throw new IllegalArgumentException(String.format(
          "Expected ItemChoice for IsaacClozeQuestions: %s. Received (%s) ", question.getId(), answer.getClass()));
    }

    // These variables store the important features of the response we'll send.
    Content feedback = null;                        // The feedback we send the user
    boolean responseCorrect = false;                // Whether we're right or wrong
    List<Boolean> itemsCorrect = null;              // Individual item feedback.

    IsaacClozeQuestion clozeQuestion = (IsaacClozeQuestion) question;
    ItemChoice submittedChoice = (ItemChoice) answer;
    boolean detailedItemFeedback =
        clozeQuestion.getDetailedItemFeedback() != null && clozeQuestion.getDetailedItemFeedback();


    List<String> submittedItemIds = Collections.emptyList();
    Set<String> allowedItemIds = Collections.emptySet();

    // STEP 0: Is it even possible to answer this question?

    if (null == clozeQuestion.getChoices() || clozeQuestion.getChoices().isEmpty()) {
      log.error("Question does not have any answers. " + question.getId() + " src: "
          + question.getCanonicalSourceFile());
      feedback = new Content("This question does not have any correct answers!");
    } else if (null == clozeQuestion.getItems() || clozeQuestion.getItems().isEmpty()) {
      log.error("ItemQuestion does not have any items. " + question.getId() + " src: "
          + question.getCanonicalSourceFile());
      feedback = new Content("This question does not have any items to choose from!");
    }

    // STEP 1: Did they provide a valid answer?

    if (null == feedback) {
      if (null == submittedChoice.getItems() || submittedChoice.getItems().isEmpty()) {
        feedback = new Content("You did not provide an answer.");
      } else if (submittedChoice.getItems().stream().anyMatch(i -> i.getClass() != Item.class)) {
        feedback = new Content("Your answer is not in a recognised format!");
      } else {
        allowedItemIds =
            Stream.concat(Stream.of(NULL_CLOZE_ITEM_ID), clozeQuestion.getItems().stream().map(Item::getId))
                .collect(Collectors.toSet());
        submittedItemIds = submittedChoice.getItems().stream().map(Item::getId).collect(Collectors.toList());
        if (!allowedItemIds.containsAll(submittedItemIds)) {
          feedback = new Content("Your answer contained unrecognised items.");
        } else if (submittedItemIds.stream().allMatch(NULL_CLOZE_ITEM_ID::equals)) {
          // At least one item in the list must be non-null, else this is a blank submission!
          feedback = new Content("You did not provide an answer!");
        }
      }
    }

    // STEP 2: If they did, does their answer match a known answer?

    if (null == feedback) {
      // Sort the choices so that we match incorrect choices last, taking precedence over correct ones.
      List<Choice> orderedChoices = getOrderedChoices(clozeQuestion.getChoices());

      // For all the choices on this question...
      for (Choice c : orderedChoices) {

        // ... that are ItemChoices (and not subclasses) ...
        if (!ItemChoice.class.equals(c.getClass())) {
          log.error(String.format("Expected ItemChoice in question (%s), instead found %s!", clozeQuestion.getId(),
              c.getClass().toString()));
          continue;
        }

        ItemChoice itemChoice = (ItemChoice) c;

        // ... and that have valid items ...
        if (null == itemChoice.getItems() || itemChoice.getItems().isEmpty()) {
          log.error(String.format("Expected list of Items, but none found in choice for question id (%s)!",
              clozeQuestion.getId()));
          continue;
        }
        if (itemChoice.getItems().stream().anyMatch(i -> i.getClass() != Item.class)) {
          log.error(String.format("Expected list of Items, but something else found in choice for question id (%s)!",
              clozeQuestion.getId()));
          continue;
        }
        List<String> trustedChoiceItemIds =
            itemChoice.getItems().stream().map(Item::getId).collect(Collectors.toList());

        // ... look for a match to the submitted answer.
        if (trustedChoiceItemIds.size() != submittedItemIds.size()) {
          if (itemChoice.isCorrect()) {
            /* Assume a correct choice has correct number of gaps. (Legacy incorrect options may be missing items!).
               It should not be possible to cause this size mismatch from a compliant frontend; it should submit null
               placeholders.
             */
            if (trustedChoiceItemIds.size() > submittedItemIds.size()) {
              feedback = new Content("You did not provide a valid answer; it does not contain an item for each gap.");
            } else {
              feedback = new Content("You did not provide a valid answer; it contains more items than gaps.");
            }
          }
          // If the lengths aren't equal, we can't compare to this choice:
          continue;
        }

        boolean allowSubsetMatch = null != itemChoice.isAllowSubsetMatch() && itemChoice.isAllowSubsetMatch();

        boolean submissionMatches = true;
        List<Boolean> itemMatches = Lists.newArrayListWithCapacity(trustedChoiceItemIds.size());
        for (int i = 0; i < trustedChoiceItemIds.size(); i++) {
          boolean itemMatch = true;
          String trustedItemId = trustedChoiceItemIds.get(i);
          String submittedItemId = submittedItemIds.get(i);

          if (NULL_CLOZE_ITEM_ID.equals(trustedItemId)) {
            // It doesn't matter what the submission has here, but only if we are allowed subset matching:
            if (!allowSubsetMatch) {
              log.error(String.format("ItemChoice does not allow subset match but contains NULL item in question (%s)!",
                  clozeQuestion.getId()));
              itemMatch = false;
            }
          } else {
            if (!trustedItemId.equals(submittedItemId)) {
              itemMatch = false;
            }
          }
          submissionMatches = submissionMatches && itemMatch;
          itemMatches.add(itemMatch);
        }

        // If this is the first correct choice, the status of each item might be useful feedback:
        if (null == itemsCorrect && itemChoice.isCorrect() && detailedItemFeedback) {
          itemsCorrect = itemMatches;
        }

        // Did we match this choice?
        if (submissionMatches) {
          responseCorrect = itemChoice.isCorrect();
          feedback = (Content) itemChoice.getExplanation();
          // Since choices are ordered, this is the best we can do, so stop looking:
          break;
        }
      }
    }

    // STEP 3: If we still have no feedback to give, use the question's default feedback if any to use:
    if (feedbackIsNullOrEmpty(feedback) && null != clozeQuestion.getDefaultFeedback()) {
      feedback = clozeQuestion.getDefaultFeedback();
    }

    return new ItemValidationResponse(question.getId(), answer, responseCorrect, itemsCorrect, feedback, new Date());
  }

  @Override
  public List<Choice> getOrderedChoices(final List<Choice> choices) {
    return IsaacItemQuestionValidator.getOrderedChoicesWithSubsets(choices);
  }

}
