/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.daveho.ba.bcp;

import java.util.*;
import org.apache.bcel.generic.InstructionHandle;
import edu.umd.cs.daveho.ba.*;

public class PatternMatcher {
	private ByteCodePattern pattern;
	private CFG cfg;
	private DepthFirstSearch dfs;
	private LinkedList<BasicBlock> workList;
	private IdentityHashMap<BasicBlock, BasicBlock> visitedBlockMap;

	public PatternMatcher(ByteCodePattern pattern, CFG cfg, DepthFirstSearch dfs) {
		this.pattern = pattern;
		this.cfg = cfg;
		this.dfs = dfs;
		this.workList = new LinkedList<BasicBlock>();
		this.visitedBlockMap = new IdentityHashMap<BasicBlock, BasicBlock>();
	}

	public void execute() {
		workList.addLast(cfg.getEntry());

		while (!workList.isEmpty()) {
			BasicBlock basicBlock = workList.removeLast();
			visitedBlockMap.put(basicBlock, basicBlock);

			// Scan instructions of basic block for possible matches
			Iterator<InstructionHandle> i = basicBlock.instructionIterator();
			while (i.hasNext()) {
				InstructionHandle handle = i.next();
				attemptMatch(basicBlock, handle);
			}

			// Add successors of the basic block (which haven't been visited already)
			Iterator<BasicBlock> succIterator = cfg.successorIterator(basicBlock);
			while (succIterator.hasNext()) {
				BasicBlock succ = succIterator.next();
				if (visitedBlockMap.get(succ) == null)
					workList.addLast(succ);
			}
		}
	}

	private void attemptMatch(BasicBlock basicBlock, InstructionHandle handle) {
	}
}

// vim:ts=4
