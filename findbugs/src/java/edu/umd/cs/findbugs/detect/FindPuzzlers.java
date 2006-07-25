/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;

public class FindPuzzlers extends BytecodeScanningDetector {


	BugReporter bugReporter;
	public FindPuzzlers(BugReporter bugReporter) {
		this.bugReporter =  bugReporter;
	}



	@Override
         public void visit(Code obj) {
		prevOpcodeIncrementedRegister = -1;
		best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = LOW_PRIORITY+1;
		prevOpCode = NOP;
		stack.resetForMethodEntry(this);
		badlyComputingOddState = 0;
		super.visit(obj);
	}

	int priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG;
	int prevOpcodeIncrementedRegister;
	int valueOfConstantArgumentToShift;
	int best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG ;
	boolean constantArgumentToShift;
	
	int badlyComputingOddState;
	int prevOpCode;
	OpcodeStack stack = new OpcodeStack();
	
	private int adjustPriority(Object constant, int priority) {
		if (!(constant instanceof Integer)) return priority;
		int value = Math.abs(((Integer) constant).intValue());
		if (value <= 4) return LOW_PRIORITY+2;
		if (value <= 10000) return priority+1;
		if (value <= 60*60*1000) return priority;
		return priority-1;
	}
	@Override
         public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		
		if (seen == IMUL) {
			priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = NORMAL_PRIORITY;
			if (stack.getStackDepth() > 1) {
				OpcodeStack.Item item0 = stack.getStackItem(0);
				OpcodeStack.Item item1 = stack.getStackItem(1);
				priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = adjustPriority(item0.getConstant(), priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG);
				priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = adjustPriority(item1.getConstant(), priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG);
				if (priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG >= LOW_PRIORITY && (item0.isInitialParameter() || item1.isInitialParameter()))
					priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = NORMAL_PRIORITY;
			}}
		if (prevOpCode == IMUL && seen == I2L)
			if (priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG <= best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG) {
				best_priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG = priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG;
			bugReporter.reportBug(new BugInstance(this, 
					"ICAST_INTEGER_MULTIPLY_CAST_TO_LONG", 
					priority_for_ICAST_INTEGER_MULTIPLY_CAST_TO_LONG)
						.addClassAndMethod(this)
						.addSourceLine(this));
			}
		
		if (getMethodName().equals("<clinit>") && (seen == PUTSTATIC || seen == GETSTATIC || seen == INVOKESTATIC)) {
			 String clazz = getClassConstantOperand();
			 if (!clazz.equals(getClassName())) {
				 try {
					 JavaClass targetClass = Repository.lookupClass(clazz);
					if (Repository.instanceOf(targetClass, getThisClass())) {
						int priority = NORMAL_PRIORITY;
						if (seen == GETSTATIC) priority--;
						if (!targetClass.isPublic()) priority++;
						 bugReporter.reportBug(new BugInstance(this, 
								 "IC_SUPERCLASS_USES_SUBCLASS_DURING_INITIALIZATION", 
								priority)
						 .addClassAndMethod(this).addClass(getDottedClassConstantOperand())
						 .addSourceLine(this)
							);
					}
				} catch (ClassNotFoundException e) {
					// ignore it
				}
				 
			 }
		}
         if (false && (seen == INVOKEVIRTUAL)
                &&   getNameConstantOperand().equals("equals")
                &&   getSigConstantOperand().equals("(Ljava/lang/Object;)Z")
		&& stack.getStackDepth() > 1) {
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);

