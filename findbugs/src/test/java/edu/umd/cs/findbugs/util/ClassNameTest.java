/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006,2008 University of Maryland
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

package edu.umd.cs.findbugs.util;

import junit.framework.TestCase;

/**
 * @author pugh
 */
public class ClassNameTest extends TestCase {

    public void testExtractPackagePrefix() {
        assertEquals("", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 0));
        assertEquals("org", ClassName.extractPackagePrefix("org", 1));
        assertEquals("org.apache.ant", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 3));
        assertEquals("org.apache.ant.subpkg.sub2", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 5));
        assertEquals("org.apache.ant.subpkg.sub2", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 6));
    }

    public void testExtractClassName() {
        assertEquals("java/lang/Integer", ClassName.extractClassName("Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[[[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("java/lang/Integer"));
    }

    public void testGetPrimitiveType() {
        assertEquals("I", ClassName.getPrimitiveType("java/lang/Integer"));
        assertEquals("F", ClassName.getPrimitiveType("java/lang/Float"));
        assertEquals("D", ClassName.getPrimitiveType("java/lang/Double"));
        assertEquals("J", ClassName.getPrimitiveType("java/lang/Long"));
        assertEquals("B", ClassName.getPrimitiveType("java/lang/Byte"));
        assertEquals("C", ClassName.getPrimitiveType("java/lang/Character"));
        assertEquals("S", ClassName.getPrimitiveType("java/lang/Short"));
        assertEquals("Z", ClassName.getPrimitiveType("java/lang/Boolean"));
        assertNull(ClassName.getPrimitiveType("java/lang/String"));
        assertNull(ClassName.getPrimitiveType("java/util/HashMap"));
    }

    public void testExtractClassNameBad() {
        try {
            ClassName.extractClassName("L[Ljava/lang/Integer;");
            fail();
        } catch (IllegalArgumentException e) {
            assert true;
        }

    }

}
