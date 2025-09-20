package edu.umd.cs.findbugs.test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import edu.umd.cs.findbugs.BugCollectionBugReporter;
import edu.umd.cs.findbugs.BugRanker;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.FindBugs2;
import edu.umd.cs.findbugs.IFindBugsEngine;
import edu.umd.cs.findbugs.Plugin;
import edu.umd.cs.findbugs.PluginException;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.annotations.CheckReturnValue;
import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.plugins.DuplicatePluginIdException;

/**
 * <p>
 * This class runs analysis with SpotBugs. The target class files and
 * auxClasspathEntries should be specified before you invoke {@link #run(Consumer, Path...)}
 * method.
 * </p>
 *
 * @since 3.1
 */
@ParametersAreNonnullByDefault
public class AnalysisRunner {
    private final List<Path> auxClasspathEntries = new ArrayList<>();

    /**
     * SpotBugs stores relation between plugin-id and {@link Plugin} instance in a static field ({@code Plugin.allPlugins}),
     * so we need to store Plugin information in static field too, to avoid duplicated plugin loading.
     */
    @Nullable
    private static final File PLUGIN_JAR;

    static {
        File jarFile;
        try {
            jarFile = createTempJar();
            Plugin.loadCustomPlugin(jarFile, null);
        } catch (DuplicatePluginIdException ignore) {
            // loading core plugin
            jarFile = null;
        } catch (IOException | URISyntaxException | PluginException e) {
            throw new AssertionError(e);
        }

        PLUGIN_JAR = jarFile;
    }

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
        return this.run(engine -> {
        }, files);
    }

    @Nonnull
    public BugCollectionBugReporter run(Consumer<IFindBugsEngine> engineCustomization, Path... files) {
        DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection());

        try (FindBugs2 engine = new FindBugs2(); Project project = createProject(files)) {
            engine.setProject(project);

            final DetectorFactoryCollection detectorFactoryCollection = DetectorFactoryCollection.instance();
            engine.setDetectorFactoryCollection(detectorFactoryCollection);

            BugCollectionBugReporter bugReporter = new BugCollectionBugReporter(project);
            bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
            bugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX);

            engine.setBugReporter(bugReporter);
            final UserPreferences preferences = UserPreferences.createDefaultUserPreferences();
            preferences.getFilterSettings().clearAllCategories();
            preferences.enableAllDetectors(true);
            engine.setUserPreferences(preferences);

            engineCustomization.accept(engine);
            try {
                engine.execute();
            } catch (final IOException e) {
                throw new AssertionError("Analysis failed with exception", e);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Analysis failed with exception", e);
            } finally {
                FindBugs2.resetPriorityAdjustments();
            }
            if (!bugReporter.getQueuedErrors().isEmpty()) {
                bugReporter.reportQueuedErrors();
                throw new AssertionError(
                        "Analysis failed with exception. Check stderr for detail.");
            }
            return bugReporter;
        }
    }

    @CheckReturnValue
    private Project createProject(Path[] files) {
        final Project project = new Project();
        project.setProjectName(getClass().getSimpleName());
        if (PLUGIN_JAR != null) {
            try {
                String pluginId = Plugin.addCustomPlugin(PLUGIN_JAR.toURI()).getPluginId();
                project.setPluginStatusTrinary(pluginId, Boolean.TRUE);
            } catch (PluginException e) {
                throw new AssertionError("Failed to load plugin", e);
            }
        }

        for (Path file : files) {
            project.addFile(file.toAbsolutePath().toString());
        }
        for (Path auxClasspathEntry : auxClasspathEntries) {
            project.addAuxClasspathEntry(auxClasspathEntry.toAbsolutePath().toString());
        }
        return project;
    }

    /**
     * Create a jar file which contains all resource files. This is necessary to
     * let {@link Plugin#loadCustomPlugin(File, Project)} load custom plugin to
     * test.
     *
     * @return a {@link File} instance which represent generated jar file
     * @throws IOException
     * @throws URISyntaxException
     */
    private static File createTempJar() throws IOException, URISyntaxException {
        ClassLoader cl = AnalysisRunner.class.getClassLoader();

        URL resource = cl.getResource("findbugs.xml");
        URI uri = resource.toURI();

        if ("jar".equals(uri.getScheme())) {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            URL url = connection.getJarFileURL();
            return new File(url.toURI());
        }

        Path tempJar = File.createTempFile("SpotBugsAnalysisRunner", ".jar").toPath();
        try (OutputStream output = Files.newOutputStream(tempJar, StandardOpenOption.WRITE);
                JarOutputStream jar = new JarOutputStream(output)) {
            Path resourceRoot = Path.of(uri).getParent();

            byte[] data = new byte[4 * 1024];
            Files.walkFileTree(resourceRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String name = resourceRoot.relativize(file).toString();
                    jar.putNextEntry(new ZipEntry(name));
                    try (InputStream input = Files.newInputStream(file, StandardOpenOption.READ)) {
                        int len;
                        while ((len = input.read(data)) > 0) {
                            jar.write(data, 0, len);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        return tempJar.toFile();
    }
}
