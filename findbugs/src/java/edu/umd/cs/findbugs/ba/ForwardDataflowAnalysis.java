package edu.umd.cs.daveho.ba;

/**
 * Abstract base class for forward dataflow analyses.
 * Provides convenient implementations for isForwards() and getBlockOrder()
 * methods.
 * @see Dataflow
 * @see DataflowAnalysis
 */
public abstract class ForwardDataflowAnalysis<Fact> implements DataflowAnalysis<Fact> {
	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostfixOrder(cfg);
	}
}

// vim:ts=4
