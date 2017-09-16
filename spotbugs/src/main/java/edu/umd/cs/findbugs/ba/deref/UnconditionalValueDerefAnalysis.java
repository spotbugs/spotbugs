/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.deref;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.INVOKEDYNAMIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;
import edu.umd.cs.findbugs.ba.npe.IsNullConditionDecision;
import edu.umd.cs.findbugs.ba.npe.IsNullValue;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.npe.IsNullValueFrame;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.AvailableLoad;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.visitclass.Util;

/**
 * Dataflow analysis to find values unconditionally dereferenced in the future.
 *
 * @author David Hovemeyer
 */
public class UnconditionalValueDerefAnalysis extends BackwardDataflowAnalysis<UnconditionalValueDerefSet> {

    public static final boolean DEBUG = SystemProperties.getBoolean("fnd.derefs.debug");

    public static final boolean ASSUME_NONZERO_TRIP_LOOPS = SystemProperties.getBoolean("fnd.derefs.nonzerotrip");

    public static final boolean IGNORE_DEREF_OF_NCP = SystemProperties.getBoolean("fnd.derefs.ignoreNCP", false);

    public static final boolean CHECK_ANNOTATIONS = SystemProperties.getBoolean("fnd.derefs.checkannotations", true);

    public static final boolean CHECK_CALLS = SystemProperties.getBoolean("fnd.derefs.checkcalls", true);

    public static final boolean DEBUG_CHECK_CALLS = SystemProperties.getBoolean("fnd.derefs.checkcalls.debug");

    private static final int NULLCHECK1[] = { Opcodes.DUP, Opcodes.INVOKESPECIAL, Opcodes.ATHROW };

    private static final int NULLCHECK2[] = { Opcodes.DUP, Opcodes.LDC, Opcodes.INVOKESPECIAL, Opcodes.ATHROW };

    private final CFG cfg;

    private final Method method;

    private final MethodGen methodGen;

    private final ValueNumberDataflow vnaDataflow;

    private final AssertionMethods assertionMethods;

    private IsNullValueDataflow invDataflow;

    private TypeDataflow typeDataflow;

    /**
     * Constructor.
     *
     * @param rdfs
     *            the reverse depth-first-search (for the block order)
     * @param cfg
     *            the CFG for the method
     * @param methodGen
     *            the MethodGen for the method
     * @param vnaDataflow
     * @param assertionMethods
     *            AssertionMethods for the analyzed class
     */
    public UnconditionalValueDerefAnalysis(ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs, CFG cfg, Method method,
            MethodGen methodGen, ValueNumberDataflow vnaDataflow, AssertionMethods assertionMethods) {
        super(rdfs, dfs);
        this.cfg = cfg;
        this.methodGen = methodGen;
        this.method = method;
        this.vnaDataflow = vnaDataflow;
        this.assertionMethods = assertionMethods;
        if (DEBUG) {
            System.out.println("UnconditionalValueDerefAnalysis analysis " + methodGen.getClassName() + "." + methodGen.getName()
                    + " : " + methodGen.getSignature());
        }
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " of " + method;
    }

    /**
     * HACK: use the given is-null dataflow to clear deref sets for values that
     * are known to be definitely non-null on a branch.
     *
     * @param invDataflow
     *            the IsNullValueDataflow to use
     */
    public void clearDerefsOnNonNullBranches(IsNullValueDataflow invDataflow) {
        this.invDataflow = invDataflow;
    }

    public void setTypeDataflow(TypeDataflow typeDataflow) {
        this.typeDataflow = typeDataflow;
    }

    @Override
    public boolean isFactValid(UnconditionalValueDerefSet fact) {
        return !fact.isTop() && !fact.isBottom();
    }

    private static boolean check(InstructionHandle h, int[] opcodes) {
        for (int opcode : opcodes) {
            if (h == null) {
                return false;
            }
            short opcode2 = h.getInstruction().getOpcode();
            if (opcode == Const.LDC) {
                switch (opcode2) {
                case Const.LDC:
                case Const.ALOAD:
                case Const.ALOAD_0:
                case Const.ALOAD_1:
                case Const.ALOAD_2:
                case Const.ALOAD_3:
                    break;
                default:
                    return false;
                }
            } else if (opcode2 != opcode) {
                return false;
            }
            h = h.getNext();
        }
        return true;
    }

