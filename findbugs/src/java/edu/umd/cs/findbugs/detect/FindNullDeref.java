/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

package edu.umd.cs.findbugs.detect;

import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.WarningSuppressor;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisException;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.IsNullValue;
import edu.umd.cs.findbugs.ba.IsNullValueAnalysis;
import edu.umd.cs.findbugs.ba.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.props.GeneralWarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.props.WarningPropertyUtil;

/**
 * A Detector to find instructions where a NullPointerException
 * might be raised.  We also look for useless reference comparisons
 * involving null and non-null values.
 *
 * @author David Hovemeyer
 * @see IsNullValueAnalysis
 */
public class FindNullDeref implements Detector, StatelessDetector {

	/**
	 * An instruction recorded as a redundant reference comparison.
	 * We keep track of the line number, in order to ensure that if
	 * the branch was duplicated, all duplicates are determined in
	 * the same way.  (If they aren't, then we don't report it.)
	 */
	private static class RedundantBranch {
		public final Location location;
		public final int lineNumber;
		public boolean checkedValue;

		public RedundantBranch(Location location, int lineNumber, boolean checkedValue) {
			this.location = location;
			this.lineNumber = lineNumber;
			this.checkedValue = checkedValue;
		}

		public String toString() {
			return location.toString() + ": line " + lineNumber;
		}
	}

	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug");

	private BugReporter bugReporter;
	private List<RedundantBranch> redundantBranchList;
	private BitSet definitelySameBranchSet;
	private BitSet definitelyDifferentBranchSet;
	private BitSet undeterminedBranchSet;

	public FindNullDeref(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.redundantBranchList = new LinkedList<RedundantBranch>();
		this.definitelySameBranchSet = new BitSet();
		this.definitelyDifferentBranchSet = new BitSet();
		this.undeterminedBranchSet = new BitSet();
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
	}

