/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Facade for class hierarchy queries.
 * These typically access the class hierarchy using
 * the {@link Repository} class.  Callers should generally
 * expect to handle ClassNotFoundException for when referenced
 * classes can't be found.
 *
 * @author David Hovemeyer
 */
public class Hierarchy {
	/**
	 * Determine whether one class (or reference type) is a subtype
	 * of another.
	 * @param clsName the name of the class or reference type
	 * @param possibleSupertypeClassName the name of the possible superclass
	 * @return true if clsName is a subtype of possibleSupertypeClassName,
	 *   false if not
	 */
	public static boolean isSubtype(String clsName, String possibleSupertypeClassName) throws ClassNotFoundException {
		ObjectType cls = new ObjectType(clsName);
		ObjectType superCls = new ObjectType(possibleSupertypeClassName);
		return isSubtype(cls, superCls);
	}

	/**
	 * Determine if one reference type is a subtype of another.
	 * @param t a reference type
	 * @param possibleSupertype the possible supertype
	 * @return true if t is a subtype of possibleSupertype,
	 *   false if not
	 */
	public static boolean isSubtype(ReferenceType t, ReferenceType possibleSupertype) throws ClassNotFoundException {
		return t.isAssignmentCompatibleWith(possibleSupertype);
	}

	/**
	 * Determine if method whose name and signature is specified
	 * is a monitor wait operation.
	 * @param methodName name of the method
	 * @param methodSig signature of the method
	 * @return true if the method is a monitor wait, false if not
	 */
	public static boolean isMonitorWait(String methodName, String methodSig) {
		return methodName.equals("wait") &&
			(methodSig.equals("()V") || methodSig.equals("(J)V") || methodSig.equals("(JI)V"));
	}

	/**
	 * Determine if method whose name and signature is specified
	 * is a monitor notify operation.
	 * @param methodName name of the method
	 * @param methodSig signature of the method
	 * @return true if the method is a monitor notify, false if not
	 */
	public static boolean isMonitorNotify(String methodName, String methodSig) {
		return (methodName.equals("notify") || methodName.equals("notifyAll")) &&
			methodSig.equals("()V");
	}

	/**
	 * Look up the method referenced by given InvokeInstruction.
	 * This method does <em>not</em> look for implementations in
	 * super or subclasses according to the virtual dispatch rules.
	 * @param inv the InvokeInstruction
	 * @param cpg the ConstantPoolGen used by the class the InvokeInstruction belongs to
	 * @return the Method, or null if no such method is defined in the class
	 */
	public static Method findExactMethod(InvokeInstruction inv, ConstantPoolGen cpg) throws ClassNotFoundException {
		String className = inv.getClassName(cpg);
		String methodName = inv.getName(cpg);
		String methodSig = inv.getSignature(cpg);

		JavaClass jclass = Repository.lookupClass(className);
		Method[] methodList = jclass.getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			if (method.getName().equals(methodName) && method.getSignature().equals(methodSig))
				return method;
		}

		return null;
	}

	/**
	 * Find a field with given name defined in given class.
	 * @param className the name of the class
	 * @param fieldName the name of the field
	 * @return the Field, or null if no such field could be found
	 */
	public static Field findField(String className, String fieldName) throws ClassNotFoundException {
		JavaClass jclass = Repository.lookupClass(className);

		while (jclass != null) {
			Field[] fieldList = jclass.getFields();
			for (int i = 0; i < fieldList.length; ++i) {
				Field field = fieldList[i];
				if (field.getName().equals(fieldName)) {
					return field;
				}
			}
	
			jclass = jclass.getSuperClass();
		}

		return null;
	}

/*
	public static JavaClass findClassDefiningField(String className, String fieldName, String fieldSig)
		throws ClassNotFoundException {

		JavaClass jclass = Repository.lookupClass(className);

		while (jclass != null) {
			Field[] fieldList = jclass.getFields();
			for (int i = 0; i < fieldList.length; ++i) {
				Field field = fieldList[i];
				if (field.getName().equals(fieldName) && field.getSignature().equals(fieldSig)) {
					return jclass;
				}
			}
	
			jclass = jclass.getSuperClass();
		}

		return null;
	}
*/

	/**
	 * Look up a field with given name and signature in given class,
	 * returning it as an {@link XField XField} object.
	 * If a field can't be found in the immediate class,
	 * its superclass is search, and so forth.
	 *
	 * @param className name of the class through which the field
	 *   is referenced
	 * @param fieldName name of the field
	 * @param fieldSig signature of the field
	 * @return an XField object representing the field, or null if no such field could be found
	 */
	public static XField findXField(String className, String fieldName, String fieldSig)
		throws ClassNotFoundException {

		JavaClass classDefiningField = Repository.lookupClass(className);

		Field field = null;
	loop:
		while (classDefiningField != null) {
			Field[] fieldList = classDefiningField.getFields();
			for (int i = 0; i < fieldList.length; ++i) {
				field = fieldList[i];
				if (field.getName().equals(fieldName) && field.getSignature().equals(fieldSig)) {
					break loop;
				}
			}
	
			classDefiningField = classDefiningField.getSuperClass();
		}

		if (classDefiningField == null)
			return null;
		else {
			String realClassName = classDefiningField.getClassName();
			int accessFlags = field.getAccessFlags();
			return field.isStatic()
				? (XField) new StaticField(realClassName, fieldName, fieldSig, accessFlags)
				: (XField) new InstanceField(realClassName, fieldName, fieldSig, accessFlags);
		}
	}

	/**
	 * Look up the field referenced by given FieldInstruction,
	 * returning it as an {@link XField XField} object.
	 * @param fins the FieldInstruction
	 * @param cpg the ConstantPoolGen used by the class containing the instruction
	 * @return an XField object representing the field, or null
	 *   if no such field could be found
	 */
	public static XField findXField(FieldInstruction fins, ConstantPoolGen cpg)
		throws ClassNotFoundException {

		String className = fins.getClassName(cpg);
		String fieldName = fins.getFieldName(cpg);
		String fieldSig = fins.getSignature(cpg);

		XField xfield = findXField(className, fieldName, fieldSig);
		short opcode = fins.getOpcode();
		if (xfield != null &&
			xfield.isStatic() == (opcode == Constants.GETSTATIC || opcode == Constants.PUTSTATIC))
			return xfield;
		else
			return null;
	}
}

// vim:ts=4
