package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.classfile.Method;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ca.CallListAnalysis;
import edu.umd.cs.findbugs.ba.ca.CallListDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce CallListDataflow objects for a method.
 *
 * @author David Hovemeyer
 */
public class CallListDataflowFactory extends AnalysisFactory<CallListDataflow> {
    public CallListDataflowFactory() {
        super("call list analysis", CallListDataflow.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public CallListDataflow analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, descriptor.getClassDescriptor());
        Method method = analysisCache.getMethodAnalysis(Method.class, descriptor);

        CallListAnalysis analysis = new CallListAnalysis(classContext, method);
        CallListDataflow dataflow = new CallListDataflow(getCFG(analysisCache, descriptor), analysis);
        dataflow.execute();

        return dataflow;
    }
}
