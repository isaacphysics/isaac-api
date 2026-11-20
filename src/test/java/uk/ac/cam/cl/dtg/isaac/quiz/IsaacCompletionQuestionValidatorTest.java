package uk.ac.cam.cl.dtg.isaac.quiz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.cam.cl.dtg.isaac.api.managers.GameManager;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacCompletionQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.IsaacMultiChoiceQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.QuestionValidationResponse;
import uk.ac.cam.cl.dtg.isaac.dos.content.Choice;
import uk.ac.cam.cl.dtg.isaac.dos.content.Content;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacCompletionQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacMultiChoiceQuestionDTO;
import uk.ac.cam.cl.dtg.isaac.dto.IsaacQuestionPageDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentBaseDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.ContentDTO;
import uk.ac.cam.cl.dtg.isaac.dto.content.QuestionDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Small test class for the Completion Question Validator class.
 *
 */
public class IsaacCompletionQuestionValidatorTest {
    private IsaacCompletionQuestionValidator questionValidator;

    @BeforeEach
    public final void setUp() {
        questionValidator = new IsaacCompletionQuestionValidator();
    }

    @Test
    public final void anyAnswer_IsCorrect_WithDefaultFeedbackAsExplanation() {
        IsaacCompletionQuestion question = new IsaacCompletionQuestion();
        question.setId("page|section_complete");
        Content feedback = new Content("Good Job!");
        question.setDefaultFeedback(feedback);

        Choice answer = new Choice();
        answer.setValue("completed");

        QuestionValidationResponse questionValidationResponse = questionValidator.validateQuestionResponse(question, answer);

        assertTrue(questionValidationResponse.isCorrect());
        assertEquals("page|section_complete", questionValidationResponse.getQuestionId());
        assertSame(feedback, questionValidationResponse.getExplanation());
    }

    @Test
    public final void otherQuestionType_IsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> questionValidator.validateQuestionResponse(new IsaacMultiChoiceQuestion(), new Choice()));
    }


    @Test
    public final void completionQuestions_AreNotMarkableQuestionParts() {

        //Create two questions, put them in an accordion, and test that it returns only the markable one
        IsaacMultiChoiceQuestionDTO multiChoice = new IsaacMultiChoiceQuestionDTO();
        multiChoice.setId("page|multi_choice");

        IsaacCompletionQuestionDTO completion = new IsaacCompletionQuestionDTO();
        completion.setId("page|section_complete");

        ContentDTO firstSection = new ContentDTO();
        firstSection.setChildren(List.<ContentBaseDTO>of(multiChoice));
        ContentDTO secondSection = new ContentDTO();
        secondSection.setChildren(List.<ContentBaseDTO>of(completion));
        ContentDTO accordion = new ContentDTO();
        accordion.setChildren(List.<ContentBaseDTO>of(firstSection, secondSection));
        IsaacQuestionPageDTO page = new IsaacQuestionPageDTO();
        page.setChildren(List.<ContentBaseDTO>of(accordion));

        List<QuestionDTO> markableParts = GameManager.getAllMarkableQuestionPartsDFSOrder(page);

        assertEquals(List.of(multiChoice), markableParts);

    }

}


