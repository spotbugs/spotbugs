/*
 * Bytecode analysis framework
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A user-friendly front end for finding null pointer dereferences
 * and redundant null comparisions.
 * 
 * @see IsNullValueAnalysis
 * @author David Hovemeyer
 */
public class NullDerefAndRedundantComparisonFinder {
	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug");
	
	private ClassContext classContext;
	private Method method;
	private IsNullValueDataflow invDataflow;
	private NullDerefAndRedundantComparisonCollector collector;
	
	private List<RedundantBranch> redundantBranchList;
	private BitSet definitelySameBranchSet;
	private BitSet definitelyDifferentBranchSet;
	private BitSet undeterminedBranchSet;
	private BitSet lineMentionedMultipleTimes;

	static {
		if (DEBUG) System.out.println("fnd.debug enabled");
	}
	/**
	 * Constructor.
	 * 
	 * @param classContext the ClassContext
	 * @param method       the method to analyze
	 * @param invDataflow  the IsNullValueDataflow to use
	 * @param collector    the NullDerefAndRedundantComparisonCollector used to report
	 *                     null derefs and redundant null comparisons
	 */
	public NullDerefAndRedundantComparisonFinder(
			ClassContext classContext,
			Method method,
			IsNullValueDataflow invDataflow,
			NullDerefAndRedundantComparisonCollector collector) {
		
		this.classContext = classContext;
		this.method = method;
		this.invDataflow = invDataflow;
		this.collector = collector;
		this.lineMentionedMultipleTimes = ClassContext.linesMentionedMultipleTimes(method);
		
		this.redundantBranchList = new LinkedList<RedundantBranch>();
		this.definitelySameBranchSet = new BitSet();
		this.definitelyDifferentBranchSet = new BitSet();
		this.undeterminedBranchSet = new BitSet();
	}
	
	public void execute() throws DataflowAnalysisException, CFGBuilderException {
		// Look for null check blocks where the reference being checked
		// is definitely null, or null on some path
		Iterator<BasicBlock> bbIter = invDataflow.getCFG().blockIterator();
		while (bbIter.hasNext()) {
			BasicBlock basicBlock = bbIter.next();

			if (basicBlock.isNullCheck()) {
				analyzeNullCheck(classContext, method, invDataflow, basicBlock);
			} else if (!basicBlock.isEmpty()) {
				// Look for all reference comparisons where
				//    - both values compared are definitely null, or
				//    - one value is definitely null and one is definitely not null
				// These cases are not null dereferences,
				// but they are quite likely to indicate an error, so while we've got
				// information about null values, we may as well report them.
				InstructionHandle lastHandle = basicBlock.getLastInstruction();
				Instruction last = lastHandle.getInstruction();
				switch (last.getOpcode()) {
				case Constants.IF_ACMPEQ:
				case Constants.IF_ACMPNE:
					analyzeRefComparisonBranch(basicBlock, lastHandle);
					break;
				case Constants.IFNULL:
				case Constants.IFNONNULL:
					analyzeIfNullBranch(basicBlock, lastHandle);
					break;
				}
			}
		}

		for (RedundantBranch redundantBranch : redundantBranchList) {
			if (DEBUG) System.out.println("Redundant branch: " + redundantBranch);
			int lineNumber = redundantBranch.lineNumber;

			// The source to bytecode compiler may sometimes duplicate blocks of
			// code along different control paths.  So, to report the bug,
			// we check to ensure that the branch is REALLY determined each
			// place it is duplicated, and that it is determined in the same way.

			boolean confused = undeterminedBranchSet.get(lineNumber) ||
					(definitelySameBranchSet.get(lineNumber) && definitelyDifferentBranchSet.get(lineNumber));

			// confused if there is JSR confusion or multiple null checks with different results on the same line

			boolean reportIt = true;
			if (lineMentionedMultipleTimes.get(lineNumber) && confused)
				reportIt = false;
			if (redundantBranch.location.getBasicBlock().isInJSRSubroutine() /* occurs in a JSR */
					&& confused)
				reportIt = false;
			if (reportIt) {
				collector.foundRedundantNullCheck(redundantBranch.location, redundantBranch);
			}
		}

	}

