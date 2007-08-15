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

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * A type qualifier applied to a field, method, parameter, or return value.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
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

	// When lattice:
	//
	// In subtypes:
	//    - return value type must be at least as narrow as supertypes
	//    - parameter types must be at least as wide as supertypes 
	//
	//            TOP               TOP is invalid as return type:
	//         /   |   \            means that no When value is narrow enough
	//        /    |    \           for combination of supertype annotations
	//       /     |     \
	// Always   Unknown   Never  ^  Narrower
	//       \     |     /       |
	//        \    |    /        |
	//         \   |   /         |
	//           Maybe           v  Wider 
	//
	
	private static final When TOP = null;
	
	private static final When[][] combineReturnValueMatrix = {
		//                   ALWAYS       UNKNOWN        MAYBE            NEVER
		/* ALWAYS */       { When.ALWAYS, },
		/* UNKNOWN */      { When.ALWAYS, When.UNKNOWN, },
		/* MAYBE */        { When.ALWAYS, When.UNKNOWN,  When.MAYBE, },
		/* NEVER */        { TOP,         TOP,           When.NEVER,       When.NEVER },
	};
	
	private static final When[][] combineParameterMatrix = {
		//                   ALWAYS             UNKNOWN         MAYBE            NEVER
		/* ALWAYS */       { When.ALWAYS, },
		/* UNKNOWN */      { When.UNKNOWN,    When.UNKNOWN, },
		/* MAYBE */        { When.MAYBE,      When.MAYBE,          When.MAYBE, },
		/* NEVER */        { When.MAYBE,      When.UNKNOWN,        When.MAYBE,     When.NEVER },
	};

	/**
	 * Combine return type annotations.
	 * 
	 * @param a a TypeQualifierAnnotation used on a return value
	 * @param b another TypeQualifierAnnotation used on a return value 
	 * @return combined return type annotation that is at least as narrow as
	 *         both <code>a</code> or <code>b</code>,
	 *         or null if no such TypeQualifierAnnotation exists
	 */
	public static @CheckForNull TypeQualifierAnnotation combineReturnTypeAnnotations(TypeQualifierAnnotation a, TypeQualifierAnnotation b) {
		return combineAnnotations(a, b, combineReturnValueMatrix);
	}
	
	/**
	 * 
	 * @param a a TypeQualifierAnnotation used on a method parameter
	 * @param b another TypeQualifierAnnotation used on a method parameter  
	 * @return combined parameter annotation that is at least as wide
	 *         as both a and b
	 */
	public static @NonNull TypeQualifierAnnotation combineParameterAnnotations(TypeQualifierAnnotation a, TypeQualifierAnnotation b) {
		return combineAnnotations(a, b, combineParameterMatrix);
	}

	private static TypeQualifierAnnotation combineAnnotations(TypeQualifierAnnotation a, TypeQualifierAnnotation b,
			When[][] mergeMatrix) {
		assert a.typeQualifier.equals(b.typeQualifier);

		When aWhen = a.when;
		When bWhen = b.when;
		if (aWhen.ordinal() < bWhen.ordinal()) {
			When tmp = aWhen;
			aWhen = bWhen;
			bWhen = tmp;
		}

		When combined = mergeMatrix[aWhen.ordinal()][bWhen.ordinal()];
		if (combined != null) {
			return getValue(a.typeQualifier, combined);
		} else {
			return null;
		}
	}
		
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
