package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.content.LLMFreeTextMarkSchemeEntryDTO;

import java.util.List;

public class LLMFreeTextQuestionValidationResponseDTO extends QuestionValidationResponseDTO {
    private Integer maxMarks;
    private Integer marksAwarded;
    private List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown;

    public LLMFreeTextQuestionValidationResponseDTO() {
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
