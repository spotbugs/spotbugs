/*
 * Bytecode Analysis Framework
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.ba.constant;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.FrameDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Location;

/**
 * Dataflow analysis to find constant values.
 *
 * @see edu.umd.cs.findbugs.ba.constant.Constant
 * @author David Hovemeyer
 */
public class ConstantAnalysis extends FrameDataflowAnalysis<Constant, ConstantFrame> {
    private final MethodGen methodGen;

    private final ConstantFrameModelingVisitor visitor;

    public ConstantAnalysis(MethodGen methodGen, DepthFirstSearch dfs) {
        super(dfs);
        this.methodGen = methodGen;
        this.visitor = new ConstantFrameModelingVisitor(methodGen.getConstantPool());
    }

    @Override
    public ConstantFrame createFact() {
        return new ConstantFrame(methodGen.getMaxLocals());
    }

    @Override
    public void initEntryFact(ConstantFrame frame) {
        frame.setValid();
        frame.clearStack();
        int numSlots = frame.getNumSlots();
        for (int i = 0; i < numSlots; ++i) {
            frame.setValue(i, Constant.NOT_CONSTANT);
        }
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ConstantFrame frame)
            throws DataflowAnalysisException {
        visitor.setFrameAndLocation(frame, new Location(handle, basicBlock));
        visitor.analyzeInstruction(handle.getInstruction());
    }

    @Override
    public void meetInto(ConstantFrame fact, Edge edge, ConstantFrame result) throws DataflowAnalysisException {

        if (fact.isValid()) {
            ConstantFrame tmpFact = null;

            if (edge.isExceptionEdge()) {
                tmpFact = modifyFrame(fact, null);
                tmpFact.clearStack();
                tmpFact.pushValue(Constant.NOT_CONSTANT);
            }

            if (tmpFact != null) {
                fact = tmpFact;
            }
        }

        mergeInto(fact, result);
    }

    @Override
    protected void mergeValues(ConstantFrame otherFrame, ConstantFrame resultFrame, int slot) throws DataflowAnalysisException {
        Constant value = Constant.merge(resultFrame.getValue(slot), otherFrame.getValue(slot));
        resultFrame.setValue(slot, value);
    }

    // /*
    // * Test driver.
    // */
    // public static void main(String[] argv) throws Exception {
    // if (argv.length != 1) {
    // System.err.println("Usage: " + ConstantAnalysis.class.getName() +
    // " <class file>");
    // System.exit(1);
    // }
    //
    // DataflowTestDriver<ConstantFrame, ConstantAnalysis> driver =
    // new DataflowTestDriver<ConstantFrame, ConstantAnalysis>() {
    // @Override
    // public Dataflow<ConstantFrame, ConstantAnalysis> createDataflow(
    // ClassContext classContext,
    // Method method) throws CFGBuilderException, DataflowAnalysisException {
    // return classContext.getConstantDataflow(method);
    // }
    // };
    //
    // driver.execute(argv[0]);
    // }
}
