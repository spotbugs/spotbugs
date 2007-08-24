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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collection;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;
import edu.umd.cs.findbugs.ba.ObjectTypeFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Detector for static fields of type {@link java.util.Calendar} or
 * {@link java.text.DateFormat} and their subclasses. Because {@link Calendar}
 * is unsafe for multithreaded use, static fields look suspicous. To work
 * correctly, all access would need to be synchronized by the client which
 * cannot be guaranteed.
 * 
 * @author Daniel Schneller
 */
public class StaticCalendarDetector extends OpcodeStackDetector {

	/** External Debug flag set? */
	private static final boolean DEBUG = Boolean.getBoolean("debug.staticcal");

	/**
	 * External flag to determine whether to skip the test for synchronized
	 * blocks (default: if a call on a static Calendar or DateFormat is detected
	 * inside a synchronizationb block, it will not be reported). Setting this
	 * to <code>true</code> will report method calls on static fields if they
	 * are in a synchronized block. As the check currently does not take into
	 * account the lock's mutex it may be useful to switch allow
	 */
	private static final String PROP_SKIP_SYNCHRONIZED_CHECK = "staticcal.skipsynccheck";

	/** The reporter to report to */
	final private BugReporter reporter;
	final private BugAccumulator bugAccumulator;

	/** Name of the class being inspected */
	private String currentClass;

	/**
	 * {@link ObjectType} for {@link java.util.Calendar}
	 */
	private final ObjectType calendarType = ObjectTypeFactory.getInstance("java.util.Calendar");

	/**
	 * {@link ObjectType} for {@link java.text.DateFormat}
	 */
	private final ObjectType dateFormatType = ObjectTypeFactory.getInstance("java.text.DateFormat");

	/** Stores the current method */
	private Method currentMethod = null;

	/** Stores current Control Flow Graph */
	private CFG currentCFG;

	/** Stores current LDF */
	private LockDataflow currentLockDataFlow;

	/**
	 * Creates a new instance of this Detector.
	 * 
	 * @param aReporter
	 *            {@link BugReporter} instance to report found problems to.
	 */
	public StaticCalendarDetector(BugReporter aReporter) {
		reporter = aReporter;
		bugAccumulator = new BugAccumulator(reporter);
	}

	private boolean sawDateClass;
	/**
	 * Remembers the class name and resets temporary fields.
	 */
	@Override
	public void visit(JavaClass someObj) {
		currentClass = someObj.getClassName();
		currentMethod = null;
		currentCFG = null;
		currentLockDataFlow = null;
		sawDateClass = false;

	}

