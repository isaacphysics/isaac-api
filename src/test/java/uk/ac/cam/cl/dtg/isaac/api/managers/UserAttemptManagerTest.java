package uk.ac.cam.cl.dtg.isaac.api.managers;

import com.google.api.client.util.Lists;
import com.google.api.client.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.dtg.isaac.dos.LightweightQuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentSummaryDTO;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static uk.ac.cam.cl.dtg.isaac.api.Constants.*;
import static uk.ac.cam.cl.dtg.segue.api.managers.QuestionManager.extractPageIdFromQuestionId;

public class UserAttemptManagerTest {

    private static final Logger log = LoggerFactory.getLogger(UserAttemptManagerTest.class);
    private final String QUESTION_ID = "questionId";
    private final String QUESTION_PART_1_ID = "questionId|part1";
    private final String QUESTION_PART_2_ID = "questionId|part2";
    private final String QUESTION_PART_3_ID = "questionId|part3";

    private ContentSummaryDTO fakeQuestionSummary;
    private ContentSummaryDTO fakeConceptSummary;

    private final LightweightQuestionValidationResponse p1CorrectAttempt = new LightweightQuestionValidationResponse(QUESTION_PART_1_ID, true, null, 1);
    private final LightweightQuestionValidationResponse p1IncorrectAttempt = new LightweightQuestionValidationResponse(QUESTION_PART_1_ID, false, null, 0);
    private final LightweightQuestionValidationResponse p2CorrectAttempt = new LightweightQuestionValidationResponse(QUESTION_PART_2_ID, true, null, 1);
    private final LightweightQuestionValidationResponse p2IncorrectAttempt = new LightweightQuestionValidationResponse(QUESTION_PART_2_ID, false, null, 0);
    private final LightweightQuestionValidationResponse p3CorrectAttempt = new LightweightQuestionValidationResponse(QUESTION_PART_3_ID, true, null, 1);
    private final LightweightQuestionValidationResponse p3IncorrectAttempt = new LightweightQuestionValidationResponse(QUESTION_PART_3_ID, false, null, 0);

    private final LightweightQuestionValidationResponse someOtherCorrectAttempt = new LightweightQuestionValidationResponse("some-other-part-id", true, null, 1);

    @Before
    public void setUp() throws Exception {
        fakeQuestionSummary = new ContentSummaryDTO();
        fakeQuestionSummary.setId(QUESTION_ID);
        fakeQuestionSummary.setType(QUESTION_TYPE);
        fakeQuestionSummary.setQuestionPartIds(List.of(QUESTION_PART_1_ID, QUESTION_PART_2_ID, QUESTION_PART_3_ID));

        fakeConceptSummary = new ContentSummaryDTO();
        fakeConceptSummary.setId("conceptId");
        fakeConceptSummary.setType(CONCEPT_TYPE);
        fakeConceptSummary.setQuestionPartIds(Collections.emptyList());
    }


    /**
     * Convert a list of question attempts into a map by question IDs, preserving order.
     */
    private Map<String, Map<String, List<LightweightQuestionValidationResponse>>> buildAttemptsMap(final List<LightweightQuestionValidationResponse> attempts) {

        Map<String, Map<String, List<LightweightQuestionValidationResponse>>> result = Maps.newLinkedHashMap();

        for (LightweightQuestionValidationResponse attempt : attempts) {
            String questionPageId = extractPageIdFromQuestionId(attempt.getQuestionId());

            Map<String, List<LightweightQuestionValidationResponse>> attemptsForThisQuestionPage
                    = result.computeIfAbsent(questionPageId, k -> Maps.newLinkedHashMap());

            List<LightweightQuestionValidationResponse> listOfResponses
                    = attemptsForThisQuestionPage.computeIfAbsent(attempt.getQuestionId(), k -> Lists.newArrayList());

            listOfResponses.add(attempt);
        }

        return result;
    }

    /**
     * Check a list of attempts leads to a specific completion state.
     */
    private void assertCompletionState(final List<LightweightQuestionValidationResponse> attempts,
                                       final CompletionState state) {
        UserAttemptManager.augmentContentSummaryWithAttemptInformation(fakeQuestionSummary, buildAttemptsMap(attempts));

        assertEquals(state, fakeQuestionSummary.getState());
    }

    ///// end setup, begin test cases:

    @Test
    public void completionStatus_OnlyAllCorrectAttempts_AllCorrect() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                p1CorrectAttempt,
                p2CorrectAttempt,
                p3CorrectAttempt);

        assertCompletionState(attempts, CompletionState.ALL_CORRECT);
    }

    @Test
    public void completionStatus_AllCorrectWithOtherIncorrectAttempts_AllCorrect() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                p1IncorrectAttempt, p1CorrectAttempt, p1IncorrectAttempt, // wrong, RIGHT, wrong == correct overall
                p2CorrectAttempt,
                p3CorrectAttempt);

        assertCompletionState(attempts, CompletionState.ALL_CORRECT);
    }

    @Test
    public void completionStatus_AllIncorrectAllParts_AllIncorrect() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                p1IncorrectAttempt, p1IncorrectAttempt, p1IncorrectAttempt, // wrong, wrong, wrong == wrong overall
                p2IncorrectAttempt,
                p3IncorrectAttempt
        );

        assertCompletionState(attempts, CompletionState.ALL_INCORRECT);
    }

    @Test
    public void completionStatus_NoAttempts_NotAttempted() {
        List<LightweightQuestionValidationResponse> attempts = Collections.emptyList();  // no attempts at all

        assertCompletionState(attempts, CompletionState.NOT_ATTEMPTED);
    }

    @Test
    public void completionStatus_IrrelevantAttempts_NotAttempted() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                someOtherCorrectAttempt  // attempt at unrelated question
        );

        assertCompletionState(attempts, CompletionState.NOT_ATTEMPTED);
    }

    @Test
    public void completionStatus_IncorrectAttemptsOnlySomeParts_InProgress() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                p1IncorrectAttempt,
                p2IncorrectAttempt
                // no p3 attempt!
        );

        assertCompletionState(attempts, CompletionState.IN_PROGRESS);
    }

    @Test
    public void completionStatus_CorrectAttemptsOnlySomeParts_InProgress() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                p1CorrectAttempt,
                // no p2 attempt
                p3CorrectAttempt
        );

        assertCompletionState(attempts, CompletionState.IN_PROGRESS);
    }

    @Test
    public void completionStatus_SomeCorrectSomeIncorrectAllParts_AllAttempted() {
        List<LightweightQuestionValidationResponse> attempts = List.of(
                p1CorrectAttempt,
                p2IncorrectAttempt,
                p3CorrectAttempt
        ); // all parts attempted, not all correct, not all incorrect

        assertCompletionState(attempts, CompletionState.ALL_ATTEMPTED);
    }

    @Test
    public void completionStatus_ConceptPageNotQuestionPage_NoAugmentation() {
        List<LightweightQuestionValidationResponse> attempts = List.of(p1CorrectAttempt);  // irrelevant attempt

        UserAttemptManager.augmentContentSummaryWithAttemptInformation(fakeConceptSummary, buildAttemptsMap(attempts));

        assertNull(fakeQuestionSummary.getState());  // not augmented with NOT_ATTEMPTED!
    }


}