    public static boolean isNullCheck(InstructionHandle h, ConstantPoolGen cpg) {
        if (!(h.getInstruction() instanceof IFNONNULL)) {
            return false;
        }
        h = h.getNext();
        final Instruction newInstruction = h.getInstruction();
        if (!(newInstruction instanceof NEW)) {
            return false;
        }
        final ObjectType loadClassType = ((NEW) newInstruction).getLoadClassType(cpg);
        if (!"java.lang.NullPointerException".equals(loadClassType.getClassName())) {
            return false;
        }
        h = h.getNext();
        return check(h, NULLCHECK1) || check(h, NULLCHECK2);

    }

    private void handleNullCheck(Location location, ValueNumberFrame vnaFrame, UnconditionalValueDerefSet fact)
            throws DataflowAnalysisException {
        if (reportPotentialDereference(location, invDataflow.getFactAtLocation(location))) {
            ValueNumber vn = vnaFrame.getTopValue();
            fact.addDeref(vn, location);
        }
    }

    public static boolean reportPotentialDereference(Location location, IsNullValueFrame invFrame)
            throws DataflowAnalysisException {
        if (!invFrame.isValid()) {
            return false;
        }
        IsNullValue value = invFrame.getTopValue();
        if (value.isDefinitelyNotNull()) {
            return false;
        }
        if (value.isDefinitelyNull()) {
            return false;
        }
        return true;
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, UnconditionalValueDerefSet fact)
            throws DataflowAnalysisException {

        Instruction instruction = handle.getInstruction();
        if (fact.isTop()) {
            return;
        }
        Location location = new Location(handle, basicBlock);

        // If this is a call to an assertion method,
        // change the dataflow value to be TOP.
        // We don't want to report future derefs that would
        // be guaranteed only if the assertion methods
        // returns normally.
        // TODO: at some point, evaluate whether we should revisit this
        if (isAssertion(handle) // || handle.getInstruction() instanceof ATHROW
                ) {
            if (DEBUG) {
                System.out.println("MAKING BOTTOM0 AT: " + location);
            }
            fact.clear();
            return;
        }

        // Get value number frame
        ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(location);
        if (!vnaFrame.isValid()) {
            if (DEBUG) {
                System.out.println("MAKING TOP1 AT: " + location);
            }
            // Probably dead code.
            // Assume this location can't be reached.
            makeFactTop(fact);
            return;
        }
        if (isNullCheck(handle, methodGen.getConstantPool())) {
            handleNullCheck(location, vnaFrame, fact);
        }

        // Check for calls to a method that unconditionally dereferences
        // a parameter. Mark any such arguments as derefs.
        if (CHECK_CALLS && instruction instanceof InvokeInstruction) {
            checkUnconditionalDerefDatabase(location, vnaFrame, fact);
        }

        // If this is a method call instruction,
        // check to see if any of the parameters are @NonNull,
        // and treat them as dereferences.
        if (CHECK_ANNOTATIONS && instruction instanceof InvokeInstruction) {
            checkNonNullParams(location, vnaFrame, fact);
        }

        if (CHECK_ANNOTATIONS && instruction instanceof ARETURN) {
            XMethod thisMethod = XFactory.createXMethod(methodGen);
            checkNonNullReturnValue(thisMethod, location, vnaFrame, fact);
        }

        if (CHECK_ANNOTATIONS && (instruction instanceof PUTFIELD || instruction instanceof PUTSTATIC)) {
            checkNonNullPutField(location, vnaFrame, fact);
        }

        // Check to see if an instance value is dereferenced here
        checkInstance(location, vnaFrame, fact);

        /*
        if (false) {
            fact.cleanDerefSet(location, vnaFrame);
        }*/

        if (DEBUG && fact.isTop()) {
            System.out.println("MAKING TOP2 At: " + location);
        }

    }

    /**
     * Check method call at given location to see if it unconditionally
     * dereferences a parameter. Mark any such arguments as derefs.
     *
     * @param location
     *            the Location of the method call
     * @param vnaFrame
     *            ValueNumberFrame at the Location
     * @param fact
     *            the dataflow value to modify
     * @throws DataflowAnalysisException
     */
    private void checkUnconditionalDerefDatabase(Location location, ValueNumberFrame vnaFrame, UnconditionalValueDerefSet fact)
            throws DataflowAnalysisException {
        ConstantPoolGen constantPool = methodGen.getConstantPool();

        for (ValueNumber vn : checkUnconditionalDerefDatabase(location, vnaFrame, constantPool,
                invDataflow.getFactAtLocation(location), typeDataflow)) {
            fact.addDeref(vn, location);
        }
    }

