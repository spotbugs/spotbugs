package edu.umd.cs.daveho.ba;

public class DFSCFGPrinter extends CFGPrinter implements DFSEdgeTypes {
	private DepthFirstSearch dfs;

	public DFSCFGPrinter(CFG cfg, DepthFirstSearch dfs) {
		super(cfg);
		this.dfs = dfs;
	}

	public String edgeAnnotate(Edge edge) {
		int dfsEdgeType = dfs.getDFSEdgeType(edge);
		switch (dfsEdgeType) {
		case UNKNOWN_EDGE: return "UNKNOWN_EDGE";
		case TREE_EDGE: return "TREE_EDGE";
		case BACK_EDGE: return "BACK_EDGE";
		case CROSS_EDGE: return "CROSS_EDGE";
		case FORWARD_EDGE: return "FORWARD_EDGE";
		default: throw new IllegalStateException("no DFS edge type?");
		}
	}
}

