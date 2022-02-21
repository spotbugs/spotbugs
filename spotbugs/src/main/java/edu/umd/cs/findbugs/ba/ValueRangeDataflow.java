package edu.umd.cs.findbugs.ba;

public class ValueRangeDataflow extends Dataflow<ValueRangeMap, ValueRangeAnalysis> {

    public ValueRangeDataflow(CFG cfg, ValueRangeAnalysis analysis) {
        super(cfg, analysis);
    }
}
