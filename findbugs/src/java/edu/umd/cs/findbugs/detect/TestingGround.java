/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, Tom Truscott <trt@unx.sas.com>
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
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;

import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class TestingGround extends BytecodeScanningDetector implements Constants2 {

	private BugReporter bugReporter;
	private final boolean active = false;

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	public void visit(Code obj) {
		// unless active, don't bother dismantling bytecode
		if (active) {
			System.out.println("TestingGround: " + getFullyQualifiedMethodName());
			super.visit(obj);
			}
		}


	public void sawOpcode(int seen) {
		printOpCode(seen);
	}
	
	private void printOpCode(int seen) {
		System.out.print("  TestingGround: " +  OPCODE_NAMES[seen]);
		if ((seen == INVOKEVIRTUAL) || (seen == INVOKESPECIAL) || (seen == INVOKEINTERFACE))
			System.out.print("   " + getClassConstantOperand() + "." + getNameConstantOperand() + " " + getSigConstantOperand());
		else if (seen == LDC)
			System.out.print("   \"" + getStringConstantOperand() + "\"");
		else if ((seen == ALOAD) || (seen == ASTORE))
			System.out.print("   " + getRegisterOperand());
		
		System.out.println();
	}
}
