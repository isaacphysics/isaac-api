package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacMultiPartQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

@ValidatesWith(IsaacMultiPartQuestionValidator.class)
public class MultiPartChoiceDTO extends ChoiceDTO {
    private List<ContentBaseDTO> choices;

    /**
     * Default constructor required for mapping.
     */
    public MultiPartChoiceDTO() {
    }

    public List<ContentBaseDTO> getChoices() {
        return choices;
    }

    public void setChoices(final List<ContentBaseDTO> choices) {
        this.choices = choices;
    }
}
