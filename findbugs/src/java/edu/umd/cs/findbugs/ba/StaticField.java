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

public class StaticField implements XField {
	private String className;
	private String fieldName;
	private String fieldSig;

	public StaticField(String className, String fieldName, String fieldSig) {
		this.className = className;
		this.fieldName = fieldName;
		this.fieldSig = fieldSig;
	}

	public String getFieldName() {
		return fieldName;
	}

	public String getClassName() {
		return className;
	}

	public String getFieldSignature() {
		return fieldSig;
	}

	public boolean isStatic() {
		return true;
	}

	public int compareTo(XField other) {
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
}

// vim:ts=4
