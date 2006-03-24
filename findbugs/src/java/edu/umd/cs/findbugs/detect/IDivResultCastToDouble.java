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
	
	//@Override
	@Override
         public void visit(Method obj) {
		if (DEBUG) System.out.println("Visiting " + obj);
	}

	@Override
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
