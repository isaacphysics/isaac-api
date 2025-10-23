package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dto.LLMFreeTextQuestionValidationResponseDTO;

import java.util.Date;
import java.util.List;

@DTOMapping(LLMFreeTextQuestionValidationResponseDTO.class)
public class LLMFreeTextQuestionValidationResponse extends QuestionValidationResponse {
    private List<LLMFreeTextMarkSchemeEntry> markBreakdown;

    public LLMFreeTextQuestionValidationResponse() {
    }

    // This constructor is included as a mechanism to consider the need to update this class if the superclass is changed
    public LLMFreeTextQuestionValidationResponse(final String questionId, final Choice answer, final Boolean correct,
                                      final Content explanation, final Date dateAttempted) {
        super(questionId, answer, correct, explanation, dateAttempted);
    }

    public Integer getMarksAwarded() {
        return super.getMarks();
    }
    public void setMarksAwarded(Integer marksAwarded) {
        super.setMarks(marksAwarded);
    }

    public List<LLMFreeTextMarkSchemeEntry> getMarkBreakdown() {
        return markBreakdown;
    }
    public void setMarkBreakdown(List<LLMFreeTextMarkSchemeEntry> markBreakdown) {
        this.markBreakdown = markBreakdown;
    }
}
