package uk.ac.cam.cl.dtg.isaac;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MarkingFormulaFactory {
    public static final LLMMarkingFunction advantageDisadvantageMarkingFormula = markingFormulaFunction("SUM",
            Arrays.asList(
                    markingFormulaFunction("MAX",
                            Arrays.asList(
                                    markingFormulaVariable("advantageOne"),
                                    markingFormulaVariable("advantageTwo"))),
                    markingFormulaFunction("MAX",
                            Arrays.asList(
                                    markingFormulaVariable("disadvantageOne"),
                                    markingFormulaVariable("disadvantageTwo")))));

    public static final LLMMarkingFunction pointExplanationMarkingFormula = markingFormulaFunction("SUM",
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
}
