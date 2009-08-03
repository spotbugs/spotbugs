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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LineNumberTable;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.EditDistance;
import edu.umd.cs.findbugs.util.Util;

public class FindSelfComparison extends OpcodeStackDetector {

	final BugAccumulator bugAccumulator;

	public FindSelfComparison(BugReporter bugReporter) {
		this.bugAccumulator = new BugAccumulator(bugReporter);
	}
	
	int putFieldPC = Integer.MIN_VALUE;
	OpcodeStack.Item putFieldObj;
	OpcodeStack.Item putFieldValue;
	XField putFieldXField;
	

	@Override
	public void visit(Code obj) {
		// System.out.println(getFullyQualifiedMethodName());
		whichRegister = -1;
		registerLoadCount = 0;
		resetDoubleAssignmentState();
		super.visit(obj);
		resetDoubleAssignmentState();
		bugAccumulator.reportAccumulatedBugs();
	}

    private void resetDoubleAssignmentState() {
	    putFieldPC = Integer.MIN_VALUE;
		putFieldXField = null;
		putFieldValue = null;
		putFieldObj = null;
    }

	@Override
    public void sawBranchTo(int target) {
		resetDoubleAssignmentState();
	}
	@Override
	public void sawOpcode(int seen) {
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + whichRegister + " " + registerLoadCount);
		
		if (stack.hasIncomingBranches(getPC()))
			resetDoubleAssignmentState();
		
	     if (seen == PUTFIELD) {
	    	OpcodeStack.Item obj = stack.getStackItem(1);
	    	OpcodeStack.Item value = stack.getStackItem(0);
	    	XField f = getXFieldOperand();
	    	XClass x = getXClassOperand();
    		
	    	checkPUTFIELD: if (putFieldPC + 10 > getPC() 
	    			&& f != null && obj != null && f.equals(putFieldXField)
	    			&& !f.isSynthetic()
	    			&& obj.equals(putFieldObj)
	    			&& x != null) {
	    		
	    		LineNumberTable table = getCode().getLineNumberTable();
	    		if (table != null) {
	    			int first = table.getSourceLine(putFieldPC);
	    			int second = table.getSourceLine(getPC());
	    			if (first+1 != second && first != second) 
	    				break checkPUTFIELD;
	    		} else if (putFieldPC + 3 != getPC()) 
	    			break checkPUTFIELD;
	    		
	    		int priority = NORMAL_PRIORITY;
	    		if (value.equals(putFieldValue) && putFieldPC + 3 != getPC())
	    			priority++;
	    		boolean storeOfDefaultValue = putFieldValue.isNull() || putFieldValue.hasConstantValue(0);
				if (storeOfDefaultValue) 
	    			priority++;
	    		if (f.isVolatile()) 
	    			priority++;
	    		XField intendedTarget = null;
	    		
	    		double minimumDistance = 2;
	    		int matches = 0;
	    		for(XField f2 : x.getXFields()) 
	    			if (!f.equals(f2) && !f2.isStatic() && !f2.isFinal() && !f2.isSynthetic() 
	    					&& f2.getSignature().equals(f.getSignature())) {
	    				
	    				double distance = EditDistance.editDistanceRatio(f.getName(), f2.getName());
	    				matches++;
	    				if (minimumDistance > distance) {
	    					minimumDistance = distance;
	    					intendedTarget = f2;
	    				}
	    				
	    			}
	    		if (minimumDistance >  0.6 && (matches > 1 || storeOfDefaultValue))
	    			intendedTarget = null;
	    		else if (intendedTarget != null)
	    			priority--;
	    		BugInstance bug = new BugInstance(this, "SA_FIELD_DOUBLE_ASSIGNMENT", priority)
				.addClassAndMethod(this)
				.addReferencedField(this);
	    		if (intendedTarget != null) 
	    			bug.addField(intendedTarget).describe(FieldAnnotation.DID_YOU_MEAN_ROLE);
	    		
				bugAccumulator.accumulateBug(bug, this);
	    	}
	    	putFieldPC = getPC();
	    	putFieldXField = f;
	    	putFieldObj = obj;
	    	putFieldValue = value;
	    	
	    } else if (isReturn(seen))
	    	resetDoubleAssignmentState();
	    else if (seen == GETFIELD  && Util.nullSafeEquals(getXFieldOperand(), putFieldXField)) {
	    	OpcodeStack.Item obj = stack.getStackItem(0);
	    	if (obj.equals(putFieldObj))
	    		resetDoubleAssignmentState();
	    }
	    
	    
		switch (seen) {
		case INVOKEVIRTUAL:
		case INVOKEINTERFACE:
			if (getClassName().toLowerCase().indexOf("test") >= 0)
				break;
			if (getMethodName().toLowerCase().indexOf("test") >= 0)
				break;
			if (getSuperclassName().toLowerCase().indexOf("test") >= 0)
				break;
			if (getNextOpcode() == POP)
				break;
			String name = getNameConstantOperand();
			
			if (name.equals("equals") || name.equals("compareTo")) {
				String sig = getSigConstantOperand();
				SignatureParser parser = new SignatureParser(sig);
				if (parser.getNumParameters() == 1
				        && (name.equals("equals") && sig.endsWith(";)Z") || name.equals("compareTo") && sig.endsWith(";)I")))
					checkForSelfOperation(seen, "COMPARISON");
			}
			break;

		case LOR:
		case LAND:
		case LXOR:
		case LSUB:
		case IOR:
		case IAND:
		case IXOR:
		case ISUB:
			checkForSelfOperation(seen, "COMPUTATION");
			break;
		case FCMPG:
		case DCMPG:
		case DCMPL:
		case FCMPL:
			break;
		case LCMP:
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case IF_ICMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPGE: 
			checkForSelfOperation(seen, "COMPARISON");
		}
		if (isRegisterLoad() && seen != IINC) {
			if (getRegisterOperand() == whichRegister) registerLoadCount++;
			else {
				whichRegister = getRegisterOperand();
				registerLoadCount = 1;
			}
		} else {
			whichRegister = -1;
			registerLoadCount = 0;
		}
	}
	int whichRegister;
	int registerLoadCount;



