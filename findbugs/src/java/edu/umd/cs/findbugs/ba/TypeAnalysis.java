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
 * <p> As a side effect, the analysis computes the exception
 * set throwable on each exception edge in the CFG.
 * This information can be used to prune infeasible exception
 * edges, and mark exception edges which propagate only
 * implicit exceptions.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @see TypeFrame
 * @author David Hovemeyer
 */
public class TypeAnalysis extends FrameDataflowAnalysis<Type, TypeFrame> {
	private static final boolean DEBUG  = Boolean.getBoolean("ta.debug");

	private static class CachedExceptionSet {
		private TypeFrame result;
		private ExceptionSet exceptionSet;

		public CachedExceptionSet(TypeFrame result, ExceptionSet exceptionSet) {
			this.result = result;
			this.exceptionSet = exceptionSet;
		}

		public boolean isUpToDate(TypeFrame result) {
			return this.result.equals(result);
		}

		public ExceptionSet getExceptionSet() {
			return exceptionSet;
		}
	}

	private MethodGen methodGen;
	private TypeMerger typeMerger;
	private TypeFrameModelingVisitor visitor;
	private Map<BasicBlock, CachedExceptionSet> exceptionSetMap;
	private RepositoryLookupFailureCallback lookupFailureCallback;

	/**
	 * Constructor.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 * @param dfs DepthFirstSearch of the method
	 * @param typeMerger object to merge types
	 * @param visitor a TypeFrameModelingVisitor to use to model the effect
	 *   of instructions
	 * @param lookupFailureCallback lookup failure callback
	 */
	public TypeAnalysis(MethodGen methodGen, DepthFirstSearch dfs, TypeMerger typeMerger, TypeFrameModelingVisitor visitor,
		RepositoryLookupFailureCallback lookupFailureCallback) {
		super(dfs);
		this.methodGen = methodGen;
		this.typeMerger = typeMerger;
		this.visitor = visitor;
		this.lookupFailureCallback = lookupFailureCallback;
		this.exceptionSetMap = new HashMap<BasicBlock, CachedExceptionSet>();
	}

	/**
	 * Constructor.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 * @param dfs DepthFirstSearch of the method
	 * @param typeMerger object to merge types
	 * @param lookupFailureCallback lookup failure callback
	 */
	public TypeAnalysis(MethodGen methodGen, DepthFirstSearch dfs, TypeMerger typeMerger,
		RepositoryLookupFailureCallback lookupFailureCallback) {
		this(methodGen, dfs, typeMerger, new TypeFrameModelingVisitor(methodGen.getConstantPool()), lookupFailureCallback);
	}

