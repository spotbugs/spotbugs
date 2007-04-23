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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;

public class ReadReturnShouldBeChecked extends BytecodeScanningDetector
		implements StatelessDetector {

	boolean sawRead = false;

	boolean sawSkip = false;

	boolean recentCallToAvailable = false;

	int sawAvailable = 0;

	boolean wasBufferedInputStream = false;
	BugAccumulator accumulator;


	private int locationOfCall;

	private String lastCallClass = null, lastCallMethod = null,
			lastCallSig = null;

	public ReadReturnShouldBeChecked(BugReporter bugReporter) {
	  this.accumulator = new BugAccumulator(bugReporter);
	}

	@Override
	public void visit(Code obj) {
		sawAvailable = 0;
		sawRead = false;
		sawSkip = false;
		super.visit(obj);
		accumulator.reportAccumulatedBugs();
	}

	private boolean isInputStream()  {
		try {
		if (lastCallClass.startsWith("[")) return false;
		return (Repository.instanceOf(lastCallClass,
		"java.io.InputStream")
		|| Repository.implementationOf(lastCallClass,
				"java.io.DataInput") || Repository
		.instanceOf(lastCallClass, "java.io.Reader"))
		&& !Repository.instanceOf(lastCallClass,
				"java.io.ByteArrayInputStream");
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private boolean isBufferedInputStream() {
		try {
		if (lastCallClass.startsWith("[")) return false;
		return Repository.instanceOf(
				lastCallClass,"java.io.BufferedInputStream");
	} catch (ClassNotFoundException e) {
		return false;
	}
	}
	@Override
	public void sawOpcode(int seen) {

		if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) {
			lastCallClass = getDottedClassConstantOperand();
			lastCallMethod = getNameConstantOperand();
			lastCallSig = getSigConstantOperand();
		}

		if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
			if (getNameConstantOperand().equals("available")
					&& getSigConstantOperand().equals("()I")
					|| getNameConstantOperand().startsWith("get")
					&& getNameConstantOperand().endsWith("Length")
					&& getSigConstantOperand().equals("()I")
					|| getClassConstantOperand().equals("java/io/File")
					&& getNameConstantOperand().equals("length")
					&& getSigConstantOperand().equals("()J")) {
				sawAvailable = 70;
				return;
			}
		sawAvailable--;
		if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
				&& getNameConstantOperand().equals("read")

				&& (getSigConstantOperand().equals("([B)I")
						|| getSigConstantOperand().equals("([BII)I")
						|| getSigConstantOperand().equals("([C)I") || getSigConstantOperand()
						.equals("([CII)I")) && isInputStream()) {
			sawRead = true;
			recentCallToAvailable = sawAvailable > 0;
			locationOfCall = getPC();
			return;
		}
		if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) 
				&& 
			   ( getNameConstantOperand().equals("skip")
				&& getSigConstantOperand().equals("(J)J")
				||  getNameConstantOperand().equals("skipBytes")
				&& getSigConstantOperand().equals("(I)I"))
				&& isInputStream() ) {
			// if not ByteArrayInput Stream
			// and either no recent calls to length
			// or it is a BufferedInputStream

			wasBufferedInputStream = isBufferedInputStream();
			sawSkip = true;
			locationOfCall = getPC();
			recentCallToAvailable = sawAvailable > 0 && !wasBufferedInputStream;
			return;

		}

		if ((seen == POP) || (seen == POP2)) {

			if (sawRead) {
				accumulator.accumulateBug(new BugInstance(this, "RR_NOT_CHECKED",
						recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)
						.addClassAndMethod(this).addCalledMethod(lastCallClass,
								lastCallMethod, lastCallSig, false),
							   SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));


			} else if (sawSkip) {

				accumulator.accumulateBug(new BugInstance(this, "SR_NOT_CHECKED",
						(wasBufferedInputStream ? HIGH_PRIORITY
								: recentCallToAvailable ? LOW_PRIORITY
										: NORMAL_PRIORITY)).addClassAndMethod(
						this).addCalledMethod(lastCallClass, lastCallMethod,
						lastCallSig, false),
						SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));
			}
		}
		sawRead = false;
		sawSkip = false;
	}
}
