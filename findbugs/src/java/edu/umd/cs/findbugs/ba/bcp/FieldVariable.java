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

package edu.umd.cs.daveho.ba.bcp;

import edu.umd.cs.daveho.ba.ValueNumber;

public class FieldVariable implements Variable {
	private final ValueNumber ref;
	private final String className;
	private final String fieldName;

	/**
	 * Constructor for static fields.
	 * @param className the class name
	 * @param fieldName the field name
	 */
	public FieldVariable(String className, String fieldName) {
		this(null, className, fieldName);
	}

	/**
	 * Constructor for instance fields.
	 * @param ref ValueNumber of the object reference
	 * @param className the class name
	 * @param fieldName the field name
	 */
	public FieldVariable(ValueNumber ref, String className, String fieldName) {
		this.ref = ref;
		this.className = className;
		this.fieldName = fieldName;
	}

	/**
	 * Return whether or not this is a static field.
	 */
	public boolean isStatic() { return ref == null; }

	public boolean sameAs(Variable other) {
		if (!(other instanceof FieldVariable))
			return false;
		FieldVariable otherField = (FieldVariable) other;
		if (isStatic() != otherField.isStatic())
			return false;
		return (ref == null || ref.equals(otherField.ref))
			&& className.equals(otherField.className)
			&& fieldName.equals(otherField.fieldName);
	}
}

// vim:ts=4
