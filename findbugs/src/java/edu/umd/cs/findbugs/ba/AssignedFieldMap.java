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

import java.util.*;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

public class AssignedFieldMap implements Constants {
	private final ClassContext classContext;
	private final Map<Method, Set<XField>> assignedFieldSetForMethodMap;

	public AssignedFieldMap(ClassContext classContext) {
		this.classContext = classContext;
		this.assignedFieldSetForMethodMap = new IdentityHashMap<Method, Set<XField>>();
	}

	public void build() throws ClassNotFoundException {
		JavaClass jclass = classContext.getJavaClass();

		// Build a set of all fields that could be assigned
		// by methods in this class
		HashSet<XField> assignableFieldSet = new HashSet<XField>();
		scanFields(jclass, assignableFieldSet);
		JavaClass[] superClassList = jclass.getSuperClasses();
		if (superClassList != null) {
			for (int i = 0; i < superClassList.length; ++i) {
				scanFields(superClassList[i], assignableFieldSet);
			}
		}

		Method[] methodList = jclass.getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			scanMethod(method, assignableFieldSet);
		}
	}

	public Set<XField> getAssignedFieldSetForMethod(Method method) {
		Set<XField> set = assignedFieldSetForMethodMap.get(method);
		if (set == null) {
			set = new HashSet<XField>();
			assignedFieldSetForMethodMap.put(method, set);
		}
		return set;
	}

	private void scanFields(JavaClass jclass, Set<XField> assignableFieldSet) {
		JavaClass myClass = classContext.getJavaClass();
		String myClassName = myClass.getClassName();
		String myPackageName = myClass.getPackageName();

		String superClassName = jclass.getClassName();
		String superPackageName = jclass.getPackageName();

		Field[] fieldList = jclass.getFields();
		for (int i = 0; i < fieldList.length; ++i) {
			Field field = fieldList[i];
			if (field.isStatic())
				continue;
			boolean assignable = false;
			if (field.isPublic() || field.isProtected())
				assignable = true;
			else if (field.isPrivate())
				assignable = myClassName.equals(superClassName);
			else // package protected
				assignable = myPackageName.equals(superPackageName);

			if (assignable) {
				assignableFieldSet.add(new InstanceField(superClassName, field.getName(), field.getSignature(),
				        field.getAccessFlags()));
			}
		}
	}

	private void scanMethod(Method method, Set<XField> assignableFieldSet) throws ClassNotFoundException {
		MethodGen methodGen = classContext.getMethodGen(method);
		InstructionList il = methodGen.getInstructionList();
		InstructionHandle handle = il.getStart();

		ConstantPoolGen cpg = methodGen.getConstantPool();

		while (handle != null) {
			Instruction ins = handle.getInstruction();
			short opcode = ins.getOpcode();
			if (opcode == Constants.PUTFIELD) {
				PUTFIELD putfield = (PUTFIELD) ins;

				XField instanceField = Hierarchy.findXField(putfield, cpg);
				if (instanceField != null && assignableFieldSet.contains(instanceField)) {
					Set<XField> assignedFieldSetForMethod = getAssignedFieldSetForMethod(method);
					assignedFieldSetForMethod.add(instanceField);
				}
			}

			handle = handle.getNext();
		}
	}
}

// vim:ts=4
