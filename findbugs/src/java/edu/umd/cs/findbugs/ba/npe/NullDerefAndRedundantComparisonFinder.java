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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
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
	private static final boolean DEBUG_DEREFS = Boolean.getBoolean("fnd.derefs.debug");
	private static final boolean FIND_GUARANTEED_DEREFS = Boolean.getBoolean("fnd.derefs");
	
	private ClassContext classContext;
	private Method method;
	private NullDerefAndRedundantComparisonCollector collector;
	
	private List<RedundantBranch> redundantBranchList;
	private BitSet definitelySameBranchSet;
	private BitSet definitelyDifferentBranchSet;
	private BitSet undeterminedBranchSet;
	private BitSet lineMentionedMultipleTimes;

	private IsNullValueDataflow invDataflow;
	private ValueNumberDataflow vnaDataflow;
	private UnconditionalValueDerefDataflow uvdDataflow;

	static {
		if (DEBUG) System.out.println("fnd.debug enabled");
	}
	/**
	 * Constructor.
	 * 
	 * @param classContext the ClassContext
	 * @param method       the method to analyze
	 * @param collector    the NullDerefAndRedundantComparisonCollector used to report
	 *                     null derefs and redundant null comparisons
	 */
	public NullDerefAndRedundantComparisonFinder(
			ClassContext classContext,
			Method method,
			NullDerefAndRedundantComparisonCollector collector) {
		
		this.classContext = classContext;
		this.method = method;
		this.collector = collector;
		this.lineMentionedMultipleTimes = ClassContext.linesMentionedMultipleTimes(method);
		
		this.redundantBranchList = new LinkedList<RedundantBranch>();
		this.definitelySameBranchSet = new BitSet();
		this.definitelyDifferentBranchSet = new BitSet();
		this.undeterminedBranchSet = new BitSet();
	}
	
	public void execute() throws DataflowAnalysisException, CFGBuilderException {
		// Do the null-value analysis
		this.invDataflow = classContext.getIsNullValueDataflow(method);
		this.vnaDataflow = classContext.getValueNumberDataflow(method);
		if (FIND_GUARANTEED_DEREFS) {
			if (DEBUG_DEREFS) {
				System.out.println(
						"Checking for guaranteed derefs in " +
						classContext.getCFG(method).getMethodName());
			}
			this.uvdDataflow = classContext.getUnconditionalValueDerefDataflow(method);
		}

		// Check method and report potential null derefs and
		// redundant null comparisons.
		examineBasicBlocks();
		if (FIND_GUARANTEED_DEREFS) {
			examineNullValues();
		}
		examineRedundantBranches();

	}

	/**
	 * Examine basic blocks for null checks and potentially-redundant
	 * null comparisons.
	 * 
	 * @throws DataflowAnalysisException
	 * @throws CFGBuilderException
	 */
	private void examineBasicBlocks() throws DataflowAnalysisException, CFGBuilderException {
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
	}
	
	static class NullValueInfo {
		Set<Location> valueBecomesNullLocationSet;
		Set<Location> valueDereferencedSet;
	}
	
	/**
	 * Examine null values.
	 * Report any that are guaranteed to be dereferenced on
	 * non-implicit-exception paths.
	 * 
	 * @throws CFGBuilderException 
	 * @throws DataflowAnalysisException 
	 */
	private void examineNullValues() throws CFGBuilderException, DataflowAnalysisException {
		Set<LocationWhereValueBecomesNull> locationWhereValueBecomesNullSet =
			invDataflow.getAnalysis().getLocationWhereValueBecomesNullSet();

		// For each value number that is null somewhere in the
		// method, collect the set of locations where it becomes null.
		// FIXME: we may see some locations that are not guaranteed to be dereferenced (how to fix this?)
		Map<ValueNumber, Set<Location>> nullValueAssignmentMap =
			new HashMap<ValueNumber, Set<Location>>();
		for (LocationWhereValueBecomesNull lwvbn : locationWhereValueBecomesNullSet) {
			Set<Location> locationSet = nullValueAssignmentMap.get(lwvbn.getValueNumber());
			if (locationSet == null) {
				locationSet = new HashSet<Location>();
				nullValueAssignmentMap.put(lwvbn.getValueNumber(), locationSet);
			}
			locationSet.add(lwvbn.getLocation());
		}
		
		// Inspect the method for locations where a null value is guaranteed to
		// be dereferenced.  Add the dereference locations
		Map<ValueNumber, Set<Location>> nullValueGuaranteedDerefMap =
			new HashMap<ValueNumber, Set<Location>>();
		
		// Check every location
		for (Iterator<Location> i = classContext.getCFG(method).locationIterator(); i.hasNext();) {
			Location location = i.next();
			
			if (DEBUG_DEREFS) {
				System.out.println("At location " + location.getBasicBlock().getId() + ":" +
						location.getHandle().getPosition());
			}
			
			checkForUnconditionallyDereferencedNullValues(
					nullValueGuaranteedDerefMap,
					vnaDataflow.getFactAtLocation(location),
					invDataflow.getFactAtLocation(location),
					uvdDataflow.getFactAfterLocation(location));
		}
		
		// Check every non-exception control edge
		for (Iterator<Edge> i = classContext.getCFG(method).edgeIterator(); i.hasNext();) {
			Edge edge = i.next();
			
			if (DEBUG_DEREFS) {
				System.out.println("On edge " + edge.formatAsString(false));
			}
			
			if (!edge.isExceptionEdge()) {
				checkForUnconditionallyDereferencedNullValues(
						nullValueGuaranteedDerefMap,
						vnaDataflow.getFactOnEdge(edge),
						invDataflow.getFactOnEdge(edge),
						uvdDataflow.getFactOnEdge(edge));
			}
		}
		
		// Report 
		for (Map.Entry<ValueNumber, Set<Location>> e  : nullValueGuaranteedDerefMap.entrySet()) {
			ValueNumber valueNumber = e.getKey();
			Set<Location> derefLocationSet = e.getValue();
			Set<Location> assignedNullLocationSet = nullValueAssignmentMap.get(valueNumber);
			if (assignedNullLocationSet == null)
				throw new RuntimeException("No assigned NullLocationSet for " + valueNumber + " in " + nullValueAssignmentMap.keySet());
			
			collector.foundGuaranteedNullDeref(assignedNullLocationSet, derefLocationSet, valueNumber);
		}
	}

	/**
	 * Check for unconditionally dereferenced null values
	 * at a particular location in the CFG.
	 * 
	 * @param nullValueGuaranteedDerefMap map to be populated with null values and where they are derefed 
	 * @param vnaFrame                    value number frame to check
	 * @param invFrame                    null-value frame to check
	 * @param derefSet                    set of unconditionally derefed values at this location 
	 */
	private void checkForUnconditionallyDereferencedNullValues(
			Map<ValueNumber, Set<Location>> nullValueGuaranteedDerefMap,
			ValueNumberFrame vnaFrame,
			IsNullValueFrame invFrame,
			UnconditionalValueDerefSet derefSet) {
		
		if (DEBUG_DEREFS) {
			System.out.println("*** " + vnaFrame);
			System.out.println("*** " + invFrame);
			System.out.println("*** " + derefSet);
		}
		
		// Make sure the frames contain meaningful information
		if (!vnaFrame.isValid() || !invFrame.isValid() || vnaFrame.getNumSlots() != invFrame.getNumSlots())  {
			return;
		}

		// See if there are any definitely-null values in the frame
		for (int j = 0; j < invFrame.getNumSlots(); j++) {
			if (invFrame.getValue(j).isDefinitelyNull()) {
				// Is this value unconditionally dereferenced?
				ValueNumber valueNumber = vnaFrame.getValue(j);
				
				if (derefSet.isUnconditionallyDereferenced(valueNumber)) {
					if (DEBUG_DEREFS) {
						System.out.println("%%% HIT for value number " + valueNumber);
					}
					
					// OK, we have a null value that is unconditionally
					// derferenced.  Make a note of the locations where it
					// will be dereferenced.
					Set<Location> thisValueNumberDerefLocationSet = nullValueGuaranteedDerefMap.get(valueNumber);
					if (thisValueNumberDerefLocationSet == null) {
						thisValueNumberDerefLocationSet = new HashSet<Location>();
						nullValueGuaranteedDerefMap.put(valueNumber, thisValueNumberDerefLocationSet);
					}
					thisValueNumberDerefLocationSet.addAll(derefSet.getUnconditionalDerefLocationSet(valueNumber));
				}
			}
		}
	}

	/**
	 * Examine redundant branches.
	 */
	private void examineRedundantBranches() {
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