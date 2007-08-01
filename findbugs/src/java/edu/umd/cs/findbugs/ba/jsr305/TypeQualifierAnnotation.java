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

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * @author pwilliam
 */
public class TypeQualifierAnnotation {
	
	public final TypeQualifierValue typeQualifier;
	public final When when;
	
	private TypeQualifierAnnotation(TypeQualifierValue typeQualifier, When when) {
		this.typeQualifier =  typeQualifier;
		this.when = when;
	}
	
	private static DualKeyHashMap <TypeQualifierValue, When, TypeQualifierAnnotation> map = new DualKeyHashMap <TypeQualifierValue, When, TypeQualifierAnnotation> ();
	
//	public static synchronized  @NonNull TypeQualifierAnnotation getValue(ClassDescriptor desc, Object value, When when) {
//		return getValue(TypeQualifierValue.getValue(desc, value), when);
//	}
		
	public static @NonNull Collection<TypeQualifierAnnotation> getValues(Map<TypeQualifierValue, When> map) {
		Collection<TypeQualifierAnnotation> result = new LinkedList<TypeQualifierAnnotation>();
		for(Map.Entry<TypeQualifierValue, When> e : map.entrySet()) {
			result.add(getValue(e.getKey(), e.getValue()));
		}
		return result;
		
	}
	public static synchronized  @NonNull TypeQualifierAnnotation getValue(TypeQualifierValue desc, When when) {
		TypeQualifierAnnotation result = map.get(desc, when);
		if (result != null) return result;
		result = new TypeQualifierAnnotation(desc, when);
		map.put(desc, when, result);
		return result;
	}
	
	public int hashCode() {
		return typeQualifier.hashCode() * 37 + when.hashCode();
	}
	public boolean equals(Object o) {
		if (!(o instanceof TypeQualifierAnnotation)) return false;
		TypeQualifierAnnotation other = (TypeQualifierAnnotation) o;
		return typeQualifier.equals(other.typeQualifier) && when.equals(other.when);
	}
	public String toString() {
		return typeQualifier + ":" + when;
	}
	

}
