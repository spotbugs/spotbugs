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
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.SystemProperties;

/**
 * Dataflow analysis to compute dominator sets for a CFG.
 *
 * @author David Hovemeyer
 * @see CFG
 * @see AbstractDominatorsAnalysis
 */
public class DominatorsAnalysis extends AbstractDominatorsAnalysis {
	private DepthFirstSearch dfs;

	/**
	 * Constructor.
	 *
	 * @param cfg                  the CFG to compute dominator relationships for
	 * @param dfs                  the DepthFirstSearch on the CFG
	 * @param ignoreExceptionEdges true if exception edges should be ignored
	 */
	public DominatorsAnalysis(CFG cfg, DepthFirstSearch dfs, boolean ignoreExceptionEdges) {
		super(cfg, ignoreExceptionEdges);
		this.dfs = dfs;
	}

	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostfixOrder(cfg, dfs);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + DominatorsAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		RepositoryLookupFailureCallback lookupFailureCallback = new DebugRepositoryLookupFailureCallback();

		AnalysisContext analysisContext = AnalysisContext.create(lookupFailureCallback);

		JavaClass jclass = new ClassParser(argv[0]).parse();
		ClassContext classContext = analysisContext.getClassContext(jclass);

		String methodName = SystemProperties.getProperty("dominators.method");
		boolean ignoreExceptionEdges = SystemProperties.getBoolean("dominators.ignoreExceptionEdges");

		Method[] methodList = jclass.getMethods();
		for (Method method : methodList) {
			if (method.isNative() || method.isAbstract())
				continue;

			if (methodName != null && !methodName.equals(method.getName()))
				continue;

			System.out.println("Method: " + method.getName());

			CFG cfg = classContext.getCFG(method);
			DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);

			DominatorsAnalysis analysis = new DominatorsAnalysis(cfg, dfs, ignoreExceptionEdges);
			Dataflow<BitSet, DominatorsAnalysis> dataflow =
					new Dataflow<BitSet, DominatorsAnalysis>(cfg, analysis);
			dataflow.execute();

			for (Iterator<BasicBlock> j = cfg.blockIterator(); j.hasNext();) {
				BasicBlock block = j.next();
				BitSet dominators = analysis.getResultFact(block);
				System.out.println("Block " + block.getId() + ": " + dominators);
			}
		}
	}
}

// vim:ts=4
