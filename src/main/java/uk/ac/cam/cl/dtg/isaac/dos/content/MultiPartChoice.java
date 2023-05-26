package uk.ac.cam.cl.dtg.isaac.dos.content;
import uk.ac.cam.cl.dtg.isaac.dto.content.MultiPartChoiceDTO;
import java.util.List;

@DTOMapping(MultiPartChoiceDTO.class)
@JsonContentType("multiPartChoice")
public class MultiPartChoice extends Choice {
    private List<List<Choice>> itemCategories;
    private List<Integer> choiceTemplate;

    public MultiPartChoice() {
    }

    public final List<List<Choice>> getItemCategories() {
        return itemCategories;
    }

    public final void setItemCategories(final List<List<Choice>> itemCategories) {
        this.itemCategories = itemCategories;
    }

    public final List<Integer> getChoiceTemplate() {
        return choiceTemplate;
    }

    public final void setChoiceTemplate(final List<Integer> choiceTemplate) {
        this.choiceTemplate = choiceTemplate;
    }
}
