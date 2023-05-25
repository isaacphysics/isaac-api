package uk.ac.cam.cl.dtg.isaac.dos.content;
import uk.ac.cam.cl.dtg.isaac.dto.content.ChoiceDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.MultiPartChoiceDTO;
import java.util.List;

@DTOMapping(MultiPartChoiceDTO.class)
@JsonContentType("multiPartChoice")
public class MultiPartChoice extends Choice {
    private List<List<ChoiceDTO>> itemCategories;
    private List<Integer> choiceTemplate;

    public MultiPartChoice() {
    }

    public final List<List<ChoiceDTO>> getItemCategories() {
        return itemCategories;
    }

    public final void setItemCategories(final List<List<ChoiceDTO>> itemCategories) {
        this.itemCategories = itemCategories;
    }

    public final List<Integer> getChoiceTemplate() {
        return choiceTemplate;
    }

    public final void setChoiceTemplate(final List<Integer> choiceTemplate) {
        this.choiceTemplate = choiceTemplate;
    }
}
