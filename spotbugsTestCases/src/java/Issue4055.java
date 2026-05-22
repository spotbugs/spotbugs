import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * Methods of a local class that are only reached through method references
 * (compiled to {@code invokedynamic}) must not be reported as UMAC.
 */
public class Issue4055 {

    public static <IN extends String> Collector<IN, ?, String> concatenatingGeneric() {
        class Accumulator {
            final StringBuilder sb = new StringBuilder();

            void accumulate(IN s) {
                sb.append(s);
            }

            Accumulator combine(Accumulator other) {
                sb.append(other.sb);
                return this;
            }

            String finish() {
                return sb.toString();
            }
        }
        return Collector.of(Accumulator::new, Accumulator::accumulate, Accumulator::combine, Accumulator::finish);
    }

    public static Collector<String, ?, String> concatenatingPlain() {
        class Accumulator {
            final StringBuilder sb = new StringBuilder();

            void accumulate(String s) {
                sb.append(s);
            }

            Accumulator combine(Accumulator other) {
                sb.append(other.sb);
                return this;
            }

            String finish() {
                return sb.toString();
            }
        }
        return Collector.of(Accumulator::new, Accumulator::accumulate, Accumulator::combine, Accumulator::finish);
    }

    public static String useGeneric() {
        return Stream.of("a", "b", "c").collect(concatenatingGeneric());
    }

    public static String usePlain() {
        return Stream.of("a", "b", "c").collect(concatenatingPlain());
    }
}