private void checkForSelfOperation(int opCode, String op) {
	{

		OpcodeStack.Item item0 = stack.getStackItem(0);
		OpcodeStack.Item item1 = stack.getStackItem(1);

		if (item0.getSignature().equals("D")
				|| item0.getSignature().equals("F"))
			return;
		if (item1.getSignature().equals("D")
				|| item1.getSignature().equals("F"))
			return;

		XField field0 = item0.getXField();
		XField field1 = item1.getXField();
		int fr0 = item0.getFieldLoadedFromRegister();
		int fr1 = item1.getFieldLoadedFromRegister();
		if (field0 != null && field0.equals(field1) && fr0 != -1 && fr0 == fr1)
			bugAccumulator.accumulateBug(new BugInstance(this,
					"SA_FIELD_SELF_" + op, NORMAL_PRIORITY)
			.addClassAndMethod(this).addField(field0), this);

		else if (opCode == IXOR && item0.equals(item1)) {
			LocalVariableAnnotation localVariableAnnotation = LocalVariableAnnotation
            .getLocalVariableAnnotation(this, item0);
			if (localVariableAnnotation != null) 
				bugAccumulator.accumulateBug(new BugInstance(this,
					"SA_LOCAL_SELF_" + op,  HIGH_PRIORITY)
			.addClassAndMethod(this).add(
					localVariableAnnotation),this);
		} else if (opCode == ISUB  && registerLoadCount >= 2) { // let FindSelfComparison2 report this; more accurate
			bugAccumulator.accumulateBug(new BugInstance(this,
					"SA_LOCAL_SELF_" + op, (opCode == ISUB || opCode == LSUB  || opCode == INVOKEINTERFACE || opCode == INVOKEVIRTUAL) ? NORMAL_PRIORITY : HIGH_PRIORITY)
			.addClassAndMethod(this).add(
					LocalVariableAnnotation
					.getLocalVariableAnnotation(
							getMethod(), whichRegister, getPC(),
							getPC() - 1)),this);
		}
	}
}
}
