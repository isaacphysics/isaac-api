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
    private String markCalculationInstructions;
    private String additionalMarkingInstructions;
    private Integer maxMarks;
    private Integer marksAwarded;
    private List<LLMFreeTextMarkSchemeEntry> markBreakdown;

    public LLMFreeTextQuestionValidationResponse() {
    }

    // This constructor is included as a mechanism to consider the need to update this class if the superclass is changed
    public LLMFreeTextQuestionValidationResponse(final String questionId, final Choice answer, final Boolean correct,
                                      final Content explanation, final Date dateAttempted) {
        super(questionId, answer, correct, explanation, dateAttempted);
    }

    public String getMarkCalculationInstructions() {
        return markCalculationInstructions;
    }
    public void setMarkCalculationInstructions(String markCalculationInstructions) {
        this.markCalculationInstructions = markCalculationInstructions;
    }

    public String getAdditionalMarkingInstructions() {
        return additionalMarkingInstructions;
    }
    public void setAdditionalMarkingInstructions(String additionalMarkingInstructions) {
        this.additionalMarkingInstructions = additionalMarkingInstructions;
    }

    public Integer getMaxMarks() {
        return maxMarks;
    }
    public void setMaxMarks(Integer maxMarks) {
        this.maxMarks = maxMarks;
    }

    public Integer getMarksAwarded() {
        return marksAwarded;
    }
    public void setMarksAwarded(Integer marksAwarded) {
        this.marksAwarded = marksAwarded;
    }

    public List<LLMFreeTextMarkSchemeEntry> getMarkBreakdown() {
        return markBreakdown;
    }
    public void setMarkBreakdown(List<LLMFreeTextMarkSchemeEntry> markBreakdown) {
        this.markBreakdown = markBreakdown;
    }
}