    public static Set<ValueNumber> checkUnconditionalDerefDatabase(Location location, ValueNumberFrame vnaFrame,
            ConstantPoolGen constantPool, @CheckForNull IsNullValueFrame invFrame, TypeDataflow typeDataflow)
                    throws DataflowAnalysisException {
        if (invFrame != null && !invFrame.isValid()) {
            return Collections.emptySet();
        }

        InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();

        SignatureParser sigParser = new SignatureParser(inv.getSignature(constantPool));
        int numParams = sigParser.getNumParameters();
        if (numParams == 0 || !sigParser.hasReferenceParameters()) {
            return Collections.emptySet();
        }
        ParameterNullnessPropertyDatabase database = AnalysisContext.currentAnalysisContext()
                .getUnconditionalDerefParamDatabase();
        if (database == null) {
            if (DEBUG_CHECK_CALLS) {
                System.out.println("no database!");
            }
            return Collections.emptySet();
        }

        TypeFrame typeFrame = typeDataflow.getFactAtLocation(location);
        if (!typeFrame.isValid()) {
            if (DEBUG_CHECK_CALLS) {
                System.out.println("invalid type frame!");
            }
            return Collections.emptySet();
        }

        try {
            Set<XMethod> targetSet = Hierarchy2.resolveMethodCallTargets(inv, typeFrame, constantPool);

            if (targetSet.isEmpty()) {
                return Collections.emptySet();
            }

            if (DEBUG_CHECK_CALLS) {
                System.out.println("target set size: " + targetSet.size());
            }
            // Compute the intersection of all properties
            ParameterProperty derefParamSet = null;
            for (XMethod target : targetSet) {
                if (target.isStub()) {
                    continue;
                }
                if (DEBUG_CHECK_CALLS) {
                    System.out.print("Checking: " + target + ": ");
                }

                ParameterProperty targetDerefParamSet = database.getProperty(target.getMethodDescriptor());
                if (targetDerefParamSet == null) {
                    // Hmm...no information for this target.
                    // assume it doesn't dereference anything
                    if (DEBUG_CHECK_CALLS) {
                        System.out.println("==> no information, assume no guaranteed dereferences");
                    }
                    return Collections.emptySet();
                }

                if (DEBUG_CHECK_CALLS) {
                    System.out.println("==> " + targetDerefParamSet);
                }
                if (derefParamSet == null) {
                    derefParamSet = new ParameterProperty();
                    derefParamSet.copyFrom(targetDerefParamSet);
                } else {
                    derefParamSet.intersectWith(targetDerefParamSet);
                }
            }

            if (derefParamSet == null || derefParamSet.isEmpty()) {
                if (DEBUG) {
                    System.out.println("** Nothing");
                }
                return Collections.emptySet();
            }
            if (DEBUG_CHECK_CALLS) {
                System.out.println("** Summary of call @ " + location.getHandle().getPosition() + ": " + derefParamSet);
            }

            HashSet<ValueNumber> requiredToBeNonnull = new HashSet<>();
            for (int i = 0; i < numParams; i++) {
                if (!derefParamSet.hasProperty(i)) {
                    continue;
                }
                int argSlot = vnaFrame.getStackLocation(sigParser.getSlotsFromTopOfStackForParameter(i));
                if (invFrame != null && !reportDereference(invFrame, argSlot)) {
                    continue;
                }
                if (DEBUG_CHECK_CALLS) {
                    System.out.println("  dereference @ " + location.getHandle().getPosition() + " of parameter " + i);
                }

                requiredToBeNonnull.add(vnaFrame.getValue(argSlot));
            }
            return requiredToBeNonnull;

        } catch (ClassNotFoundException e) {
            AnalysisContext.reportMissingClass(e);
        }
        return Collections.emptySet();
    }

    public static final boolean VERBOSE_NULLARG_DEBUG = SystemProperties.getBoolean("fnd.debug.nullarg.verbose");

