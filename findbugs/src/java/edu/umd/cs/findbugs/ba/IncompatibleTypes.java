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
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.impl.AnalysisCache;

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
		return msg;
	}

	@Override
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

	static public @NonNull IncompatibleTypes getPriorityForAssumingCompatible(
			Type lhsType, Type rhsType) {
		return getPriorityForAssumingCompatible(lhsType, rhsType, false);
	}
	
	static public @NonNull IncompatibleTypes getPriorityForAssumingCompatible(
			Type lhsType, Type rhsType, boolean pointerEquality) {
		if (!(lhsType instanceof ReferenceType))
			return SEEMS_OK;
		if (!(rhsType instanceof ReferenceType))
			return SEEMS_OK;


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
				(ObjectType) rhsType, pointerEquality);

	}

	
	static @NonNull XMethod getInvokedMethod(XClass xClass, String name, String sig, boolean isStatic) throws CheckedAnalysisException {
		IAnalysisCache cache = Global.getAnalysisCache();
		while (true) {
		 XMethod result = xClass.findMethod(name, sig, isStatic);
		 if (result != null) return result;
		 if (isStatic) throw  new CheckedAnalysisException();
		 ClassDescriptor superclassDescriptor = xClass.getSuperclassDescriptor();
		 if (superclassDescriptor == null) throw new CheckedAnalysisException();
		xClass = cache.getClassAnalysis(XClass.class, superclassDescriptor);
		}
		 
	}
	static public @NonNull IncompatibleTypes getPriorityForAssumingCompatible(
			ObjectType lhsType, ObjectType rhsType, boolean pointerEquality) {
		if (lhsType.equals(rhsType)) return SEEMS_OK;
		try {
		// See if the types are related by inheritance.
		ClassDescriptor lhsDescriptor = DescriptorFactory.createClassDescriptorFromDottedClassName(lhsType.getClassName());
		ClassDescriptor rhsDescriptor = DescriptorFactory.createClassDescriptorFromDottedClassName(rhsType.getClassName());
		
		IAnalysisCache cache = Global.getAnalysisCache();
		XClass lhs = cache.getClassAnalysis(XClass.class, lhsDescriptor);
		XClass rhs = cache.getClassAnalysis(XClass.class, rhsDescriptor);
			if (!Hierarchy.isSubtype(lhsType, rhsType)
					&& !Hierarchy.isSubtype(rhsType, lhsType)) {
				AnalysisContext analysisContext = AnalysisContext
						.currentAnalysisContext();
				// Look up the classes
				XMethod lhsEquals = getInvokedMethod(lhs, "equals", "(Ljava/lang/Object;)Z", false);
				XMethod rhsEquals = getInvokedMethod(rhs, "equals", "(Ljava/lang/Object;)Z", false);
				String lhsClassName = lhsEquals.getClassName();
				if (lhsEquals.equals(rhsEquals)) {
					if (lhsClassName.equals("java.lang.Enum")) return INCOMPATIBLE_CLASSES;
					if (!pointerEquality && !lhsClassName.equals("java.lang.Object")) return SEEMS_OK;
				}
					
				
				
				if (!lhs.isInterface() && !rhs.isInterface()) {
					// Both are class types, and therefore there is no possible
					// way
					// the compared objects can have the same runtime type.
					return INCOMPATIBLE_CLASSES;
				} else {

					// Look up the common subtypes of the two types. If the
					// intersection does not contain at least one instantiable
					// class,
					// then issue a warning of the appropriate type.
					Set<ClassDescriptor> commonSubtypes = analysisContext
							.getSubtypes2().getTransitiveCommonSubtypes(
									lhsDescriptor, rhsDescriptor);

					if (!containsAtLeastOneInstantiableClass(commonSubtypes)) {
						if (lhs.isInterface() && rhs.isInterface())
							return UNRELATED_INTERFACES;
						else
							return UNRELATED_CLASS_AND_INTERFACE;
					}

				}
			}

		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		} catch (MissingClassException e) {
			AnalysisContext.reportMissingClass(e.getClassNotFoundException());
		} catch (CheckedAnalysisException e) {
			AnalysisContext.logError("Error checking for incompatible types", e);
        }
		return SEEMS_OK;
	}

	private static boolean containsAtLeastOneInstantiableClass(
			Set<ClassDescriptor> commonSubtypes) throws CheckedAnalysisException {
		IAnalysisCache cache = Global.getAnalysisCache();
		for (ClassDescriptor classDescriptor : commonSubtypes) {
			
			XClass xclass = cache.getClassAnalysis(XClass.class, classDescriptor);
			
			if (!xclass.isInterface() && !xclass.isAbstract())
				return true;
		}
		return false;
	}

}
