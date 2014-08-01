package uk.ac.cam.cl.dtg.segue.dto;

import uk.ac.cam.cl.dtg.segue.dos.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceDTO;

/**
 * Question Validation Response DTO.
 *
 */
public class QuestionValidationResponseDTO {
	private String questionId;
	private ChoiceDTO answer;
	private boolean correct;
	private Content explanation;

	public QuestionValidationResponseDTO() {

	}

	public QuestionValidationResponseDTO(String questionId, ChoiceDTO answer,
			boolean correct, Content explanation) {
		this.questionId = questionId;
		this.answer = answer;
		this.correct = correct;
		this.explanation = explanation;
	}

	/**
	 * Gets the questionId.
	 * @return the questionId
	 */
	public final String getQuestionId() {
		return questionId;
	}

	/**
	 * Sets the questionId.
	 * @param questionId the questionId to set
	 */
	public final void setQuestionId(final String questionId) {
		this.questionId = questionId;
	}

	/**
	 * Gets the answer.
	 * @return the answer
	 */
	public final ChoiceDTO getAnswer() {
		return answer;
	}

	/**
	 * Sets the answer.
	 * @param answer the answer to set
	 */
	public final void setAnswer(final ChoiceDTO answer) {
		this.answer = answer;
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
	public final Content getExplanation() {
		return explanation;
	}

	/**
	 * Sets the explanation.
	 * @param explanation the explanation to set
	 */
	public final void setExplanation(final Content explanation) {
		this.explanation = explanation;
	}
}
