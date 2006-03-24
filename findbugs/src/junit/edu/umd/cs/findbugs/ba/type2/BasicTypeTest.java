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
 * JUnit tests for BasicType class.
 */
public class BasicTypeTest extends TestCase {
	private BasicType booleanType;
	private BasicType byteType;
	private BasicType charType;
	private BasicType shortType;
	private BasicType intType;
	private BasicType longType;
	private BasicType floatType;
	private BasicType doubleType;
	private BasicType voidType;

	@Override
         protected void setUp() {
		booleanType = new BasicType(Constants.T_BOOLEAN);
		byteType = new BasicType(Constants.T_BYTE);
		charType = new BasicType(Constants.T_CHAR);
		shortType = new BasicType(Constants.T_SHORT);
		intType = new BasicType(Constants.T_INT);
		longType = new BasicType(Constants.T_LONG);
		floatType = new BasicType(Constants.T_FLOAT);
		doubleType = new BasicType(Constants.T_DOUBLE);
		voidType = new BasicType(Constants.T_VOID);
	}

	public void testSignatures() {
		Assert.assertEquals(booleanType.getSignature(), "Z");
		Assert.assertEquals(byteType.getSignature(), "B");
		Assert.assertEquals(charType.getSignature(), "C");
		Assert.assertEquals(shortType.getSignature(), "S");
		Assert.assertEquals(intType.getSignature(), "I");
		Assert.assertEquals(longType.getSignature(), "J");
		Assert.assertEquals(floatType.getSignature(), "F");
		Assert.assertEquals(doubleType.getSignature(), "D");
		Assert.assertEquals(voidType.getSignature(), "V");
	}

	public void testNotReferenceType() {
		Assert.assertFalse(booleanType.isReferenceType());
	}

	public void testIsBasicType() {
		Assert.assertTrue(booleanType.isBasicType());
	}

	public void testIsValidArrayElementType() {
		Assert.assertTrue(booleanType.isValidArrayElementType());
		// Void is not a valid array element type
		Assert.assertFalse(voidType.isValidArrayElementType());
	}

	public void testEquals() {
		BasicType other = new BasicType(Constants.T_BOOLEAN);
		Assert.assertTrue(booleanType.equals(other));
		Assert.assertFalse(booleanType.equals(voidType));
	}
}

// vim:ts=4
