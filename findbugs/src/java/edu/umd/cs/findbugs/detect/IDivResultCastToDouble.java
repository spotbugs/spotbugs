package edu.umd.cs.findbugs.detect;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

public class IDivResultCastToDouble extends BytecodeScanningDetector {
	private static final boolean DEBUG = Boolean.getBoolean("idcd.debug");
	
	private static final int SCAN = 0;
	private static final int SAW_IDIV = 1;
	private static final int SAW_IDIV_BUG = 2;
	
	private BugReporter bugReporter;
	private int prevOpCode;
	
	public IDivResultCastToDouble(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	//@Override
	public void visit(Method obj) {
		if (DEBUG) System.out.println("Visiting " + obj);
	}

	public void sawOpcode(int seen) {
		
		if (DEBUG) System.out.println("Saw opcode " + seen);
	
		if ((prevOpCode  == I2D || prevOpCode == L2D)
                        && seen == INVOKESTATIC
                                && getClassConstantOperand().equals("java/lang/Math")
                                && getNameConstantOperand().equals("ceil"))
			bugReporter.reportBug(new BugInstance(this, 
				"ICAST_INT_CAST_TO_DOUBLE_PASSED_TO_CEIL", 
				HIGH_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));

		if (prevOpCode  == IDIV && (seen == I2D|| seen == I2F)
			|| prevOpCode  == LDIV && (seen == L2D || seen==L2F))
			bugReporter.reportBug(new BugInstance(this, "ICAST_IDIV_CAST_TO_DOUBLE", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));

		prevOpCode = seen;
		}
}
