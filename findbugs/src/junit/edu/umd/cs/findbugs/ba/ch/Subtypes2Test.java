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

import edu.umd.cs.findbugs.FindBugsTestCase;
import edu.umd.cs.findbugs.RunnableWithExceptions;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
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
	ArrayType typeArraySerializable;
	ArrayType typeArrayClonable;
	ArrayType typeArrayObject;
	ArrayType typeArrayArraySerializable;
	ArrayType typeArrayArrayClonable;
	ArrayType typeArrayArrayObject;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		typeSerializable = ObjectTypeFactory.getInstance("java.io.Serializable");
		typeClonable = ObjectTypeFactory.getInstance("java.lang.Cloneable");
		typeObject = ObjectTypeFactory.getInstance("java.lang.Object");
		typeArraySerializable = new ArrayType(typeSerializable,1);
		typeArrayClonable = new ArrayType(typeClonable,1);
		typeArrayObject = new ArrayType(typeObject,1);
		typeArrayArraySerializable = new ArrayType(typeSerializable,1);
		typeArrayArrayClonable = new ArrayType(typeClonable,1);
		typeArrayArrayObject = new ArrayType(typeObject,1);
	}
	
	private static Subtypes2 getSubtypes2() {
		try {
			return Global.getAnalysisCache().getDatabase(Subtypes2.class);
		} catch (CheckedAnalysisException e) {
			throw new IllegalStateException();
		}
	}

	public void testSelfSubtype() throws Throwable {
		executeFindBugsTest(new RunnableWithExceptions() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.RunnableWithExceptions#run()
			 */
			public void run() throws Throwable {
				Subtypes2 test = getSubtypes2();

				assertTrue(test.isSubtype(typeObject, typeObject));
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
}
