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
 * Abstract base class for forward dataflow analyses.
 * Provides convenient implementations for isForwards() and getBlockOrder()
 * methods.
 *
 * @author David Hovemeyer
 * @see Dataflow
 * @see DataflowAnalysis
 */
public abstract class ForwardDataflowAnalysis <Fact> extends AbstractDataflowAnalysis<Fact> {
	private DepthFirstSearch dfs;

	public ForwardDataflowAnalysis(DepthFirstSearch dfs) {
		if (dfs == null) throw new IllegalArgumentException();
		this.dfs = dfs;
	}

	protected DepthFirstSearch getDepthFirstSearch() {
		return dfs;
	}

	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostOrder(cfg, dfs);
	}
}

// vim:ts=4
