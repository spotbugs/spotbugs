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

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class InnerClassAccessMap {
	private Map<String, Map<String, XField>> classToAccessMap;

	private InnerClassAccessMap() {
		this.classToAccessMap = new HashMap<String, Map<String, XField>>();
	}

	private static InnerClassAccessMap instance = new InnerClassAccessMap();

	public static InnerClassAccessMap instance() { return instance; }

	private static int toInt(byte b) {
		int value = b & 0x7F;
		if ((b & 0x80) != 0)
			value |= 0x80;
		return value;
	}

	private static int getIndex(byte[] instructionList, int index) {
		return (toInt(instructionList[index+1]) << 8) | toInt(instructionList[index+2]);
	}

	private static class InstructionCallback implements BytecodeScanner.Callback {
		private JavaClass javaClass;
		private String methodName;
		private byte[] instructionList;
		private Map<String, XField> accessMap;

		public InstructionCallback(JavaClass javaClass, String methodName, byte[] instructionList, Map<String, XField> accessMap) {
			this.javaClass = javaClass;
			this.methodName = methodName;
			this.instructionList = instructionList;
			this.accessMap = accessMap;
		}

		public void handleInstruction(int opcode, int index) {
			switch (opcode) {
			case Constants.GETFIELD:
			case Constants.PUTFIELD:
				setField(getIndex(instructionList, index), false);
				break;
			case Constants.GETSTATIC:
			case Constants.PUTSTATIC:
				setField(getIndex(instructionList, index), true);
				break;
			}
		}

		private void setField(int cpIndex, boolean isStatic) {
			ConstantPool cp = javaClass.getConstantPool();
			ConstantFieldref fieldref = (ConstantFieldref) cp.getConstant(cpIndex);

			ConstantClass cls = (ConstantClass) cp.getConstant(fieldref.getClassIndex());
			String className = cls.getBytes(cp);

			ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(fieldref.getNameAndTypeIndex());
			String fieldName = nameAndType.getName(cp);
			String fieldSig = nameAndType.getSignature(cp);

			XField xfield = isStatic
				? (XField) new StaticField(className, fieldName, fieldSig)
				: (XField) new InstanceField(className, fieldName, fieldSig);

			accessMap.put(methodName, xfield);
		}
	}

	private static final Map<String, XField> emptyMap = new HashMap<String, XField>();

	/**
	 * Return a map of inner-class member access method names to
	 * the fields that they access for given class name.
	 * @param className the name of the class
	 * @return map of access method names to the fields they access
	 */
	public Map<String, XField> getAccessMapForClass(String className)
		throws ClassNotFoundException {

		Map<String, XField> map = classToAccessMap.get(className);
		if (map == null) {
			map = new HashMap<String, XField>();
			JavaClass javaClass = Repository.lookupClass(className);

			Method[]  methodList = javaClass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				String methodName = method.getName();
				if (!methodName.startsWith("access$"))
					continue;

				Code code = method.getCode();
				if (code == null)
					continue;

				byte[] instructionList = code.getCode();
				InstructionCallback callback = new InstructionCallback(javaClass, methodName, instructionList, map);
				new BytecodeScanner().scan(instructionList, callback);
			}

			if (map.size() == 0)
				map = emptyMap;
		}

		return map;
	}

}

// vim:ts=4
