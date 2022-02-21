package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.ValueRangeAnalysis;
import edu.umd.cs.findbugs.ba.ValueRangeDataflow;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class ValueRangeDataflowFactory extends AnalysisFactory<ValueRangeDataflow> {
    /**
     * Constructor.
     */
    public ValueRangeDataflowFactory() {
        super("ValueRange set analysis", ValueRangeDataflow.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public ValueRangeDataflow analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        MethodGen methodGen = getMethodGen(analysisCache, descriptor);
        if (methodGen == null) {
            throw new MethodUnprofitableException(descriptor);
        }
        CFG cfg = getCFG(analysisCache, descriptor);
        DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);

        ValueRangeAnalysis analysis = new ValueRangeAnalysis(descriptor, dfs);
        ValueRangeDataflow dataflow = new ValueRangeDataflow(cfg, analysis);
        dataflow.execute();
        return dataflow;

    }
}
