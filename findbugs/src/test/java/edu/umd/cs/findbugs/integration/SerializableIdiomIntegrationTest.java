/*
 * SpotBugs - Find Bugs in Java programs
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

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class SerializableIdiomIntegrationTest extends AbstractIntegrationTest {

    @Test
    public void testBadFieldNotSerializable() {
        performAnalysis("sfBugs/RFE3062724.class", "sfBugs/RFE3062724$A.class",
                "sfBugs/RFE3062724$B.class", "sfBugs/RFE3062724$C.class");

        // There should only be 2 issue of this type
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SE_BAD_FIELD").build();
        assertThat(getBugCollection(), containsExactly(bugTypeMatcher, 2));

        // It must be on the notSerializable field
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("SE_BAD_FIELD")
                .inClass("sfBugs.RFE3062724$B")
                .atField("notSerializable")
                .withConfidence(Confidence.HIGH)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));

        // It must be on the notSerializable field
        final BugInstanceMatcher bugInstanceMatcher2 = new BugInstanceMatcherBuilder()
                .bugType("SE_BAD_FIELD")
                .inClass("sfBugs.RFE3062724$C")
                .atField("notSerializable")
                .withConfidence(Confidence.HIGH)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher2));
    }
}