	private void analyzeRefComparisonBranch(
			BasicBlock basicBlock,
			InstructionHandle lastHandle) throws DataflowAnalysisException {
		
		Location location = new Location(lastHandle, basicBlock);

		IsNullValueFrame frame = invDataflow.getFactAtLocation(location);
		if (!frame.isValid()) {
			// Probably dead code due to pruning infeasible exception edges.
			return;
		}
		if (frame.getStackDepth() < 2)
			throw new DataflowAnalysisException("Stack underflow at " + lastHandle);

		// Find the line number.
		int lineNumber = getLineNumber(method, lastHandle);
		if (lineNumber < 0)
			return;

		int numSlots = frame.getNumSlots();
		IsNullValue top = frame.getValue(numSlots - 1);
		IsNullValue topNext = frame.getValue(numSlots - 2);

		boolean definitelySame = top.isDefinitelyNull() && topNext.isDefinitelyNull();
		boolean definitelyDifferent =
		        (top.isDefinitelyNull() && topNext.isDefinitelyNotNull()) ||
		        (top.isDefinitelyNotNull() && topNext.isDefinitelyNull());

		if (definitelySame || definitelyDifferent) {
			if (definitelySame) {
				if (DEBUG) System.out.println("Line " + lineNumber + " always same");
				definitelySameBranchSet.set(lineNumber);
			}
			if (definitelyDifferent) {
				if (DEBUG) System.out.println("Line " + lineNumber + " always different");
				definitelyDifferentBranchSet.set(lineNumber);
			}

			RedundantBranch redundantBranch = new RedundantBranch(location, lineNumber, top, topNext);
			
			// Figure out which control edge is made infeasible by the redundant comparison
			boolean wantSame = (lastHandle.getInstruction().getOpcode() == Constants.IF_ACMPEQ);
			int infeasibleEdgeType = (wantSame == definitelySame)
					? EdgeTypes.FALL_THROUGH_EDGE : EdgeTypes.IFCMP_EDGE;
			Edge infeasibleEdge = invDataflow.getCFG().getOutgoingEdgeWithType(basicBlock, infeasibleEdgeType);
			redundantBranch.setInfeasibleEdge(infeasibleEdge);
			
			if (DEBUG) System.out.println("Adding redundant branch: " + redundantBranch);
			redundantBranchList.add(redundantBranch);
		} else {
			if (DEBUG) System.out.println("Line " + lineNumber + " undetermined");
			undeterminedBranchSet.set(lineNumber);
		}
	}

	// This is called for both IFNULL and IFNONNULL instructions.
	private void analyzeIfNullBranch(
			BasicBlock basicBlock,
			InstructionHandle lastHandle) throws DataflowAnalysisException {
		
		Location location = new Location(lastHandle, basicBlock);

		IsNullValueFrame frame = invDataflow.getFactAtLocation(location);

		if (!frame.isValid()) {
			// This is probably dead code due to an infeasible exception edge.
			return;
		}

		IsNullValue top = frame.getTopValue();

		// Find the line number.
		int lineNumber = getLineNumber(method, lastHandle);
		if (lineNumber < 0)
			return;
		
		if (!(top.isDefinitelyNull() || top.isDefinitelyNotNull())) {
			if (DEBUG) System.out.println("Line " + lineNumber + " undetermined");
			undeterminedBranchSet.set(lineNumber);
			return;
		}

		// Figure out if the branch is always taken
		// or always not taken.
		short opcode = lastHandle.getInstruction().getOpcode();
		boolean definitelySame = top.isDefinitelyNull();
		if (opcode != Constants.IFNULL) definitelySame = !definitelySame;

		if (definitelySame) {
			if (DEBUG) System.out.println("Line " + lineNumber + " always same");
			definitelySameBranchSet.set(lineNumber);
		} else {
			if (DEBUG) System.out.println("Line " + lineNumber + " always different");
			definitelyDifferentBranchSet.set(lineNumber);
		}

		RedundantBranch redundantBranch = new RedundantBranch(location, lineNumber, top);
		
		// Determine which control edge is made infeasible by the redundant comparison
		boolean wantNull = (opcode == Constants.IFNULL);
		int infeasibleEdgeType = (wantNull == top.isDefinitelyNull())
				? EdgeTypes.FALL_THROUGH_EDGE : EdgeTypes.IFCMP_EDGE;
		Edge infeasibleEdge = invDataflow.getCFG().getOutgoingEdgeWithType(basicBlock, infeasibleEdgeType);
		redundantBranch.setInfeasibleEdge(infeasibleEdge);
		
		if (DEBUG) System.out.println("Adding redundant branch: " + redundantBranch);
		redundantBranchList.add(redundantBranch);
	}

	private void analyzeNullCheck(ClassContext classContext, Method method, IsNullValueDataflow invDataflow,
            BasicBlock basicBlock)
		throws DataflowAnalysisException, CFGBuilderException {
		// Look for null checks where the value checked is definitely
		// null or null on some path.
		
		InstructionHandle exceptionThrowerHandle = basicBlock.getExceptionThrower();
		Instruction exceptionThrower = exceptionThrowerHandle.getInstruction();
		
		// Get the stack values at entry to the null check.
		IsNullValueFrame frame = invDataflow.getStartFact(basicBlock);
		if (!frame.isValid())
			return;

		
		// Could the reference be null?
		IsNullValue refValue = frame.getInstance(exceptionThrower, classContext.getConstantPoolGen());
		if (!refValue.mightBeNull())
			return;
		
		// Get the value number
		ValueNumberFrame vnaFrame = classContext.getValueNumberDataflow(method).getStartFact(basicBlock);
		if (!vnaFrame.isValid())
			return;
		ValueNumber valueNumber = vnaFrame.getInstance(exceptionThrower, classContext.getConstantPoolGen());
		
		// Issue a warning
		collector.foundNullDeref(new Location(exceptionThrowerHandle, basicBlock), valueNumber, refValue);
	}

	private static int getLineNumber(Method method, InstructionHandle handle) {
		LineNumberTable table = method.getCode().getLineNumberTable();
		if (table == null)
			return -1;
		return table.getSourceLine(handle.getPosition());
	}
}
