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

import org.apache.bcel.Constants;

public class ArrayType extends ObjectType {
	private static final String brackets =
	        "[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[";

	private int numDimensions;
	private Type baseType;

	public static String makeArraySignature(int numDimensions, Type baseType) {
		if (!baseType.isValidArrayBaseType())
			throw new IllegalArgumentException("Illegal request to use array type " +
			        baseType.getSignature() + " as base type of array");
		StringBuffer buf = new StringBuffer();
		if (numDimensions <= brackets.length()) {
			buf.append(brackets.substring(0, numDimensions));
		} else {
			for (int i = 0; i < numDimensions; ++i)
				buf.append('[');
		}
		buf.append(baseType.getSignature());

		return buf.toString();
	}

	ArrayType(int numDimensions, Type baseType) {
		super(makeArraySignature(numDimensions, baseType));
		this.numDimensions = numDimensions;
		this.baseType = baseType;
	}

	ArrayType(String signature, int numDimensions, Type baseType) {
		super(signature);
		this.numDimensions = numDimensions;
		this.baseType = baseType;
	}

	static ArrayType typeFromSignature(TypeRepository repos, String signature) throws InvalidSignatureException {
		int numDimensions = 0;
		while (numDimensions < signature.length()) {
			if (signature.charAt(numDimensions) != '[')
				break;
			++numDimensions;
		}
		if (numDimensions == 0 || numDimensions == signature.length())
			throw new InvalidSignatureException("Bad array signature: " + signature);
		Type baseType = repos.typeFromSignature(signature.substring(numDimensions));
		return new ArrayType(numDimensions, baseType);
	}

	public int getNumDimensions() {
		return numDimensions;
	}

	public Type getBaseType() {
		return baseType;
	}

	public Type getElementType(TypeRepository repos) {
		if (numDimensions == 1)
			return baseType;
		else
			return repos.arrayTypeFromDimensionsAndBaseType(numDimensions - 1, baseType);
	}

	public int getTypeCode() {
		return Constants.T_ARRAY;
	}

	public boolean isValidArrayBaseType() {
		return false;
	}

	public void accept(TypeVisitor visitor) {
		visitor.visitArrayType(this);
	}

	@Override
         public boolean isInterface() {
		return false;
	}

	@Override
         public boolean isArray() {
		return true;
	}

	@Override
         public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o.getClass() != this.getClass())
			return false;
		ArrayType other = (ArrayType) o;
		return this.numDimensions == other.numDimensions
		        && this.baseType.equals(other.baseType);
	}

	@Override
         public int hashCode() {
		return baseType.hashCode() + numDimensions;
	}

	@Override
         public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(baseType.toString());
		for (int i = 0; i < numDimensions; ++i)
			buf.append("[]");
		return buf.toString();
	}
}

// vim:ts=4
