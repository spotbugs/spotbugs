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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author pugh
 */
class ClassNameTest {

    @Test
    void testExtractPackagePrefix() {
        assertEquals("", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 0));
        assertEquals("org", ClassName.extractPackagePrefix("org", 1));
        assertEquals("org.apache.ant", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 3));
        assertEquals("org.apache.ant.subpkg.sub2", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 5));
        assertEquals("org.apache.ant.subpkg.sub2", ClassName.extractPackagePrefix("org.apache.ant.subpkg.sub2", 6));
    }

    @Test
    void testExtractClassName() {
        assertEquals("java/lang/Integer", ClassName.extractClassName("Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("[[[Ljava/lang/Integer;"));
        assertEquals("java/lang/Integer", ClassName.extractClassName("java/lang/Integer"));
    }

    @Test
    void testGetPrimitiveType() {
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

    @Test
    void testExtractClassNameBad() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ClassName.extractClassName("L[Ljava/lang/Integer;");
        });
    }

    @Test
    void testMatchedPrefix() {
        List<String[]> negativeCases = Arrays.asList(
                new String[] { "foobar" },
                new String[] { "" },
                new String[] { "testObject" });

        for (String[] searchString : negativeCases) {
            Assertions.assertFalse(ClassName.matchedPrefixes(searchString, "com.test.TestClass"),
                    "com.text.TestClass should not be matched by " + Arrays.toString(searchString));
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
            assertTrue(ClassName.matchedPrefixes(searchString, "com.test.TestClass"),
                    "com.text.TestClass should be matched by " + Arrays.toString(searchString));
        }
    }

    @Test
    void testSimpleBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent"));
    }

    @Test
    void testSimpleDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent"));
    }

    @Test
    void testInnerClassBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$Child"));
    }

    @Test
    void testInnerClassDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$Child"));
    }

    @Test
    void testJavaStyleAnonymousInnerClassBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$Child$1"));
    }

    @Test
    void testJavaStyleAnonymousInnerClassDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$Child$1"));
    }

    @Test
    void testKotlinStyleAnonymousInnerClassBinaryClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$function$variable$1"));
    }

    @Test
    void testKotlinStyleAnonymousInnerClassDottedClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$function$variable$1"));
    }

    @Test
    void testBinaryClassNameContainingAllowedSpecialCharactersIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com/bla/Parent$function!@#%^&*,?{}]()$variable$1"));
    }

    @Test
    void testDottedClassNameContainingAllowedSpecialCharactersIsValidClassName() {
        assertTrue(ClassName.isValidClassName("com.bla.Parent$function!@#%^&*,?{}]()$variable$1"));
    }

    @Test
    void testFieldDescriptorClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("Lcom/bla/Parent;"));
    }

    @Test
    void testFieldDescriptorOneDimensionalArrayClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[Lcom/bla/Parent;"));
    }

    @Test
    void testFieldDescriptorOneDimensionalPrimitiveArrayClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[I"));
    }

    @Test
    void testFieldDescriptorTwoDimensionalArrayClassNameIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[[Lcom/bla/Parent;"));
    }

    @Test
    void testFieldDescriptorClassNameContainingAllowedSpecialCharactersIsValidClassName() {
        assertTrue(ClassName.isValidClassName("[[Lcom/bla/Parent$function!@#%^&*,?{}]()$variable$1;"));
    }

    @Test
    void testFieldDescriptorClassNameContainingDotsIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName("Lcom.bla.Parent;"));
    }

    @Test
    void testClassNameContainingBothSlashesAndDotsIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName("com.bla/Parent"));
    }

    @Test
    void testBinaryClassNameContainingDisallowedSpecialCharactersIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName("com/bla/Parent$function$variable$1;"));
        assertFalse(ClassName.isValidClassName("com/bla/Parent$function;$variable$1"));
        assertFalse(ClassName.isValidClassName("com/bla/Parent$function[$variable$1"));
    }

    @Test
    void testEmptyStringIsInvalidClassName() {
        assertFalse(ClassName.isValidClassName(("")));
    }
}
