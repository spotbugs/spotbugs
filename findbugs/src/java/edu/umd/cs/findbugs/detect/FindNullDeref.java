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

import edu.umd.cs.findbugs.AnalysisLocal;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowValueChooser;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.JavaClassAndMethodChooser;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodFactory;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.NonNullParamProperty;
import edu.umd.cs.findbugs.ba.npe.NonNullParamPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonCollector;
import edu.umd.cs.findbugs.ba.npe.NullDerefAndRedundantComparisonFinder;
import edu.umd.cs.findbugs.ba.npe.RedundantBranch;
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
		implements Detector, NullDerefAndRedundantComparisonCollector {

	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug");
	private static final boolean DEBUG_NULLARG = Boolean.getBoolean("fnd.debug.nullarg");
	private static final boolean REPORT_SAFE_METHOD_TARGETS = true;

	private static final String METHOD = System.getProperty("fnd.method");

	public static final String UNCONDITIONAL_DEREF_DB_FILENAME = "unconditionalDeref.db";
	
	// Method property databases
	static AnalysisLocal<NonNullParamPropertyDatabase> unconditionalDerefDatabase =
		new AnalysisLocal<NonNullParamPropertyDatabase>();
	static AnalysisLocal<NonNullParamPropertyDatabase> nonNullParamDatabase =
		new AnalysisLocal<NonNullParamPropertyDatabase>();
	static AnalysisLocal<NonNullParamPropertyDatabase> possiblyNullParamDatabase =
		new AnalysisLocal<NonNullParamPropertyDatabase>();
	
	// Fields
	private BugReporter bugReporter;
	
	// Transient state
	private ClassContext classContext;
	private Method method;
	private boolean checkDatabase;

	public FindNullDeref(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visitClassContext(ClassContext classContext) {
		if (!checkDatabase) {
			if (AnalysisContext.currentAnalysisContext().getDatabaseInputDir() != null) {
				unconditionalDerefDatabase.set(AnalysisContext.currentAnalysisContext().loadPropertyDatabase(
						new NonNullParamPropertyDatabase(),
						UNCONDITIONAL_DEREF_DB_FILENAME,
						"unconditional param deref database"));
			}
			checkDatabase = true;
		}
		
		this.classContext = classContext;
		
		try {
			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative() || method.getCode() == null)
					continue;
				
				if (METHOD != null && !method.getName().equals(METHOD))
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

		if (unconditionalDerefDatabase.get() != null || nonNullParamDatabase.get() != null) {
			examineCalledMethods();
		}
	}

	private void examineCalledMethods()
			throws CFGBuilderException, DataflowAnalysisException {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);
		
		for (Iterator<Location> i = classContext.getCFG(method).locationIterator(); i.hasNext();) {
			Location location = i.next();
			try {
				examineLocation(location, cpg, typeDataflow);
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}
	}
	
	private void examineLocation(
			Location location,
			ConstantPoolGen cpg,
			TypeDataflow typeDataflow)
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
		
		if (unconditionalDerefDatabase.get() != null) {
			checkUnconditionallyDereferencedParam(location, cpg, typeDataflow, invokeInstruction, nullArgSet, definitelyNullArgSet);
		}
		
		if (nonNullParamDatabase.get() != null && possiblyNullParamDatabase.get() != null) {
			if (DEBUG_NULLARG) {
				System.out.println("Checking nonnull params");
			}
			checkNonNullParam(location, cpg, typeDataflow, invokeInstruction, nullArgSet, definitelyNullArgSet);
		}
	}

	private void checkUnconditionallyDereferencedParam(
			Location location,
			ConstantPoolGen cpg,
			TypeDataflow typeDataflow,
			InvokeInstruction invokeInstruction,
			BitSet nullArgSet, BitSet definitelyNullArgSet) throws DataflowAnalysisException, ClassNotFoundException {
		
		NonNullParamPropertyDatabase database = unconditionalDerefDatabase.get();
		
		// See what methods might be called here
		TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
		Set<JavaClassAndMethod> targetMethodSet = Hierarchy.resolveMethodCallTargets(invokeInstruction, typeFrame, cpg);
		if (DEBUG_NULLARG) {
			System.out.println("Possibly called methods: " + targetMethodSet);
		}
		
		// See if any call targets unconditionally dereference one of the null arguments
		BitSet unconditionallyDereferencedNullArgSet = new BitSet();
		List<JavaClassAndMethod> dangerousCallTargetList = new LinkedList<JavaClassAndMethod>();
		for (JavaClassAndMethod targetMethod : targetMethodSet) {
			if (DEBUG_NULLARG) {
				System.out.println("For target method " + targetMethod);
			}
			
			NonNullParamProperty property = database.getProperty(targetMethod.toXMethod());
			if (property == null)
				continue;
			if (DEBUG_NULLARG) {
				System.out.println("\tUnconditionally dereferenced params: " + property);
			}
			
			BitSet targetUnconditionallyDereferencedNullArgSet =
				property.getViolatedParamSet(nullArgSet);
			
			if (targetUnconditionallyDereferencedNullArgSet.isEmpty())
				continue;
			
			dangerousCallTargetList.add(targetMethod);
			
			unconditionallyDereferencedNullArgSet.or(targetUnconditionallyDereferencedNullArgSet);
		}
		
		if (dangerousCallTargetList.isEmpty())
			return;
		
		WarningPropertySet propertySet = new WarningPropertySet();
		
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		BugInstance warning = new BugInstance("NP_NULL_PARAM_DEREF", NORMAL_PRIORITY)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, location.getHandle());
		
		// Check which params might be null
		addParamAnnotations(definitelyNullArgSet, unconditionallyDereferencedNullArgSet, propertySet, warning);

		// Add annotations for dangerous method call targets
		for (JavaClassAndMethod dangerousCallTarget : dangerousCallTargetList) {
			warning.addMethod(dangerousCallTarget.getJavaClass(), dangerousCallTarget.getMethod()).describe("METHOD_DANGEROUS_TARGET");
		}

		// See if there are any safe targets
		Set<JavaClassAndMethod> safeCallTargetSet = new HashSet<JavaClassAndMethod>();
		safeCallTargetSet.addAll(targetMethodSet);
		safeCallTargetSet.removeAll(dangerousCallTargetList);
		if (safeCallTargetSet.isEmpty()) {
			propertySet.addProperty(NullArgumentWarningProperty.ALL_DANGEROUS_TARGETS);
			if (dangerousCallTargetList.size() == 1) {
				propertySet.addProperty(NullArgumentWarningProperty.MONOMORPHIC_CALL_SITE);
			}
		}
		if (REPORT_SAFE_METHOD_TARGETS) {
			// This is useful to see which other call targets the analysis
			// considered.
			for (JavaClassAndMethod safeMethod : safeCallTargetSet) {
				warning.addMethod(safeMethod.getJavaClass(), safeMethod.getMethod()).describe("METHOD_SAFE_TARGET");
			}
		}

		finishWarning(location, propertySet, warning);
		
		bugReporter.reportBug(warning);
	}

	private void finishWarning(Location location, WarningPropertySet propertySet, BugInstance warning) {
		warning.setPriority(propertySet.computePriority(NORMAL_PRIORITY));
		if (FindBugsAnalysisProperties.isRelaxedMode()) {
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			propertySet.decorateBugInstance(warning);
		}
	}

	private void addParamAnnotations(
			BitSet definitelyNullArgSet,
			BitSet violatedParamSet,
			WarningPropertySet propertySet,
			BugInstance warning) {
		for (int i = 0; i < 32; ++i) {
			if (violatedParamSet.get(i)) {
				boolean definitelyNull = definitelyNullArgSet.get(i);
				
				if (definitelyNull) {
					propertySet.addProperty(NullArgumentWarningProperty.ARG_DEFINITELY_NULL);
				}

				// Note: we report params as being indexed starting from 1, not 0
				warning.addInt(i + 1).describe(
						definitelyNull ? "INT_NULL_ARG" : "INT_MAYBE_NULL_ARG");
			}
		}
	}
	
	static class NonNullSpecification {
		//final XMethod xmethod;
		final JavaClassAndMethod classAndMethod;
		final NonNullParamProperty nonNullProperty;
		final NonNullParamProperty possiblyNullProperty;
		NonNullSpecification(JavaClassAndMethod classAndMethod, NonNullParamProperty nonParamProperty, NonNullParamProperty possiblyNullProperty) {
			this.classAndMethod = classAndMethod;
			this.nonNullProperty = nonParamProperty;
			this.possiblyNullProperty = possiblyNullProperty;
		}
	}

	static NonNullParamProperty wrapProperty(NonNullParamProperty property) {
		return property != null ? property : new NonNullParamProperty();
	}
	
	static class NonNullParamViolation {
		JavaClassAndMethod classAndMethod;
		int param;
		NonNullParamViolation(JavaClassAndMethod classAndMethod, int param) {
			this.classAndMethod = classAndMethod;
			this.param = param;
		}
	}
	
	private void checkNonNullParam(
			Location location, 
			ConstantPoolGen cpg,
			TypeDataflow typeDataflow,
			InvokeInstruction invokeInstruction,
			BitSet nullArgSet,
			BitSet definitelyNullArgSet) throws ClassNotFoundException {

		// Go up the class hierarchy finding @NonNull and @PossiblyNull annotations.
		final List<NonNullSpecification> specificationList = new LinkedList<NonNullSpecification>();
		JavaClassAndMethodChooser nonNullContractCollector = new JavaClassAndMethodChooser() {
			public boolean choose(JavaClassAndMethod classAndMethod) {
				XMethod xmethod = XMethodFactory.createXMethod(
						classAndMethod.getJavaClass(), classAndMethod.getMethod());

				NonNullSpecification specification = new NonNullSpecification(
						classAndMethod,
						wrapProperty(nonNullParamDatabase.get().getProperty(xmethod)),
						wrapProperty(possiblyNullParamDatabase.get().getProperty(xmethod)));
				specificationList.add(specification);
				
				return false;
			}
		};

		// See if any null arguments violate a @NonNull annotation.
		int numParams = new SignatureParser(method.getSignature()).getNumParameters();
		Hierarchy.findInvocationLeastUpperBound(invokeInstruction, cpg, nonNullContractCollector);
		BitSet checkedParams = new BitSet();
		BitSet violatedParamSet = new BitSet();
		List<NonNullParamViolation> violationList = new LinkedList<NonNullParamViolation>();
		for (NonNullSpecification specification : specificationList) {
			for (int i = 0; i < numParams; ++i) {
				if (!nullArgSet.get(i) || checkedParams.get(i))
					continue;
				
				// Arg is null, and we haven't seen a specification for the parameter yet.
				// See if this method defines a specification.
				if (specification.possiblyNullProperty.isNonNull(i)) {
					// Parameter declared @PossiblyNull.
					// So it's OK to pass null.
					checkedParams.set(i);
				} else if (specification.nonNullProperty.isNonNull(i)) {
					// Parameter declated @NonNull.
					// This is a violation.
					violationList.add(new NonNullParamViolation(specification.classAndMethod, i));
					violatedParamSet.set(i);
					checkedParams.set(i);
				}
			}
		}
		if (violationList.isEmpty())
			return;
		
		XMethod xmethod = XMethodFactory.createXMethod(invokeInstruction, cpg);
		
		WarningPropertySet propertySet = new WarningPropertySet();

		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		BugInstance warning = new BugInstance("NP_NONNULL_PARAM_VIOLATION", NORMAL_PRIORITY)
			.addClassAndMethod(methodGen, sourceFile)
			.addSourceLine(methodGen, sourceFile, location.getHandle())
			.addMethod(xmethod).describe("METHOD_CALLED");
		
		addParamAnnotations(definitelyNullArgSet, violatedParamSet, propertySet, warning);
		
		for (NonNullParamViolation violation : violationList) {
			warning.addMethod(violation.classAndMethod.getJavaClass(), violation.classAndMethod.getMethod());
			warning.addInt(violation.param).describe("INT_NONNULL_PARAM");
		}

		finishWarning(location, propertySet, warning);
		
		bugReporter.reportBug(warning);
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

		if (FindBugsAnalysisProperties.isRelaxedMode()) {
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			propertySet.decorateBugInstance(bugInstance);
		}
		
		bugReporter.reportBug(bugInstance);
	}

	public void foundRedundantNullCheck(Location location, RedundantBranch redundantBranch) {
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		MethodGen methodGen = classContext.getMethodGen(method);
		
		boolean isChecked = redundantBranch.firstValue.isChecked();
		boolean wouldHaveBeenAKaboom = redundantBranch.firstValue.wouldHaveBeenAKaboom();
		
		
		int priority = LOW_PRIORITY;
		String warning;
		if (redundantBranch.secondValue == null) {
			if (redundantBranch.firstValue.isDefinitelyNull() ) {
				warning = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE";
			}
			else {
				warning = "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE";
			}

		} else {
			boolean bothNull =  redundantBranch.firstValue.isDefinitelyNull() && redundantBranch.secondValue.isDefinitelyNull();		
			if (bothNull) warning = "RCN_REDUNDANT_COMPARISON_TWO_NULL_VALUES";
			else warning = "RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE";
			if (redundantBranch.secondValue.isChecked()) isChecked = true;
			if (redundantBranch.secondValue.wouldHaveBeenAKaboom()) wouldHaveBeenAKaboom = true;

		}
		
		if (wouldHaveBeenAKaboom) priority = HIGH_PRIORITY;
		else if (isChecked) priority = NORMAL_PRIORITY;
		BugInstance bugInstance =
			new BugInstance(this, warning, priority)
				.addClassAndMethod(methodGen, sourceFile)
				.addSourceLine(methodGen, sourceFile, location.getHandle());
		
		if (FindBugsAnalysisProperties.isRelaxedMode()) {
			WarningPropertySet propertySet = new WarningPropertySet();
			WarningPropertyUtil.addPropertiesForLocation(propertySet, classContext, method, location);
			if (isChecked) 
				propertySet.addProperty(NullDerefProperty.CHECKED_VALUE);
			if (wouldHaveBeenAKaboom) 
				propertySet.addProperty(NullDerefProperty.WOULD_HAVE_BEEN_A_KABOOM);
			
			
			propertySet.decorateBugInstance(bugInstance);
			
			priority = propertySet.computePriority(NORMAL_PRIORITY);
			bugInstance.setPriority(priority);
		}

		bugReporter.reportBug(bugInstance);
	}

}

// vim:ts=4
