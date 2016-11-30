/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba.type;

import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTypeTable;
import org.apache.bcel.generic.*;

import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.InvalidBytecodeException;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.generic.GenericObjectType;
import edu.umd.cs.findbugs.ba.generic.GenericSignatureParser;
import edu.umd.cs.findbugs.ba.generic.GenericUtilities;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.util.Util;

/**
 * Visitor to model the effects of bytecode instructions on the types of the
 * values (local and operand stack) in Java stack frames. This visitor does not
 * verify that the types are sensible for the bytecodes executed. In other
 * words, this isn't a bytecode verifier, although it wouldn't be too hard to
 * turn it into something vaguely verifier-like.
 *
 * @author David Hovemeyer
 * @see TypeFrame
 * @see TypeAnalysis
 */
public class TypeFrameModelingVisitor extends AbstractFrameModelingVisitor<Type, TypeFrame> implements Constants, Debug {

    private ValueNumberDataflow valueNumberDataflow;

    // Fields for precise modeling of instanceof instructions.
    private boolean instanceOfFollowedByBranch;

    private ReferenceType instanceOfType;

    private ValueNumber instanceOfValueNumber;

    private final Set<ReferenceType> typesComputedFromGenerics = Util.newSetFromMap(new IdentityHashMap<ReferenceType, Boolean>());

    protected final TypeMerger typeMerger;

    protected LocalVariableTypeTable localTypeTable;

    protected BitSet genericLocalVariables;


    /**
     * Constructor.
     *
     * @param cpg
     *            the ConstantPoolGen of the method whose instructions we are
     *            examining
     * @param typeMerger
     *            TODO
     */
    public TypeFrameModelingVisitor(ConstantPoolGen cpg, TypeMerger typeMerger) {
        super(cpg);
        this.typeMerger = typeMerger;

    }

    /**
     * Set ValueNumberDataflow for the method being analyzed. This is optional;
     * if set, we will use the information to more accurately model the effects
     * of instanceof instructions.
     *
     * @param valueNumberDataflow
     *            the ValueNumberDataflow
     */
    public void setValueNumberDataflow(ValueNumberDataflow valueNumberDataflow) {
        this.valueNumberDataflow = valueNumberDataflow;
    }

    public void setLocalTypeTable(LocalVariableTypeTable localTypeTable) {
        this.localTypeTable = localTypeTable;
        if (localTypeTable == null) {
            genericLocalVariables = null;
        } else {
            genericLocalVariables = new BitSet();
            for(LocalVariable lv : localTypeTable.getLocalVariableTypeTable()) {
                if (lv.getSignature().indexOf('<') > 0) {
                    genericLocalVariables.set(lv.getIndex());
                }

            }
        }


    }

    /**
     * Return whether an instanceof instruction was followed by a branch. The
     * TypeAnalysis may use this to get more precise types in the resulting
     * frame.
     *
     * @return true if an instanceof instruction was followed by a branch, false
     *         if not
     */
    public boolean isInstanceOfFollowedByBranch() {
        return instanceOfFollowedByBranch;
    }

    /**
     * Get the type of the most recent instanceof instruction modeled. The
     * TypeAnalysis may use this to get more precise types in the resulting
     * frame.
     *
     * @return the Type checked by the most recent instanceof instruction
     */
    public Type getInstanceOfType() {
        return instanceOfType;
    }

    /**
     * Get the value number of the most recent instanceof instruction modeled.
     * The TypeAnalysis may use this to get more precise types in the resulting
     * frame.
     *
     * @return the ValueNumber checked by the most recent instanceof instruction
     */
    public ValueNumber getInstanceOfValueNumber() {
        return instanceOfValueNumber;
    }

    /**
     * Set the field store type database. We can use this to get more accurate
     * types for values loaded from fields.
     *
     * @param database
     *            the FieldStoreTypeDatabase
     */
    public void setFieldStoreTypeDatabase(FieldStoreTypeDatabase database) {

    }

    @Override
    public Type getDefaultValue() {
        return TypeFrame.getBottomType();
    }

    boolean sawEffectiveInstanceOf;

    boolean previousWasEffectiveInstanceOf;

    @Override
    public void analyzeInstruction(Instruction ins) throws DataflowAnalysisException {
        instanceOfFollowedByBranch = false;
        sawEffectiveInstanceOf = false;
        super.analyzeInstruction(ins);
        previousWasEffectiveInstanceOf = sawEffectiveInstanceOf;
    }

