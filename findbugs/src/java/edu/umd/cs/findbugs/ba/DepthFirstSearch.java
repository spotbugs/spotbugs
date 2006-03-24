/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

/**
 * Algorithm to perform a depth first search on a CFG.
 *
 * @see CFG
 * @author David Hovemeyer
 */
public class DepthFirstSearch
	extends edu.umd.cs.findbugs.graph.DepthFirstSearch<CFG, Edge, BasicBlock>
{
	private BasicBlock firstRoot;

	/**
	 * Constructor.
	 *
	 * @param cfg the CFG to perform the depth first search on
	 */
	public DepthFirstSearch(CFG cfg) {
		super(cfg);
		firstRoot = cfg.getEntry();
	}

	@Override
         protected BasicBlock getNextSearchTreeRoot() {
		BasicBlock result = firstRoot;
		firstRoot = null;
		return result;
	}
}

// vim:ts=4
