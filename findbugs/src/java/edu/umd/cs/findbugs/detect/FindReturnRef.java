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


import java.util.HashSet;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XField;

public class FindReturnRef extends BytecodeScanningDetector {
	boolean check = false;
	boolean thisOnTOS = false;
	boolean fieldOnTOS = false;
	boolean publicClass = false;
	boolean staticMethod = false;
	boolean dangerousToStoreIntoField = false;
	boolean emptyArrayOnTOS;
	String nameOnStack;
	String classNameOnStack;
	String sigOnStack;
	int parameterCount;
	//int r;
	int timesRead [] = new int[256];
	boolean fieldIsStatic;
	HashSet<XField> emptyArray = new HashSet<XField>();
	private BugReporter bugReporter;
	//private LocalVariableTable variableNames;

	public FindReturnRef(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
		 public void visit(JavaClass obj) {
		publicClass = obj.isPublic();
		super.visit(obj);
	}

	@Override
		 public void visit(Method obj) {
		check = publicClass && (obj.getAccessFlags() & (ACC_PUBLIC)) != 0;
		
		dangerousToStoreIntoField = false;
		staticMethod = (obj.getAccessFlags() & (ACC_STATIC)) != 0;
		//variableNames = obj.getLocalVariableTable();
		parameterCount = getNumberMethodArguments();
		/*
		System.out.println(betterMethodName);
		for(int i = 0; i < parameterCount; i++)
			System.out.println("parameter " + i + ": " + obj.getArgumentTypes()[i]);
		*/

		if (!staticMethod) parameterCount++;

		for (int i = 0; i < parameterCount; i++)
			timesRead[i] = 0;
		thisOnTOS = false;
		fieldOnTOS = false;
		super.visit(obj);
		thisOnTOS = false;
		fieldOnTOS = false;
	}


	@Override
		 public void visit(Code obj) {
		super.visit(obj);
	}

	@Override
		 public void sawOpcode(int seen) {
		
		if (seen == PUTFIELD || seen == PUTSTATIC) {
			XField f = getXFieldOperand();
			if (f.isFinal())
				emptyArray.add(f);
			
				
		}
		emptyArrayOnTOS = (seen == ANEWARRAY || seen == NEWARRAY) && getPrevOpcode(-1) == ICONST_0;
		
		if (!check) return;
		
		if (staticMethod && dangerousToStoreIntoField && seen == PUTSTATIC
				&& MutableStaticFields.mutableSignature(getSigConstantOperand())) {
			bugReporter.reportBug(new BugInstance(this, "EI_EXPOSE_STATIC_REP2", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addField(getDottedClassConstantOperand(), getNameConstantOperand(), getSigConstantOperand(),
							true)
					.addSourceLine(this));
		}
		if (!staticMethod && dangerousToStoreIntoField && seen == PUTFIELD
				&& MutableStaticFields.mutableSignature(getSigConstantOperand())) {
			bugReporter.reportBug(new BugInstance(this, "EI_EXPOSE_REP2", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addField(getDottedClassConstantOperand(), getNameConstantOperand(), getSigConstantOperand(),
							true)
					.addSourceLine(this));
			/*
			System.out.println("Store of parameter "
					+ r +"/" + parameterCount
					+ " into field of type " + sigConstant
					+ " in " + betterMethodName);
				bugReporter.reportBug(new BugInstance("EI_EXPOSE_REP2", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addField(betterClassConstant, nameConstant, betterSigConstant,
							false)
					.addSourceLine(this));
		`	*/
		}
		dangerousToStoreIntoField = false;
		int reg = -1; // this value should never be seen
		checkStore: {
			switch (seen) {
			case ALOAD_0:
				reg = 0;
				break;
			case ALOAD_1:
				reg = 1;
				break;
			case ALOAD_2:
				reg = 2;
				break;
			case ALOAD_3:
				reg = 3;
				break;
			case ALOAD:
				reg = getRegisterOperand();
				break;
			default:
				break checkStore;
			}
			if (reg < parameterCount)
				timesRead[reg]++;
		}
		if (thisOnTOS && !staticMethod) {
			switch (seen) {
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ALOAD:
				if (reg < parameterCount) {
					//r = reg;
					dangerousToStoreIntoField = true;
					// System.out.println("Found dangerous value from parameter " + reg);
				}
			default:
			}
		} else if (staticMethod) {
			switch (seen) {
			case ALOAD_0:
			case ALOAD_1:
			case ALOAD_2:
			case ALOAD_3:
			case ALOAD:
				if (reg < parameterCount) {
					//r = reg;
					dangerousToStoreIntoField = true;
				}
			default:
			}
		}

		if (seen == ALOAD_0 && !staticMethod) {
			thisOnTOS = true;
			fieldOnTOS = false;
			return;
		}


		if (thisOnTOS && seen == GETFIELD && getClassConstantOperand().equals(getClassName()) && !emptyArray.contains(getXFieldOperand())) {
			fieldOnTOS = true;
			thisOnTOS = false;
			nameOnStack = getNameConstantOperand();
			classNameOnStack = getDottedClassConstantOperand();
			sigOnStack = getSigConstantOperand();
			fieldIsStatic = false;
			// System.out.println("Saw getfield");
			return;
		}
		if (seen == GETSTATIC && getClassConstantOperand().equals(getClassName()) && !emptyArray.contains(getXFieldOperand())) {
			fieldOnTOS = true;
			thisOnTOS = false;
			nameOnStack = getNameConstantOperand();
			classNameOnStack = getDottedClassConstantOperand();
			sigOnStack = getSigConstantOperand();
			fieldIsStatic = true;
			return;
		}
		thisOnTOS = false;
		if (check && fieldOnTOS && seen == ARETURN
				/*
				&& !sigOnStack.equals("Ljava/lang/String;")
				&& sigOnStack.indexOf("Exception") == -1
				&& sigOnStack.indexOf("[") >= 0
				*/
				&& nameOnStack.indexOf("EMPTY") == -1
				&& MutableStaticFields.mutableSignature(sigOnStack)
		) {
			bugReporter.reportBug(new BugInstance(this, staticMethod ? "MS_EXPOSE_REP" : "EI_EXPOSE_REP", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addField(classNameOnStack, nameOnStack, sigOnStack, fieldIsStatic)
					.addSourceLine(this));
		}

		fieldOnTOS = false;
		thisOnTOS = false;
	}


}	
