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
 * @see ThisValueAnalysis
 * @author David Hovemeyer
 */
public class OtherLockCountAnalysis extends LockCountAnalysis {
	public OtherLockCountAnalysis(MethodGen methodGen, Dataflow<ThisValueFrame> tvaDataflow) {
		super(methodGen, tvaDataflow);
	}

	public void initEntryFact(LockCount result) {
		if (methodGen.isStatic() && methodGen.isSynchronized())
			result.setCount(1);
		else
			result.setCount(0);
	}

	public int getDelta(Instruction ins, ThisValueFrame frame) throws DataflowAnalysisException  {
		int delta = 0;

		if (ins instanceof MONITORENTER) {
			if (frame == null || frame.getTopValue().isNotThis())
				++delta;
		} else if (ins instanceof MONITOREXIT) {
			if (frame == null || frame.getTopValue().isNotThis())
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

			DataflowTestDriver<LockCount> driver = new DataflowTestDriver<LockCount>() {
				public DataflowAnalysis<LockCount> createAnalysis(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException {
					// Perform the analysis to propagate "this" value references,
					// since ThisLockCountAnalysis depends on it.
					Dataflow<ThisValueFrame> tvaDataflow = new Dataflow<ThisValueFrame>(cfg, new ThisValueAnalysis(methodGen));
					tvaDataflow.execute();

					// Now we can create ThisLockCountAnalysis.
					return new OtherLockCountAnalysis(methodGen, tvaDataflow);
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
