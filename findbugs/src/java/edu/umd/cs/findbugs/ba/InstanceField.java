/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

public class InstanceField implements XField {
	private String className;
	private String fieldName;
	private String fieldSig;
	private int cachedHashCode = 0;

	public InstanceField(String className, String fieldName, String fieldSig) {
		this.className = className;
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
	}

	public String getClassName() {
		return className;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getFieldSignature() {
		return fieldSig;
	}

	public boolean isStatic() {
		return false;
	}

	public int compareTo(XField other) {
		// This may be compared to any kind of XField object.
		// If the other object is a different kind of field,
		// just compare class names.
		if (this.getClass() != other.getClass())
			return this.getClass().getName().compareTo(other.getClass().getName());

		int cmp;
		cmp = className.compareTo(other.getClassName());
		if (cmp != 0)
			return cmp;
		cmp = fieldName.compareTo(other.getFieldName());
		if (cmp != 0)
			return cmp;
		return fieldSig.compareTo(other.getFieldSignature());
	}

	public int hashCode() {
		if (cachedHashCode == 0) {
			cachedHashCode = className.hashCode() ^ fieldName.hashCode() ^ fieldSig.hashCode();
		}
		return cachedHashCode;
	}

	public boolean equals(Object o) {
		if (this.getClass() != o.getClass())
			return false;
		InstanceField other = (InstanceField) o;
		return className.equals(other.className)
			&& fieldName.equals(other.fieldName)
			&& fieldSig.equals(other.fieldSig);
	}

	public String toString() {
		return className + "." + fieldName;
	}
}

// vim:ts=4
