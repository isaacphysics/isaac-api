
package uk.ac.cam.cl.dtg.isaac;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import uk.ac.cam.cl.dtg.isaac.dos.IsaacLLMFreeTextQuestion;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkSchemeEntry;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMFreeTextMarkedExample;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;

public class QuestionFactory {
    public static IsaacLLMFreeTextQuestion genericOneMarkQuestion() {
        return createLLMFreeTextQuestion(
                generateMarkScheme(
                        field().setName("reasonFoo").setMarks(1),
                        field().setName("reasonBar").setMarks(1),
                        field().setName("reasonFizz").setMarks(1)),
                1,
                generateMarkedExamples(
                        example().setAnswer("Foo and Bar").setMarksAwarded(1).setMarks(
                                mark().setReasonFoo(1).setReasonBar(1)),
                        example().setAnswer("Fizz").setMarksAwarded(1).setMarks(
                                mark().setReasonFizz(1))),
                null);
    }

    public static IsaacLLMFreeTextQuestion genericTwoMarkQuestion() {
        return createLLMFreeTextQuestion(
                generateMarkScheme(
                        field().setName("reasonFoo").setMarks(1),
                        field().setName("reasonBar").setMarks(1),
                        field().setName("reasonFizz").setMarks(1)),
                2,
                generateMarkedExamples(
                        example().setAnswer("Foo and Bar").setMarksAwarded(2).setMarks(
                                mark().setReasonFoo(1).setReasonBar(1)),
                        example().setAnswer("Fizz").setMarksAwarded(1).setMarks(
                                mark().setReasonFizz(1))),
                null);

    }

    public static IsaacLLMFreeTextQuestion advantageQuestion() {
        return createLLMFreeTextQuestion(
                generateMarkScheme(
                        field().setName("advantageOne").setMarks(1),
                        field().setName("advantageTwo").setMarks(1),
                        field().setName("disadvantageOne").setMarks(1),
                        field().setName("disadvantageTwo").setMarks(1)),
                2,
                generateMarkedExamples(
                        advantageExample().setAnswer("Advantage").setMarksAwarded(1).setMarks(
                                advantageMark().setAdvantageOne(1)),
                        advantageExample().setAnswer("Disadvantage Disadvantage").setMarksAwarded(1).setMarks(
                                advantageMark().setAdvantageOne(1).setDisadvantageOne(1).setDisadvantageTwo(1))),
                advantageDisadvantageMarkingFormula);
    }

    public static IsaacLLMFreeTextQuestion pointExplanationQuestion() {
        return createLLMFreeTextQuestion(
                generateMarkScheme(
                        field().setName("pointOne").setMarks(1),
                        field().setName("pointTwo").setMarks(1),
                        field().setName("explanationOne").setMarks(1),
                        field().setName("explanationTwo").setMarks(1)),
                2,
                generateMarkedExamples(
                        pointExample().setAnswer("Explanation").setMarksAwarded(0).setMarks(
                                pointMark().setExplanationOne(1)),
                        pointExample().setAnswer("Point2 Explanation 2").setMarksAwarded(2).setMarks(
                                pointMark().setPointTwo(1).setExplanationTwo(1))),
                pointExplanationMarkingFormula);
    }

    
    public static GenericMark mark() {
        return new GenericMark();
    }

    public static AdvantageMark advantageMark() {
        return new AdvantageMark();
    }

    public static PointMark pointMark() {
        return new PointMark();
    }

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * generates a list of marks and their values from json input.
     *
     * @param entries - a JSON array containing marks in the format {"jsonField":
     *                markName, "marks": markValue}
     * @return A list of marks with corresponding "jsonField" and "marks" fields
     */
    public static List<LLMFreeTextMarkSchemeEntry> generateMarkScheme(Field... entries) {
        return Stream.of(entries).map(input -> {
            var output = new LLMFreeTextMarkSchemeEntry();
            output.setJsonField(input.name());
            output.setMarks(input.marks());
            return output;
        })
                .collect(Collectors.toList());
    }

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * generates an IsaacLLMFreeTextQuestion with provided marking information.
     *
     * @param markScheme     - Each available mark for the question and its
     *                       corresponding value
     * @param maxMarks       - Maximum available number of marks for the question
     * @param markedExamples - Example marked answers to the question
     * @param markingFormula - Formula used to calculate the mark total.
     *                       If null, defaults to MIN(maxMarks, SUM(... all marks
     *                       ...))
     * @return The new IsaacLLMFreeTextQuestion
     */
    public static IsaacLLMFreeTextQuestion createLLMFreeTextQuestion(final List<LLMFreeTextMarkSchemeEntry> markScheme,
            final Integer maxMarks,
            final List<LLMFreeTextMarkedExample> markedExamples,
            final LLMMarkingExpression markingFormula) {
        IsaacLLMFreeTextQuestion question = new IsaacLLMFreeTextQuestion();
        question.setMarkScheme(markScheme);
        question.setMaxMarks(maxMarks);
        question.setMarkedExamples(markedExamples);
        question.setMarkingFormula(markingFormula);

        return question;
    }

