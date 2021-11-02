/*
 * Contributions to SpotBugs
 * Copyright (C) 2020, ritchan
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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/374">GitHub issue</a>
 */
public class Issue374Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis(
                "ghIssues/issue374/package-info.class",
                "ghIssues/issue374/ClassLevel.class",
                "ghIssues/issue374/MethodLevel.class",
                "ghIssues/issue374/PackageLevel.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
                .build();
        assertThat(getBugCollection(), containsExactly(3, matcher));
    }
}
