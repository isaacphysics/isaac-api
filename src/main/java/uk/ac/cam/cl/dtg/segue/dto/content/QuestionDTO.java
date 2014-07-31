package uk.ac.cam.cl.dtg.segue.dto.content;

import java.util.List;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
public class QuestionDTO extends ContentDTO {
	protected ContentBaseDTO answer;
	protected List<ContentBaseDTO> hints;

	public QuestionDTO() {

	}

	/**
	 * Gets the answer.
	 * @return the answer
	 */
	public final ContentBaseDTO getAnswer() {
		return answer;
	}

	/**
	 * Sets the answer.
	 * @param answer the answer to set
	 */
	public void setAnswer(final ContentBaseDTO answer) {
		this.answer = answer;
	}

	/**
	 * Gets the hints.
	 * @return the hints
	 */
	public List<ContentBaseDTO> getHints() {
		return hints;
	}

	/**
	 * Sets the hints.
	 * @param hints the hints to set
	 */
	public void setHints(final List<ContentBaseDTO> hints) {
		this.hints = hints;
	}


}
