/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * Find instances of double check locking by enumerating the simple
 * paths through methods of a class.
 */
public class SimplePathsFindDoubleCheck extends SimplePathEnumeratingDetector implements EdgeTypes {

	private BugReporter bugReporter;

	private static final boolean DEBUG = Boolean.getBoolean("spfdc.debug");

	/**
	 * Constructor.
	 * @param bugReporter the BugReporter to use to report double checks
	 */
	public SimplePathsFindDoubleCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/**
	 * Pre-screening hook.
	 * We can avoid the expensive step of building a CFG and enumerating
	 * simple paths if there are no MONITORENTER/MONITOREXIT instructions
	 * in the method, or if there are no field accesses.
	 * This more than halves the amount of time consumed by this
	 * visitor on the JDK 1.4.1 rt.jar.
	 *
	 * @param mg the method to screen
	 * @return true if the method should be analyzed, false if it should be ignored
	 */
	public boolean preScreen(MethodGen mg) {
		boolean sawLock = false, sawGetfield = false, sawPutfield = false;

		InstructionList il = mg.getInstructionList();
		InstructionHandle handle = il.getStart();

		while (handle != null && !(sawLock && sawGetfield && sawPutfield)) {
			Instruction ins = handle.getInstruction();

			if (ins instanceof MONITORENTER || ins instanceof MONITOREXIT)
				sawLock = true;
			else if (ins instanceof GETFIELD || ins instanceof GETSTATIC)
				sawGetfield = true;
			else if (ins instanceof PUTFIELD || ins instanceof PUTSTATIC)
				sawPutfield = true;

			handle = handle.getNext();
		}

		return sawLock && sawGetfield && sawPutfield;
	}

	// Scanning states for doublecheck
	private static final int START_STATE = 0;
	private static final int GETFIELD1_STATE = 1;
	private static final int IFNULL1_STATE = 2;
	private static final int LOCK_STATE = 3;
	private static final int GETFIELD2_STATE = 4;
	private static final int IFNULL2_STATE = 5;
	private static final int PUTFIELD_STATE = 6;
	private static final int FINAL_STATE = 7;

	/**
	 * This class is the workhorse.
	 * It scans the edges and instructions of a simple path
	 * looking for probable instances of double check locking,
	 * using a simple state machine.
	 */
	private class DoubleCheckInstructionScanner implements InstructionScanner {
		private MethodGen methodGen;
		private ConstantPoolGen cpg;
		private int state;
		private FieldAnnotation field;
		private InstructionHandle start, end;

		/**
		 * Constructor.
		 * @param methodGen the method containing the simple path
		 */
		public DoubleCheckInstructionScanner(MethodGen methodGen) {
			this.methodGen = methodGen;
			cpg = methodGen.getConstantPool();
			state = START_STATE;
		}

		/**
		 * Handle an edge in the simple path.
		 */
		public void traverseEdge(Edge edge) {
			if (DEBUG) System.out.println("Examining edge: " + edge);
			switch (state) {
			case GETFIELD1_STATE:
				if (isNullBranch(edge)) {
					if (DEBUG) debug(edge.getSource().getLastInstruction().getInstruction(), "GETFIELD1 -> IFNULL1");
					state = IFNULL1_STATE;
				}
				break;
			case GETFIELD2_STATE:
				if (isNullBranch(edge)) {
					if (DEBUG) debug(edge.getSource().getLastInstruction().getInstruction(), "GETFIELD2 -> IFNULL2");
					state = IFNULL2_STATE;
				} else if (DEBUG)
					System.out.println("===> Not a null branch");
				break;
			}
		}

		/**
		 * Handle an instruction in the simple path.
		 */
		public void scanInstruction(InstructionHandle handle) {
			Instruction ins = handle.getInstruction();
			FieldAnnotation f;
			switch (state) {
			case START_STATE:
				if ((f = FieldAnnotation.isRead(ins, cpg)) != null) {
					if (DEBUG) debug(ins, "START -> GETFIELD1");
					field = f;
					state = GETFIELD1_STATE;
					start = handle;
				}
				else if (DEBUG) debug(ins, "not a read!");
				break;
			case IFNULL1_STATE:
				if (ins instanceof MONITORENTER) {
					if (DEBUG) debug(ins, "IFNULL1 -> LOCK");
					state = LOCK_STATE;
				}
				break;
			case LOCK_STATE:
				if ((f = FieldAnnotation.isRead(ins, cpg)) != null && f.equals(field)) {
					if (DEBUG) debug(ins, "LOCK -> GETFIELD2");
					state = GETFIELD2_STATE;
					end = handle;
				}
				break;
			case IFNULL2_STATE:
				if ((f = FieldAnnotation.isWrite(ins, cpg)) != null && f.equals(field)) {
					if (DEBUG) debug(ins, "IFNULL2 -> PUTFIELD");
					state = PUTFIELD_STATE;
				}
				break;
			case PUTFIELD_STATE:
				if (ins instanceof MONITOREXIT) {
					// That's it!
					state = FINAL_STATE;
					String bugType = field.isStatic() ? "SPDC_STATIC_DOUBLECHECK" : "SPDC_DOUBLECHECK";
					bugReporter.reportBug(new BugInstance(bugType, NORMAL_PRIORITY)
						.addClass(getJavaClass())
						.addMethod(methodGen)
						.addField(field)
						.addSourceLine(methodGen, start, end)
					);
					break;
				}
			}
		}

		private void debug(Instruction ins, String msg) {
			System.out.println("State=" + state + ": " + msg + " (" + ins + ")");
		}

		/**
		 * Return whether or not we're done scanning this path.
		 * We return true when we reach the final state,
		 * meaning that we found a doublecheck.
		 */
		public boolean isDone() {
			return state == FINAL_STATE;
		}

		/**
		 * Determine if given Edge is the true arm of an "is null" comparison,
		 * or if it is the false arm of an "is not null" comparison.
		 * @param edge the Edge to check
		 */
		private boolean isNullBranch(Edge edge) {
			InstructionHandle handle = edge.getSource().getLastInstruction();
			if (handle == null)
				return false;
			Instruction branch = handle.getInstruction();

			// FIXME: would be nice to handle IF_ACMPEQ and IF_ACMPNE,
			// but that would require more knowledge about what is on the stack.

			switch (edge.getType()) {
			case IFCMP_EDGE:
				return (branch instanceof IFNULL);
			case FALL_THROUGH_EDGE:
				return (branch instanceof IFNONNULL);
			default:
				return false;
			}
		}
	}

	/**
	 * The scanner generator class.
	 * This creates an instruction scanner at each field read.
	 * There may be many getfields and getstatics in a path, and we don't
	 * know a priori which one might be the start of the pattern.
	 * Therefore, we create a seperate scanner for each one.
	 */
	private class DoubleCheckInstructionScannerGenerator implements InstructionScannerGenerator {
		private MethodGen methodGen;

		public DoubleCheckInstructionScannerGenerator(MethodGen methodGen) {
			this.methodGen = methodGen;
		}

		public boolean start(InstructionHandle handle) {
			Instruction ins = handle.getInstruction();
			return (ins instanceof GETFIELD) || (ins instanceof GETSTATIC);
		}

		public InstructionScanner createScanner() {
			return new DoubleCheckInstructionScanner(methodGen);
		}
	}

	public InstructionScannerGenerator createInstructionScannerGenerator(MethodGen methodGen) {
		return new DoubleCheckInstructionScannerGenerator(methodGen);
	}
}

// vim:ts=4
