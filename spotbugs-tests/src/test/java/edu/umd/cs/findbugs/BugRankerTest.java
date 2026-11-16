package edu.umd.cs.findbugs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import edu.umd.cs.findbugs.detect.TestingGround;

class BugRankerTest {

    /**
     * The rank of a bug pattern of the SECURITY category which has no rank of its own, see the {@code bugrank.txt} of
     * the core plugin.
     */
    private static final int SECURITY_RANK = 5;

    @TempDir
    Path tempDir;

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1161">GitHub issue</a>
     */
    @Test
    void testTrimToMaxRank() {
        SortedBugCollection bugCollection = new SortedBugCollection();
        bugCollection.add(new BugInstance("type", Priorities.HIGH_PRIORITY).addClass("the/target/Class"));
        BugRanker.trimToMaxRank(bugCollection, 0);

        assertTrue(bugCollection.getCollection().isEmpty());
    }

    /**
     * A plugin which does not ship a {@code bugrank.txt} has no ranks of its own, so its bugs must be ranked exactly
     * like the core plugin would rank them. The ranks of the core plugin used to be applied twice.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs-maven-plugin/issues/144">GitHub issue</a>
     */
    @Test
    void testPluginWithoutBugRankFile() throws Exception {
        Plugin plugin = loadTestPlugin("without-bugrank", "BUG_RANKER_TEST_WITHOUT_BUGRANK", null);
        try {
            assertEquals(SECURITY_RANK, rank(plugin));
        } finally {
            unloadTestPlugin(plugin);
        }
    }

    /**
     * The {@code bugrank.txt} of a plugin is still picked up: its ranks are added to the ones which the core plugin
     * gives to the bug patterns of that plugin.
     */
    @Test
    void testPluginWithBugRankFile() throws Exception {
        String bugType = "BUG_RANKER_TEST_WITH_BUGRANK";
        Plugin plugin = loadTestPlugin("with-bugrank", bugType, "+3 BugPattern " + bugType + "\n");
        try {
            assertEquals(SECURITY_RANK + 3, rank(plugin));
        } finally {
            unloadTestPlugin(plugin);
        }
    }

    /**
     * @return the rank of the only bug pattern of the plugin, when it is reported by a detector of that plugin
     */
    private static int rank(Plugin plugin) {
        BugPattern bugPattern = plugin.getBugPatterns().iterator().next();
        DetectorFactory detectorFactory = new DetectorFactory(plugin, TestingGround.class.getName(), TestingGround.class, true,
                "fast", bugPattern.getType(), "");
        return BugRanker.findRank(bugPattern, detectorFactory);
    }

    /**
     * Loads a plugin with a single bug pattern of the SECURITY category.
     *
     * @param bugRank
     *            the content of the {@code bugrank.txt} of the plugin, or null if it has none
     */
    private Plugin loadTestPlugin(String name, String bugType, String bugRank) throws IOException, PluginException {
        Path pluginJar = tempDir.resolve(name + ".jar");
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            writeJarEntry(jarOutputStream, "findbugs.xml",
                    "<FindbugsPlugin pluginid=\"com.example.bug-ranker-test." + name + "\">"
                            + "<BugPattern type=\"" + bugType + "\" abbrev=\"BRT\" category=\"SECURITY\"/>"
                            + "</FindbugsPlugin>");
            writeJarEntry(jarOutputStream, "messages.xml",
                    "<MessageCollection>"
                            + "<Plugin><ShortDescription>Bug ranker test plugin</ShortDescription></Plugin>"
                            + "<BugPattern type=\"" + bugType + "\">"
                            + "<ShortDescription>Test bug</ShortDescription>"
                            + "<LongDescription>Test bug</LongDescription>"
                            + "<Details>Test bug</Details>"
                            + "</BugPattern>"
                            + "<BugCode abbrev=\"BRT\">Bug ranker test</BugCode>"
                            + "</MessageCollection>");
            if (bugRank != null) {
                writeJarEntry(jarOutputStream, BugRanker.FILENAME, bugRank);
            }
        }
        // Only the bug ranker of the plugin is needed, so the plugin is not added to the DetectorFactoryCollection
        return PluginLoader.getPluginLoader(pluginJar.toUri().toURL(), PluginLoader.class.getClassLoader(), false, true)
                .loadPlugin();
    }

    private static void unloadTestPlugin(Plugin plugin) throws IOException {
        Plugin.removePlugin(plugin.getPluginLoader().getURI());
        PluginLoader.loadedPluginIds.remove(plugin.getPluginId());
        // release the jar file, otherwise the temporary directory can not be deleted on Windows
        plugin.close();
    }

    private static void writeJarEntry(JarOutputStream jarOutputStream, String entryName, String content) throws IOException {
        jarOutputStream.putNextEntry(new JarEntry(entryName));
        jarOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        jarOutputStream.closeEntry();
    }
}
