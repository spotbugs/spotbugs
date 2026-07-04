/*
 * SpotBugs - Find bugs in Java programs
 * Copyright (C) 2024, SpotBugs authors
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DetectorFactoryCollection}.
 */
class DetectorFactoryCollectionTest {

    private static final String JAWS_DEBUG_PROP = "findbugs.jaws.debug";
    private static final String TEST_PLUGIN_ID = "com.example.cli-regression-test-plugin";

    private final String previousJawsDebugProperty = System.getProperty(JAWS_DEBUG_PROP);

    @AfterEach
    void clearJawsDebugProperty() {
        SystemProperties.removeProperty(JAWS_DEBUG_PROP);
        if (previousJawsDebugProperty == null) {
            System.clearProperty(JAWS_DEBUG_PROP);
        } else {
            System.setProperty(JAWS_DEBUG_PROP, previousJawsDebugProperty);
        }
    }

    /**
     * Calling {@code jawsDebugMessage} when {@code findbugs.jaws.debug} is {@code false}
     * must not throw.
     */
    @Test
    void jawsDebugMessageDoesNotThrowWhenDebugIsDisabled() {
        SystemProperties.setProperty(JAWS_DEBUG_PROP, "false");
        assertDoesNotThrow(() -> DetectorFactoryCollection.jawsDebugMessage("test message"));
    }

    /**
     * Verify that instantiating {@link DetectorFactoryCollection} with a single
     * {@link Plugin} completes without error.
     */
    @Test
    void instanceCreationForSinglePluginDoesNotThrow() {
        Plugin corePlugin = PluginLoader.getCorePluginLoader().getPlugin();
        assertNotNull(corePlugin);
        DetectorFactoryCollection dfc =
                assertDoesNotThrow(() -> new DetectorFactoryCollection(corePlugin));
        assertNotNull(dfc);
    }

    @Test
    void defaultConstructorLoadsInitialPluginsInFreshJvm() throws Exception {
        Path spotBugsHome = Files.createTempDirectory("spotbugs-home");
        Path pluginJar = spotBugsHome.resolve("plugin").resolve("cli-regression-test-plugin.jar");
        Files.createDirectories(pluginJar.getParent());
        createTestPluginJar(pluginJar);

        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(
                javaBin,
                "-Dfindbugs.home=" + spotBugsHome,
                "-cp",
                System.getProperty("java.class.path"),
                CliBootstrapVerifier.class.getName())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();
        assertEquals(0, exitCode, output);
    }

    private static void createTestPluginJar(Path pluginJar) throws IOException {
        try (JarOutputStream jarOutputStream = new JarOutputStream(Files.newOutputStream(pluginJar))) {
            writeJarEntry(jarOutputStream, "findbugs.xml", "<FindbugsPlugin pluginid=\"" + TEST_PLUGIN_ID + "\"/>");
            writeJarEntry(jarOutputStream, "messages.xml",
                    "<MessageCollection><Plugin><ShortDescription>CLI regression plugin</ShortDescription></Plugin></MessageCollection>");
        }
    }

    private static void writeJarEntry(JarOutputStream jarOutputStream, String entryName, String content) throws IOException {
        jarOutputStream.putNextEntry(new JarEntry(entryName));
        jarOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        jarOutputStream.closeEntry();
    }

    public static final class CliBootstrapVerifier {
        public static void main(String[] args) {
            DetectorFactoryCollection.resetInstance(null);
            DetectorFactoryCollection detectorFactoryCollection = DetectorFactoryCollection.instance();
            if (detectorFactoryCollection.getPluginById(TEST_PLUGIN_ID) == null) {
                System.err.println("Expected initial plugin to be loaded into DetectorFactoryCollection: " + TEST_PLUGIN_ID);
                System.exit(1);
            }
        }
    }
}
