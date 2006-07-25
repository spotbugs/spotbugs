package edu.umd.cs.findbugs.detect;


import edu.umd.cs.findbugs.*;
import org.apache.bcel.classfile.Method;

public class IDivResultCastToDouble extends BytecodeScanningDetector {
	private static final boolean DEBUG = Boolean.getBoolean("idcd.debug");
	
	private BugReporter bugReporter;
	private int prevOpCode;
	
	public IDivResultCastToDouble(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	

	@Override
         public void visit(Method obj) {
		if (DEBUG) System.out.println("Visiting " + obj);
	}

	BugInstance pendingBug = null;
	@Override
         public void sawOpcode(int seen) {
		
		if (DEBUG) System.out.println("Saw opcode " + OPCODE_NAMES[seen] + " " + pendingBug);
	
		
			
		if ((prevOpCode  == I2D || prevOpCode == L2D)
                        && seen == INVOKESTATIC
                                && getClassConstantOperand().equals("java/lang/Math")
                                && getNameConstantOperand().equals("ceil")) {
			bugReporter.reportBug(new BugInstance(this, 
				"ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL", 
				HIGH_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));
			pendingBug = null;
		}
		else if ((prevOpCode  == I2F || prevOpCode == L2F)
                && seen == INVOKESTATIC
                        && getClassConstantOperand().equals("java/lang/Math")
                        && getNameConstantOperand().equals("round")) {
			bugReporter.reportBug(new BugInstance(this, 
					"ICAST_INT_CAST_TO_FLOAT_PASSED_TO_ROUND", 
					HIGH_PRIORITY)
			.addClassAndMethod(this)
			.addSourceLine(this));
			pendingBug = null;
		}
		else if (pendingBug != null) {
			bugReporter.reportBug(pendingBug);
			pendingBug = null;
		}
		
		if (prevOpCode  == IDIV && (seen == I2D|| seen == I2F)
			|| prevOpCode  == LDIV && (seen == L2D || seen==L2F))
			pendingBug = new BugInstance(this, "ICAST_IDIV_CAST_TO_DOUBLE", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this);

		prevOpCode = seen;
		}
}
