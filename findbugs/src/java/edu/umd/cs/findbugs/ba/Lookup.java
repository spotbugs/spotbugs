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

package edu.umd.cs.daveho.ba;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class Lookup {
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

	public static XField findXField(FieldInstruction fins, ConstantPoolGen cpg)
		throws ClassNotFoundException {

		String className = fins.getClassName(cpg);
		String fieldName = fins.getFieldName(cpg);
		String fieldSig = fins.getSignature(cpg);

		JavaClass classDefiningField = Lookup.findClassDefiningField(className, fieldName, fieldSig);

		if (classDefiningField == null)
			return null;
		else {
			short opcode = fins.getOpcode();
			boolean isStatic = (opcode == Constants.GETSTATIC || opcode == Constants.PUTSTATIC);
			String realClassName = classDefiningField.getClassName();
			return isStatic
				? (XField) new StaticField(realClassName, fieldName, fieldSig)
				: (XField) new InstanceField(realClassName, fieldName, fieldSig);
		}
	}
}

// vim:ts=4
