/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.MissingClassException;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.util.DualKeyHashMap;
import edu.umd.cs.findbugs.util.Util;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * A TypeQualifierValue is a pair specifying a type qualifier annotation
 * and a value.  Each TypeQualifierValue is effectively a different
 * type qualifier.  For example, if Foo is a type qualifier annotation
 * having an int value, then Foo(0), Foo(1), etc. are all
 * different type qualifiers which must be checked separately.
 *
 * @author William Pugh
 */
public class TypeQualifierValue {
	private static final boolean DEBUG = SystemProperties.getBoolean("tqv.debug");
	
	private static final ClassDescriptor EXCLUSIVE_ANNOTATION =
		DescriptorFactory.instance().getClassDescriptor("javax/annotation/meta/Exclusive");
	
	public final ClassDescriptor typeQualifier;
	public final @CheckForNull Object value;
	private boolean isStrict;
	private boolean isExclusive;

	private TypeQualifierValue(ClassDescriptor typeQualifier, @CheckForNull Object value) {
		this.typeQualifier =  typeQualifier;
		this.value = value;
		this.isStrict = false; // will be set to true if this is a strict type qualifier value
		this.isExclusive = false; // will be set to true if this is an exclusive type qualifier value
	}

//	private static DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> map = new DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> ();

	static class Data {
		/**
		 * Cache in which constructed TypeQualifierValues are interned.
		 */
		DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue> typeQualifierMap =
			new DualKeyHashMap <ClassDescriptor, Object, TypeQualifierValue>();
		
		/**
		 * Set of all known TypeQualifierValues.
		 */
		Set<TypeQualifierValue> allKnownTypeQualifiers =
			new HashSet<TypeQualifierValue>();
	}

	private static ThreadLocal<Data> instance = new ThreadLocal<Data>() {
		@Override
		protected Data initialValue() {
			return new Data();
		}
	};
	
	public static void clearInstance() {
		instance.remove();
	}

	/**
	 * Given a ClassDescriptor/value pair, return the
	 * interned TypeQualifierValue representing that pair.
	 *
	 * @param desc  a ClassDescriptor denoting a type qualifier annotation
	 * @param value a value
	 * @return an interned TypeQualifierValue object
	 */
	public static @NonNull TypeQualifierValue getValue(ClassDescriptor desc, Object value) {
		DualKeyHashMap<ClassDescriptor, Object, TypeQualifierValue> map = instance.get().typeQualifierMap;
		TypeQualifierValue result = map.get(desc, value);
		if (result != null) return result;
		result = new TypeQualifierValue(desc, value);
		determineIfQualifierIsStrict(desc, result);
		determineIfQualifierIsExclusive(desc, result);
		map.put(desc, value, result);
		instance.get().allKnownTypeQualifiers.add(result);
		return result;
	}
	
	/**
	 * Get Collection of all known TypeQualifierValues.
	 * 
	 * @return Collection of all known TypeQualifierValues
	 */
	public static Collection<TypeQualifierValue> getAllKnownTypeQualifiers() {
		return Collections.unmodifiableSet(instance.get().allKnownTypeQualifiers);
	}
	
	/**
	 * Get the "complementary" TypeQualifierValues for given exclusive type qualifier.
	 * 
	 * @param tqv a type qualifier (which must be exclusive)
	 * @return Collection of complementary exclusive type qualifiers
	 */
	public static Collection<TypeQualifierValue> getComplementaryExclusiveTypeQualifierValue(TypeQualifierValue tqv) {
		assert tqv.isExclusiveQualifier();
		
		LinkedList<TypeQualifierValue> result = new LinkedList<TypeQualifierValue>();
		
		for (TypeQualifierValue t : instance.get().allKnownTypeQualifiers) {
			//
			// Any TypeQualifierValue with the same
			// annotation class but a different value is a complementary
			// type qualifier.
			//
			if (t.typeQualifier.equals(tqv.typeQualifier) && !t.value.equals(tqv.value)) {
				result.add(t);
			}
		}
		
		return result;
	}

