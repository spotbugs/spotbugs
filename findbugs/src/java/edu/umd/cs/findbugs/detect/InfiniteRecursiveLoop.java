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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.*;

public class InfiniteRecursiveLoop extends BytecodeScanningDetector implements Constants2 {

	private BugReporter bugReporter;
	private boolean seenTransferOfControl;
	private boolean thisOnTopOfStack ;
	private boolean staticMethod ;

	public InfiniteRecursiveLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	public void visit(JavaClass obj) {
	}

	public void visit(Method obj) {
		seenTransferOfControl = false;
		thisOnTopOfStack = false;
		staticMethod = (obj.getAccessFlags() & (ACC_STATIC)) != 0;
	}

	public void sawOffset(int offset) {
		seenTransferOfControl = true;
	}

	public void sawOpcode(int seen) {

		switch(seen) {
			case ARETURN:
			case IRETURN:
			case LRETURN:
			case RETURN:
			case DRETURN:
			case FRETURN:
				seenTransferOfControl = true;
			}
		if (seenTransferOfControl) return;
		
		if ((seen == INVOKEVIRTUAL) || (seen == INVOKESPECIAL) || (seen == INVOKEINTERFACE) || (seen == INVOKESTATIC)) 
			if (getClassConstantOperand().equals(getClassName())
			    && getNameConstantOperand().equals(getMethodName())
			    && getSigConstantOperand().equals(getMethodSig())) 
			if (seen == INVOKESTATIC 
				|| getNameConstantOperand().equals("<init>")
				|| thisOnTopOfStack 
					&& getSigConstantOperand().startsWith("()")
				)
				

				bugReporter.reportBug(new BugInstance(this, "IL_INFINITE_RECURSIVE_LOOP", HIGH_PRIORITY)
				        .addClassAndMethod(this));

		thisOnTopOfStack = seen == ALOAD_0 && !staticMethod;
	}

}
