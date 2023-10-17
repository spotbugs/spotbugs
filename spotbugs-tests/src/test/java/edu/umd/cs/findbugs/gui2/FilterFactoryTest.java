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

package edu.umd.cs.findbugs.gui2;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.filter.Matcher;
import edu.umd.cs.findbugs.filter.NotMatcher;

/**
 * @author Graham Allan (grundlefleck@gmail.com)
 */
class FilterFactoryTest {

    @Test
    void invertMatcherShouldNegateTheOriginalMatchingResult() {
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        Matcher originalMatcher = FilterFactory.makeMatcher(asList(Sortables.BUGCODE), bug);

        assertTrue(originalMatcher.match(bug), "Original matcher should match bug.");

        Matcher notMatcher = FilterFactory.invertMatcher(originalMatcher);
        assertTrue(notMatcher instanceof NotMatcher, "Should return an instance of NotMatcher.");
        assertFalse(notMatcher.match(bug), "Inverted matcher should now not match.");
    }

    @Test
    void shouldReturnTheOriginalMatcherWhenAskedToInvertANotMatcher() {
        BugInstance bug = new BugInstance("UUF_UNUSED_FIELD", 0);
        Matcher originalMatcher = FilterFactory.makeMatcher(asList(Sortables.BUGCODE), bug);
        Matcher notMatcher = FilterFactory.invertMatcher(originalMatcher);
        Matcher notNotMatcher = FilterFactory.invertMatcher(notMatcher);

        assertSame(originalMatcher, notNotMatcher, "Should return the originally wrapped matcher.");
        assertTrue(notNotMatcher.match(bug), "Original matcher should now not match.");
    }
}
