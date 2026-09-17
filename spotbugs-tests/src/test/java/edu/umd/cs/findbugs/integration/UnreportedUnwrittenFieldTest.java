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
import static org.junit.Assert.assertThat;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

public class UnreportedUnwrittenFieldTest extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("UnreportedUnwrittenField.class");
        BugCollection bugCollection = getBugCollection();

        System.err.println("GRANT: " + bugCollection.toString());

        // There should be 1 UWF_UNWRITTEN_FIELD issue.
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
            .bugType("UWF_UNWRITTEN_FIELD").build();
        assertThat(bugCollection, containsExactly(1, bugTypeMatcher));
    }
}
