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

package edu.umd.cs.findbugs.ba.type;

import org.apache.bcel.Constants;

public class XArrayType extends XObjectType {
	private static final String brackets =
		"[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[";

	private int numDimensions;
	private XType baseType;

	private static String makeArraySignature(int numDimensions, XType baseType) {
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

	XArrayType(int numDimensions, XType baseType) {
		super(makeArraySignature(numDimensions, baseType));
		this.numDimensions = numDimensions;
		this.baseType = baseType;
	}

	static XArrayType createFromSignature(XTypeRepository repos, String signature) throws InvalidSignatureException {
		int numDimensions = 0;
		while (numDimensions < signature.length()) {
			if (signature.charAt(numDimensions) != '[')
				break;
			++numDimensions;
		}
		if (numDimensions == 0 || numDimensions == signature.length())
			throw new InvalidSignatureException("Bad array signature: " + signature);
		XType baseType = repos.createFromSignature(signature.substring(numDimensions));
		return new XArrayType(numDimensions, baseType);
	}

	public int getNumDimensions() {
		return numDimensions;
	}

	public XType getBaseType() {
		return baseType;
	}

	// TODO: getElementType()?
	// Would require a lookup in the type repository.

	public int getTypeCode() {
		return Constants.T_ARRAY;
	}

	public void accept(XTypeVisitor visitor) {
		visitor.visitXArrayType(this);
	}

	public boolean isInterface() {
		return false;
	}

	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o.getClass() != this.getClass())
			return false;
		XArrayType other = (XArrayType) o;
		return this.numDimensions == other.numDimensions
			&& this.baseType.equals(other.baseType);
	}

	public int hashCode() {
		return baseType.hashCode() + numDimensions;
	}
}

// vim:ts=4
