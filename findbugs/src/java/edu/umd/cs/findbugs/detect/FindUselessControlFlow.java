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


import edu.umd.cs.findbugs.*;
import java.util.BitSet;
import org.apache.bcel.Constants;

/**
 * A Detector to look for useless control flow.  For example,
 * <pre>
 *     if (argv.length == 1);
 *         System.out.println("Hello, " + argv[0]);
 * </pre>
 * In this kind of bug, we'll see an ifcmp instruction where the IF
 * target is the same as the fall-through target.
 * <p/>
 * <p> The idea for this detector came from Richard P. King,
 * and the idea of looking for if instructions with identical
 * branch and fall-through targets is from Mike Fagan.
 *
 * @author David Hovemeyer
 */
public class FindUselessControlFlow extends BytecodeScanningDetector implements StatelessDetector {
	private static final BitSet ifInstructionSet = new BitSet();

	static {
		ifInstructionSet.set(Constants.IF_ACMPEQ);
		ifInstructionSet.set(Constants.IF_ACMPNE);
		ifInstructionSet.set(Constants.IF_ICMPEQ);
		ifInstructionSet.set(Constants.IF_ICMPNE);
		ifInstructionSet.set(Constants.IF_ICMPLT);
		ifInstructionSet.set(Constants.IF_ICMPLE);
		ifInstructionSet.set(Constants.IF_ICMPGT);
		ifInstructionSet.set(Constants.IF_ICMPGE);
		ifInstructionSet.set(Constants.IFEQ);
		ifInstructionSet.set(Constants.IFNE);
		ifInstructionSet.set(Constants.IFLT);
		ifInstructionSet.set(Constants.IFLE);
		ifInstructionSet.set(Constants.IFGT);
		ifInstructionSet.set(Constants.IFGE);
		ifInstructionSet.set(Constants.IFNULL);
		ifInstructionSet.set(Constants.IFNONNULL);
	}

	private BugReporter bugReporter;

	public FindUselessControlFlow(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	
	@Override
         public void sawOpcode(int seen) {
		if (ifInstructionSet.get(seen)) {
			if (getBranchTarget() == getBranchFallThrough()) {
				bugReporter.reportBug(new BugInstance(this, "UCF_USELESS_CONTROL_FLOW", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
			}
		}
	}
}

// vim:ts=4
