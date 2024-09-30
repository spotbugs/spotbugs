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
import org.junit.jupiter.api.Test;


class Bug1864046Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("sfBugs/Bug1864046.class");
        assertNoBugType("DLS_DEAD_LOCAL_STORE");
        assertNoBugType("IP_PARAMETER_IS_DEAD_BUT_OVERWRITTEN");
        assertNoBugType("DLS_DEAD_LOCAL_STORE_OF_NULL");
        assertNoBugType("DLS_DEAD_STORE_OF_CLASS_LITERAL");
        assertNoBugType("DLS_DEAD_LOCAL_STORE_SHADOWS_FIELD");
        assertNoBugType("DLS_DEAD_LOCAL_INCREMENT_IN_RETURN");
    }
}
