/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

//import org.apache.bcel.classfile.Method;
import javax.annotation.CheckForNull;

import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ObjectType;


/**
 * Dataflow analysis to determine the nesting of catch and finally blocks within
 * a method.
 *
 * @see BlockType
 * @author David Hovemeyer
 */
public class BlockTypeAnalysis extends BasicAbstractDataflowAnalysis<BlockType> {
    private final DepthFirstSearch dfs;

    /**
     * Constructor.
     *
     * @param dfs
     *            a DepthFirstSearch for the method to be analyzed
     */
    public BlockTypeAnalysis(DepthFirstSearch dfs) {
        this.dfs = dfs;
    }

    @Override
    public BlockType createFact() {
        return new BlockType();
    }

    @Override
    public void copy(BlockType source, BlockType dest) {
        dest.copyFrom(source);
    }

    @Override
    public void initEntryFact(BlockType result) throws DataflowAnalysisException {
        result.setNormal();
    }

    @Override
    public void makeFactTop(BlockType fact) {
        fact.setTop();
    }

    @Override
    public boolean isTop(BlockType fact) {
        return fact.isTop();
    }

    @Override
    public boolean isForwards() {
        return true;
    }

    @Override
    public BlockOrder getBlockOrder(CFG cfg) {
        return new ReversePostOrder(cfg, dfs);
    }

    @Override
    public boolean same(BlockType fact1, BlockType fact2) {
        return fact1.sameAs(fact2);
    }

    @Override
    public void transfer(BasicBlock basicBlock, @CheckForNull InstructionHandle end, BlockType start, BlockType result)
            throws DataflowAnalysisException {
        result.copyFrom(start);

        if (start.isValid()) {
            if (basicBlock.isExceptionHandler()) {
                CodeExceptionGen exceptionGen = basicBlock.getExceptionGen();
                ObjectType catchType = exceptionGen.getCatchType();
                if (catchType == null) {
                    // Probably a finally block, or a synchronized block
                    // exception-compensation catch block.
                    result.pushFinally();
                } else {
                    // Catch type was explicitly specified:
                    // this is probably a programmer-written catch block
                    result.pushCatch();
                }
            }
        }
    }

    @Override
    public void meetInto(BlockType fact, Edge edge, BlockType result) throws DataflowAnalysisException {
        result.mergeWith(fact);
    }

    // public static void main(String[] argv) throws Exception {
    // if (argv.length != 1) {
    // System.err.println("Usage: " + BlockTypeAnalysis.class.getName() +
    // " <classfile>");
    // System.exit(1);
    // }
    //
    // DataflowTestDriver<BlockType, BlockTypeAnalysis> driver = new
    // DataflowTestDriver<BlockType, BlockTypeAnalysis>() {
    // /* (non-Javadoc)
    // * @see
    // edu.umd.cs.findbugs.ba.DataflowTestDriver#createDataflow(edu.umd.cs.findbugs.ba.ClassContext,
    // org.apache.bcel.classfile.Method)
    // */
    // @Override
    // public Dataflow<BlockType, BlockTypeAnalysis> createDataflow(ClassContext
    // classContext, Method method) throws CFGBuilderException,
    // DataflowAnalysisException {
    // return classContext.getBlockTypeDataflow(method);
    // }
    // };
    //
    // driver.execute(argv[0]);
    // }
}

