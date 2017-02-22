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

package edu.umd.cs.findbugs.detect;

import junit.framework.TestCase;

/**
 * @author pugh
 */
public class IncompatMaskTest extends TestCase {

    void check(long value) {
        assertEquals(1, IncompatMask.populationCount(value));
        boolean isLong = (value >>> 32) != 0;
        assertEquals(value, IncompatMask.getFlagBits(true, value));
        assertEquals(value, IncompatMask.getFlagBits(true, ~value));
        if (!isLong) {
            assertEquals(value, IncompatMask.getFlagBits(false, value));
            assertEquals(value, IncompatMask.getFlagBits(false, ~value));
        }
    }

    public void testGetFlagBits() {
        check(1);
        check(4);
        check(0x10000000L);
        check(0x80000000L);
        check(0x100000000L);
        check(0x10000000000L);
        check(Long.MIN_VALUE >>> 1);
        check(Long.MIN_VALUE);

    }

}
