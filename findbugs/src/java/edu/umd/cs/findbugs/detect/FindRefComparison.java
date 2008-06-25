/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2007, University of Maryland
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
import java.util.StringTokenizer;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.type.ExceptionSetFactory;
import edu.umd.cs.findbugs.ba.type.ExtendedTypes;
import edu.umd.cs.findbugs.ba.type.StandardTypeMerger;
import edu.umd.cs.findbugs.ba.type.TypeAnalysis;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.type.TypeMerger;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.props.WarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.props.WarningPropertyUtil;

/**
 * Find suspicious reference comparisons.
 * This includes:
 * <ul>
 * <li>Strings and other java.lang objects compared by reference equality</li>
 * <li>Calls to equals(Object) where the argument is a different type than
 *     the receiver object</li>
 * </ul>
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class FindRefComparison implements Detector, ExtendedTypes {
	private static final boolean DEBUG = SystemProperties.getBoolean("frc.debug");
	private static final boolean REPORT_ALL_REF_COMPARISONS = SystemProperties.getBoolean("findbugs.refcomp.reportAll");
	private static final int BASE_ES_PRIORITY = SystemProperties.getInteger("es.basePriority", NORMAL_PRIORITY);

	/**
	 * Classes that are suspicious if compared by reference.
	 */
	private static final HashSet<String> DEFAULT_SUSPICIOUS_SET = new HashSet<String>();

	static {
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Boolean");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Byte");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Character");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Double");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Float");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Integer");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Long");
		DEFAULT_SUSPICIOUS_SET.add("java.lang.Short");
	}

	/**
	 * Set of opcodes that invoke instance methods on an object.
	 */
	private static final BitSet invokeInstanceSet = new BitSet();

	static {
		invokeInstanceSet.set(Constants.INVOKEVIRTUAL);
		invokeInstanceSet.set(Constants.INVOKEINTERFACE);
		invokeInstanceSet.set(Constants.INVOKESPECIAL);
	}

	/**
	 * Set of bytecodes using for prescreening.
	 */
	private static final BitSet prescreenSet = new BitSet();

	static {
		prescreenSet.or(invokeInstanceSet);
		prescreenSet.set(Constants.IF_ACMPEQ);
		prescreenSet.set(Constants.IF_ACMPNE);
	}

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static final byte T_DYNAMIC_STRING = T_AVAIL_TYPE + 0;
	private static final byte T_STATIC_STRING = T_AVAIL_TYPE + 1;
	private static final byte T_PARAMETER_STRING = T_AVAIL_TYPE + 2;

	private static final String STRING_SIGNATURE = "Ljava/lang/String;";

	/**
     * @author pugh
     */
    private final static class SpecialTypeAnalysis extends TypeAnalysis {
	    /**
	     * @param method
	     * @param methodGen
	     * @param cfg
	     * @param dfs
	     * @param typeMerger
	     * @param visitor
	     * @param lookupFailureCallback
	     * @param exceptionSetFactory
	     */
	    private SpecialTypeAnalysis(Method method, MethodGen methodGen, CFG cfg, DepthFirstSearch dfs, TypeMerger typeMerger,
	            TypeFrameModelingVisitor visitor, RepositoryLookupFailureCallback lookupFailureCallback,
	            ExceptionSetFactory exceptionSetFactory) {
		    super(method, methodGen, cfg, dfs, typeMerger, visitor, lookupFailureCallback, exceptionSetFactory);
	    }

	    @Override public void initEntryFact(TypeFrame result) {
	    	super.initEntryFact(result);
	    	for(int i = 0; i < methodGen.getMaxLocals(); i++) {
	    		Type t = result.getValue(i);
	    		if (t.equals(ObjectType.STRING))
	    			result.setValue(i, parameterStringTypeInstance);
	    	}
	    }
    }

	/**
	 * Type representing a dynamically created String.
	 * This sort of String should never be compared using reference
	 * equality.
	 */
	public static class DynamicStringType extends ObjectType {
		private static final long serialVersionUID = 1L;

		public DynamicStringType() {
			super("java.lang.String");
		}

		@Override
		public byte getType() {
			return T_DYNAMIC_STRING;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object o) {
			return o == this;
		}

		@Override
		public String toString() {
			return "<dynamic string>";
		}
	}

	private static final Type dynamicStringTypeInstance = new DynamicStringType();

	/**
	 * Type representing a static String.
	 * E.g., interned strings and constant strings.
	 * It is generally OK to compare this sort of String
	 * using reference equality.
	 */
	public static class StaticStringType extends ObjectType {
		private static final long serialVersionUID = 1L;

		public StaticStringType() {
			super("java.lang.String");
		}

		@Override
		public byte getType() {
			return T_STATIC_STRING;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object o) {
			return o == this;
		}

		@Override
		public String toString() {
			return "<static string>";
		}
	}

	private static final Type staticStringTypeInstance = new StaticStringType();

	/**
	 * Type representing a String passed as a parameter.
	 */
	public static class ParameterStringType extends ObjectType {
		private static final long serialVersionUID = 1L;

		public ParameterStringType() {
			super("java.lang.String");
		}

		@Override
		public byte getType() {
			return T_PARAMETER_STRING;
		}

		@Override
		public int hashCode() {
			return System.identityHashCode(this);
		}

		@Override
		public boolean equals(Object o) {
			return o == this;
		}

		@Override
		public String toString() {
			return "<parameter string>";
		}
	}

	private static final Type parameterStringTypeInstance = new ParameterStringType();

	private static class RefComparisonTypeFrameModelingVisitor extends TypeFrameModelingVisitor {
		private RepositoryLookupFailureCallback lookupFailureCallback;
		private boolean sawStringIntern;

		public RefComparisonTypeFrameModelingVisitor(
				ConstantPoolGen cpg,
				RepositoryLookupFailureCallback lookupFailureCallback) {
			super(cpg);
			this.lookupFailureCallback = lookupFailureCallback;
			this.sawStringIntern = false;
		}

		public boolean sawStringIntern() {
			return sawStringIntern;
		}

		// Override handlers for bytecodes that may return String objects
		// known to be dynamic or static.

		@Override
		public void visitINVOKESTATIC(INVOKESTATIC obj) {
			consumeStack(obj);
			if (returnsString(obj)) {
				String className = obj.getClassName(getCPG());
				if (className.equals("java.lang.String")) {
					pushValue(dynamicStringTypeInstance);
				} else {
					pushReturnType(obj);
				}
			} else {
				pushReturnType(obj);
			}
		}

		@Override
		public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
			handleInstanceMethod(obj);
		}

		@Override
		public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
			handleInstanceMethod(obj);
		}

		@Override
		public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
			handleInstanceMethod(obj);
		}

		private boolean returnsString(InvokeInstruction inv) {
			String methodSig = inv.getSignature(getCPG());
			return methodSig.endsWith(")Ljava/lang/String;");
		}

		private void handleInstanceMethod(InvokeInstruction obj) {
			consumeStack(obj);
			if (returnsString(obj)) {
				String className = obj.getClassName(getCPG());
				String methodName = obj.getName(getCPG());
				// System.out.println(className + "." + methodName);

				if (methodName.equals("intern") && className.equals("java.lang.String")) {
					sawStringIntern = true;
					pushValue(staticStringTypeInstance);
				} else if (methodName.equals("toString")
						|| className.equals("java.lang.String")) {
					pushValue(dynamicStringTypeInstance);
					// System.out.println("  dynamic");
				} else
					pushReturnType(obj);
			} else
				pushReturnType(obj);
		}

		@Override
		public void visitLDC(LDC obj) {
			Type type = obj.getType(getCPG());
			pushValue(isString(type) ? staticStringTypeInstance : type);
		}

		@Override
		public void visitLDC2_W(LDC2_W obj) {
			Type type = obj.getType(getCPG());
			pushValue(isString(type) ? staticStringTypeInstance : type);
		}

		private boolean isString(Type type) {
			return type.getSignature().equals(STRING_SIGNATURE);
		}

		@Override
		public void visitGETSTATIC(GETSTATIC obj) {
			handleLoad(obj);
		}

		@Override
		public void visitGETFIELD(GETFIELD obj) {
			handleLoad(obj);
		}

		private void handleLoad(FieldInstruction obj) {
			consumeStack(obj);

			Type type = obj.getType(getCPG());
			if (type.getSignature().equals(STRING_SIGNATURE)) {
				try {
					String className = obj.getClassName(getCPG());
					String fieldName = obj.getName(getCPG());
					Field field = Hierarchy.findField(className, fieldName);

					if (field != null) {
						// If the field is final, we'll assume that the String value
						// is static.
						if (field.isFinal())
							pushValue(staticStringTypeInstance);
						else
							pushValue(type);

						return;
					}
				} catch (ClassNotFoundException ex) {
					lookupFailureCallback.reportMissingClass(ex);
				}
			}

			pushValue(type);
		}
	}

	/**
	 * Type merger to use the extended String types.
	 */
	private static class RefComparisonTypeMerger extends StandardTypeMerger {
		public RefComparisonTypeMerger(RepositoryLookupFailureCallback lookupFailureCallback,
				ExceptionSetFactory exceptionSetFactory) {
			super(lookupFailureCallback, exceptionSetFactory);
		}

		@Override
		protected boolean isReferenceType(byte type) {
			return super.isReferenceType(type) || type == T_STATIC_STRING || type == T_DYNAMIC_STRING;
		}

		@Override
		protected Type mergeReferenceTypes(ReferenceType aRef, ReferenceType bRef) throws DataflowAnalysisException {
			byte aType = aRef.getType();
			byte bType = bRef.getType();

			if (isExtendedStringType(aType) || isExtendedStringType(bType)) {
				// If both types are the same extended String type,
				// then the same type is returned.  Otherwise, extended
				// types are downgraded to plain java.lang.String,
				// and a standard merge is applied.
				if (aType == bType)
					return aRef;

				if (isExtendedStringType(aType))
					aRef = Type.STRING;
				if (isExtendedStringType(bType))
					bRef = Type.STRING;
			}

			return super.mergeReferenceTypes(aRef, bRef);
		}

		private boolean isExtendedStringType(byte type) {
			return type == T_DYNAMIC_STRING || type == T_STATIC_STRING || type == T_PARAMETER_STRING;
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BugReporter bugReporter;
	private BugAccumulator bugAccumulator;
	private ClassContext classContext;
	private Set<String> suspiciousSet;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindRefComparison(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.bugAccumulator = new BugAccumulator(bugReporter);
		this.suspiciousSet = new HashSet<String>(DEFAULT_SUSPICIOUS_SET);
		
		// Check frc.suspicious system property for additional suspicious types to check
		String extraSuspiciousTypes = SystemProperties.getProperty("frc.suspicious");
		if (extraSuspiciousTypes != null) {
			StringTokenizer tok = new StringTokenizer(extraSuspiciousTypes, ",");
			while (tok.hasMoreTokens()) {
				suspiciousSet.add(tok.nextToken());
			}
		}
	}

	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;

		JavaClass jclass = classContext.getJavaClass();
		Method[] methodList = jclass.getMethods();

		for (Method method : methodList) {
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			// Prescreening - must have IF_ACMPEQ, IF_ACMPNE,
			// or an invocation of an instance method
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (bytecodeSet == null || !bytecodeSet.intersects(prescreenSet))
				continue;

			if (DEBUG)
				System.out.println("FindRefComparison: analyzing " +
						SignatureConverter.convertMethodSignature(methodGen));

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("Error analyzing " + method.toString(), e);
			} catch (DataflowAnalysisException e) {
				// bugReporter.logError("Error analyzing " + method.toString(), e);
			}
		}
		bugAccumulator.reportAccumulatedBugs();
	}

	/**
	 * A BugInstance and its WarningPropertySet.
	 */
	private static class WarningWithProperties {
		BugInstance instance;
		WarningPropertySet<WarningProperty> propertySet;
		Location location;

		WarningWithProperties(BugInstance warning, WarningPropertySet<WarningProperty> propertySet, Location location) {
			this.instance = warning;
			this.propertySet = propertySet;
			this.location = location;
		}
	}

	private interface WarningDecorator {
		public void decorate(WarningWithProperties warn);
	}

	private void analyzeMethod(ClassContext classContext, final Method method)
	throws CFGBuilderException, DataflowAnalysisException {

		MethodGen methodGen = classContext.getMethodGen(method);
		if (methodGen == null) return;
		boolean sawCallToEquals = false;
		JavaClass jclass = classContext.getJavaClass();
		ConstantPoolGen cpg = classContext.getConstantPoolGen();


		// Enqueue all of the potential violations we find in the method.
		// Normally we'll only report the first highest-priority warning,
		// but if in relaxed mode or if REPORT_ALL_REF_COMPARISONS is set,
		// then we'll report everything.
		LinkedList<WarningWithProperties> refComparisonList =
			new LinkedList<WarningWithProperties>();
		LinkedList<WarningWithProperties> stringComparisonList =
			new LinkedList<WarningWithProperties>();

		CFG cfg = classContext.getCFG(method);
		DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
		ExceptionSetFactory exceptionSetFactory =
			classContext.getExceptionSetFactory(method);

		// Perform type analysis using our special type merger
		// (which handles String types specially, keeping track of
		// which ones appear to be dynamically created)
		RefComparisonTypeMerger typeMerger =
			new RefComparisonTypeMerger(bugReporter, exceptionSetFactory);
		RefComparisonTypeFrameModelingVisitor visitor =
			new RefComparisonTypeFrameModelingVisitor(methodGen.getConstantPool(), bugReporter);
		TypeAnalysis typeAnalysis =
			new SpecialTypeAnalysis(method, methodGen, cfg, dfs, typeMerger, visitor, bugReporter, exceptionSetFactory);
		TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
		Profiler profiler = Profiler.getInstance();
		profiler.start(SpecialTypeAnalysis.class);
		try {
		typeDataflow.execute();
		} finally {
			profiler.end(SpecialTypeAnalysis.class);
		}

		// Inspect Locations in the method for suspicious ref comparisons and calls to equals()
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			sawCallToEquals = inspectLocation(
					sawCallToEquals,
					jclass,
					cpg,
					method,
					methodGen,
					refComparisonList,
					stringComparisonList,
					visitor,
					typeDataflow, location);
		}

		// Add method-wide properties to BugInstances
		final boolean sawEquals = sawCallToEquals;
		decorateWarnings(stringComparisonList, new WarningDecorator(){
			public void decorate(WarningWithProperties warn) {
				if (sawEquals) {
					warn.propertySet.addProperty(RefComparisonWarningProperty.SAW_CALL_TO_EQUALS);
				}

				if (false && !(method.isPublic() || method.isProtected())) {
					warn.propertySet.addProperty(RefComparisonWarningProperty.PRIVATE_METHOD);
				}
			}
		});
		decorateWarnings(refComparisonList, new WarningDecorator() {
			public void decorate(WarningWithProperties warn) {
				if (sawEquals) {
					warn.propertySet.addProperty(RefComparisonWarningProperty.SAW_CALL_TO_EQUALS);
				}
			}
		});

		// Report violations
		boolean relaxed = FindBugsAnalysisFeatures.isRelaxedMode();
		reportBest(classContext, method, stringComparisonList, relaxed);
		reportBest(classContext, method, refComparisonList, relaxed);
	}

	private boolean inspectLocation(
			boolean sawCallToEquals,
			JavaClass jclass,
			ConstantPoolGen cpg,
			Method method,
			MethodGen methodGen,
			LinkedList<WarningWithProperties> refComparisonList,
			LinkedList<WarningWithProperties> stringComparisonList,
			RefComparisonTypeFrameModelingVisitor visitor,
			TypeDataflow typeDataflow, Location location) throws DataflowAnalysisException {
		Instruction ins = location.getHandle().getInstruction();
		short opcode = ins.getOpcode();
		if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
			checkRefComparison(
					location,
					jclass,
					methodGen,
					visitor,
					typeDataflow,
					stringComparisonList,
					refComparisonList);
		} else if (invokeInstanceSet.get(opcode)) {
			InvokeInstruction inv = (InvokeInstruction) ins;
			String methodName = inv.getMethodName(cpg);
			String methodSig = inv.getSignature(cpg);
			if (isEqualsMethod(methodName, methodSig)) {
				sawCallToEquals = true;
				checkEqualsComparison(location, jclass, method, methodGen, cpg, typeDataflow);
			}
		}
		return sawCallToEquals;
	}

	private void decorateWarnings(
			LinkedList<WarningWithProperties> stringComparisonList,
			WarningDecorator warningDecorator) {
		for (WarningWithProperties warn : stringComparisonList) {
			warningDecorator.decorate(warn);
			warn.instance.setPriority(warn.propertySet.computePriority(NORMAL_PRIORITY));
		}
	}

	private void reportBest(
			ClassContext classContext,
			Method method,
			LinkedList<WarningWithProperties> warningList,
			boolean relaxed) {
		boolean reportAll = relaxed || REPORT_ALL_REF_COMPARISONS;

		WarningWithProperties best = null;
		for (WarningWithProperties warn : warningList) {
			if (best == null || warn.instance.getPriority() < best.instance.getPriority()) {
				best = warn;
			}

			if (reportAll) {
				if (relaxed) {
					// Add general warning properties
					WarningPropertyUtil.addPropertiesForLocation(
							warn.propertySet,
							classContext,
							method,
							warn.location);

					// Convert warning properties to bug properties
					warn.propertySet.decorateBugInstance(warn.instance);
				}
				bugReporter.reportBug(warn.instance);
			}
		}
		if (best != null && !reportAll) {
			bugReporter.reportBug(best.instance);
		}
	}

	private boolean isEqualsMethod(String methodName, String methodSig) {
		return (methodName.equals("equals") && methodSig.equals("(Ljava/lang/Object;)Z"))
		|| (methodName.equals("equalIgnoreCases") && methodSig.equals("(Ljava/lang/String;)Z"));
	}

	private void checkRefComparison(
			Location location,
			JavaClass jclass,
			MethodGen methodGen,
			RefComparisonTypeFrameModelingVisitor visitor,
			TypeDataflow typeDataflow,
			List<WarningWithProperties> stringComparisonList,
			List<WarningWithProperties> refComparisonList) throws DataflowAnalysisException {

		InstructionHandle handle = location.getHandle();

		TypeFrame frame = typeDataflow.getFactAtLocation(location);
		if (frame.getStackDepth() < 2)
			throw new DataflowAnalysisException("Stack underflow", methodGen, handle);

		int numSlots = frame.getNumSlots();
		Type lhsType = frame.getValue(numSlots - 1);
		Type rhsType = frame.getValue(numSlots - 2);

		if (lhsType instanceof ReferenceType && rhsType instanceof ReferenceType) {
			IncompatibleTypes result = IncompatibleTypes.getPriorityForAssumingCompatible(lhsType, rhsType, true);
			if (result != IncompatibleTypes.SEEMS_OK) {
				String sourceFile = jclass.getSourceFileName();
				
				bugAccumulator.accumulateBug(new BugInstance(this, "EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", result.getPriority())
				.addClassAndMethod(methodGen, sourceFile)
				.addFoundAndExpectedType(rhsType.getSignature(), lhsType.getSignature()),
				SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, handle)
				);
			}
			String lhs = SignatureConverter.convert(lhsType.getSignature());
			String rhs = SignatureConverter.convert(rhsType.getSignature());

			if (!lhs.equals(rhs))
				return;

			if (lhs.equals("java.lang.String")) {
				handleStringComparison(jclass, methodGen, visitor, stringComparisonList, location, lhsType, rhsType);
			} else if (suspiciousSet.contains(lhs)) {
				handleSuspiciousRefComparison(jclass, methodGen, refComparisonList, location, lhs);
			}
		}
	}

	private void handleStringComparison(
			JavaClass jclass,
			MethodGen methodGen,
			RefComparisonTypeFrameModelingVisitor visitor,
			List<WarningWithProperties> stringComparisonList,
			Location location,
			Type lhsType,
			Type rhsType) {
		if (DEBUG) System.out.println("String/String comparison at " + location.getHandle());

		// Compute the priority:
		// - two static strings => do not report
		// - dynamic string and anything => high
		// - static string and unknown => medium
		// - all other cases => low
		// System.out.println("Compare " + lhsType + " == " + rhsType);
		byte type1 = lhsType.getType();
		byte type2 = rhsType.getType();

		String bugPattern = "ES_COMPARING_STRINGS_WITH_EQ";
		// T1 T2 result
		// S  S  no-op
		// D  ?  high
		// ?  D  high
		// S  ?  normal
		// ?  S  normal
		WarningPropertySet<WarningProperty> propertySet = new WarningPropertySet<WarningProperty>();
		if (type1 == T_STATIC_STRING && type2 == T_STATIC_STRING) {
			propertySet.addProperty(RefComparisonWarningProperty.COMPARE_STATIC_STRINGS);
		} else if (type1 == T_DYNAMIC_STRING || type2 == T_DYNAMIC_STRING) {
			propertySet.addProperty(RefComparisonWarningProperty.DYNAMIC_AND_UNKNOWN);
		} else if (type2 == T_PARAMETER_STRING || type1 == T_PARAMETER_STRING) {
			bugPattern = "ES_COMPARING_PARAMETER_STRING_WITH_EQ";
			if (methodGen.isPublic() || methodGen.isProtected()) propertySet.addProperty(RefComparisonWarningProperty.STRING_PARAMETER_IN_PUBLIC_METHOD);
			else propertySet.addProperty(RefComparisonWarningProperty.STRING_PARAMETER);
		} else if (type1 == T_STATIC_STRING || type2 == T_STATIC_STRING) {
			propertySet.addProperty(RefComparisonWarningProperty.STATIC_AND_UNKNOWN);
		} else if (visitor.sawStringIntern()) {
			propertySet.addProperty(RefComparisonWarningProperty.SAW_INTERN);
		}

		String sourceFile = jclass.getSourceFileName();
		BugInstance instance =
			new BugInstance(this, bugPattern, BASE_ES_PRIORITY)
		.addClassAndMethod(methodGen, sourceFile)
		.addType("Ljava/lang/String;").describe(TypeAnnotation.FOUND_ROLE)
		.addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle());

		WarningWithProperties warn = new WarningWithProperties(instance, propertySet, location);
		stringComparisonList.add(warn);
	}

	private void handleSuspiciousRefComparison(
			JavaClass jclass,
			MethodGen methodGen,
			List<WarningWithProperties> refComparisonList,
			Location location,
			String lhs) {
		String sourceFile = jclass.getSourceFileName();
		BugInstance instance = new BugInstance(this, "RC_REF_COMPARISON", lhs.equals("java.lang.Boolean") ? NORMAL_PRIORITY : HIGH_PRIORITY)
		.addClassAndMethod(methodGen, sourceFile)
		.addType("L" + lhs.replace('.', '/')+";").describe(TypeAnnotation.FOUND_ROLE)
		.addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle());

		refComparisonList.add(new WarningWithProperties(instance, new WarningPropertySet<WarningProperty>(), location));
	}

	private static boolean testLikeName(String name) {
		return name.toLowerCase().indexOf("test") >= 0;
	}
	private void checkEqualsComparison(
			Location location,
			JavaClass jclass,
			Method method,
			MethodGen methodGen, ConstantPoolGen cpg, TypeDataflow typeDataflow) throws DataflowAnalysisException {

		InstructionHandle handle = location.getHandle();
		InstructionHandle next = handle.getNext();
		if (next != null && next.getInstruction() instanceof INVOKESTATIC) {
			INVOKESTATIC is = (INVOKESTATIC) next.getInstruction();
			if (is.getMethodName(cpg).equals("assertFalse")) return;
		}
		String sourceFile = jclass.getSourceFileName();

		TypeFrame frame = typeDataflow.getFactAtLocation(location);
		if (frame.getStackDepth() < 2)
			throw new DataflowAnalysisException("Stack underflow", methodGen, handle);

		int numSlots = frame.getNumSlots();
		Type lhsType_ = frame.getValue(numSlots - 2);
		Type rhsType_ = frame.getValue(numSlots - 1);


		// Ignore top and bottom values
		if (lhsType_.getType() == T_TOP || lhsType_.getType() == T_BOTTOM
				|| rhsType_.getType() == T_TOP || rhsType_.getType() == T_BOTTOM)
			return;

		String methodName = method.getName();
		boolean looksLikeTestCase = methodName.startsWith("test") && method.isPublic() 
		&& method.getSignature().equals("()V")
		|| testLikeName(jclass.getClassName())|| testLikeName(jclass.getSuperclassName());
		int priorityModifier = 0;
		if (looksLikeTestCase) {
			priorityModifier = 1;
			try {
				if (jclass.getSuperclassName().equals("junit.framework.TestCase") || Hierarchy.isSubtype(methodGen.getClassName(), "junit.framework.TestCase"))
					priorityModifier=2;
			} catch (ClassNotFoundException e) { 
				AnalysisContext.reportMissingClass(e);
			}
		}

		if (!(lhsType_ instanceof ReferenceType) || !(rhsType_ instanceof ReferenceType)) {
			if (rhsType_.getType() == T_NULL) {	
				// A literal null value was passed directly to equals().
				if (!looksLikeTestCase)
					bugAccumulator.accumulateBug(new BugInstance(this, "EC_NULL_ARG", NORMAL_PRIORITY)
					.addClassAndMethod(methodGen, sourceFile),
					SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle()));
			} else if (lhsType_.getType() == T_NULL) {
				// Hmm...in this case, equals() is being invoked on
				// a literal null value.  This is really the
				// purview of FindNullDeref.  So, we'll just do nothing.
			} else {
				bugReporter.logError("equals() used to compare non-object type(s) " +
						lhsType_ + " and " + rhsType_ +
						" in " +
						SignatureConverter.convertMethodSignature(methodGen) +
						" at " + location.getHandle());
			}
			return;
		}
		if (lhsType_ instanceof ArrayType && rhsType_ instanceof ArrayType) {
			bugAccumulator.accumulateBug(new BugInstance(this, "EC_BAD_ARRAY_COMPARE", NORMAL_PRIORITY)
			.addClassAndMethod(methodGen, sourceFile)
			.addFoundAndExpectedType(rhsType_.getSignature(), lhsType_.getSignature()),
			SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle())
			);
		}
		IncompatibleTypes result = IncompatibleTypes.getPriorityForAssumingCompatible(lhsType_, rhsType_);
		if (result == IncompatibleTypes.ARRAY_AND_NON_ARRAY || result == IncompatibleTypes.ARRAY_AND_OBJECT) 
			bugAccumulator.accumulateBug(new BugInstance(this, "EC_ARRAY_AND_NONARRAY", result.getPriority())
			.addClassAndMethod(methodGen, sourceFile)
			.addFoundAndExpectedType(rhsType_.getSignature(), lhsType_.getSignature()),
			SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle())
			);
		else if (result == IncompatibleTypes.INCOMPATIBLE_CLASSES) {
			String lhsSig = lhsType_.getSignature();
			String rhsSig = rhsType_.getSignature();
			boolean core = lhsSig.startsWith("Ljava") && rhsSig.startsWith("Ljava");
			if (core) {
				looksLikeTestCase = false;
				priorityModifier = 0;
			}
			if (!looksLikeTestCase) {
				bugAccumulator.accumulateBug(new BugInstance(this, "EC_UNRELATED_TYPES", result.getPriority() + priorityModifier)
				.addClassAndMethod(methodGen, sourceFile)
				.addFoundAndExpectedType(rhsSig, lhsSig),
				SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle())
				);
			}
		}
		else if (result == IncompatibleTypes.UNRELATED_CLASS_AND_INTERFACE 
				|| result == IncompatibleTypes.UNRELATED_FINAL_CLASS_AND_INTERFACE) 
			bugAccumulator.accumulateBug(new BugInstance(this, "EC_UNRELATED_CLASS_AND_INTERFACE", result.getPriority())
			.addClassAndMethod(methodGen, sourceFile)
			.addFoundAndExpectedType(rhsType_.getSignature(), lhsType_.getSignature()),
			SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle())
			);
		else if (result == IncompatibleTypes.UNRELATED_INTERFACES) 
			bugAccumulator.accumulateBug(new BugInstance(this, "EC_UNRELATED_INTERFACES", result.getPriority())
			.addClassAndMethod(methodGen, sourceFile)
			.addFoundAndExpectedType(rhsType_.getSignature(), lhsType_.getSignature()),
			SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle())
			);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + FindRefComparison.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<TypeFrame, TypeAnalysis> driver =
			new DataflowTestDriver<TypeFrame, TypeAnalysis>() {
			@Override
			public Dataflow<TypeFrame, TypeAnalysis> createDataflow(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {

				RepositoryLookupFailureCallback lookupFailureCallback =
					classContext.getLookupFailureCallback();
				MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					throw new DataflowAnalysisException("Could not get methodGen for " + method.toString());
				DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
				CFG cfg = classContext.getCFG(method);
				ExceptionSetFactory exceptionSetFactory = classContext.getExceptionSetFactory(method);

				TypeMerger typeMerger =
					new RefComparisonTypeMerger(lookupFailureCallback, exceptionSetFactory);
				TypeFrameModelingVisitor visitor =
					new RefComparisonTypeFrameModelingVisitor(methodGen.getConstantPool(), lookupFailureCallback);
				TypeAnalysis analysis = new TypeAnalysis(method, methodGen, cfg, dfs, typeMerger,
						visitor, lookupFailureCallback, exceptionSetFactory);
				Dataflow<TypeFrame, TypeAnalysis> dataflow =
					new Dataflow<TypeFrame, TypeAnalysis>(cfg, analysis);
				dataflow.execute();

				return dataflow;
			}
		};

		driver.execute(argv[0]);
	}



	public void report() {
		// do nothing
	}
}

//vim:ts=3
