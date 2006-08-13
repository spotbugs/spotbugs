/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.deref;

import java.util.BitSet;
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.npe.IsNullConditionDecision;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFactory;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Dataflow analysis to find values unconditionally derefenced in the future.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalValueDerefAnalysis extends
		BackwardDataflowAnalysis<UnconditionalValueDerefSet> {
	
	private static final boolean DEBUG = SystemProperties.getBoolean("fnd.derefs.debug");
	
	private DepthFirstSearch dfs;
	private CFG cfg;
	private MethodGen methodGen;
	private ValueNumberDataflow vnaDataflow;
	private AssertionMethods assertionMethods;
	
	private IsNullValueDataflow invDataflow;
	
	/**
	 * Constructor.
	 * 
	 * @param rdfs               the reverse depth-first-search (for the block order)
	 * @param dfs                the forward depth-first-search
	 * @param cfg                the CFG for the method
	 * @param methodGen          the MethodGen for the method
	 * @param valueNumberFactory the value number factory
	 * @param assertionMethods   AssertionMethods for the analyzed class
	 */
	public UnconditionalValueDerefAnalysis(
			ReverseDepthFirstSearch rdfs,
			DepthFirstSearch dfs,
			CFG cfg,
			MethodGen methodGen,
			ValueNumberDataflow vnaDataflow,
			AssertionMethods assertionMethods
			) {
		super(rdfs);
		this.dfs = dfs;
		this.cfg = cfg;
		this.methodGen = methodGen;
		this.vnaDataflow = vnaDataflow;
		this.assertionMethods = assertionMethods;
	}
	
	/**
	 * HACK: use the given is-null dataflow to clear deref sets for
	 * values that are known to be definitely non-null on a branch.
	 * 
	 * @param invDataflow the IsNullValueDataflow to use
	 */
	public void clearDerefsOnNonNullBranches(IsNullValueDataflow invDataflow) {
		this.invDataflow = invDataflow;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#isFactValid(java.lang.Object)
	 */
	@Override
	public boolean isFactValid(UnconditionalValueDerefSet fact) {
		return !fact.isTop() && !fact.isBottom();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transferInstruction(org.apache.bcel.generic.InstructionHandle, edu.umd.cs.findbugs.ba.BasicBlock, java.lang.Object)
	 */
	@Override
	public void transferInstruction(InstructionHandle handle,
			BasicBlock basicBlock, UnconditionalValueDerefSet fact)
			throws DataflowAnalysisException {
		
		// If this is a call to an assertion method,
		// change the dataflow value to be TOP.
		// We don't want to report future derefs that would
		// be guaranteed only if the assertion methods
		// returns normally.
		if (isAssertion(handle) || handle.getInstruction() instanceof ATHROW) {
			makeFactTop(fact);
			return;
		}

		// See if this instruction has a null check.
		// If it does, the fall through predecessor will be
		// identify itself as the null check.
		if (handle != basicBlock.getFirstInstruction()) {
			return;
		}
		BasicBlock fallThroughPredecessor =
			cfg.getPredecessorWithEdgeType(basicBlock, EdgeTypes.FALL_THROUGH_EDGE);
		if (fallThroughPredecessor == null || !fallThroughPredecessor.isNullCheck()) {
			return;
		}
		
		// Get the null-checked value
		ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(fallThroughPredecessor);
		if (!vnaFrame.isValid()) {
			// Probably dead code.
			// Assume this location can't be reached.
			makeFactTop(fact);
			return;
		}
		ValueNumber vn = vnaFrame.getInstance(handle.getInstruction(), methodGen.getConstantPool()); 
		Location location = new Location(handle, basicBlock);
		
		if (DEBUG) {
			System.out.println("FOUND GUARANTEED DEREFERENCE");
			System.out.println("Location: " + location);
			System.out.println("Value number frame: " + vnaFrame);
			System.out.println("Dereferenced valueNumber: " + vn);
		}
		// Mark the value number as being dereferenced at this location
		fact.addDeref(vn, location);
	}

	/**
	 * @param handle
	 * @return
	 */
	private boolean isAssertion(InstructionHandle handle) {
		return handle.getInstruction() instanceof InvokeInstruction
				&& assertionMethods.isAssertionCall((InvokeInstruction) handle.getInstruction());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	public void copy(UnconditionalValueDerefSet source, UnconditionalValueDerefSet dest) {
		dest.makeSameAs(source);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#createFact()
	 */
	public UnconditionalValueDerefSet createFact() {
		return new UnconditionalValueDerefSet(vnaDataflow.getAnalysis().getNumValuesAllocated());
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
	 */
	public void initEntryFact(UnconditionalValueDerefSet result)
			throws DataflowAnalysisException {
		result.clear();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initResultFact(java.lang.Object)
	 */
	public void initResultFact(UnconditionalValueDerefSet result) {
		result.setIsTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(java.lang.Object)
	 */
	public void makeFactTop(UnconditionalValueDerefSet fact) {
		fact.setIsTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(java.lang.Object, edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	public void meetInto(UnconditionalValueDerefSet fact, Edge edge,
			UnconditionalValueDerefSet result) throws DataflowAnalysisException {
		
		if (ignoreThisEdge(edge)) {
			return;
		}
		
		// Edge transfer function
		if (isFactValid(fact)) {
			fact = propagateDerefSetsToMergeInputValues(fact, edge);
			if (invDataflow != null) {
				fact = clearDerefsOnNonNullBranch(fact, edge);
			}
		}

		if (result.isTop() || fact.isBottom()) {
			// Make result identical to other fact
			copy(fact, result);
		} else if (result.isBottom() || fact.isTop()) {
			// No change in result fact
		} else {
			// Dataflow merge
			// (intersection of unconditional deref values)
			result.mergeWith(fact, vnaDataflow.getAnalysis().getFactory());
		}
	}

	/**
	 * Find out if any VNs in the source block
	 * contribute to unconditionally dereferenced VNs in the
	 * target block.  If so, the VN in the source block is
	 * also unconditionally dereferenced, and we must propagate
	 * the target VN's dereferences.
	 *  
	 * @param fact a dataflow value
	 * @param edge edge to check for merge input values
	 * @return possibly-modified dataflow value
	 */
	private UnconditionalValueDerefSet propagateDerefSetsToMergeInputValues(
			UnconditionalValueDerefSet fact, Edge edge) {
		
		ValueNumberFrame blockValueNumberFrame =
			vnaDataflow.getResultFact(edge.getSource());
		ValueNumberFrame targetValueNumberFrame =
			vnaDataflow.getStartFact(edge.getTarget());

		fact = duplicateFact(fact);

		if (blockValueNumberFrame.isValid() && targetValueNumberFrame.isValid() &&
				blockValueNumberFrame.getNumSlots() == targetValueNumberFrame.getNumSlots()) {
			if (false && DEBUG) {
				System.out.println("** Valid VNA frames");
				System.out.println("** Block : " + blockValueNumberFrame);
				System.out.println("** Target: " + targetValueNumberFrame);
			}

			for (int i = 0; i < blockValueNumberFrame.getNumSlots(); i++) {
				ValueNumber blockVN = blockValueNumberFrame.getValue(i);
				ValueNumber targetVN = targetValueNumberFrame.getValue(i);
				if (!blockVN.equals(targetVN)) {
					if (DEBUG) {
						System.out.println("Merge: " + targetVN + " -> " + blockVN);
					}
					if (fact.isUnconditionallyDereferenced(targetVN)
							&& !fact.isUnconditionallyDereferenced(blockVN)) {
						// Block VN is also dereferenced unconditionally.
						if (DEBUG) {
							System.out.println("** Copy vn derefs " + targetVN.getNumber() + 
									" --> " + blockVN.getNumber());
						}
						fact.setDerefSet(blockVN, fact.getUnconditionalDerefLocationSet(targetVN));
					}
				}
			}
		}
		return fact;
	}

	/**
	 * Return a duplicate of given dataflow fact.
	 * 
	 * @param fact a dataflow fact
	 * @return a duplicate of the input dataflow fact
	 */
	private UnconditionalValueDerefSet duplicateFact(UnconditionalValueDerefSet fact) {
		UnconditionalValueDerefSet copyOfFact = createFact();
		copy(fact, copyOfFact);
		fact = copyOfFact;
		return fact;
	}

	/**
	 * Clear deref sets of values if this edge is the non-null branch
	 * of an if comparison.
	 * 
	 * @param fact a datflow fact
	 * @param edge edge to check
	 * @return possibly-modified dataflow fact
	 */
	private UnconditionalValueDerefSet clearDerefsOnNonNullBranch(
			UnconditionalValueDerefSet fact, Edge edge) {
		
		IsNullValueFrame invFrame = invDataflow.getResultFact(edge.getSource());
		if (!invFrame.isValid()) {
			return fact;
		}
		IsNullConditionDecision decision = invFrame.getDecision();
		if (decision == null) {
			return fact;
		}
		
		IsNullValue inv = decision.getDecision(edge.getType());
		if (inv == null || !inv.isDefinitelyNotNull()) {
			return fact;
		}
		ValueNumber value = decision.getValue();
		
		fact = duplicateFact(fact);
		fact.clearDerefSet(value);
		
		return fact;
	}

	/**
	 * Determine whether dataflow should be propagated on given edge.
	 * 
	 * @param edge the edge
	 * @return true if dataflow should be propagated on the edge, false otherwise
	 */
	private boolean ignoreThisEdge(Edge edge) {
		return edge.isExceptionEdge();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(java.lang.Object, java.lang.Object)
	 */
	public boolean same(UnconditionalValueDerefSet fact1, UnconditionalValueDerefSet fact2) {
		return fact1.isSameAs(fact2);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + UnconditionalValueDerefAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}
		
		DataflowTestDriver<UnconditionalValueDerefSet, UnconditionalValueDerefAnalysis> driver =
			new DataflowTestDriver<UnconditionalValueDerefSet, UnconditionalValueDerefAnalysis>() {
			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.ba.DataflowTestDriver#createDataflow(edu.umd.cs.findbugs.ba.ClassContext, org.apache.bcel.classfile.Method)
			 */
			@Override
			public Dataflow<UnconditionalValueDerefSet, UnconditionalValueDerefAnalysis> createDataflow(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getUnconditionalValueDerefDataflow(method);
			}
		};
		if (SystemProperties.getBoolean("forwardcfg")) {
			driver.overrideIsForwards();
		}
		driver.execute(args[0]);
	}
}
