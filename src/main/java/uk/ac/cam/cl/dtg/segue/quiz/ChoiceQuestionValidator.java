package uk.ac.cam.cl.dtg.segue.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.ChoiceQuestion;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;

public class ChoiceQuestionValidator implements Validator {
	
	private static final Logger log = LoggerFactory.getLogger(ChoiceQuestionValidator.class);
	
	@Override
	public QuestionValidationResponse validateQuestionResponse(
			Question question, String answer) {
		
		// check that the question is of type ChoiceQuestion before we go ahead
		ChoiceQuestion choiceQuestion = null;
		if(question instanceof ChoiceQuestion){
			choiceQuestion = (ChoiceQuestion) question;
			
			for(Choice choice : choiceQuestion.getChoices()){
				if(choice.getValue().equals(answer)){
					return new QuestionValidationResponse(question.getId(), answer, choice.isCorrect(), (Content) choice.getExplanation());
				}
			}
			
			log.info("Unable to find choice for question ( " + question.getId() + " ) matching the answer supplied (" + answer + "). Returning that it is incorrect with out an explanation.");
			
			return new QuestionValidationResponse(question.getId(), answer, false, null);
		}
		else{
			log.error("Expected to be able to cast the question as a ChoiceQuestion but this cast failed.");
			throw new ClassCastException("Incorrect type of question received. Unable to validate.");
		}		
	}

}
