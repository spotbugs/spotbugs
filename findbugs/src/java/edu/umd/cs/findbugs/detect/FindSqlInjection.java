/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.Iterator;
import java.lang.IllegalArgumentException;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.constant.Constant;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.ba.constant.ConstantFrame;

/**
 * Find potential SQL injection vulnerabilities.
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 * @author Matt Hargett
 */
public class FindSqlInjection implements Detector {
	private static class StringAppendState {
		boolean sawOpenQuote = false;
		boolean sawCloseQuote = false;
		boolean sawComma = false;
		boolean sawAppend = false;
		boolean sawUnsafeAppend = false;

		public boolean getSawOpenQuote() { return sawOpenQuote; }
		public boolean getSawCloseQuote() { return sawCloseQuote; }
		public boolean getSawComma() { return sawComma; }
		public boolean getSawAppend() { return sawAppend; }
		public boolean getSawUnsafeAppend() { return sawUnsafeAppend; }

		public void setSawOpenQuote (boolean saw) { 
			sawOpenQuote = saw; 
		}

		public void setSawCloseQuote (boolean saw) {
			sawCloseQuote = saw; 
		}
		
		public void setSawComma (boolean saw) { 
			sawComma = saw; 
		}
		
		public void setSawAppend (boolean saw) { 
			sawAppend = saw; 
		}

		public void setSawUnsafeAppend (boolean saw) { 
			sawUnsafeAppend = saw; 
		}
	}

	BugReporter bugReporter;
	
