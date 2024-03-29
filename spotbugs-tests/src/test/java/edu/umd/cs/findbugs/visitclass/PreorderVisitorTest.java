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

package edu.umd.cs.findbugs.visitclass;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author bill.pugh
 */
class PreorderVisitorTest {

    @Test
    void testGetNumberArguments() {
        assertEquals(0, PreorderVisitor.getNumberArguments("()V"));
        assertEquals(0, PreorderVisitor.getNumberArguments("()I"));
        assertEquals(0, PreorderVisitor.getNumberArguments("()J"));
        assertEquals(1, PreorderVisitor.getNumberArguments("(I)V"));
        assertEquals(1, PreorderVisitor.getNumberArguments("(I)I"));
        assertEquals(1, PreorderVisitor.getNumberArguments("(J)I"));
        assertEquals(1, PreorderVisitor.getNumberArguments("([J)I"));
        assertEquals(1, PreorderVisitor.getNumberArguments("([Ljava/lang/String;)I"));
        assertEquals(3, PreorderVisitor.getNumberArguments("(J[Ljava/lang/String;J)I"));
    }
}
