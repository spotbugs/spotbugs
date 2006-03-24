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

/**
 * Special return address type.
 * This is the type of the value pushed onto the stack
 * by a JSR instruction.
 *
 * @author David Hovemeyer
 */
public class ReturnAddressType implements Type {
	ReturnAddressType() {
	}

	public String getSignature() {
		return SpecialTypeSignatures.RETURN_ADDRESS_TYPE_SIGNATURE;
	}

	public boolean isBasicType() {
		return false;
	}

	public boolean isReferenceType() {
		return false;
	}

	public boolean isValidArrayElementType() {
		return false;
	}

	public boolean isValidArrayBaseType() {
		return false;
	}

	public int getTypeCode() {
		return Constants.T_ADDRESS;
	}

	public void accept(TypeVisitor visitor) {
		visitor.visitReturnAddressType(this);
	}

	@Override
         public boolean equals(Object o) {
		if (o == null) return false;
		return this.getClass() == o.getClass();
	}

	@Override
         public int hashCode() {
		return ReturnAddressType.class.getName().hashCode();
	}
}

// vim:ts=4
