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
 * Dataflow analysis to compute postdominator sets for a CFG.
 *
 * @author David Hovemeyer
 * @see CFG
 * @see AbstractDominatorsAnalysis
 */
public class PostDominatorsAnalysis extends AbstractDominatorsAnalysis {
	private ReverseDepthFirstSearch rdfs;

	/**
	 * Constructor.
	 *
	 * @param cfg                  the CFG to compute dominator relationships for
	 * @param rdfs                 the ReverseDepthFirstSearch on the CFG
	 * @param ignoreExceptionEdges true if exception edges should be ignored
	 */
	public PostDominatorsAnalysis(CFG cfg, ReverseDepthFirstSearch rdfs,
	                              boolean ignoreExceptionEdges) {
		super(cfg, ignoreExceptionEdges);
		this.rdfs = rdfs;
	}

	public boolean isForwards() {
		return false;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReverseDFSOrder(cfg, rdfs);
	}
}

// vim:ts=4
