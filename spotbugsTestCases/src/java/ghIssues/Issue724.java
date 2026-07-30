package ghIssues;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/724">GitHub issue #724</a>
 */
public class Issue724 {

    // Compiled into the annotated constructor below, so its lambda is covered by the constructor suppression
    public final Supplier<Integer> suppressedFieldInitializer = () -> {
        String x = null;
        return x.length();
    };

    public final Supplier<Integer> suppressedInConstructor;

    @SuppressFBWarnings(value = "NP", justification = "Suppression on a constructor covers its lambdas")
    public Issue724() {
        suppressedInConstructor = () -> {
            String x = null;
            return x.length();
        };
    }

    @SuppressFBWarnings(value = "NP", justification = "Suppression on a method covers its lambdas")
    public Supplier<Integer> suppressedInLambda() {
        return () -> {
            String x = null;
            return x.length();
        };
    }

    @SuppressFBWarnings(value = "NP", justification = "Suppression on a method covers its nested lambdas")
    public Supplier<Integer> suppressedInNestedLambda() {
        return () -> {
            Supplier<Integer> inner = () -> {
                String x = null;
                return x.length();
            };
            return inner.get();
        };
    }

    @SuppressFBWarnings(value = "NP", justification = "Suppression on a method covers its lambdas")
    public Supplier<Integer> suppressedOverload(int overload) {
        return () -> overload;
    }

    // Known limitation: lambda method names do not encode signature, so the suppression above also covers the lambda of this overload
    public Supplier<Integer> suppressedOverload(String overload) {
        return () -> {
            String x = null;
            return x.length();
        };
    }

    public Supplier<Integer> notSuppressedInLambda() {
        return () -> {
            String x = null;
            return x.length();
        };
    }
}
