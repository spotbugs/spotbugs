package ghIssues;

import java.util.stream.Collector;
import java.util.stream.Stream;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/4055">#4055</a>.
 */
public class Issue4055 {
    public static <IN extends String> Collector<IN, ?, String> concatenating() {
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

    public static void useConcatenating() {
        Stream.of("a", "b", "c").collect(concatenating());
    }
}