	private static void determineIfQualifierIsStrict(ClassDescriptor desc, TypeQualifierValue result) {
		if (DEBUG) {
			System.out.print("Checking to see if " + desc + " requires strict checking...");
		}
		// Check to see if the type qualifier should be checked strictly
		try {
			XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc);

			// Annotation elements appear as abstract methods in the annotation class (interface).
			// So, if the type qualifier annotation has specified a default When value,
			// it will appear as an abstract method called "when".
			XMethod whenMethod = xclass.findMethod("when", "()Ljavax/annotation/meta/When;", false);
			if (whenMethod == null) {
				result.setIsStrict();
			}
		} catch (MissingClassException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassNotFoundException());
		} catch (CheckedAnalysisException e) {
			AnalysisContext.logError("Error looking up annotation class " + desc.toDottedClassName(), e);
		}
		if (DEBUG) {
			System.out.println(result.isStrictQualifier() ? "yes" : "no");
		}
	}
	
	private static void determineIfQualifierIsExclusive(ClassDescriptor desc, TypeQualifierValue result) {
		if (DEBUG) {
			System.out.print("Checking to see if " + desc + " is exclusive...");
		}
		
		boolean isExclusive = false;
		
		try {
			XClass xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, desc); 

			// If the value() method is annotated as @Exclusive, the type qualifier is exclusive.
			for (XMethod xmethod : xclass.getXMethods()) {
				if (xmethod.getName().equals("value") && xmethod.getSignature().startsWith("()")) {
					isExclusive = xmethod.getAnnotation(EXCLUSIVE_ANNOTATION) != null;
					break;
				}
			}
			
/*
			// If the "exclusive" element is set to true in the type qualifier's
			// @TypeQualifier annotation, then the type qualifier is exclusive.
			// XXX: not supported currently in JSR 305 reference implementation
			if (!isExclusive) {
				AnnotationValue tqv = xclass.getAnnotation(TYPE_QUALIFIER_ANNOTATION);
				assert tqv != null;
				Object exclusiveVal = tqv.getValue("exclusive");
				if (exclusiveVal != null && exclusiveVal instanceof Boolean && exclusiveVal.equals(Boolean.TRUE)) {
					isExclusive = true;
				}
			}
*/
		} catch (MissingClassException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassNotFoundException());
		} catch (CheckedAnalysisException e) {
			AnalysisContext.logError("Error looking up annotation class " + desc.toDottedClassName(), e);
		}
		
		if (isExclusive) {
			result.setIsExclusive();
		}
		
		if (DEBUG) {
			System.out.println(result.isExclusiveQualifier() ? "yes" : "no");
		}
	}

	/**
	 * Get the ClassDescriptor which specifies the type qualifier annotation.
	 *
	 * @return ClassDescriptor which specifies the type qualifier annotation
	 */
	public ClassDescriptor getTypeQualifierClassDescriptor() {
		return typeQualifier;
	}

	/**
	 * Mark this as a type qualifier value that should
	 * be checked strictly.
	 */
	private void setIsStrict() {
		this.isStrict = true;
	}

	/**
	 * Return whether or not this TypeQualifierValue denotes
	 * a strict qualifier.
	 *
	 * @return true if type qualifier is strict, false otherwise
	 */
	public boolean isStrictQualifier() {
		return isStrict;
	}
	
    private void setIsExclusive() {
    	this.isExclusive = true;
    }
	
	/**
	 * Return whether or not this TypeQualifierValue denotesw
	 * an exclusive qualifier.
	 * 
	 * @return true if type qualifier is exclusive, false otherwise
	 */
	public boolean isExclusiveQualifier() {
		return isExclusive;
	}

	@Override
    public int hashCode() {
		int result = typeQualifier.hashCode();
		if (value != null) result += 37*value.hashCode();
		return result;
	}

	@Override
    public boolean equals(Object o) {
		if (!(o instanceof TypeQualifierValue)) return false;
		TypeQualifierValue other = (TypeQualifierValue) o;
		return typeQualifier.equals(other.typeQualifier) && Util.nullSafeEquals(value, other.value);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append(typeQualifier.toString());
		if (value != null) {
			buf.append(':');
			buf.append(value.toString());
		}
		return buf.toString();
	}


}
