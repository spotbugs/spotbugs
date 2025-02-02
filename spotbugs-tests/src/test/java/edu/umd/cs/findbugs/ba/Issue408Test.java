package edu.umd.cs.findbugs.ba;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/408">#408</a>
 * @since 3.1
 */
class Issue408Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testSingleClass() {
        Throwable throwable = Assertions.assertThrows(AssertionError.class, () -> performAnalysis("../java11/module-info.class"));
        Assertions.assertEquals("Analysis failed with exception", throwable.getMessage());
        assertThat(throwable.getCause(), is(instanceOf(IOException.class)));
    }

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testFewClasses() {
        Assertions.assertDoesNotThrow(() -> performAnalysis("../java11/module-info.class", "../java11/Issue408.class"));
    }

}
