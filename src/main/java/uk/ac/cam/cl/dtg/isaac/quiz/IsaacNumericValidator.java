package uk.ac.cam.cl.dtg.isaac.quiz;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.segue.api.QuestionManager;
import uk.ac.cam.cl.dtg.segue.dto.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.segue.dto.content.Choice;
import uk.ac.cam.cl.dtg.segue.dto.content.Content;
import uk.ac.cam.cl.dtg.segue.dto.content.Question;
import uk.ac.cam.cl.dtg.segue.quiz.IMultiFieldQuestion;
import uk.ac.cam.cl.dtg.segue.quiz.IMultiFieldValidator;
import uk.ac.cam.cl.dtg.segue.quiz.IValidator;

/**
 * Validator that only provides functionality to validate Numeric questions.
 * 
 *
 */
public class IsaacNumericValidator implements IMultiFieldValidator {
	private static final Logger log = LoggerFactory.getLogger(IsaacNumericValidator.class);
	
	@Override
	public QuestionValidationResponse validateQuestionResponse(Question question, Choice answer) {
		// to conform to the interface we could implement this I suppose and just assume that only the first field is being evaluated? Unsure if this is useful.
		log.error("This validator does not support single field validation.");
		throw new UnsupportedOperationException("This validator does not support single question validation");
	}

	@Override
	public List<QuestionValidationResponse> validateMultiFieldQuestionResponses(Question question, List<Choice> answers) {

		// Some of this multifield question stuff can probably be pulled out somewhere common - it depends if we need it for anything else.
		if(question instanceof IMultiFieldQuestion){
			IMultiFieldQuestion mfq = (IMultiFieldQuestion) question;
			
			List<? extends Question> subquestions = mfq.getFields();
			
			List<QuestionValidationResponse> result = new ArrayList<QuestionValidationResponse>();
			int index = 0;
			for(Question subquestion : subquestions){
				
				if(subquestions.size() > answers.size()){
					// then they haven't answered all of the questions.
					result.add(new QuestionValidationResponse(question.getId(),null, false, new Content("You have not answered all components of this question.")));
					return result;
				}
				
				IValidator validator = QuestionManager.locateValidator(subquestion.getClass());
				// assume that list of answers is ordered
				
				result.add(validator.validateQuestionResponse(subquestion, answers.get(index)));
				
				index++;
			}
			
			return result;
		}
		else{
			log.error("Incorrect question type trying to use this validator. All questions wishing to use this validator must implement IMultiFieldQuestion.");
			throw new IllegalArgumentException("Question type is " + question.getClass() + " is not valid for the " + this.getClass() + " validator.");
		}
	}
}
