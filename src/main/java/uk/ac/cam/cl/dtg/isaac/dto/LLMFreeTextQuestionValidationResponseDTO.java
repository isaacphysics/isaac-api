package uk.ac.cam.cl.dtg.isaac.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uk.ac.cam.cl.dtg.isaac.dto.content.LLMFreeTextMarkSchemeEntryDTO;

import java.util.List;

public class LLMFreeTextQuestionValidationResponseDTO extends QuestionValidationResponseDTO {
    private List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown;

    public LLMFreeTextQuestionValidationResponseDTO() {
    }

    public List<LLMFreeTextMarkSchemeEntryDTO> getMarkBreakdown() {
        return markBreakdown;
    }
    public void setMarkBreakdown(List<LLMFreeTextMarkSchemeEntryDTO> markBreakdown) {
        this.markBreakdown = markBreakdown;
    }
}
