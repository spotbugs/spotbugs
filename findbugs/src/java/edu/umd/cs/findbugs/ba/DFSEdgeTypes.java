package edu.umd.cs.daveho.ba;

/**
 * Edge types in a depth first search.
 * @see DepthFirstSearch
 */
public interface DFSEdgeTypes {
	/** Unknown DFS edge type. This is for internal use only. */
	public static final int UNKNOWN_EDGE = -1;
	/** Tree edge.  Basically, and edge that is part of a depth-first search tree. */
	public static final int TREE_EDGE = 0;
	/** Back edge. An edge to an ancestor in the same depth-first search tree. */
	public static final int BACK_EDGE = 1;
	/** Forward edge. An edge to a descendant in the same depth-first search tree. */
	public static final int FORWARD_EDGE = 2;
	/** Cross edge. Edge between unrelated nodes in the same depth-first search tree,
		or an edge between nodes in different depth-first search trees. */
	public static final int CROSS_EDGE = 3;
}

// vim:ts=4
