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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

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
		public DynamicStringType() {
			super("java.lang.String");
		}

		public byte getType() {
			return T_DYNAMIC_STRING;
		}

		public int hashCode() {
			return System.identityHashCode(this);
		}

		public boolean equals(Object o) {
			return o == this;
		}

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
		public StaticStringType() {
			super("java.lang.String");
		}

		public byte getType() {
			return T_STATIC_STRING;
		}

		public int hashCode() {
			return System.identityHashCode(this);
		}

		public boolean equals(Object o) {
			return o == this;
		}

		public String toString() {
			return "<static string>";
		}
	}

	private static final Type staticStringTypeInstance = new StaticStringType();

	private static class RefComparisonTypeFrameModelingVisitor extends TypeFrameModelingVisitor {
		private RepositoryLookupFailureCallback lookupFailureCallback;

		public RefComparisonTypeFrameModelingVisitor(ConstantPoolGen cpg, RepositoryLookupFailureCallback lookupFailureCallback) {
			super(cpg);
			this.lookupFailureCallback = lookupFailureCallback;
		}

		// Override handlers for bytecodes that may return String objects
		// known to be dynamic or static.

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

		public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
			handleInstanceMethod(obj);
		}

		public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
			handleInstanceMethod(obj);
		}

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

		public void visitLDC(LDC obj) {
			Type type = obj.getType(getCPG());
			pushValue(isString(type) ? staticStringTypeInstance : type);
		}

		public void visitLDC2_W(LDC2_W obj) {
			Type type = obj.getType(getCPG());
			pushValue(isString(type) ? staticStringTypeInstance : type);
		}

		private boolean isString(Type type) {
			return type.getSignature().equals(STRING_SIGNATURE);
		}

		public void visitGETSTATIC(GETSTATIC obj) {
			handleLoad(obj);
		}

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

		protected boolean isReferenceType(byte type) {
			return super.isReferenceType(type) || type == T_STATIC_STRING || type == T_DYNAMIC_STRING;
		}

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
	//private AnalysisContext analysisContext;
	private BugInstance stringComparison;
	private BugInstance refComparison;

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindRefComparison(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
		//this.analysisContext = analysisContext;
	}

	// XXX BAD EVIL NOT THREAD SAFE YUCK FIXME
	static boolean sawStringIntern;

	public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();
		Method[] methodList = jclass.getMethods();
		sawStringIntern = false;

		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			// Prescreening - must have IF_ACMPEQ, IF_ACMPNE,
			// or an invocation of an instance method
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (!bytecodeSet.intersects(prescreenSet))
				continue;

			if (DEBUG)
				System.out.println("FindRefComparison: analyzing " +
				        SignatureConverter.convertMethodSignature(methodGen));

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError(e.toString());
			} catch (DataflowAnalysisException e) {
				bugReporter.logError(e.toString());
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {

		boolean sawCallToEquals = false;
		JavaClass jclass = classContext.getJavaClass();
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		MethodGen methodGen = classContext.getMethodGen(method);

		// Report at most one String comparison per method.
		// We report the first highest priority warning.
		stringComparison = null;
		refComparison = null;

		CFG cfg = classContext.getCFG(method);
		DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
		ExceptionSetFactory exceptionSetFactory =
		        classContext.getExceptionSetFactory(method);

		// Perform type analysis using our special type merger
		// (which handles String types specially, keeping track of
		// which ones appear to be dynamically created)
		RefComparisonTypeMerger typeMerger =
		        new RefComparisonTypeMerger(bugReporter, exceptionSetFactory);
		TypeFrameModelingVisitor visitor =
		        new RefComparisonTypeFrameModelingVisitor(methodGen.getConstantPool(), bugReporter);
		TypeAnalysis typeAnalysis =
		        new TypeAnalysis(methodGen, cfg, dfs, typeMerger, visitor, bugReporter, exceptionSetFactory);
		TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
		typeDataflow.execute();

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			Instruction ins = location.getHandle().getInstruction();
			short opcode = ins.getOpcode();
			if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
				checkRefComparison(location, jclass, methodGen, typeDataflow);
			} else if (invokeInstanceSet.get(opcode)) {
				InvokeInstruction inv = (InvokeInstruction) ins;
				String methodName = inv.getMethodName(cpg);
				String methodSig = inv.getSignature(cpg);
				if ((methodName.equals("equals") && methodSig.equals("(Ljava/lang/Object;)Z"))
				        || (methodName.equals("equalIgnoreCases") && methodSig.equals("(Ljava/lang/String;)Z"))) {
					sawCallToEquals = true;
					checkEqualsComparison(location, jclass, methodGen, typeDataflow);
				}
			}
		}

		// If a String reference comparison was found in the method,
		// report it
		if (stringComparison != null) {
			if (sawCallToEquals &&
			        stringComparison.getPriority() >= NORMAL_PRIORITY) {
				// System.out.println("Reducing priority of " + stringComparison);
				stringComparison.setPriority(1 + stringComparison.getPriority());
			}
			if (stringComparison.getPriority() >= NORMAL_PRIORITY
			        && !(method.isPublic() ||
			        method.isProtected())) {
				// System.out.print("private/packed");
				stringComparison.setPriority(1 + stringComparison.getPriority());
			}
			if (stringComparison.getPriority() <= LOW_PRIORITY) {
				bugReporter.reportBug(stringComparison);
			}
		}
		if (refComparison != null) {
			if (false && sawCallToEquals) {
				// System.out.println("Reducing priority of " + refComparison);
				refComparison.setPriority(1 + refComparison.getPriority());
			}
			if (refComparison.getPriority() <= LOW_PRIORITY)
				bugReporter.reportBug(refComparison);
		}
	}

	private void checkRefComparison(Location location, JavaClass jclass, MethodGen methodGen,
	                                TypeDataflow typeDataflow) throws DataflowAnalysisException {

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

			if (lhs.equals("java.lang.String") && rhs.equals("java.lang.String")) {
				if (DEBUG)
					System.out.println("String/String comparison at " +
					        handle);

				// Compute the priority:
				// - two static strings => do not report
				// - dynamic string and anything => high
				// - static string and unknown => medium
				// - all other cases => low
				int priority = NORMAL_PRIORITY;
				// System.out.println("Compare " + lhsType + " == " + rhsType);
				byte type1 = lhsType.getType();
				byte type2 = rhsType.getType();
				// T1 T2 result
				// S  S  no-op
				// D  ?  high
				// ?  D  high
				// S  ?  normal
				// ?  S  normal
				if (type1 == T_STATIC_STRING && type2 == T_STATIC_STRING)
					priority = LOW_PRIORITY + 1;
				else if (type1 == T_DYNAMIC_STRING || type2 == T_DYNAMIC_STRING)
					priority = HIGH_PRIORITY;
				else if (type1 == T_STATIC_STRING || type2 == T_STATIC_STRING)
					priority = LOW_PRIORITY;
				else if (sawStringIntern)
					priority = LOW_PRIORITY;

				if (priority <= LOW_PRIORITY) {
					String sourceFile = jclass.getSourceFileName();
					BugInstance instance =
					        new BugInstance(this, "ES_COMPARING_STRINGS_WITH_EQ", priority)
					        .addClassAndMethod(methodGen, sourceFile)
					        .addSourceLine(methodGen, sourceFile, handle)
					        .addClass("java.lang.String").describe("CLASS_REFTYPE");

					if (REPORT_ALL_REF_COMPARISONS)
						bugReporter.reportBug(instance);
					else if (stringComparison == null || priority < stringComparison.getPriority())
						stringComparison = instance;
				}

			} else if (suspiciousSet.contains(lhs) && suspiciousSet.contains(rhs)) {
				String sourceFile = jclass.getSourceFileName();
				BugInstance instance = new BugInstance(this, "RC_REF_COMPARISON", NORMAL_PRIORITY)
				        .addClassAndMethod(methodGen, sourceFile)
				        .addSourceLine(methodGen, sourceFile, handle)
				        .addClass(lhs).describe("CLASS_REFTYPE");
				if (REPORT_ALL_REF_COMPARISONS)
					bugReporter.reportBug(instance);
				else if (refComparison == null)
					refComparison = instance;
			}
		}
	}

	private void checkEqualsComparison(Location location, JavaClass jclass, MethodGen methodGen,
	                                   TypeDataflow typeDataflow) throws DataflowAnalysisException {

		InstructionHandle handle = location.getHandle();
		String sourceFile = jclass.getSourceFileName();

		TypeFrame frame = typeDataflow.getFactAtLocation(location);
		if (frame.getStackDepth() < 2)
			throw new DataflowAnalysisException("Stack underflow", methodGen, handle);

		int numSlots = frame.getNumSlots();
		Type lhsType_ = frame.getValue(numSlots - 2);
		Type rhsType_ = frame.getValue(numSlots - 1);

		if (lhsType_.equals(rhsType_))
			return;

		// Ignore top and bottom values
		if (lhsType_.getType() == T_TOP || lhsType_.getType() == T_BOTTOM
		        || rhsType_.getType() == T_TOP || rhsType_.getType() == T_BOTTOM)
			return;

		if (!(lhsType_ instanceof ReferenceType) || !(rhsType_ instanceof ReferenceType)) {
			if (rhsType_.getType() == T_NULL) {
				// A literal null value was passed directly to equals().
				bugReporter.reportBug(new BugInstance(this, "EC_NULL_ARG", NORMAL_PRIORITY)
				        .addClassAndMethod(methodGen, sourceFile)
				        .addSourceLine(methodGen, sourceFile, location.getHandle()));
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
				// We have unrelated types.
				// If both types are interfaces, then it is conceivable that
				// there are class types that implement both interfaces,
				// so the comparision might be meaningful.  Classify such
				// cases as medium.  Other cases are high priority.
				if (lhsType.referencesInterfaceExact() && rhsType.referencesInterfaceExact()) {
					// TODO: This would be a good place to assume a closed
					// universe and look at subclasses.
					priority = NORMAL_PRIORITY;
					bugType = "EC_UNRELATED_INTERFACES";
				} else {
					// TODO: it is possible that an unknown subclass of
					// the class type implements the interface.
					// Again, a subclass search could answer this
					// question if we had a closed universe.
					priority = HIGH_PRIORITY;
				}
			}
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return;
		}

		if (priority <= LOW_PRIORITY) {
			bugReporter.reportBug(new BugInstance(this, bugType, priority)
			        .addClassAndMethod(methodGen, sourceFile)
			        .addSourceLine(methodGen, sourceFile, location.getHandle())
			        .addClass(lhsType.getClassName()).describe("CLASS_REFTYPE")
			        .addClass(rhsType.getClassName()).describe("CLASS_REFTYPE"));
		}
	}

	public void report() {
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + FindRefComparison.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<TypeFrame, TypeAnalysis> driver =
		        new DataflowTestDriver<TypeFrame, TypeAnalysis>() {
			        public Dataflow<TypeFrame, TypeAnalysis> createDataflow(ClassContext classContext, Method method)
			                throws CFGBuilderException, DataflowAnalysisException {

				        RepositoryLookupFailureCallback lookupFailureCallback =
				                classContext.getLookupFailureCallback();
				        MethodGen methodGen = classContext.getMethodGen(method);
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
