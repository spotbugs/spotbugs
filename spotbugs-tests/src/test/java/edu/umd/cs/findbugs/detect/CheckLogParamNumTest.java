package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class CheckLogParamNumTest extends AbstractIntegrationTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        String property = System.getProperty("AUX_CLASSPATH");
        assumeThat(property, notNullValue());
        for (String path : property.split(",")) {
            spotbugs.addAuxClasspathEntry(Paths.get(path));
        }

        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/WrongPlaceholderOfSlf4j.class"));
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder().bugType("FS_LOG_PARAM_NUM").build();
        assertThat(bugCollection, containsExactly(8, matcher));
    }

}
