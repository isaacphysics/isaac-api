package uk.ac.cam.cl.dtg.isaac.dto.content;

import uk.ac.cam.cl.dtg.isaac.quiz.IsaacMultiPartQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

import java.util.List;

@ValidatesWith(IsaacMultiPartQuestionValidator.class)
public class MultiPartChoiceDTO extends ChoiceDTO {
    private List<List<ChoiceDTO>> itemCategories;
    private List<Integer> choiceTemplate;

    public MultiPartChoiceDTO() {
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
