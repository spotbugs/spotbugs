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
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
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

	/** The reporter to report to */
	private BugReporter reporter;

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

	/** ProgramCounter of the last seen  {@link org.apache.bcel.generic.GETSTATIC} call to a {@link Calendar} */
	private int seenStaticGetCalendarAt;

	/** ProgramCounter of the last seen  {@link org.apache.bcel.generic.GETSTATIC} call to a {@link DateFormat} */
	private int seenStaticGetDateFormatAt;

	/**
	 * Map of stores of a static {@link Calendar} instance to a register.
	 * Keys: Registers, Values: ProgramCounters
	 */
	private Map<Integer, Integer> registerStaticStoreCalendarAt = new HashMap<Integer, Integer>();

	/**
	 * Map of stores of a static {@link DateFormat} instance to a register.
	 * Keys: Registers, Values: ProgramCounters
	 */
	private Map<Integer, Integer> registerStaticStoreDateFormatAt = new HashMap<Integer, Integer>();

	/**
	 * Remembers the class name
	 */
	@Override
	public void visit(JavaClass someObj) {
		currentClass = someObj.getClassName();
		super.visit(someObj);
	}

	/**
	 * Checks if the visited field is of type {@link Calendar} or
	 * {@link DateFormat} or a subclass of either one. If so and the field is
	 * static it is suspicious and will be reported.
	 */
	@Override
	public void visit(Field aField) {
		super.visit(aField);
		int tTyp = 0;
		String tFieldSig = aField.getSignature();
		if (aField.getType() instanceof ObjectType) {
			ObjectType tType = (ObjectType)aField.getType();
			try {
				if (tType.subclassOf(calendarType)) {
					tTyp = 1;
				} else if (tType.subclassOf(dateFormatType)) {
					tTyp = 2;
				}
			} catch (ClassNotFoundException e) {
				; // ignore
			}
		} else {
			return;
		}
		boolean tIsStatic = aField.isStatic();
		if (tIsStatic) {
			String tType = null;
			switch (tTyp) {
			case 1:
				tType = "STCAL_STATIC_CALENDAR_INSTANCE";
				break;
			case 2:
				tType = "STCAL_STATIC_SIMPLE_DATA_FORMAT_INSTANCE";
				break;
			default:
				break;
			}
			if (tType != null) {
				reporter.reportBug(new BugInstance(this, tType, tTyp == 1 ? HIGH_PRIORITY : NORMAL_PRIORITY).addClass(currentClass).addField(
						currentClass, aField.getName(), tFieldSig, tIsStatic));
			}
		}
	}

	/**
	 * @see edu.umd.cs.findbugs.visitclass.DismantleBytecode#visit(org.apache.bcel.classfile.Code)
	 */
	@Override
	public void visit(Code obj) {
		synchronizationNestingLevel = 0;
		seenStaticGetCalendarAt = Integer.MIN_VALUE;
		seenStaticGetDateFormatAt = Integer.MIN_VALUE;
		registerStaticStoreCalendarAt.clear();
		registerStaticStoreDateFormatAt.clear();
		super.visit(obj);
	}
    int synchronizationNestingLevel = 0;
	/**
	 * @see edu.umd.cs.findbugs.visitclass.DismantleBytecode#sawOpcode(int)
	 */
	
	@Override
	public void sawOpcode(int seen) {
		/* check simple case first. must be called before the rest, because it will set some fields if appropriate */
		if (simpleCase(seen)) {
			return;
		}
		if (seen == MONITORENTER)
			synchronizationNestingLevel++;
		else if (seen == MONITOREXIT && synchronizationNestingLevel > 0)
			synchronizationNestingLevel--;
		// trickier case ----------------------->>
		/* store to a register */
		if (seen == ASTORE || seen == ASTORE_0 || seen == ASTORE_1 || seen == ASTORE_2 || seen == ASTORE_3) {
			int tRegister = getRegisterOperand();
			int tPC = getPC();
			if (tPC >= seenStaticGetCalendarAt + 3 && tPC < seenStaticGetCalendarAt + 4) {
				/* store to register is following the get of a static calendar, remember this for later */
				registerStaticStoreCalendarAt.put(tRegister, tPC);
				if (DEBUG) {
					System.out.println("STCAL: astore: reg " + tRegister + " at " + tPC);
				}
			} else if (tPC >= seenStaticGetDateFormatAt + 3 && tPC < seenStaticGetDateFormatAt + 4) {
				/* store to register is following the get of a static dateformat, remember this for later */
				registerStaticStoreDateFormatAt.put(tRegister, tPC);
				if (DEBUG) {
					System.out.println("STCAL: astore: reg " + tRegister + " at " + tPC);
				}
			} else {
				/* this register if used for something else. forget about it */
				registerStaticStoreCalendarAt.remove(tRegister);
				registerStaticStoreDateFormatAt.remove(tRegister);
				if (DEBUG) {
					System.out.println("STCAL: astore: clear reg " + tRegister + " at " + tPC);
				}
			}
		}

		/* load of a register value */
		if (seen == ALOAD || seen == ALOAD_0 || seen == ALOAD_1 || seen == ALOAD_2 || seen == ALOAD_3) {
			int tPC = getPC();
			int tRegister = getRegisterOperand();
			Integer tPCOfStore = registerStaticStoreCalendarAt.get(tRegister);
			/* if we find an entry in the map for this register, the last store to it has been a static calendar */
			if (tPCOfStore != null) {
				if (DEBUG) {
					System.out.println("STCAL: aload: reg " + tRegister + " at " + tPC);
				}
				reporter.reportBug(new BugInstance(this, "STCAL_INVOKE_ON_STATIC_CALENDAR_INSTANCE", NORMAL_PRIORITY)
						.addClassAndMethod(this).addSourceLine(this, tPCOfStore).addSourceLine(this, tPC));
				return;
			}

			tPCOfStore = registerStaticStoreDateFormatAt.get(tRegister);
			/* if we find an entry in the map for this register, the last store to it has been a static dateformat */
			if (tPCOfStore != null) {
				if (DEBUG) {
					System.out.println("STCAL: aload: reg " + tRegister + " at " + tPC);
				}
				reporter
						.reportBug(new BugInstance(this, "STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE", NORMAL_PRIORITY)
								.addClassAndMethod(this).addSourceLine(this, tPCOfStore).addSourceLine(this, tPC));
				return;
			}
			// <<--------------------------------------------
		}
	}

	/**
	 * Checks for a "simple match", e. g. an immediate succession of getstatic/invokevirtual on 
	 * a static {@link Calendar} or {@link DateFormat} field.
	 * @param seen Opcode
	 * @return <code>true</code> if we detected a simple case, i. e. the caller need not proceed with its anaysis.
	 * <code>false</code> if further analysis is recommended.
	 */
	private boolean simpleCase(int seen) {
		try {
		if (seen == GETSTATIC) {
			String tClassName = getSigConstantOperand();
			if (tClassName != null && tClassName.startsWith("L") && tClassName.endsWith(";")) {
				tClassName = tClassName.substring(1, tClassName.length() - 1);
				ObjectType tType = ObjectTypeFactory.getInstance(tClassName);

					if (tType.subclassOf(calendarType)) {
						seenStaticGetCalendarAt = getPC();
						return true;
					} else if (tType.subclassOf(dateFormatType)) {
						seenStaticGetDateFormatAt = getPC();
						return true;
					}
			
			}
			return false;
		}
		if (seen == INVOKEVIRTUAL) {
			ObjectType tType = ObjectTypeFactory.getInstance(getClassConstantOperand());
			if (!tType.subclassOf(calendarType) && !tType.subclassOf(dateFormatType)) return false;
			int numArguments = getNumberArguments(getSigConstantOperand());
			OpcodeStack.Item invokedOn = stack.getStackItem(numArguments);
			XField field = invokedOn.getXField();
			boolean isStatic = field != null && field.isStatic();
			String nameInvokedMethod = getNameConstantOperand();
			if (!isStatic && nameInvokedMethod.equals("equals") && numArguments == 1) {
				OpcodeStack.Item passedAsArgument = stack.getStackItem(0);
				field = passedAsArgument.getXField();
				isStatic = field != null && field.isStatic();
			}
			if (!isStatic) return false;
			if (getMethod().isSynchronized() || synchronizationNestingLevel > 0) return false;
			if (getMethodName().equals("<clinit>")) return false;
			int priority = LOW_PRIORITY;
			
			if (nameInvokedMethod.startsWith("set")) priority--;
			
			if (tType.subclassOf(calendarType)) {
				priority--;
				reporter.reportBug(new BugInstance(this, "STCAL_INVOKE_ON_STATIC_CALENDAR_INSTANCE", priority)
						.addClassAndMethod(this).addCalledMethod(this).addOptionalField(field).addSourceLine(this));
				return true;
			} else if (tType.subclassOf(dateFormatType)) {
				reporter.reportBug(new BugInstance(this, "STCAL_INVOKE_ON_STATIC_DATE_FORMAT_INSTANCE",  priority)
						.addClassAndMethod(this).addCalledMethod(this).addOptionalField(field).addSourceLine(this));
				return true;
			}
		}
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}
		return false;
	}

	/**
	 * Creates a new instance of this Detector.
	 * 
	 * @param aReporter
	 *            {@link BugReporter} instance to report found problems to.
	 */
	public StaticCalendarDetector(BugReporter aReporter) {
		reporter = aReporter;
	}

}