	public void visitClassContext(ClassContext classContext) {
		try {

			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative() || method.getCode() == null)
					continue;

				analyzeMethod(classContext, method);
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindNullDeref caught exception: " + e, e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException(e.getMessage());
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {

		if (DEBUG) System.out.println("Clearing redundant branch information");
		redundantBranchList.clear();
		definitelySameBranchSet.clear();
		definitelyDifferentBranchSet.clear();
		undeterminedBranchSet.clear();

		if (DEBUG)
			System.out.println(SignatureConverter.convertMethodSignature(classContext.getMethodGen(method)));

		// Get the IsNullValueAnalysis for the method from the ClassContext
		IsNullValueDataflow invDataflow = classContext.getIsNullValueDataflow(method);

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
					analyzeRefComparisonBranch(method, invDataflow, basicBlock, lastHandle);
					break;
				case Constants.IFNULL:
				case Constants.IFNONNULL:
					analyzeIfNullBranch(method, invDataflow, basicBlock, lastHandle);
					break;
				}
			}
		}

		Iterator<RedundantBranch> i = redundantBranchList.iterator();
		while (i.hasNext()) {
			RedundantBranch redundantBranch = i.next();
			if (DEBUG) System.out.println("Redundant branch: " + redundantBranch);
			int lineNumber = redundantBranch.lineNumber;

			// The source to bytecode compiler may sometimes duplicate blocks of
			// code along different control paths.  So, to report the bug,
			// we check to ensure that the branch is REALLY determined each
			// place it is duplicated, and that it is determined in the same way.
			if (!undeterminedBranchSet.get(lineNumber) &&
			        !(definitelySameBranchSet.get(lineNumber) && definitelyDifferentBranchSet.get(lineNumber))) {
				reportRedundantNullCheck(classContext, method, redundantBranch.location, redundantBranch);
			}
		}
	}

	private void analyzeNullCheck(ClassContext classContext, Method method, IsNullValueDataflow invDataflow,
	                              BasicBlock basicBlock)
	        throws DataflowAnalysisException {

		// Look for null checks where the value checked is definitely
		// null or null on some path.

		InstructionHandle exceptionThrowerHandle = basicBlock.getExceptionThrower();
		Instruction exceptionThrower = exceptionThrowerHandle.getInstruction();

		// Figurinformation out where the reference operand is in the stack frame.
		int consumed = exceptionThrower.consumeStack(classContext.getConstantPoolGen());
		if (consumed == Constants.UNPREDICTABLE)
			throw new DataflowAnalysisException("Unpredictable stack consumption for " + exceptionThrower);

		// Get the stack values at entry to the null check.
		IsNullValueFrame frame = invDataflow.getStartFact(basicBlock);
		if (!frame.isValid())
			return;

		// Could the reference be null?
		IsNullValue refValue = frame.getValue(frame.getNumSlots() - consumed);
		if (!refValue.mightBeNull())
			return;
	
		// Issue a warning
		issueNullDerefWarning(classContext, method, new Location(exceptionThrowerHandle, basicBlock), refValue);
	}

	private void issueNullDerefWarning(
			ClassContext classContext,
			Method method,
			Location location,
			IsNullValue refValue) {
		WarningPropertySet propertySet = new WarningPropertySet();
		
		boolean onExceptionPath = refValue.isException();
		if (onExceptionPath) {
			propertySet.addProperty(GeneralWarningProperty.ON_EXCEPTION_PATH);
		}
		
		if (refValue.isDefinitelyNull()) {
			String type = onExceptionPath ? "NP_ALWAYS_NULL_EXCEPTION" : "NP_ALWAYS_NULL";
			int priority = onExceptionPath ? LOW_PRIORITY : HIGH_PRIORITY;
			reportNullDeref(propertySet, classContext, method, location, type, priority);
		} else if (refValue.isNullOnSomePath()) {
			String type = onExceptionPath ? "NP_NULL_ON_SOME_PATH_EXCEPTION" : "NP_NULL_ON_SOME_PATH";
			int priority = onExceptionPath ? LOW_PRIORITY : NORMAL_PRIORITY;
			if (DEBUG) System.out.println("Reporting null on some path: value=" + refValue);
			reportNullDeref(propertySet, classContext, method, location, type, priority);
		}
	}

	private void analyzeRefComparisonBranch(
			Method method,
			IsNullValueDataflow invDataflow,
			BasicBlock basicBlock,
			InstructionHandle lastHandle) throws DataflowAnalysisException {
		
		Location location = new Location(lastHandle, basicBlock);

		IsNullValueFrame frame = invDataflow.getFactAtLocation(location);
		if (!frame.isValid()) {
			// Probably dead code due to pruning infeasible exception edges.
			return;
		}
		if (frame.getStackDepth() < 2)
			throw new AnalysisException("Stack underflow at " + lastHandle);

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

			// If at least one of the values compared was
			// the result of an explicit null value or null check,
			// remember it.  We report these cases as low
			// priority.
			boolean checkedValue = top.isChecked() || topNext.isChecked();

			//reportRedundantNullCheck(classContext, method, lastHandle);
			RedundantBranch redundantBranch = new RedundantBranch(location, lineNumber, checkedValue);
			if (DEBUG) System.out.println("Adding redundant branch: " + redundantBranch);
			redundantBranchList.add(redundantBranch);
		} else {
			if (DEBUG) System.out.println("Line " + lineNumber + " undetermined");
			undeterminedBranchSet.set(lineNumber);
		}
	}

	// This is called for both IFNULL and IFNONNULL instructions.
	private void analyzeIfNullBranch(
			Method method,
			IsNullValueDataflow invDataflow,
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

		// Is this a null check made redundant by an earlier check?
		// Such code is not as likely to be an error
		// as when a check is done after an explicit dereference.
		boolean redundantNullCheck = top.isChecked();

		RedundantBranch redundantBranch = new RedundantBranch(location, lineNumber, redundantNullCheck);
		if (DEBUG) System.out.println("Adding redundant branch: " + redundantBranch);
		redundantBranchList.add(redundantBranch);
	}

	private static int getLineNumber(Method method, InstructionHandle handle) {
		LineNumberTable table = method.getCode().getLineNumberTable();
		if (table == null)
			return -1;
		return table.getSourceLine(handle.getPosition());
	}

	private void reportNullDeref(
			WarningPropertySet propertySet,
			ClassContext classContext,
			Method method,
			Location location,
			String type,
			int priority) {
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		BugInstance bugInstance = new BugInstance(this, type, priority)
		        .addClassAndMethod(methodGen, sourceFile)
		        .addSourceLine(methodGen, sourceFile, location.getHandle());

		if (DEBUG)
			bugInstance.addInt(location.getHandle().getPosition()).describe("INT_BYTECODE_OFFSET");

		if (AnalysisContext.currentAnalysisContext().getBoolProperty(
				FindBugsAnalysisProperties.RELAXED_REPORTING_MODE)) {
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			propertySet.decorateBugInstance(bugInstance);
		}
		
		bugReporter.reportBug(bugInstance);
	}

	private void reportRedundantNullCheck(
			ClassContext classContext,
			Method method,
			Location location,
			RedundantBranch redundantBranch) {
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		MethodGen methodGen = classContext.getMethodGen(method);

		boolean redundantNullCheck = redundantBranch.checkedValue;
//		String type = redundantNullCheck
//		        ? "RCN_REDUNDANT_CHECKED_NULL_COMPARISON"
//		        : "RCN_REDUNDANT_COMPARISON_TO_NULL";
		int priority = redundantNullCheck ? LOW_PRIORITY : NORMAL_PRIORITY;
		
		BugInstance bugInstance =
			new BugInstance(this, "RCN_REDUNDANT_COMPARISON_TO_NULL", priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, location.getHandle());
		
		if (AnalysisContext.currentAnalysisContext().getBoolProperty(
				FindBugsAnalysisProperties.RELAXED_REPORTING_MODE)) {
			WarningPropertySet propertySet = new WarningPropertySet();
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			if (redundantBranch.checkedValue) {
				propertySet.addProperty(NullDerefProperty.CHECKED_VALUE);
			}
			
			propertySet.decorateBugInstance(bugInstance);
			
			priority = propertySet.computePriority(NORMAL_PRIORITY);
			bugInstance.setPriority(priority);
		}

		bugReporter.reportBug(bugInstance);
	}

	public void report() {
	}

}

// vim:ts=4
