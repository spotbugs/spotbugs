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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.JavaVersion;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.OpcodeStack.Item;

public class DumbMethods extends BytecodeScanningDetector  {
	
	private static final ObjectType CONDITION_TYPE = new ObjectType("java.util.concurrent.locks.Condition");

	private HashSet<String> alreadyReported = new HashSet<String>();
	private BugReporter bugReporter;
	private boolean sawCurrentTimeMillis;
	private BugInstance gcInvocationBugReport;
	private int gcInvocationPC;
	private CodeException[] exceptionTable;
/*
   private boolean sawLDCEmptyString;
*/
	private String primitiveObjCtorSeen;
	private boolean ctorSeen;
	private boolean prevOpcodeWasReadLine;
	private int prevOpcode;
	private boolean isPublicStaticVoidMain;
	private int randomNextIntState;
	private boolean checkForBitIorofSignedByte;
	
	private boolean jdk15ChecksEnabled;

	public DumbMethods(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		
		jdk15ChecksEnabled = JavaVersion.getRuntimeVersion().isSameOrNewerThan(JavaVersion.JAVA_1_5);
	}
	
	
	OpcodeStack stack = new OpcodeStack();
	
	@Override
         public void visit(Method method) {
		String cName = getDottedClassName();
		stack.resetForMethodEntry(this);
		
		isPublicStaticVoidMain = method.isPublic() && method.isStatic()
		        && getMethodName().equals("main")
		        || cName.toLowerCase().indexOf("benchmark") >= 0;
		prevOpcodeWasReadLine = false;
		pendingRemOfRandomIntBug = null;
		Code code = method.getCode();
		if (code != null)
			this.exceptionTable = code.getExceptionTable();
		if (this.exceptionTable == null)
			this.exceptionTable = new CodeException[0];
		primitiveObjCtorSeen = null;
		ctorSeen = false;
		randomNextIntState = 0;
		checkForBitIorofSignedByte = false;
	}

	BugInstance pendingRemOfRandomIntBug;
	@Override
         public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		if (pendingRemOfRandomIntBug != null && !(seen == INVOKESTATIC
				&& getClassConstantOperand().equals("java/lang/Math")
				&& getNameConstantOperand().equals("abs"))) 
			bugReporter.reportBug(pendingRemOfRandomIntBug);

