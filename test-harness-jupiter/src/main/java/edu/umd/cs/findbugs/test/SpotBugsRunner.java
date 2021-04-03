package edu.umd.cs.findbugs.test;

import edu.umd.cs.findbugs.BugCollection;
import java.nio.file.Path;
import javax.annotation.Nonnull;

/**
 * A class to execute integration test for SpotBugs. This is basically designed to help SpotBugs
 * plugin developer to test their product.
 *
 * @since 3.1
 */
public class SpotBugsRunner {
    @Nonnull
    private final AnalysisRunner runner = new AnalysisRunner();

    /**
     * Add an entry to aux classpath of SpotBugs analysis.
     *
     * @param path A path of the target class file or jar file. Non-null.
     * @return callee itself, so caller can chain another method in fluent interface.
     */
    // TODO let users specify "groupId:artifactId:packaging:version:classifier" like Grape in Groovy
    @Nonnull
    public SpotBugsRunner addAuxClasspathEntry(Path path) {
        if (runner == null) {
            throw new IllegalStateException(
                    "Please call this addAuxClasspathEntry() method in @Before method or test method");
        }
        runner.addAuxClasspathEntry(path);
        return this;
    }

    /**
     * Run SpotBugs under given condition, and return its result.
     *
     * @param paths Paths of target class files
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
