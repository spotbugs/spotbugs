package edu.umd.cs.daveho.ba;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class AnyLockCountAnalysis extends LockCountAnalysis {

	public AnyLockCountAnalysis(MethodGen methodGen, Dataflow<ThisValueFrame> tvaDataflow) {
		super(methodGen, tvaDataflow);
	}

	public void initEntryFact(LockCount result) {
		if (methodGen.isSynchronized())
			result.setCount(1);
		else
			result.setCount(0);
	}

	public int getDelta(Instruction ins, ThisValueFrame frame) throws DataflowAnalysisException {
		int delta = 0;
		if (ins instanceof MONITORENTER)
			++delta;
		else if (ins instanceof MONITOREXIT)
			--delta;
		return delta;
	}
}

// vim:ts=4
