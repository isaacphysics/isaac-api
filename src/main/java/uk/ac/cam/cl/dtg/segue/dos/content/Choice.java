package uk.ac.cam.cl.dtg.segue.dos.content;

import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@DTOMapping(ChoiceDTO.class)
@JsonType("choice")
public class Choice extends Content {
	protected boolean correct;
	protected ContentBase explanation;

	/**
	 * Default Constructor required for mappers.
	 */
	public Choice() {
		
	}

	/**
	 * Gets the correct.
	 * @return the correct
	 */
	public final boolean isCorrect() {
		return correct;
	}

	/**
	 * Sets the correct.
	 * @param correct the correct to set
	 */
	public final void setCorrect(final boolean correct) {
		this.correct = correct;
	}

	/**
	 * Gets the explanation.
	 * @return the explanation
	 */
	public final ContentBase getExplanation() {
		return explanation;
	}

	/**
	 * Sets the explanation.
	 * @param explanation the explanation to set
	 */
	public final void setExplanation(final ContentBase explanation) {
		this.explanation = explanation;
	}
}
