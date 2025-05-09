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

    private void augmentContentSummaryWithAttemptInformation(
            final ContentSummaryDTO contentSummary,
            final Map<String, ? extends Map<String, ? extends List<? extends LightweightQuestionValidationResponse>>> usersQuestionAttempts) {

        if (!QUESTION_PAGE_TYPES_SET.contains(contentSummary.getType())) {
            // Do not augment non-question pages.
            return;
        }

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
        if (attempted) {
            if (questionAnsweredCorrectly) {
                contentSummary.setState(Constants.CompletionState.ALL_CORRECT);
            } else {
                contentSummary.setState(Constants.CompletionState.IN_PROGRESS);
            }
        } else {
            contentSummary.setState(Constants.CompletionState.NOT_ATTEMPTED);
        }
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
