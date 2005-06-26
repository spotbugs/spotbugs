/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class UselessSubclassMethod extends BytecodeScanningDetector implements Constants2, StatelessDetector {

	public static final int SEEN_NOTHING = 0;
	public static final int SEEN_PARM = 1;
	public static final int SEEN_LAST_PARM = 2;
	public static final int SEEN_INVOKE = 3;
	public static final int SEEN_RETURN = 4;
	public static final int SEEN_INVALID = 5;
	
	private BugReporter bugReporter;
	private int state;
	private int curParm;
	private int curParmOffset;
	private int invokePC;
	private Type[] argTypes;
	private int register;
	
	public UselessSubclassMethod(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void visitCode(Code obj)
	{
		if (!getMethodName().equals("<init>")
		&&  ((this.getMethod().getModifiers() & Constants.ACC_STATIC)) == 0) {
			state = SEEN_NOTHING;
			invokePC = 0;
			super.visitCode(obj);
			if (state == SEEN_RETURN) {
				bugReporter.reportBug( new BugInstance( this, "USM_USELESS_SUBCLASS_METHOD", LOW_PRIORITY )
					.addClassAndMethod(this)
					.addSourceLine(this, invokePC));
			}
		}
	}
	
	public void sawOpcode(int seen) {
		switch (state) {
			case SEEN_NOTHING:
				if (seen == ALOAD_0) {
					argTypes = Type.getArgumentTypes(this.getMethodSig()); 
					curParm = 0;
					curParmOffset = 1;
					if (argTypes.length > 0)
						state = SEEN_PARM;
					else
						state = SEEN_LAST_PARM;
				} else
					state = SEEN_INVALID;	
			break;
			
			case SEEN_PARM:
				if (curParm >= argTypes.length)
					state = SEEN_INVALID;
				else {
					String signature = argTypes[curParm++].getSignature();
					char typeChar0 = signature.charAt(0);
					if ((typeChar0 == 'L') || (typeChar0 == '[')) {
						checkParm(seen, ALOAD_0, ALOAD, 1);
					}
					else if (typeChar0 == 'D') {
						checkParm(seen, DLOAD_0, DLOAD, 2);
					}
					else if (typeChar0 == 'F') {
						checkParm(seen, FLOAD_0, FLOAD, 1);
					}
					else if (typeChar0 == 'I') {
						checkParm(seen, ILOAD_0, ILOAD, 1);
					}
					else if (typeChar0 == 'J') {
						checkParm(seen, LLOAD_0, LLOAD, 2);
					}
					if ((state != SEEN_INVALID) && (curParm >= argTypes.length))
						state = SEEN_LAST_PARM;
						
				}
			break;
			
			case SEEN_LAST_PARM:
				if ((seen == INVOKENONVIRTUAL) && getMethodSig().equals(getSigConstantOperand())) {
					invokePC = getPC();
					state = SEEN_INVOKE;
				}
				else
					state = SEEN_INVALID;
			break;
			
			case SEEN_INVOKE:
				Type returnType = getMethod().getReturnType();
				char retSigChar0 = returnType.getSignature().charAt(0);
				if ((retSigChar0 == 'V') && (seen == RETURN))
					state = SEEN_RETURN;
				else if (((retSigChar0 == 'L') || (retSigChar0 == '[')) && (seen == ARETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'D') && (seen == DRETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'F') && (seen == FRETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'I') && (seen == IRETURN))
					state = SEEN_RETURN;
				else if ((retSigChar0 == 'J') && (seen == LRETURN))
					state = SEEN_RETURN;
				else
					state = SEEN_INVALID;
			break;
			
			case SEEN_RETURN:
				state = SEEN_INVALID;
			break;
		}
	}
	
	private void checkParm(int seen, int fastOpBase, int slowOp, int parmSize) {
		if ((curParmOffset >= 1) && (curParmOffset <= 3)) {
			if (seen == (fastOpBase + curParmOffset))
				curParmOffset += parmSize;
			else
				state = SEEN_INVALID;
		}
		else if (curParmOffset == 0)
			state = SEEN_INVALID;
		else if ((seen == slowOp) && (getRegisterOperand() == curParmOffset))
			curParmOffset += parmSize;
		else
			state = SEEN_INVALID;
	}
}
