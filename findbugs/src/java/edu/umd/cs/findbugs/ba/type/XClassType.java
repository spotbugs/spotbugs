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

import edu.umd.cs.findbugs.ba.Debug;
import org.apache.bcel.Constants;

/**
 * Type of objects that are instances of a class.
 * This includes all class and interface types,
 * but excludes array types.
 */
public class XClassType extends XObjectType {
	private static final int CLASS_OR_INTERFACE_KNOWN = 1;
	private static final int IS_INTERFACE = 2;

	private String className;
	private int flags;

	XClassType(String typeSignature) throws InvalidSignatureException {
		super(typeSignature);
		if (!(typeSignature.startsWith("L") && typeSignature.endsWith(";")))
			throw new InvalidSignatureException("Bad type signature for class/interface: " + typeSignature);
	}

	public void setIsInterface() throws UnknownTypeException {
		if ((flags & CLASS_OR_INTERFACE_KNOWN) != 0 && !isInterface())
			throw new UnknownTypeException("Class " + getClassName() + " registered as both class and interface");
		flags |= CLASS_OR_INTERFACE_KNOWN;
		flags |= IS_INTERFACE;
	}

	public void setIsClass() throws UnknownTypeException {
		if ((flags & CLASS_OR_INTERFACE_KNOWN) != 0 && isInterface())
			throw new UnknownTypeException("Class " + getClassName() + " registered as both class and interface");
		flags |= CLASS_OR_INTERFACE_KNOWN;
		flags &= ~(IS_INTERFACE);
	}

	public int getTypeCode() {
		return Constants.T_OBJECT;
	}

	public String getClassName() {
		// Note: According to JSR-133 (which will be the official JMM
		// as of JDK 1.5), String objects can be safely passed via data races,
		// so this code is MT-safe.  (Threads calling this method may
		// get different String objects returned, but the values of
		// those String objects will be correct.)
		if (className == null) {
			String typeSignature = getSignature();
			typeSignature = typeSignature.substring(1, typeSignature.length() - 1);
			className = typeSignature.replace('/', '.');
		}
		return className;
	}

	public void accept(XTypeVisitor visitor) {
		visitor.visitXClassType(this);
	}

	public boolean equals(Object o) {
		if (o.getClass() != this.getClass())
			return false;
		XClassType other = (XClassType) o;
		return this.getSignature().equals(other.getSignature());
	}

	public int hashCode() {
		return getSignature().hashCode();
	}

	public boolean isInterface() throws UnknownTypeException {
		if ((flags & CLASS_OR_INTERFACE_KNOWN) == 0)
			throw new UnknownTypeException("Don't know whether type " + getClassName() +
				" is a class or interface");

		return (flags & IS_INTERFACE) != 0;
	}

	public boolean isArray() {
		return false;
	}
}

// vim:ts=4
