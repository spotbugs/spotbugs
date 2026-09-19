/*
 * Contributions to SpotBugs
 * Copyright (C) 2026, spotbugs
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

package edu.umd.cs.findbugs.classfile.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.ICodeBaseEntry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests that {@link JrtfsCodeBase} reads the module image of the JDK the given {@code jrt-fs.jar}
 * belongs to, and not the image of the JDK that happens to run SpotBugs.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/pull/4329">PR #4329</a>
 */
class JrtfsCodeBaseTest {

    /** Name of the environment variable pointing at a JDK home other than the running one. */
    private static final String OTHER_JDK_HOME = "TEST_JRT_OTHER_JDK_HOME";

    /** Offset between a Java feature version and the class file major version, e.g. 17 -&gt; 61. */
    private static final int CLASS_FILE_MAJOR_OFFSET = 44;

    /** Key of the version line of a JDK {@code release} file. */
    private static final String JAVA_VERSION_KEY = "JAVA_VERSION=";

    private static final byte[] MARKER_CLASS_FILE = "spotbugs-jrtfs-marker".getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        Global.setAnalysisCacheForCurrentThread(new NoopAnalysisCache());
    }

    @AfterEach
    void tearDown() {
        Global.setAnalysisCacheForCurrentThread(null);
    }

    /**
     * Builds a minimal foreign "JDK" consisting of a copy of the running JDK's {@code jrt-fs.jar}
     * and an exploded module directory holding a single marker resource. The {@code jrt} provider
     * serves such an exploded {@code <javaHome>/modules} directory when {@code lib/modules} is
     * absent, so the codebase must expose exactly that one resource - and none of the classes of
     * the running JVM.
     */
    @Test
    void readsImageOfTheJdkOwningTheJrtFsJar(@TempDir Path javaHome) throws Exception {
        Path jrtFsJar = createFakeJdk(javaHome);

        try (JrtfsCodeBase codeBase = new JrtfsCodeBase(new FilesystemCodeBaseLocator(jrtFsJar.toString()),
                jrtFsJar.toString())) {
            ICodeBaseEntry entry = codeBase.lookupResource("com/example/Marker.class");
            assertNotNull(entry, "must read the module image of the JDK owning the given jrt-fs.jar");
            assertArrayEquals(MARKER_CLASS_FILE, readAllBytes(entry));

            assertNull(codeBase.lookupResource("java/lang/Object.class"),
                    "must not fall back to the module image of the running JVM");
        }
    }

    /**
     * Reads {@code java/lang/Object.class} from a second, real JDK and checks that the class file
     * really comes from that JDK. Enabled by CI, which provides a JDK other than the one the build
     * runs on; skipped when both happen to be the same feature version, because the assertion
     * could not tell the two images apart then.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = OTHER_JDK_HOME, matches = ".+")
    void readsClassFilesOfAnotherJdk() throws Exception {
        Path javaHome = Path.of(System.getenv(OTHER_JDK_HOME));
        Path jrtFsJar = javaHome.resolve("lib").resolve("jrt-fs.jar");
        assumeTrue(Files.isRegularFile(jrtFsJar), () -> jrtFsJar + " does not exist");

        int feature = featureVersionOf(javaHome);
        assumeTrue(feature != Runtime.version().feature(),
                () -> OTHER_JDK_HOME + " is Java " + feature + ", same as the running JVM");

        try (JrtfsCodeBase codeBase = new JrtfsCodeBase(new FilesystemCodeBaseLocator(jrtFsJar.toString()),
                jrtFsJar.toString())) {
            ICodeBaseEntry entry = codeBase.lookupResource("java/lang/Object.class");
            assertNotNull(entry, "java/lang/Object.class must be found in " + javaHome);

            assertEquals(feature + CLASS_FILE_MAJOR_OFFSET, classFileMajorVersion(readAllBytes(entry)),
                    "java/lang/Object.class must be read from " + javaHome + " and not from the running JVM");
        }
    }

    /**
     * Creates {@code <javaHome>/lib/jrt-fs.jar} and an exploded module holding the marker
     * resource, and returns the path of the jar.
     */
    private static Path createFakeJdk(Path javaHome) throws IOException {
        Path runtimeJrtFsJar = Path.of(System.getProperty("java.home"), "lib", "jrt-fs.jar");
        assumeTrue(Files.isRegularFile(runtimeJrtFsJar), () -> runtimeJrtFsJar + " does not exist");

        Path jrtFsJar = javaHome.resolve("lib").resolve("jrt-fs.jar");
        Files.createDirectories(jrtFsJar.getParent());
        Files.copy(runtimeJrtFsJar, jrtFsJar, StandardCopyOption.REPLACE_EXISTING);

        Path marker = javaHome.resolve("modules").resolve("spotbugs.test.module").resolve("com").resolve("example")
                .resolve("Marker.class");
        Files.createDirectories(marker.getParent());
        Files.write(marker, MARKER_CLASS_FILE);

        return jrtFsJar;
    }

    /**
     * Reads the feature version, e.g. 17, from the {@code JAVA_VERSION="17.0.20.1"} line of the
     * {@code release} file of a JDK.
     */
    private static int featureVersionOf(Path javaHome) throws IOException {
        Path release = javaHome.resolve("release");
        assumeTrue(Files.isRegularFile(release), () -> release + " does not exist");

        String version = Files.readAllLines(release, StandardCharsets.UTF_8).stream()
                .filter(line -> line.startsWith(JAVA_VERSION_KEY))
                .map(line -> line.substring(JAVA_VERSION_KEY.length()).replace("\"", "").trim())
                .findFirst()
                .orElse("");
        assumeTrue(!version.isEmpty(), () -> "no " + JAVA_VERSION_KEY + " line in " + release);
        return Runtime.Version.parse(version).feature();
    }

    private static int classFileMajorVersion(byte[] classFile) {
        return ((classFile[6] & 0xff) << 8) | (classFile[7] & 0xff);
    }

    private static byte[] readAllBytes(ICodeBaseEntry entry) throws IOException {
        try (InputStream in = entry.openResource()) {
            return in.readAllBytes();
        }
    }
}
