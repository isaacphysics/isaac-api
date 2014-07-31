package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.quiz.ChoiceQuestionValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

/**
 * Choice object The choice object is a specialised form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@ValidatesWith(ChoiceQuestionValidator.class)
public class ChoiceQuestionDTO extends QuestionDTO {

	protected List<ChoiceDTO> choices;

	/**
	 * Default Constructor for mappers.
	 */
	public ChoiceQuestionDTO() {
		super();
	}

	/**
	 * Gets the choices.
	 * @return the choices
	 */
	public final List<ChoiceDTO> getChoices() {
		return choices;
	}

	/**
	 * Sets the choices.
	 * @param choices the choices to set
	 */
	public final void setChoices(List<ChoiceDTO> choices) {
		this.choices = choices;
	}


}