	@Override
	public void visit(ConstantPool pool) {
		for(Constant constant : pool.getConstantPool()) {
			if (constant instanceof ConstantClass) {
				ConstantClass cc = (ConstantClass) constant;
				String className = cc.getBytes(pool);
				if (className.equals("java/util/Calendar") || className.equals("java/text/DateFormat"))
						sawDateClass = true;
			}
		}
	}
	/**
	 * Checks if the visited field is of type {@link Calendar} or
	 * {@link DateFormat} or a subclass of either one. If so and the field is
	 * static it is suspicious and will be reported.
	 */
	@Override
	public void visit(Field aField) {
		super.visit(aField);
		String tFieldSig = aField.getSignature();
		if (aField.getType() instanceof ObjectType) {
			String tBugType = null;
			ObjectType tType = (ObjectType) aField.getType();
			try {
				if (tType.subclassOf(calendarType) && aField.isStatic()) {
					tBugType = "STCAL_STATIC_CALENDAR_INSTANCE";
				} else if (tType.subclassOf(dateFormatType) && aField.isStatic()) {
					tBugType = "STCAL_STATIC_SIMPLE_DATA_FORMAT_INSTANCE";
				}
				if (tBugType != null) {
					reporter.reportBug(new BugInstance(this, tBugType, aField.isPublic() ? HIGH_PRIORITY : NORMAL_PRIORITY).addClass(currentClass).addField(
					        currentClass, aField.getName(), tFieldSig, true));
				}
			} catch (ClassNotFoundException e) {
				AnalysisContext.reportMissingClass(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.visitclass.BetterVisitor#visitMethod(org.apache.bcel.classfile.Method)
	 */
	@Override
	public void visitMethod(Method obj) {
		if (sawDateClass) try {
			super.visitMethod(obj);
			currentMethod = obj;
			currentLockDataFlow = getClassContext().getLockDataflow(currentMethod);
			currentCFG = getClassContext().getCFG(currentMethod);
		} catch (CFGBuilderException e) {
			reporter.logError("Synchronization check in Static Calendar Detector caught an error.", e);
		} catch (DataflowAnalysisException e) {
			reporter.logError("Synchronization check in Static Calendar Detector caught an error.", e);
		}
	}
	
	@Override public void visit(Code obj) {
		if (sawDateClass) {
			super.visit(obj);
			bugAccumulator.reportAccumulatedBugs();
		}
	}

	/**
	 * Checks for method invocations ({@link org.apache.bcel.generic.INVOKEVIRTUAL})
	 * call on a static {@link Calendar} or {@link DateFormat} fields. The
	 * {@link OpcodeStack} is used to determine if an invocation is done on such
	 * a static field.
	 * 
	 * @param seen
	 *            An opcode to be analyzed
	 * @see edu.umd.cs.findbugs.visitclass.DismantleBytecode#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {
		// we are only interested in method calls
		if (seen != INVOKEVIRTUAL) {
			return;
		}

		try {
			String className = getClassConstantOperand();
			
			if (className.startsWith("[")) {
				// Ignore array classes
				return;
			}
			
			// determine type of the object the method is invoked on
			ObjectType tType = ObjectTypeFactory.getInstance(className);

			// if it is not compatible with Calendar or DateFormat, we are not
			// interested anymore
			if (!tType.subclassOf(calendarType) && !tType.subclassOf(dateFormatType)) {
				return;
			}

			// determine the number of arguments the method expects
			int numArguments = getNumberArguments(getSigConstantOperand());
			// go back on the stack to find what the receiver of the method is
			OpcodeStack.Item invokedOn = stack.getStackItem(numArguments);
			XField field = invokedOn.getXField();
			// find out, if the field is static. if not, we are not interested
			// anymore
			if (field == null || !field.isStatic()) {
				return;
			}

			if (getMethodName().equals("<clinit>") && field.getClassName().equals(getDottedClassName())) return;
			if (getNameConstantOperand().equals("equals") && numArguments == 1) {
				OpcodeStack.Item passedAsArgument = stack.getStackItem(0);
				field = passedAsArgument.getXField();
				if (field == null || !field.isStatic()) {
					return;
				}
			}

			if (!SystemProperties.getBoolean(PROP_SKIP_SYNCHRONIZED_CHECK)) {
				// check synchronization
				try {
					if (currentMethod != null && currentLockDataFlow != null && currentCFG != null) {
						Collection<Location> tLocations = currentCFG.getLocationsContainingInstructionWithOffset(getPC());
						for (Location tLoc : tLocations) {
							LockSet lockSet = currentLockDataFlow.getFactAtLocation(tLoc);
							if (lockSet.getNumLockedObjects() > 0) {
								// within a synchronized block
								return;
							}
						}
					}
				} catch (DataflowAnalysisException e) {
					reporter.logError("Synchronization check in Static Calendar Detector caught an error.", e);
				}
			}

			// if we get here, we want to generate a report, depending on the
			// type
			String tBugType = null;
			if (tType.subclassOf(calendarType)) {
				tBugType = "STCAL_INVOKE_ON_STATIC_CALENDAR_INSTANCE";
			} else if (tType.subclassOf(dateFormatType)) {
				tBugType = "STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE";
			}
			if (tBugType != null) {
				bugAccumulator.accumulateBug(new BugInstance(this, tBugType, NORMAL_PRIORITY).addClassAndMethod(this).addCalledMethod(this)
				        .addOptionalField(field), this);
			}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
	}

}
