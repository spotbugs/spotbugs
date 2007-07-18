/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.util.DualKeyHashMap;
import edu.umd.cs.findbugs.util.Util;

/**
 * @author pugh
 */
public class TypeQualifierValue {
	public final ClassDescriptor typeQualifier;
	public final @CheckForNull Object value;
	
	private TypeQualifierValue(ClassDescriptor typeQualifier, @CheckForNull Object value) {
		
		this.typeQualifier =  typeQualifier;
		this.value = value;
	}
	
	private static DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> map = new DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> ();
	
	public static synchronized  @NonNull TypeQualifierValue getValue(ClassDescriptor desc, Object value) {
		TypeQualifierValue result = map.get(desc, value);
		if (result != null) return result;
		result = new TypeQualifierValue(desc, value);
		map.put(desc, value, result);
		return result;
	}
	
	public int hashCode() {
		int result = typeQualifier.hashCode();
		if (value != null) result += 37*value.hashCode();
		return result;
	}
	
	public boolean equals(Object o) {
		if (!(o instanceof TypeQualifierValue)) return false;
		TypeQualifierValue other = (TypeQualifierValue) o;
		return typeQualifier.equals(other.typeQualifier) && Util.nullSafeEquals(value, other.value);
	}
	

}
