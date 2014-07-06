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

import java.io.PrintStream;

import org.apache.bcel.generic.InstructionHandle;

/**
 * CFGPrinter class which prints dataflow values at each basic block and
 * instruction.
 */
public class DataflowCFGPrinter<Fact, AnalysisType extends DataflowAnalysis<Fact>> extends CFGPrinter {
    private final Dataflow<Fact, AnalysisType> dataflow;

    /**
     * Constructor.
     *
     * @param dataflow
     *            the Dataflow object whose values should be used to annotate
     *            the printed CFG
     */
    public DataflowCFGPrinter(Dataflow<Fact, AnalysisType> dataflow) {
        super(dataflow.getCFG());
        this.dataflow = dataflow;

        setIsForwards(dataflow.getAnalysis().isForwards());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.CFGPrinter#edgeAnnotate(edu.umd.cs.findbugs.ba
     * .Edge)
     */
    @Override
    public String edgeAnnotate(Edge edge) {
        String edgeAnnotation = "";
        try {
            edgeAnnotation = " " + dataflow.getAnalysis().factToString(dataflow.getAnalysis().getFactOnEdge(edge));
        } catch (Throwable e) {
            // ignore
        }
        return edgeAnnotation;
    }

    @Override
    public String blockStartAnnotate(BasicBlock bb) {
        boolean flip = isForwards() != dataflow.getAnalysis().isForwards();
        Fact fact = flip ? dataflow.getResultFact(bb) : dataflow.getStartFact(bb);

        return " " + dataflow.getAnalysis().factToString(fact);
    }

    @Override
    public String blockAnnotate(BasicBlock bb) {
        boolean flip = isForwards() != dataflow.getAnalysis().isForwards();
        Fact fact = flip ? dataflow.getStartFact(bb) : dataflow.getResultFact(bb);

        return " " + dataflow.getAnalysis().factToString(fact);
    }

    @Override
    public String instructionAnnotate(InstructionHandle handle, BasicBlock bb) {
        try {
            boolean flip = isForwards() != dataflow.getAnalysis().isForwards();

            Location loc = new Location(handle, bb);

            Fact fact = flip ? dataflow.getAnalysis().getFactAfterLocation(loc) : dataflow.getAnalysis().getFactAtLocation(loc);
            return " " + dataflow.getAnalysis().factToString(fact);
        } catch (DataflowAnalysisException e) {
            throw new IllegalStateException("Caught exception: " + e.toString());
        }
    }

    /**
     * Print CFG annotated with results from given dataflow analysis.
     *
     * @param <Fact>
     *            Dataflow fact type
     * @param <AnalysisType>
     *            Dataflow analysis type
     * @param dataflow
     *            dataflow driver
     * @param out
     *            PrintStream to use
     */
    public static <Fact, AnalysisType extends BasicAbstractDataflowAnalysis<Fact>> void printCFG(
            Dataflow<Fact, AnalysisType> dataflow, PrintStream out) {
        DataflowCFGPrinter<Fact, AnalysisType> printer = new DataflowCFGPrinter<Fact, AnalysisType>(dataflow);
        printer.print(out);
    }

}