			if (item0.isArray() || item1.isArray()) {
				bugReporter.reportBug(new BugInstance("EC_BAD_ARRAY_COMPARE", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));
		}
		}
 

         if (seen >= IALOAD && seen <= SALOAD || seen >= IASTORE && seen <= SASTORE ) {
        	 Item index  = stack.getStackItem(0);
        	 if (index.getSpecialKind() == Item.AVERAGE_COMPUTED_USING_DIVISION)
        		 bugReporter.reportBug(new BugInstance(this, "IM_AVERAGE_COMPUTATION_COULD_OVERFLOW", NORMAL_PRIORITY)
                 .addClassAndMethod(this)
                 .addSourceLine(this));
         }

		if ((seen == IFEQ || seen == IFNE) && getPrevOpcode(1) == IMUL
			&& ( getPrevOpcode(2) == SIPUSH
				|| getPrevOpcode(2) == BIPUSH
				)
			&& getPrevOpcode(3) == IREM
				)
			 bugReporter.reportBug(new BugInstance(this, "IM_MULTIPLYING_RESULT_OF_IREM", LOW_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));
		if (seen == I2S && getPrevOpcode(1) == IUSHR
				&& (!constantArgumentToShift || valueOfConstantArgumentToShift % 16 != 0)
			||
		    seen == I2B && getPrevOpcode(1) == IUSHR
				&& (!constantArgumentToShift || valueOfConstantArgumentToShift % 8 != 0)
			 )
			 bugReporter.reportBug(new BugInstance(this, "ICAST_QUESTIONABLE_UNSIGNED_RIGHT_SHIFT", LOW_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));

		constantArgumentToShift = false;
		if ( (seen == IUSHR 
				|| seen == ISHR 
				|| seen == ISHL )) {
			if (stack.getStackDepth() <= 1) {
				// don't understand; lie so other detectors won't get concerned
				constantArgumentToShift = true;
				valueOfConstantArgumentToShift = 8;
				}
			else {
			Object rightHandSide
				 = stack.getStackItem(0).getConstant();
			Object leftHandSide 
				=  stack.getStackItem(1).getConstant();
			if (rightHandSide instanceof Integer) {
				constantArgumentToShift = true;
				valueOfConstantArgumentToShift = ((Integer) rightHandSide);
				if (valueOfConstantArgumentToShift < 0 || valueOfConstantArgumentToShift >= 32)
				 bugReporter.reportBug(new BugInstance(this, "ICAST_BAD_SHIFT_AMOUNT", 
						 	valueOfConstantArgumentToShift < 0 ? LOW_PRIORITY : NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addInt(valueOfConstantArgumentToShift)
						.addSourceLine(this)
						);
				}
				if (leftHandSide != null 
					&& leftHandSide instanceof Integer
					&& ((Integer) leftHandSide)
						> 0) {
				// boring; lie so other detectors won't get concerned
				constantArgumentToShift = true;
				valueOfConstantArgumentToShift = 8;
					}
			}
			}



	   if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 0
                        && getClassConstantOperand().equals("java/util/Date")
                        && getNameConstantOperand().equals("setMonth")
                        && getSigConstantOperand().equals("(I)V")) {
			OpcodeStack.Item item = stack.getStackItem(0);
			Object o = item.getConstant();
			if (o != null && o instanceof Integer) {
				int v = (Integer) o;
				if (v < 0 || v > 11)
				 bugReporter.reportBug(new BugInstance(this, "DMI_BAD_MONTH", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addInt(v)
						.addCalledMethod(this)
						.addSourceLine(this)
					);
				}
		}
				
	   if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 1
                        && getClassConstantOperand().equals("java/util/Calendar")
                        && getNameConstantOperand().equals("set")
                        && getSigConstantOperand().equals("(III)V")
		||
	   	seen == INVOKESPECIAL && stack.getStackDepth() > 1
                        && getClassConstantOperand().equals("java/util/GregorianCalendar")
                        && getNameConstantOperand().equals("<init>")
                        && getSigConstantOperand().equals("(III)V")
		) {
			OpcodeStack.Item item = stack.getStackItem(1);
			Object o = item.getConstant();
			if (o != null && o instanceof Integer) {
				int v = (Integer) o;
				if (v < 0 || v > 11)
				 bugReporter.reportBug(new BugInstance(this, "DMI_BAD_MONTH", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addInt(v)
						.addCalledMethod(this)
						.addSourceLine(this)
						);
				}
		}
				


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
		
		
		// Java Puzzlers, Chapter 2, puzzle 1
		  // Look for ICONST_2 IREM ICONST_1  IF_ICMPNE L1
		
		switch (badlyComputingOddState) {
		case 0:
			if (seen == ICONST_2) badlyComputingOddState++;
			break;
		case 1:
			if (seen == IREM) badlyComputingOddState++;
			else badlyComputingOddState = 0;
			break;
		case 2:
			if (seen == ICONST_1) badlyComputingOddState++;
			else badlyComputingOddState = 0;
			break;
		case 3:
			if (seen == IF_ICMPEQ || seen == IF_ICMPNE) 
				 bugReporter.reportBug(new BugInstance(this, "IM_BAD_CHECK_FOR_ODD", NORMAL_PRIORITY)
                         .addClassAndMethod(this)
                         .addSourceLine(this));
				badlyComputingOddState = 0;
			break;
		}
		
		// Java Puzzlers, chapter 3, puzzle 12
		  if (seen == INVOKEVIRTUAL && stack.getStackDepth() > 0 
                  && (getNameConstantOperand().equals("toString")
                      && getSigConstantOperand().equals("()Ljava/lang/String;")
                      || getNameConstantOperand().equals("append")
                      && getSigConstantOperand().equals("(Ljava/lang/Object;)Ljava/lang/StringBuilder;") && getClassConstantOperand().equals("java/lang/StringBuilder")
                      || getNameConstantOperand().equals("append")
                      && getSigConstantOperand().equals("(Ljava/lang/Object;)Ljava/lang/StringBuffer;") && getClassConstantOperand().equals("java/lang/StringBuffer")
                      )
                  ) {
			  String classConstants = getClassConstantOperand();
			  OpcodeStack.Item item = stack.getStackItem(0);
			  String signature = item.getSignature();
			  if (signature != null && signature.startsWith("[")) 
					 bugReporter.reportBug(new BugInstance(this, "DMI_INVOKING_TOSTRING_ON_ARRAY", NORMAL_PRIORITY)
	                         .addClassAndMethod(this)
	                         .addSourceLine(this));
		  }

	
	
		stack.sawOpcode(this,seen);
		prevOpCode = seen;
	}

}
