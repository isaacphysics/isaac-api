package uk.ac.cam.cl.dtg.isaac.dos.content;

import uk.ac.cam.cl.dtg.isaac.dto.content.MultiPartAnswerDTO;

import java.util.Map;

@DTOMapping(MultiPartAnswerDTO.class)
@JsonContentType("multiPartAnswer")
public class MultiPartAnswer extends Choice {
    private Map<Integer, Choice> answers;

    public Map<Integer, Choice> getAnswers() {
        return answers;
    }

    public void setAnswers(final Map<Integer, Choice> answers) {
        this.answers = answers;
    }
}
