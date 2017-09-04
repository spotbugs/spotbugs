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

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.test.AnalysisRunner;
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
public class AbstractIntegrationTest {

    private BugCollectionBugReporter bugReporter;

    private File getFindbugsTestCases() {
        final File f = new File(SystemProperties.getProperty("spotbugsTestCases.home", "../spotbugsTestCases"));
        assertTrue("'spotbugsTestCases' directory not found", f.exists());
        assertTrue(f.isDirectory());
        assertTrue(f.canRead());

        return f;
    }

    private File getFindbugsTestCasesFile(final String path) {
        final File f = new File(getFindbugsTestCases(), path);
        assertTrue(f.getAbsolutePath() + " not found", f.exists());
        assertTrue(f.getAbsolutePath() + " is not readable", f.canRead());

        return f;
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

        final File lib = getFindbugsTestCasesFile("lib");
        for (final File f : lib.listFiles()) {
            final String path = f.getPath();
            if (f.canRead() && path.endsWith(".jar")) {
                runner.addAuxClasspathEntry(f.toPath());
            }
        }

        // TODO : Unwire this once we move bug samples to a proper sourceset
        Path[] paths = Arrays.stream(analyzeMe)
                .map(s -> getFindbugsTestCasesFile("/build/classes/java/main/" + s).toPath())
                .collect(Collectors.toList())
                .toArray(new Path[analyzeMe.length]);
        this.bugReporter = runner.run(paths);
    }
}
