package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.api.Constants;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.AbstractSegueUserDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;

/**
 * A class to augment content with user attempt information.
 */
public class UserAttemptManager {
    private final QuestionManager questionManager;

    @Inject
    public UserAttemptManager(final QuestionManager questionManager) {
        this.questionManager = questionManager;
    }

    /**
     * A method which augments related questions with attempt information.
     *
     * i.e. sets whether the related content summary has been completed.
     *
     * @param content the content to be augmented.
     * @param usersQuestionAttempts the user's question attempts.
     */
    public void augmentRelatedQuestionsWithAttemptInformation(
            final ContentDTO content,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> usersQuestionAttempts) {
        // Check if all question parts have been answered
        List<ContentSummaryDTO> relatedContentSummaries = content.getRelatedContent();
        if (relatedContentSummaries != null) {
            for (ContentSummaryDTO relatedContentSummary : relatedContentSummaries) {
                augmentContentSummaryWithAttemptInformation(relatedContentSummary, usersQuestionAttempts);
            }
        }
        // for all children recurse
        List<ContentBaseDTO> children = content.getChildren();
        if (children != null) {
            for (ContentBaseDTO child : children) {
                if (child instanceof ContentDTO) {
                    ContentDTO childContent = (ContentDTO) child;
                    augmentRelatedQuestionsWithAttemptInformation(childContent, usersQuestionAttempts);
                }
            }
        }
    }

    /**
     * Augment a ContentSummary object with question attempt information.
     *
     * @param contentSummary - the ContentSummaryDTO of a question page object.
     * @param usersQuestionAttempts - the user's question attempts.
     */
    public static void augmentContentSummaryWithAttemptInformation(
            final ContentSummaryDTO contentSummary,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> usersQuestionAttempts) {

        if (!QUESTION_PAGE_TYPES_SET.contains(contentSummary.getType())) {
            // Do not augment non-question pages.
            return;
        }

        String questionId = contentSummary.getId();
        Map<String, ? extends List<? extends LightweightQuestionValidationResponse>> questionAttempts = usersQuestionAttempts.get(questionId);
        boolean questionAnsweredCorrectly = false;
        int questionPartsCorrect = 0;
        int questionPartsIncorrect = 0;
        int questionPartsTotal = contentSummary.getQuestionPartIds().size();
        if (questionAttempts != null) {
            for (String relatedQuestionPartId : contentSummary.getQuestionPartIds()) {
                List<? extends LightweightQuestionValidationResponse> questionPartAttempts = questionAttempts.get(relatedQuestionPartId);
                if (questionPartAttempts != null) {
                    for (LightweightQuestionValidationResponse partAttempt : questionPartAttempts) {
                        questionAnsweredCorrectly = partAttempt.isCorrect();
                        if (questionAnsweredCorrectly) {
                            questionPartsCorrect++;
                            break; // exit on first correct attempt
                        }
                    }
                    if (!questionAnsweredCorrectly) {
                        questionPartsIncorrect++;
                    }
                }
            }
        }
        CompletionState state = getCompletionState(questionPartsTotal, questionPartsCorrect, questionPartsIncorrect);
        contentSummary.setState(state);
    }

    /**
     *  Get a CompletionState from question part attempt correct, incorrect and total counts.
     *
     * @param questionPartsTotal total number of question parts.
     * @param questionPartsCorrect total answered correctly.
     * @param questionPartsIncorrect total answered incorrectly.
     * @return the state of the question
     */
    public static CompletionState getCompletionState(final int questionPartsTotal, final int questionPartsCorrect,
                                                      final int questionPartsIncorrect) {
        int questionPartsNotAttempted = questionPartsTotal - (questionPartsCorrect + questionPartsIncorrect);

        CompletionState state;
        if (questionPartsCorrect == questionPartsTotal) {
            state = CompletionState.ALL_CORRECT;
        } else if (questionPartsIncorrect == questionPartsTotal) {
            state = CompletionState.ALL_INCORRECT;
        } else if (questionPartsNotAttempted == questionPartsTotal) {
            state = CompletionState.NOT_ATTEMPTED;
        } else if (questionPartsNotAttempted > 0) {
            state = CompletionState.IN_PROGRESS;
        } else {
            state = CompletionState.ALL_ATTEMPTED;
        }
        return state;
    }

    /**
     * Augment a list of content summary objects with user attempt information.
     * @param user the user to augment the content summary list for.
     * @param summarizedResults the content summary list to augment.
     * @return the augmented content summary list.
     * @throws SegueDatabaseException if there is an error retrieving question attempts.
     */
    public List<ContentSummaryDTO> augmentContentSummaryListWithAttemptInformation(AbstractSegueUserDTO user, List<ContentSummaryDTO> summarizedResults) throws SegueDatabaseException {

        Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> questionAttempts;

        if (user instanceof RegisteredUserDTO) {
            // Load only relevant attempts:
            RegisteredUserDTO registeredUser = (RegisteredUserDTO) user;
            List<String> questionPageIds = summarizedResults.stream().map(ContentSummaryDTO::getId).collect(Collectors.toList());
            questionAttempts = questionManager.getMatchingLightweightQuestionAttempts(Collections.singletonList(registeredUser), questionPageIds)
                    .getOrDefault(registeredUser.getId(), Collections.emptyMap());
        } else {
            // For anon users, all attempts are in one place so just load all:
            questionAttempts = questionManager.getQuestionAttemptsByUser(user);
        }

        for (ContentSummaryDTO result : summarizedResults) {
            augmentContentSummaryWithAttemptInformation(result, questionAttempts);
        }

        return summarizedResults;
    }
}
