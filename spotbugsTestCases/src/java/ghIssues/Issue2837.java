package ghIssues;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Issue2837 {
    public boolean overridable1(String s) {
        return true;
    }

    public boolean overridable2(String s) {
        return true;
    }

    // overridable method referenced but not used
    public final Predicate<String> predicate1 = this::overridable1;

    // overridable method used
    public final long matchesCount = Stream.of("Test").filter(this::overridable2).count();

    public final Predicate<String> predicate2 = Predicate.not(this::overridable1);

    // Reusing the method reference must not hide the earlier call in matchesCount.
    public final Predicate<String> predicate3 = Predicate.not(this::overridable2);

    public static class InvokedNegation {
        public boolean overridable(String s) {
            return true;
        }

        public final boolean matches = Predicate.not(this::overridable).test("Test");
    }

    public static class EagerFactory {
        public boolean overridable(String s) {
            return true;
        }

        private static Predicate<String> not(Predicate<String> predicate) {
            predicate.test("Test");
            return predicate;
        }

        public final Predicate<String> predicate = not(this::overridable);
    }

    public static class OverriddenNegation {
        interface EagerPredicate extends Predicate<String> {
            @Override
            default Predicate<String> negate() {
                test("Test");
                return this;
            }
        }

        public boolean overridable(String s) {
            return true;
        }

        public final Predicate<String> predicate = Predicate.not((EagerPredicate) this::overridable);
    }

    public static class DeferredCombinators {
        interface InheritedPredicate extends Predicate<String> {
        }

        public final Predicate<String> negated = Predicate.not((InheritedPredicate) this::test);
        public final Predicate<String> chained = ((Predicate<String>) this::test).and(this::test).or(this::test).negate();
        public final Consumer<String> consumer = ((Consumer<String>) this::consume).andThen(value -> { });
        public final Function<String, String> function = Function.<String>identity().andThen(this::map);
        public final Supplier<String> supplier = this::get;
        public final Function<String, String> localFunction;

        public DeferredCombinators() {
            Function<String, String> local = this::map;
            localFunction = local.compose(Function.identity());
        }

        public boolean test(String value) {
            return true;
        }

        public void consume(String value) {
        }

        public String map(String value) {
            return value;
        }

        public String get() {
            return "value";
        }
    }

    public static class StoredThenInvoked {
        public final Predicate<String> predicate = Predicate.not(this::test);
        public final Consumer<String> consumer = ((Consumer<String>) this::consume).andThen(value -> { });
        public final Function<String, String> function = Function.<String>identity().andThen(this::map);
        public final Supplier<String> supplier = this::get;

        public StoredThenInvoked() {
            predicate.test("value");
            consumer.accept("value");
            function.apply("value");
            supplier.get();
        }

        public boolean test(String value) {
            return true;
        }

        public void consume(String value) {
        }

        public String map(String value) {
            return value;
        }

        public String get() {
            return "value";
        }
    }

    public static class UnknownReceiver {
        public final Consumer<String> consumer;

        public UnknownReceiver(Consumer<String> supplied) {
            consumer = supplied.andThen(this::consume);
        }

        public void consume(String value) {
        }
    }
}