		pendingRemOfRandomIntBug = null;
		try {

		if (seen == IREM) {
			OpcodeStack.Item item0 = stack.getStackItem(0);
			Object constant0 = item0.getConstant();
			OpcodeStack.Item item1 = stack.getStackItem(1);
			int special = item1.getSpecialKind();
			if (special == OpcodeStack.Item.RANDOM_INT) {
				pendingRemOfRandomIntBug = new BugInstance(this, "RV_REM_OF_RANDOM_INT", HIGH_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this);
						}
			else if (constant0 instanceof Integer && ((Integer)constant0).intValue() == 1)
			bugReporter.reportBug(new BugInstance(this, "INT_BAD_REM_BY_1", HIGH_PRIORITY)
			.addClassAndMethod(this)
			.addSourceLine(this));
			
			
		}
		if (checkForBitIorofSignedByte && seen != I2B) {
			  bugReporter.reportBug(new BugInstance(this, "BIT_IOR_OF_SIGNED_BYTE", 
					prevOpcode == LOR ? HIGH_PRIORITY : NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
			  checkForBitIorofSignedByte = false;
		} else if ((seen == IOR || seen == LOR) && stack.getStackDepth() >= 2) {
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);
			
			int special0 = item0.getSpecialKind();
			int special1 = item1.getSpecialKind();
			if  (special0 == OpcodeStack.Item.BYTE_ARRAY_LOAD  
					&& special1 == OpcodeStack.Item.LOW_8_BITS_CLEAR
					|| special0 == OpcodeStack.Item.LOW_8_BITS_CLEAR && special1 == OpcodeStack.Item.BYTE_ARRAY_LOAD )
				checkForBitIorofSignedByte = true;
			else checkForBitIorofSignedByte = false;
		} else checkForBitIorofSignedByte = false;

	if (prevOpcodeWasReadLine && seen == INVOKEVIRTUAL
		&& getClassConstantOperand().equals("java/lang/String")) {
	  bugReporter.reportBug(new BugInstance(this, "NP_IMMEDIATE_DEREFERENCE_OF_READLINE", NORMAL_PRIORITY)
		.addClassAndMethod(this)
		.addSourceLine(this));
		}

	prevOpcodeWasReadLine =
		(seen == INVOKEVIRTUAL||seen == INVOKEINTERFACE)
		&& getNameConstantOperand().equals("readLine")
		&& getSigConstantOperand().equals("()Ljava/lang/String;");

		
	switch(randomNextIntState) {
		case 0:
			if (seen == INVOKEVIRTUAL
				&& getClassConstantOperand().equals("java/util/Random")
				&& getNameConstantOperand().equals("nextLong")
			   || seen == INVOKESTATIC
				&& getClassConstantOperand().equals("java/lang/Math")
				&& getNameConstantOperand().equals("random"))
			  randomNextIntState = 1;
			break;
		case 1:
			if (seen == D2I) {
			  bugReporter.reportBug(new BugInstance(this, "RV_01_TO_INT", HIGH_PRIORITY)
			        .addClassAndMethod(this)
			        .addSourceLine(this));
			  randomNextIntState = 0;
			  }
			else randomNextIntState = 2;
			break;
		case 2:
			if (seen == I2D) randomNextIntState = 3;
			else randomNextIntState = 0;
			break;
		case 3:
			if (seen == DMUL) randomNextIntState = 4;
			else randomNextIntState = 0;
			break;
		case 4:
			if (seen == D2I) 
			  bugReporter.reportBug(new BugInstance(this, "DM_NEXTINT_VIA_NEXTDOUBLE", NORMAL_PRIORITY)
			        .addClassAndMethod(this)
			        .addSourceLine(this));
			randomNextIntState = 0;
			break;
		default:
			throw new IllegalStateException();
			}
		if (isPublicStaticVoidMain && seen == INVOKEVIRTUAL
		        && getClassConstantOperand().startsWith("javax/swing/")
		        && (getNameConstantOperand().equals("show")
		        && getSigConstantOperand().equals("()V")
		        || getNameConstantOperand().equals("pack")
		        && getSigConstantOperand().equals("()V")
		        || getNameConstantOperand().equals("setVisible")
		        && getSigConstantOperand().equals("(Z)V")))
			bugReporter.reportBug(new BugInstance(this, "SW_SWING_METHODS_INVOKED_IN_SWING_THREAD", LOW_PRIORITY)
			        .addClassAndMethod(this)
			        .addSourceLine(this));
		
//		if ((seen == INVOKEVIRTUAL)
//				&& getClassConstantOperand().equals("java/lang/String")
//				&& getNameConstantOperand().equals("substring")
//				&& getSigConstantOperand().equals("(I)Ljava/lang/String;")
//				&& stack.getStackDepth() > 1) {
//			OpcodeStack.Item item = stack.getStackItem(0);
//			Object o = item.getConstant();
//			if (o != null && o instanceof Integer) {
//				int v = ((Integer) o).intValue();
//				if (v == 0)
//					bugReporter.reportBug(new BugInstance(this, "DMI_USELESS_SUBSTRING", NORMAL_PRIORITY)
//					        .addClassAndMethod(this)
//					        .addSourceLine(this));
//			}
//		}
	
		if ((seen == INVOKEVIRTUAL)
				&& getNameConstantOperand().equals("next")
				&& getSigConstantOperand().equals("()Ljava/lang/Object;")
				&& getMethodName().equals("hasNext")
				&& getMethodSig().equals("()Z")
				&& stack.getStackDepth() > 0) {
			OpcodeStack.Item item = stack.getStackItem(0);
			
				bugReporter.reportBug(new BugInstance(this, "DMI_CALLING_NEXT_FROM_HASNEXT",
						item.isInitialParameter() && item.getRegisterNumber() == 0 ? NORMAL_PRIORITY : LOW_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this)
				        .addCalledMethod(this));
			
		}
			
	
		if ((seen == INVOKESPECIAL)
		        && getClassConstantOperand().equals("java/lang/String")
		        && getNameConstantOperand().equals("<init>")
		        && getSigConstantOperand().equals("(Ljava/lang/String;)V"))
			if (alreadyReported.add(getRefConstantOperand()))
				bugReporter.reportBug(new BugInstance(this, "DM_STRING_CTOR", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		if (seen == INVOKESTATIC
		        && getClassConstantOperand().equals("java/lang/System")
		        && getNameConstantOperand().equals("runFinalizersOnExit")
		    || seen == INVOKEVIRTUAL
		        && getClassConstantOperand().equals("java/lang/Runtime")
		        && getNameConstantOperand().equals("runFinalizersOnExit"))
				bugReporter.reportBug(new BugInstance(this, "DM_RUN_FINALIZERS_ON_EXIT", HIGH_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		if ((seen == INVOKESPECIAL)
		        && getClassConstantOperand().equals("java/lang/String")
		        && getNameConstantOperand().equals("<init>")
		        && getSigConstantOperand().equals("()V"))
			if (alreadyReported.add(getRefConstantOperand()))
				bugReporter.reportBug(new BugInstance(this, "DM_STRING_VOID_CTOR", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		if (!isPublicStaticVoidMain && seen == INVOKESTATIC
		        && getClassConstantOperand().equals("java/lang/System")
		        && getNameConstantOperand().equals("exit")
		        && !getMethodName().equals("processWindowEvent")
		        && !getMethodName().startsWith("windowClos")
		        && getMethodName().indexOf("exit") == -1
		        && getMethodName().indexOf("Exit") == -1
		        && getMethodName().indexOf("crash") == -1
		        && getMethodName().indexOf("Crash") == -1
		        && getMethodName().indexOf("die") == -1
		        && getMethodName().indexOf("Die") == -1
		        && getMethodName().indexOf("main") == -1)
			bugReporter.reportBug(new BugInstance(this, "DM_EXIT", 
				getMethod().isStatic() ? LOW_PRIORITY : NORMAL_PRIORITY)
			        .addClassAndMethod(this)
			        .addSourceLine(this));
		if (((seen == INVOKESTATIC
		        && getClassConstantOperand().equals("java/lang/System"))
		        || (seen == INVOKEVIRTUAL
		        && getClassConstantOperand().equals("java/lang/Runtime")))
		        && getNameConstantOperand().equals("gc")
		        && getSigConstantOperand().equals("()V")
		        && !getDottedClassName().startsWith("java.lang")
		        && !getMethodName().startsWith("gc")
		        && !getMethodName().endsWith("gc"))
			if (alreadyReported.add(getRefConstantOperand())) {
				// System.out.println("Saw call to GC");
				if (isPublicStaticVoidMain) {
					// System.out.println("Skipping GC complaint in main method");
					return;
				}
				// Just save this report in a field; it will be flushed
				// IFF there were no calls to System.currentTimeMillis();
				// in the method.
				gcInvocationBugReport = new BugInstance(this, "DM_GC", HIGH_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this);
				gcInvocationPC = getPC();
				//System.out.println("GC invocation at pc " + PC);
			}
		if ((seen == INVOKESPECIAL)
		        && getClassConstantOperand().equals("java/lang/Boolean")
		        && getNameConstantOperand().equals("<init>")
		        && !getClassName().equals("java/lang/Boolean")
		)
			if (alreadyReported.add(getRefConstantOperand()))
				bugReporter.reportBug(new BugInstance(this, "DM_BOOLEAN_CTOR", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		if ((seen == INVOKESTATIC)
		        && getClassConstantOperand().equals("java/lang/System")
		        && (getNameConstantOperand().equals("currentTimeMillis")
		           || getNameConstantOperand().equals("nanoTime")))
			sawCurrentTimeMillis = true;
		if ((seen == INVOKEVIRTUAL)
		        && getClassConstantOperand().equals("java/lang/String")
		        && getNameConstantOperand().equals("toString")
		        && getSigConstantOperand().equals("()Ljava/lang/String;"))
			if (alreadyReported.add(getRefConstantOperand()))
				bugReporter.reportBug(new BugInstance(this, "DM_STRING_TOSTRING", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		if ((seen == INVOKEVIRTUAL)
		        && getClassConstantOperand().equals("java/lang/String")
		        && (getNameConstantOperand().equals("toUpperCase")
		        ||  getNameConstantOperand().equals("toLowerCase"))
		        && getSigConstantOperand().equals("()Ljava/lang/String;"))
			if (alreadyReported.add(getRefConstantOperand()))
				bugReporter.reportBug(new BugInstance(this, "DM_CONVERT_CASE", LOW_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		
		if ((seen == INVOKESPECIAL) && getNameConstantOperand().equals("<init>")) {
			String cls = getClassConstantOperand();
			String sig = getSigConstantOperand();
			if ((cls.equals("java/lang/Integer") && sig.equals("(I)V"))
			||  (cls.equals("java/lang/Float") && sig.equals("(F)V"))
			||  (cls.equals("java/lang/Double") && sig.equals("(D)V"))
			||  (cls.equals("java/lang/Long") && sig.equals("(J)V"))
			||  (cls.equals("java/lang/Byte") && sig.equals("(B)V"))
			||  (cls.equals("java/lang/Character") && sig.equals("(C)V"))
			||  (cls.equals("java/lang/Short") && sig.equals("(S)V"))
			||  (cls.equals("java/lang/Boolean") && sig.equals("(Z)V"))) {
				primitiveObjCtorSeen = cls;
			} else {
				primitiveObjCtorSeen = null;
			}
		} else if ((primitiveObjCtorSeen != null)
		       &&  (seen == INVOKEVIRTUAL) 
		       &&   getNameConstantOperand().equals("toString")
		       &&   getClassConstantOperand().equals(primitiveObjCtorSeen)
		       &&   getSigConstantOperand().equals("()Ljava/lang/String;")) {
				bugReporter.reportBug(new BugInstance(this, "DM_BOXED_PRIMITIVE_TOSTRING", LOW_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
			primitiveObjCtorSeen = null;
		}
		else
			primitiveObjCtorSeen = null;
			
		if ((seen == INVOKESPECIAL) && getNameConstantOperand().equals("<init>")) {
			ctorSeen = true;
		} else if (ctorSeen 
		        && (seen == INVOKEVIRTUAL) 
		        && getClassConstantOperand().equals("java/lang/Object")
		        && getNameConstantOperand().equals("getClass")
		        && getSigConstantOperand().equals("()Ljava/lang/Class;")) {
					bugReporter.reportBug(new BugInstance(this, "DM_NEW_FOR_GETCLASS", LOW_PRIORITY)
					        .addClassAndMethod(this)
					        .addSourceLine(this));
			ctorSeen = false;
		} else {
			ctorSeen = false;
		}

		if (jdk15ChecksEnabled
				&& (seen == INVOKEVIRTUAL)
				&& isMonitorWait(getNameConstantOperand(), getSigConstantOperand())) {
			checkMonitorWait();
		}


		if ((seen == INVOKESPECIAL) 
		&&  getNameConstantOperand().equals("<init>")
		&&  getClassConstantOperand().equals("java/lang/Thread")) {
			String sig = getSigConstantOperand();
			if (sig.equals("()V")
			||  sig.equals("(Ljava/lang/String;)V")
			||  sig.equals("(Ljava/lang/ThreadGroup;Ljava/lang/String;)V"))
				if (!getMethodName().equals("<init>") || (getPC() > 20)) {
					bugReporter.reportBug(new BugInstance(this, "DM_USELESS_THREAD", LOW_PRIORITY)
					        .addClassAndMethod(this)
					        .addSourceLine(this));
				}
		}
			
		
/*
	//
	// TODO: put this back in when we have a standard way
	// of enabling and disabling warnings on a per-bug-pattern
	// basis.
	//
	if ((seen == INVOKEVIRTUAL)
				&& sawLDCEmptyString
				&& getNameConstantOperand().equals("equals"))
		bugReporter.reportBug(new BugInstance("DM_STRING_EMPTY_EQUALS", LOW_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if ((seen == LDC)
				&& (getConstantRefOperand() instanceof ConstantString)
				&& (getStringConstantOperand().length() == 0))
		sawLDCEmptyString = true;
	else
		sawLDCEmptyString = false;
*/
		
	} finally {
		stack.sawOpcode(this,seen);
		prevOpcode = seen;
	}
	}

	private void checkMonitorWait() {
		try {
			TypeDataflow typeDataflow = getClassContext().getTypeDataflow(getMethod());
			TypeDataflow.LocationAndFactPair pair = typeDataflow.getLocationAndFactForInstruction(getPC());
			
			if (pair == null)
				return;

			Type receiver = pair.frame.getInstance(
					pair.location.getHandle().getInstruction(),
					getClassContext().getConstantPoolGen()
			);
			
			if (!(receiver instanceof ReferenceType))
				return;
			
			if (Hierarchy.isSubtype((ReferenceType) receiver, CONDITION_TYPE)) {
				bugReporter.reportBug(new BugInstance("DM_MONITOR_WAIT_ON_CONDITION", HIGH_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("Exception caught by DumbMethods", e);
		} catch (CFGBuilderException e) {
			bugReporter.logError("Exception caught by DumbMethods", e);
		}
	}

	private boolean isMonitorWait(String name, String sig) {
//		System.out.println("Check call " + name + "," + sig);
		return name.equals("wait")
				&& (sig.equals("()V") || sig.equals("(J)V") || sig.equals("(JI)V"));
	}

	@Override
	public void visit(Code obj) {
		super.visit(obj);
		flush();
	}
	@Override
         public void report() {
	}

	/**
	 * A heuristic - how long a catch block for OutOfMemoryError might be.
	 */
	private static final int OOM_CATCH_LEN = 20;

	/**
	 * Flush out cached state at the end of a method.
	 */
	private void flush() {
		if (gcInvocationBugReport != null && !sawCurrentTimeMillis) {
			// Make sure the GC invocation is not in an exception handler
			// for OutOfMemoryError.
			boolean outOfMemoryHandler = false;
			for (CodeException handler : exceptionTable) {
				if (gcInvocationPC < handler.getHandlerPC() ||
						gcInvocationPC > handler.getHandlerPC() + OOM_CATCH_LEN)
					continue;
				int catchTypeIndex = handler.getCatchType();
				if (catchTypeIndex > 0) {
					ConstantPool cp = getThisClass().getConstantPool();
					Constant constant = cp.getConstant(catchTypeIndex);
					if (constant instanceof ConstantClass) {
						String exClassName = (String) ((ConstantClass) constant).getConstantValue(cp);
						if (exClassName.equals("java/lang/OutOfMemoryError")) {
							outOfMemoryHandler = true;
							break;
						}
					}
				}
			}

			if (!outOfMemoryHandler)
				bugReporter.reportBug(gcInvocationBugReport);
		}

		sawCurrentTimeMillis = false;
		gcInvocationBugReport = null;
		alreadyReported.clear();
		exceptionTable = null;
	}
}
