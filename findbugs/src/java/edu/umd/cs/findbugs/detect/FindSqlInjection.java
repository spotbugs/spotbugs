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

import edu.umd.cs.findbugs.ba.constant.Constant;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.constant.*;
import edu.umd.cs.findbugs.props.WarningPropertySet;

/**
 * Find dead stores to local variables.
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class FindSqlInjection implements Detector {

	BugReporter bugReporter;
	
	public FindSqlInjection(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void setAnalysisContext(AnalysisContext analysisContext) {
	}
	
	private boolean prescreen(ClassContext classContext, Method method) {
		return true;
	}
	
	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		Method[] methodList = javaClass.getMethods();
		
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;
			
			if (!prescreen(classContext, method))
				continue;
			
			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				throw new AnalysisException("FindDeadLocalStores caught exception", methodGen, e);
			} catch (CFGBuilderException e) {
				throw new AnalysisException("FindDeadLocalStores caught exception", methodGen, e);
			}
		}
	}
	
	private void analyzeMethod(ClassContext classContext, Method method)
			throws DataflowAnalysisException, CFGBuilderException {
		
		JavaClass javaClass = classContext.getJavaClass();
		MethodGen methodGen = classContext.getMethodGen(method);
		ConstantPoolGen cpg = methodGen.getConstantPool();
		try {
		CFG cfg = classContext.getCFG(method);

	        ConstantDataflow dataflow 
			= classContext.getConstantDataflow(method);
		
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			
			Instruction ins = location.getHandle().getInstruction();
			if (!(ins instanceof INVOKEINTERFACE)) continue;
			INVOKEINTERFACE invoke = (INVOKEINTERFACE) ins;

			String methodName = invoke.getMethodName(cpg);
			if (!methodName.startsWith("execute")) continue;
			String interfaceName = invoke.getClassName(cpg);
			if (!interfaceName.equals("java.sql.Statement")) continue;
			ConstantFrame frame = dataflow.getFactAtLocation(location);
		        Constant value = frame.getStackValue(0);
			if (!value.isConstantString())

			    bugReporter.reportBug(
				new BugInstance(this, 
				 "SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE",  
				 NORMAL_PRIORITY) 
				.addClassAndMethod(methodGen, javaClass.getSourceFileName())
				.addSourceLine(methodGen, javaClass.getSourceFileName(), location.getHandle()));



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
