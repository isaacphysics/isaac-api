package uk.ac.cam.cl.dtg.isaac.dto;

import uk.ac.cam.cl.dtg.isaac.dto.content.LLMFreeTextMarkSchemeEntryDTO;

import java.util.List;

public class LLMFreeTextQuestionValidationResponseDTO extends QuestionValidationResponseDTO {
    private List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown;

    public LLMFreeTextQuestionValidationResponseDTO() {
    }

    public Integer getMarksAwarded() {
        return super.getMarks();
    }
    public void setMarksAwarded(Integer marksAwarded) {
        super.setMarks(marksAwarded);
    }

    public List<LLMFreeTextMarkSchemeEntryDTO> getMarkBreakdown() {
        return markBreakdown;
    }
    public void setMarkBreakdown(List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown) {
        this.markBreakdown = markBreakdown;
    }
}
