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

package edu.umd.cs.findbugs.integration;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindNullDerefIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void testNullFromReturnOnLambda() {
        performAnalysis("Elvis.class");

        // There should only be 1 issue of this type
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SI_INSTANCE_BEFORE_FINALS_ASSIGNED").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        // It must be on the INSTANCE field
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("SI_INSTANCE_BEFORE_FINALS_ASSIGNED")
                .inClass("Elvis")
                .atField("INSTANCE")
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    @Test
    public void testLambdaIssue20() throws IOException, InterruptedException {
        performAnalysis("lambdas/Issue20.class");

        // There should only be 1 issue of this type
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        // It must be on the lambda method, checking by line number
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
                .inClass("Issue20")
                .atLine(24)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
