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

class BadCastTest extends AbstractIntegrationTest {
    @Test
    void testCastOfArray() {
        performAnalysis("CastOfArray.class");

        assertNoBugType("BC_BAD_CAST_TO_ABSTRACT_COLLECTION");
        assertNoBugType("BC_IMPOSSIBLE_CAST_PRIMITIVE_ARRAY");
        assertNoBugType("BC_IMPOSSIBLE_CAST");
        assertNoBugType("BC_IMPOSSIBLE_DOWNCAST");
        assertNoBugType("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY");
        assertNoBugType("BC_UNCONFIRMED_CAST");
        assertNoBugType("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE");
        assertNoBugType("BC_BAD_CAST_TO_CONCRETE_COLLECTION");
        assertNoBugType("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY");
    }

    @Test
    void testBC_Unconfirmed_Cast() {
        performAnalysis("BC_Unconfirmed_Cast.class", "BC_Unconfirmed_Cast$CastToMe.class",
                "Parent.class", "Child1.class", "Child2.class");

        assertBugTypeCount("BC_IMPOSSIBLE_CAST", 2);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "BC_Unconfirmed_Cast", "main", 16);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "BC_Unconfirmed_Cast", "main", 18);
    }

    @Test
    void testIDiv() {
        performAnalysis("IDiv.class");

        assertBugTypeCount("ICAST_IDIV_CAST_TO_DOUBLE", 1);
        assertBugInMethodAtLine("ICAST_IDIV_CAST_TO_DOUBLE", "IDiv", "main", 6);
    }
}
