package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.MultiPartAnswerDTO;

import java.util.List;
import java.util.Map;

@DTOMapping(MultiPartAnswerDTO.class)
@JsonContentType("multiPartAnswer")
public class MultiPartAnswer extends Choice {
    private Map<Integer, ChoiceDTO> answers;

    public Map<Integer, ChoiceDTO> getAnswers() {
        return answers;
    }

    public void setAnswers(final Map<Integer, ChoiceDTO> answers) {
        this.answers = answers;
    }
}
