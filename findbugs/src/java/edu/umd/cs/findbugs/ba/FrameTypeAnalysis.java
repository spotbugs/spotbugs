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

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A forward dataflow analysis to determine the types of all values
 * in the Java stack frame at all points in a Java method.
 * The values include local variables and values on the Java operand stack.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @see TypedFrame
 * @author David Hovemeyer
 */
public class FrameTypeAnalysis extends ForwardDataflowAnalysis<TypedFrame> {
	private MethodGen methodGen;

	/**
	 * Constructor.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 */
	public FrameTypeAnalysis(MethodGen methodGen) {
		this.methodGen = methodGen;
	}

	public TypedFrame createFact() {
		return new TypedFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(TypedFrame result) {
		int slot = 0;

		// Clear the stack slots in the frame
		result.clearStack();

		// Add local for "this" pointer, if present
		if (!methodGen.isStatic())
			result.setValue(slot++, new ObjectType(methodGen.getClassName()));

		// Add locals for parameters
		Type[] argumentTypes = methodGen.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; ++i)
			result.setValue(slot++, argumentTypes[i]);

		// Set remaining parameters to BOTTOM; this will cause any
		// uses of them to be flagged
		while (slot < methodGen.getMaxLocals())
			result.setValue(slot++, TypedFrame.getBottomType());
	}

	public void copy(TypedFrame source, TypedFrame dest) {
		dest.copyFrom(source);
	}

	public void initResultFact(TypedFrame result) {
		// This is important.  Sometimes we need to use a result value
		// before having a chance to initialize it.  We don't want such
		// values to corrupt other TypedFrame values that we merge them with.
		// So, all result values must be TOP.
		result.setTop();
	}

	public void makeFactTop(TypedFrame fact) {
		fact.setTop();
	}

	public boolean isFactValid(TypedFrame fact) {
		return fact.isValid();
	}

	public boolean same(TypedFrame fact1, TypedFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transferInstruction(InstructionHandle handle, TypedFrame fact) throws DataflowAnalysisException {
		FrameTypeModelingVisitor visitor = new FrameTypeModelingVisitor(fact, methodGen.getConstantPool());
		handle.getInstruction().accept(visitor);
	}

	public void meetInto(TypedFrame fact, Edge edge, TypedFrame result) throws DataflowAnalysisException {
		BasicBlock basicBlock = edge.getDest();
		if (basicBlock.isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.
			CodeExceptionGen exceptionGen = basicBlock.getExceptionGen(); 
			TypedFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();
			tmpFact.pushValue(exceptionGen.getCatchType());
			fact = tmpFact;
		}
		result.mergeWith(fact);
	}
}

// vim:ts=4
