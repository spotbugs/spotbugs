/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.generic;

import java.util.Iterator;

import junit.framework.TestCase;

/**
 * @author Nat Ayewah
 */
public class TestGenericSignatureParser extends TestCase {

    public void processTest(String genericSignature, String... substrings) {
        GenericSignatureParser parser = new GenericSignatureParser(genericSignature);
        Iterator<String> iter = parser.parameterSignatureIterator();

        for (String s : substrings) {
            assertTrue(iter.hasNext());
            assertEquals(s, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    public void testSignatures() {
        processTest("(Ljava/lang/Comparable;)V", "Ljava/lang/Comparable;");

        processTest("(Ljava/lang/Comparable;TE;**[Ljava/lang/Comparable;)V", "Ljava/lang/Comparable;", "TE;", "*", "*",
                "[Ljava/lang/Comparable;");

        processTest("(TE;*+[Ljava/lang/Comparable;-TV;)V", "TE;", "*", "+[Ljava/lang/Comparable;", "-TV;");
    }
}
