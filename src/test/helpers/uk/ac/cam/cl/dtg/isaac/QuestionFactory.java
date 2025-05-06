
package uk.ac.cam.cl.dtg.isaac;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.marks.Mark;

import static uk.ac.cam.cl.dtg.isaac.marks.Mark.*;

public class QuestionFactory {
    public static IsaacLLMFreeTextQuestion someQuestion() {
        return createLLMFreeTextQuestion(
                mark().setReasonFoo(1).setReasonBar(1).setReasonFizz(1),
                1,
                emptyExamples(),
                null
        );
    }

    public static IsaacLLMFreeTextQuestion createLLMFreeTextQuestion(final Mark mark,
                                                                     final Integer maxMarks,
                                                                     final List<LLMFreeTextMarkedExample> markedExamples,
                                                                     final LLMMarkingExpression markingFormula) {
        IsaacLLMFreeTextQuestion question = new IsaacLLMFreeTextQuestion();
        question.setMarkScheme((mark != null) ? mark.toMarkScheme() : null);
        question.setMaxMarks(maxMarks);
        question.setMarkedExamples(markedExamples);
        question.setMarkingFormula(markingFormula);

        return question;
    }

    public static List<LLMFreeTextMarkedExample> emptyExamples() {
        return new ArrayList<>();
    }
}
