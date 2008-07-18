/*
 * Bytecode Analysis Framework
 * Copyright (C) 2008 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.generic.InstructionHandle;

/**
 * Visit the BasicBlocks, InstructionHandles, and Edges
 * along a Path.
 * 
 * @author David Hovemeyer
 */
public interface PathVisitor {
	/**
	 * Start to visit the given BasicBlock.
	 * 
	 * @param basicBlock a BasicBlock in the Path being visited
	 */
	public void visitBasicBlock(BasicBlock basicBlock);
	
	/**
	 * Visit an InstructionHandle within the BasicBlock currently being visited.
	 * 
	 * @param handle an InstructionHandle within the current BasicBlock
	 */
	public void visitInstructionHandle(InstructionHandle handle);
	
	/**
	 * Visit an Edge connecting two BasicBlocks in the Path being visited.
	 * 
	 * @param edge an Edge connecting two BasicBlocks in the Path being visited
	 */
	public void visitEdge(Edge edge);
}
