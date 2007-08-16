/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Detector to find calls to Number constructors with base type argument in
 * Java 5 or newer bytecode.
 * 
 * Using <code>new Integer(int)</code> is guaranteed to always result in a
 * new object whereas <code>Integer.valueOf(int)</code> allows caching of
 * values to be done by the javac, JVM class library or JIT.
 * 
 * Currently only the JVM class library seems to do caching in the range of
 * -128 to 127. There does not seem to be any caching for float and double
 * which is why those are reported as low priority.
 * 
 * All invokes of Number constructor with a constant argument are
 * flagged as high priority and invokes with unknwon value are normal priority.
 * 
 * @author Mikko Tiihonen
 */
public class NumberConstructor extends BytecodeScanningDetector {

  private final Map<String, XMethod> boxClasses = new HashMap<String, XMethod>();
  private final BugAccumulator bugAccumulator;
  private final BugReporter bugReporter;
  private boolean constantArgument;

  /**
   * Constructs a NC detector given the reporter to report bugs on
   * @param bugReporter the sync of bug reports
   */
  public NumberConstructor(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	this.bugAccumulator = new BugAccumulator(bugReporter);
	handle("java/lang/Byte", false, "(B)V");
	handle("java/lang/Character", false, "(C)V");
	handle("java/lang/Short", false, "(S)V");
	handle("java/lang/Integer", false, "(I)V");
	handle("java/lang/Long", false, "(J)V");
	handle("java/lang/Float", true, "(F)V");
	handle("java/lang/Double", true, "(D)V");

  }

  private void handle(@SlashedClassName String className, boolean isFloatingPoint, String sig) {
	  XMethod m = XFactory.createXMethod(ClassName.toDottedClassName(className), "valueOf", sig, true);
	  boxClasses.put(className, m);
  }
  /**
   * The detector is only meaningful for Java5 class libraries.
   * 
   * @param classContext the context object that holds the JavaClass parsed
   */
  @Override
  public void visitClassContext(ClassContext classContext) {
	int majorVersion = classContext.getJavaClass().getMajor();
	if (majorVersion >= MAJOR_1_5) {
	  super.visitClassContext(classContext);
	}
  }
  
  @Override
  public void visit(Code obj) {
	  super.visit(obj);
	  bugAccumulator.reportAccumulatedBugs();
  }
  @Override
  public void sawOpcode(int seen) {
	// detect if previous op was a constant number
	if (seen == ICONST_0 || seen == LCONST_0 ||
	seen == ICONST_1 || seen == LCONST_1 ||
	seen == ICONST_2 || seen == ICONST_3 ||
	seen == ICONST_4 || seen == ICONST_5 ||
	seen == BIPUSH || seen == LDC) {
	  constantArgument = true;
	  return;
	}

	// only acts on constructor invoke
	if (seen != INVOKESPECIAL) {
	  constantArgument = false;
	  return;
	}

	if (!"<init>".equals(getNameConstantOperand())) {
	  return;
	}
	String cls = getClassConstantOperand(); 
	XMethod shouldCall = boxClasses.get(cls);
	if (shouldCall == null) {
	  return;
	}

	if (!shouldCall.getSignature().substring(0,3).equals(getSigConstantOperand().substring(0,3))) {
	  return;
	}

	int prio;
	String type;
	if (cls.equals("java/lang/Float") || cls.equals("java/lang/Double")) {
	  prio = LOW_PRIORITY;
	  type = "DM_FP_NUMBER_CTOR";
	} else {
	  prio = NORMAL_PRIORITY;
	  type = "DM_NUMBER_CTOR";
	}

	cls = cls.substring(cls.lastIndexOf('/')+1);
	bugAccumulator.accumulateBug(
			new BugInstance(this, type, prio)
			  .addClass(this)
			  .addMethod(this)
			  .addCalledMethod(this)
			  .addMethod(shouldCall).describe("SHOULD_CALL"), 
			this);
  }
}
