package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.junit.Assume.assumeThat;

public class PreconditionsCheckNotNullNullableTest {
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
    public void testNullableIsSanitizedByCheckNonNull() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/bugPatterns/NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE_Guava_Preconditions.class"));
        Assert.assertThat(bugCollection, is(emptyIterable()));
    }

}
