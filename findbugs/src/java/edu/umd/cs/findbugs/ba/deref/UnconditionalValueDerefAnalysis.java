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

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
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
	
	private CFG cfg;
	private MethodGen methodGen;
	private ValueNumberDataflow vnaDataflow;
	
	/**
	 * Constructor.
	 * 
	 * @param rdfs               the reverse depth-first-search
	 * @param cfg                the CFG for the method
	 * @param methodGen          the MethodGen for the method
	 * @param valueNumberFactory the value number factory
	 */
	public UnconditionalValueDerefAnalysis(
			ReverseDepthFirstSearch rdfs,
			CFG cfg,
			MethodGen methodGen,
			ValueNumberDataflow vnaDataflow) {
		super(rdfs);
		this.cfg = cfg;
		this.methodGen = methodGen;
		this.vnaDataflow = vnaDataflow;
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

		// Mark the value number as being dereferenced at this location
		fact.addDeref(vn, new Location(handle, basicBlock));
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
		
		// Ignore implicit exceptions
		if (AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.ACCURATE_EXCEPTIONS)
				&& edge.isExceptionEdge()
				&& !edge.isFlagSet(EdgeTypes.EXPLICIT_EXCEPTIONS_FLAG)) {
			return;
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
		driver.execute(args[0]);
	}
}
