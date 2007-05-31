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

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;

public class ReturnPathAnalysis extends ForwardDataflowAnalysis<ReturnPath> implements EdgeTypes {
	public ReturnPathAnalysis(DepthFirstSearch dfs) {
		super(dfs);
	}

	public ReturnPath createFact() {
		return new ReturnPath(ReturnPath.TOP);
	}

	public void copy(ReturnPath source, ReturnPath dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(ReturnPath fact) {
		fact.setKind(ReturnPath.RETURNS);
	}

	public void makeFactTop(ReturnPath fact) {
		fact.setKind(ReturnPath.TOP);
	}

	public boolean isTop(ReturnPath fact) {
		return fact.getKind() == ReturnPath.TOP;
	}
	public boolean same(ReturnPath fact1, ReturnPath fact2) {
		return fact1.sameAs(fact2);
	}

	@Override
		 public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ReturnPath fact)
			throws DataflowAnalysisException {
		// Nothing to do
	}

	@Override
		 public boolean isFactValid(ReturnPath fact) {
		return true;
	}

	public void meetInto(ReturnPath fact, Edge edge, ReturnPath result) throws DataflowAnalysisException {
		switch (edge.getType()) {
		case UNHANDLED_EXCEPTION_EDGE:
			fact = new ReturnPath(ReturnPath.UE);
			break;
		case EXIT_EDGE:
			fact = new ReturnPath(ReturnPath.EXIT);
			break;
		}

		result.mergeWith(fact);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + ReturnPathAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		DataflowTestDriver<ReturnPath, ReturnPathAnalysis> driver = new DataflowTestDriver<ReturnPath, ReturnPathAnalysis>() {
			@Override
						 public Dataflow<ReturnPath, ReturnPathAnalysis>
					createDataflow(ClassContext classContext, Method method)
					throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getReturnPathDataflow(method);
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=4
