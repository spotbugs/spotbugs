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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
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
 */
public class FindSqlInjection implements Detector {

	BugReporter bugReporter;
	
	public FindSqlInjection(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	private boolean prescreen(ClassContext classContext, Method method) {
		return true;
	}
	
	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();

		for (Method method : methodList) {
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			if (!prescreen(classContext, method))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("FindSqlInjection caught exception while analyzing " + methodGen, e);
			} catch (CFGBuilderException e) {
				bugReporter.logError("FindSqlInjection caught exception while analyzing " + methodGen, e);
			}
		}
	}
	
	private void analyzeMethod(ClassContext classContext, Method method)
			throws DataflowAnalysisException, CFGBuilderException {
		
		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		ConstantPoolGen cpg = methodGen.getConstantPool();
		try {
		CFG cfg = classContext.getCFG(method);
	        ConstantDataflow dataflow 
			= classContext.getConstantDataflow(method);
	    boolean sawOpenQuote = false;
	    boolean sawCloseQuote = false;
	    boolean sawComma = false;
	    boolean sawAppend = false;
	    boolean sawUnsafeAppend = false;
	    for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
				Location location = i.next();
				Instruction ins = location.getHandle().getInstruction();
				if (ins instanceof LDC) {
					LDC load = (LDC) ins;
					Object value = load.getValue(cpg);
					if (value instanceof String) {
						String stringValue = ((String)value).trim();
						if (stringValue.startsWith(",") || stringValue.endsWith(","))
							sawComma = true;
						if (stringValue.endsWith("'"))
							sawOpenQuote = true;
						if (stringValue.startsWith("'"))
							sawCloseQuote = true;
					}
				} else if (ins instanceof INVOKEVIRTUAL) {
					INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) ins;

					if (invoke.getMethodName(cpg).equals("append")
						&& invoke.getClassName(cpg).startsWith("java.lang.StringB")) {
						sawAppend = true;
						InstructionHandle prev = location.getHandle().getPrev();
						if (prev != null) {
							Instruction prevIns = prev.getInstruction();
							if (!(prevIns instanceof LDC || prevIns instanceof GETSTATIC))
								sawUnsafeAppend = true;
						}
						else sawUnsafeAppend = true;
					}
				}
	    }
				
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			
			Instruction ins = location.getHandle().getInstruction();
			if (!(ins instanceof INVOKEINTERFACE)) continue;
			INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;

			String methodName = invoke.getMethodName(cpg);
			String interfaceName = invoke.getClassName(cpg);
			if (methodName.equals("prepareStatement") && interfaceName.equals("java.sql.Connection")
					||  methodName.startsWith("execute") && interfaceName.equals("java.sql.Statement")) {
			ConstantFrame frame = dataflow.getFactAtLocation(location);
		        Constant value = frame.getStackValue(0);
		    
			if (!value.isConstantString()) {
				int priority = LOW_PRIORITY;
				if (sawAppend && sawOpenQuote && sawCloseQuote) priority = HIGH_PRIORITY;
				else if (sawAppend && sawComma) priority = NORMAL_PRIORITY;
				if (!sawUnsafeAppend) priority+=2;
			    bugReporter.reportBug(
				new BugInstance(this, 
						methodName.equals("prepareStatement")
						? "SQL_PREPARED_STATEMENT_GENERATED_FROM_NONCONSTANT_STRING" 
								: "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",  
				 priority) 
				.addClassAndMethod(methodGen, javaClass.getSourceFileName())
				.addSourceLine(classContext, methodGen, javaClass.getSourceFileName(), location.getHandle()));
			}
			}


			}
	} catch (RuntimeException e) {
		System.out.println("Exception while checking for SQL injection in " 
				+ methodGen + " in " + javaClass.getSourceFileName());
		e.printStackTrace(System.out);
		}
			
	}
	
	
	public void report() {
	}
}

//vim:ts=4