	public FindSqlInjection(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		for (Method method : methodList) {
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError(
					"FindSqlInjection caught exception while analyzing " + 
					methodGen, e);
			} catch (CFGBuilderException e) {
				bugReporter.logError(
					"FindSqlInjection caught exception while analyzing " + 
					methodGen, e);
			} catch (RuntimeException e) {
				System.out.println(
					"Exception while checking for SQL injection in " 
					+ methodGen + " in " + javaClass.getSourceFileName());
					e.printStackTrace(System.out);
			}
		}
	}

	private boolean isStringAppend(Instruction ins, ConstantPoolGen cpg) {
		if (ins instanceof INVOKEVIRTUAL) {
			INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) ins;

			if (invoke.getMethodName(cpg).equals("append")
				&& invoke.getClassName(cpg).startsWith("java.lang.StringB")) {

				return true;
			}
		}

		return false;
	}

	private boolean isConstantStringLoad(Instruction ins, ConstantPoolGen cpg) {
		if (ins instanceof LDC) {
			LDC load = (LDC) ins;
			Object value = load.getValue(cpg);
			if (value instanceof String) {
				return true;
			}
		}

		return false;
	}

	private StringAppendState updateStringAppendState(Instruction ins, ConstantPoolGen cpg, StringAppendState stringAppendState) {
		if (!isConstantStringLoad(ins, cpg)) {
			throw new IllegalArgumentException("instruction must be LDC");
		}

		LDC load = (LDC) ins;
		Object value = load.getValue(cpg);
		String stringValue = ((String)value).trim();
		if (stringValue.startsWith(",") || stringValue.endsWith(","))
			stringAppendState.setSawComma(true);
		if (stringValue.endsWith("'"))
			stringAppendState.setSawOpenQuote(true);
		if (stringValue.startsWith("'"))
			stringAppendState.setSawCloseQuote(true);

		return stringAppendState;
	}

	private boolean isPreparedStatementDatabaseSink(Instruction ins, ConstantPoolGen cpg) {
		if (!(ins instanceof INVOKEINTERFACE)) {
			return false;
		}

		INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;

		String methodName = invoke.getMethodName(cpg);
		String interfaceName = invoke.getClassName(cpg);
		if (methodName.equals("prepareStatement") && 
			interfaceName.equals("java.sql.Connection")) {
			return true;
		}

		return false;
	}

	private boolean isExecuteDatabaseSink(Instruction ins, ConstantPoolGen cpg) {
		if (!(ins instanceof INVOKEINTERFACE)) {
			return false;
		}

		INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;

		String methodName = invoke.getMethodName(cpg);
		String interfaceName = invoke.getClassName(cpg);
		if (methodName.startsWith("execute") && 
			interfaceName.equals("java.sql.Statement")) {
			return true;
		}

		return false;
	}

	private boolean isDatabaseSink(Instruction ins, ConstantPoolGen cpg) {
		return isPreparedStatementDatabaseSink(ins, cpg) ||
			isExecuteDatabaseSink(ins, cpg);
	}

	private StringAppendState getStringAppendState(CFG cfg, ConstantPoolGen cpg) {
		StringAppendState stringAppendState = new StringAppendState();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			Instruction ins = location.getHandle().getInstruction();
			if (isConstantStringLoad(ins, cpg)) {
				stringAppendState = updateStringAppendState(ins, cpg, stringAppendState);
			} else if (isStringAppend(ins, cpg)) { 
				stringAppendState.setSawAppend(true);

				InstructionHandle prev = getPreviousInstruction(location.getHandle(), true);
				if (prev != null) {
					Instruction prevIns = prev.getInstruction();
					if (!(prevIns instanceof LDC || prevIns instanceof GETSTATIC))
						stringAppendState.setSawUnsafeAppend(true);
				} else {
					// FIXME: when would prev legitimately be null, and why would we report?
                    AnalysisContext.logError("In FindSqlInjection, saw null previous in " + cfg.getMethodGen().getClassName() +"." + cfg.getMethodName());
					stringAppendState.setSawUnsafeAppend(true);
				}
			}
		}

		return stringAppendState;
	}

	private InstructionHandle getPreviousInstruction(InstructionHandle startHandle, boolean skipNops) {
		InstructionHandle previousHandle = startHandle.getPrev();
		while (previousHandle != null) {
			Instruction prevIns = previousHandle.getInstruction();
			if (!skipNops && !(prevIns instanceof NOP)) {
				return previousHandle;
			}

			previousHandle = previousHandle.getPrev();
		}

		return null;
	}

	private BugInstance generateBugInstance(JavaClass javaClass, MethodGen methodGen, Instruction ins, StringAppendState stringAppendState) {
		ConstantPoolGen cpg = methodGen.getConstantPool();
		int priority = LOW_PRIORITY;
		if (stringAppendState.getSawAppend()) {  
			if (stringAppendState.getSawOpenQuote() && 
				stringAppendState.getSawCloseQuote()) {
				priority = HIGH_PRIORITY;
			} else if (stringAppendState.getSawComma()) {
				priority = NORMAL_PRIORITY;
			}

			if (!stringAppendState.getSawUnsafeAppend()) {
				priority += 2;
			}
		}

		String description = "";
		if (isExecuteDatabaseSink(ins, cpg)) {
			description = "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE";  
		} else if (isPreparedStatementDatabaseSink(ins, cpg)) {
			description = "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING";
		}
		
		BugInstance bug = new BugInstance(this, description, priority);
		bug.addClassAndMethod(methodGen, javaClass.getSourceFileName());

		return bug;
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws DataflowAnalysisException, CFGBuilderException {
		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) 
			return;

		ConstantPoolGen cpg = methodGen.getConstantPool();
		CFG cfg = classContext.getCFG(method);

		StringAppendState stringAppendState = getStringAppendState(cfg, cpg);
				
	        ConstantDataflow dataflow = classContext.getConstantDataflow(method);
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			Instruction ins = location.getHandle().getInstruction();
			if (isDatabaseSink(ins, cpg)) {
				ConstantFrame frame = dataflow.getFactAtLocation(location);
				Constant value = frame.getStackValue(0);
			    
				if (!value.isConstantString()) {
					// TODO: verify it's the same string represented by stringAppendState
					// FIXME: will false positive on const/static strings returns by methods 
					BugInstance bug = generateBugInstance(javaClass, methodGen, ins, stringAppendState);
					bug.addSourceLine(classContext, methodGen, javaClass.getSourceFileName(), location.getHandle());
					bugReporter.reportBug(bug);
				}
			}
		}
	}
	
	
	public void report() {
	}
}

//vim:ts=4
