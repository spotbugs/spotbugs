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

public class InstanceField implements Comparable<InstanceField> {
	private String className;
	private String fieldName;
	private String fieldSig;

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

	public int compareTo(InstanceField other) {
		int cmp;
		cmp = className.compareTo(other.className);
		if (cmp != 0)
			return cmp;
		cmp = fieldName.compareTo(other.fieldName);
		if (cmp != 0)
			return cmp;
		return fieldSig.compareTo(other.fieldSig);
	}
}

// vim:ts=4
