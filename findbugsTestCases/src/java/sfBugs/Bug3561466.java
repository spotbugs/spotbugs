package sfBugs;

import java.util.List;

import javax.annotation.CheckForNull;

public class Bug3561466 {

    public interface Call<T1, T2> {

    }

    public static class ReflectedCall<T1, T2> implements Call<T1, T2> {

        public ReflectedCall(String string, Class[] classes, Metric<?, String> domainLabelMetric) {

        }

    }

    static class Metric<K, V> {

    }

    static class PrivateFunctions {

        public static Call<java.util.List<?>, java.util.List<java.lang.String>> calculateSymbolicValues(
                Metric<?, java.lang.String> domainLabelMetric) {
            ReflectedCall<List<?>, List<String>> result = new ReflectedCall<java.util.List<?>, java.util.List<java.lang.String>>(
                    "CalculateSymbolicValuesFunction",
                    new Class[] { Metric.class }, domainLabelMetric);
            return result;
        }

    }

    static class SymbolAxisModel {

        public void setSymbolicValues(List<String> symbolicValues) {

        }

    }

    static class CalculateSwingTask<T> {
        CalculateSwingTask(List<?> items, Call<List<?>, List<String>> m, EvaluationContextUtils e) {
        }
    }

    enum EvaluationContextUtils {
        EMPTY
    };

    void calculateSymbolicValues(final List<?> items, @CheckForNull final Metric<?, String> domainLabelMap,
            final SymbolAxisModel domainAxisModel) {
        Metric<?, String> labellingMetric = null;

        final CalculateSwingTask<List<String>> task = new CalculateSwingTask<List<String>>(items,
                PrivateFunctions.calculateSymbolicValues((Metric) labellingMetric), EvaluationContextUtils.EMPTY) {

            protected void succeeded(List<String> symbolicValues) {
                domainAxisModel.setSymbolicValues(symbolicValues);
            }
        };

    }

}
