/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.classfile;

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Common superclass for FieldDescriptor and MethodDescriptor.
 * 
 * @author David Hovemeyer
 */
public abstract class FieldOrMethodDescriptor implements FieldOrMethodName, Comparable {
	private final @SlashedClassName String slashedClassName;
	private final String name;
	private final String signature;
	private final boolean isStatic;
	private int cachedHashCode;

	public FieldOrMethodDescriptor(@SlashedClassName String slashedClassName, String name, String signature, boolean isStatic) {
		assert slashedClassName.indexOf('.') == -1 : "class name not in VM format: " + slashedClassName;
			
		this.slashedClassName = DescriptorFactory.canonicalizeString(slashedClassName);
		this.name = DescriptorFactory.canonicalizeString(name);
		this.signature = DescriptorFactory.canonicalizeString(signature);
		this.isStatic = isStatic;
	}

	/**
	 * @return Returns the class name
	 */
	public @SlashedClassName String getSlashedClassName() {
		return slashedClassName;
	}
	
	/**
	 * @return a ClassDescriptor for the method's class
	 */
	public ClassDescriptor getClassDescriptor() {
		return DescriptorFactory.createClassDescriptor(slashedClassName);
	}


	/**
	 * @return Returns the method name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return Returns the method signature
	 */
	public String getSignature() {
		return signature;
	}

	/**
	 * @return Returns true if method is static, false if not
	 */
	public boolean isStatic() {
		return isStatic;
	}

	
	protected int compareTo(FieldOrMethodName o) {
		int cmp;
		cmp = this.getClassDescriptor().compareTo(o.getClassDescriptor());
		if (cmp != 0) {
			return cmp;
		}
		cmp = this.name.compareTo(o.getName());
		if (cmp != 0) {
			return cmp;
		}
		cmp = this.signature.compareTo(o.getSignature());
		if (cmp != 0) {
			return cmp;
		}
		return (this.isStatic ? 1 : 0) - (o.isStatic() ? 1 : 0);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj == null || !(obj instanceof FieldOrMethodDescriptor)) {
			return false;
		}
		FieldOrMethodDescriptor other = (FieldOrMethodDescriptor) obj;
		return this.slashedClassName.equals(other.slashedClassName)
			&& this.name.equals(other.name)
			&& this.signature.equals(other.signature)
			&& this.isStatic == other.isStatic;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public final int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = slashedClassName.hashCode() * 7919
				+ name.hashCode() * 3119  
				+ signature.hashCode() * 131
				+ (isStatic ? 1 : 0);
		}
		return cachedHashCode;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// FIXME: format more nicely
		return (isStatic ? "static " : "") + getClassDescriptor().getDottedClassName() + "." + name + ":" + signature;
	}
	
	public int compareTo(Object o) {
		if (!(o instanceof FieldOrMethodDescriptor)) throw new IllegalArgumentException("Can't compare " + this.getClass().getName() + " to a " + o.getClass().getName());
		FieldOrMethodDescriptor that = (FieldOrMethodDescriptor) o;
		int result =  this.slashedClassName.compareTo(that.slashedClassName);
		if (result != 0) return result;
		result = this.name.compareTo(that.name);
		if (result != 0) return result;
		result = this.signature.compareTo(that.signature);
		if (result != 0) return result;
		result = (this.isStatic ? 1 : 0) - (that.isStatic ? 1 : 0);
		return result;
	}
}
