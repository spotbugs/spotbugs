package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.ba.BlockTypeAnalysis;
import edu.umd.cs.findbugs.ba.BlockTypeDataflow;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine for producing BlockTypeDataflow for an analyzed method.
 * 
 * @author David Hovemeyer
 */
public class BlockTypeAnalysisFactory extends AnalysisFactory<BlockTypeDataflow> {
	public BlockTypeAnalysisFactory() {
		super("block type analysis", BlockTypeDataflow.class);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
	 */
	public Object analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
		CFG cfg = getCFG(analysisCache, descriptor);
		DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);
		BlockTypeAnalysis analysis = new BlockTypeAnalysis(dfs);
		BlockTypeDataflow dataflow = new BlockTypeDataflow(cfg, analysis);
		dataflow.execute();
		return dataflow;
	}
}
