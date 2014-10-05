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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

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
import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.TypeAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassSummary;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.IncompatibleTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.RepositoryLookupFailureCallback;
import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.ba.TestCaseDetector;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.type.ExceptionSetFactory;
import edu.umd.cs.findbugs.ba.type.ExtendedTypes;
import edu.umd.cs.findbugs.ba.type.NullType;
import edu.umd.cs.findbugs.ba.type.StandardTypeMerger;
import edu.umd.cs.findbugs.ba.type.TypeAnalysis;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.type.TypeFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.type.TypeMerger;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.StaticConstant;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.props.WarningProperty;
import edu.umd.cs.findbugs.props.WarningPropertySet;
import edu.umd.cs.findbugs.props.WarningPropertyUtil;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Find suspicious reference comparisons. This includes:
 * <ul>
 * <li>Strings and other java.lang objects compared by reference equality</li>
 * <li>Calls to equals(Object) where the argument is a different type than the
 * receiver object</li>
 * </ul>
 *
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class FindRefComparison implements Detector, ExtendedTypes {
    private static final boolean DEBUG = SystemProperties.getBoolean("frc.debug");

    private static final boolean REPORT_ALL_REF_COMPARISONS = true /*|| SystemProperties.getBoolean("findbugs.refcomp.reportAll")*/;

    private static final int BASE_ES_PRIORITY = SystemProperties.getInt("es.basePriority", NORMAL_PRIORITY);

    /**
     * Classes that are suspicious if compared by reference.
     */
    @StaticConstant
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
        invokeInstanceSet.set(Constants.INVOKESTATIC);
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

    /*
     * ----------------------------------------------------------------------
     * Helper classes
     * ----------------------------------------------------------------------
     */

    private static final byte T_DYNAMIC_STRING = T_AVAIL_TYPE + 0;

    private static final byte T_STATIC_STRING = T_AVAIL_TYPE + 1;

    private static final byte T_PARAMETER_STRING = T_AVAIL_TYPE + 2;

    //    private static final byte T_STATIC_FINAL_PUBLIC_CONSTANT = T_AVAIL_TYPE + 3;

    private static final String STRING_SIGNATURE = "Ljava/lang/String;";

    /**
     * @author pugh
     */
    private final static class SpecialTypeAnalysis extends TypeAnalysis {

        private SpecialTypeAnalysis(Method method, MethodGen methodGen, CFG cfg, DepthFirstSearch dfs, TypeMerger typeMerger,
                TypeFrameModelingVisitor visitor, RepositoryLookupFailureCallback lookupFailureCallback,
                ExceptionSetFactory exceptionSetFactory) {
            super(method, methodGen, cfg, dfs, typeMerger, visitor, lookupFailureCallback, exceptionSetFactory);
        }

        @Override
        public void initEntryFact(TypeFrame result) {
            super.initEntryFact(result);
            for (int i = 0; i < methodGen.getMaxLocals(); i++) {
                Type t = result.getValue(i);
                if (t.equals(Type.STRING)) {
                    result.setValue(i, parameterStringTypeInstance);
                }
            }
        }
    }

    /**
     * Type representing a dynamically created String. This sort of String
     * should never be compared using reference equality.
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

    public static class FinalConstant extends ObjectType {
        private static final long serialVersionUID = 1L;

        final @Nonnull
        XField field;

        public FinalConstant(@DottedClassName String type, @Nonnull XField field) {
            super(type);
            this.field = field;
        }

        @Override
        public int hashCode() {
            return super.hashCode() * 31 + field.hashCode();

        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FinalConstant)) {
                return false;
            }
            FinalConstant other = (FinalConstant) obj;

            return super.equals(other) && this.field.equals(other.field);
        }

        public XField getXField() {
            return field;
        }

        @Override
        public String toString() {
            return super.toString() + " " + field;
        }
    }

    private static final Type dynamicStringTypeInstance = new DynamicStringType();

    /**
     * Type representing a static String. E.g., interned strings and constant
     * strings. It is generally OK to compare this sort of String using
     * reference equality.
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

    public static class EmptyStringType extends StaticStringType {
        private static final long serialVersionUID = 1L;

        public EmptyStringType() {
            super();
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
            return "<empty string>";
        }
    }

    private static final Type emptyStringTypeInstance = new EmptyStringType();


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
        private final RepositoryLookupFailureCallback lookupFailureCallback;

        private boolean sawStringIntern;

        public RefComparisonTypeFrameModelingVisitor(ConstantPoolGen cpg, TypeMerger typeMerger,
                RepositoryLookupFailureCallback lookupFailureCallback) {
            super(cpg, typeMerger);
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
            if (returnsString(obj)) {
                consumeStack(obj);

                String className = obj.getClassName(getCPG());
                if ("java.lang.String".equals(className)) {
                    pushValue(dynamicStringTypeInstance);
                } else {
                    pushReturnType(obj);
                }
            } else {
                super.visitINVOKESTATIC(obj);
            }
        }

        @Override
        public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
            if (returnsString(obj)) {
                handleInstanceMethod(obj);
            } else {
                super.visitINVOKESPECIAL(obj);
            }
        }

        @Override
        public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
            if (returnsString(obj)) {
                handleInstanceMethod(obj);
            } else {
                super.visitINVOKEINTERFACE(obj);
            }

        }

        @Override
        public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
            if (returnsString(obj)) {
                handleInstanceMethod(obj);
            } else {
                super.visitINVOKEVIRTUAL(obj);
            }
        }

        private boolean returnsString(InvokeInstruction inv) {
            String methodSig = inv.getSignature(getCPG());
            return methodSig.endsWith(")Ljava/lang/String;");
        }

        private void handleInstanceMethod(InvokeInstruction obj) {

            assert returnsString(obj);
            consumeStack(obj);
            String className = obj.getClassName(getCPG());
            String methodName = obj.getName(getCPG());
            // System.out.println(className + "." + methodName);

            if ("intern".equals(methodName) && "java.lang.String".equals(className)) {
                sawStringIntern = true;
                pushValue(staticStringTypeInstance);
            } else if ("toString".equals(methodName) || "java.lang.String".equals(className)) {
                pushValue(dynamicStringTypeInstance);
                // System.out.println("  dynamic");
            } else {
                pushReturnType(obj);
            }

        }

        @Override
        public void visitLDC(LDC obj) {
            Type type = obj.getType(getCPG());
            if (isString(type)) {
                Object value = obj.getValue(getCPG());
                if (value instanceof String && ((String)value).length() == 0) {
                    pushValue( emptyStringTypeInstance);
                } else {
                    pushValue( staticStringTypeInstance);
                }
            } else {
                pushValue(type);
            }
        }

        @Override
        public void visitLDC2_W(LDC2_W obj) {
            Type type = obj.getType(getCPG());
            pushValue(isString(type) ? staticStringTypeInstance : type);
        }

        private boolean isString(Type type) {
            return STRING_SIGNATURE.equals(type.getSignature());
        }

        @Override
        public void visitGETSTATIC(GETSTATIC obj) {
            Type type = obj.getType(getCPG());
            XField xf = XFactory.createXField(obj, cpg);
            if (xf.isFinal()) {
                FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
                Item summary = fieldSummary.getSummary(xf);
                if (summary.isNull()) {
                    pushValue(TypeFrame.getNullType());
                    return;
                }

                String slashedClassName = ClassName.fromFieldSignature(type.getSignature());
                if (slashedClassName != null) {
                    String dottedClassName = ClassName.toDottedClassName(slashedClassName);
                    if (DEFAULT_SUSPICIOUS_SET.contains(dottedClassName)) {
                        type = new FinalConstant(dottedClassName, xf);
                        consumeStack(obj);
                        pushValue(type);
                        return;
                    }
                }

            }
            if (STRING_SIGNATURE.equals(type.getSignature())) {
                handleLoad(obj);
            } else {
                super.visitGETSTATIC(obj);
            }
        }

        @Override
        public void visitGETFIELD(GETFIELD obj) {
            Type type = obj.getType(getCPG());
            if (STRING_SIGNATURE.equals(type.getSignature())) {
                handleLoad(obj);
            } else {
                XField xf = XFactory.createXField(obj, cpg);
                if (xf.isFinal()) {
                    FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
                    Item summary = fieldSummary.getSummary(xf);
                    if (summary.isNull()) {
                        consumeStack(obj);
                        pushValue(TypeFrame.getNullType());
                        return;
                    }

                    String slashedClassName = ClassName.fromFieldSignature(type.getSignature());
                    if (slashedClassName != null) {
                        String dottedClassName = ClassName.toDottedClassName(slashedClassName);
                        if (DEFAULT_SUSPICIOUS_SET.contains(dottedClassName)) {
                            type = new FinalConstant(dottedClassName, xf);
                            consumeStack(obj);
                            pushValue(type);
                            return;
                        }
                    }
                }
                super.visitGETFIELD(obj);
            }
        }

        private void handleLoad(FieldInstruction obj) {
            consumeStack(obj);

            Type type = obj.getType(getCPG());
            if (!STRING_SIGNATURE.equals(type.getSignature())) {
                throw new IllegalArgumentException("type is not String: " + type);
            }
            try {
                String className = obj.getClassName(getCPG());
                String fieldName = obj.getName(getCPG());
                Field field = Hierarchy.findField(className, fieldName);

                if (field != null) {
                    // If the field is final, we'll assume that the String value
                    // is static.
                    if (field.isFinal() && field.isFinal()) {
                        pushValue(staticStringTypeInstance);
                    } else {
                        pushValue(type);
                    }

                    return;
                }
            } catch (ClassNotFoundException ex) {
                lookupFailureCallback.reportMissingClass(ex);
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
        protected ReferenceType mergeReferenceTypes(ReferenceType aRef, ReferenceType bRef) throws DataflowAnalysisException {
            byte aType = aRef.getType();
            byte bType = bRef.getType();

            if (isExtendedStringType(aType) || isExtendedStringType(bType)) {
                // If both types are the same extended String type,
                // then the same type is returned. Otherwise, extended
                // types are downgraded to plain java.lang.String,
                // and a standard merge is applied.
                if (aType == bType) {
                    return aRef;
                }

                if (isExtendedStringType(aType)) {
                    aRef = Type.STRING;
                }
                if (isExtendedStringType(bType)) {
                    bRef = Type.STRING;
                }
            }

            return super.mergeReferenceTypes(aRef, bRef);
        }

        private boolean isExtendedStringType(byte type) {
            return type == T_DYNAMIC_STRING || type == T_STATIC_STRING || type == T_PARAMETER_STRING;
        }
    }

    /*
     * ----------------------------------------------------------------------
     * Fields
     * ----------------------------------------------------------------------
     */

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private ClassContext classContext;

    private final Set<String> suspiciousSet;

    private final boolean testingEnabled;

    /*
     * ----------------------------------------------------------------------
     * Implementation
     * ----------------------------------------------------------------------
     */

    public FindRefComparison(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
        this.suspiciousSet = new HashSet<String>(DEFAULT_SUSPICIOUS_SET);

        // Check frc.suspicious system property for additional suspicious types
        // to check
        String extraSuspiciousTypes = SystemProperties.getProperty("frc.suspicious");
        if (extraSuspiciousTypes != null) {
            StringTokenizer tok = new StringTokenizer(extraSuspiciousTypes, ",");
            while (tok.hasMoreTokens()) {
                suspiciousSet.add(tok.nextToken());
            }
        }
        testingEnabled = SystemProperties.getBoolean("report_TESTING_pattern_in_standard_detectors");
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        this.classContext = classContext;

        JavaClass jclass = classContext.getJavaClass();
        Method[] methodList = jclass.getMethods();

        for (Method method : methodList) {
            MethodGen methodGen = classContext.getMethodGen(method);
            if (methodGen == null) {
                continue;
            }

            // Prescreening - must have IF_ACMPEQ, IF_ACMPNE,
            // or an invocation of an instance method
            BitSet bytecodeSet = classContext.getBytecodeSet(method);
            if (bytecodeSet == null || !bytecodeSet.intersects(prescreenSet)) {
                continue;
            }

            if (DEBUG) {
                System.out.println("FindRefComparison: analyzing " + SignatureConverter.convertMethodSignature(methodGen));
            }

            try {
                analyzeMethod(classContext, method);
            } catch (CFGBuilderException e) {
                bugReporter.logError("Error analyzing " + method.toString(), e);
            } catch (DataflowAnalysisException e) {
                // bugReporter.logError("Error analyzing " + method.toString(),
                // e);
            }
            bugAccumulator.reportAccumulatedBugs();
        }

    }

    /**
     * A BugInstance and its WarningPropertySet.
     */
    private static class WarningWithProperties {
        final BugInstance instance;

        final SourceLineAnnotation sourceLine;

        final WarningPropertySet<WarningProperty> propertySet;

        final Location location;

        WarningWithProperties(BugInstance warning, WarningPropertySet<WarningProperty> propertySet,
                SourceLineAnnotation sourceLine, Location location) {
            this.instance = warning;
            this.propertySet = propertySet;
            this.sourceLine = sourceLine;
            this.location = location;
        }
    }

    private interface WarningDecorator {
        public void decorate(WarningWithProperties warn);
    }

    private void analyzeMethod(ClassContext classContext, final Method method) throws CFGBuilderException,
    DataflowAnalysisException {

        MethodGen methodGen = classContext.getMethodGen(method);
        if (methodGen == null) {
            return;
        }

        JavaClass jclass = classContext.getJavaClass();
        ConstantPoolGen cpg = classContext.getConstantPoolGen();

        // Enqueue all of the potential violations we find in the method.
        // Normally we'll only report the first highest-priority warning,
        // but if in relaxed mode or if REPORT_ALL_REF_COMPARISONS is set,
        // then we'll report everything.
        LinkedList<WarningWithProperties> refComparisonList = new LinkedList<WarningWithProperties>();
        LinkedList<WarningWithProperties> stringComparisonList = new LinkedList<WarningWithProperties>();


        comparedForEqualityInThisMethod = new HashMap<String,Integer>();
        CFG cfg = classContext.getCFG(method);
        DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
        ExceptionSetFactory exceptionSetFactory = classContext.getExceptionSetFactory(method);

        // Perform type analysis using our special type merger
        // (which handles String types specially, keeping track of
        // which ones appear to be dynamically created)
        RefComparisonTypeMerger typeMerger = new RefComparisonTypeMerger(bugReporter, exceptionSetFactory);
        RefComparisonTypeFrameModelingVisitor visitor = new RefComparisonTypeFrameModelingVisitor(methodGen.getConstantPool(),
                typeMerger, bugReporter);
        TypeAnalysis typeAnalysis = new SpecialTypeAnalysis(method, methodGen, cfg, dfs, typeMerger, visitor, bugReporter,
                exceptionSetFactory);
        TypeDataflow typeDataflow = new TypeDataflow(cfg, typeAnalysis);
        Profiler profiler = Global.getAnalysisCache().getProfiler();
        profiler.start(SpecialTypeAnalysis.class);
        try {
            typeDataflow.execute();
        } finally {
            profiler.end(SpecialTypeAnalysis.class);
        }

        // Inspect Locations in the method for suspicious ref comparisons and
        // calls to equals()
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();

            inspectLocation(jclass, cpg, method, methodGen, refComparisonList, stringComparisonList, visitor, typeDataflow,
                    location);
        }

        if (stringComparisonList.isEmpty() && refComparisonList.isEmpty()) {
            return;
        }
        // Add method-wide properties to BugInstances
        final boolean likelyTestcase = TestCaseDetector.likelyTestCase(XFactory.createXMethod(jclass, method));

        decorateWarnings(stringComparisonList, new WarningDecorator() {
            @Override
            public void decorate(WarningWithProperties warn) {
                if (mightBeLaterCheckedUsingEquals(warn)) {
                    warn.propertySet.addProperty(RefComparisonWarningProperty.SAW_CALL_TO_EQUALS);
                }

                if (likelyTestcase) {
                    warn.propertySet.addProperty(RefComparisonWarningProperty.COMPARE_IN_TEST_CASE);
                }
                /*
                if (false && !(method.isPublic() || method.isProtected())) {
                    warn.propertySet.addProperty(RefComparisonWarningProperty.PRIVATE_METHOD);
                }
                 */
            }
        });
        decorateWarnings(refComparisonList, new WarningDecorator() {
            @Override
            public void decorate(WarningWithProperties warn) {
                if (likelyTestcase) {
                    warn.propertySet.addProperty(RefComparisonWarningProperty.COMPARE_IN_TEST_CASE);
                }

                if (mightBeLaterCheckedUsingEquals(warn)) {
                    warn.propertySet.addProperty(RefComparisonWarningProperty.SAW_CALL_TO_EQUALS);
                }
            }
        });

        // Report violations
        boolean relaxed = FindBugsAnalysisFeatures.isRelaxedMode();
        reportBest(classContext, method, stringComparisonList, relaxed);
        reportBest(classContext, method, refComparisonList, relaxed);
    }

    boolean mightBeLaterCheckedUsingEquals(WarningWithProperties warning) {
        for (BugAnnotation a : warning.instance.getAnnotations()) {
            if (a instanceof TypeAnnotation) {
                String signature = ((TypeAnnotation) a).getTypeDescriptor();
                Integer pc = comparedForEqualityInThisMethod.get(signature);
                if (pc != null && pc > warning.location.getHandle().getPosition()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void inspectLocation(JavaClass jclass, ConstantPoolGen cpg, Method method, MethodGen methodGen,
            LinkedList<WarningWithProperties> refComparisonList, LinkedList<WarningWithProperties> stringComparisonList,
            RefComparisonTypeFrameModelingVisitor visitor, TypeDataflow typeDataflow, Location location)
                    throws DataflowAnalysisException {
        Instruction ins = location.getHandle().getInstruction();
        short opcode = ins.getOpcode();
        if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
            checkRefComparison(location, jclass, method, methodGen, visitor, typeDataflow, stringComparisonList,
                    refComparisonList);
        } else if (ins instanceof InvokeInstruction) {
            InvokeInstruction inv = (InvokeInstruction) ins;
            boolean isStatic = inv instanceof INVOKESTATIC;
            @DottedClassName String className = inv.getClassName(cpg);
            String methodName = inv.getMethodName(cpg);
            String methodSig = inv.getSignature(cpg);
            if ( "assertSame".equals(methodName) && "(Ljava/lang/Object;Ljava/lang/Object;)V".equals(methodSig)) {
                checkRefComparison(location, jclass, method, methodGen, visitor, typeDataflow, stringComparisonList,
                        refComparisonList);
            } else if ( "assertFalse".equals(methodName) && "(Z)V".equals(methodSig)) {
                SourceLineAnnotation lastLocation = bugAccumulator.getLastBugLocation();
                InstructionHandle prevHandle = location.getHandle().getPrev();
                if (lastLocation != null && prevHandle != null && lastLocation.getEndBytecode() == prevHandle.getPosition()){
                    bugAccumulator.forgetLastBug();
                    if (DEBUG) {
                        System.out.println("Forgetting last bug due to call to " + className +"." + methodName);
                    }
                }

            } else {
                boolean equalsMethod = !isStatic && "equals".equals(methodName) && "(Ljava/lang/Object;)Z".equals(methodSig)
                        || isStatic &&  "assertEquals".equals(methodName)
                        && "(Ljava/lang/Object;Ljava/lang/Object;)V".equals(methodSig)
                        || isStatic && "equal".equals(methodName) && "(Ljava/lang/Object;Ljava/lang/Object;)Z".equals(methodSig)
                        && "com.google.common.base.Objects".equals(className)
                        || isStatic && "equals".equals(methodName) && "(Ljava/lang/Object;Ljava/lang/Object;)Z".equals(methodSig)
                        && "java.util.Objects".equals(className);

                if (equalsMethod) {
                    checkEqualsComparison(location, jclass, method, methodGen, cpg, typeDataflow);
                }
            }
        }

    }

    private void decorateWarnings(LinkedList<WarningWithProperties> stringComparisonList, WarningDecorator warningDecorator) {
        for (WarningWithProperties warn : stringComparisonList) {
            warningDecorator.decorate(warn);
            warn.propertySet.decorateBugInstance(warn.instance);
        }
    }

    private void reportBest(ClassContext classContext, Method method, LinkedList<WarningWithProperties> warningList,
            boolean relaxed) {
        boolean reportAll = relaxed || REPORT_ALL_REF_COMPARISONS;

        int bestPriority = Integer.MAX_VALUE;
        for (WarningWithProperties warn : warningList) {
            int priority = warn.instance.getPriority();
            if (bestPriority > priority) {
                bestPriority = priority;
            }

            if (reportAll) {
                if (relaxed) {
                    // Add general warning properties
                    WarningPropertyUtil.addPropertiesForDataMining(warn.propertySet, classContext, method, warn.location);

                    // Convert warning properties to bug properties
                    warn.propertySet.decorateBugInstance(warn.instance);
                }
                bugAccumulator.accumulateBug(warn.instance, warn.sourceLine);
            }

        }
        if (!reportAll) {
            for (WarningWithProperties warn : warningList) {
                int priority = warn.instance.getPriority();
                if (priority <= bestPriority) {
                    bugAccumulator.accumulateBug(warn.instance, warn.sourceLine);
                }
            }
        }
    }

    private void checkRefComparison(Location location, JavaClass jclass, Method method, MethodGen methodGen,
            RefComparisonTypeFrameModelingVisitor visitor, TypeDataflow typeDataflow,
            List<WarningWithProperties> stringComparisonList, List<WarningWithProperties> refComparisonList)
                    throws DataflowAnalysisException {

        InstructionHandle handle = location.getHandle();

        TypeFrame frame = typeDataflow.getFactAtLocation(location);
        if (frame.getStackDepth() < 2) {
            throw new DataflowAnalysisException("Stack underflow", methodGen, handle);
        }

        int numSlots = frame.getNumSlots();
        Type lhsType = frame.getValue(numSlots - 2);
        Type rhsType = frame.getValue(numSlots - 1);

        if (lhsType instanceof NullType || rhsType instanceof NullType) {
            return;
        }
        if (lhsType instanceof ReferenceType && rhsType instanceof ReferenceType) {
            IncompatibleTypes result = IncompatibleTypes.getPriorityForAssumingCompatible(lhsType, rhsType, true);
            if (result != IncompatibleTypes.SEEMS_OK && result != IncompatibleTypes.UNCHECKED) {
                String sourceFile = jclass.getSourceFileName();

                boolean isAssertSame = handle.getInstruction() instanceof INVOKESTATIC;
                if (isAssertSame) {
                    if(testingEnabled) {
                        bugAccumulator.accumulateBug(
                                new BugInstance(this, "TESTING", result.getPriority())
                                .addClassAndMethod(methodGen, sourceFile)
                                .addString("Calling assertSame with two distinct objects")
                                .addFoundAndExpectedType(rhsType, lhsType)
                                .addSomeSourceForTopTwoStackValues(classContext, method, location),
                                SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, handle));
                    }
                } else {
                    bugAccumulator.accumulateBug(
                            new BugInstance(this, "EC_UNRELATED_TYPES_USING_POINTER_EQUALITY", result.getPriority())
                            .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(rhsType, lhsType)
                            .addSomeSourceForTopTwoStackValues(classContext, method, location),
                            SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen, sourceFile, handle));
                }
                return;
            }
            if (lhsType.equals(Type.OBJECT) && rhsType.equals(Type.OBJECT)) {
                return;
            }
            String lhs = SignatureConverter.convert(lhsType.getSignature());
            String rhs = SignatureConverter.convert(rhsType.getSignature());

            if ("java.lang.String".equals(lhs) || "java.lang.String".equals(rhs)) {
                handleStringComparison(jclass, method, methodGen, visitor, stringComparisonList, location, lhsType, rhsType);
            } else if (suspiciousSet.contains(lhs)) {
                handleSuspiciousRefComparison(jclass, method, methodGen, refComparisonList, location, lhs,
                        (ReferenceType) lhsType, (ReferenceType) rhsType);
            } else if (suspiciousSet.contains(rhs)) {
                handleSuspiciousRefComparison(jclass, method, methodGen, refComparisonList, location, rhs,
                        (ReferenceType) lhsType, (ReferenceType) rhsType);
            }
        }
    }

    private void handleStringComparison(JavaClass jclass, Method method, MethodGen methodGen,
            RefComparisonTypeFrameModelingVisitor visitor, List<WarningWithProperties> stringComparisonList, Location location,
            Type lhsType, Type rhsType) {
        if (DEBUG) {
            System.out.println("String/String comparison at " + location.getHandle());
        }

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
        // S S no-op
        // D ? high
        // ? D high
        // S ? normal
        // ? S normal
        WarningPropertySet<WarningProperty> propertySet = new WarningPropertySet<WarningProperty>();
        if (type1 == T_STATIC_STRING && type2 == T_STATIC_STRING) {
            propertySet.addProperty(RefComparisonWarningProperty.COMPARE_STATIC_STRINGS);
        } else if (type1 == T_DYNAMIC_STRING || type2 == T_DYNAMIC_STRING) {
            propertySet.addProperty(RefComparisonWarningProperty.DYNAMIC_AND_UNKNOWN);
        } else if (type2 == T_PARAMETER_STRING || type1 == T_PARAMETER_STRING) {
            bugPattern = "ES_COMPARING_PARAMETER_STRING_WITH_EQ";
            if (methodGen.isPublic() || methodGen.isProtected()) {
                propertySet.addProperty(RefComparisonWarningProperty.STRING_PARAMETER_IN_PUBLIC_METHOD);
            } else {
                propertySet.addProperty(RefComparisonWarningProperty.STRING_PARAMETER);
            }
        } else if (type1 == T_STATIC_STRING || type2 == T_STATIC_STRING) {
            if (lhsType instanceof EmptyStringType || rhsType instanceof EmptyStringType) {
                propertySet.addProperty(RefComparisonWarningProperty.EMPTY_AND_UNKNOWN);
            } else {
                propertySet.addProperty(RefComparisonWarningProperty.STATIC_AND_UNKNOWN);
            }
        } else if (visitor.sawStringIntern()) {
            propertySet.addProperty(RefComparisonWarningProperty.SAW_INTERN);
        }

        String sourceFile = jclass.getSourceFileName();
        BugInstance instance = new BugInstance(this, bugPattern, BASE_ES_PRIORITY).addClassAndMethod(methodGen, sourceFile)
                .addType("Ljava/lang/String;").describe(TypeAnnotation.FOUND_ROLE).addSomeSourceForTopTwoStackValues(classContext, method, location);
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                sourceFile, location.getHandle());

        WarningWithProperties warn = new WarningWithProperties(instance, propertySet, sourceLineAnnotation, location);
        stringComparisonList.add(warn);
    }

    private void handleSuspiciousRefComparison(JavaClass jclass, Method method, MethodGen methodGen,
            List<WarningWithProperties> refComparisonList, Location location, String lhs, ReferenceType lhsType,
            ReferenceType rhsType) {
        XField xf = null;
        if (lhsType instanceof FinalConstant) {
            xf = ((FinalConstant) lhsType).getXField();
        } else if (rhsType instanceof FinalConstant) {
            xf = ((FinalConstant) rhsType).getXField();
        }
        String sourceFile = jclass.getSourceFileName();
        String bugPattern = "RC_REF_COMPARISON";
        int priority = Priorities.HIGH_PRIORITY;
        if ("java.lang.Boolean".equals(lhs)) {
            bugPattern = "RC_REF_COMPARISON_BAD_PRACTICE_BOOLEAN";
            priority = Priorities.NORMAL_PRIORITY;
        } else if (xf != null && xf.isStatic() && xf.isFinal()) {
            bugPattern = "RC_REF_COMPARISON_BAD_PRACTICE";
            if (xf.isPublic() || !methodGen.isPublic()) {
                priority = Priorities.NORMAL_PRIORITY;
            }
        }
        BugInstance instance = new BugInstance(this, bugPattern, priority).addClassAndMethod(methodGen, sourceFile)
                .addType("L" + lhs.replace('.', '/') + ";").describe(TypeAnnotation.FOUND_ROLE);
        if (xf != null) {
            instance.addField(xf).describe(FieldAnnotation.LOADED_FROM_ROLE);
        } else {
            instance.addSomeSourceForTopTwoStackValues(classContext, method, location);
        }
        SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, methodGen,
                sourceFile, location.getHandle());

        refComparisonList.add(new WarningWithProperties(instance, new WarningPropertySet<WarningProperty>(),
                sourceLineAnnotation, location));
    }

    private Map<String, Integer> comparedForEqualityInThisMethod;

    void addEqualsCheck(String type, int pc) {
        Integer oldPC = comparedForEqualityInThisMethod.get(type);
        if (oldPC == null || pc < oldPC) {
            comparedForEqualityInThisMethod.put(type, pc);
        }
    }

    private void checkEqualsComparison(Location location, JavaClass jclass, Method method, MethodGen methodGen,
            ConstantPoolGen cpg, TypeDataflow typeDataflow) throws DataflowAnalysisException {

        InstructionHandle handle = location.getHandle();
        InstructionHandle next = handle.getNext();
        if (next != null && next.getInstruction() instanceof INVOKESTATIC) {
            INVOKESTATIC is = (INVOKESTATIC) next.getInstruction();
            if ("assertFalse".equals(is.getMethodName(cpg))) {
                return;
            }
        }
        String sourceFile = jclass.getSourceFileName();

        TypeFrame frame = typeDataflow.getFactAtLocation(location);
        if (frame.getStackDepth() < 2) {
            throw new DataflowAnalysisException("Stack underflow", methodGen, handle);
        }

        int numSlots = frame.getNumSlots();
        Type lhsType_ = frame.getValue(numSlots - 2);
        Type rhsType_ = frame.getValue(numSlots - 1);

        // Ignore top and bottom values
        if (lhsType_.getType() == T_TOP || lhsType_.getType() == T_BOTTOM || rhsType_.getType() == T_TOP
                || rhsType_.getType() == T_BOTTOM) {
            return;
        }
        InvokeInstruction inv = (InvokeInstruction) handle.getInstruction();
        MethodAnnotation calledMethodAnnotation = getMethodCalledAnnotation(cpg, inv);
        boolean looksLikeTestCase = TestCaseDetector.likelyTestCase(XFactory.createXMethod(methodGen));
        int priorityModifier = 0;
        if (looksLikeTestCase) {
            priorityModifier = 1;
        }

        if (rhsType_.getType() == T_NULL) {
            // A literal null value was passed directly to equals().
            if (!looksLikeTestCase) {

                try {
                    IsNullValueDataflow isNullDataflow = classContext.getIsNullValueDataflow(method);
                    IsNullValueFrame isNullFrame = isNullDataflow.getFactAtLocation(location);
                    BugAnnotation a = BugInstance.getSourceForTopStackValue(classContext, method, location);
                    int priority = NORMAL_PRIORITY;
                    if (a instanceof FieldAnnotation && ((FieldAnnotation) a).isStatic()) {
                        priority = LOW_PRIORITY;
                    }
                    if (isNullFrame.isValid() && isNullFrame.getTopValue().isDefinitelyNull()) {
                        String type = "EC_NULL_ARG";
                        if (calledMethodAnnotation != null && calledMethodAnnotation.isStatic()){
                            type = "DMI_DOH";
                            priority = LOW_PRIORITY;
                        }
                        BugInstance bug = new BugInstance(this, type, priority + priorityModifier).addClassAndMethod(methodGen, sourceFile)
                                .addOptionalAnnotation(calledMethodAnnotation);
                        if ("DMI_DOH".equals(type)) {
                            bug.addString("Use \"== null\" to check for a value being null");
                        }
                        bugAccumulator.accumulateBug(
                                bug,
                                SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile,
                                        location.getHandle()));
                    }
                } catch (CFGBuilderException e) {
                    AnalysisContext.logError("Error getting null value analysis", e);
                }

            }
            return;
        } else if (lhsType_.getType() == T_NULL) {
            // Hmm...in this case, equals() is being invoked on
            // a literal null value. This is really the
            // purview of FindNullDeref. So, we'll just do nothing.
            return;
        } else if (!(lhsType_ instanceof ReferenceType) || !(rhsType_ instanceof ReferenceType)) {
            bugReporter.logError("equals() used to compare non-object type(s) " + lhsType_ + " and " + rhsType_ + " in "
                    + SignatureConverter.convertMethodSignature(methodGen) + " at " + location.getHandle());
            return;
        }
        IncompatibleTypes result = IncompatibleTypes.getPriorityForAssumingCompatible(lhsType_, rhsType_);

        if (lhsType_ instanceof ArrayType && rhsType_ instanceof ArrayType) {
            String pattern = "EC_BAD_ARRAY_COMPARE";
            IncompatibleTypes result2 = IncompatibleTypes.getPriorityForAssumingCompatible(lhsType_, rhsType_, true);
            if (result2.getPriority() <= Priorities.NORMAL_PRIORITY) {
                pattern = "EC_INCOMPATIBLE_ARRAY_COMPARE";
            } else if (calledMethodAnnotation != null && "org.testng.Assert".equals(calledMethodAnnotation.getClassName())) {
                return;
            }
            bugAccumulator.accumulateBug(new BugInstance(this, pattern, NORMAL_PRIORITY).addClassAndMethod(methodGen, sourceFile)
                    .addFoundAndExpectedType(rhsType_, lhsType_)
                    .addSomeSourceForTopTwoStackValues(classContext, method, location)
                    .addOptionalAnnotation(calledMethodAnnotation, MethodAnnotation.METHOD_CALLED),
                    SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle()));
            return;
        }

        if (result.getPriority() >= Priorities.LOW_PRIORITY) {
            addEqualsCheck(lhsType_.getSignature(), handle.getPosition());
            addEqualsCheck(rhsType_.getSignature(), handle.getPosition());
        }

        if (result == IncompatibleTypes.SEEMS_OK) {
            return;
        }


        if (result.getPriority() > Priorities.LOW_PRIORITY) {
            return;
        }

        if (result == IncompatibleTypes.ARRAY_AND_NON_ARRAY || result == IncompatibleTypes.ARRAY_AND_OBJECT) {
            String lhsSig = lhsType_.getSignature();
            String rhsSig = rhsType_.getSignature();
            boolean allOk = checkForWeirdEquals(lhsSig, rhsSig, new HashSet<XMethod>());
            if (allOk) {
                priorityModifier += 2;
            }
            bugAccumulator.accumulateBug(new BugInstance(this, "EC_ARRAY_AND_NONARRAY", result.getPriority() + priorityModifier)
            .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(rhsType_, lhsType_)
            .addSomeSourceForTopTwoStackValues(classContext, method, location)
            .addOptionalAnnotation(calledMethodAnnotation, MethodAnnotation.METHOD_CALLED),
            SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle()));
        } else if (result == IncompatibleTypes.INCOMPATIBLE_CLASSES) {
            String lhsSig = lhsType_.getSignature();
            String rhsSig = rhsType_.getSignature();
            boolean core = lhsSig.startsWith("Ljava") && rhsSig.startsWith("Ljava");
            if (core) {
                looksLikeTestCase = false;
                priorityModifier = 0;
            }
            if (true) {
                Set<XMethod> targets = new HashSet<XMethod>();
                boolean allOk = checkForWeirdEquals(lhsSig, rhsSig, targets);
                if (allOk) {
                    priorityModifier += 2;
                }

                int priority = result.getPriority() + priorityModifier;
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "EC_UNRELATED_TYPES", priority)
                        .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(rhsType_, lhsType_)
                        .addSomeSourceForTopTwoStackValues(classContext, method, location).addEqualsMethodUsed(targets)
                        .addOptionalAnnotation(calledMethodAnnotation, MethodAnnotation.METHOD_CALLED),
                        SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile,
                                location.getHandle()));
            }
        } else if (result == IncompatibleTypes.UNRELATED_CLASS_AND_INTERFACE
                || result == IncompatibleTypes.UNRELATED_FINAL_CLASS_AND_INTERFACE) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "EC_UNRELATED_CLASS_AND_INTERFACE", result.getPriority() + priorityModifier)
                    .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(rhsType_, lhsType_)
                    .addSomeSourceForTopTwoStackValues(classContext, method, location)
                    .addEqualsMethodUsed(DescriptorFactory.createClassDescriptorFromSignature(lhsType_.getSignature()))
                    .addOptionalAnnotation(calledMethodAnnotation, MethodAnnotation.METHOD_CALLED),
                    SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle()));
        } else if (result == IncompatibleTypes.UNRELATED_INTERFACES) {
            bugAccumulator.accumulateBug(
                    new BugInstance(this, "EC_UNRELATED_INTERFACES", result.getPriority() + priorityModifier)
                    .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(rhsType_, lhsType_)
                    .addSomeSourceForTopTwoStackValues(classContext, method, location)
                    .addEqualsMethodUsed(DescriptorFactory.createClassDescriptorFromSignature(lhsType_.getSignature()))
                    .addOptionalAnnotation(calledMethodAnnotation, MethodAnnotation.METHOD_CALLED),
                    SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle()));
        } else if (result != IncompatibleTypes.UNCHECKED && result.getPriority() <= Priorities.LOW_PRIORITY) {
            bugAccumulator.accumulateBug(new BugInstance(this, "EC_UNRELATED_TYPES", result.getPriority() + priorityModifier)
            .addClassAndMethod(methodGen, sourceFile).addFoundAndExpectedType(rhsType_, lhsType_)
            .addSomeSourceForTopTwoStackValues(classContext, method, location)
            .addOptionalAnnotation(calledMethodAnnotation, MethodAnnotation.METHOD_CALLED),
            SourceLineAnnotation.fromVisitedInstruction(this.classContext, methodGen, sourceFile, location.getHandle()));
        }

    }

    public @CheckForNull MethodAnnotation getMethodCalledAnnotation(ConstantPoolGen cpg, InvokeInstruction inv) {
        MethodDescriptor invokedMethod = getInvokedMethod(cpg, inv);
        boolean standardEquals = "equals".equals(invokedMethod.getName())
                && "(Ljava/lang/Object;)Z".equals(invokedMethod.getSignature()) && !invokedMethod.isStatic();
        return standardEquals ? null : MethodAnnotation.fromMethodDescriptor(invokedMethod);
    }

    public MethodDescriptor getInvokedMethod(ConstantPoolGen cpg, InvokeInstruction inv) {
        String invoked = inv.getClassName(cpg);
        String methodName = inv.getMethodName(cpg);
        String methodSig = inv.getSignature(cpg);
        MethodDescriptor invokedMethod =
                DescriptorFactory.instance().getMethodDescriptor(ClassName.toSlashedClassName(invoked), methodName, methodSig, inv instanceof INVOKESTATIC);
        return invokedMethod;
    }

    private boolean checkForWeirdEquals(String lhsSig, String rhsSig, Set<XMethod> targets) {
        boolean allOk = false;
        try {
            ClassSummary classSummary = AnalysisContext.currentAnalysisContext().getClassSummary();

            ClassDescriptor expectedClassDescriptor = DescriptorFactory.createClassDescriptorFromSignature(lhsSig);
            ClassDescriptor actualClassDescriptor = DescriptorFactory.createClassDescriptorFromSignature(rhsSig);

            targets.addAll(Hierarchy2.resolveVirtualMethodCallTargets(expectedClassDescriptor, "equals", "(Ljava/lang/Object;)Z",
                    false, false));
            allOk = targets.size() > 0;
            for (XMethod m2 : targets) {
                if (!classSummary.mightBeEqualTo(m2.getClassDescriptor(), actualClassDescriptor)) {
                    allOk = false;
                }
            }

        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return allOk;
    }

    @Override
    public void report() {
        // do nothing
    }
}

