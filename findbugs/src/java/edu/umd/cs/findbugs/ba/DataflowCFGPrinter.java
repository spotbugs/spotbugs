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
 * CFGPrinter class which prints dataflow values at
 * each basic block and instruction.
 */
public class DataflowCFGPrinter <Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> extends CFGPrinter {
	private Dataflow<Fact, AnalysisType> dataflow;
	private AnalysisType analysis;

	public DataflowCFGPrinter(CFG cfg, Dataflow<Fact, AnalysisType> dataflow, AnalysisType analysis) {
		super(cfg);
		this.dataflow = dataflow;
		this.analysis = analysis;

		setIsForwards(analysis.isForwards());
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.CFGPrinter#edgeAnnotate(edu.umd.cs.findbugs.ba.Edge)
	 */
	@Override
	public String edgeAnnotate(Edge edge) {
		String edgeAnnotation= "";
		try {
			edgeAnnotation = " " + analysis.factToString(analysis.getFactOnEdge(edge)); 
		} catch (Throwable e) {
			// ignore
		}
		return edgeAnnotation;
	}

	@Override
	public String blockStartAnnotate(BasicBlock bb) {
		return " " + analysis.factToString(dataflow.getStartFact(bb));
	}

	@Override
	public String blockAnnotate(BasicBlock bb) {
		return " " + analysis.factToString(dataflow.getResultFact(bb));
	}

	@Override
	public String instructionAnnotate(InstructionHandle handle, BasicBlock bb) {
		try {
			Fact result = analysis.getFactAtLocation(new Location(handle, bb));
			return " " + analysis.factToString(result);
		} catch (DataflowAnalysisException e) {
			throw new IllegalStateException("Caught exception: " + e.toString());
		}
	}

	/**
	 * Print CFG annotated with results from given dataflow analysis.
	 * 
	 * @param <Fact>         Dataflow fact type
	 * @param <AnalysisType> Dataflow analysis type
	 * @param cfg            control flow graph
	 * @param dataflow       dataflow driver
	 * @param analysis       dataflow analysis
	 * @param out            PrintStream to use
	 */
	private static<Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>>
	void printCFG(CFG cfg, Dataflow<Fact, AnalysisType> dataflow, AnalysisType analysis, PrintStream out) {
		DataflowCFGPrinter<Fact, AnalysisType> printer =
			new DataflowCFGPrinter<Fact, AnalysisType>(cfg, dataflow, analysis);
		printer.print(out);
	}
	
}

// vim:ts=4
