/*
 * Contributions to SpotBugs
 * Copyright (C) 2017, kengo
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
package edu.umd.cs.findbugs.filter;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @since 3.1
 */
class RelationalOpTest {

    @Test
    void testRelationalObject() {
        assertTrue(RelationalOp.byName("EQ").check("A", "A"));
        assertFalse(RelationalOp.byName("EQ").check("B", "C"));

        assertFalse(RelationalOp.byName("NEQ").check("A", "A"));
        assertTrue(RelationalOp.byName("NEQ").check("B", "C"));

        assertTrue(RelationalOp.byName("GEQ").check("A", "A"));
        assertFalse(RelationalOp.byName("GEQ").check("B", "C"));
        assertTrue(RelationalOp.byName("GEQ").check("E", "D"));

        assertFalse(RelationalOp.byName("GT").check("A", "A"));
        assertFalse(RelationalOp.byName("GT").check("B", "C"));
        assertTrue(RelationalOp.byName("GT").check("E", "D"));

        assertTrue(RelationalOp.byName("LEQ").check("A", "A"));
        assertTrue(RelationalOp.byName("LEQ").check("B", "C"));
        assertFalse(RelationalOp.byName("LEQ").check("E", "D"));
    }

    @Test
    void testToString() {
        assertThat(RelationalOp.EQ.toString(), is("=="));
    }

    @Test
    void testByName() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            RelationalOp.byName("Unknown");
        });
    }
}