    /**
     * This method must be called at the beginning of modeling a basic block in
     * order to clear information cached for instanceof modeling.
     */
    public void startBasicBlock() {
        instanceOfType = null;
        instanceOfValueNumber = null;
    }

    /**
     * Consume stack. This is a convenience method for instructions where the
     * types of popped operands can be ignored.
     */
    protected void consumeStack(Instruction ins) {
        ConstantPoolGen cpg = getCPG();
        TypeFrame frame = getFrame();

        int numWordsConsumed = ins.consumeStack(cpg);
        if (numWordsConsumed == Constants.UNPREDICTABLE) {
            throw new InvalidBytecodeException("Unpredictable stack consumption for " + ins);
        }
        if (numWordsConsumed > frame.getStackDepth()) {
            throw new InvalidBytecodeException("Stack underflow for " + ins + ", " + numWordsConsumed + " needed, " + frame.getStackDepth() + " avail, frame is " + frame);
        }
        try {
            while (numWordsConsumed-- > 0) {
                frame.popValue();
            }
        } catch (DataflowAnalysisException e) {
            throw new InvalidBytecodeException("Stack underflow for " + ins + ": " + e.getMessage());
        }
    }

    /**
     * Work around some weirdness in BCEL (inherited from JVM Spec 1): BCEL
     * considers long and double types to consume two slots on the stack. This
     * method ensures that we push two types for each double or long value.
     */
    protected void pushValue(Type type) {
        if (type.getType() == T_VOID) {
            throw new IllegalArgumentException("Can't push void");
        }
        TypeFrame frame = getFrame();
        if (type.getType() == T_LONG) {
            frame.pushValue(Type.LONG);
            frame.pushValue(TypeFrame.getLongExtraType());
        } else if (type.getType() == T_DOUBLE) {
            frame.pushValue(Type.DOUBLE);
            frame.pushValue(TypeFrame.getDoubleExtraType());
        } else {
            frame.pushValue(type);
        }
    }

    /**
     * Helper for pushing the return type of an invoke instruction.
     */
    protected void pushReturnType(InvokeInstruction ins) {
        ConstantPoolGen cpg = getCPG();
        Type type = ins.getType(cpg);
        if (type.getType() != T_VOID) {
            pushValue(type);
        }
    }

    /**
     * This is overridden only to ensure that we don't rely on the base class to
     * handle instructions that produce stack operands.
     */
    @Override
    public void modelNormalInstruction(Instruction ins, int numWordsConsumed, int numWordsProduced) {
        if (VERIFY_INTEGRITY) {
            if (numWordsProduced > 0) {
                throw new InvalidBytecodeException("missing visitor method for " + ins);
            }
        }
        super.modelNormalInstruction(ins, numWordsConsumed, numWordsProduced);
    }

    // ----------------------------------------------------------------------
    // Instruction visitor methods
    // ----------------------------------------------------------------------

    // NOTES:
    // - Instructions that only consume operands need not be overridden,
    // because the base class visit methods handle them correctly.
    // - Instructions that simply move values around in the frame,
    // such as DUP, xLOAD, etc., do not need to be overridden because
    // the base class handles them.
    // - Instructions that consume and produce should call
    // consumeStack(Instruction) and then explicitly push produced operands.

    @Override
    public void visitATHROW(ATHROW obj) {
        // do nothing. The same value remains on the stack (but we jump to a new
        // location)
    }

    @Override
    public void visitACONST_NULL(ACONST_NULL obj) {
        pushValue(TypeFrame.getNullType());
    }

