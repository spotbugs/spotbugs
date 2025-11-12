package edu.umd.cs.findbugs;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.test.AnalysisRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for the {@link UnsafeDetector}. */
class UnsafeDetectorTest {

    /** Build prefix directories to search for classes */
    private static final List<Path> BUILD_CLASS_SEARCH_PREFIXES =
            List.of(
                    // Build path if running in Maven
                    Paths.get("spotbugsTestCases/target/classes"),
                    // Build path if running command line build
                    Paths.get("spotbugsTestCases/build/classes/java/main"));

    private static final String BUG_TYPE = "UNS_UNSAFE_CALL";

    private BugCollectionBugReporter bugReporter;

    /** Tests the bad case for {@code sun.misc.Unsafe}. */
    @Test
    void testBadUnsafeSun() {
        performAnalysis("BadUnsafeSun.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(10);
    }

    /** Tests the bad case for {@code sun.misc.Unsafe} with a fully qualified name. */
    @Test
    void testUnsafeSunQualified() {
        performAnalysis("BadUnsafeSunQualified.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    private void performAnalysis(@SlashedClassName final String... analyzeMe) {
        AnalysisRunner runner = new AnalysisRunner();
        // TODO : Unwire this once we move bug samples to a proper sourceset
        Path[] paths =
                Arrays.stream(analyzeMe)
                        .map(UnsafeDetectorTest::getFindbugsTestCasesFile)
                        .toArray(Path[]::new);
        bugReporter = runner.run(UserPreferences.createDefaultUserPreferences(), paths);
    }

    private static Path getFindbugsTestCases() {
        final Path p = Paths.get("..");
        assertTrue(Files.exists(p));
        assertTrue(Files.isDirectory(p));
        assertTrue(Files.isReadable(p));
        return p;
    }

    private static Path getFindbugsTestCasesFile(final String path) {
        final Path root = getFindbugsTestCases();
        final Path p =
                BUILD_CLASS_SEARCH_PREFIXES.stream()
                        .map(prefix -> root.resolve(prefix).resolve(path))
                        .filter(Files::exists)
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(path + " not found"));

        assertTrue(Files.isReadable(p), p + " is not readable");
        return p;
    }

    private final void assertNumOfCTBugs(int count) {
        final BugInstanceMatcher matcher =
                new BugInstanceMatcherBuilder().bugType(BUG_TYPE).build();
        assertThat(getBugCollection(), containsExactly(count, matcher));
    }

    private BugCollection getBugCollection() {
        return bugReporter.getBugCollection();
    }

    private final void assertCTBugInLine(int line) {
        final BugInstanceMatcher matcher =
                new BugInstanceMatcherBuilder().bugType(BUG_TYPE).atLine(line).build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
