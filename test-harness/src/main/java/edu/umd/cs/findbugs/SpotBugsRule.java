package edu.umd.cs.findbugs;

import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.junit.rules.ExternalResource;

import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * <p>
 * A JUnit rule to execute integration test for SpotBugs. This is basically
 * designed to help SpotBugs plugin developer to test their product. Basic usage:
 * </p>
 * <pre><code>
 * public class MyIntegrationTest {
 *     &#064;Rule
 *     public SpotBugsRule spotbugs = new SpotBugsRule();
 *     &#064;Test
 *     public void testIssuesAreFound() {
 *         Path path = Paths.get("target/classes/my/company/AnalyzedClass.class")
 *         BugCollection bugCollection = spotbugs.performAnalysis(path);
 *
 *         // There should only be exactly 1 issue of this type
 *         final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
 *                 .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
 *         assertThat(bugCollection, containsExactly(bugTypeMatcher, 1));
 *
 *         final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
 *                 .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
 *                 .inClass("AnalyzedClass")
 *                 .atLine(25)
 *                 .build();
 *         assertThat(bugCollection, hasItem(bugInstanceMatcher));
 *     }
 * }
 * </code></pre>
 * @since 3.1
 */
@ParametersAreNonnullByDefault
public class SpotBugsRule extends ExternalResource {
    private AnalysisRunner runner;

    @Override
    protected void before() throws Throwable {
        runner = new AnalysisRunner();
    }

    @Override
    protected void after() {
        runner = null;
    }

    /**
     * <p>
     * A factory method to build a {@link Matcher} instance to check how many
     * {@link BugInstance} matches in {@link BugCollection}. Basic usage:
     * </p>
     * <pre>
     * <code>
     * BugCollection bugCollection = spotBugsRule.performAnalysis(...);
     * assertThat(
     *     bugCollection,
     *     containsExactly(bugInstanceMatcher, 1));
     * </code>
     * </pre>
     * @param matcher
     *            A matcher to match {@link BugInstance}. non-null.
     * @param count
     *            How many times you expect that specified matcher matches.
     * @return A matcher which matches specified times
     * @see BugInstanceMatcher
     * @see BugInstanceMatcherBuilder
     */
    public static <T> Matcher<Iterable<T>> containsExactly(final Matcher<T> matcher, final int count) {
        return new CountMatcher<T>(count, matcher);
    }

    /**
     * <p>
     * Add an entry to aux classpath of SpotBugs analysis.
     * </p>
     * @param path
     *      A path of the target class file or jar file. Non-null.
     * @return callee itself, so caller can chain another method in fluent interface.
     */
    // TODO let users specify "groupId:artifactId:packaging:version:classifier" like Grape in Groovy
    @Nonnull
    public SpotBugsRule addAuxClasspathEntry(Path path) {
        if (runner == null) {
            throw new IllegalStateException(
                    "Please call this addAuxClasspathEntry() method in @Before method or test method");
        }
        runner.addAuxClasspathEntry(path);
        return this;
    }

    /**
     * <p>
     * Run SpotBugs under given condition, and return its result.
     * </p>
     * @param paths
     *     Paths of target class files
     * @return a {@link BugCollection} which contains all detected bugs.
     */
    // TODO let users specify SlashedClassName, then find its file path automatically
    @Nonnull
    public BugCollection performAnalysis(Path... paths) {
        if (runner == null) {
            throw new IllegalStateException("Please call this performAnalysis() method in test method");
        }
        return runner.run(paths).getBugCollection();
    }
}
