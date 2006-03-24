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
 * Type of objects that are instances of a class.
 * This includes all class and interface types,
 * but excludes array types.
 */
public class ClassType extends ObjectType {

	private String className;
	private boolean isInterface;
	private ClassNotFoundException resolverFailure;

	ClassType(String typeSignature) {
		super(typeSignature);
	}

	void setResolverFailure(ClassNotFoundException e) {
		this.resolverFailure = e;
	}

	ClassNotFoundException getResolverFailure() {
		return resolverFailure;
	}

	/**
	 * Mark the type as an interface.
	 * The user is responsible for ensuring that
	 * a class type is not marked as both a class and and
	 * interface, and that the type is marked as one
	 * or the other before isInterface() is called.
	 */
	public void setIsInterface(boolean isInterface) {
		if (getState() == KNOWN && isInterface() != isInterface)
			throw new IllegalStateException("Type " + getClassName() +
			        " marked as both class and interface");
		setState(KNOWN);
		this.isInterface = isInterface;
	}

	/**
	 * Mark the type as unknown: a check to determine
	 * whether it was a class or interface failed.
	 */
	public void setUnknown() {
		setState(UNKNOWN);
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

	public boolean isValidArrayBaseType() {
		return true;
	}

	public void accept(TypeVisitor visitor) {
		visitor.visitClassType(this);
	}

	@Override
         public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass())
			return false;
		ClassType other = (ClassType) o;
		return this.getSignature().equals(other.getSignature());
	}

	@Override
         public int hashCode() {
		return getSignature().hashCode();
	}

	@Override
         public boolean isInterface() {
		if (getState() != KNOWN)
			throw new IllegalStateException("Don't know whether type " + getClassName() +
			        " is a class or interface");

		return isInterface;
	}

	@Override
         public boolean isArray() {
		return false;
	}

	@Override
         public String toString() {
		return getClassName();
	}
}

// vim:ts=4
