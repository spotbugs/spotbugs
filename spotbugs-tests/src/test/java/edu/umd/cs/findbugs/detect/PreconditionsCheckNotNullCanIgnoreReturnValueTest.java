package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

class PreconditionsCheckNotNullCanIgnoreReturnValueTest extends AbstractIntegrationTest {

    @Test
    void testDoNotWarnOnCanIgnoreReturnValue() {
        performAnalysis("bugPatterns/RV_RETURN_VALUE_IGNORED_Guava_Preconditions.class");
        assertThat(getBugCollection(), is(emptyIterable()));
    }

}
