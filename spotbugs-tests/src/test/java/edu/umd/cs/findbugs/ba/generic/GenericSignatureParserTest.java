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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.spotbugs.java.lang.classfile.MethodSignature;
import com.github.spotbugs.java.lang.classfile.Signature;

/**
 * @author pugh
 */
class GenericSignatureParserTest {

    @Test
    void testGenerics() {
        MethodSignature signature = MethodSignature.parseFrom(
                "(Lcom/sleepycat/persist/EntityJoin<TPK;TE;>.JoinForwardCursor<TV;>;)V");
        assertEquals(1, signature.arguments().size());
    }

    @Test
    void testThrowsGenerics() {
        MethodSignature signature = MethodSignature.parseFrom("(Ljava/lang/String;)V^TE1;^TE2;^TE3;");
        assertEquals(1, signature.arguments().size());
    }

    private void processTest(String genericSignature, String... substrings) {
        MethodSignature signature = MethodSignature.parseFrom(genericSignature);
        Iterator<String> iter = signature.arguments().stream().map(Signature::signatureString).iterator();

        for (String s : substrings) {
            assertTrue(iter.hasNext());
            assertEquals(s, iter.next());
        }
        assertFalse(iter.hasNext());
    }

    @Test
    void testSignatures() {
        processTest("(Ljava/lang/Comparable;)V", "Ljava/lang/Comparable;");

        processTest("(Ljava/lang/Comparable;TE;[Ljava/lang/Comparable;)V", "Ljava/lang/Comparable;", "TE;",
                "[Ljava/lang/Comparable;");

        processTest("(TE;[Ljava/lang/Comparable;TV;)V", "TE;", "[Ljava/lang/Comparable;", "TV;");
    }

    @Test
    @Disabled("Invalid signature fixed in Eclipse JDT: https://bugs.eclipse.org/bugs/show_bug.cgi?id=494198")
    void testEclipseJDTInvalidSignature() {
        MethodSignature signature = MethodSignature.parseFrom("(!+LHasUniqueKey<Ljava/lang/Integer;>;)V");
        assertEquals(1, signature.arguments().size());
        assertEquals("+LHasUniqueKey<Ljava/lang/Integer;>;", signature.arguments().get(0).signatureString());
    }
}
