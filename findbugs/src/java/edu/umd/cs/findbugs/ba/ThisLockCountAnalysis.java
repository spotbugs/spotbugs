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

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A dataflow analysis to count the number of times the "this" object has been
 * locked.
 *
 * @see LockCountAnalysis
 * @see ThisValueAnalysis
 * @author David Hovemeyer
 */
public class ThisLockCountAnalysis extends LockCountAnalysis {

	/**
	 * Constructor.
	 * @param methodGen method to be analyzed
	 * @param vnaDataflow the Dataflow object used to execute ValueNumberAnalysis on the method
	 * @param dfs DepthFirstSearch of the method
	 */
	public ThisLockCountAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs) {
		super(methodGen, vnaDataflow, dfs);
	}

	public void initEntryFact(LockCount result) {
		if (!methodGen.isStatic() && methodGen.isSynchronized())
			result.setCount(1);
		else
			result.setCount(0);
	}

	public int getDelta(Instruction ins, ValueNumberFrame frame) throws DataflowAnalysisException {
		int delta = 0;

		// Update when we see a MONITORENTER or MONITOREXIT on the
		// "this" reference
		if (ins instanceof MONITORENTER) {
			if (frame != null && isThisValue(frame.getTopValue()))
				++delta;
		} else if (ins instanceof MONITOREXIT) {
			if (frame != null && isThisValue(frame.getTopValue()))
				--delta;
		}

		return delta;
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("edu.umd.cs.daveho.ba.ThisLockCountAnalysis <filename>");
				System.exit(1);
			}

			DataflowTestDriver<LockCount, LockCountAnalysis> driver = new DataflowTestDriver<LockCount, LockCountAnalysis>() {
				public LockCountAnalysis createAnalysis(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException {
					DepthFirstSearch dfs = new DepthFirstSearch(cfg).search();

					// Perform the analysis to propagate "this" value references,
					// since ThisLockCountAnalysis depends on it.
					ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, new ValueNumberAnalysis(methodGen, dfs));
					vnaDataflow.execute();

					// Now we can create ThisLockCountAnalysis.
					return new ThisLockCountAnalysis(methodGen, vnaDataflow, dfs);
				}
			};

			driver.execute(argv[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

// vim:ts=4
