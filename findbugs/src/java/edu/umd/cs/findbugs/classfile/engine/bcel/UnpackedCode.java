package edu.umd.cs.findbugs.classfile.engine.bcel;

import edu.umd.cs.findbugs.ba.MethodBytecodeSet;

/**
 * Unpacked code for a method.
 * Contains set of all opcodes in the method, as well as a map
 * of bytecode offsets to opcodes.
 */
public class UnpackedCode {
	private MethodBytecodeSet bytecodeSet;
	private short[] offsetToBytecodeMap;
	public UnpackedCode(MethodBytecodeSet bytecodeSet, short[] offsetToBytecodeMap) {
		this.bytecodeSet = bytecodeSet;
		this.offsetToBytecodeMap = offsetToBytecodeMap;
	}

	/**
	 * @return Returns the bytecodeSet.
	 */
	public MethodBytecodeSet getBytecodeSet() {
		return bytecodeSet;
	}

	/**
	 * @return Returns the offsetToBytecodeMap.
	 */
	public short[] getOffsetToBytecodeMap() {
		return offsetToBytecodeMap;
	}
}