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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.*;

public class DumbMethods extends BytecodeScanningDetector implements Constants2 {

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
	private boolean isPublicStaticVoidMain;
        private int randomNextIntState;

	public DumbMethods(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Method method) {
		flush();
		String cName = getDottedClassName();

		isPublicStaticVoidMain = method.isPublic() && method.isStatic()
		        && getMethodName().equals("main")
		        || cName.toLowerCase().indexOf("benchmark") >= 0;
		// System.out.println("method " + getMethodName() + " is main? " + isPublicStaticVoidMain);
		Code code = method.getCode();
		if (code != null)
			this.exceptionTable = code.getExceptionTable();
		if (this.exceptionTable == null)
			this.exceptionTable = new CodeException[0];
		primitiveObjCtorSeen = null;
		ctorSeen = false;
		randomNextIntState = 0;
	}

	public void sawOpcode(int seen) {

		switch(randomNextIntState) {
		case 0:
			if (seen == INVOKEVIRTUAL
				&& getClassConstantOperand().equals("java/util/Random")
				&& getNameConstantOperand().equals("nextDouble"))
			  randomNextIntState = 1;
			break;
		case 1:
			randomNextIntState = 2;
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

		if ((seen == INVOKESPECIAL)
		        && getClassConstantOperand().equals("java/lang/String")
		        && getNameConstantOperand().equals("<init>")
		        && getSigConstantOperand().equals("(Ljava/lang/String;)V"))
			if (alreadyReported.add(getRefConstantOperand()))
				bugReporter.reportBug(new BugInstance(this, "DM_STRING_CTOR", NORMAL_PRIORITY)
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
		        && !getMethodName().startsWith("windowClos"))
			bugReporter.reportBug(new BugInstance(this, "DM_EXIT", LOW_PRIORITY)
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
	}

	public void report() {
		flush();
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
			for (int i = 0; i < exceptionTable.length; ++i) {
				CodeException handler = exceptionTable[i];
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
