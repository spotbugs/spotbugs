/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.Priorities;

public class IncompatibleTypes {
	final int priority;

	final String msg;

	private IncompatibleTypes(String msg, int priority) {
		this.msg = msg;
		this.priority = priority;
	}

	public int getPriority() {
		return priority;
	}

	public String getMsg() {
		return getMsg();
	}

	public String toString() {
		return msg;
	}

	public static final IncompatibleTypes SEEMS_OK = new IncompatibleTypes(
			"Seems OK", Priorities.IGNORE_PRIORITY);

	public static final IncompatibleTypes ARRAY_AND_NON_ARRAY = new IncompatibleTypes(
			"Array and non array", Priorities.HIGH_PRIORITY);

	public static final IncompatibleTypes ARRAY_AND_OBJECT = new IncompatibleTypes(
			"Array and Object", Priorities.LOW_PRIORITY);

	public static final IncompatibleTypes INCOMPATIBLE_CLASSES = new IncompatibleTypes(
			"Incompatible classes", Priorities.HIGH_PRIORITY);

	public static final IncompatibleTypes UNRELATED_CLASS_AND_INTERFACE = new IncompatibleTypes(
			"Unrelated class and interface", Priorities.HIGH_PRIORITY);

	public static final IncompatibleTypes UNRELATED_INTERFACES = new IncompatibleTypes(
			"Unrelated interfaces", Priorities.NORMAL_PRIORITY);

	static public IncompatibleTypes getPriorityForAssumingCompatible(
			Type lhsType, Type rhsType) {
		if (!(lhsType instanceof ReferenceType))
			return SEEMS_OK;
		if (!(rhsType instanceof ReferenceType))
			return SEEMS_OK;

		ReferenceType originalLhsType = (ReferenceType) lhsType;
		ReferenceType originalRhsType = (ReferenceType) rhsType;

		while (lhsType instanceof ArrayType && rhsType instanceof ArrayType) {
			lhsType = ((ArrayType) lhsType).getElementType();
			rhsType = ((ArrayType) rhsType).getElementType();
		} 

		if (lhsType instanceof ArrayType) {

			if (rhsType.equals(ObjectType.OBJECT))
				return ARRAY_AND_OBJECT;
			else
				return ARRAY_AND_NON_ARRAY;
		}
		if (rhsType instanceof ArrayType) {
			if (lhsType.equals(ObjectType.OBJECT))
				return ARRAY_AND_OBJECT;
			else
				return ARRAY_AND_NON_ARRAY;
		}
		if (lhsType.equals(rhsType))
			return SEEMS_OK;

		// For now, ignore the case where either reference is not
		// of an object type. (It could be either an array or null.)
		if (!(lhsType instanceof ObjectType)
				|| !(rhsType instanceof ObjectType))
			return SEEMS_OK;

		return getPriorityForAssumingCompatible((ObjectType) lhsType,
				(ObjectType) rhsType);

	}

	static public IncompatibleTypes getPriorityForAssumingCompatible(
			ObjectType lhsType, ObjectType rhsType) {
		// See if the types are related by inheritance.
		try {
			if (!Hierarchy.isSubtype(lhsType, rhsType)
					&& !Hierarchy.isSubtype(rhsType, lhsType)) {
				AnalysisContext analysisContext = AnalysisContext
						.currentAnalysisContext();

				// Look up the classes
				JavaClass lhsClass = analysisContext.lookupClass(lhsType
						.getClassName());
				JavaClass rhsClass = analysisContext.lookupClass(rhsType
						.getClassName());

				if (!lhsClass.isInterface() && !rhsClass.isInterface()) {
					// Both are class types, and therefore there is no possible
					// way
					// the compared objects can have the same runtime type.
					return INCOMPATIBLE_CLASSES;
				} else {

					// Look up the common subtypes of the two types. If the
					// intersection does not contain at least one instantiable
					// class,
					// then issue a warning of the appropriate type.
					Set<JavaClass> commonSubtypes = analysisContext
							.getSubtypes().getTransitiveCommonSubtypes(
									lhsClass, rhsClass);

					if (!containsAtLeastOneInstantiableClass(commonSubtypes)) {
						if (lhsClass.isInterface() && rhsClass.isInterface())
							return UNRELATED_INTERFACES;
						else
							return UNRELATED_CLASS_AND_INTERFACE;
					}

				}
			}

		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
		return null;
	}

	private static boolean containsAtLeastOneInstantiableClass(
			Set<JavaClass> commonSubtypes) {
		for (JavaClass javaClass : commonSubtypes) {
			if (!javaClass.isInterface() && !javaClass.isAbstract())
				return true;
		}
		return false;
	}

}
