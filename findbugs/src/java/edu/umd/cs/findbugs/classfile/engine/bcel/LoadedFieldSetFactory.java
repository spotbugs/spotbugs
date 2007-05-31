/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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
package edu.umd.cs.findbugs.classfile.engine.bcel;

import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.InnerClassAccess;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.vna.LoadedFieldSet;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Factory to determine which fields are loaded and stored
 * by the instructions in a method, and the overall method.
 * The main purpose is to support efficient redundant load elimination
 * and forward substitution in ValueNumberAnalysis (there is no need to
 * remember stores of fields that are never read,
 * or loads of fields that are only loaded in one location).
 * However, it might be useful for other kinds of analysis.
 *
 * <p> The tricky part is that in addition to fields loaded and stored
 * with get/putfield and get/putstatic, we also try to figure
 * out field accessed through calls to inner-class access methods.
 */
public class LoadedFieldSetFactory extends AnalysisFactory<LoadedFieldSet> {

	static final BitSet fieldInstructionOpcodeSet = new BitSet();
	static {
		fieldInstructionOpcodeSet.set(Constants.GETFIELD);
		fieldInstructionOpcodeSet.set(Constants.PUTFIELD);
		fieldInstructionOpcodeSet.set(Constants.GETSTATIC);
		fieldInstructionOpcodeSet.set(Constants.PUTSTATIC);
	}

	/**
	 * Constructor.
	 */
	public LoadedFieldSetFactory() {
		super("loaded field set factory", LoadedFieldSet.class);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
		MethodGen methodGen = getMethodGen(analysisCache, descriptor);
		if (methodGen == null) return null;
		InstructionList il = methodGen.getInstructionList();

		LoadedFieldSet loadedFieldSet = new LoadedFieldSet(methodGen);
		JavaClass jclass = getJavaClass(analysisCache, descriptor.getClassDescriptor());
		ConstantPoolGen cpg = getConstantPoolGen(analysisCache, descriptor.getClassDescriptor());

		for (InstructionHandle handle = il.getStart(); handle != null; handle = handle.getNext()) {
			Instruction ins = handle.getInstruction();
			short opcode = ins.getOpcode();
			try {
				if (opcode == Constants.INVOKESTATIC) {
					INVOKESTATIC inv = (INVOKESTATIC) ins;
					if (Hierarchy.isInnerClassAccess(inv, cpg)) {
						InnerClassAccess access = Hierarchy.getInnerClassAccess(inv, cpg);
						/*
    									if (access == null) {
    										System.out.println("Missing inner class access in " +
    											SignatureConverter.convertMethodSignature(methodGen) + " at " +
    											inv);
    									}
						 */
						if (access != null) {
							if (access.isLoad())
								loadedFieldSet.addLoad(handle, access.getField());
							else
								loadedFieldSet.addStore(handle, access.getField());
						}
					}
				} else if (fieldInstructionOpcodeSet.get(opcode)) {
					boolean isLoad = (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC);
					XField field = Hierarchy.findXField((FieldInstruction) ins, cpg);
					if (field != null) {
						if (isLoad)
							loadedFieldSet.addLoad(handle, field);
						else
							loadedFieldSet.addStore(handle, field);
					}
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			}
		}

		return loadedFieldSet;
	}
}
