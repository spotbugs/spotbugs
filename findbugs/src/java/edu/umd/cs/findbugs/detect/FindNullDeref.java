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

import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowValueChooser;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonCollector;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder;
import edu.umd.cs.findbugs.ba.npe.RedundantBranch;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefProperty;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
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
 * @see edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis
 */
public class FindNullDeref
		implements Detector, StatelessDetector, NullDerefAndRedundantComparisonCollector {

	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug");
	private static final boolean DEBUG_NULLARG = Boolean.getBoolean("fnd.debug.nullarg");
	private static final boolean REPORT_SAFE_METHOD_TARGETS = true;//Boolean.getBoolean("fnd.reportSafeTargets");

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

		if (DEBUG || DEBUG_NULLARG)
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
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		
		for (Iterator<Location> i = classContext.getCFG(method).locationIterator(); i.hasNext();) {
			Location location = i.next();
			try {
				examineLocation(location, cpg, typeDataflow, database);
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}
	}
	
	static class CallTarget {
		JavaClass javaClass;
		XMethod xmethod;
		CallTarget(JavaClass javaClass, XMethod xmethod) {
			this.javaClass = javaClass;
			this.xmethod = xmethod;
		}
	}
	
	private void examineLocation(Location location, ConstantPoolGen cpg, TypeDataflow typeDataflow, UnconditionalDerefPropertyDatabase database)
			throws DataflowAnalysisException, CFGBuilderException, ClassNotFoundException {
		if (!(location.getHandle().getInstruction() instanceof InvokeInstruction))
			return;
		
		InvokeInstruction invokeInstruction = (InvokeInstruction)
			location.getHandle().getInstruction();
		
		if (DEBUG_NULLARG) {
			System.out.println("Examining call site: " + location.getHandle());
		}
		
		String signature = invokeInstruction.getSignature(cpg); 
		int returnTypeStart = signature.indexOf(')');
		if (returnTypeStart < 0)
			return;
		String paramList = signature.substring(0, returnTypeStart + 1);
		
		if (paramList.equals("()") ||
				(paramList.indexOf("L") < 0 && paramList.indexOf('[') < 0))
			// Method takes no arguments, or takes no reference arguments
			return;

		// See if any null arguments are passed
		IsNullValueFrame frame =
			classContext.getIsNullValueDataflow(method).getFactAtLocation(location);
		if (!frame.isValid())
			return;
		BitSet nullArgSet = frame.getArgumentSet(invokeInstruction, cpg, new DataflowValueChooser<IsNullValue>() {
			public boolean choose(IsNullValue value) {
				// Only choose non-exception values.
				// Values null on an exception path might be due to
				// infeasible control flow.
				return value.mightBeNull() && !value.isException();
			}
		});
		BitSet definitelyNullArgSet = frame.getArgumentSet(invokeInstruction, cpg, new DataflowValueChooser<IsNullValue>() {
			public boolean choose(IsNullValue value) {
				return value.isDefinitelyNull();
			}
		});
		if (nullArgSet.isEmpty())
			return;
		if (DEBUG_NULLARG) {
			System.out.println("Null arguments passed: " + nullArgSet);
		}
		
		// See what methods might be called here
		TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
		Set<XMethod> targetMethodSet = Hierarchy.resolveMethodCallTargets(invokeInstruction, typeFrame, cpg);
		if (DEBUG_NULLARG) {
			System.out.println("Possibly called methods: " + targetMethodSet);
		}
		
		// See if any call targets unconditionally dereference one of the null arguments
		BitSet unconditionallyDereferencedNullArgSet = new BitSet();
		List<CallTarget> dangerousCallTargetList = new LinkedList<CallTarget>();
		for (XMethod targetMethod : targetMethodSet) {
			if (DEBUG_NULLARG) {
				System.out.println("For target method " + targetMethod);
			}
			
			UnconditionalDerefProperty property = database.getProperty(targetMethod);
			if (property == null)
				continue;
			if (DEBUG_NULLARG) {
				System.out.println("\tUnconditionally dereferenced params: " + property);
			}
			
			BitSet targetUnconditionallyDereferencedNullArgSet =
				property.getUnconditionallyDereferencedNullArgSet(nullArgSet);
			
			if (targetUnconditionallyDereferencedNullArgSet.isEmpty())
				continue;
			
			JavaClass targetClass = AnalysisContext.currentAnalysisContext().lookupClass(targetMethod.getClassName());
			dangerousCallTargetList.add(new CallTarget(targetClass, targetMethod));
			
			unconditionallyDereferencedNullArgSet.or(targetUnconditionallyDereferencedNullArgSet);
		}
		
		if (dangerousCallTargetList.isEmpty())
			return;
		
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		BugInstance warning = new BugInstance("NP_NULL_PARAM_DEREF", NORMAL_PRIORITY)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, location.getHandle());
		for (int i = 0; i < 32; ++i) {
			if (unconditionallyDereferencedNullArgSet.get(i)) {
				// Note: we report params as being indexed starting from 1, not 0
				warning.addInt(i + 1).describe(
						definitelyNullArgSet.get(i) ? "INT_NULL_ARG" : "INT_MAYBE_NULL_ARG");
			}
		}
		for (CallTarget dangerousCallTarget : dangerousCallTargetList) {
			JavaClass targetClass = dangerousCallTarget.javaClass;
			XMethod targetMethod = dangerousCallTarget.xmethod;
			addMethodAnnotationForCalledMethod(warning, targetClass, targetMethod, "METHOD_DANGEROUS_TARGET");
		}
		if (REPORT_SAFE_METHOD_TARGETS) {
			// This is useful to see which other call targets the analysis
			// considered.
			Set<XMethod> safeCallTargetSet = new HashSet<XMethod>();
			safeCallTargetSet.addAll(targetMethodSet);
			for (CallTarget dangerousCallTarget : dangerousCallTargetList) {
				safeCallTargetSet.remove(dangerousCallTarget.xmethod);
			}
			for (XMethod safeMethod : safeCallTargetSet) {
				JavaClass targetClass = AnalysisContext.currentAnalysisContext().lookupClass(safeMethod.getClassName());
				addMethodAnnotationForCalledMethod(warning, targetClass, safeMethod, "METHOD_SAFE_TARGET");
			}
		}
		
		bugReporter.reportBug(warning);
	}

	private void addMethodAnnotationForCalledMethod(BugInstance warning, JavaClass targetClass, XMethod targetMethod,
			String description) {
		MethodAnnotation calledMethod = MethodAnnotation.fromXMethod(targetMethod);
		SourceLineAnnotation methodSourceLines = SourceLineAnnotation.forEntireMethod(
				targetClass,
				targetMethod);
		calledMethod.setSourceLines(methodSourceLines);
		warning.addMethod(calledMethod).describe(description);
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
