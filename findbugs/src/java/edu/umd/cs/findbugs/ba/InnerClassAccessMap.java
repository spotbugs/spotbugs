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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Determine which methods are accessors used
 * by inner classes to access fields in their enclosing classes.
 * This has been tested with javac from the Sun JDK 1.4.x,
 * but will probably not work with other source to bytecode compilers.
 * 
 * <p>
 * The instance of InnerClassAccessMap should be retrieved
 * from the AnalysisContext.
 * </p>
 *
 * @author David Hovemeyer
 * @see InnerClassAccess
 */
public class InnerClassAccessMap {
	private static final boolean DEBUG = SystemProperties.getBoolean("icam.debug");

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	/**
	 * Map of class names to maps of method names to InnerClassAccess objects
	 * representing access methods.
	 */
	private Map<String, Map<String, InnerClassAccess>> classToAccessMap;

	/* ----------------------------------------------------------------------
	 * Public interface
	 * ---------------------------------------------------------------------- */
	
	/**
	 * Create an instance.
	 * 
	 * @return a new instance of InnerClassAccessMap
	 */
	public static InnerClassAccessMap create() {
		return new InnerClassAccessMap();
	}

	/**
	 * Get the InnerClassAccess in given class with the given method name.
	 *
	 * @param className  the name of the class
	 * @param methodName the name of the access method
	 * @return the InnerClassAccess object for the method, or null if
	 *         the method doesn't seem to be an inner class access
	 */
	public InnerClassAccess getInnerClassAccess(String className, String methodName) throws ClassNotFoundException {
		Map<String, InnerClassAccess> map = getAccessMapForClass(className);
		return map.get(methodName);
	}

	/**
	 * Get the inner class access object for given invokestatic instruction.
	 * Returns null if the called method is not an inner class access.
	 *
	 * @param inv the invokestatic instruction
	 * @param cpg the ConstantPoolGen for the method
	 * @return the InnerClassAccess, or null if the call is not an inner class access
	 */
	public InnerClassAccess getInnerClassAccess(INVOKESTATIC inv, ConstantPoolGen cpg) throws ClassNotFoundException {
		String methodName = inv.getMethodName(cpg);
		if (methodName.startsWith("access$")) {
			String className = inv.getClassName(cpg);

			return getInnerClassAccess(className, methodName);
		}
		return null;
	}

	/**
	 * Clear the cache.
	 */
	public void clearCache() {
		classToAccessMap.clear();
	}

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 */
	private InnerClassAccessMap() {
		this.classToAccessMap = new HashMap<String, Map<String, InnerClassAccess>>();
	}

	/**
	 * Convert byte to unsigned int.
	 */
	private static int toInt(byte b) {
		int value = b & 0x7F;
		if ((b & 0x80) != 0)
			value |= 0x80;
		return value;
	}

	/**
	 * Get an unsigned 16 bit constant pool index from a byte array.
	 */
	private static int getIndex(byte[] instructionList, int index) {
		return (toInt(instructionList[index + 1]) << 8) | toInt(instructionList[index + 2]);
	}

	private static class LookupFailure extends RuntimeException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final ClassNotFoundException exception;

		public LookupFailure(ClassNotFoundException exception) {
			this.exception = exception;
		}

