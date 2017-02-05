package edu.umd.cs.findbugs.test;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.config.UserPreferences;

/**
 * <p>
 * This class runs analysis with SpotBugs. The target class files and
 * auxClasspathEntries should be specified before you invoke {@link #run(Path...)}
 * method.
 * </p>
 * 
 * @since 3.1
 */
@ParametersAreNonnullByDefault
public class AnalysisRunner {
    private final List<Path> auxClasspathEntries = new ArrayList<>();

    @Nonnull
    public AnalysisRunner addAuxClasspathEntry(Path path) {
        Objects.requireNonNull(path);
        if (!path.toFile().canRead()) {
            throw new IllegalArgumentException("Cannot read " + path.toAbsolutePath());
        }
        auxClasspathEntries.add(path);
        return this;
    }

    @Nonnull
    public BugCollectionBugReporter run(Path... files) {
        DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection());

        FindBugs2 engine = new FindBugs2();
        final Project project = new Project();
        project.setProjectName(getClass().getSimpleName());
        engine.setProject(project);

        final DetectorFactoryCollection detectorFactoryCollection = DetectorFactoryCollection.instance();
        engine.setDetectorFactoryCollection(detectorFactoryCollection);

        BugCollectionBugReporter bugReporter = new BugCollectionBugReporter(project);
        bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
        bugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX);

        engine.setBugReporter(bugReporter);
        final UserPreferences preferences = UserPreferences.createDefaultUserPreferences();
        preferences.getFilterSettings().clearAllCategories();
        engine.setUserPreferences(preferences);

        for (Path file : files) {
            project.addFile(file.toAbsolutePath().toString());
        }
        for (Path auxClasspathEntry : auxClasspathEntries) {
            project.addAuxClasspathEntry(auxClasspathEntry.toAbsolutePath().toString());
        }

        try {
            engine.execute();
        } catch (final IOException | InterruptedException e) {
            fail("Analysis failed with exception; " + e.getMessage());
        }
        if (!bugReporter.getQueuedErrors().isEmpty()) {
            AssertionError assertionError = new AssertionError(
                    "Analysis failed with exception. Check stderr for detail.");
            bugReporter.getQueuedErrors().stream().map(error -> error.getCause())
                    .forEach(assertionError::addSuppressed);
            throw assertionError;
        }
        return bugReporter;
    }
}
