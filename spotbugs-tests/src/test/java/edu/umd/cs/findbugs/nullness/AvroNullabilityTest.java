package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

/**
 * Check if Nullable annotation from org.apache.avro.reflect is detected.
 *
 * @author HoelzelJon
 */
@ExtendWith(SpotBugsExtension.class)
class AvroNullabilityTest {

    @Test
    void checkedAvroNullableReturn_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Path.of("../spotbugsTestCases/build/classes/java/main/CheckedAvroNullableReturn.class"),
                Path.of("../spotbugsTestCases/build/classes/java/main/org/apache/avro/reflect/Nullable.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    void uncheckedAvroNullableReturn_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Path.of("../spotbugsTestCases/build/classes/java/main/UncheckedAvroNullableReturn.class"),
                Path.of("../spotbugsTestCases/build/classes/java/main/org/apache/avro/reflect/Nullable.class"));

        assertThat(bugCollection, containsExactly(1, bug()));
    }

    private BugInstanceMatcher bug() {
        return new BugInstanceMatcherBuilder().bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
    }
}
