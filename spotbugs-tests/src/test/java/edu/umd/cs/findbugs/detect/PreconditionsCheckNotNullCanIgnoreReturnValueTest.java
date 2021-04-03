package edu.umd.cs.findbugs.detect;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;

public class PreconditionsCheckNotNullCanIgnoreReturnValueTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void testDoNotWarnOnCanIgnoreReturnValue() {
        BugCollection bugCollection =
                spotbugs.performAnalysis(
                        Paths.get(
                                "../spotbugsTestCases/build/classes/java/main/bugPatterns/RV_RETURN_VALUE_IGNORED_Guava_Preconditions.class"));
        assertThat(bugCollection, is(emptyIterable()));
    }
}
