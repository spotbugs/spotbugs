package edu.umd.cs.findbugs.classfile.engine.bcel;

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.DominatorsAnalysis;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce DominatorsAnalysis objects for analyzed methods.
 *
 * @author David Hovemeyer
 */
public class NonExceptionDominatorsAnalysisFactory extends AnalysisFactory<NonExceptionDominatorsAnalysis> {
    /**
     * Constructor.
     */
    public NonExceptionDominatorsAnalysisFactory() {
        super("non-exception dominators analysis", NonExceptionDominatorsAnalysis.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public NonExceptionDominatorsAnalysis analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        CFG cfg = getCFG(analysisCache, descriptor);
        DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);
        NonExceptionDominatorsAnalysis analysis = new NonExceptionDominatorsAnalysis(cfg, dfs);
        Dataflow<java.util.BitSet, DominatorsAnalysis> dataflow = new Dataflow<>(cfg,
                analysis);
        dataflow.execute();
        return analysis;
    }
}
