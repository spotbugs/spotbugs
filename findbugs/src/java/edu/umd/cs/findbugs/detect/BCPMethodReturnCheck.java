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

package edu.umd.cs.findbugs.detect;

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.daveho.ba.*;
import edu.umd.cs.daveho.ba.bcp.*;

public class BCPMethodReturnCheck extends ByteCodePatternDetector {
	private BugReporter bugReporter;

	private static final ByteCodePattern pattern = new ByteCodePattern()
		.add(new MatchAny(new PatternElement[] {
			new Invoke("+java.io.InputStream", "read", "/^\\((\\[B|\\[BII)\\)I$", Invoke.INSTANCE),
			new Invoke("/^java.lang.(String|Byte|Boolean|Character|Short|Integer|Long|Float|Double)$", "/.*", "/.*", Invoke.ANY)
		}).label("call"))
		.add(new Opcode(Constants.POP));

	public BCPMethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public ByteCodePattern getPattern() { return pattern; }

	public boolean prescreen(Method method, ClassContext classContext) { return true; }

	public void reportMatch(MethodGen methodGen, ByteCodePatternMatch match) {
		InstructionHandle call = match.getLabeledInstruction("call");

		bugReporter.reportBug(new BugInstance("RV_RETURN_VALUE_IGNORED", NORMAL_PRIORITY)
			.addClassAndMethod(methodGen)
			.addCalledMethod(methodGen, (InvokeInstruction) call.getInstruction())
			.addSourceLine(methodGen, call));
	}

}

// vim:ts=4
