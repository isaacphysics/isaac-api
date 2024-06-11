package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dos.content.LlmFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LlmFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.Question;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacLlmFreeTextQuestionDTO;

import java.util.List;

@DTOMapping(IsaacLlmFreeTextQuestionDTO.class)
@JsonContentType("isaacLlmFreeTextQuestion")
public class IsaacLlmFreeTextQuestion extends Question {
    private String promptInstructionOverride;
    private List<LlmFreeTextMarkSchemeEntry> markScheme;
    private Integer maxMarks;
    private String additionalMarkingInstructions;
    private String markCalculationInstructions;
    private List<LlmFreeTextMarkedExample> markedExamples;

    public IsaacLlmFreeTextQuestion() {
    }

    public String getPromptInstructionOverride() {
        return promptInstructionOverride;
    }
    public void setPromptInstructionOverride(String promptInstructionOverride) {
        this.promptInstructionOverride = promptInstructionOverride;
    }

    public List<LlmFreeTextMarkSchemeEntry> getMarkScheme() {
        return markScheme;
    }
    public void setMarkScheme(List<LlmFreeTextMarkSchemeEntry> markScheme) {
        this.markScheme = markScheme;
    }

    public Integer getMaxMarks() {
        return maxMarks;
    }
    public void setMaxMarks(Integer maxMarks) {
        this.maxMarks = maxMarks;
    }

    public String getAdditionalMarkingInstructions() {
        return additionalMarkingInstructions;
    }
    public void setAdditionalMarkingInstructions(String additionalMarkingInstructions) {
        this.additionalMarkingInstructions = additionalMarkingInstructions;
    }

    public String getMarkCalculationInstructions() {
        return markCalculationInstructions;
    }
    public void setMarkCalculationInstructions(String markCalculationInstructions) {
        this.markCalculationInstructions = markCalculationInstructions;
    }

    public List<LlmFreeTextMarkedExample> getMarkedExamples() {
        return markedExamples;
    }
    public void setMarkedExamples(List<LlmFreeTextMarkedExample> markedExamples) {
        this.markedExamples = markedExamples;
    }
}
