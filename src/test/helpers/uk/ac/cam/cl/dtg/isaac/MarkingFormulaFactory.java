package uk.ac.cam.cl.dtg.isaac;

import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingExpression;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingFunction;
import uk.ac.cam.cl.dtg.isaac.dos.content.LLMMarkingVariable;

import java.util.Arrays;
import java.util.Objects;

public class MarkingFormulaFactory {
    public static LLMMarkingFunction function(final String name, final LLMMarkingExpression... args) {
        LLMMarkingFunction function = new LLMMarkingFunction();
        if (Objects.equals(name, "SUM")) {
            function.setName(LLMMarkingFunction.FunctionName.SUM);
        } else if (Objects.equals(name, "MIN")) {
            function.setName(LLMMarkingFunction.FunctionName.MIN);
        } else if (Objects.equals(name, "MAX")) {
            function.setName(LLMMarkingFunction.FunctionName.MAX);
        }
        function.setArguments(Arrays.asList(args));
        return function;
    }

    public static LLMMarkingVariable variable(final String name) {
        LLMMarkingVariable variable = new LLMMarkingVariable();
        variable.setName(name);
        return variable;
    }
}
