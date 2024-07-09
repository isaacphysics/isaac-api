package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.content.LLMFreeTextMarkSchemeEntryDTO;

import java.util.List;

public class LLMFreeTextQuestionValidationResponseDTO extends QuestionValidationResponseDTO {
    private String markCalculationInstructions;
    private String additionalMarkingInstructions;
    private Integer maxMarks;
    private Integer marksAwarded;
    private List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown;

    public LLMFreeTextQuestionValidationResponseDTO() {
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

    public List<LLMFreeTextMarkSchemeEntryDTO> getMarkBreakdown() {
        return markBreakdown;
    }

    public void setMarkBreakdown(List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown) {
        this.markBreakdown = markBreakdown;
    }
}
