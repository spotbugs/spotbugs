package edu.umd.cs.daveho.ba;

import java.util.*;

/**
 * Specify an order for visiting basic blocks.
 */
public interface BlockOrder {
	/**
	 * Return an Iterator which visits the basic blocks in order.
	 */
	public Iterator<BasicBlock> blockIterator();
}

// vim:ts=4
