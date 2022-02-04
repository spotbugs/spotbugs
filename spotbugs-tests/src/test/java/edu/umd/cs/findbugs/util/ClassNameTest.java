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

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author pugh
 */
public class ClassNameTest {

    @Test
    public void testExtractPackagePrefix() {
        assertEquals("", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 0));
        assertEquals("org", ClassName.extractPackagePrefix("org", 1));
        assertEquals("org.apache.ant", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 3));
        assertEquals("org.apache.ant.subpkg.sub2", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 5));
        assertEquals("org.apache.ant.subpkg.sub2", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 6));
    }

    @Test
    public void testExtractClassName() {
        assertEquals("java/lang/Integer", ClassName.extractClassName("Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[[[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("java/lang/Integer"));
    }

    @Test
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

    @Test(expected = IllegalArgumentException.class)
    public void testExtractClassNameBad() {
        ClassName.extractClassName("L[Ljava/lang/Integer;");
    }

    @Test
    public void testMatchedPrefix() {
        List<String[]> negativeCases = Arrays.asList(
                new String[] { "foobar" },
                new String[] { "" },
                new String[] { "testObject" });

        for (String[] searchString : negativeCases) {
            assertFalse("com.text.TestClass should not be matched by " + Arrays.toString(searchString),
                    ClassName.matchedPrefixes(searchString, "com.test.TestClass"));
        }

        List<String[]> positiveCases = Arrays.asList(
                new String[] {},
                null,
                new String[] { "TestClass" },
                new String[] { "testclass" },
                new String[] { "testclass" },
                new String[] { "testClass" },
                new String[] { "tetGlass" },
                new String[] { "bastClass" });

        for (String[] searchString : positiveCases) {
            assertTrue("com.text.TestClass should be matched by " + Arrays.toString(searchString),
                    ClassName.matchedPrefixes(searchString, "com.test.TestClass"));
        }
    }

    @Test
    public void testSimpleBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent"));
    }

    @Test
    public void testSimpleDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent"));
    }

    @Test
    public void testInnerClassBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$Child"));
    }

    @Test
    public void testInnerClassDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$Child"));
    }

    @Test
    public void testJavaStyleAnonymousInnerClassBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$Child$1"));
    }

    @Test
    public void testJavaStyleAnonymousInnerClassDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$Child$1"));
    }

    @Test
    public void testKotlinStyleAnonymousInnerClassBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$function$variable$1"));
    }

    @Test
    public void testKotlinStyleAnonymousInnerClassDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$function$variable$1"));
    }

    @Test
    public void testBinaryClassNameContainingAllowedSpecialCharactersIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$function!@#%^&*,?{}]()$variable$1"));
    }

    @Test
    public void testDottedClassNameContainingAllowedSpecialCharactersIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$function!@#%^&*,?{}]()$variable$1"));
    }

    @Test
    public void testFieldDescriptorClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("Lcom/bla/Parent;"));
    }

    @Test
    public void testFieldDescriptorOneDimensionalArrayClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[Lcom/bla/Parent;"));
    }

    @Test
    public void testFieldDescriptorOneDimensionalPrimitiveArrayClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[I"));
    }

    @Test
    public void testFieldDescriptorTwoDimensionalArrayClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[[Lcom/bla/Parent;"));
    }

    @Test
    public void testFieldDescriptorClassNameContainingAllowedSpecialCharactersIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[[Lcom/bla/Parent$function!@#%^&*,?{}]()$variable$1;"));
    }

    @Test
    public void testFieldDescriptorClassNameContainingDotsIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName("Lcom.bla.Parent;"));
    }

    @Test
    public void testClassNameContainingBothSlashesAndDotsIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName("com.bla/Parent"));
    }

    @Test
    public void testBinaryClassNameContainingDisallowedSpecialCharactersIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName("com/bla/Parent$function$variable$1;"));
        assertFalse(ClassName.isValidClassName("com/bla/Parent$function;$variable$1"));
        assertFalse(ClassName.isValidClassName("com/bla/Parent$function[$variable$1"));
    }

    @Test
    public void testEmptyStringIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName(("")));
    }
}
