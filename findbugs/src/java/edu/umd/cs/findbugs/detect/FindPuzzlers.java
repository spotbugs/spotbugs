/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005, Tom Truscott <trt@unx.sas.com>
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

import java.text.NumberFormat;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindPuzzlers extends BytecodeScanningDetector implements Constants2 {


	BugReporter bugReporter;
	public FindPuzzlers(BugReporter bugReporter) {
		this.bugReporter =  bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visit(Code obj) {
		prevOpcodeIncrementedRegister = -1;
		super.visit(obj);
	}

	int prevOpcodeIncrementedRegister;
	public void sawOpcode(int seen) {

		if ((seen == IFEQ || seen == IFNE) && getPrevOpcode(1) == IMUL
			&& ( getPrevOpcode(2) == SIPUSH
				|| getPrevOpcode(2) == BIPUSH
				))
			 bugReporter.reportBug(new BugInstance(this, "TESTING", NORMAL_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));
		if ((seen == I2S || seen == I2B) && getPrevOpcode(1) == IUSHR)
			 bugReporter.reportBug(new BugInstance(this, "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT", NORMAL_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));

		if (isRegisterStore() && (seen == ISTORE 
			|| seen == ISTORE_0
			|| seen == ISTORE_1
			|| seen == ISTORE_2
			|| seen == ISTORE_3)
			&& getRegisterOperand() == prevOpcodeIncrementedRegister) {
			 bugReporter.reportBug(new BugInstance(this, "DLS_OVERWRITTEN_INCREMENT", HIGH_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));

			}
		if (seen == IINC) {
			prevOpcodeIncrementedRegister = getRegisterOperand();	
			}
		else
			prevOpcodeIncrementedRegister = -1;
	}

}
