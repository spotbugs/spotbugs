/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005, University of Maryland
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

import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;

/**
 * Abstract representation of a field.
 * Note that this is called "XField" to distinguish it from
 * BCEL's Field class.  Also, you can think of the "X" as expanding
 * to "Instance" or "Static".
 * 
 * <p> This interface and its implementations exist because Field
 * objects in BCEL are awkward to deal with.  They are not Comparable,
 * it is difficult to find out what class they belong to, etc.</p>
 * 
 * <p>
 * If the resolved() method returns true, then any information queried
 * from this object can be assumed to be accurate.
 * If the resolved() method returns false, then FindBugs can't
 * find the field and any information other than name/signature/etc.
 * cannot be trusted.
 * </p>
 */
public interface XField extends ClassMember, AnnotatedObject{
	/**
	 * Is the type of the field a reference type?
	 */
	public boolean isReferenceType();

	/**
	 * Is this a volatile field?
	 */
	public boolean isVolatile();

	/**
	 * @return FieldDescriptor referring to this field
	 */
	public FieldDescriptor getFieldDescriptor();
}

// vim:ts=4
