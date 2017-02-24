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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Abstract base class for type qualifier dataflow analyses.
 *
 * @author David Hovemeyer
 */
public abstract class TypeQualifierDataflowAnalysis extends AbstractDataflowAnalysis<TypeQualifierValueSet> {

    static String primitiveType (String simpleClass) {
        if ("Integer".equals(simpleClass)) {
            return "int";
        }
        return  simpleClass.toLowerCase();
    }

    static boolean isIdentifyFunctionForTypeQualifiers(XMethod m) {
        String className = m.getClassName();
        if (!className.startsWith("java.lang")) {
            return false;
        }
        String methodName = m.getName();

        if (m.isStatic()) {
            if (!"valueOf".equals(methodName)) {
                return false;
            }
            String signature = m.getSignature();
            if (signature.charAt(2) != ')') {
                return false;
            }
        } else {
            String simpleClassName = ClassName.extractSimpleName(className);

            if (!methodName.equals(primitiveType(simpleClassName) + "Value")) {
                return false;
            }
            String signature = m.getSignature();
            if (signature.charAt(1) != ')') {
                return false;
            }
        }
        return true;
    }


    static final boolean DEBUG_VERBOSE = SystemProperties.getBoolean("ctq.dataflow.debug.verbose");

    protected final XMethod xmethod;

    protected final CFG cfg;

    protected final ValueNumberDataflow vnaDataflow;

    protected final TypeQualifierValue<?> typeQualifierValue;

    protected final ConstantPoolGen cpg;

    private final Map<Location, Set<SourceSinkInfo>> sourceSinkMap;

