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
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A forward dataflow analysis to determine the types of all values
 * in the Java stack frame at all points in a Java method.
 * The values include local variables and values on the Java operand stack.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @see TypeFrame
 * @author David Hovemeyer
 */
public class TypeAnalysis extends FrameDataflowAnalysis<Type, TypeFrame> {
	private MethodGen methodGen;
	private TypeMerger typeMerger;
	private TypeFrameModelingVisitor visitor;

	/**
	 * Constructor.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 * @param dfs DepthFirstSearch of the method
	 * @param typeMerger object to merge types
	 * @param visitor a TypeFrameModelingVisitor to use to model the effect
	 *   of instructions
	 */
	public TypeAnalysis(MethodGen methodGen, DepthFirstSearch dfs, TypeMerger typeMerger, TypeFrameModelingVisitor visitor) {
		super(dfs);
		this.methodGen = methodGen;
		this.typeMerger = typeMerger;
		this.visitor = visitor;
	}

	/**
	 * Constructor.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 * @param dfs DepthFirstSearch of the method
	 * @param typeMerger object to merge types
	 */
	public TypeAnalysis(MethodGen methodGen, DepthFirstSearch dfs, TypeMerger typeMerger) {
		this(methodGen, dfs, typeMerger, new TypeFrameModelingVisitor(methodGen.getConstantPool()));
	}

	/**
	 * Constructor which uses StandardTypeMerger.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 * @param dfs DepthFirstSearch of the method
	 * @param lookupFailureCallback callback for Repository lookup failures
	 */
	public TypeAnalysis(MethodGen methodGen, DepthFirstSearch dfs, RepositoryLookupFailureCallback lookupFailureCallback) {
		this(methodGen, dfs, new StandardTypeMerger(lookupFailureCallback));
	}

	public TypeFrame createFact() {
		return new TypeFrame(methodGen.getMaxLocals(), typeMerger);
	}

	public void initEntryFact(TypeFrame result) {
		// Make the frame valid
		result.setValid();

		int slot = 0;

		// Clear the stack slots in the frame
		result.clearStack();

		// Add local for "this" pointer, if present
		if (!methodGen.isStatic())
			result.setValue(slot++, new ObjectType(methodGen.getClassName()));

		// Add locals for parameters.
		// Note that long and double parameters need to be handled
		// specially because they occupy two locals.
		Type[] argumentTypes = methodGen.getArgumentTypes();
		for (int i = 0; i < argumentTypes.length; ++i) {
			Type argType = argumentTypes[i];

			// Add special "extra" type for long or double params.
			// These occupy the slot before the "plain" type.
			if (argType.getType() == Constants.T_LONG) {
				result.setValue(slot++, TypeFrame.getLongExtraType());
			} else if (argType.getType() == Constants.T_DOUBLE) {
				result.setValue(slot++, TypeFrame.getDoubleExtraType());
			}

			// Add the plain parameter type.
			result.setValue(slot++, argType);
		}

		// Set remaining locals to BOTTOM; this will cause any
		// uses of them to be flagged
		while (slot < methodGen.getMaxLocals())
			result.setValue(slot++, TypeFrame.getBottomType());
	}

	public void copy(TypeFrame source, TypeFrame dest) {
		dest.copyFrom(source);
	}

	public void initResultFact(TypeFrame result) {
		// This is important.  Sometimes we need to use a result value
		// before having a chance to initialize it.  We don't want such
		// values to corrupt other TypeFrame values that we merge them with.
		// So, all result values must be TOP.
		result.setTop();
	}

	public void makeFactTop(TypeFrame fact) {
		fact.setTop();
	}

	public boolean isFactValid(TypeFrame fact) {
		return fact.isValid();
	}

	public boolean same(TypeFrame fact1, TypeFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, TypeFrame fact)
		throws DataflowAnalysisException {
		visitor.setFrame(fact);
		handle.getInstruction().accept(visitor);
	}

	public void meetInto(TypeFrame fact, Edge edge, TypeFrame result) throws DataflowAnalysisException {
		BasicBlock basicBlock = edge.getTarget();
		if (basicBlock.isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.
			CodeExceptionGen exceptionGen = basicBlock.getExceptionGen(); 
			TypeFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();
			Type catchType = exceptionGen.getCatchType();
			if (catchType == null)
				catchType = Type.THROWABLE; // handle catches anything throwable
			tmpFact.pushValue(catchType);
			fact = tmpFact;
		}
		result.mergeWith(fact);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + TypeAnalysis.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<TypeFrame, TypeAnalysis> driver = new DataflowTestDriver<TypeFrame, TypeAnalysis>() {
			public Dataflow<TypeFrame, TypeAnalysis> createDataflow(ClassContext classContext, Method method)
				throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getTypeDataflow(method);
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=4
