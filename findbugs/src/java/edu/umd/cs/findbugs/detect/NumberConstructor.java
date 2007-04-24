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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;

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

  /**
   * Immutable information class of one handled number types.
   */
  static class Info {
	public final boolean isRealNumber;
	public final String argName;       
	public final String constrArgs;

	public Info(boolean isRealNumber, String argName, String constrArgs) {
	  this.isRealNumber = isRealNumber;
	  this.argName = argName;
	  this.constrArgs = constrArgs;         
	}
  }

  private static final Map<String, Info> boxClasses = new HashMap<String, Info>();
  static {
	boxClasses.put("java/lang/Byte", new Info(false, "byte", "(B)V"));
	boxClasses.put("java/lang/Character", new Info(false, "char", "(C)V"));
	boxClasses.put("java/lang/Short", new Info(false, "short", "(S)V"));
	boxClasses.put("java/lang/Integer", new Info(false, "int", "(I)V"));
	boxClasses.put("java/lang/Long", new Info(false, "long", "(J)V"));
	boxClasses.put("java/lang/Float", new Info(true, "float", "(F)V"));
	boxClasses.put("java/lang/Double", new Info(true, "double", "(D)V"));
  }

  private final BugReporter bugReporter;
  private boolean constantArgument;

  /**
   * Constructs a NC detector given the reporter to report bugs on
   * @param bugReporter the sync of bug reports
   */
  public NumberConstructor(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
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
	Info info = boxClasses.get(cls);
	if (info == null) {
	  return;
	}

	if (!info.constrArgs.equals(getSigConstantOperand())) {
	  return;
	}

	int prio;
	String type;
	if (info.isRealNumber) {
	  prio = LOW_PRIORITY;
	  type = "DM_FP_NUMBER_CTOR";
	} else {
	  prio = NORMAL_PRIORITY;
	  type = "DM_NUMBER_CTOR";
	}

	cls = cls.substring(cls.lastIndexOf('/')+1);
	bugReporter.reportBug(new BugInstance(this, type, prio)
			  .addClass(this)
			  .addMethod(this)
			  .addSourceLine(this)
			  .addString(cls+"("+info.argName+")")
			  .addString(cls+".valueOf("+info.argName+")"));
  }
}
