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

package edu.umd.cs.daveho.ba;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.classfile.Method;

public class ExceptionPathValueAnalysis extends ForwardDataflowAnalysis<ExceptionPathValue>
	implements EdgeTypes {

	public ExceptionPathValueAnalysis(DepthFirstSearch dfs) {
		super(dfs);
	}

	public ExceptionPathValue createFact() {
		return new ExceptionPathValue(ExceptionPathValue.TOP);
	}

	public void copy(ExceptionPathValue source, ExceptionPathValue dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(ExceptionPathValue result) {
		result.setKind(ExceptionPathValue.NON_EXCEPTION);
	}

	public void initResultFact(ExceptionPathValue result) {
		result.setKind(ExceptionPathValue.TOP);
	}

	public void makeFactTop(ExceptionPathValue fact) {
		fact.setKind(ExceptionPathValue.TOP);
	}

	public boolean same(ExceptionPathValue fact1, ExceptionPathValue fact2) {
		return fact1.equals(fact2);
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ExceptionPathValue fact)
		throws DataflowAnalysisException {
		// Nothing to do
	}

	public boolean isFactValid(ExceptionPathValue fact) { return true; }

	public void meetInto(ExceptionPathValue fact, Edge edge, ExceptionPathValue result) throws DataflowAnalysisException {
		if (edge.isExceptionEdge()) {
			fact = (edge.getType() == HANDLED_EXCEPTION_EDGE)
				? new ExceptionPathValue(ExceptionPathValue.HANDLED_EXCEPTION)
				: new ExceptionPathValue(ExceptionPathValue.UNHANDLED_EXCEPTION);
		}

		result.mergeWith(fact);
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + ExceptionPathValueAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		DataflowTestDriver<ExceptionPathValue, ExceptionPathValueAnalysis> driver = new DataflowTestDriver<ExceptionPathValue, ExceptionPathValueAnalysis>() {
			public Dataflow<ExceptionPathValue, ExceptionPathValueAnalysis>
				createDataflow(ClassContext classContext, Method method)
				throws CFGBuilderException, DataflowAnalysisException {

				ExceptionPathValueAnalysis analysis =
					new ExceptionPathValueAnalysis(classContext.getDepthFirstSearch(method));
				ExceptionPathValueDataflow dataflow =
					new ExceptionPathValueDataflow(classContext.getCFG(method), analysis);
				dataflow.execute();
				return dataflow;
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=4
