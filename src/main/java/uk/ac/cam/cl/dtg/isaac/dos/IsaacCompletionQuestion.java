package uk.ac.cam.cl.dtg.isaac.dos;

import uk.ac.cam.cl.dtg.isaac.dos.content.DTOMapping;
import uk.ac.cam.cl.dtg.isaac.dos.content.JsonContentType;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCompletionQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.quiz.IsaacCompletionQuestionValidator;
import uk.ac.cam.cl.dtg.isaac.quiz.ValidatesWith;

/**
 * Content DO for IsaacCompletionQuestions.
 *
 */
@DTOMapping(IsaacCompletionQuestionDTO.class)
@JsonContentType("isaacCompletionQuestion")
@ValidatesWith(IsaacCompletionQuestionValidator.class)
public class IsaacCompletionQuestion extends IsaacQuestionBase {

}


