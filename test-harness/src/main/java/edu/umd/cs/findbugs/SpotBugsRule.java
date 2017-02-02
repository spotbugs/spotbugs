package edu.umd.cs.findbugs;

import java.nio.file.Path;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.hamcrest.Matcher;
import org.junit.rules.ExternalResource;

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * <p>
 * A JUnit rule to execute integration test for SpotBugs. This is basically
 * designed to help SpotBugs plugin developer to test their product.
 * </p>
 *
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

    public static <T> Matcher<Iterable<T>> containsExactly(final Matcher<T> matcher, final int count) {
        return new CountMatcher<T>(count, matcher);
    }

    @Nonnull
    public SpotBugsRule addFile(Path path) {
        if (runner == null) {
            throw new IllegalStateException("Please call this addFile() method in @Before method or test method");
        }
        runner.addFile(path);
        return this;
    }

    @Nonnull
    public SpotBugsRule addAuxClasspathEntry(Path path) {
        if (runner == null) {
            throw new IllegalStateException("Please call this addAuxClasspathEntry() method in @Before method or test method");
        }
        runner.addAuxClasspathEntry(path);
        return this;
    }

    @Nonnull
    public BugCollectionBugReporter performAnalysis(@SlashedClassName final String... analyzeMe) {
        if (runner == null) {
            throw new IllegalStateException("Please call this performAnalysis() method in test method");
        }
        return runner.run();
    }
}
