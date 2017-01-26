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
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import edu.umd.cs.findbugs.config.UserPreferences;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
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
 *         assertThat(getBugCollection(), AbstractIntegrationTest.containsExactly(bugTypeMatcher, 1));
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
    private IFindBugsEngine engine;

    private File getFindbugsTestCases() {
        final File f = new File(SystemProperties.getProperty("findbugsTestCases.home", "../findbugsTestCases"));
        assertTrue("'findbugsTestCases' directory not found", f.exists());
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

    protected static <T> Matcher<Iterable<T>> containsExactly(final Matcher<T> matcher, final int count) {
        return new CountMatcher<T>(count, matcher);
    }

    /**
     * Sets up a FB engine to run on the 'findbugsTestCases' project. It enables
     * all the available detectors and reports all the bug categories. Uses a
     * low priority threshold.
     */
    protected void performAnalysis(@SlashedClassName final String... analyzeMe) {
        DetectorFactoryCollection.resetInstance(new DetectorFactoryCollection());

        this.engine = new FindBugs2();
        final Project project = new Project();
        project.setProjectName(getClass().getSimpleName());
        this.engine.setProject(project);

        final DetectorFactoryCollection detectorFactoryCollection = DetectorFactoryCollection.instance();
        engine.setDetectorFactoryCollection(detectorFactoryCollection);

        bugReporter = new BugCollectionBugReporter(project);
        bugReporter.setPriorityThreshold(Priorities.LOW_PRIORITY);
        bugReporter.setRankThreshold(BugRanker.VISIBLE_RANK_MAX);

        engine.setBugReporter(this.bugReporter);
        final UserPreferences preferences = UserPreferences.createDefaultUserPreferences();
        preferences.getFilterSettings().clearAllCategories();
        this.engine.setUserPreferences(preferences);

        for (final String s : analyzeMe) {
            // TODO : Unwire this once we move bug samples to a proper sourceset
            project.addFile(getFindbugsTestCasesFile("/build/classes/main/" + s).getPath());
        }

        try {
            engine.execute();
        } catch (final IOException | InterruptedException e) {
            fail("Analysis failed with exception; " + e.getMessage());
        }
        if (! bugReporter.getQueuedErrors().isEmpty()) {
            AssertionError assertionError = new AssertionError("Analysis failed with exception. Check stderr for detail.");
            bugReporter.getQueuedErrors().stream()
                .map(error -> error.getCause())
                .forEach(assertionError::addSuppressed);
            throw assertionError;
        }
    }

    private static final class CountMatcher<T> extends BaseMatcher<Iterable<T>> {
        private final int count;
        private final Matcher<T> matcher;

        /**
         * @param count
         * @param matcher
         */
        private CountMatcher(int count, Matcher<T> matcher) {
            this.count = count;
            this.matcher = matcher;
        }

        @Override
        public boolean matches(final Object obj) {
            int matches = 0;

            if (obj instanceof Iterable<?>) {
                final Iterable<?> it = (Iterable<?>) obj;
                for (final Object o : it) {
                    if (matcher.matches(o)) {
                        matches++;
                    }
                }
            }

            return matches == count;
        }

        @Override
        public void describeTo(final Description desc) {
            desc.appendText("Iterable containing exactly " + count + " ").appendDescriptionOf(matcher);
        }
    }
}
