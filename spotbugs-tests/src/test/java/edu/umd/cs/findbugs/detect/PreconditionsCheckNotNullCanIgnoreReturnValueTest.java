package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpotBugsExtension.class)
class PreconditionsCheckNotNullCanIgnoreReturnValueTest {

    @Test
    void testDoNotWarnOnCanIgnoreReturnValue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get(
                "../spotbugsTestCases/build/classes/java/main/bugPatterns/RV_RETURN_VALUE_IGNORED_Guava_Preconditions.class"));
        assertThat(bugCollection, is(emptyIterable()));
    }

}
