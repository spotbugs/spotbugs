package ghIssues;

import java.util.function.Supplier;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/724">GitHub issue #724</a>
 */
public class Issue724 {

    // Compiled into every constructor, so its lambda is covered as long as one constructor is annotated
    public final Supplier<Integer> suppressedFieldInitializer = () -> {
        String x = null;
        return x.length();
    };

    // Compiled into every constructor, but the constructor suppressions only cover NP
    public final Supplier<Integer> unsuppressedFieldInitializer = () -> {
        String[] array = { "x" };
        return array.toString().length();
    };

    @SuppressFBWarnings(value = "NP", justification = "Known limitation: does not cover the lambda compiled into the static initializer")
    public static final Supplier<Integer> unsupportedSuppressedStaticFieldInitializer = () -> {
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

    @SuppressFBWarnings(value = "NP", justification = "Suppression on a constructor covers the lambdas of this overload only")
    public Issue724(int overload) {
        suppressedInConstructor = () -> {
            String x = null;
            return x.length();
        };
    }

    // Not annotated: the suppression on the other constructor does not cover this overload's lambda
    public Issue724(String overload) {
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

    @SuppressFBWarnings(value = "NP", justification = "Suppression on a method covers the lambdas of this overload only")
    public Supplier<Integer> suppressedOverload(int overload) {
        return () -> {
            String x = null;
            return x.length();
        };
    }

    // Not annotated: the suppression on the other overload does not cover this overload's lambda
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
