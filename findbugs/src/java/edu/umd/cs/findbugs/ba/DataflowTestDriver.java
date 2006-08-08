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

import java.io.IOException;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.SystemProperties;

/**
 * A test driver for dataflow analysis classes.
 * It runs the dataflow analysis on the methods of a single class,
 * and has options (properties) to restrict the analysis to a single
 * method, and to print out a CFG annotated with dataflow values.
 *
 * @author David Hovemeyer
 * @see Dataflow
 * @see DataflowAnalysis
 */
public abstract class DataflowTestDriver <Fact, AnalysisType extends AbstractDataflowAnalysis<Fact>> {
	private boolean overrideIsForwards;
	
	public void overrideIsForwards() {
		this.overrideIsForwards = true;
	}

	/**
	 * Execute the analysis on a single class.
	 *
	 * @param filename the name of the class file
	 */
	public void execute(String filename) throws DataflowAnalysisException, CFGBuilderException, IOException {
		JavaClass jclass = new RepositoryClassParser(filename).parse();

		final RepositoryLookupFailureCallback lookupFailureCallback = new DebugRepositoryLookupFailureCallback();

		AnalysisContext analysisContext = AnalysisContext.create(lookupFailureCallback);
		analysisContext.setBoolProperty(AnalysisFeatures.ACCURATE_EXCEPTIONS, true);
		
		configureAnalysisContext(analysisContext);

		ClassContext classContext = analysisContext.getClassContext(jclass);
		String methodName = SystemProperties.getProperty("dataflow.method");

		Method[] methods = jclass.getMethods();
		for (Method method : methods) {
			if (methodName != null && !method.getName().equals(methodName))
				continue;

			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			System.out.println("-----------------------------------------------------------------");
			System.out.println("Method: " + SignatureConverter.convertMethodSignature(methodGen));
			System.out.println("-----------------------------------------------------------------");

			execute(classContext, method);
		}
	}
	
	static class Knob {
		String systemPropertyName;
		int analysisProperty;
		
		Knob(String systemPropertyName, int analysisProperty) {
			this.systemPropertyName = systemPropertyName;
			this.analysisProperty = analysisProperty;
		}
	}
	
	private static final Knob[] KNOB_LIST = {
		new Knob("ta.instanceof", AnalysisFeatures.MODEL_INSTANCEOF),
		new Knob("inva.trackvalues", AnalysisFeatures.TRACK_VALUE_NUMBERS_IN_NULL_POINTER_ANALYSIS),
		new Knob("fnd.derefs", AnalysisFeatures.TRACK_GUARANTEED_VALUE_DEREFS_IN_NULL_POINTER_ANALYSIS),
	};

	/**
	 * Configure the analysis context.
	 * 
	 * @param analysisContext
	 */
	private void configureAnalysisContext(AnalysisContext analysisContext) {
		boolean max = SystemProperties.getBoolean("dataflow.max");

		for (Knob knob : KNOB_LIST) {
			boolean enable = max ? true : SystemProperties.getBoolean(knob.systemPropertyName); 
			System.out.println("Setting " + knob.systemPropertyName + "=" + enable);
			analysisContext.setBoolProperty(knob.analysisProperty, enable);
		}
	}

	/**
	 * Execute the analysis on a single method of a class.
	 */
	public void execute(ClassContext classContext, Method method) throws DataflowAnalysisException, CFGBuilderException {

		Dataflow<Fact, AnalysisType> dataflow = createDataflow(classContext, method);
		System.out.println("Finished in " + dataflow.getNumIterations() + " iterations");

		CFG cfg = classContext.getCFG(method);
		examineResults(cfg, dataflow);

		if (SystemProperties.getBoolean("dataflow.printcfg")) {
			CFGPrinter p = new DataflowCFGPrinter<Fact, AnalysisType>(cfg, dataflow, dataflow.getAnalysis());
			if (overrideIsForwards) {
				p.setIsForwards(!p.isForwards());
			}
			p.print(System.out);
		}
	}

	/**
	 * Downcall method to create the dataflow driver object
	 * and execute the analysis.
	 *
	 * @param classContext ClassContext for the class
	 * @param method       the Method
	 * @return the Dataflow driver
	 */
	public abstract Dataflow<Fact, AnalysisType> createDataflow(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException;

	/**
	 * Downcall method to inspect the analysis results.
	 * Need not be implemented by subclasses.
	 *
	 * @param cfg      the control flow graph
	 * @param dataflow the analysis results
	 */
	public void examineResults(CFG cfg, Dataflow<Fact, AnalysisType> dataflow) {
	}
}

// vim:ts=4