    @Override
    public void visitDCONST(DCONST obj) {
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitFCONST(FCONST obj) {
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitICONST(ICONST obj) {
        pushValue(Type.INT);
    }

    @Override
    public void visitLCONST(LCONST obj) {
        pushValue(Type.LONG);
    }

    @Override
    public void visitLDC(LDC obj) {
        pushValue(obj.getType(getCPG()));
    }

    @Override
    public void visitLDC2_W(LDC2_W obj) {
        pushValue(obj.getType(getCPG()));
    }

    @Override
    public void visitBIPUSH(BIPUSH obj) {
        pushValue(Type.INT);
    }

    @Override
    public void visitSIPUSH(SIPUSH obj) {
        pushValue(Type.INT);
    }

    @Override
    public void visitGETSTATIC(GETSTATIC obj) {
        modelFieldLoad(obj);
    }

    @Override
    public void visitGETFIELD(GETFIELD obj) {
        modelFieldLoad(obj);
    }

    public void modelFieldLoad(FieldInstruction obj) {
        consumeStack(obj);

        Type loadType = obj.getFieldType(cpg);

        XField xfield = Hierarchy.findXField(obj, getCPG());
        if (xfield != null) {
            loadType = getType(xfield);
        }


        pushValue(loadType);
    }

    public static Type getType(XField xfield) {
        Type t = Type.getType(xfield.getSignature());
        if (!(t instanceof ReferenceType)) {
            return t;
        }
        ReferenceType loadType = (ReferenceType) t;

        // Check the field store type database to see if we can
        // get a more precise type for this load.

        useDatabase: {
            FieldStoreTypeDatabase database = AnalysisContext
                    .currentAnalysisContext().getFieldStoreTypeDatabase();
            if (database != null) {
                FieldStoreType property = database.getProperty(xfield
                        .getFieldDescriptor());
                if (property != null) {
                    loadType = property.getLoadType(loadType);
                    break useDatabase;
                }
            }

            FieldSummary fieldSummary = AnalysisContext
                    .currentAnalysisContext().getFieldSummary();
            if (fieldSummary != null) {
                Item summary = fieldSummary.getSummary(xfield);
                if (summary != null) {
                    if (xfield.isFinal() && summary.isNull()) {
                        return TypeFrame.getNullType();
                    }
                    if (!"Ljava/lang/Object;".equals(summary.getSignature())) {
                        loadType = (ReferenceType) Type.getType(summary
                                .getSignature());
                    }
                }
            }
        }

        String sourceSignature = xfield.getSourceSignature();
        if (sourceSignature != null && loadType instanceof ObjectType) {
            loadType = GenericUtilities.merge(
                    GenericUtilities.getType(sourceSignature),
                    (ObjectType) loadType);
        }

        return loadType;
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC obj) {
        String methodName = obj.getMethodName(cpg);
        String signature = obj.getSignature(cpg);
        String className = obj.getClassName(cpg);
        if ("asList".equals(methodName) && "java.util.Arrays".equals(className)
                && "([Ljava/lang/Object;)Ljava/util/List;".equals(signature)) {
            consumeStack(obj);
            Type returnType = Type.getType("Ljava/util/Arrays$ArrayList;");
            pushValue(returnType);
            return;
        }
        visitInvokeInstructionCommon(obj);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
        visitInvokeInstructionCommon(obj);
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
        visitInvokeInstructionCommon(obj);
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
        visitInvokeInstructionCommon(obj);
    }

    private boolean getResultTypeFromGenericType(TypeFrame frame, int index, int expectedParameters) {
        try {
            Type mapType = frame.getStackValue(0);
            if (mapType instanceof GenericObjectType) {
                GenericObjectType genericMapType = (GenericObjectType) mapType;
                List<? extends ReferenceType> parameters = genericMapType.getParameters();
                if (parameters != null && parameters.size() == expectedParameters) {
                    ReferenceType resultType = parameters.get(index);
                    if (resultType instanceof GenericObjectType) {
                        resultType = ((GenericObjectType) resultType).produce();
                    }
                    typesComputedFromGenerics.add(resultType);
                    frame.popValue();
                    frame.pushValue(resultType);
                    return true;
                }
            }

        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("oops", e);
        }

        return false;
    }

    private boolean handleGetMapView(TypeFrame frame, String typeName, int index, int expectedNumberOfTypeParameters) {
        try {
            Type mapType = frame.getStackValue(0);
            if (mapType instanceof GenericObjectType) {
                GenericObjectType genericMapType = (GenericObjectType) mapType;
                List<? extends ReferenceType> parameters = genericMapType.getParameters();
                if (parameters == null) {
                    return false;
                }
                if (parameters.size() == expectedNumberOfTypeParameters) {
                    ReferenceType keyType = parameters.get(index);
                    frame.popValue();
                    typesComputedFromGenerics.add(keyType);
                    GenericObjectType keySetType = GenericUtilities.getType(typeName, Collections.singletonList(keyType));
                    typesComputedFromGenerics.add(keySetType);
                    frame.pushValue(keySetType);
                    return true;
                }
            }

        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("oops", e);
        }
        return false;

    }

    public static final boolean DEBUG = SystemProperties.getBoolean("tfmv.debug");

    public void visitInvokeInstructionCommon(InvokeInstruction obj) {
        TypeFrame frame = getFrame();

        String methodName = obj.getMethodName(cpg);
        String signature = obj.getSignature(cpg);
        String className = obj.getClassName(cpg);

        String returnValueSignature = new SignatureParser(signature).getReturnTypeSignature();
        if ("V".equals(returnValueSignature)) {
            consumeStack(obj);
            return;
        }

        if ("isInstance".equals(methodName)) {
            if ("java.lang.Class".equals(className) && valueNumberDataflow != null) {
                // Record the value number of the value checked by this
                // instruction,
                // and the type the value was compared to.
                try {
                    ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
                    if (vnaFrame.isValid()) {
                        ValueNumber stackValue = vnaFrame.getStackValue(1);
                        if (stackValue.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
                            String c = valueNumberDataflow.getClassName(stackValue);
                            if (c != null) {
                                if (c.charAt(0) != '[' && !c.endsWith(";")) {
                                    c = "L" + c.replace('.', '/') + ";";
                                }
                                Type type = Type.getType(c);
                                if (type instanceof ReferenceType) {
                                    instanceOfValueNumber = vnaFrame.getTopValue();
                                    instanceOfType = (ReferenceType) type;
                                    sawEffectiveInstanceOf = true;
                                }
                            }
                        }

                    }
                } catch (DataflowAnalysisException e) {
                    // Ignore
                }
            }
        }

        Type returnTypeOfMethod = obj.getType(cpg);
        if (!(returnTypeOfMethod instanceof ReferenceType)) {
            consumeStack(obj);
            pushReturnType(obj);
            return;
        }

        if ("cast".equals(methodName) && "java.lang.Class".equals(className)) {
            try {
                Type resultType = frame.popValue();
                frame.popValue();
                frame.pushValue(resultType);
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("oops", e);
            }

            return;
        }

        mapGetCheck: if ("get".equals(methodName) && "(Ljava/lang/Object;)Ljava/lang/Object;".equals(signature)
                && className.endsWith("Map") && Subtypes2.instanceOf(className, "java.util.Map")
                && frame.getStackDepth() >= 2) {
            try {
                Type mapType = frame.getStackValue(1);
                if (mapType instanceof GenericObjectType) {
                    GenericObjectType genericMapType = (GenericObjectType) mapType;
                    List<? extends ReferenceType> parameters = genericMapType.getParameters();
                    if (parameters == null || parameters.size() != 2) {
                        break mapGetCheck;
                    }

                    ClassDescriptor c = DescriptorFactory.getClassDescriptor(genericMapType);
                    if (!Subtypes2.instanceOf(c, Map.class)) {
                        break mapGetCheck;
                    }
                    if (!isStraightGenericMap(c)) {
                        break mapGetCheck;
                    }

                    ReferenceType valueType = parameters.get(1);
                    consumeStack(obj);
                    frame.pushValue(valueType);
                    return;

                }
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("oops", e);
            }

        }

        if ("java.util.Map$Entry".equals(className)) {
            if ("getKey".equals(methodName) && getResultTypeFromGenericType(frame, 0, 2) || "getValue".equals(methodName)
                    && getResultTypeFromGenericType(frame, 1, 2)) {
                return;
            }
        }

        if ("entrySet".equals(methodName) && "()Ljava/util/Set;".equals(signature) && className.startsWith("java.util")
                && className.endsWith("Map")) {
            Type argType;
            try {
                argType = frame.popValue();
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("oops", e);
                return;
            }
            ObjectType mapType = (ObjectType) Type.getType("Ljava/util/Map$Entry;");

            if (argType instanceof GenericObjectType) {
                GenericObjectType genericArgType = (GenericObjectType) argType;
                List<? extends ReferenceType> parameters = genericArgType.getParameters();
                if (parameters != null && parameters.size() == 2) {
                    mapType = GenericUtilities.getType("java.util.Map$Entry", parameters);
                }
            }
            GenericObjectType entrySetType = GenericUtilities.getType("java.util.Set", Collections.singletonList(mapType));
            frame.pushValue(entrySetType);
            return;

        }
        if (className.startsWith("java.util") && className.endsWith("Map")) {
            if ("keySet".equals(methodName) && "()Ljava/util/Set;".equals(signature)
                    && handleGetMapView(frame, "java.util.Set", 0, 2) || "values".equals(methodName)
                    && "()Ljava/util/Collection;".equals(signature) && handleGetMapView(frame, "java.util.Collection", 1, 2)) {
                return;
            }
        }

        if ("iterator".equals(methodName) && "()Ljava/util/Iterator;".equals(signature) && className.startsWith("java.util")
                && handleGetMapView(frame, "java.util.Iterator", 0, 1)) {
            return;
        }
        if ("java.util.Iterator".equals(className) && "next".equals(methodName) && "()Ljava/lang/Object;".equals(signature)
                && getResultTypeFromGenericType(frame, 0, 1)) {
            return;
        }

        if ("initCause".equals(methodName) && "(Ljava/lang/Throwable;)Ljava/lang/Throwable;".equals(signature)
                && (className.endsWith("Exception")
                        || className.endsWith("Error"))) {
            try {

                frame.popValue();
                return;
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("Ooops", e);
            }
        }
        if (handleToArray(obj)) {
            return;
        }
        Type result = TopType.instance();
        try {
            Set<XMethod> targets = Hierarchy2.resolveMethodCallTargets(obj, frame, cpg);
            if (DEBUG) {
                System.out.println(" For call to " + className + "." + methodName + signature);
                System.out.println("   for " + targets.size() + " targets: " + targets);
            }
            for (XMethod m : targets) {
                m = m.resolveAccessMethodForMethod();
                String sourceSignature = m.getSourceSignature();
                if (DEBUG) {
                    System.out.println(" Call target: " + m);
                    if (sourceSignature != null) {
                        System.out.println("  source signature: " + sourceSignature);
                    }
                }
                boolean foundSomething = false;
                XMethod m2 = m.bridgeTo();
                if (m2 != null) {
                    m = m2;
                }
                if (sourceSignature != null && !sourceSignature.equals(m.getSignature())) {
                    GenericSignatureParser p = new GenericSignatureParser(sourceSignature);
                    String rv = p.getReturnTypeSignature();
                    if (rv.charAt(0) != 'T') {
                        try {
                            Type t = GenericUtilities.getType(rv);
                            if (t != null) {
                                assert t.getType() != T_VOID;
                                result = merge(result, t);
                                foundSomething = true;
                            }
                        } catch (RuntimeException e) {
                            AnalysisContext.logError("Problem analyzing call to " + m + " with source signature"
                                    + sourceSignature, e);
                            break;
                        }
                    }
                }

                if (m == m2) {
                    SignatureParser p = new SignatureParser(m.getSignature());
                    String rv = p.getReturnTypeSignature();

                    Type t = Type.getType(rv);
                    result = merge(result, t);
                    foundSomething = true;

                }
                if (!foundSomething) {
                    result = TopType.instance();
                    if (DEBUG) {
                        System.out.println(" giving up");
                    }
                    break;
                }
            }

        } catch (RuntimeException e) {
            AnalysisContext.logError("Problem analyzing call to " + className + "." + methodName + signature, e);
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("Problem analyzing call to " + className + "." + methodName + signature, e);
        } catch (ClassNotFoundException e) {
            AnalysisContext.logError("Problem analyzing call to " + className + "." + methodName + signature, e);
        }

        consumeStack(obj);
        if (result instanceof TopType) {
            pushReturnType(obj);
        } else {
            pushValue(result);
        }
    }

    public static final Pattern mapSignaturePattern = Pattern.compile("<(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*):L[^;]*;(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*):L[^;]*;>.*Ljava/util/(\\p{javaJavaIdentifierStart}(\\p{javaJavaIdentifierPart}|/)*)?Map<T\\1;T\\2;>;.*");
    public static boolean isStraightGenericMap(ClassDescriptor c) {
        if (c.matches(Map.class)) {
            return true;
        }
        XClass xc;
        try {
            xc = c.getXClass();
        } catch (CheckedAnalysisException e) {
            return false;
        }
        String sourceSignature = xc.getSourceSignature();
        if (sourceSignature == null) {
            return false;
        }
        if (sourceSignature.startsWith("<")) {
            Matcher matcher = mapSignaturePattern.matcher(sourceSignature);
            if (!matcher.matches()) {
                if (DEBUG) {
                    System.out.println(c + " has a complex generic signature: " + sourceSignature);
                }
                // See Bug3470297 and Bug3470297a examples
                return false;
            }
        }

        return true;
    }

    private Type merge(Type prevType, Type newType) throws DataflowAnalysisException {
        if (prevType.equals(TopType.instance())) {

            if (DEBUG) {
                System.out.println("Got " + newType);
            }
            return newType;
        } else if (prevType.equals(newType)) {
            return prevType;
        } else {
            Type result = typeMerger.mergeTypes(prevType, newType);
            if (DEBUG) {
                System.out.println("Merged " + newType + ", got " + result);
            }
            return result;
        }
    }

    private boolean handleToArray(InvokeInstruction obj) {
        try {
            TypeFrame frame = getFrame();
            if (frame.getStackDepth() == 0) {
                // operand stack is empty
                return false;
            }
            Type topValue = frame.getTopValue();
            if ("toArray".equals(obj.getName(getCPG()))) {
                ReferenceType target = obj.getReferenceType(getCPG());
                String signature = obj.getSignature(getCPG());
                if ("([Ljava/lang/Object;)[Ljava/lang/Object;".equals(signature) && Subtypes2.isCollection(target)) {

                    boolean topIsExact = frame.isExact(frame.getStackLocation(0));
                    Type resultType = frame.popValue();
                    frame.popValue();
                    frame.pushValue(resultType);
                    frame.setExact(frame.getStackLocation(0), topIsExact);
                    return true;
                } else if ("()[Ljava/lang/Object;".equals(signature) && Subtypes2.isCollection(target)
                        && !"Ljava/util/Arrays$ArrayList;".equals(topValue.getSignature())) {
                    consumeStack(obj);
                    pushReturnType(obj);
                    frame.setExact(frame.getStackLocation(0), true);
                    return true;
                }
            }
            return false;
        } catch (DataflowAnalysisException e) {
            return false;
        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
            return false;
        }
    }

    @CheckForNull
    GenericObjectType getLocalVariable(int index, int pos) {
        if (genericLocalVariables == null || !genericLocalVariables.get(index)) {
            return null;
        }
        for (LocalVariable local : localTypeTable.getLocalVariableTypeTable()) {
            if (local.getIndex() == index && local.getStartPC() <= pos
                    && pos < +local.getStartPC() + local.getLength()) {
                String signature = local.getSignature();
                if (signature.indexOf('<') < 0) {
                    continue;
                }
                Type t;
                try {
                    t = GenericUtilities.getType(signature);
                    if (t instanceof GenericObjectType) {
                        return (GenericObjectType) t;
                    }
                } catch (RuntimeException e) {
                    AnalysisContext.logError("Bad signature " + signature
                            + " for " + local.getName(), e);

                }
                return null;
            }
        }
        return null;
    }


    @Override
    public void handleStoreInstruction(StoreInstruction obj) {
        try {
            int numConsumed = obj.consumeStack(cpg);
            if (numConsumed == 1) {
                boolean isExact = isTopOfStackExact();
                TypeFrame frame = getFrame();
                Type value = frame.popValue();
                int index = obj.getIndex();
                if (value instanceof ReferenceType && !(value instanceof GenericObjectType)) {
                    GenericObjectType gType = getLocalVariable(index,
                            getLocation().getHandle().getPosition());
                    value = GenericUtilities.merge(gType, value);
                }
                frame.setValue(index, value);
                frame.setExact(index, isExact);
            } else {
                super.handleStoreInstruction(obj);
            }

        } catch (DataflowAnalysisException e) {
            throw new InvalidBytecodeException(
                    "error handling store instruction ", e);
        }
    }

    /**
     * Handler for all instructions which load values from a local variable and
     * push them on the stack. Note that two locals are loaded for long and
     * double loads.
     */
    @Override
    public void handleLoadInstruction(LoadInstruction obj) {
        int numProduced = obj.produceStack(cpg);
        if (numProduced == Constants.UNPREDICTABLE) {
            throw new InvalidBytecodeException("Unpredictable stack production");
        }

        if (numProduced != 1) {
            super.handleLoadInstruction(obj);
            return;
        }
        int index = obj.getIndex();
        TypeFrame frame = getFrame();
        Type value = frame.getValue(index);
        if (value instanceof ReferenceType && !(value instanceof GenericObjectType)) {
            GenericObjectType gType = getLocalVariable(index,
                    getLocation().getHandle().getPosition());
            value = GenericUtilities.merge(gType, value);
        }
        boolean isExact = frame.isExact(index);
        frame.pushValue(value);
        if (isExact) {
            setTopOfStackIsExact();
        }
    }

    @Override
    public void visitCHECKCAST(CHECKCAST obj) {

        try {
            Type t = getFrame().popValue();
            if (t instanceof NullType) {
                pushValue(t);
            } else {
                pushValue(obj.getType(getCPG()));
            }
        } catch (DataflowAnalysisException e) {
            throw new InvalidBytecodeException("Stack underflow for " + obj + ": " + e.getMessage());
        }
    }

    @Override
    public void visitINSTANCEOF(INSTANCEOF obj) {
        if (valueNumberDataflow != null) {
            // Record the value number of the value checked by this instruction,
            // and the type the value was compared to.
            try {
                ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
                if (vnaFrame.isValid()) {
                    final Type type = obj.getType(getCPG());
                    if (type instanceof ReferenceType) {
                        instanceOfValueNumber = vnaFrame.getTopValue();
                        instanceOfType = (ReferenceType) type;
                        sawEffectiveInstanceOf = true;
                    }
                }
            } catch (DataflowAnalysisException e) {
                // Ignore
            }
        }

        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitIFNULL(IFNULL obj) {

        if (valueNumberDataflow != null) {
            // Record the value number of the value checked by this instruction,
            // and the type the value was compared to.
            try {
                ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
                if (vnaFrame.isValid()) {
                    instanceOfValueNumber = vnaFrame.getTopValue();

                    instanceOfType = NullType.instance();
                    instanceOfFollowedByBranch = true;
                }
            } catch (DataflowAnalysisException e) {
                // Ignore
            }
        }

        consumeStack(obj);
    }

    @Override
    public void visitIFNONNULL(IFNONNULL obj) {

        if (valueNumberDataflow != null) {
            // Record the value number of the value checked by this instruction,
            // and the type the value was compared to.
            try {
                ValueNumberFrame vnaFrame = valueNumberDataflow.getFactAtLocation(getLocation());
                if (vnaFrame.isValid()) {
                    instanceOfValueNumber = vnaFrame.getTopValue();

                    instanceOfType = NullType.instance();
                    instanceOfFollowedByBranch = true;
                }
            } catch (DataflowAnalysisException e) {
                // Ignore
            }
        }

        consumeStack(obj);
    }

    @Override
    public void visitFCMPL(FCMPL obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitFCMPG(FCMPG obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitDCMPL(DCMPL obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitDCMPG(DCMPG obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLCMP(LCMP obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitD2F(D2F obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitD2I(D2I obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitD2L(D2L obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitF2D(F2D obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitF2I(F2I obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitF2L(F2L obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitI2B(I2B obj) {
        consumeStack(obj);
        pushValue(Type.BYTE);
    }

    @Override
    public void visitI2C(I2C obj) {
        consumeStack(obj);
        pushValue(Type.CHAR);
    }

    @Override
    public void visitI2D(I2D obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitI2F(I2F obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitI2L(I2L obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitI2S(I2S obj) {
    } // no change

    @Override
    public void visitL2D(L2D obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitL2F(L2F obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitL2I(L2I obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitIAND(IAND obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLAND(LAND obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitIOR(IOR obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLOR(LOR obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitIXOR(IXOR obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLXOR(LXOR obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitISHR(ISHR obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitIUSHR(IUSHR obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLSHR(LSHR obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitLUSHR(LUSHR obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitISHL(ISHL obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLSHL(LSHL obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitDADD(DADD obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitFADD(FADD obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitIADD(IADD obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLADD(LADD obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitDSUB(DSUB obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitDUP(DUP obj) {
        try {
            TypeFrame frame = getFrame();
            boolean isExact = isTopOfStackExact();
            Type value = frame.popValue();
            frame.pushValue(value);
            if (isExact) {
                setTopOfStackIsExact();
            }
            frame.pushValue(value);
            if (isExact) {
                setTopOfStackIsExact();
            }
        } catch (DataflowAnalysisException e) {
            throw new InvalidBytecodeException(e.toString());
        }
    }

    @Override
    public void visitFSUB(FSUB obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitISUB(ISUB obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLSUB(LSUB obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitDMUL(DMUL obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitFMUL(FMUL obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitIMUL(IMUL obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLMUL(LMUL obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitDDIV(DDIV obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitFDIV(FDIV obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitIDIV(IDIV obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLDIV(LDIV obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitDREM(DREM obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitFREM(FREM obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitIREM(IREM obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLREM(LREM obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitIINC(IINC obj) {
    } // no change to types of stack or locals

    @Override
    public void visitDNEG(DNEG obj) {
    } // no change

    @Override
    public void visitFNEG(FNEG obj) {
    } // no change

    @Override
    public void visitINEG(INEG obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLNEG(LNEG obj) {
    } // no change

    @Override
    public void visitARRAYLENGTH(ARRAYLENGTH obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitAALOAD(AALOAD obj) {
        // To determine the type pushed on the stack,
        // we look at the type of the array reference which was
        // popped off of the stack.
        TypeFrame frame = getFrame();
        try {
            frame.popValue(); // index
            Type arrayType = frame.popValue(); // arrayref
            if (arrayType instanceof ArrayType) {
                ArrayType arr = (ArrayType) arrayType;
                pushValue(arr.getElementType());
            } else {
                pushValue(TypeFrame.getBottomType());
            }
        } catch (DataflowAnalysisException e) {
            throw new InvalidBytecodeException("Stack underflow: " + e.getMessage());
        }
    }

    @Override
    public void visitBALOAD(BALOAD obj) {
        consumeStack(obj);
        pushValue(Type.BYTE);
    }

    @Override
    public void visitCALOAD(CALOAD obj) {
        consumeStack(obj);
        pushValue(Type.CHAR);
    }

    @Override
    public void visitDALOAD(DALOAD obj) {
        consumeStack(obj);
        pushValue(Type.DOUBLE);
    }

    @Override
    public void visitFALOAD(FALOAD obj) {
        consumeStack(obj);
        pushValue(Type.FLOAT);
    }

    @Override
    public void visitIALOAD(IALOAD obj) {
        consumeStack(obj);
        pushValue(Type.INT);
    }

    @Override
    public void visitLALOAD(LALOAD obj) {
        consumeStack(obj);
        pushValue(Type.LONG);
    }

    @Override
    public void visitSALOAD(SALOAD obj) {
        consumeStack(obj);
        pushValue(Type.SHORT);
    }

    // The various xASTORE instructions only consume stack.

    @Override
    public void visitNEW(NEW obj) {
        // FIXME: type is technically "uninitialized"
        // However, we don't model that yet.
        pushValue(obj.getType(getCPG()));

        // We now have an exact type for this value.
        setTopOfStackIsExact();
    }

    @Override
    public void visitNEWARRAY(NEWARRAY obj) {
        consumeStack(obj);
        Type elementType = obj.getType();
        pushValue(elementType);

        // We now have an exact type for this value.
        setTopOfStackIsExact();
    }

    @Override
    public void visitANEWARRAY(ANEWARRAY obj) {
        consumeStack(obj);
        Type elementType = obj.getType(getCPG());
        pushValue(new ArrayType(elementType, 1));

        // We now have an exact type for this value.
        setTopOfStackIsExact();
    }

    @Override
    public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
        consumeStack(obj);
        Type elementType = obj.getType(getCPG());
        pushValue(elementType);
        // We now have an exact type for this value.
        setTopOfStackIsExact();
    }

    private void setTopOfStackIsExact() {
        TypeFrame frame = getFrame();
        frame.setExact(frame.getNumSlots() - 1, true);
    }

    private boolean isTopOfStackExact() {
        TypeFrame frame = getFrame();
        return frame.isExact(frame.getNumSlots() - 1);
    }

    @Override
    public void visitJSR(JSR obj) {
        pushValue(ReturnaddressType.NO_TARGET);
    }

    @Override
    public void visitJSR_W(JSR_W obj) {
        pushValue(ReturnaddressType.NO_TARGET);
    }

    @Override
    public void visitRET(RET obj) {
    } // no change

    @Override
    public void visitIFEQ(IFEQ obj) {
        if (previousWasEffectiveInstanceOf) {
            instanceOfFollowedByBranch = true;
        }
        super.visitIFEQ(obj);
    }

    @Override
    public void visitIFGT(IFGT obj) {
        if (previousWasEffectiveInstanceOf) {
            instanceOfFollowedByBranch = true;
        }
        super.visitIFGT(obj);
    }

    @Override
    public void visitIFLE(IFLE obj) {
        if (previousWasEffectiveInstanceOf) {
            instanceOfFollowedByBranch = true;
        }
        super.visitIFLE(obj);
    }

    @Override
    public void visitIFNE(IFNE obj) {
        if (previousWasEffectiveInstanceOf) {
            instanceOfFollowedByBranch = true;
        }
        super.visitIFNE(obj);
    }

    public boolean isImpliedByGenericTypes(ReferenceType t) {
        return typesComputedFromGenerics.contains(t);
    }
}

