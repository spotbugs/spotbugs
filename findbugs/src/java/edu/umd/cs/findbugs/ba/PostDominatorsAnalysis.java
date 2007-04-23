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

import java.util.BitSet;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Dataflow analysis to compute postdominator sets for a CFG.
 *
 * @author David Hovemeyer
 * @see CFG
 * @see AbstractDominatorsAnalysis
 */
public class PostDominatorsAnalysis extends AbstractDominatorsAnalysis {
	final private ReverseDepthFirstSearch rdfs;
	final private DepthFirstSearch dfs;

	/**
	 * Constructor.
	 *
	 * @param cfg         the CFG to compute dominator relationships for
	 * @param rdfs        the ReverseDepthFirstSearch on the CFG
	 * @param dfs TODO
	 * @param edgeChooser EdgeChooser to choose which Edges to consider significant
	 */
	public PostDominatorsAnalysis(CFG cfg, ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs, EdgeChooser edgeChooser) {
		super(cfg, edgeChooser);
		this.rdfs = rdfs;
		this.dfs = dfs;
	}

	/**
	 * Constructor.
	 *
	 * @param cfg                  the CFG to compute dominator relationships for
	 * @param rdfs                 the ReverseDepthFirstSearch on the CFG
	 * @param dfs TODO
	 * @param ignoreExceptionEdges true if exception edges should be ignored
	 */
	public PostDominatorsAnalysis(CFG cfg, ReverseDepthFirstSearch rdfs,
								  DepthFirstSearch dfs, boolean ignoreExceptionEdges) {
		super(cfg, ignoreExceptionEdges);
		this.rdfs = rdfs;
		this.dfs = dfs;
	}

	public boolean isForwards() {
		return false;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReverseDFSOrder(cfg, rdfs, dfs);
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.err.println("Usage: " + PostDominatorsAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		DataflowTestDriver<BitSet, PostDominatorsAnalysis> driver =
			new DataflowTestDriver<BitSet, PostDominatorsAnalysis>() {

			/* (non-Javadoc)
			 * @see edu.umd.cs.findbugs.ba.DataflowTestDriver#createDataflow(edu.umd.cs.findbugs.ba.ClassContext, org.apache.bcel.classfile.Method)
			 */
			@Override
			public Dataflow<BitSet, PostDominatorsAnalysis> createDataflow(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
				CFG cfg = classContext.getCFG(method);
				ReverseDepthFirstSearch rdfs = classContext.getReverseDepthFirstSearch(method);

				PostDominatorsAnalysis analysis =
					new PostDominatorsAnalysis(cfg, rdfs, classContext.getDepthFirstSearch(method), SystemProperties.getBoolean("dominators.ignoreexceptionedges"));

				Dataflow<BitSet, PostDominatorsAnalysis> dataflow =
					new Dataflow<BitSet, PostDominatorsAnalysis>(cfg, analysis);

				dataflow.execute();

				return dataflow;
			}

		};

		driver.execute(args[0]);
	}
}

// vim:ts=4
