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
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.MONITORENTER;
import org.apache.bcel.generic.MONITOREXIT;
import org.apache.bcel.generic.MethodGen;

/**
 * A dataflow analysis to count the number of times an object other than "this"
 * has been locked.
 *
 * @author David Hovemeyer
 * @see LockCountAnalysis
 * @see ValueNumberAnalysis
 */
public class OtherLockCountAnalysis extends LockCountAnalysis {
	public OtherLockCountAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs) {
		super(methodGen, vnaDataflow, dfs);
	}

	public void initEntryFact(LockCount result) {
		if (methodGen.isStatic() && methodGen.isSynchronized())
			result.setCount(1);
		else
			result.setCount(0);
	}

	public int getDelta(Instruction ins, ValueNumberFrame frame) throws DataflowAnalysisException {
		int delta = 0;

		if (ins instanceof MONITORENTER) {
			if (frame == null || !isThisValue(frame.getTopValue()))
				++delta;
		} else if (ins instanceof MONITOREXIT) {
			if (frame == null || !isThisValue(frame.getTopValue()))
				--delta;
		}

		return delta;
	}

	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("edu.umd.cs.findbugs.ba.OtherLockCountAnalysis <filename>");
				System.exit(1);
			}

			DataflowTestDriver<LockCount, LockCountAnalysis> driver = new DataflowTestDriver<LockCount, LockCountAnalysis>() {
				public Dataflow<LockCount, LockCountAnalysis> createDataflow(ClassContext classContext, Method method)
				        throws CFGBuilderException, DataflowAnalysisException {

					MethodGen methodGen = classContext.getMethodGen(method);
					CFG cfg = classContext.getCFG(method);
					DepthFirstSearch dfs = classContext.getDepthFirstSearch(method);
					ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);

					LockCountAnalysis analysis = new OtherLockCountAnalysis(methodGen, vnaDataflow, dfs);
					Dataflow<LockCount, LockCountAnalysis> dataflow = new Dataflow<LockCount, LockCountAnalysis>(cfg, analysis);
					dataflow.execute();
					return dataflow;
				}
			};

			driver.execute(argv[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

// vim:ts=4
