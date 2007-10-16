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


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.SignatureParser;

public class FindFieldSelfAssignment extends BytecodeScanningDetector implements StatelessDetector {
	private BugReporter bugReporter;
	int state;



	public FindFieldSelfAssignment(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(Code obj) {
		state = 0;
		super.visit(obj);
		initializedFields.clear();
	}


	int register;
	String f;
	String className;
	Set<String> initializedFields = new HashSet<String>();

	@Override
		 public void sawOpcode(int seen) {

		switch (state) {
		case 0:
			if (seen == ALOAD_0)
				state = 1;
			else if (seen == DUP)
				state = 6;
			break;
		case 1:
			if (seen == ALOAD_0)
				state = 2;
			else
				state = 0;
			break;
		case 2:
			if (seen == GETFIELD) {
				state = 3;
				f = getRefConstantOperand();
				className = getClassConstantOperand();
			} else
				state = 0;
			break;
		case 3:
			if (seen == PUTFIELD && getRefConstantOperand().equals(f) && getClassConstantOperand().equals(className)) {

				int priority = NORMAL_PRIORITY;
				SignatureParser parser = new SignatureParser(getMethodSig());
				boolean foundMatch = false;
				for(Iterator<String> i =  parser.parameterSignatureIterator(); i.hasNext(); ) {
					String s = i.next();
					if (s.equals(getSigConstantOperand())) {
						foundMatch = true;
						break;
					}
				}
				if (getMethodName().equals("<init>") && !initializedFields.contains(getRefConstantOperand()) && foundMatch)
					priority = HIGH_PRIORITY;

				bugReporter.reportBug(new BugInstance(this, "SA_FIELD_SELF_ASSIGNMENT", priority)
						.addClassAndMethod(this)
						.addReferencedField(this)
						.addSourceLine(this));
			}
			state = 0;
			break;
		 case 6:
			if (isRegisterStore()) {
				state = 7;
				register = getRegisterOperand();
			} else state = 0;
			break;
		case 7:
			if (isRegisterStore() && register ==  getRegisterOperand()) {
				bugReporter.reportBug(new BugInstance(this, "SA_LOCAL_DOUBLE_ASSIGNMENT", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.add( LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), register, getPC(), getPC()-1))
				.addSourceLine(this));
			} 
			state = 0;
			break;
		}

		if (seen == PUTFIELD  && getClassConstantOperand().equals(className))
			initializedFields.add(getRefConstantOperand());

	}

}
