package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.inject.Inject;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;
import uk.ac.cam.cl.dtg.isaac.dto.users.RegisteredUserDTO;
import uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dao.SegueDatabaseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static uk.ac.cam.cl.dtg.isaac.api.Constants.CompletionState.*;

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

    private void augmentContentSummaryWithAttemptInformation(
            final ContentSummaryDTO contentSummary,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> usersQuestionAttempts) {
        String questionId = contentSummary.getId();
        Map<String, ? extends List<? extends LightweightQuestionValidationResponse>> questionAttempts = usersQuestionAttempts.get(questionId);
        boolean questionAnsweredCorrectly = false;
        boolean attempted = false;
        if (questionAttempts != null) {
            for (String relatedQuestionPartId : contentSummary.getQuestionPartIds()) {
                questionAnsweredCorrectly = false;
                List<? extends LightweightQuestionValidationResponse> questionPartAttempts = questionAttempts.get(relatedQuestionPartId);
                if (questionPartAttempts != null) {
                    attempted = true;
                    for (LightweightQuestionValidationResponse partAttempt : questionPartAttempts) {
                        questionAnsweredCorrectly = partAttempt.isCorrect();
                        if (questionAnsweredCorrectly) {
                            break; // exit on first correct attempt
                        }
                    }
                }
                if (!questionAnsweredCorrectly) {
                    break; // exit on first false question part
                }
            }
        }
        contentSummary.setState(attempted
                ? questionAnsweredCorrectly ? ALL_CORRECT : IN_PROGRESS
                : NOT_ATTEMPTED);
    }

    /**
     * Augment a list of content summary objects with user attempt information.
     * @param user the user to augment the content summary list for.
     * @param summarizedResults the content summary list to augment.
     * @return the augmented content summary list.
     * @throws SegueDatabaseException if there is an error retrieving question attempts.
     */
    public List<ContentSummaryDTO> augmentContentSummaryListWithAttemptInformation(RegisteredUserDTO user, List<ContentSummaryDTO> summarizedResults) throws SegueDatabaseException {
        List<String> questionPageIds = summarizedResults.stream().map(ContentSummaryDTO::getId).collect(Collectors.toList());

        Map<String, Map<String, List<LightweightQuestionValidationResponse>>> questionAttempts =
                questionManager.getMatchingLightweightQuestionAttempts(Collections.singletonList(user), questionPageIds)
                        .getOrDefault(user.getId(), Collections.emptyMap());

        for (ContentSummaryDTO result : summarizedResults) {
            augmentContentSummaryWithAttemptInformation(result, questionAttempts);
        }

        return summarizedResults;
    }
}
