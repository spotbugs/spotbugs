package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

public class PreconditionsCheckNotNullCanIgnoreReturnValueTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Before
    public void setUp() {
        String property = System.getProperty("AUX_CLASSPATH");
        assumeThat(property, notNullValue());
        for (String path : property.split(",")) {
            spotbugs.addAuxClasspathEntry(Paths.get(path));
        }
    }

    @Test
    public void testDoNotWarnOnCanIgnoreReturnValue() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get(
                "../spotbugsTestCases/build/classes/java/main/bugPatterns/RV_RETURN_VALUE_IGNORED_Guava_Preconditions.class"));
        assertThat(bugCollection, is(emptyIterable()));
    }

}
