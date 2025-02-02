/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.ch;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.FindBugsTestCase;
import edu.umd.cs.findbugs.RunnableWithExceptions;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.detect.FindRefComparison;
import edu.umd.cs.findbugs.util.Values;

/**
 * Tests for Subtypes2.
 *
 * @author Bill Pugh
 * @author David Hovemeyer
 */
class Subtypes2Test extends FindBugsTestCase {

    ObjectType typeSerializable;

    ObjectType typeClonable;

    ObjectType typeObject;

    ObjectType typeInteger;

    ObjectType typeString;

    ObjectType typeList;

    ObjectType typeCollection;

    ObjectType typeHashSet;

    ArrayType typeArrayClonable;

    ObjectType typeComparable;

    ArrayType typeArrayObject;

    ArrayType typeArrayInteger;

    ArrayType typeArrayString;

    ArrayType typeArrayComparable;

    ArrayType typeArrayArrayObject;

    ArrayType typeArrayArraySerializable;

    ArrayType typeArrayArrayString;

    ArrayType typeArrayInt;

    ArrayType typeArrayArrayInt;

    ArrayType typeArrayArrayArrayInt;

    ArrayType typeArrayChar;

    ArrayType typeArrayArrayChar;

    ArrayType typeArrayArrayArrayChar;

    ObjectType typeDynamicString;

    ObjectType typeStaticString;

    ObjectType typeParameterString;

    @BeforeEach
    void setUp() {
        typeSerializable = ObjectTypeFactory.getInstance("java.io.Serializable");
        typeClonable = ObjectTypeFactory.getInstance("java.lang.Cloneable");
        typeObject = ObjectTypeFactory.getInstance(Values.DOTTED_JAVA_LANG_OBJECT);
        typeInteger = ObjectTypeFactory.getInstance("java.lang.Integer");
        typeString = ObjectTypeFactory.getInstance(Values.DOTTED_JAVA_LANG_STRING);
        typeComparable = ObjectTypeFactory.getInstance("java.lang.Comparable");

        typeList = ObjectTypeFactory.getInstance("java.util.List");
        typeCollection = ObjectTypeFactory.getInstance("java.util.Collection");
        typeHashSet = ObjectTypeFactory.getInstance("java.util.HashSet");
        typeArrayClonable = new ArrayType(typeClonable, 1);
        typeArrayComparable = new ArrayType(typeComparable, 1);
        typeArrayObject = new ArrayType(typeObject, 1);
        typeArrayInteger = new ArrayType(typeInteger, 1);
        typeArrayString = new ArrayType(typeString, 1);
        typeArrayArrayObject = new ArrayType(typeObject, 2);
        typeArrayArraySerializable = new ArrayType(typeSerializable, 2);
        typeArrayArrayString = new ArrayType(typeString, 2);
        typeArrayInt = new ArrayType(Type.INT, 1);
        typeArrayArrayInt = new ArrayType(Type.INT, 2);
        typeArrayArrayArrayInt = new ArrayType(Type.INT, 3);
        typeArrayChar = new ArrayType(Type.CHAR, 1);
        typeArrayArrayChar = new ArrayType(Type.CHAR, 2);
        typeArrayArrayArrayChar = new ArrayType(Type.CHAR, 3);
        typeDynamicString = new FindRefComparison.DynamicStringType();
        typeStaticString = new FindRefComparison.StaticStringType();
        typeParameterString = new FindRefComparison.ParameterStringType();
    }

    private static Subtypes2 getSubtypes2() {
        return Global.getAnalysisCache().getDatabase(Subtypes2.class);
    }

