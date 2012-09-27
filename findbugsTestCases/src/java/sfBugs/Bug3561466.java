package sfBugs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

public class Bug3561466 {
    static class PrivateFunctions {
        static Map barChartCalculateSymbolicValues(Map m) {
            return m;
        }
    }

    static class SymbolAxisModel {

    }

    static class CalculateSwingTask<T> {
        CalculateSwingTask(List<?> items, Map m, EvaluationContextUtils e) {
        }
    }

    enum EvaluationContextUtils {
        EMPTY
    };

    private void calculateSymbolicValues(final List<?> items, 
            @CheckForNull final Map<?, String> domainLabelMap,
            final SymbolAxisModel domainAxisModel) {
        Map<?, String> labellingMetric = null;

        Map barChartCalculateSymbolicValues = PrivateFunctions.barChartCalculateSymbolicValues((Map) labellingMetric);
        final CalculateSwingTask<List<String>> task 
            = new CalculateSwingTask<List<String>>(items,
                barChartCalculateSymbolicValues,
                EvaluationContextUtils.EMPTY) {

            protected void succeeded(List<String> symbolicValues) {
                //
            }
        };

    }

}
