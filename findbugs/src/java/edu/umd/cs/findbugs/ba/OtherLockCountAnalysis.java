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
 * A dataflow analysis to count the number of times an object other than "this"
 * has been locked.
 *
 * @see LockCountAnalysis
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class OtherLockCountAnalysis extends LockCountAnalysis {
	public OtherLockCountAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow) {
		super(methodGen, vnaDataflow);
	}

	public void initEntryFact(LockCount result) {
		if (methodGen.isStatic() && methodGen.isSynchronized())
			result.setCount(1);
		else
			result.setCount(0);
	}

	public int getDelta(Instruction ins, ValueNumberFrame frame) throws DataflowAnalysisException  {
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
				System.out.println("edu.umd.cs.daveho.ba.OtherLockCountAnalysis <filename>");
				System.exit(1);
			}

			DataflowTestDriver<LockCount, LockCountAnalysis> driver = new DataflowTestDriver<LockCount, LockCountAnalysis>() {
				public LockCountAnalysis createAnalysis(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException {
					ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, new ValueNumberAnalysis(methodGen));
					vnaDataflow.execute();

					return new OtherLockCountAnalysis(methodGen, vnaDataflow);
				}
			};

			driver.execute(argv[0]);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

// vim:ts=4
