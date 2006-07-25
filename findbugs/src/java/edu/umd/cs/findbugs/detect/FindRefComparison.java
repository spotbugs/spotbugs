/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.type.*;
import edu.umd.cs.findbugs.props.*;
import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

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
	private static final boolean DEBUG = Boolean.getBoolean("frc.debug");
	private static final boolean REPORT_ALL_REF_COMPARISONS = Boolean.getBoolean("findbugs.refcomp.reportAll");

	/**
	 * Classes that are suspicious if compared by reference.
	 */
	private static final HashSet<String> suspiciousSet = new HashSet<String>();

	static {
		suspiciousSet.add("java.lang.Boolean");
		suspiciousSet.add("java.lang.Byte");
		suspiciousSet.add("java.lang.Character");
		suspiciousSet.add("java.lang.Double");
		suspiciousSet.add("java.lang.Float");
		suspiciousSet.add("java.lang.Integer");
		suspiciousSet.add("java.lang.Long");
		suspiciousSet.add("java.lang.Short");
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

	private static final String STRING_SIGNATURE = "Ljava/lang/String;";

	/**
	 * Type representing a dynamically created String.
	 * This sort of String should never be compared using reference
	 * equality.
	 */
	private static class DynamicStringType extends ObjectType {
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
	private static class StaticStringType extends ObjectType {
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
			return type == T_DYNAMIC_STRING || type == T_STATIC_STRING;
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BugReporter bugReporter;
	private ClassContext classContext;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindRefComparison(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
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
	}

	/**
	 * A BugInstance and its WarningPropertySet.
	 */
	private static class WarningWithProperties {
		BugInstance instance;
		WarningPropertySet propertySet;
		Location location;
		
		WarningWithProperties(BugInstance warning, WarningPropertySet propertySet, Location location) {
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
		        new TypeAnalysis(methodGen, cfg, dfs, typeMerger, visitor, bugReporter, exceptionSetFactory);
		TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
		typeDataflow.execute();

		// Inspect Locations in the method for suspicious ref comparisons and calls to equals()
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			sawCallToEquals = inspectLocation(
					sawCallToEquals,
					jclass,
					cpg,
					methodGen,
					refComparisonList,
					stringComparisonList,
					visitor,
					typeDataflow,
					location);
		}
		
		// Add method-wide properties to BugInstances
		final boolean sawEquals = sawCallToEquals;
		decorateWarnings(stringComparisonList, new WarningDecorator(){
			public void decorate(WarningWithProperties warn) {
				if (sawEquals) {
					warn.propertySet.addProperty(RefComparisonWarningProperty.SAW_CALL_TO_EQUALS);
				}
				
				if (!(method.isPublic() || method.isProtected())) {
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
			MethodGen methodGen,
			LinkedList<WarningWithProperties> refComparisonList,
			LinkedList<WarningWithProperties> stringComparisonList,
			RefComparisonTypeFrameModelingVisitor visitor,
			TypeDataflow typeDataflow,
			Location location) throws DataflowAnalysisException {
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
				checkEqualsComparison(location, jclass, methodGen, typeDataflow);
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
		
		// T1 T2 result
		// S  S  no-op
		// D  ?  high
		// ?  D  high
		// S  ?  normal
		// ?  S  normal
		WarningPropertySet propertySet = new WarningPropertySet();
		if (type1 == T_STATIC_STRING && type2 == T_STATIC_STRING) {
			//priority = LOW_PRIORITY + 1;
			propertySet.addProperty(RefComparisonWarningProperty.COMPARE_STATIC_STRINGS);
		} else if (type1 == T_DYNAMIC_STRING || type2 == T_DYNAMIC_STRING) {
			//priority = HIGH_PRIORITY;
			propertySet.addProperty(RefComparisonWarningProperty.DYNAMIC_AND_UNKNOWN);
		} else if (type1 == T_STATIC_STRING || type2 == T_STATIC_STRING) {
			//priority = LOW_PRIORITY;
			propertySet.addProperty(RefComparisonWarningProperty.STATIC_AND_UNKNOWN);
		} else if (visitor.sawStringIntern()) {
			//priority = LOW_PRIORITY;
			propertySet.addProperty(RefComparisonWarningProperty.SAW_INTERN);
		}
		
		String sourceFile = jclass.getSourceFileName();
		BugInstance instance =
			new BugInstance(this, "ES_COMPARING_STRINGS_WITH_EQ", NORMAL_PRIORITY)
			.addClassAndMethod(methodGen, sourceFile)
			.addClass("java.lang.String").describe("CLASS_REFTYPE")
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
		        .addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle())
		        .addClass(lhs).describe("CLASS_REFTYPE");
		refComparisonList.add(new WarningWithProperties(instance, new WarningPropertySet(), location));
	}

	private void checkEqualsComparison(
			Location location,
			JavaClass jclass,
			MethodGen methodGen,
			TypeDataflow typeDataflow) throws DataflowAnalysisException {

		InstructionHandle handle = location.getHandle();
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

		if (!(lhsType_ instanceof ReferenceType) || !(rhsType_ instanceof ReferenceType)) {
			if (rhsType_.getType() == T_NULL) {
				// A literal null value was passed directly to equals().
				bugReporter.reportBug(new BugInstance(this, "EC_NULL_ARG", NORMAL_PRIORITY)
				        .addClassAndMethod(methodGen, sourceFile)
				        .addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle()));
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
			bugReporter.reportBug(new BugInstance(this, "EC_BAD_ARRAY_COMPARE", NORMAL_PRIORITY)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addClass(lhsType_.getSignature()).describe("CLASS_REFTYPE")
			        .addClass(rhsType_.getSignature()).describe("CLASS_REFTYPE")
			        .addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle())
			         );
		do {
			lhsType_ = ((ArrayType)lhsType_).getElementType();
			rhsType_ = ((ArrayType)rhsType_).getElementType();
		} while  (lhsType_ instanceof ArrayType && rhsType_ instanceof ArrayType);
		}
		
		if (lhsType_ instanceof ArrayType) {
			int priority = HIGH_PRIORITY;
			if (rhsType_.equals(ObjectType.OBJECT)) priority = LOW_PRIORITY;
			bugReporter.reportBug(new BugInstance(this, "EC_ARRAY_AND_NONARRAY", priority)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addClass(lhsType_.getSignature()).describe("CLASS_REFTYPE")
			        .addClass(rhsType_.getSignature()).describe("CLASS_REFTYPE")
			        .addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle())
			         );
		}
		if (rhsType_ instanceof ArrayType) {
			int priority = HIGH_PRIORITY;
			if (lhsType_.equals(ObjectType.OBJECT)) priority = LOW_PRIORITY;
			bugReporter.reportBug(new BugInstance(this, "EC_ARRAY_AND_NONARRAY", priority)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addClass(rhsType_.getSignature()).describe("CLASS_REFTYPE")
			        .addClass(lhsType_.getSignature()).describe("CLASS_REFTYPE")
			        .addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle())
			         );
		}
		if (lhsType_.equals(rhsType_))
			return;

		// For now, ignore the case where either reference is not
		// of an object type.  (It could be either an array or null.)
		if (!(lhsType_ instanceof ObjectType) || !(rhsType_ instanceof ObjectType))
			return;

		ObjectType lhsType = (ObjectType) lhsType_;
		ObjectType rhsType = (ObjectType) rhsType_;

		int priority = LOW_PRIORITY + 1;
		String bugType = "EC_UNRELATED_TYPES";

		// See if the types are related by inheritance.
		try {
			if (!Hierarchy.isSubtype(lhsType, rhsType) && !Hierarchy.isSubtype(rhsType, lhsType)) {
				AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
				
				// Look up the classes
				JavaClass lhsClass = analysisContext.lookupClass(lhsType.getClassName());
				JavaClass rhsClass = analysisContext.lookupClass(rhsType.getClassName());

				if (!lhsClass.isInterface() && !rhsClass.isInterface()) {
					// Both are class types, and therefore there is no possible way
					// the compared objects can have the same runtime type.
					priority = HIGH_PRIORITY;
				} else {
					/*
					if (DEBUG) {
						System.out.println("Subtypes of " + lhsClass.getClassName() + " are " +
								classSetToString(analysisContext.getSubtypes().getTransitiveSubtypes(lhsClass)));
						System.out.println("Subtypes of " + rhsClass.getClassName() + " are " +
								classSetToString(analysisContext.getSubtypes().getTransitiveSubtypes(rhsClass)));
					}
					*/
					
					// Look up the common subtypes of the two types.  If the
					// intersection does not contain at least one instantiable class,
					// then issue a warning of the appropriate type.
					Set<JavaClass> commonSubtypes =
						analysisContext.getSubtypes().getTransitiveCommonSubtypes(lhsClass, rhsClass);
					
					if (!containsAtLeastOneInstantiableClass(commonSubtypes)) {
						priority = HIGH_PRIORITY;
						bugType = (lhsClass.isInterface() && rhsClass.isInterface())
							? "EC_UNRELATED_INTERFACES" : "EC_UNRELATED_CLASS_AND_INTERFACE";
					}
				}
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return;
		}

		if (methodGen.getName().startsWith("test") && methodGen.getSignature().equals("()V")) {
			try {
				if (Hierarchy.isSubtype(methodGen.getClassName(), "junit.framework.TestCase"))
					priority+=2;
			} catch (ClassNotFoundException e) { priority+=2; }
			}


		

		if (priority <= LOW_PRIORITY) {
			bugReporter.reportBug(new BugInstance(this, bugType, priority)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addClass(lhsType.getClassName()).describe("CLASS_REFTYPE")
			        .addClass(rhsType.getClassName()).describe("CLASS_REFTYPE")
			        .addSourceLine(this.classContext, methodGen, sourceFile, location.getHandle())
			        );
		}
	}
	
	private static boolean containsAtLeastOneInstantiableClass(Set<JavaClass> commonSubtypes) {
		for (JavaClass javaClass : commonSubtypes) {
			if (!javaClass.isInterface() && !javaClass.isAbstract())
				return true;
		}
		return false;
	}

	/*
	private static String classSetToString(Set<JavaClass> classSet) {
		StringBuffer buf = new StringBuffer();
		for (JavaClass javaClass : classSet) {
			if (buf.length() > 0)
				buf.append(',');
			buf.append(javaClass.getClassName());
		}
		return buf.toString();
	}
	*/

	public void report() {
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
				        TypeAnalysis analysis = new TypeAnalysis(methodGen, cfg, dfs, typeMerger, visitor,
				                lookupFailureCallback, exceptionSetFactory);
				        Dataflow<TypeFrame, TypeAnalysis> dataflow =
				                new Dataflow<TypeFrame, TypeAnalysis>(cfg, analysis);
				        dataflow.execute();

				        return dataflow;
			        }
		        };

		driver.execute(argv[0]);
	}
}

// vim:ts=3
