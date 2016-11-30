/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

/**
 * A really simple forward dataflow analysis to find the depth of the Java
 * operand stack. This is more of a proof of concept for the dataflow analysis
 * framework than anything useful.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 */
public class StackDepthAnalysis extends ForwardDataflowAnalysis<StackDepth> {
    public static final int TOP = -1;

    public static final int BOTTOM = -2;

    private final ConstantPoolGen cpg;

    /**
     * Constructor.
     *
     * @param cpg
     *            the ConstantPoolGen of the method whose CFG we're performing
     *            the analysis on
     * @param dfs
     *            DepthFirstSearch of the method's CFG
     */
    public StackDepthAnalysis(ConstantPoolGen cpg, DepthFirstSearch dfs) {
        super(dfs);
        this.cpg = cpg;
    }

    @Override
    public StackDepth createFact() {
        return new StackDepth(TOP);
    }

    @Override
    public void makeFactTop(StackDepth fact) {
        fact.setDepth(TOP);
    }

    @Override
    public boolean isTop(StackDepth fact) {
        return fact.getDepth() == TOP;
    }

    @Override
    public boolean isFactValid(StackDepth fact) {
        int depth = fact.getDepth();
        return depth != TOP && depth != BOTTOM;
    }

    @Override
    public void copy(StackDepth source, StackDepth dest) {
        dest.setDepth(source.getDepth());
    }

    @Override
    public void initEntryFact(StackDepth entryFact) {
        entryFact.setDepth(0); // stack depth == 0 at entry to CFG
    }

    @Override
    public boolean same(StackDepth fact1, StackDepth fact2) {
        return fact1.getDepth() == fact2.getDepth();
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, StackDepth fact)
            throws DataflowAnalysisException {
        Instruction ins = handle.getInstruction();
        int produced = ins.produceStack(cpg);
        int consumed = ins.consumeStack(cpg);
        if (produced == Constants.UNPREDICTABLE || consumed == Constants.UNPREDICTABLE) {
            throw new IllegalStateException("Unpredictable stack delta for instruction: " + handle);
        }
        int depth = fact.getDepth();
        depth += (produced - consumed);
        if (depth < 0) {
            fact.setDepth(BOTTOM);
        } else {
            fact.setDepth(depth);
        }
    }

    @Override
    public void meetInto(StackDepth fact, Edge edge, StackDepth result) {
        int a = fact.getDepth();
        int b = result.getDepth();
        int combined;

        if (a == TOP) {
            combined = b;
        } else if (b == TOP) {
            combined = a;
        } else if (a == BOTTOM || b == BOTTOM || a != b) {
            combined = BOTTOM;
        } else {
            combined = a;
        }

        result.setDepth(combined);
    }

    // /**
    // * Command line driver, for testing.
    // */
    // public static void main(String[] argv) throws Exception {
    // if (argv.length != 1) {
    // System.out.println("Usage: " + StackDepthAnalysis.class.getName() +
    // " <class file>");
    // System.exit(1);
    // }
    //
    // DataflowTestDriver<StackDepth, StackDepthAnalysis> driver = new
    // DataflowTestDriver<StackDepth, StackDepthAnalysis>() {
    // @Override
    // public Dataflow<StackDepth, StackDepthAnalysis>
    // createDataflow(ClassContext classContext, Method method)
    // throws CFGBuilderException, DataflowAnalysisException {
    //
    // DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
    // CFG cfg = classContext.getCFG(method);
    //
    // StackDepthAnalysis analysis = new
    // StackDepthAnalysis(classContext.getConstantPoolGen(), dfs);
    // Dataflow<StackDepth, StackDepthAnalysis> dataflow = new
    // Dataflow<StackDepth, StackDepthAnalysis>(cfg, analysis);
    // dataflow.execute();
    //
    // return dataflow;
    // }
    // };
    //
    // driver.execute(argv[0]);
    // }
}

