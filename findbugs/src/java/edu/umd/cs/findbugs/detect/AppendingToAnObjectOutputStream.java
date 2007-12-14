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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class AppendingToAnObjectOutputStream extends OpcodeStackDetector {

	BugReporter bugReporter;

	public AppendingToAnObjectOutputStream(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	boolean sawOpenInAppendMode;

	@Override
	public void visit(Method obj) {
		sawOpenInAppendMode = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {
		if (seen != INVOKESPECIAL) {
			sawOpenInAppendMode = false;
			return;
		}
		String calledClassName = getClassConstantOperand();
		String calledMethodName = getNameConstantOperand();
		String calledMethodSig = getSigConstantOperand();
		if (!sawOpenInAppendMode) {
			if (calledClassName.equals("java/io/ObjectOutputStream") && calledMethodName.equals("<init>")
			        && calledMethodSig.equals("(Ljava/io/OutputStream;)V")
			        && stack.getStackItem(0).getSpecialKind() == OpcodeStack.Item.FILE_OPENED_IN_APPEND_MODE)
				bugReporter.reportBug(new BugInstance(this, "IO_APPENDING_TO_OBJECT_OUTPUT_STREAM", Priorities.HIGH_PRIORITY)
				        .addClassAndMethod(this).addSourceLine(this));
			return;
		}
		if (calledClassName.equals("java/io/FileOutputStream") && calledMethodName.equals("<init>")
		        && (calledMethodSig.equals("(Ljava/io/File;Z)V") || calledMethodSig.equals("(Ljava/lang/String;Z)V"))) {
			OpcodeStack.Item item = stack.getStackItem(0);
			Object value = item.getConstant();
			sawOpenInAppendMode = value instanceof Integer && ((Integer) value).intValue() == 1;
		} else if (!sawOpenInAppendMode) {
			return;
		} else if (calledClassName.equals("java/io/BufferedOutputStream") && calledMethodName.equals("<init>")
		        && calledMethodSig.equals("(Ljava/io/OutputStream;)V")) {
			// do nothing

		} else if (calledClassName.equals("java/io/ObjectOutputStream") && calledMethodName.equals("<init>")
		        && calledMethodSig.equals("(Ljava/io/OutputStream;)V")) {
			bugReporter.reportBug(new BugInstance(this, "IO_APPENDING_TO_OBJECT_OUTPUT_STREAM", Priorities.HIGH_PRIORITY)
			        .addClassAndMethod(this).addSourceLine(this));
			sawOpenInAppendMode = false;
		} else
			sawOpenInAppendMode = false;

	}

}