    // --- Useful Example Marking Formulae ---

    /*
     * - advantageDisadvantage = SUM(MAX(... All Advantage Marks ...), MAX(... All
     * Disadvantage Marks ...))
     * Labelled here and in documentation as advantage/disadvantage, although this
     * structure can also be used
     * for any two mutually exclusive categories each required to get full marks
     */
    private static final LLMMarkingFunction advantageDisadvantageMarkingFormula = markingFormulaFunction("SUM",
            Arrays.asList(
                    markingFormulaFunction("MAX",
                            Arrays.asList(
                                    markingFormulaVariable("advantageOne"),
                                    markingFormulaVariable("advantageTwo"))),
                    markingFormulaFunction("MAX",
                            Arrays.asList(
                                    markingFormulaVariable("disadvantageOne"),
                                    markingFormulaVariable("disadvantageTwo")))));

    /*
     * - pointExplanation = SUM(MAX(pointOne, pointTwo, ... pointN),
     * MAX(MIN(pointOne, explanationOne), MIN(pointTwo, explanationTwo), ...
     * MIN(pointN, explanationN))
     * Used for questions where a point is a prerequisite for its matching
     * explanation
     */
    private static final LLMMarkingFunction pointExplanationMarkingFormula = markingFormulaFunction("SUM",
            Arrays.asList(
                    markingFormulaFunction("MAX",
                            Arrays.asList(
                                    markingFormulaVariable("pointOne"),
                                    markingFormulaVariable("pointTwo"))),
                    markingFormulaFunction("MAX",
                            Arrays.asList(
                                    markingFormulaFunction("MIN",
                                            Arrays.asList(
                                                    markingFormulaVariable("pointOne"),
                                                    markingFormulaVariable("explanationOne"))),
                                    markingFormulaFunction("MIN",
                                            Arrays.asList(
                                                    markingFormulaVariable("pointTwo"),
                                                    markingFormulaVariable("explanationTwo")))))));

    /**
     * Helper method for the isaacLLMFreeTextValidator tests,
     * generates a list of marked example answers (consisting of a written answer
     * and its marks) from json input.
     *
     * @param jsonMarkedExamples - a JSON array containing examples in the format:
     *                           {"answer": exampleAnswer, "marksAwarded:
     *                           exampleValue, "marks": {...exampleMarkScheme...}}
     * @return A list of marked examples with corresponding "answer",
     *         "marksAwarded", and "marks" fields
     */
    @SafeVarargs
    private static <T extends Mark> List<LLMFreeTextMarkedExample> generateMarkedExamples(Example<T>... examples) {
        return Stream.of(examples).map(input -> {
            var output = new LLMFreeTextMarkedExample();
            output.setAnswer(input.answer());
            output.setMarksAwarded(input.marksAwarded());
            output.setMarks(input.marks().toHashMap());
            return output;
        }).collect(Collectors.toList());
    }

    // --- LLMMarkingElement Syntax Sugar ---

    private static LLMMarkingFunction markingFormulaFunction(final String name, final List<LLMMarkingExpression> args) {
        LLMMarkingFunction function = new LLMMarkingFunction();
        if (Objects.equals(name, "SUM")) {
            function.setName(LLMMarkingFunction.FunctionName.SUM);
        } else if (Objects.equals(name, "MIN")) {
            function.setName(LLMMarkingFunction.FunctionName.MIN);
        } else if (Objects.equals(name, "MAX")) {
            function.setName(LLMMarkingFunction.FunctionName.MAX);
        }
        function.setArguments(args);
        return function;
    }

    private static LLMMarkingVariable markingFormulaVariable(final String name) {
        LLMMarkingVariable variable = new LLMMarkingVariable();
        variable.setName(name);
        return variable;
    }

    private static Example<GenericMark> example() {
        return Example.example();
    }

    private static Example<AdvantageMark> advantageExample() {
        return Example.example();
    }

    private static Example<PointMark> pointExample() {
        return Example.example();
    }

    private static Field field() {
        return Field.field();
    }
}

class Example<T extends Mark> {
    private String answer;
    private int marksAwarded;
    private T marks;

    public static <T extends Mark> Example<T> example() {
        return new Example<T>();
    }

    public Example<T> setAnswer(String answer) {
        this.answer = answer;
        return this;
    }

    public String answer() {
        return answer;
    }

    public Example<T> setMarksAwarded(int marksAwarded) {
        this.marksAwarded = marksAwarded;
        return this;
    }

    public int marksAwarded() {
        return marksAwarded;
    }

    public Example<T> setMarks(T marks) {
        this.marks = marks;
        return this;
    }

    public T marks() {
        return this.marks;
    }
}