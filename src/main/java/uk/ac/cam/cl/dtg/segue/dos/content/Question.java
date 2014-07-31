package uk.ac.cam.cl.dtg.segue.dos.content;

import java.util.List;
import uk.ac.cam.cl.dtg.segue.dto.content.QuestionDTO;

/**
 * Choice object The choice object is a specialized form of content and allows
 * the storage of data relating to possible answers to questions.
 * 
 */
@DTOMapping(QuestionDTO.class)
@JsonType("question")
public class Question extends Content {

	protected ContentBase answer;
	protected List<ContentBase> hints;

	public Question() {

	}

	/**
	 * Gets the answer.
	 * @return the answer
	 */
	public final ContentBase getAnswer() {
		return answer;
	}

	/**
	 * Sets the answer.
	 * @param answer the answer to set
	 */
	public final void setAnswer(final ContentBase answer) {
		this.answer = answer;
	}

	/**
	 * Gets the hints.
	 * @return the hints
	 */
	public final List<ContentBase> getHints() {
		return hints;
	}

	/**
	 * Sets the hints.
	 * @param hints the hints to set
	 */
	public final void setHints(final List<ContentBase> hints) {
		this.hints = hints;
	}
}
