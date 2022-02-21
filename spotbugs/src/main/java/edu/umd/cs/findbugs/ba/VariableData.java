package edu.umd.cs.findbugs.ba;

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Map;

public class VariableData {
    private final LongRangeSet splitSet;
    private final Map<Edge, Branch> edges = new IdentityHashMap<>();
    private final BitSet reachableBlocks = new BitSet();

    public VariableData(String type) {
        splitSet = new LongRangeSet(type);
    }

    public void addBranch(Edge edge, Branch branch) {
        edges.put(edge, branch);
    }

    public LongRangeSet getSplitSet() {
        return splitSet;
    }

    public Map<Edge, Branch> getEdges() {
        return edges;
    }

    public BitSet getReachableBlocks() {
        return reachableBlocks;
    }
}
