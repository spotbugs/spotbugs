/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.*;
import java.io.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A test driver for dataflow analysis classes.
 * It runs the dataflow analysis on the methods of a single class,
 * and has options (properties) to restrict the analysis to a single
 * method, and to print out a CFG annotated with dataflow values.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @author David Hovemeyer
 */
public abstract class DataflowTestDriver<Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> {

	private static class DataflowCFGPrinter<Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> extends CFGPrinter {
		private Dataflow<Fact, AnalysisType> dataflow;
		private AnalysisType analysis;

		public DataflowCFGPrinter(CFG cfg, Dataflow<Fact, AnalysisType> dataflow, AnalysisType analysis) {
			super(cfg);
			this.dataflow = dataflow;
			this.analysis = analysis;
		}

		public String blockStartAnnotate(BasicBlock bb) {
			return " " + analysis.factToString(dataflow.getStartFact(bb));
		}

		public String blockAnnotate(BasicBlock bb) {
			return " " + analysis.factToString(dataflow.getResultFact(bb));
		}

		public String instructionAnnotate(InstructionHandle handle, BasicBlock bb) {
			Fact result = analysis.getFactAtLocation(new Location(handle, bb));
			return " " + analysis.factToString(result);
		}
	}

	/**
	 * Execute the analysis on a single class.
	 * @param filename the name of the class file
	 */
	public void execute(String filename) throws DataflowAnalysisException, CFGBuilderException, IOException {
		JavaClass jclass = new RepositoryClassParser(filename).parse();
		ClassGen cg = new ClassGen(jclass);
		ConstantPoolGen cpg = cg.getConstantPool();
		String methodName = System.getProperty("dataflow.method");

		Method[] methods = cg.getMethods();
		for (int i = 0; i < methods.length; ++i) {
			Method method = methods[i];
			if (method.isAbstract() || method.isNative())
				continue;
			if (methodName != null && !method.getName().equals(methodName))
				continue;

			MethodGen methodGen = new MethodGen(method, jclass.getClassName(), cpg);

			System.out.println("-----------------------------------------------------------------");
			System.out.println("Method: " + SignatureConverter.convertMethodSignature(methodGen));
			System.out.println("-----------------------------------------------------------------");

			CFGBuilder cfgBuilder = CFGBuilderFactory.create(methodGen);
			cfgBuilder.build();
			CFG cfg = cfgBuilder.getCFG();
			cfg.assignEdgeIds(0);

			execute(methodGen, cfg);
		}
	}

	/**
	 * Execute the analysis on a single method of a class.
	 */
	public void execute(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException, CFGBuilderException {

		AnalysisType analysis = createAnalysis(methodGen, cfg);
		Dataflow<Fact, AnalysisType> dataflow = new Dataflow<Fact, AnalysisType>(cfg, analysis);

		dataflow.execute();

		System.out.println("Finished in " + dataflow.getNumIterations() + " iterations");

		examineResults(cfg, dataflow);

		if (Boolean.getBoolean("dataflow.printcfg")) {
			CFGPrinter p = new DataflowCFGPrinter<Fact, AnalysisType>(cfg, dataflow, analysis);
			p.print(System.out);
		}
	}

	/**
	 * Downcall method to create the dataflow analysis.
	 * @param methodGen the method to be analyzed
	 * @param cfg control flow graph of the method to be analyzed
	 */
	public abstract AnalysisType createAnalysis(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException;

	/**
	 * Downcall method to inspect the analysis results.
	 * Need not be implemented by subclasses.
	 * @param cfg the control flow graph
	 * @param dataflow the analysis results
	 */
	public void examineResults(CFG cfg, Dataflow<Fact, AnalysisType> dataflow) {
	}
}

// vim:ts=4