    /**
     * If this is a method call instruction, check to see if any of the
     * parameters are @NonNull, and treat them as dereferences.
     *
     * @param location
     *            the Location of the instruction
     * @param vnaFrame
     *            the ValueNumberFrame at the Location of the instruction
     * @param fact
     *            the dataflow value to modify
     *
     * @throws DataflowAnalysisException
     */
    private void checkNonNullReturnValue(XMethod thisMethod, Location location, ValueNumberFrame vnaFrame,
            UnconditionalValueDerefSet fact) throws DataflowAnalysisException {
        INullnessAnnotationDatabase database = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase();

        if (database.getResolvedAnnotation(thisMethod, true) != NullnessAnnotation.NONNULL) {
            return;
        }
        if (reportPotentialDereference(location, invDataflow.getFactAtLocation(location))) {
            ValueNumber vn = vnaFrame.getTopValue();
            fact.addDeref(vn, location);
        }
    }

    /**
     * If this is a putfield or putstatic instruction, check to see if the field
     * is @NonNull, and treat it as dereferences.
     *
     * @param location
     *            the Location of the instruction
     * @param vnaFrame
     *            the ValueNumberFrame at the Location of the instruction
     * @param fact
     *            the dataflow value to modify
     * @throws DataflowAnalysisException
     */
    private void checkNonNullPutField(Location location, ValueNumberFrame vnaFrame, UnconditionalValueDerefSet fact)
            throws DataflowAnalysisException {
        INullnessAnnotationDatabase database = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase();

        FieldInstruction fieldIns = (FieldInstruction) location.getHandle().getInstruction();

        XField field = XFactory.createXField(fieldIns, methodGen.getConstantPool());
        char firstChar = field.getSignature().charAt(0);
        if (firstChar != 'L' && firstChar != '[') {
            return;
        }
        NullnessAnnotation resolvedAnnotation = database.getResolvedAnnotation(field, true);
        if (resolvedAnnotation == NullnessAnnotation.NONNULL) {
            IsNullValueFrame invFrame = invDataflow.getFactAtLocation(location);
            if (!invFrame.isValid()) {
                return;
            }
            IsNullValue value = invFrame.getTopValue();
            if (reportDereference(value)) {
                ValueNumber vn = vnaFrame.getTopValue();
                fact.addDeref(vn, location);
            }
        }
    }

    /**
     * If this is a method call instruction, check to see if any of the
     * parameters are @NonNull, and treat them as dereferences.
     *
     * @param location
     *            the Location of the instruction
     * @param vnaFrame
     *            the ValueNumberFrame at the Location of the instruction
     * @param fact
     *            the dataflow value to modify
     * @throws DataflowAnalysisException
     */
    private void checkNonNullParams(Location location, ValueNumberFrame vnaFrame, UnconditionalValueDerefSet fact)
            throws DataflowAnalysisException {
        ConstantPoolGen constantPool = methodGen.getConstantPool();
        Set<ValueNumber> nonNullParams = checkNonNullParams(location, vnaFrame, constantPool, method,
                invDataflow.getFactAtLocation(location));
        for (ValueNumber vn : nonNullParams) {
            fact.addDeref(vn, location);
        }
    }

    public static Set<ValueNumber> checkAllNonNullParams(Location location, ValueNumberFrame vnaFrame,
            ConstantPoolGen constantPool, @CheckForNull Method method, @CheckForNull IsNullValueDataflow invDataflow,
            TypeDataflow typeDataflow) throws DataflowAnalysisException {
        IsNullValueFrame invFrame = null;
        if (invDataflow != null) {
            invFrame = invDataflow.getFactAtLocation(location);
        }
        Set<ValueNumber> result1 = checkNonNullParams(location, vnaFrame, constantPool, method, invFrame);
        Set<ValueNumber> result2 = checkUnconditionalDerefDatabase(location, vnaFrame, constantPool, invFrame, typeDataflow);
        if (result1.isEmpty()) {
            return result2;
        }
        if (result2.isEmpty()) {
            return result1;
        }
        result1.addAll(result2);
        return result1;
    }

