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
import org.apache.bcel.classfile.*;
import java.util.zip.*;
import java.io.*;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class DumbMethods extends BytecodeScanningDetector implements   Constants2 {

   private HashSet<String> alreadyReported = new HashSet<String>();
   private BugReporter bugReporter;
   private boolean sawCurrentTimeMillis;
   private BugInstance gcInvocationBugReport;
   private int gcInvocationPC;
   private CodeException[] exceptionTable;
   private boolean sawLDCEmptyString;

   public DumbMethods(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
   }

   public void visit(Method method) {
	flush();

	Code code = method.getCode();
	if (code != null)
		this.exceptionTable = code.getExceptionTable();
	if (this.exceptionTable == null)
		this.exceptionTable = new CodeException[0];
   }

   public void sawOpcode(int seen) {
	if ((seen == INVOKESPECIAL)
				&& classConstant.equals("java/lang/String")
				&& nameConstant.equals("<init>")
				&& sigConstant.equals("(Ljava/lang/String;)V"))
		if (alreadyReported.add(refConstant))
			bugReporter.reportBug(new BugInstance("DM_STRING_CTOR", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if ((seen == INVOKESPECIAL)
				&& classConstant.equals("java/lang/String")
				&& nameConstant.equals("<init>")
				&& sigConstant.equals("()V"))
		if (alreadyReported.add(refConstant))
			bugReporter.reportBug(new BugInstance("DM_STRING_VOID_CTOR", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if (((seen == INVOKESTATIC
				&& classConstant.equals("java/lang/System"))
	    || (seen == INVOKEVIRTUAL
				&& classConstant.equals("java/lang/Runtime")))
				&& nameConstant.equals("gc")
				&& sigConstant.equals("()V")
				&& !betterClassName.startsWith("java.lang"))
		if (alreadyReported.add(refConstant)) {
			// Just save this report in a field; it will be flushed
			// IFF there were no calls to System.currentTimeMillis();
			// in the method.
			gcInvocationBugReport = new BugInstance("DM_GC", HIGH_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this);
			gcInvocationPC = PC;
			//System.out.println("GC invocation at pc " + PC);
			}
	if ((seen == INVOKESPECIAL)
				&& classConstant.equals("java/lang/Boolean")
				&& nameConstant.equals("<init>")
				&& !className.equals("java/lang/Boolean")
				)
		if (alreadyReported.add(refConstant))
			bugReporter.reportBug(new BugInstance("DM_BOOLEAN_CTOR", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if ((seen == INVOKESTATIC)
				&& classConstant.equals("java/lang/System")
				&& nameConstant.equals("currentTimeMillis"))
			sawCurrentTimeMillis = true;
	if ((seen == INVOKEVIRTUAL)
				&& sawLDCEmptyString
				&& nameConstant.equals("equals"))
		bugReporter.reportBug(new BugInstance("DM_STRING_EMPTY_EQUALS", LOW_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
	if ((seen == LDC)
				&& (constantRef instanceof ConstantString)
				&& (stringConstant.length() == 0))
		sawLDCEmptyString = true;
	else
		sawLDCEmptyString = false;
   }

   public void report() {
	flush();
   }

   /** A heuristic - how long a catch block for OutOfMemoryError might be. */
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
				ConstantPool cp = thisClass.getConstantPool();
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
