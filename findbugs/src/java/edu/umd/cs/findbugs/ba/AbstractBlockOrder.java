package edu.umd.cs.daveho.ba;

import java.util.*;

/**
 * Abstract base class for BlockOrder variants.
 * It allows the subclass to specify just a Comparator for
 * BasicBlocks, and handles the work of doing the sorting
 * and providing Iterators.
 * @see BlockOrder
 */
public abstract class AbstractBlockOrder implements BlockOrder {
	private ArrayList<BasicBlock> blockList;

	public AbstractBlockOrder(CFG cfg, Comparator<BasicBlock> comparator) {
		// Put the blocks in an array
		int numBlocks = cfg.getNumBasicBlocks(), count = 0;
		BasicBlock[] blocks = new BasicBlock[numBlocks];
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext(); ) {
			blocks[count++] = i.next();
		}
		assert count == numBlocks;

		// Sort the blocks according to the comparator
		Arrays.sort(blocks, comparator);

		// Put the ordered blocks into an array list
		blockList = new ArrayList<BasicBlock>(numBlocks);
		for (int i = 0; i < numBlocks; ++i)
			blockList.add(blocks[i]);
	}

	public Iterator<BasicBlock> blockIterator() {
		return blockList.iterator();
	}
}

// vim:ts=4
