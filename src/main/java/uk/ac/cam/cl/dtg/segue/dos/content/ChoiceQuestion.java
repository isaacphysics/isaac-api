package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;
import java.util.Set;

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.segue.quiz.ChoiceQuestionValidator;
import uk.ac.cam.cl.dtg.segue.quiz.ValidatesWith;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Choice object The choice object is a specialised form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@DTOMapping(ChoiceQuestionDTO.class)
@JsonType("choiceQuestion")
@ValidatesWith(ChoiceQuestionValidator.class)
public class ChoiceQuestion extends Question {

	protected List<Choice> choices;

	public ChoiceQuestion() {
		
	}
	
	/**
	 * Gets the choices.
	 * @return the choices
	 */
	public final List<Choice> getChoices() {
		return choices;
	}

	/**
	 * Sets the choices.
	 * @param choices the choices to set
	 */
	public final void setChoices(List<Choice> choices) {
		this.choices = choices;
	}


}
