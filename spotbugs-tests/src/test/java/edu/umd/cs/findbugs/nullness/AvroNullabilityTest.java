package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

/**
 * Check if Nullable annotation from org.apache.avro.reflect is detected.
 *
 * @author HoelzelJon
 */
public class AvroNullabilityTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void checkedAvroNullableReturn_isOk() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/CheckedAvroNullableReturn.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void uncheckedAvroNullableReturn_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/UncheckedAvroNullableReturn.class"));

        assertThat(bugCollection, containsExactly(1, bug()));
    }

    private BugInstanceMatcher bug() {
        return new BugInstanceMatcherBuilder().bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
    }
}