		public ClassNotFoundException getException() {
			return exception;
		}
	}

	/**
	 * Callback to scan an access method to determine what
	 * field it accesses, and whether the field is loaded or stored.
	 */
	private static class InstructionCallback implements BytecodeScanner.Callback {
		private JavaClass javaClass;
		private String methodName;
		private String methodSig;
		private byte[] instructionList;
		private InnerClassAccess access;
		private int accessCount;

		/**
		 * Constructor.
		 *
		 * @param javaClass       the class containing the access method
		 * @param methodName      the name of the access method
		 * @param methodSig       the signature of the access method
		 * @param instructionList the bytecode of the method
		 */
		public InstructionCallback(JavaClass javaClass, String methodName, String methodSig, byte[] instructionList) {
			this.javaClass = javaClass;
			this.methodName = methodName;
			this.methodSig = methodSig;
			this.instructionList = instructionList;
			this.access = null;
			this.accessCount = 0;
		}

		public void handleInstruction(int opcode, int index) {
			switch (opcode) {
			case Constants.GETFIELD:
			case Constants.PUTFIELD:
				setField(getIndex(instructionList, index), false, opcode == Constants.GETFIELD);
				break;
			case Constants.GETSTATIC:
			case Constants.PUTSTATIC:
				setField(getIndex(instructionList, index), true, opcode == Constants.GETSTATIC);
				break;
			}
		}

		/**
		 * Get the InnerClassAccess object representing the method.
		 *
		 * @return the InnerClassAccess, or null if the method
		 *         was not found to be a simple load or store in the
		 *         expected form
		 */
		public InnerClassAccess getAccess() {
			return access;
		}

		/**
		 * Called to indicate that a field load or store was encountered.
		 *
		 * @param cpIndex  the constant pool index of the fieldref
		 * @param isStatic true if it is a static field access
		 * @param isLoad   true if the access is a load
		 */
		private void setField(int cpIndex, boolean isStatic, boolean isLoad) {
			// We only allow one field access for an accessor method.
			accessCount++;
			if (accessCount != 1) {
				access = null;
				return;
			}

			ConstantPool cp = javaClass.getConstantPool();
			ConstantFieldref fieldref = (ConstantFieldref) cp.getConstant(cpIndex);

			ConstantClass cls = (ConstantClass) cp.getConstant(fieldref.getClassIndex());
			String className = cls.getBytes(cp).replace('/', '.');

			ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(fieldref.getNameAndTypeIndex());
			String fieldName = nameAndType.getName(cp);
			String fieldSig = nameAndType.getSignature(cp);

			try {
				XField xfield = Hierarchy.findXField(className, fieldName, fieldSig);
				if (xfield != null && xfield.isStatic() == isStatic && isValidAccessMethod(methodSig, xfield, isLoad)) {
					access = new InnerClassAccess(methodName, methodSig, xfield, isLoad);
				}
			} catch (ClassNotFoundException e) {
				throw new LookupFailure(e);
			}
		}

		/**
		 * Determine if the method appears to be an accessor of the expected form.
		 * This has only been tested with the Sun JDK 1.4 javac (definitely)
		 * and jikes 1.18 (I think).
		 *
		 * @param methodSig the method's signature
		 * @param field     the field accessed by the method
		 * @param isLoad    true if the access is a load
		 */
		private boolean isValidAccessMethod(String methodSig, XField field, boolean isLoad) {

			// Get the method parameters and return type
			// (as they appear in the method signature).
			int paramsEnd = methodSig.indexOf(')');
			if (paramsEnd < 0)
				return false;
			String methodParams = methodSig.substring(0, paramsEnd + 1);
			String methodReturnType = methodSig.substring(paramsEnd + 1);

			// Figure out what the expected method parameters should be
			String classSig = "L" + javaClass.getClassName().replace('.', '/') + ";";
			StringBuffer buf = new StringBuffer();
			buf.append('(');
			if (!field.isStatic())
				buf.append(classSig); // the OuterClass.this reference
			if (!isLoad)
				buf.append(field.getSignature()); // the value being stored
			buf.append(')');
			String expectedMethodParams = buf.toString();

			// See if params match
			if (!methodParams.equals(expectedMethodParams)) {
				if (DEBUG) {
					System.out.println("In " + javaClass.getClassName() + "." + methodName +
						" expected params " +
						expectedMethodParams + ", saw " + methodParams);
					System.out.println(isLoad ? "LOAD" : "STORE");
				}
				return false;
			}

			// Return type can be either the type of the field, or void.
			if (!methodReturnType.equals("V") && !methodReturnType.equals(field.getSignature())) {
				if (DEBUG) {
					System.out.println("In " + javaClass.getClassName() + "." + methodName +
						" expected return type V or " + field.getSignature() +
						", saw " + methodReturnType);
					System.out.println(isLoad ? "LOAD" : "STORE");
				}
				return false;
			}

			return true;
		}
	}

	private static final Map<String, InnerClassAccess> emptyMap = new HashMap<String, InnerClassAccess>();

	/**
	 * Return a map of inner-class member access method names to
	 * the fields that they access for given class name.
	 *
	 * @param className the name of the class
	 * @return map of access method names to the fields they access
	 */
	private Map<String, InnerClassAccess> getAccessMapForClass(String className)
	        throws ClassNotFoundException {

		Map<String, InnerClassAccess> map = classToAccessMap.get(className);
		if (map == null) {
			map = new HashMap<String, InnerClassAccess>();

			if (!className.startsWith("[")) {
				JavaClass javaClass = Repository.lookupClass(className);

				Method[] methodList = javaClass.getMethods();
				for (Method method : methodList) {
					String methodName = method.getName();
					if (!methodName.startsWith("access$"))
						continue;

					Code code = method.getCode();
					if (code == null)
						continue;

					if (DEBUG) System.out.println("Analyzing " + className + "." +
							method.getName() + " as an inner-class access method...");

					byte[] instructionList = code.getCode();
					String methodSig = method.getSignature();
					InstructionCallback callback = new InstructionCallback(javaClass, methodName, methodSig, instructionList);
					try {
						new BytecodeScanner().scan(instructionList, callback);
					} catch (LookupFailure lf) {
						throw lf.getException();
					}
					InnerClassAccess access = callback.getAccess();
					if (DEBUG) System.out.println((access != null ? "IS" : "IS NOT") + " an inner-class access method");
					if (access != null)
						map.put(methodName, access);
				}
			}

			if (map.size() == 0)
				map = emptyMap;

			classToAccessMap.put(className, map);
		}

		return map;
	}

}

// vim:ts=4
