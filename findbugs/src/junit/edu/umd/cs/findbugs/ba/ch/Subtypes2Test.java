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

import java.io.IOException;

import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.FindBugsTestCase;
import edu.umd.cs.findbugs.RunnableWithExceptions;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.detect.FindRefComparison;
import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * Tests for Subtypes2.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class Subtypes2Test extends FindBugsTestCase {

	ObjectType typeSerializable;
	ObjectType typeClonable;
	ObjectType typeObject;
	ObjectType typeInteger;
	ObjectType typeString;
	ArrayType typeArraySerializable;
	ArrayType typeArrayClonable;
	ArrayType typeArrayObject;
	ArrayType typeArrayInteger;
	ArrayType typeArrayString;
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

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		typeSerializable = ObjectTypeFactory.getInstance("java.io.Serializable");
		typeClonable = ObjectTypeFactory.getInstance("java.lang.Cloneable");
		typeObject = ObjectTypeFactory.getInstance("java.lang.Object");
		typeInteger = ObjectTypeFactory.getInstance("java.lang.Integer");
		typeString = ObjectTypeFactory.getInstance("java.lang.String");
		typeArraySerializable = new ArrayType(typeSerializable,1);
		typeArrayClonable = new ArrayType(typeClonable,1);
		typeArrayObject = new ArrayType(typeObject,1);
		typeArrayInteger = new ArrayType(typeInteger,1);
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
		try {
			return Global.getAnalysisCache().getDatabase(Subtypes2.class);
		} catch (CheckedAnalysisException e) {
			throw new IllegalStateException();
		}
	}

	public void testStringSubtypeOfObject() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeString, typeObject));
			}
		});
	}

	public void testStringSubtypeOfSerializable() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeString, typeSerializable));
			}
		});
	}

	public void testIdentitySubtype() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeObject, typeObject));
				assertTrue(test.isSubtype(typeSerializable, typeSerializable));
				assertTrue(test.isSubtype(typeArrayClonable, typeArrayClonable));
			}
		});
	}

	public void testInterfaceIsSubtypeOfObject() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions() {
			public void run() throws ClassNotFoundException {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeClonable, typeObject));
			}
		});
	}

	public void testArrays() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeArrayClonable, typeObject));
				assertTrue(test.isSubtype(typeArrayClonable, typeArrayObject));
			}
		});
	}

	public void testUnrelatedTypes() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertFalse(test.isSubtype(typeInteger, typeString));
			}
		});
	}

	public void testArraysWrongDimension() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertFalse(test.isSubtype(typeArrayArrayString, typeArrayString));
			}
		});
	}

	public void testMultidimensionalArrayIsSubtypeOfObjectArray() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeArrayArrayString, typeArrayObject));
				assertTrue(test.isSubtype(typeArrayArraySerializable, typeArrayObject));
				assertTrue(test.isSubtype(typeArrayArrayInt, typeArrayObject));
			}
		});
	}

	public void testArrayOfPrimitiveIsSubtypeOfObject() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Exception {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeArrayInt, typeObject));
			}
		});
	}

	public void testSpecialStringSubclasses() throws Exception {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Exception {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeDynamicString, typeString));
				assertTrue(test.isSubtype(typeStaticString, typeString));
				assertTrue(test.isSubtype(typeParameterString, typeString));
			}
		});
	}

	public void testEasyFirstCommonSuperclass() throws Exception {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertEquals(typeObject, test.getFirstCommonSuperclass(typeObject, typeObject));
				assertEquals(typeString, test.getFirstCommonSuperclass(typeString, typeString));
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeString, typeObject));
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeObject, typeString));

				// Slightly harder one
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeString, typeInteger));
			}
		});
	}

	public void testInterfaceFirstCommonSuperclass() throws Exception {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();
				
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeSerializable, typeObject));
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeObject, typeSerializable));
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeSerializable, typeClonable));
				
				assertEquals(typeSerializable, test.getFirstCommonSuperclass(typeSerializable, typeSerializable));
			}
		});
	}
	
	public void testArrayFirstCommonSuperclass() throws Exception {
		executeFindBugsTest(new RunnableWithExceptions(){
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayInteger, typeObject));
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeSerializable, typeArrayClonable));
				
				assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayString, typeArrayInteger));
				
				assertEquals(typeArrayInt, test.getFirstCommonSuperclass(typeArrayInt, typeArrayInt));
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayChar, typeArrayInt));
				
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayString, typeArrayInt));
				
				assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayArraySerializable, typeArrayString));
				
				assertEquals(typeObject, test.getFirstCommonSuperclass(typeArrayArrayString, typeArrayInt));
			}
		});
	}
	
	public void testArrayFirstCommonSuperclassTricky() throws Exception {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();
				
				assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayArrayInt, typeArrayArrayChar));
				assertEquals(typeArrayObject, test.getFirstCommonSuperclass(typeArrayArrayInt, typeArrayArrayArrayChar));
				assertEquals(typeArrayArrayObject, test.getFirstCommonSuperclass(typeArrayArrayArrayChar, typeArrayArrayArrayInt));

				// Sanity check
				assertEquals(typeArrayArrayArrayChar, test.getFirstCommonSuperclass(typeArrayArrayArrayChar, typeArrayArrayArrayChar));
			}
		});
	}
}
