/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.Iterator;

import javax.annotation.CheckForNull;
import javax.annotation.meta.When;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.ConstantPushInstruction;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LDC;
import org.apache.bcel.generic.LDC2_W;
import org.apache.bcel.generic.LocalVariableInstruction;

import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReversePostOrder;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * Forward type qualifier dataflow analysis.
 *
 * @author David Hovemeyer
 */
public class ForwardTypeQualifierDataflowAnalysis extends TypeQualifierDataflowAnalysis {
    private final DepthFirstSearch dfs;

    /**
     * Constructor.
     *
     * @param dfs
     *            DepthFirstSearch on the analyzed method
     * @param xmethod
     *            XMethod for the analyzed method
     * @param cfg
     *            CFG of the analyzed method
     * @param vnaDataflow
     *            ValueNumberDataflow on the analyzed method
     * @param cpg
     *            ConstantPoolGen of the analyzed method
     * @param typeQualifierValue
     *            TypeQualifierValue representing type qualifier the analysis
     *            should check
     */
    public ForwardTypeQualifierDataflowAnalysis(DepthFirstSearch dfs, XMethod xmethod, CFG cfg, ValueNumberDataflow vnaDataflow,
            ConstantPoolGen cpg, TypeQualifierValue<?> typeQualifierValue) {
        super(xmethod, cfg, vnaDataflow, cpg, typeQualifierValue);
        this.dfs = dfs;
    }

    @Override
    public BlockOrder getBlockOrder(CFG cfg1) {
        return new ReversePostOrder(cfg1, dfs);
    }

    @Override
    public boolean isForwards() {
        return true;
    }

    @Override
    public void registerSourceSinkLocations() throws DataflowAnalysisException {
        registerParameterSources();
        registerInstructionSources();
    }

