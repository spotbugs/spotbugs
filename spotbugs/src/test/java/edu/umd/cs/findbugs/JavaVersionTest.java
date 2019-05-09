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

package edu.umd.cs.findbugs;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JavaVersionTest {

    private static void testJavaVersionString(String version, int maj, int min, String rest)
            throws JavaVersionException {
        JavaVersion jv = new JavaVersion(version);
        assertEquals(maj, jv.getMajor());
        assertEquals(min, jv.getMinor());
        assertEquals(rest, jv.getRest());
    }

    /**
     * Test method for {@link edu.umd.cs.findbugs.JavaVersion#JavaVersion(java.lang.String)}.
     * @throws JavaVersionException if version string cannot be parsed
     */
    @Test
    public void testJavaVersionString() throws JavaVersionException {
        // Historical versions (up to Java 8)
        testJavaVersionString("1.7", 1, 7, "");
        testJavaVersionString("1.7.0", 1, 7, "0");
        testJavaVersionString("1.7.0_80", 1, 7, "0_80");
        testJavaVersionString("1.8.0_66", 1, 8, "0_66");
        // New scheme for Java 9 and later (JEP 223)
        // See http://openjdk.java.net/jeps/223
        testJavaVersionString("9-ea", 9, 0, "-ea");
        testJavaVersionString("9", 9, 0, "");
        testJavaVersionString("9.1.2", 9, 1, "2");
        testJavaVersionString("9.0.1", 9, 0, "1");
        // Long versions
        testJavaVersionString("1.7.0_65-b20", 1, 7, "0_65-b20");
        testJavaVersionString("7.6.15+20", 7, 6, "15+20");
        testJavaVersionString("1.9.0-ea-b19", 1, 9, "0-ea-b19");
        testJavaVersionString("9-ea+19", 9, 0, "-ea+19");
    }
}
