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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class Lookup {
	public static boolean isMonitorWait(String methodName, String methodSig) {
		return methodName.equals("wait") &&
			(methodSig.equals("()V") || methodSig.equals("(J)V") || methodSig.equals("(JI)V"));
	}

	public static boolean isMonitorNotify(String methodName, String methodSig) {
		return (methodName.equals("notify") || methodName.equals("notifyAll")) &&
			methodSig.equals("()V");
	}

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
