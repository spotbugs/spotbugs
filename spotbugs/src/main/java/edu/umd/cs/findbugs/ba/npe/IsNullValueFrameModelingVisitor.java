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

package edu.umd.cs.findbugs.ba.npe;

import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ANEWARRAY;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.MULTIANEWARRAY;
import org.apache.bcel.generic.NEW;
import org.apache.bcel.generic.NEWARRAY;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefAnalysis;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.type.TypeFrame;
import edu.umd.cs.findbugs.ba.vna.AvailableLoad;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysisFeatures;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

public class IsNullValueFrameModelingVisitor extends AbstractFrameModelingVisitor<IsNullValue, IsNullValueFrame> {

    private static final boolean NO_ASSERT_HACK = SystemProperties.getBoolean("inva.noAssertHack");

    private static final boolean MODEL_NONNULL_RETURN = SystemProperties.getBoolean("fnd.modelNonnullReturn", true);

    private final AssertionMethods assertionMethods;

    private final ValueNumberDataflow vnaDataflow;

    private final TypeDataflow typeDataflow;

    private final boolean trackValueNumbers;

    private int slotContainingNewNullValue;

    public IsNullValueFrameModelingVisitor(ConstantPoolGen cpg, AssertionMethods assertionMethods,
            ValueNumberDataflow vnaDataflow, TypeDataflow typeDataflow, boolean trackValueNumbers) {
        super(cpg);
        this.assertionMethods = assertionMethods;
        this.vnaDataflow = vnaDataflow;
        this.trackValueNumbers = trackValueNumbers;
        this.typeDataflow = typeDataflow;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor#analyzeInstruction
     * (org.apache.bcel.generic.Instruction)
     */
    @Override
    public void analyzeInstruction(Instruction ins) throws DataflowAnalysisException {
        if (!getFrame().isValid()) {
            return;
        }
        slotContainingNewNullValue = -1;
        super.analyzeInstruction(ins);
        if (!getFrame().isValid()) {
            return;
        }

        if (!NO_ASSERT_HACK) {
            if (assertionMethods.isAssertionHandle(getLocation().getHandle(), cpg)) {
                IsNullValueFrame frame = getFrame();
                for (int i = 0; i < frame.getNumSlots(); ++i) {
                    IsNullValue value = frame.getValue(i);
                    if (value.isDefinitelyNull() || value.isNullOnSomePath()) {
                        frame.setValue(i, IsNullValue.nonReportingNotNullValue());
                    }
                }
                for (Map.Entry<ValueNumber, IsNullValue> e : frame.getKnownValueMapEntrySet()) {
                    IsNullValue value = e.getValue();
                    if (value.isDefinitelyNull() || value.isNullOnSomePath()) {
                        e.setValue(IsNullValue.nonReportingNotNullValue());
                    }

                }
            }
        }

    }

    /**
     * @return Returns the slotContainingNewNullValue; or -1 if no new null
     *         value was produced
     */
    public int getSlotContainingNewNullValue() {
        return slotContainingNewNullValue;
    }

    @Override
    public IsNullValue getDefaultValue() {
        return IsNullValue.nonReportingNotNullValue();
    }

    // Overrides of specific instruction visitor methods.
    // ACONST_NULL obviously produces a value that is DEFINITELY NULL.
    // LDC produces values that are NOT NULL.
    // NEW produces values that are NOT NULL.

    // Note that all instructions that have an implicit null
    // check (field access, invoke, etc.) are handled in IsNullValueAnalysis,
    // because handling them relies on control flow (the existence of
    // an ETB and exception edge prior to the block containing the
    // instruction with the null check.)

    // Note that we don't override IFNULL and IFNONNULL.
    // Those are handled in the analysis itself, because we need
    // to produce different values in each of the control successors.

    private void produce(IsNullValue value) {
        IsNullValueFrame frame = getFrame();
        frame.pushValue(value);
        newValueOnTOS();
    }

    private void produce2(IsNullValue value) {
        IsNullValueFrame frame = getFrame();
        frame.pushValue(value);
        frame.pushValue(value);
    }

    /**
     * Handle method invocations. Generally, we want to get rid of null
     * information following a call to a likely exception thrower or assertion.
     */
    private void handleInvoke(InvokeInstruction obj) {
        Type returnType = obj.getReturnType(getCPG());

        Location location = getLocation();

        if (trackValueNumbers) {
            try {
                ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(location);
                Set<ValueNumber> nonnullParameters = UnconditionalValueDerefAnalysis.checkAllNonNullParams(location, vnaFrame,
                        cpg, null, null, typeDataflow);

                if (!nonnullParameters.isEmpty()) {
                    IsNullValue kaboom = IsNullValue.noKaboomNonNullValue(location);
                    IsNullValueFrame frame = getFrame();
                    for (ValueNumber vn : nonnullParameters) {
                        IsNullValue knownValue = frame.getKnownValue(vn);
                        if (knownValue != null && !knownValue.isDefinitelyNotNull()) {
                            if (knownValue.isDefinitelyNull()) {
                                frame.setTop();
                                return;
                            }
                            frame.setKnownValue(vn, kaboom);
                        }
                        for (int i = 0; i < vnaFrame.getNumSlots(); i++) {
                            IsNullValue value = frame.getValue(i);
                            if (vnaFrame.getValue(i).equals(vn) && !value.isDefinitelyNotNull()) {
                                frame.setValue(i, kaboom);
                                if (value.isDefinitelyNull()) {
                                    frame.setTop();
                                    return;
                                }
                            }
                        }
                    }

                }
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("Error looking up nonnull parameters for invoked method", e);
            }
        }
        // Determine if we are going to model the return value of this call.
        boolean modelCallReturnValue = MODEL_NONNULL_RETURN && returnType instanceof ReferenceType;

        if (!modelCallReturnValue) {
            // Normal case: Assume returned values are non-reporting non-null.
            handleNormalInstruction(obj);
        } else {
            // Special case: some special value is pushed on the stack for the
            // return value
            IsNullValue result = null;
            TypeFrame typeFrame;
            try {
                typeFrame = typeDataflow.getFactAtLocation(location);

                Set<XMethod> targetSet = Hierarchy2.resolveMethodCallTargets(obj, typeFrame, cpg);

                if (targetSet.isEmpty()) {
                    XMethod calledMethod = XFactory.createXMethod(obj, getCPG());
                    result = getReturnValueNullness(calledMethod);
                } else {
                    for (XMethod calledMethod : targetSet) {
                        IsNullValue pushValue = getReturnValueNullness(calledMethod);
                        if (result == null) {
                            result = pushValue;
                        } else {
                            result = IsNullValue.merge(result, pushValue);
                        }
                    }
                }
            } catch (DataflowAnalysisException e) {
                result = IsNullValue.nonReportingNotNullValue();
            } catch (ClassNotFoundException e) {
                result = IsNullValue.nonReportingNotNullValue();
            }
            modelInstruction(obj, getNumWordsConsumed(obj), getNumWordsProduced(obj), result);
            newValueOnTOS();
        }

    }

    public IsNullValue getReturnValueNullness(XMethod calledMethod) {
        IsNullValue pushValue;
        if (IsNullValueAnalysis.DEBUG) {
            System.out.println("Check " + calledMethod + " for null return...");
        }
        NullnessAnnotation annotation = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase()
                .getResolvedAnnotation(calledMethod, false);
        Boolean alwaysNonNull = AnalysisContext.currentAnalysisContext().getReturnValueNullnessPropertyDatabase()
                .getProperty(calledMethod.getMethodDescriptor());
        if (annotation == NullnessAnnotation.CHECK_FOR_NULL) {
            if (IsNullValueAnalysis.DEBUG) {
                System.out.println("Null value returned from " + calledMethod);
            }
            pushValue = IsNullValue.nullOnSimplePathValue().markInformationAsComingFromReturnValueOfMethod(
                    calledMethod);
        } else if (annotation == NullnessAnnotation.NULLABLE) {
            pushValue = IsNullValue.nonReportingNotNullValue();
        } else if (annotation == NullnessAnnotation.NONNULL
                || (alwaysNonNull != null && alwaysNonNull.booleanValue())) {
            // Method is declared NOT to return null
            if (IsNullValueAnalysis.DEBUG) {
                System.out.println("NonNull value return from " + calledMethod);
            }
            pushValue = IsNullValue.nonNullValue().markInformationAsComingFromReturnValueOfMethod(calledMethod);

        } else {
            pushValue = IsNullValue.nonReportingNotNullValue();
        }
        return pushValue;
    }

    /**
     * Hook indicating that a new (possibly-null) value is on the top of the
     * stack.
     */
    private void newValueOnTOS() {
        IsNullValueFrame frame = getFrame();
        if (frame.getStackDepth() < 1) {
            return;
        }
        int tosSlot = frame.getNumSlots() - 1;
        IsNullValue tos = frame.getValue(tosSlot);
        if (tos.isDefinitelyNull()) {
            slotContainingNewNullValue = tosSlot;
        }
        if (trackValueNumbers) {
            try {
                ValueNumberFrame vnaFrameAfter = vnaDataflow.getFactAfterLocation(getLocation());
                if (vnaFrameAfter.isValid()) {
                    ValueNumber tosVN = vnaFrameAfter.getTopValue();
                    getFrame().setKnownValue(tosVN, tos);
                }
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("error", e);
            }
        }
    }

    @Override
    public void visitPUTFIELD(PUTFIELD obj) {
        if (getNumWordsConsumed(obj) != 2) {
            super.visitPUTFIELD(obj);
            return;
        }

        IsNullValue nullValueStored = null;
        try {
            nullValueStored = getFrame().getTopValue();
        } catch (DataflowAnalysisException e1) {
            AnalysisContext.logError("Oops", e1);
        }
        super.visitPUTFIELD(obj);
        XField field = XFactory.createXField(obj, cpg);
        if (nullValueStored != null && ValueNumberAnalysisFeatures.REDUNDANT_LOAD_ELIMINATION) {
            try {
                ValueNumberFrame vnaFrameBefore = vnaDataflow.getFactAtLocation(getLocation());
                ValueNumber refValue = vnaFrameBefore.getStackValue(1);
                AvailableLoad load = new AvailableLoad(refValue, field);
                ValueNumberFrame vnaFrameAfter = vnaDataflow.getFactAfterLocation(getLocation());
                ValueNumber[] newValueNumbersForField = vnaFrameAfter.getAvailableLoad(load);
                if (newValueNumbersForField != null && trackValueNumbers) {
                    for (ValueNumber v : newValueNumbersForField) {
                        getFrame().setKnownValue(v, nullValueStored);
                    }
                }
            } catch (DataflowAnalysisException e) {
                AnalysisContext.logError("Oops", e);
            }
        }
    }

    @Override
    public void visitGETFIELD(GETFIELD obj) {
        if (getNumWordsProduced(obj) != 1) {
            super.visitGETFIELD(obj);
            return;
        }

        if (checkForKnownValue(obj)) {
            return;
        }

        XField field = XFactory.createXField(obj, cpg);

        NullnessAnnotation annotation = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase()
                .getResolvedAnnotation(field, false);
        if (annotation == NullnessAnnotation.NONNULL) {
            modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
            produce(IsNullValue.nonNullValue());
        } else if (annotation == NullnessAnnotation.CHECK_FOR_NULL) {
            modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
            produce(IsNullValue.nullOnSimplePathValue().markInformationAsComingFromFieldValue(field));
        } else {

            super.visitGETFIELD(obj);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.AbstractFrameModelingVisitor#visitGETSTATIC(org
     * .apache.bcel.generic.GETSTATIC)
     */
    @Override
    public void visitGETSTATIC(GETSTATIC obj) {
        if (getNumWordsProduced(obj) != 1) {
            super.visitGETSTATIC(obj);
            return;
        }

        if (checkForKnownValue(obj)) {
            return;
        }
        XField field = XFactory.createXField(obj, cpg);
        if (field.isFinal()) {
            Item summary = AnalysisContext.currentAnalysisContext().getFieldSummary().getSummary(field);
            if (summary.isNull()) {
                produce(IsNullValue.nullValue());
                return;
            }
        }
        if ("java.util.logging.Level".equals(field.getClassName()) && "SEVERE".equals(field.getName())
                || "org.apache.log4j.Level".equals(field.getClassName())
                && ("ERROR".equals(field.getName()) || "FATAL".equals(field.getName()))) {
            getFrame().toExceptionValues();
        }

        if (field.getName().startsWith("class$")) {
            produce(IsNullValue.nonNullValue());
            return;
        }
        NullnessAnnotation annotation = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase()
                .getResolvedAnnotation(field, false);
        if (annotation == NullnessAnnotation.NONNULL) {
            modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
            produce(IsNullValue.nonNullValue());
        } else if (annotation == NullnessAnnotation.CHECK_FOR_NULL) {
            modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
            produce(IsNullValue.nullOnSimplePathValue().markInformationAsComingFromFieldValue(field));
        } else {

            super.visitGETSTATIC(obj);
        }
    }

    /**
     * Check given Instruction to see if it produces a known value. If so, model
     * the instruction and return true. Otherwise, do nothing and return false.
     * Should only be used for instructions that produce a single value on the
     * top of the stack.
     *
     * @param obj
     *            the Instruction the instruction
     * @return true if the instruction produced a known value and was modeled,
     *         false otherwise
     */
    private boolean checkForKnownValue(Instruction obj) {
        if (trackValueNumbers) {
            try {
                // See if the value number loaded here is a known value
                ValueNumberFrame vnaFrameAfter = vnaDataflow.getFactAfterLocation(getLocation());
                if (vnaFrameAfter.isValid()) {
                    ValueNumber tosVN = vnaFrameAfter.getTopValue();
                    IsNullValue knownValue = getFrame().getKnownValue(tosVN);
                    if (knownValue != null) {
                        // System.out.println("Produce known value!");
                        // The value produced by this instruction is known.
                        // Push the known value.
                        modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
                        produce(knownValue);
                        return true;
                    }
                }
            } catch (DataflowAnalysisException e) {
                // Ignore...
            }
        }
        return false;
    }

    @Override
    public void visitACONST_NULL(ACONST_NULL obj) {
        produce(IsNullValue.nullValue());
    }

    @Override
    public void visitNEW(NEW obj) {
        produce(IsNullValue.nonNullValue());
    }

    @Override
    public void visitNEWARRAY(NEWARRAY obj) {
        modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
        produce(IsNullValue.nonNullValue());
    }

    @Override
    public void visitANEWARRAY(ANEWARRAY obj) {
        modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
        produce(IsNullValue.nonNullValue());
    }

    @Override
    public void visitMULTIANEWARRAY(MULTIANEWARRAY obj) {
        modelNormalInstruction(obj, getNumWordsConsumed(obj), 0);
        produce(IsNullValue.nonNullValue());
    }

    @Override
    public void visitLDC(LDC obj) {
        produce(IsNullValue.nonNullValue());
    }

    @Override
    public void visitLDC2_W(LDC2_W obj) {
        produce2(IsNullValue.nonNullValue());
    }

    @Override
    public void visitCHECKCAST(CHECKCAST obj) {
        // Do nothing
    }

    @Override
    public void visitINVOKESTATIC(INVOKESTATIC obj) {
        handleInvoke(obj);
    }

    @Override
    public void visitINVOKESPECIAL(INVOKESPECIAL obj) {
        handleInvoke(obj);
    }

    @Override
    public void visitINVOKEINTERFACE(INVOKEINTERFACE obj) {
        handleInvoke(obj);
    }

    @Override
    public void visitINVOKEVIRTUAL(INVOKEVIRTUAL obj) {
        handleInvoke(obj);
    }

}

