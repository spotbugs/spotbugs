/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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
import java.util.*;
import java.io.PrintStream;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class ReadReturnShouldBeChecked extends BytecodeScanningDetector implements   Constants2 {

   boolean sawRead = false;
   boolean sawSkip = false;
   int sawAvailable = 0;
   private BugReporter bugReporter;
   private int readPC, skipPC;
   private String lastCallClass = null, lastCallMethod = null, lastCallSig = null;

   public ReadReturnShouldBeChecked(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

    public void visit(Method obj) {
	sawAvailable = 0;
	sawRead = false;
	sawSkip = false;
	//check =  (obj.getAccessFlags() & (ACC_PUBLIC | ACC_PROTECTED)) != 0;
	}


    public void sawOpcode(int seen) {

	if (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) {
	    lastCallClass = getDottedClassConstantOperand();
	    lastCallMethod = getNameConstantOperand();
	    lastCallSig = getDottedSigConstantOperand();
	}

	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& getNameConstantOperand().equals("available")
		&& getSigConstantOperand().equals("()I")
	    || (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& getNameConstantOperand().startsWith("get")
		&& getNameConstantOperand().endsWith("Length")
		&& getSigConstantOperand().equals("()I")
	    || (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& getClassConstantOperand().equals("java/io/File")
		&& getNameConstantOperand().equals("length")
		&& getSigConstantOperand().equals("()J"))   {
		sawAvailable = 70;
		return;
		}
	sawAvailable--;
	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& !getClassConstantOperand().equals("java/io/ByteArrayInputStream")
		&& getNameConstantOperand().equals("read")
		&& (getSigConstantOperand().startsWith("([B")
		   || getSigConstantOperand().startsWith("([C"))
		&& sawAvailable <= 0)   {
		/*
		System.out.println("Saw invocation of "
			+ nameConstant + "("
			+sigConstant
			+")");
		*/

		boolean b = false;
		try {
		b = Repository.instanceOf(getClassConstantOperand(), "java/io/ByteArrayInputStream");
		} catch (ClassNotFoundException e) {
		}
		if (!b) {
			sawRead = true;
			readPC = getPC();
			return;	
			}
	} else if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		&& !getClassConstantOperand().equals("java/io/ByteArrayInputStream")
		&& getNameConstantOperand().equals("skip")) {
		boolean b = false;
		try {
		b = Repository.instanceOf(getClassConstantOperand(), "java/io/ByteArrayInputStream");
		} catch (ClassNotFoundException e) {
		}
		if (!b) {
			sawSkip = true;
			skipPC = getPC();
			return;	
			}
	}
	
	if ((seen == POP) || (seen == POP2)) {
		if (sawRead) {
			bugReporter.reportBug(new BugInstance("RR_NOT_CHECKED", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addCalledMethod(lastCallClass, lastCallMethod, lastCallSig)
				.addSourceLine(this, readPC));
		} else if (sawSkip) {
				boolean isBufferedInputStream = false;
				try {
				isBufferedInputStream = Repository.instanceOf(lastCallClass, "java/io/BufferedInputStream");
				} catch (ClassNotFoundException e) {
				}

			bugReporter.reportBug(new BugInstance("SR_NOT_CHECKED", 
				(isBufferedInputStream ? HIGH_PRIORITY : NORMAL_PRIORITY))
				.addClassAndMethod(this)
				.addCalledMethod(lastCallClass, lastCallMethod, lastCallSig)
				.addSourceLine(this, skipPC));
		}
	}
	sawRead = false;
	sawSkip = false;
	}
}	
