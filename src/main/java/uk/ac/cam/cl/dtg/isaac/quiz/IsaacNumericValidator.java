package uk.ac.cam.cl.dtg.isaac.quiz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.models.content.IsaacNumericQuestion;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.Quantity;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 * 
 *
 */
public class IsaacNumericValidator implements IValidator{
	private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);
	
	@Override
	public QuestionValidationResponse validateQuestionResponse(Question question, Choice answer) {
		if (question instanceof IsaacNumericQuestion){
			
			IsaacNumericQuestion choiceQuestion = (IsaacNumericQuestion) question;
			
			if(answer instanceof Quantity){
				
				Quantity answerFromUser = (Quantity) answer;
				
				for(Choice c : choiceQuestion.getChoices()){
					if(c instanceof Quantity){
						Quantity quantityChoice = (Quantity) c;
						if(answerFromUser.getValue().equals(quantityChoice.getValue()) && answerFromUser.getUnit().equals(quantityChoice.getUnit())){
							return new QuestionValidationResponse(question.getId(), answerFromUser.getValue() + " " + answerFromUser.getUnit(), quantityChoice.isCorrect(), (Content) quantityChoice.getExplanation());
						}					
					}
				}

				// tell them they got it wrong but we cannot find an explanation for why.
				return new QuestionValidationResponse(question.getId(), answerFromUser.getValue() + " " + answerFromUser.getUnit(), false, null);
			}
			else
			{
				log.error("Incorrect answer type received. Expected Quantity. Received: " + answer.getClass());
				throw new IllegalArgumentException("This type of question requires a quantity object instead of a choice");			
			}			
		}
		else
		{
			log.error("Incorrect validator used for question: " + question.getId());
			throw new IllegalArgumentException("This validator only works with Isaac Numeric Questions...");
		}
	}
}
