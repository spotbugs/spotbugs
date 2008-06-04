/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import java.util.Iterator;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.ba.vna.ValueNumberSourceInfo;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindNullDerefsInvolvingNonShortCircuitEvaluation extends OpcodeStackDetector {

	private static boolean DEBUG = false;

	BugReporter bugReporter;

	public FindNullDerefsInvolvingNonShortCircuitEvaluation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Code code) {
		boolean interesting = true;
		if (interesting) {
			// initialize any variables we want to initialize for the method
			super.visit(code); // make callbacks to sawOpcode for all opcodes
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {
		if (seen == IAND || seen == IOR) {

			int nextOpcode = getCodeByte(getPC() + 1);
			if (nextOpcode == IFEQ || nextOpcode == IFNE) {
				OpcodeStack.Item left = stack.getStackItem(1);
				OpcodeStack.Item right = stack.getStackItem(0);
				checkForNullForcingABranch(seen, nextOpcode, left);
				checkForNullForcingABranch(seen, nextOpcode, right);
			}

		}
	}

	private void checkForNullForcingABranch(int seen, int nextOpcode, OpcodeStack.Item item) {
		if (nullGuaranteesBranch(seen, item)) {
			// null guarantees a branch
			boolean nullGuaranteesZero = seen == IAND;
			boolean nullGuaranteesBranch = nullGuaranteesZero ^ (nextOpcode == IFNE);
			if (DEBUG)
				System.out.println(item.getPC() + " null guarantees " + nullGuaranteesBranch + " branch");
			try {
				CFG cfg = getClassContext().getCFG(getMethod());
				Location produced = findLocation(cfg, item.getPC());
				Location branch = findLocation(cfg, getPC() + 1);
				if (produced == null || branch == null)
					return;

				IfInstruction branchInstruction = (IfInstruction) branch.getHandle().getInstruction();

				IsNullValueDataflow isNullValueDataflow = getClassContext().getIsNullValueDataflow(getMethod());
				ValueNumberDataflow valueNumberDataflow = getClassContext().getValueNumberDataflow(getMethod());
				UnconditionalValueDerefDataflow unconditionalValueDerefDataflow = getClassContext()
				        .getUnconditionalValueDerefDataflow(getMethod());
				ValueNumberFrame valueNumberFact = valueNumberDataflow.getFactAtLocation(produced);
				IsNullValueFrame isNullFact = isNullValueDataflow.getFactAtLocation(produced);
				ValueNumber value = valueNumberFact.getTopValue();
				if (isNullFact.getTopValue().isDefinitelyNotNull())
					return;
				if (DEBUG) {
					System.out.println("Produced: " + produced);
					System.out.println(valueNumberFact);
					System.out.println(isNullFact);
					System.out.println("value: " + value);
					System.out.println("branch: " + branch);
					System.out.println("instruction: " + branchInstruction);
					System.out.println("target: " + branchInstruction.getTarget());
					System.out.println("next: " + branch.getHandle().getNext());
				}
				Location guaranteed = findLocation(cfg, nullGuaranteesBranch ? branchInstruction.getTarget() : branch.getHandle()
				        .getNext());
				if (guaranteed == null)
					return;

				UnconditionalValueDerefSet unconditionalDeref = unconditionalValueDerefDataflow.getFactAtLocation(guaranteed);
				if (DEBUG) {
					System.out.println("Guaranteed on null: " + guaranteed);
					System.out.println(unconditionalDeref);
				}

				if (unconditionalDeref.isUnconditionallyDereferenced(value)) {
					SourceLineAnnotation tested = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), getMethod(),
					        produced);
					BugAnnotation variableAnnotation = ValueNumberSourceInfo.findAnnotationFromValueNumber(getMethod(), produced,
					        value, valueNumberFact);
					Set<Location> unconditionalDerefLocationSet = unconditionalDeref.getUnconditionalDerefLocationSet(value);

					BugInstance bug;
					if (unconditionalDerefLocationSet.size() > 1) {
						bug = new BugInstance(this, "NP_GUARANTEED_DEREF", NORMAL_PRIORITY).addClassAndMethod(this);
						bug.addOptionalAnnotation(variableAnnotation);
						bug.addSourceLine(tested).describe("SOURCE_LINE_KNOWN_NULL");
						for (Location dereferenced : unconditionalDerefLocationSet)
							bug.addSourceLine(getClassContext(), getMethod(), dereferenced).describe("SOURCE_LINE_DEREF");

					} else {
						bug = new BugInstance(this, "NP_NULL_ON_SOME_PATH", NORMAL_PRIORITY).addClassAndMethod(this);
						bug.addOptionalAnnotation(variableAnnotation);
						for (Location dereferenced : unconditionalDerefLocationSet)
							bug.addSourceLine(getClassContext(), getMethod(), dereferenced).describe("SOURCE_LINE_DEREF");

						bug.addSourceLine(tested).describe("SOURCE_LINE_KNOWN_NULL");

					}

					bugReporter.reportBug(bug);
				}

			} catch (DataflowAnalysisException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CFGBuilderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	@CheckForNull
	Location findLocation(CFG cfg, int pc) {
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location loc = i.next();
			if (loc.getHandle().getPosition() == pc)
				return loc;
		}
		return null;
	}

	@CheckForNull
	Location findLocation(CFG cfg, InstructionHandle handle) {
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location loc = i.next();
			if (loc.getHandle() == handle)
				return loc;
		}
		return null;
	}

	private boolean nullGuaranteesBranch(int seen, OpcodeStack.Item item) {
		return item.getSpecialKind() == OpcodeStack.Item.ZERO_MEANS_NULL && seen == IAND
		        || item.getSpecialKind() == OpcodeStack.Item.NONZERO_MEANS_NULL && seen == IOR;
	}

	private void emitWarning() {
		System.out.println("Warn about " + getMethodName()); // TODO
	}

}
