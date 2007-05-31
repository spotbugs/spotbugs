package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.constant.ConstantAnalysis;
import edu.umd.cs.findbugs.ba.constant.ConstantDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce ConstantDataflow objects for an analyzed method.
 * 
 * @author David Hovemeyer
 */
public class ConstantDataflowFactory extends AnalysisFactory<ConstantDataflow> {
	public ConstantDataflowFactory() {
		super("constant propagation analysis", ConstantDataflow.class);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
		MethodGen methodGen = getMethodGen(analysisCache, descriptor);
		if (methodGen == null) return null;
		ConstantAnalysis analysis = new ConstantAnalysis(
				methodGen,
				getDepthFirstSearch(analysisCache, descriptor)
		);
		ConstantDataflow dataflow = new ConstantDataflow(getCFG(analysisCache, descriptor), analysis);
		dataflow.execute();

		return dataflow;
	}
}
