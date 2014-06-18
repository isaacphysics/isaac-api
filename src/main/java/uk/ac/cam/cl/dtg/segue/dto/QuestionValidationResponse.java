package uk.ac.cam.cl.dtg.segue.dto;


public class QuestionValidationResponse {
	private String questionId;
	private String answer;
	private boolean correct;
	private Content explanation;
	
	public QuestionValidationResponse(){
		
	}
	
	public QuestionValidationResponse(String questionId, String answer, boolean correct, Content explanation){
		this.questionId = questionId;
		this.answer = answer;
		this.correct = correct;
		this.explanation = explanation;
	}
	
	public String getQuestionId() {
		return questionId;
	}
	
	public void setQuestionId(String questionId) {
		this.questionId = questionId;
	}
	
	public String getAnswer() {
		return answer;
	}
	
	public void setAnswer(String answer) {
		this.answer = answer;
	}
	
	public boolean isCorrect() {
		return correct;
	}
	
	public void setCorrect(boolean correct) {
		this.correct = correct;
	}
	
	public Content getExplanation() {
		return explanation;
	}
	
	public void setExplanation(Content explanation) {
		this.explanation = explanation;
	}
}
