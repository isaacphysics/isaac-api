package uk.ac.cam.cl.dtg.isaac.dto.content;

import java.util.Map;

public class MultiPartAnswerDTO extends ChoiceDTO {
    private Map<Integer, ChoiceDTO> answers;

    public Map<Integer, ChoiceDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(final Map<Integer, ChoiceDTO> answers) {
        this.answers = answers;
    }
}
