package uk.ac.cam.cl.dtg.isaac.dos.content;
import uk.ac.cam.cl.dtg.isaac.dto.content.MultiPartChoiceDTO;
import java.util.List;

@DTOMapping(MultiPartChoiceDTO.class)
@JsonContentType("multiPartChoice")
public class MultiPartChoice extends Choice {
    private List<ContentBase> choices;

    /**
     * Default constructor required for mapping.
     */
    public MultiPartChoice() {
    }

    public List<ContentBase> getChoices() {
        return choices;
    }

    public void setChoices(final List<ContentBase> choices) {
        this.choices = choices;
    }
}