    @Test
    void testStringSubtypeOfObject() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeString, typeObject));
            }
        });
    }

    @Test
    void testStringSubtypeOfSerializable() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeString, typeSerializable));
            }
        });
    }

    @Test
    void testIdentitySubtype() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeObject, typeObject));
                Assertions.assertTrue(test.isSubtype(typeSerializable, typeSerializable));
                Assertions.assertTrue(test.isSubtype(typeArrayClonable, typeArrayClonable));
            }
        });
    }

    @Test
    void testInterfaceIsSubtypeOfObject() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            @Override
            public void run() throws ClassNotFoundException {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeClonable, typeObject));
            }
        });
    }

    @Test
    void testArrays() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeArrayClonable, typeObject));
                Assertions.assertTrue(test.isSubtype(typeArrayClonable, typeArrayObject));
            }
        });
    }

    @Test
    void testUnrelatedTypes() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertFalse(test.isSubtype(typeInteger, typeString));
            }
        });
    }

    @Test
    void testArraysWrongDimension() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertFalse(test.isSubtype(typeArrayArrayString, typeArrayString));
            }
        });
    }

    @Test
    void testMultidimensionalArrayIsSubtypeOfObjectArray() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeArrayArrayString, typeArrayObject));
                Assertions.assertTrue(test.isSubtype(typeArrayArraySerializable, typeArrayObject));
                Assertions.assertTrue(test.isSubtype(typeArrayArrayInt, typeArrayObject));
            }
        });
    }

    @Test
    void testArrayOfPrimitiveIsSubtypeOfObject() throws Throwable {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Exception {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeArrayInt, typeObject));
            }
        });
    }

    @Test
    void testSpecialStringSubclasses() throws Exception {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Exception {
                Subtypes2 test = getSubtypes2();

                Assertions.assertTrue(test.isSubtype(typeDynamicString, typeString));
                Assertions.assertTrue(test.isSubtype(typeStaticString, typeString));
                Assertions.assertTrue(test.isSubtype(typeParameterString, typeString));
            }
        });
    }

    @Test
    void testEasyFirstCommonSuperclass() throws Exception {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeObject, typeObject));
                Assertions.assertEquals(typeString, test.getFirstCommonSuperclass(typeString, typeString));
                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeString, typeObject));
                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeObject, typeString));

                // Slightly harder one
                Assertions.assertEquals(typeComparable, test.getFirstCommonSuperclass(typeString, typeInteger));
            }
        });
    }

    @Test
    void testInterfaceFirstCommonSuperclass() throws Exception {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeSerializable, typeObject));
                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeObject, typeSerializable));
                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeSerializable, typeClonable));

                Assertions.assertEquals(typeSerializable, test.getFirstCommonSuperclass(typeSerializable, typeSerializable));
            }
        });
    }

    @Test
    void testArrayFirstCommonSuperclass() throws Exception {
        executeFindBugsTest(new RunnableWithExceptions() {
            /*
             * (non-Javadoc)
             *
             * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
             */
            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayInteger, typeObject));
                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeSerializable, typeArrayClonable));

                Assertions.assertEquals(typeArrayComparable, test.getFirstCommonSuperclass(typeArrayString, typeArrayInteger));

                Assertions.assertEquals(typeArrayInt, test.getFirstCommonSuperclass(typeArrayInt, typeArrayInt));
                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayChar, typeArrayInt));

                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayString, typeArrayInt));

                Assertions.assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayArraySerializable, typeArrayString));

                Assertions.assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayArrayString, typeArrayInt));
            }
        });
    }

    @Test
    void testArrayFirstCommonSuperclassTricky() throws Exception {
        executeFindBugsTest(new RunnableWithExceptions() {

            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();

                Assertions.assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayArrayInt, typeArrayArrayChar));
                Assertions.assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayArrayInt, typeArrayArrayArrayChar));
                Assertions.assertEquals(typeArrayArrayObject, test.getFirstCommonSuperclass(typeArrayArrayArrayChar, typeArrayArrayArrayInt));

                // Sanity check
                Assertions.assertEquals(typeArrayArrayArrayChar,
                        test.getFirstCommonSuperclass(typeArrayArrayArrayChar, typeArrayArrayArrayChar));
            }
        });
    }

    @Test
    void testInterfaces() throws Exception {
        executeFindBugsTest(new RunnableWithExceptions() {

            @Override
            public void run() throws Throwable {
                Subtypes2 test = getSubtypes2();
                Assertions.assertEquals(typeCollection, test.getFirstCommonSuperclass(typeCollection, typeHashSet));
                Assertions.assertEquals(typeCollection, test.getFirstCommonSuperclass(typeHashSet, typeCollection));
                Assertions.assertEquals(typeCollection, test.getFirstCommonSuperclass(typeList, typeHashSet));

            }
        });
        /*
         * ObjectType typeList; ObjectType typeMap; ObjectType typeCollection;
         * ObjectType typeHashSet;
         */

    }
}