    public static Set<ValueNumber> checkNonNullParams(Location location, ValueNumberFrame vnaFrame, ConstantPoolGen constantPool,
            @CheckForNull Method method, @CheckForNull IsNullValueFrame invFrame) throws DataflowAnalysisException {

        if (invFrame != null && !invFrame.isValid()) {
            return Collections.emptySet();
        }
        INullnessAnnotationDatabase database = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase();

        InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
        if (inv instanceof INVOKEDYNAMIC) {
            // ignore indy, it's only used to create lambda instances
            return Collections.emptySet();
        }
        XMethod called = XFactory.createXMethod(inv, constantPool);
        SignatureParser sigParser = new SignatureParser(called.getSignature());
        int numParams = sigParser.getNumParameters();

        Set<ValueNumber> result = new HashSet<>();
        Iterator<String> parameterIterator = sigParser.parameterSignatureIterator();
        for (int i = 0; i < numParams; i++) {
            String parameterSignature = parameterIterator.next();
            char firstChar = parameterSignature.charAt(0);
            if (firstChar != 'L' && firstChar != '[') {
                continue;
            }
            int offset = sigParser.getSlotsFromTopOfStackForParameter(i);
            if (invFrame != null) {
                int slot = invFrame.getStackLocation(offset);
                if (!reportDereference(invFrame, slot)) {
                    continue;
                }
            }
            if (database.parameterMustBeNonNull(called, i)) {
                int catchSizeNPE = Util.getSizeOfSurroundingTryBlock(method, "java/lang/NullPointerException", location
                        .getHandle().getPosition());
                int catchSizeNFE = Util.getSizeOfSurroundingTryBlock(method, "java/lang/NumberFormatException", location
                        .getHandle().getPosition());
                if (catchSizeNPE == Integer.MAX_VALUE
                        && (!"java.lang.Integer".equals(called.getClassName()) || catchSizeNFE == Integer.MAX_VALUE)) {
                    // Get the corresponding value number
                    ValueNumber vn = vnaFrame.getArgument(inv, constantPool, i, sigParser);
                    result.add(vn);
                }
            }
        }
        return result;
    }

    /**
     * Check to see if the instruction has a null check associated with it, and
     * if so, add a dereference.
     *
     * @param location
     *            the Location of the instruction
     * @param vnaFrame
     *            ValueNumberFrame at the Location of the instruction
     * @param fact
     *            the dataflow value to modify
     * @throws DataflowAnalysisException
     */
    private void checkInstance(Location location, ValueNumberFrame vnaFrame, UnconditionalValueDerefSet fact)
            throws DataflowAnalysisException {
        // See if this instruction has a null check.
        // If it does, the fall through predecessor will be
        // identify itself as the null check.
        if (!location.isFirstInstructionInBasicBlock()) {
            return;
        }
        if (invDataflow == null) {
            return;
        }
        BasicBlock fallThroughPredecessor = cfg.getPredecessorWithEdgeType(location.getBasicBlock(), EdgeTypes.FALL_THROUGH_EDGE);
        if (fallThroughPredecessor == null || !fallThroughPredecessor.isNullCheck()) {
            return;
        }

        // Get the null-checked value
        ValueNumber vn = vnaFrame.getInstance(location.getHandle().getInstruction(), methodGen.getConstantPool());

        // Ignore dereferences of this
        if (!methodGen.isStatic()) {
            ValueNumber v = vnaFrame.getValue(0);
            if (v.equals(vn)) {
                return;
            }
        }
        if (vn.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT)) {
            return;
        }

        IsNullValueFrame startFact = null;

        startFact = invDataflow.getStartFact(fallThroughPredecessor);

        if (!startFact.isValid()) {
            return;
        }

