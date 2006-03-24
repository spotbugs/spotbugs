/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type2;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.bcel.Constants;

/**
 * JUnit tests for ClassType class.
 */
public class ClassTypeTest extends TestCase {
	private static final String FOO_SIG = "Lcom/foobar/Foo;";

	private ClassType fooType;

	@Override
         protected void setUp() {
		fooType = new ClassType(FOO_SIG);
	}

	public void testGetSignature() {
		Assert.assertTrue(fooType.getSignature().equals(FOO_SIG));
	}

	public void testGetClassName() {
		Assert.assertTrue(fooType.getClassName().equals("com.foobar.Foo"));
	}

	public void testEquals() throws InvalidSignatureException {
		ClassType other = new ClassType(FOO_SIG);
		Assert.assertTrue(other.equals(fooType));
	}

	public void testTypeCode() {
		Assert.assertTrue(fooType.getTypeCode() == Constants.T_OBJECT);
	}

	public void testIsReferenceType() {
		Assert.assertTrue(fooType.isReferenceType());
	}

	public void testIsNotBasicType() {
		Assert.assertFalse(fooType.isBasicType());
	}

	public void testIsArrayElement() {
		Assert.assertTrue(fooType.isValidArrayElementType());
	}
}

// vim:ts=4