    /**
     * Constructor.
     *
     * @param xmethod
     *            XMethod object containing information about the method being
     *            analyzed
     * @param cfg
     *            the control-flow graph (CFG) of the method being analyzed
     * @param vnaDataflow
     *            ValueNumberDataflow for the method
     * @param cpg
     *            the constant pool being built up
     * @param typeQualifierValue
     *            the TypeQualifierValue we want the dataflow analysis to check
     */
    public TypeQualifierDataflowAnalysis(XMethod xmethod, CFG cfg, ValueNumberDataflow vnaDataflow, ConstantPoolGen cpg,
            TypeQualifierValue<?> typeQualifierValue) {
        this.xmethod = xmethod;
        this.cfg = cfg;
        this.vnaDataflow = vnaDataflow;
        this.cpg = cpg;
        this.typeQualifierValue = typeQualifierValue;
        this.sourceSinkMap = new HashMap<Location, Set<SourceSinkInfo>>();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
     */
    @Override
    public void initEntryFact(TypeQualifierValueSet result) throws DataflowAnalysisException {
        result.makeValid();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#isFactValid(java.lang
     * .Object)
     */
    @Override
    public boolean isFactValid(TypeQualifierValueSet fact) {
        return fact.isValid();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public void copy(TypeQualifierValueSet source, TypeQualifierValueSet dest) {
        dest.makeSameAs(source);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#createFact()
     */
    @Override
    public TypeQualifierValueSet createFact() {
        return new TypeQualifierValueSet(typeQualifierValue);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isTop(java.lang.Object)
     */
    @Override
    public boolean isTop(TypeQualifierValueSet fact) {
        return fact.isTop();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(java.lang.Object)
     */
    @Override
    public void makeFactTop(TypeQualifierValueSet fact) {
        fact.setTop();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(java.lang.Object,
     * edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
     */
    @Override
    public void meetInto(TypeQualifierValueSet fact, Edge edge, TypeQualifierValueSet result) throws DataflowAnalysisException {
        if (fact.isTop() || result.isBottom()) {
            // result does not change
            return;
        } else if (fact.isBottom() || result.isTop()) {
            result.makeSameAs(fact);
            return;
        }

        assert fact.isValid();
        assert result.isValid();

        result.mergeWith(fact);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(java.lang.Object,
     * java.lang.Object)
     */
    @Override
    public boolean same(TypeQualifierValueSet fact1, TypeQualifierValueSet fact2) {
        return fact1.equals(fact2);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.BasicAbstractDataflowAnalysis#edgeTransfer(edu
     * .umd.cs.findbugs.ba.Edge, java.lang.Object)
     */
    @Override
    public void edgeTransfer(Edge edge, TypeQualifierValueSet fact) throws DataflowAnalysisException {
        if (!fact.isValid()) {
            return;
        }

        // Propagate flow values and source information across phi nodes.

        ValueNumberFrame targetVnaFrame = vnaDataflow.getStartFact(edge.getTarget());
        ValueNumberFrame sourceVnaFrame = vnaDataflow.getResultFact(edge.getSource());

        if (!targetVnaFrame.isValid() || !sourceVnaFrame.isValid()) {
            return;
        }

        // The source and target frames can have different numbers of slots
        // if the target is an exception handler.
        // So, merge the minimum number of slots in either frame.
        int numSlotsToMerge = Math.min(sourceVnaFrame.getNumSlots(), targetVnaFrame.getNumSlots());

        for (int i = 0; i < numSlotsToMerge; i++) {
            ValueNumber targetVN = targetVnaFrame.getValue(i);
            ValueNumber sourceVN = sourceVnaFrame.getValue(i);

            if (!targetVN.equals(sourceVN) && targetVN.hasFlag(ValueNumber.PHI_NODE)) {
                // targetVN is a phi result
                if (DEBUG_VERBOSE) {
                    System.out.println("Phi node: " + fact.valueNumberToString(sourceVN) + " -> "
                            + fact.valueNumberToString(targetVN));
                }
                propagateAcrossPhiNode(fact, sourceVN, targetVN);
                if (DEBUG_VERBOSE) {
                    String dir = isForwards() ? "forwards" : "backwards";
                    System.out.println("After propagating phi node " + dir + ": " + fact.toString());
                }
            }
        }
    }

    protected abstract void propagateAcrossPhiNode(TypeQualifierValueSet fact, ValueNumber sourceVN, ValueNumber targetVN);

    /**
     * This method must be called before the dataflow analysis is executed.
     *
     * @throws DataflowAnalysisException
     */
    public abstract void registerSourceSinkLocations() throws DataflowAnalysisException;

    protected void registerSourceSink(SourceSinkInfo sourceSinkInfo) {
        Set<SourceSinkInfo> set = sourceSinkMap.get(sourceSinkInfo.getLocation());
        if (set == null) {
            set = new HashSet<SourceSinkInfo>();
            sourceSinkMap.put(sourceSinkInfo.getLocation(), set);
        }
        set.add(sourceSinkInfo);
    }

    /**
     * Get the set of SourceSinkInfo objects representing sources/sinks at a
     * given Location.
     *
     * @param location
     *            a Location
     * @return Set of SourceSinkInfo objects representing sources/sinks at the
     *         Location
     */
    public Set<SourceSinkInfo> getSourceSinkInfoSet(Location location) {
        Set<SourceSinkInfo> result = sourceSinkMap.get(location);
        return result != null ? result : Collections.<SourceSinkInfo> emptySet();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis#transferInstruction(org
     * .apache.bcel.generic.InstructionHandle,
     * edu.umd.cs.findbugs.ba.BasicBlock, java.lang.Object)
     */
    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, TypeQualifierValueSet fact)
            throws DataflowAnalysisException {
        if (!fact.isValid()) {
            return;
        }
        Location location = new Location(handle, basicBlock);

        Instruction i = handle.getInstruction();
        if (i instanceof InvokeInstruction) {
            InvokeInstruction ii = (InvokeInstruction) i;
            XMethod m = XFactory.createXMethod(ii, cpg);
            if (TypeQualifierDataflowAnalysis.isIdentifyFunctionForTypeQualifiers(m)) {
                ValueNumberFrame vnaFrameAtLocation = vnaDataflow.getFactAtLocation(location);
                ValueNumberFrame vnaFrameAfterInstruction = vnaDataflow.getFactAfterLocation(location);
                ValueNumber in = vnaFrameAtLocation.getStackValue(0);
                ValueNumber out = vnaFrameAfterInstruction.getStackValue(0);
                fact.copyInfo(in, out);
                return;
            }
        }

        // This is a simple process.
        // Check to see if there are any sources/sinks at this location,
        // and if so, model them.

        Set<SourceSinkInfo> sourceSinkSet = sourceSinkMap.get(location);
        if (sourceSinkSet != null) {
            if (DEBUG_VERBOSE) {
                System.out.println("Modeling source/sink at " + location.toCompactString() + ": " + sourceSinkSet.toString());
            }
            for (SourceSinkInfo sourceSinkInfo : sourceSinkSet) {
                fact.modelSourceSink(sourceSinkInfo);
            }
        }
    }
}
