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
	 * @param tvaDataflow dataflow results indicating which frame slots hold
	 *   the "this" reference (may be null, for static methods)
	 */
	public ThisLockCountAnalysis(MethodGen methodGen, Dataflow<ThisValueFrame> tvaDataflow) {
		super(methodGen, tvaDataflow);
	}

	public void initEntryFact(LockCount result) {
		if (!methodGen.isStatic() && methodGen.isSynchronized())
			result.setCount(1);
		else
			result.setCount(0);
	}

	public int getDelta(Instruction ins, ThisValueFrame frame) throws DataflowAnalysisException {
		int delta = 0;

		// Update when we see a MONITORENTER or MONITOREXIT on the
		// "this" reference
		if (ins instanceof MONITORENTER) {
			if (frame != null && frame.getTopValue().isThis())
				++delta;
		} else if (ins instanceof MONITOREXIT) {
			if (frame != null && frame.getTopValue().isThis())
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

			DataflowTestDriver<LockCount> driver = new DataflowTestDriver<LockCount>() {
				public DataflowAnalysis<LockCount> createAnalysis(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException {
					// Perform the analysis to propagate "this" value references,
					// since ThisLockCountAnalysis depends on it.
					Dataflow<ThisValueFrame> tvaDataflow = new Dataflow<ThisValueFrame>(cfg, new ThisValueAnalysis(methodGen));
					tvaDataflow.execute();

					// Now we can create ThisLockCountAnalysis.
					return new ThisLockCountAnalysis(methodGen, tvaDataflow);
				}
			};

			// Important!
			// We need to know which instructions are actually throwing
			// exceptions.  Otherwise, values will not merge properly.
			driver.setCFGBuilderMode(CFGBuilderModes.EXCEPTION_SENSITIVE_MODE);

			driver.execute(argv[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

// vim:ts=4
