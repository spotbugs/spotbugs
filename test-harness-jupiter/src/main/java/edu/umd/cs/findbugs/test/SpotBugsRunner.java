package edu.umd.cs.findbugs.test;

import java.nio.file.Path;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.BugCollection;

/**
 * <p>
 * A class to execute integration test for SpotBugs. This is basically designed to help SpotBugs plugin developer to
 * test their product.
 * </p>
 *
 * @since 3.1
 */
public class SpotBugsRunner {
    @Nonnull
    private final AnalysisRunner runner = new AnalysisRunner();

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
    public SpotBugsRunner addAuxClasspathEntry(Path path) {
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