	/**
	 * Constructor which uses StandardTypeMerger.
	 * @param methodGen the MethodGen whose CFG we'll be analyzing
	 * @param dfs DepthFirstSearch of the method
	 * @param lookupFailureCallback callback for Repository lookup failures
	 */
	public TypeAnalysis(MethodGen methodGen, DepthFirstSearch dfs, RepositoryLookupFailureCallback lookupFailureCallback) {
		this(methodGen, dfs, new StandardTypeMerger(lookupFailureCallback), lookupFailureCallback);
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

	public void endTransfer(BasicBlock basicBlock, InstructionHandle end, Object result) throws DataflowAnalysisException {
		// Figure out what exceptions can be thrown out
		// of the basic block, and mark each exception edge
		// with the set of exceptions which can be propagated
		// along the edge.

		if (basicBlock.isExceptionThrower()) {
			try {
				ExceptionSet exceptionSet = computeExceptionTypes(basicBlock, (TypeFrame) result).duplicate();
/*
				for (Iterator<Edge> i = cfg.outgoingEdgeIterator(basicBlock); i.hasNext(); ) {
				}
*/
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
				throw new DataflowAnalysisException("Could not enumerate exception types for block", e);
			}
		}
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

	private CachedExceptionSet getCachedExceptionSet(BasicBlock basicBlock) {
		CachedExceptionSet cachedExceptionSet = exceptionSetMap.get(basicBlock);
		if (cachedExceptionSet == null) {
			// When creating the cached exception type set for the first time:
			// - the block result is set to TOP, so it won't match
			//   any block result that has actually been computed
			//   using the analysis transfer function
			// - the exception set is created as empty (which makes it
			//   return TOP as its common superclass)

			TypeFrame top = createFact();
			makeFactTop(top);
			cachedExceptionSet = new CachedExceptionSet(top, new ExceptionSet());

			exceptionSetMap.put(basicBlock, cachedExceptionSet);
		}

		return cachedExceptionSet;
	}

	private ExceptionSet computeExceptionTypes(BasicBlock basicBlock, TypeFrame result)
		throws ClassNotFoundException, DataflowAnalysisException {

		CachedExceptionSet cachedExceptionSet = getCachedExceptionSet(basicBlock);

		if (!cachedExceptionSet.isUpToDate(result)) {
			ExceptionSet exceptionSet = enumerateExceptionTypes(basicBlock);
			TypeFrame copyOfResult = createFact();
			copy(result, copyOfResult);

			cachedExceptionSet = new CachedExceptionSet(copyOfResult, exceptionSet);
			exceptionSetMap.put(basicBlock, cachedExceptionSet);
		}

		return cachedExceptionSet.getExceptionSet();
	}

	private ExceptionSet enumerateExceptionTypes(BasicBlock basicBlock)
		throws ClassNotFoundException, DataflowAnalysisException {

		ExceptionSet exceptionTypeSet = new ExceptionSet();
		InstructionHandle pei = basicBlock.getExceptionThrower();
		Instruction ins = pei.getInstruction();

		// Get the exceptions that BCEL knows about.
		// Note that all of these are unchecked.
		ExceptionThrower exceptionThrower = (ExceptionThrower) ins;
		Class[] exceptionList = exceptionThrower.getExceptions();
		for (int i = 0; i < exceptionList.length; ++i) {
			exceptionTypeSet.addImplicit(new ObjectType(exceptionList[i].getName()));
		}

		// Assume that an Error may be thrown by any instruction.
		exceptionTypeSet.addImplicit(Hierarchy.ERROR_TYPE);

		if (ins instanceof ATHROW) {
			// For ATHROW instructions, we generate *two* blocks
			// for which the ATHROW is an exception thrower.
			//
			// - The first, empty basic block, does the null check
			// - The second block, which actually contains the ATHROW,
			//   throws the object on the top of the operand stack
			//
			// We make a special case of the block containing the ATHROW,
			// by removing all of the implicit exceptions,
			// and using type information to figure out what is thrown.

			if (basicBlock.containsInstruction(pei)) {
				// This is the actual ATHROW, not the null check
				// and implicit exceptions.
				exceptionTypeSet.clear();

				// The frame containing the thrown value is the start fact
				// for the block, because ATHROW is guaranteed to be
				// the only instruction in the block.
				TypeFrame frame = getStartFact(basicBlock);
	
				// Check whether or not the frame is valid.
				// Sun's javac sometimes emits unreachable code.
				// For example, it will emit code that follows a JSR
				// subroutine call that never returns.
				// If the frame is invalid, then we can just make
				// a conservative assumption that anything could be
				// thrown at this ATHROW.
				if (!frame.isValid()) {
					exceptionTypeSet.addExplicit(Type.THROWABLE);
				} else {
					Type throwType = frame.getTopValue();
					if (!(throwType instanceof ObjectType))
						throw new DataflowAnalysisException("Non object type thrown by " + pei);
					exceptionTypeSet.addExplicit((ObjectType) throwType);
				}
			}
		}

		// If it's an InvokeInstruction, add declared exceptions and RuntimeException
		if (ins instanceof InvokeInstruction) {
			ConstantPoolGen cpg = methodGen.getConstantPool();

			InvokeInstruction inv = (InvokeInstruction) ins;
			ObjectType[] declaredExceptionList = Hierarchy.findDeclaredExceptions(inv, cpg);
			if (declaredExceptionList == null) {
				// Couldn't find declared exceptions,
				// so conservatively assume it could thrown any checked exception.
				if (DEBUG) System.out.println("Couldn't find declared exceptions for " +
					SignatureConverter.convertMethodSignature(inv, cpg));
				exceptionTypeSet.addExplicit(Hierarchy.EXCEPTION_TYPE);
			} else {
				for (int i = 0; i < declaredExceptionList.length; ++i) {
					exceptionTypeSet.addExplicit(declaredExceptionList[i]);
				}
			}

			exceptionTypeSet.addImplicit(Hierarchy.RUNTIME_EXCEPTION_TYPE);
		}

		if (DEBUG) System.out.println(pei + " can throw " + exceptionTypeSet);

		return exceptionTypeSet;
	}
}

// vim:ts=4
