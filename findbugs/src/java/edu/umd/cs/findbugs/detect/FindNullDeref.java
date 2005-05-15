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

import java.util.Iterator;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonCollector;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder;
import edu.umd.cs.findbugs.ba.npe.RedundantBranch;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefProperty;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefPropertyDatabase;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.props.GeneralWarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.props.WarningPropertyUtil;

/**
 * A Detector to find instructions where a NullPointerException
 * might be raised.  We also look for useless reference comparisons
 * involving null and non-null values.
 *
 * @author David Hovemeyer
 * @see IsNullValueAnalysis
 */
public class FindNullDeref
		implements Detector, StatelessDetector, NullDerefAndRedundantComparisonCollector {

	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug");

	private BugReporter bugReporter;
	
	// Transient state
	private ClassContext classContext;
	private Method method;

	public FindNullDeref(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		
		try {
			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative() || method.getCode() == null)
					continue;

				analyzeMethod(classContext, method);
			}
		} catch (DataflowAnalysisException e) {
			bugReporter.logError("FindNullDeref caught exception", e);
		} catch (CFGBuilderException e) {
			bugReporter.logError("FindNullDeref caught exception", e);
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {
		
		this.method = method;

		if (DEBUG)
			System.out.println(SignatureConverter.convertMethodSignature(classContext.getMethodGen(method)));

		// Get the IsNullValueAnalysis for the method from the ClassContext
		IsNullValueDataflow invDataflow = classContext.getIsNullValueDataflow(method);
		
		// Create a NullDerefAndRedundantComparisonFinder object to do the actual
		// work.  It will call back to report null derefs and redundant null comparisons
		// through the NullDerefAndRedundantComparisonCollector interface we implement.
		NullDerefAndRedundantComparisonFinder worker = new NullDerefAndRedundantComparisonFinder(
				classContext,
				method,
				invDataflow,
				this);
		worker.execute();
		
		if (AnalysisContext.USE_INTERPROC_DATABASE) {
			UnconditionalDerefPropertyDatabase database =
				AnalysisContext.currentAnalysisContext().getUnconditionalDerefDatabase();
			if (database != null) {
				examineCalledMethods(database);
			}
		}
	}

	private void examineCalledMethods(UnconditionalDerefPropertyDatabase database)
			throws CFGBuilderException, DataflowAnalysisException {
		for (Iterator<Location> i = classContext.getCFG(method).locationIterator(); i.hasNext();) {
			Location location = i.next();
			if (!(location.getHandle().getInstruction() instanceof InvokeInstruction))
				continue;
			XMethod calledMethod = XMethodFactory.createXMethod(
					(InvokeInstruction) location.getHandle().getInstruction(),
					classContext.getConstantPoolGen()
					);
			UnconditionalDerefProperty property = database.getProperty(calledMethod);
			if (property == null || property.isEmpty())
				continue;
			
			IsNullValueFrame frame =
				classContext.getIsNullValueDataflow(method).getFactAtLocation(location);
			if (!frame.isValid())
				continue;
			
			int numParams = calledMethod.getNumParams();
			int shift = calledMethod.isStatic() ? 0 : 1;
			
			for (int index = 0; index < numParams; ++index) {
				if (index >= frame.getStackDepth()) {
					break;
				}

				if (!property.paramUnconditionalDeref(index + shift))
					continue;
				
				IsNullValue arg = frame.getStackValue((numParams - index) - 1);
				if (arg.mightBeNull()) {
					MethodGen methodGen = classContext.getMethodGen(method);
					String sourceFile = classContext.getJavaClass().getSourceFileName();
					bugReporter.reportBug(new BugInstance("NP_NULL_PARAM_DEREF", NORMAL_PRIORITY)
							.addClassAndMethod(methodGen, sourceFile)
							.addMethod(calledMethod).describe("METHOD_CALLED")
							.addSourceLine(methodGen, sourceFile, location.getHandle())
					);
					// XXX: should also indicate which parameter it was
					break;
				}
			}
		}
	}

	public void report() {
	}

	public void foundNullDeref(Location location, ValueNumber valueNumber, IsNullValue refValue) {
		WarningPropertySet propertySet = new WarningPropertySet();
		
		boolean onExceptionPath = refValue.isException();
		if (onExceptionPath) {
			propertySet.addProperty(GeneralWarningProperty.ON_EXCEPTION_PATH);
		}
		
		if (refValue.isDefinitelyNull()) {
			String type = onExceptionPath ? "NP_ALWAYS_NULL_EXCEPTION" : "NP_ALWAYS_NULL";
			int priority = onExceptionPath ? LOW_PRIORITY : HIGH_PRIORITY;
			reportNullDeref(propertySet, classContext, method, location, type, priority);
		} else if (refValue.isNullOnSomePath()) {
			String type = onExceptionPath ? "NP_NULL_ON_SOME_PATH_EXCEPTION" : "NP_NULL_ON_SOME_PATH";
			int priority = onExceptionPath ? LOW_PRIORITY : NORMAL_PRIORITY;
			if (DEBUG) System.out.println("Reporting null on some path: value=" + refValue);
			reportNullDeref(propertySet, classContext, method, location, type, priority);
		}
		
	}

	private void reportNullDeref(
			WarningPropertySet propertySet,
			ClassContext classContext,
			Method method,
			Location location,
			String type,
			int priority) {
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		BugInstance bugInstance = new BugInstance(this, type, priority)
		        .addClassAndMethod(methodGen, sourceFile)
		        .addSourceLine(methodGen, sourceFile, location.getHandle());

		if (DEBUG)
			bugInstance.addInt(location.getHandle().getPosition()).describe("INT_BYTECODE_OFFSET");

		if (AnalysisContext.currentAnalysisContext().getBoolProperty(
				FindBugsAnalysisProperties.RELAXED_REPORTING_MODE)) {
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			propertySet.decorateBugInstance(bugInstance);
		}
		
		bugReporter.reportBug(bugInstance);
	}

	public void foundRedundantNullCheck(Location location, RedundantBranch redundantBranch) {
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		MethodGen methodGen = classContext.getMethodGen(method);

		boolean redundantNullCheck = redundantBranch.checkedValue;
		int priority = redundantNullCheck ? LOW_PRIORITY : NORMAL_PRIORITY;
		
		BugInstance bugInstance =
			new BugInstance(this, "RCN_REDUNDANT_COMPARISON_TO_NULL", priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, location.getHandle());
		
		if (AnalysisContext.currentAnalysisContext().getBoolProperty(
				FindBugsAnalysisProperties.RELAXED_REPORTING_MODE)) {
			WarningPropertySet propertySet = new WarningPropertySet();
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			if (redundantBranch.checkedValue) {
				propertySet.addProperty(NullDerefProperty.CHECKED_VALUE);
			}
			
			propertySet.decorateBugInstance(bugInstance);
			
			priority = propertySet.computePriority(NORMAL_PRIORITY);
			bugInstance.setPriority(priority);
		}

		bugReporter.reportBug(bugInstance);
	}

}

// vim:ts=4
