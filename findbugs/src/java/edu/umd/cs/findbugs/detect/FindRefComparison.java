/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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
import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class FindRefComparison implements Detector, ExtendedTypes {
	private static final boolean DEBUG = Boolean.getBoolean("frc.debug");

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

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	private static final byte T_DYNAMIC_STRING = T_AVAIL_TYPE + 0;
	private static final byte T_STATIC_STRING  = T_AVAIL_TYPE + 1;

	private static final String STRING_SIGNATURE = "Ljava/lang/String;";

	/**
	 * Type representing a dynamically created String.
	 * This sort of String should never be compared using reference
	 * equality.
	 */
	private static class DynamicStringType extends ReferenceType {
		public DynamicStringType() {
			super(T_DYNAMIC_STRING, STRING_SIGNATURE);
		}
		public int hashCode() { return System.identityHashCode(this); }
		public boolean equals(Object o) { return o == this; }
		public String toString() { return "<dynamic string>"; }
	}

	private static final Type dynamicStringTypeInstance = new DynamicStringType();

	/**
	 * Type representing a static String.
	 * E.g., interned strings and constant strings.
	 * It is generally OK to compare this sort of String
	 * using reference equality.
	 */
	private static class StaticStringType extends ReferenceType {
		public StaticStringType() {
			super(T_STATIC_STRING, STRING_SIGNATURE);
		}
		public int hashCode() { return System.identityHashCode(this); }
		public boolean equals(Object o) { return o == this; }
		public String toString() { return "<static string>"; }
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

		public void visitINVOKESTATIC(INVOKESTATIC obj)	{
			consumeStack(obj);
			if (returnsString(obj)) {
				String className = obj.getClassName(getCPG());
				String methodName = obj.getName(getCPG());
				if (className.equals("java.lang.String") && methodName.equals("valueOf")) {
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

				if (methodName.equals("toString"))
					pushValue(dynamicStringTypeInstance);
				else if (methodName.equals("intern") && className.equals("java.lang.String"))
					pushValue(staticStringTypeInstance);
				else
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
					Field field = edu.umd.cs.findbugs.ba.Lookup.findField(className, fieldName);

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
		public RefComparisonTypeMerger(RepositoryLookupFailureCallback lookupFailureCallback) {
			super(lookupFailureCallback);
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
	private BugInstance stringComparison; 

	/* ----------------------------------------------------------------------
	 * Implementation
	 * ---------------------------------------------------------------------- */

	public FindRefComparison(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		try {

			final JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();

			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				final MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					continue;

				// Prescreening - must have IF_ACMPEQ or IF_ACMPNE
				BitSet bytecodeSet = classContext.getBytecodeSet(method);
				if (!(bytecodeSet.get(Constants.IF_ACMPEQ) || bytecodeSet.get(Constants.IF_ACMPNE)))
					continue;

				if (DEBUG) System.out.println("FindRefComparison: analyzing " +
					SignatureConverter.convertMethodSignature(methodGen));

				// Report at most one String comparison per method.
				// We report the first highest priority warning.
				stringComparison = null;

				final CFG cfg = classContext.getCFG(method);
				final DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);

				RefComparisonTypeMerger typeMerger = new RefComparisonTypeMerger(bugReporter);
				TypeFrameModelingVisitor visitor = new RefComparisonTypeFrameModelingVisitor(methodGen.getConstantPool(), bugReporter);
				TypeAnalysis typeAnalysis = new TypeAnalysis(methodGen, dfs, typeMerger, visitor);
				final TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
				typeDataflow.execute();

				new LocationScanner(cfg).scan(new LocationScanner.Callback() {
					public void visitLocation(Location location) {
						try {
							InstructionHandle handle = location.getHandle();
							Instruction ins = handle.getInstruction();
							short opcode = ins.getOpcode();
							if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
								TypeFrame frame = typeDataflow.getFactAtLocation(location);
								if (frame.getStackDepth() < 2)
									throw new AnalysisException("Stack underflow", methodGen, handle);
								int numSlots = frame.getNumSlots();
								Type lhsType = frame.getValue(numSlots - 1);
								Type rhsType = frame.getValue(numSlots - 2);
	
								if (lhsType instanceof ReferenceType && rhsType instanceof ReferenceType) {
									String lhs = SignatureConverter.convert(lhsType.getSignature());
									String rhs = SignatureConverter.convert(rhsType.getSignature());

									if (!lhs.equals(rhs))
										return;
	
									if (lhs.equals("java.lang.String") && rhs.equals("java.lang.String")) {
										//System.out.println("String/String comparison!");

										// Compute the priority:
										// - two static strings => do not report
										// - dynamic string and anything => high
										// - static string and unknown => medium
										// - all other cases => low
										int priority = LOW_PRIORITY;
										byte type1 = lhsType.getType();
										byte type2 = rhsType.getType();
										if (type1 == T_STATIC_STRING && type2 == T_STATIC_STRING)
											priority = LOW_PRIORITY + 1;
										else if (type1 == T_DYNAMIC_STRING || type2 == T_DYNAMIC_STRING)
											priority = HIGH_PRIORITY;
										else if (type1 == T_STATIC_STRING || type2 == T_STATIC_STRING)
											priority = NORMAL_PRIORITY;

										if (priority <= LOW_PRIORITY) {
											String sourceFile = jclass.getSourceFileName();
											BugInstance instance = 
												new BugInstance("ES_COMPARING_STRINGS_WITH_EQ", priority)
													.addClassAndMethod(methodGen, sourceFile)
													.addSourceLine(methodGen, sourceFile, handle)
													.addClass("java.lang.String").describe("CLASS_REFTYPE");

											if (stringComparison == null || priority < stringComparison.getPriority())
												stringComparison = instance;
										}
	
									} else if (suspiciousSet.contains(lhs) && suspiciousSet.contains(rhs)) {
										String sourceFile = jclass.getSourceFileName();
										bugReporter.reportBug(new BugInstance("RC_REF_COMPARISON", NORMAL_PRIORITY)
											.addClassAndMethod(methodGen, sourceFile)
											.addSourceLine(methodGen, sourceFile, handle)
											.addClass(lhs).describe("CLASS_REFTYPE")
										);
									}
								}
							}
						} catch (DataflowAnalysisException e) {
							throw new AnalysisException("Caught exception: " + e.toString(), e);
						}
					}
				});

				// If a String reference comparison was found in the method,
				// report it
				if (stringComparison != null)
					bugReporter.reportBug(stringComparison);
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("Exception in FindRefComparison: " + e.getMessage(), e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException("Exception in FindRefComparison: " + e.getMessage(), e);
		}
	}

	public void report() {
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + FindRefComparison.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<TypeFrame, TypeAnalysis> driver = new DataflowTestDriver<TypeFrame, TypeAnalysis>() {
			public Dataflow<TypeFrame, TypeAnalysis> createDataflow(ClassContext classContext, Method method)
				throws CFGBuilderException, DataflowAnalysisException {

				RepositoryLookupFailureCallback lookupFailureCallback = classContext.getLookupFailureCallback();
				MethodGen methodGen = classContext.getMethodGen(method);
				DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
				CFG cfg = classContext.getCFG(method);

				TypeMerger typeMerger = new RefComparisonTypeMerger(lookupFailureCallback);
				TypeFrameModelingVisitor visitor =
					new RefComparisonTypeFrameModelingVisitor(methodGen.getConstantPool(), lookupFailureCallback);
				TypeAnalysis analysis = new TypeAnalysis(methodGen, dfs, typeMerger, visitor);
				Dataflow<TypeFrame, TypeAnalysis> dataflow = new Dataflow<TypeFrame, TypeAnalysis>(cfg, analysis);
				dataflow.execute();

				return dataflow;
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=4
