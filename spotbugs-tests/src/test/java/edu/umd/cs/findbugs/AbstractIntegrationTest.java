/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import static edu.umd.cs.findbugs.test.CountMatcher.containsBetween;
import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.test.AnalysisRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Abstract class for integration testing. Extend, call {@code performAnalysis("com/company/classname.class")},
 * and finally assert the issues found over {@code getBugCollection()} by creating appropriate matchers with {@link BugInstanceMatcherBuilder}
 * For example:
 *
 * <pre>
 * <code>
 * public class MyIntegrationTest extends AbstractIntegrationTest {
 *
 *     @Test
 *     public void testIssuesAreFound() {
 *         performAnalysis("my/company/AnalyzedClass.class");
 *
 *         // There should only be exactly 1 issue of this type
 *         final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
 *                 .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
 *         assertThat(getBugCollection(), CountMatcher.containsExactly(1, bugTypeMatcher));
 *
 *         final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
 *                 .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
 *                 .inClass("AnalyzedClass")
 *                 .atLine(25)
 *                 .build();
 *         assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
 *     }
 * }
 * </code>
 * </pre>
 *
 * @author jmsotuyo
 */
public abstract class AbstractIntegrationTest {

    /**
     * Build prefix directories to search for classes
     */
    private static final List<Path> BUILD_CLASS_SEARCH_PREFIXES = List.of(
            // Build path if running command line build
            Paths.get("build/classes/java/main"),
            Paths.get("build/classes/groovy/main"),
            // Build path if running in Eclipse
            Paths.get("classesEclipse"));

    private BugCollectionBugReporter bugReporter;

    private static Path getFindbugsTestCases() {
        final Path p = Paths.get(SystemProperties.getProperty("spotbugsTestCases.home", "../spotbugsTestCases"));
        assertTrue(Files.exists(p), "'spotbugsTestCases' directory not found");
        assertTrue(Files.isDirectory(p));
        assertTrue(Files.isReadable(p));

        return p;
    }

    private static Path getFindbugsTestCasesFile(final String path) {
        final Path root = getFindbugsTestCases();
        final Path p = BUILD_CLASS_SEARCH_PREFIXES.stream()
                .map(prefix -> root.resolve(prefix).resolve(path))
                .filter(Files::exists)
                .findFirst()
                .orElseThrow(() -> new AssertionError(path + " not found"));

        assertTrue(Files.isReadable(p), p + " is not readable");
        return p;
    }

    protected BugCollection getBugCollection() {
        return bugReporter.getBugCollection();
    }

    /**
     * Sets up a FB engine to run on the 'spotbugsTestCases' project. It enables
     * all the available detectors and reports all the bug categories. Uses a
     * low priority threshold.
     */
    protected void performAnalysis(@SlashedClassName final String... analyzeMe) {
        AnalysisRunner runner = new AnalysisRunner();

        final Path lib = getFindbugsTestCases().resolve("lib");
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(lib, "*.jar")) {
            for (Path jar : ds) {
                if (Files.isReadable(jar)) {
                    runner.addAuxClasspathEntry(jar);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        final Path dependencies = getFindbugsTestCases().resolve("build/spotbugs/auxclasspath/spotbugsMain");
        try {
            final List<String> lines = Files.readAllLines(dependencies);
            for (String line : lines) {
                Path path = Paths.get(line);
                if (Files.isReadable(path)) {
                    runner.addAuxClasspathEntry(path);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // TODO : Unwire this once we move bug samples to a proper sourceset
        Path[] paths = Arrays.stream(analyzeMe)
                .map(AbstractIntegrationTest::getFindbugsTestCasesFile)
                .toArray(Path[]::new);
        bugReporter = runner.run(paths);
    }

    /**
     * Asserts that there are exactly zero bug instances with the given bug type.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     */
    protected final void assertNoBugType(String bugType) {
        assertBugTypeCount(bugType, 0);
    }

    /**
     * Asserts that there are exactly the given number of bug instances with the given bug type.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param count the expected number of matches
     */
    protected final void assertBugTypeCount(String bugType, int count) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(count, matcher));
    }

    /**
     * Asserts that there are at least {@code min} and at most {@code max} number of bug instances (both inclusive) with the given bug type.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param min the expected minimal number of matches (inclusive)
     * @param max the expected maximal number of matches (inclusive)
     */
    protected final void assertBugTypeCountBetween(String bugType, int min, int max) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsBetween(min, max, matcher));
    }

    /**
     * Asserts that there are exactly zero bug instances with the given bug type in the given class.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     */
    protected final void assertNoBugInClass(String bugType, String className) {
        assertBugInClassCount(bugType, className, 0);
    }

    /**
     * Asserts that there are exactly the given number of bug instances with the given bug type in the given class.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param count the expected number of matches
     */
    protected final void assertBugInClassCount(String bugType, String className, int count) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .build();
        assertThat(getBugCollection(), containsExactly(count, matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     */
    protected final void assertBugInClass(String bugType, String className) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there are exactly zero bug instances with the given bug type in the given class in the given method.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     */
    protected final void assertNoBugInMethod(String bugType, String className, String method) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class in the given method.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     */
    protected final void assertBugInMethod(String bugType, String className, String method) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class in the given method at the given line.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     * @param line the expected line
     */
    protected final void assertBugInMethodAtLine(String bugType, String className, String method, int line) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class in the given method at the given line with the given confidence.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     * @param line the expected line
     * @param confidence the expected confidence
     */
    protected final void assertBugInMethodAtLineWithConfidence(String bugType, String className, String method, int line, Confidence confidence) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .atLine(line)
                .withConfidence(confidence)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class in the given method at the given field.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     * @param fieldName the expected field's name
     */
    protected final void assertBugInMethodAtField(String bugType, String className, String method, String fieldName) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .atField(fieldName)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class at the given field.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param fieldName the expected field's name
     */
    protected final void assertBugAtField(String bugType, String className, String fieldName) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .atField(fieldName)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class at the given field at the given line.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param fieldName the expected field's name
     * @param line the expected line
     */
    protected final void assertBugAtFieldAtLine(String bugType, String className, String fieldName, int line) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .atField(fieldName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class in the given method at the given variable at the given line.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     * @param variableName the expected variable name
     * @param line the expected line
     */
    protected final void assertBugAtVar(String bugType, String className, String method, String variableName, int line) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .atVariable(variableName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type in the given class in the given method at the given variable.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param className the expected class name. See {@link BugInstanceMatcher#BugInstanceMatcher(String, String, String, String, String, Integer, Integer, Confidence, String, List) BugInstanceMatcher's constructor} for details.
     * @param method the expected method's name
     * @param variableName the expected variable name
     */
    protected final void assertBugInMethodAtVariable(String bugType, String className, String method, String variableName) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .atVariable(variableName)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    /**
     * Asserts that there is a bug instance with the given bug type at the given line.
     * The match is checked according to {@link BugInstanceMatcher}.
     *
     * @param bugType the expected bug type
     * @param line the expected line
     */
    protected final void assertBugAtLine(String bugType, int line) {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