    private void registerInstructionSources() throws DataflowAnalysisException {
        for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
            Location location = i.next();
            Instruction instruction = location.getHandle().getInstruction();
            short opcode = instruction.getOpcode();

            int produces = instruction.produceStack(cpg);
            if (instruction instanceof InvokeInstruction) {
                // Model return value
                registerReturnValueSource(location);
            } else if (opcode == Constants.GETFIELD || opcode == Constants.GETSTATIC) {
                // Model field loads
                registerFieldLoadSource(location);
            } else if (instruction instanceof LDC) {
                // Model constant values
                registerLDCValueSource(location);
            } else if (instruction instanceof LDC2_W) {
                // Model constant values
                registerLDC2ValueSource(location);
            } else if (instruction instanceof ConstantPushInstruction) {
                // Model constant values
                registerConstantPushSource(location);
            } else if (instruction instanceof ACONST_NULL) {
                // Model constant values
                registerPushNullSource(location);
            } else  if ((produces == 1 || produces == 2) && !(instruction instanceof LocalVariableInstruction) && !(instruction instanceof CHECKCAST)){
                // Model other sources
                registerOtherSource(location);
            }
        }
    }

    private void registerLDCValueSource(Location location) throws DataflowAnalysisException {

        LDC instruction = (LDC) location.getHandle().getInstruction();
        Object constantValue = instruction.getValue(cpg);
        registerConstantSource(location, constantValue);
    }
    private void registerLDC2ValueSource(Location location) throws DataflowAnalysisException {

        LDC2_W instruction = (LDC2_W) location.getHandle().getInstruction();
        Object constantValue = instruction.getValue(cpg);
        registerConstantSource(location, constantValue);
    }
    private void registerPushNullSource(Location location) throws DataflowAnalysisException {
        registerConstantSource(location, null);
    }
    private void registerConstantSource(Location location,  @CheckForNull Object constantValue) throws DataflowAnalysisException {

        When w;
        if (typeQualifierValue.canValidate(constantValue)) {
            w = typeQualifierValue.validate(constantValue);
        } else if (typeQualifierValue.isStrictQualifier()) {
            return;
        } else {
            w = When.UNKNOWN;
        }

        registerTopOfStackSource(SourceSinkType.CONSTANT_VALUE, location, w, false, constantValue);
    }
    private void registerOtherSource(Location location) throws DataflowAnalysisException {

        registerTopOfStackSource(SourceSinkType.OTHER, location, When.UNKNOWN, false, null);
    }

    private void registerConstantPushSource(Location location) throws DataflowAnalysisException {

        ConstantPushInstruction instruction = (ConstantPushInstruction) location.getHandle().getInstruction();
        Number constantValue = instruction.getValue();
        registerConstantSource(location, constantValue);
    }
    private void registerReturnValueSource(Location location) throws DataflowAnalysisException {
        // Nothing to do if called method does not return a value
        InvokeInstruction inv = (InvokeInstruction) location.getHandle().getInstruction();
        String calledMethodSig = inv.getSignature(cpg);
        if (calledMethodSig.endsWith(")V")) {
            return;
        }

        XMethod calledXMethod = XFactory.createXMethod(inv, cpg);
        if (TypeQualifierDataflowAnalysis.isIdentifyFunctionForTypeQualifiers(calledXMethod)) {
            return;
        }

        if (calledXMethod.isResolved()) {
            TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(calledXMethod,
                    typeQualifierValue);

            boolean interproc = false;
            if (TypeQualifierDatabase.USE_DATABASE && tqa == null) {
                // See if there's an entry in the interprocedural
                // type qualifier database.
                TypeQualifierDatabase tqdb = Global.getAnalysisCache().getDatabase(TypeQualifierDatabase.class);
                tqa = tqdb.getReturnValue(calledXMethod.getMethodDescriptor(), typeQualifierValue);
                if (tqa != null) {
                    interproc = true;
                }
            }

            When when = (tqa != null) ? tqa.when : When.UNKNOWN;
            registerTopOfStackSource(SourceSinkType.RETURN_VALUE_OF_CALLED_METHOD, location, when, interproc, null);
        }
    }

    private void registerFieldLoadSource(Location location) throws DataflowAnalysisException {
        XField loadedField = XFactory.createXField((FieldInstruction) location.getHandle().getInstruction(), cpg);
        if (loadedField.isResolved()) {
            TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(loadedField,
                    typeQualifierValue);
            When when = (tqa != null) ? tqa.when : When.UNKNOWN;
            registerTopOfStackSource(SourceSinkType.FIELD_LOAD, location, when, false, null);
        }

    }

    private void registerTopOfStackSource(SourceSinkType sourceSinkType, Location location, When when, boolean interproc,
            @CheckForNull Object constantValue) throws DataflowAnalysisException {
        if (when == When.UNKNOWN && !typeQualifierValue.isStrictQualifier()) {
            return;
        }
        ValueNumberFrame vnaFrameAfterInstruction = vnaDataflow.getFactAfterLocation(location);
        if (vnaFrameAfterInstruction.isValid()) {
            ValueNumber tosValue = vnaFrameAfterInstruction.getTopValue();
            SourceSinkInfo sourceSinkInfo = new SourceSinkInfo(sourceSinkType, location, tosValue, when);
            sourceSinkInfo.setInterproc(interproc);
            sourceSinkInfo.setConstantValue(constantValue);
            registerSourceSink(sourceSinkInfo);
        }
    }

    private void registerParameterSources() {
        ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg.getEntry());

        SignatureParser sigParser = new SignatureParser(xmethod.getSignature());
        int firstParamSlot = xmethod.isStatic() ? 0 : 1;

        int param = 0;
        int slotOffset = 0;

        for ( String paramSig : sigParser.parameterSignatures()) {

            // Get the TypeQualifierAnnotation for this parameter
            SourceSinkInfo info;
            TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xmethod, param,
                    typeQualifierValue);
            When when = (tqa != null) ? tqa.when : When.UNKNOWN;
            ValueNumber vn = vnaFrameAtEntry.getValue(slotOffset + firstParamSlot);
            info = new SourceSinkInfo(SourceSinkType.PARAMETER, cfg.getLocationAtEntry(), vn, when);
            info.setParameterAndLocal(param, slotOffset + firstParamSlot);
            registerSourceSink(info);

            param++;
            slotOffset += SignatureParser.getNumSlotsForType(paramSig);
        }
    }

    @Override
    protected void propagateAcrossPhiNode(TypeQualifierValueSet fact, ValueNumber sourceVN, ValueNumber targetVN) {
        // Forward analysis - propagate from source to target
        fact.propagateAcrossPhiNode(sourceVN, targetVN);
    }
}