        int slot = startFact.getInstanceSlot(location.getHandle().getInstruction(), methodGen.getConstantPool());
        if (!reportDereference(startFact, slot)) {
            return;
        }
        if (DEBUG) {
            System.out.println("FOUND GUARANTEED DEREFERENCE");
            System.out.println("Load: " + vnaFrame.getLoad(vn));
            System.out.println("Pred: " + fallThroughPredecessor);
            System.out.println("startFact: " + startFact);
            System.out.println("Location: " + location);
            System.out.println("Value number frame: " + vnaFrame);
            System.out.println("Dereferenced valueNumber: " + vn);
            System.out.println("invDataflow: " + startFact);
            System.out.println("IGNORE_DEREF_OF_NCP: " + IGNORE_DEREF_OF_NCP);
        }
        // Mark the value number as being dereferenced at this location
        fact.addDeref(vn, location);
    }

    private static boolean reportDereference(IsNullValueFrame invFrameAtNullCheck, int instance) {
        return reportDereference(invFrameAtNullCheck.getValue(instance));
    }

    private static boolean reportDereference(IsNullValue value) {
        if (value.isDefinitelyNotNull()) {
            return false;
        }
        if (value.isDefinitelyNull()) {
            return false;
        }
        if (IGNORE_DEREF_OF_NCP && value.isNullOnComplicatedPath()) {
            return false;
        }
        return true;
    }

    /**
     * Return whether or not given instruction is an assertion.
     *
     * @param handle
     *            the instruction
     * @return true if instruction is an assertion, false otherwise
     */
    private boolean isAssertion(InstructionHandle handle) {
        return assertionMethods.isAssertionHandle(handle, methodGen.getConstantPool());

    }

    @Override
    public void copy(UnconditionalValueDerefSet source, UnconditionalValueDerefSet dest) {
        dest.makeSameAs(source);
    }

    @Override
    public UnconditionalValueDerefSet createFact() {
        return new UnconditionalValueDerefSet(vnaDataflow.getAnalysis().getNumValuesAllocated());
    }

    @Override
    public void initEntryFact(UnconditionalValueDerefSet result) throws DataflowAnalysisException {
        result.clear();
    }

    // /* (non-Javadoc)
    // * @see
    // edu.umd.cs.findbugs.ba.DataflowAnalysis#initResultFact(java.lang.Object)
    // */
    // public void initResultFact(UnconditionalValueDerefSet result) {
    // result.setIsTop();
    // }

    @Override
    public void makeFactTop(UnconditionalValueDerefSet fact) {
        fact.setIsTop();
    }

    @Override
    public boolean isTop(UnconditionalValueDerefSet fact) {
        return fact.isTop();
    }

    @Override
    public void meetInto(UnconditionalValueDerefSet fact, Edge edge, UnconditionalValueDerefSet result)
            throws DataflowAnalysisException {
        meetInto(fact, edge, result, false);
    }

    public void meetInto(UnconditionalValueDerefSet fact, Edge edge, UnconditionalValueDerefSet result, boolean onlyEdge) {
        if (isExceptionEdge(edge) && !onlyEdge) {
            if (DEBUG) {
                System.out.println("Skipping exception edge");
            }
            return;
        }

        ValueNumber knownNonnullOnBranch = null;
        // Edge transfer function
        if (isFactValid(fact)) {
            fact = propagateDerefSetsToMergeInputValues(fact, edge);
            if (invDataflow != null) {
                knownNonnullOnBranch = findValueKnownNonnullOnBranch(fact, edge);
                if (knownNonnullOnBranch != null) {
                    fact = duplicateFact(fact);
                    fact.clearDerefSet(knownNonnullOnBranch);
                }
            }
        }
        boolean isBackEdge = edge.isBackwardInBytecode();
        Set<Integer> loopExitBranches = ClassContext.getLoopExitBranches(method, methodGen);
        assert loopExitBranches != null;
        boolean sourceIsTopOfLoop = edge.sourceIsTopOfLoop(loopExitBranches);
        if (sourceIsTopOfLoop && edge.getType() == EdgeTypes.FALL_THROUGH_EDGE) {
            isBackEdge = true;
        }
        /*
        if (false && (edge.getType() == EdgeTypes.IFCMP_EDGE || sourceIsTopOfLoop)) {
            System.out.println("Meet into " + edge);
            System.out.println("  foo2: " + sourceIsTopOfLoop);
            System.out.println("  getType: " + edge.getType());
            System.out.println("  Backedge according to bytecode: " + isBackEdge);
            System.out.println("  Fact hashCode: " + System.identityHashCode(result));
            System.out.println("  Initial fact: " + result);
            System.out.println("  Edge fact: " + fact);
        }
         */
        if (result.isTop() || fact.isBottom()) {
            // Make result identical to other fact
            copy(fact, result);
            if (ASSUME_NONZERO_TRIP_LOOPS && isBackEdge && !fact.isTop()) {
                result.resultsFromBackEdge = true;
            }
        } else if (ASSUME_NONZERO_TRIP_LOOPS && isBackEdge && !fact.isTop()) {
            result.unionWith(fact, vnaDataflow.getAnalysis().getFactory());
            result.resultsFromBackEdge = true;
            if (DEBUG) {
                System.out.println("\n Forcing union of " + System.identityHashCode(result) + " due to backedge info");
                System.out.println("  result: " + result);
            }

        } else if (result.isBottom() || fact.isTop()) {
            // No change in result fact
        } else {
            // Dataflow merge
            // (intersection of unconditional deref values)
            if (ASSUME_NONZERO_TRIP_LOOPS && result.resultsFromBackEdge) {
                result.backEdgeUpdateCount++;
                if (result.backEdgeUpdateCount < 10) {
                    if (DEBUG) {
                        System.out.println("\n Union update of " + System.identityHashCode(result) + " due to backedge info");
                    }
                    result.unionWith(fact, vnaDataflow.getAnalysis().getFactory());
                    return;
                }
            }
            result.mergeWith(fact, knownNonnullOnBranch, vnaDataflow.getAnalysis().getFactory());
            if (DEBUG) {
                System.out.println("  updated: " + System.identityHashCode(result));
                System.out.println("  result: " + result);

            }
        }
        if (DEBUG && isBackEdge && edge.getType() == EdgeTypes.IFCMP_EDGE) {
            System.out.println("  result: " + result);
        }
    }

    /**
     * Find out if any VNs in the source block contribute to unconditionally
     * dereferenced VNs in the target block. If so, the VN in the source block
     * is also unconditionally dereferenced, and we must propagate the target
     * VN's dereferences.
     *
     * @param fact
     *            a dataflow value
     * @param edge
     *            edge to check for merge input values
     * @return possibly-modified dataflow value
     */
    private UnconditionalValueDerefSet propagateDerefSetsToMergeInputValues(UnconditionalValueDerefSet fact, Edge edge) {

        ValueNumberFrame blockValueNumberFrame = vnaDataflow.getResultFact(edge.getSource());
        ValueNumberFrame targetValueNumberFrame = vnaDataflow.getStartFact(edge.getTarget());

        UnconditionalValueDerefSet originalFact = fact;
        fact = duplicateFact(fact);

        if (blockValueNumberFrame.isValid() && targetValueNumberFrame.isValid()) {
            int slots = 0;
            if (targetValueNumberFrame.getNumSlots() == blockValueNumberFrame.getNumSlots()) {
                slots = targetValueNumberFrame.getNumSlots();
            } else if (targetValueNumberFrame.getNumLocals() == blockValueNumberFrame.getNumLocals()) {
                slots = targetValueNumberFrame.getNumLocals();
            }

            if (slots > 0) {
                if (DEBUG) {
                    System.out.println("** Valid VNA frames for " + edge);
                    System.out.println("** Block : " + blockValueNumberFrame);
                    System.out.println("** Target: " + targetValueNumberFrame);
                }

                for (int i = 0; i < slots; i++) {
                    ValueNumber blockVN = blockValueNumberFrame.getValue(i);
                    ValueNumber targetVN = targetValueNumberFrame.getValue(i);
                    if (blockVN.equals(targetVN)) {
                        continue;
                    }
                    fact.clearDerefSet(blockVN);
                    if (originalFact.isUnconditionallyDereferenced(targetVN)) {
                        fact.setDerefSet(blockVN, originalFact.getUnconditionalDerefLocationSet(targetVN));
                    }

                } // for all slots

                for (ValueNumber blockVN : blockValueNumberFrame.valueNumbersForLoads()) {
                    AvailableLoad load = blockValueNumberFrame.getLoad(blockVN);
                    if (load == null) {
                        continue;
                    }
                    ValueNumber[] targetVNs = targetValueNumberFrame.getAvailableLoad(load);
                    if (targetVNs != null) {
                        for (ValueNumber targetVN : targetVNs) {
                            if (targetVN.hasFlag(ValueNumber.PHI_NODE) && fact.isUnconditionallyDereferenced(targetVN)
                                    && !fact.isUnconditionallyDereferenced(blockVN)) {
                                // Block VN is also dereferenced
                                // unconditionally.
                                AvailableLoad targetLoad = targetValueNumberFrame.getLoad(targetVN);
                                if (!load.equals(targetLoad)) {
                                    continue;
                                }
                                if (DEBUG) {
                                    System.out.println("** Copy vn derefs for " + load + " from " + targetVN + " --> " + blockVN);
                                    System.out.println("** block phi for " + System.identityHashCode(blockValueNumberFrame)
                                            + " is " + blockValueNumberFrame.phiNodeForLoads);
                                    System.out.println("** target phi for " + System.identityHashCode(targetValueNumberFrame)
                                            + " is " + targetValueNumberFrame.phiNodeForLoads);
                                }
                                fact.setDerefSet(blockVN, fact.getUnconditionalDerefLocationSet(targetVN));

                            }
                        }
                    }
                }

            }
        }
        if (DEBUG) {
            System.out.println("Target VNF: " + targetValueNumberFrame);
            System.out.println("Block VNF: " + blockValueNumberFrame);
            System.out.println("fact: " + fact);
        }
        fact.cleanDerefSet(null, blockValueNumberFrame);
        return fact;
    }

    /**
     * Return a duplicate of given dataflow fact.
     *
     * @param fact
     *            a dataflow fact
     * @return a duplicate of the input dataflow fact
     */
    private UnconditionalValueDerefSet duplicateFact(UnconditionalValueDerefSet fact) {
        UnconditionalValueDerefSet copyOfFact = createFact();
        copy(fact, copyOfFact);
        fact = copyOfFact;
        return fact;
    }

    /**
     * Clear deref sets of values if this edge is the non-null branch of an if
     * comparison.
     *
     * @param fact
     *            a datflow fact
     * @param edge
     *            edge to check
     * @return possibly-modified dataflow fact
     */
    private @CheckForNull
    ValueNumber findValueKnownNonnullOnBranch(UnconditionalValueDerefSet fact, Edge edge) {

        IsNullValueFrame invFrame = invDataflow.getResultFact(edge.getSource());
        if (!invFrame.isValid()) {
            return null;
        }
        IsNullConditionDecision decision = invFrame.getDecision();
        if (decision == null) {
            return null;
        }

        IsNullValue inv = decision.getDecision(edge.getType());
        if (inv == null || !inv.isDefinitelyNotNull()) {
            return null;
        }
        ValueNumber value = decision.getValue();
        if (DEBUG) {
            System.out.println("Value number " + value + " is known nonnull on " + edge);
        }

        return value;
    }

    /**
     * Determine whether dataflow should be propagated on given edge.
     *
     * @param edge
     *            the edge
     * @return true if dataflow should be propagated on the edge, false
     *         otherwise
     */
    private boolean isExceptionEdge(Edge edge) {
        boolean isExceptionEdge = edge.isExceptionEdge();
        if (isExceptionEdge) {
            if (DEBUG) {
                System.out.println("NOT Ignoring " + edge);
            }
            return true; // false
        }
        if (edge.getType() != EdgeTypes.FALL_THROUGH_EDGE) {
            return false;
        }
        InstructionHandle h = edge.getSource().getLastInstruction();
        if (h != null && h.getInstruction() instanceof IFNONNULL && isNullCheck(h, methodGen.getConstantPool())) {
            return true;
        }

        return false;

    }

    @Override
    public boolean same(UnconditionalValueDerefSet fact1, UnconditionalValueDerefSet fact2) {
        return fact1.resultsFromBackEdge || fact1.isSameAs(fact2);
    }

    @Override
    public void startIteration() {
        // System.out.println("analysis iteration in " +
        // methodGen.getClassName() + " on " + methodGen.toString());
    }

    @Override
    public int getLastUpdateTimestamp(UnconditionalValueDerefSet fact) {
        return fact.getLastUpdateTimestamp();
    }

    @Override
    public void setLastUpdateTimestamp(UnconditionalValueDerefSet fact, int lastUpdate) {
        fact.setLastUpdateTimestamp(lastUpdate);
    }

    // public static void main(String[] args) throws Exception {
    // if (args.length != 1) {
    // System.err.println("Usage: " +
    // UnconditionalValueDerefAnalysis.class.getName() + " <classfile>");
    // System.exit(1);
    // }
    //
    // DataflowTestDriver<UnconditionalValueDerefSet,
    // UnconditionalValueDerefAnalysis> driver =
    // new DataflowTestDriver<UnconditionalValueDerefSet,
    // UnconditionalValueDerefAnalysis>() {
    // /* (non-Javadoc)
    // * @see
    // edu.umd.cs.findbugs.ba.DataflowTestDriver#createDataflow(edu.umd.cs.findbugs.ba.ClassContext,
    // org.apache.bcel.classfile.Method)
    // */
    // @Override
    // public Dataflow<UnconditionalValueDerefSet,
    // UnconditionalValueDerefAnalysis> createDataflow(ClassContext
    // classContext, Method method) throws CFGBuilderException,
    // DataflowAnalysisException {
    // return classContext.getUnconditionalValueDerefDataflow(method);
    // }
    // };
    // if (SystemProperties.getBoolean("forwardcfg")) {
    // driver.overrideIsForwards();
    // }
    // driver.execute(args[0]);
    // }
}
