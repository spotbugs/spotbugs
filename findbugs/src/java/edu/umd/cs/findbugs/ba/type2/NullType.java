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

import edu.umd.cs.findbugs.ba.type.ExtendedTypes;

/**
 * The type of a null value.
 *
 * @author David Hovemeyer
 */
public class NullType implements ReferenceType {
	NullType() {
	}

	public String getSignature() {
		return SpecialTypeSignatures.NULL_TYPE_SIGNATURE;
	}

	public int getTypeCode() {
		return ExtendedTypes.T_NULL;
	}

	public boolean isBasicType() {
		return false;
	}

	public boolean isReferenceType() {
		return true;
	}

	// The null type can't be used as an array element type.
	public boolean isValidArrayElementType() {
		return false;
	}

	public boolean isValidArrayBaseType() {
		return false;
	}

	public void accept(TypeVisitor visitor) {
		visitor.visitNullType(this);
	}

	@Override
         public boolean equals(Object o) {
		if (o == null) return false;
		return this.getClass() == o.getClass();
	}

	@Override
         public int hashCode() {
		return NullType.class.getName().hashCode();
	}
}

// vim:ts=4
