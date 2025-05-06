
package uk.ac.cam.cl.dtg.isaac;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import static uk.ac.cam.cl.dtg.isaac.MarkingFormulaFactory.*;
import static uk.ac.cam.cl.dtg.isaac.Mark.*;

public class QuestionFactory {
    public static IsaacLLMFreeTextQuestion genericOneMarkQuestion() {
        return createLLMFreeTextQuestion(
                mark().setReasonFoo(1).setReasonBar(1).setReasonFizz(1),
                1,
                emptyExamples(),
                null);
    }

    public static IsaacLLMFreeTextQuestion genericTwoMarkQuestion() {
        return createLLMFreeTextQuestion(
                mark().setReasonFoo(1).setReasonBar(1).setReasonFizz(1),
                2,
                emptyExamples(),
                null);

    }

    public static IsaacLLMFreeTextQuestion advantageQuestion() {
        return createLLMFreeTextQuestion(
                advantageMark().setAdvantageOne(1).setAdvantageTwo(1).setDisadvantageOne(1).setDisadvantageTwo(1),
                2,
                emptyExamples(),
                advantageDisadvantageMarkingFormula);
    }

    public static IsaacLLMFreeTextQuestion pointExplanationQuestion() {
        return createLLMFreeTextQuestion(
                pointMark().setPointOne(1).setExplanationOne(1).setPointTwo(1).setExplanationTwo(1),
                2,
                emptyExamples(),
                pointExplanationMarkingFormula);
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

    private static List<LLMFreeTextMarkedExample> emptyExamples() {
        return new ArrayList<>();
    }
}
