/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;

public abstract class AbstractClassMember implements ClassMember {
	private final String className;
	private final String name;
	private final String signature;
	private final int accessFlags;
	private boolean resolved;
	private int cachedHashCode = 0;
	static  int slashCountClass = 0;
	static  int dottedCountClass = 0;
	static  int slashCountSignature= 0;
	static  int dottedCountSignature = 0;


	protected AbstractClassMember(String className, String name, String signature, int accessFlags) {
		if (className.indexOf('.') >= 0) {
			// className = className.replace('.','/');
			dottedCountClass++;
		} else if (className.indexOf('/') >= 0)  {
			assert false;
			slashCountClass++;
			className = className.replace('/','.');
		}
		if (signature.indexOf('.') >= 0) {
			assert false;
			signature = signature.replace('.','/');
			dottedCountSignature++;
		} else if (signature.indexOf('/') >= 0)  slashCountSignature++;
		this.className = className;
		this.name = name;
		this.signature = signature;
		this.accessFlags = accessFlags;
	}

	public String getClassName() {
		return className;
	}

	public String getName() {
		return name;
	}

	public String getPackageName() {
		int lastDot = className.lastIndexOf('.');
		if (lastDot == -1) return className;
		return className.substring(0,lastDot);
	}
	public String getSignature() {
		return signature;
	}

	public boolean isReferenceType() {
		return signature.startsWith("L") || signature.startsWith("[");
	}

	public int getAccessFlags() {
		return accessFlags;
	}

	public boolean isFinal() {
		return (accessFlags & Constants.ACC_FINAL) != 0;
	}

	public boolean isPublic() {
		return (accessFlags & Constants.ACC_PUBLIC) != 0;
	}

	public boolean isProtected() {
		return (accessFlags & Constants.ACC_PROTECTED) != 0;
	}

	public boolean isPrivate() {
		return (accessFlags & Constants.ACC_PRIVATE) != 0;
	}

	public int compareTo(ClassMember other) {
		// This may be compared to any kind of PackageMember object.
		// If the other object is a different kind of field,
		// just compare class names.
		if (this.getClass() != other.getClass())
			return this.getClass().getName().compareTo(other.getClass().getName());

		int cmp;
		cmp = className.compareTo(other.getClassName());
		if (cmp != 0)
			return cmp;
		cmp = name.compareTo(other.getName());
		if (cmp != 0)
			return cmp;
		return signature.compareTo(other.getSignature());
	}

	public boolean isResolved() {
		return resolved;
	}
	  void markAsResolved() {
		 resolved = true;
	 }
	@Override
		 public int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = className.hashCode() ^ name.hashCode() ^ signature.hashCode();
		}
		return cachedHashCode;
	}

	@Override
		 public boolean equals(Object o) {
		if (o == null || this.getClass() != o.getClass())
			return false;
		AbstractClassMember other = (AbstractClassMember) o;
		return className.equals(other.className)
				&& name.equals(other.name)
				&& signature.equals(other.signature);
	}

	@Override
		 public String toString() {
		return className + "." + name;
	}

}
